from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.database import get_db
from app.dependencies import get_current_user
from app.models.booking import Booking, BookingStatus, PaymentStatus
from app.models.flight import Flight
from app.models.loyalty import LoyaltyPoints
from app.models.notification import Notification
from app.models.passenger import Passenger
from app.models.seat import Seat
from app.models.user import User, UserRole
from app.schemas.booking import BookingCreate, BookingDetail, BookingResponse

router = APIRouter(prefix="/api/bookings", tags=["bookings"])


def _build_detail(booking: Booking) -> dict:
    flight = booking.flight
    dep = flight.departure_airport
    arr = flight.arrival_airport
    return {
        "id": booking.id,
        "user_id": booking.user_id,
        "flight_id": booking.flight_id,
        "status": booking.status.value,
        "total_price": booking.total_price,
        "payment_status": booking.payment_status.value,
        "created_at": booking.created_at,
        "passengers": booking.passengers,
        "flight_number": flight.flight_number,
        "departure_airport": f"{dep.code} - {dep.city}" if dep else None,
        "arrival_airport": f"{arr.code} - {arr.city}" if arr else None,
        "departure_time": flight.departure_time,
        "arrival_time": flight.arrival_time,
    }


@router.post(
    "/", response_model=BookingDetail, status_code=status.HTTP_201_CREATED
)
def create_booking(
    data: BookingCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> dict:
    flight = db.query(Flight).filter(Flight.id == data.flight_id).first()
    if not flight:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail="Flight not found"
        )
    if not data.passengers:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="At least one passenger required",
        )
    
    existing_bookings = (
        db.query(Booking)
        .filter(
            Booking.user_id == current_user.id,
            Booking.flight_id == data.flight_id,
            Booking.status != BookingStatus.CANCELLED,
        )
        .all()
    )
    
    existing_passengers: dict[str, str] = {}
    for booking in existing_bookings:
        for passenger in booking.passengers:
            existing_passengers[passenger.email.lower()] = passenger.seat_assignment or "No seat"
    
    for p in data.passengers:
        email_lower = p.email.lower()
        if email_lower in existing_passengers:
            seat_info = existing_passengers[email_lower]
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Passenger with email {p.email} is already booked on this flight (Seat: {seat_info})",
            )
    
    total_price = flight.price * len(data.passengers)
    booking = Booking(
        user_id=current_user.id,
        flight_id=data.flight_id,
        status=BookingStatus.CONFIRMED,
        total_price=total_price,
        payment_status=PaymentStatus.PAID,
    )
    db.add(booking)
    db.flush()
    for p in data.passengers:
        seat_label: str | None = None
        if p.seat_id:
            seat = db.query(Seat).filter(
                Seat.id == p.seat_id,
                Seat.flight_id == data.flight_id,
                Seat.is_available == True,  # noqa: E712
            ).first()
            if not seat:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail=f"Seat {p.seat_id} not available",
                )
            seat.is_available = False
            seat_label = f"{seat.row}{seat.column}"
        passenger = Passenger(
            booking_id=booking.id,
            name=p.name,
            email=p.email,
            seat_assignment=seat_label,
        )
        db.add(passenger)
    points_earned = int(total_price * 0.1)
    loyalty = (
        db.query(LoyaltyPoints)
        .filter(LoyaltyPoints.user_id == current_user.id)
        .first()
    )
    if loyalty:
        loyalty.earned += points_earned
        loyalty.balance += points_earned
    db.add(Notification(
        user_id=current_user.id,
        title="Booking Confirmed",
        message=f"Your booking for flight {flight.flight_number} has been confirmed. Total: ${total_price:.2f}",
    ))
    db.commit()
    db.refresh(booking)
    return _build_detail(booking)


@router.get("/", response_model=list[BookingDetail])
def list_bookings(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> list[dict]:
    if current_user.role == UserRole.ADMIN:
        bookings = db.query(Booking).order_by(Booking.created_at.desc()).all()
    else:
        bookings = (
            db.query(Booking)
            .filter(Booking.user_id == current_user.id)
            .order_by(Booking.created_at.desc())
            .all()
        )
    return [_build_detail(b) for b in bookings]


@router.get("/{booking_id}", response_model=BookingDetail)
def get_booking(
    booking_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> dict:
    booking = db.query(Booking).filter(Booking.id == booking_id).first()
    if not booking:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail="Booking not found"
        )
    if (
        current_user.role != UserRole.ADMIN
        and booking.user_id != current_user.id
    ):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN, detail="Access denied"
        )
    return _build_detail(booking)


@router.post("/{booking_id}/cancel", response_model=BookingResponse)
def cancel_booking(
    booking_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> Booking:
    booking = db.query(Booking).filter(Booking.id == booking_id).first()
    if not booking:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail="Booking not found"
        )
    if (
        current_user.role != UserRole.ADMIN
        and booking.user_id != current_user.id
    ):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN, detail="Access denied"
        )
    if booking.status == BookingStatus.CANCELLED:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Booking already cancelled",
        )
    booking.status = BookingStatus.CANCELLED
    booking.payment_status = PaymentStatus.REFUNDED
    for passenger in booking.passengers:
        if passenger.seat_assignment:
            row_str = "".join(c for c in passenger.seat_assignment if c.isdigit())
            col_str = "".join(c for c in passenger.seat_assignment if c.isalpha())
            seat = db.query(Seat).filter(
                Seat.flight_id == booking.flight_id,
                Seat.row == int(row_str),
                Seat.column == col_str,
            ).first()
            if seat:
                seat.is_available = True
    db.add(Notification(
        user_id=booking.user_id,
        title="Booking Cancelled",
        message=f"Your booking #{booking.id} has been cancelled and refunded.",
    ))
    db.commit()
    db.refresh(booking)
    return booking
