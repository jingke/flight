from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.database import get_db
from app.dependencies import get_current_user
from app.models.passenger import Passenger
from app.models.seat import Seat
from app.models.user import User
from app.schemas.seat import SeatAssignment, SeatResponse

router = APIRouter(prefix="/api/seats", tags=["seats"])


@router.get("/flight/{flight_id}", response_model=list[SeatResponse])
def get_seat_map(
    flight_id: int, db: Session = Depends(get_db)
) -> list[Seat]:
    seats = (
        db.query(Seat)
        .filter(Seat.flight_id == flight_id)
        .order_by(Seat.row, Seat.column)
        .all()
    )
    if not seats:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No seats found for this flight",
        )
    return seats


@router.post("/assign", response_model=SeatResponse)
def assign_seat(
    data: SeatAssignment,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> Seat:
    seat = db.query(Seat).filter(Seat.id == data.seat_id).first()
    if not seat:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail="Seat not found"
        )
    if not seat.is_available:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Seat is not available",
        )
    passenger = (
        db.query(Passenger).filter(Passenger.id == data.passenger_id).first()
    )
    if not passenger:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail="Passenger not found"
        )
    if passenger.seat_assignment:
        old_row = "".join(c for c in passenger.seat_assignment if c.isdigit())
        old_col = "".join(c for c in passenger.seat_assignment if c.isalpha())
        old_seat = db.query(Seat).filter(
            Seat.flight_id == seat.flight_id,
            Seat.row == int(old_row),
            Seat.column == old_col,
        ).first()
        if old_seat:
            old_seat.is_available = True
    seat.is_available = False
    passenger.seat_assignment = f"{seat.row}{seat.column}"
    db.commit()
    db.refresh(seat)
    return seat
