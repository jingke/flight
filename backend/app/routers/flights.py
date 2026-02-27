from datetime import datetime

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import func
from sqlalchemy.orm import Session, joinedload

from app.database import get_db
from app.dependencies import require_admin
from app.models.airport import Airport
from app.models.flight import Flight, FlightStatus
from app.models.seat import Seat, SeatClass
from app.models.user import User
from app.schemas.flight import FlightCreate, FlightResponse, FlightUpdate
from app.seed import SEAT_LAYOUT, _create_seats_for_flight

router = APIRouter(prefix="/api/flights", tags=["flights"])


def _enrich_flight(flight: Flight, db: Session) -> dict:
    available = db.query(func.count(Seat.id)).filter(
        Seat.flight_id == flight.id, Seat.is_available == True  # noqa: E712
    ).scalar()
    return {
        "id": flight.id,
        "flight_number": flight.flight_number,
        "departure_airport_id": flight.departure_airport_id,
        "arrival_airport_id": flight.arrival_airport_id,
        "departure_time": flight.departure_time,
        "arrival_time": flight.arrival_time,
        "price": flight.price,
        "total_seats": flight.total_seats,
        "status": flight.status.value,
        "departure_airport": flight.departure_airport,
        "arrival_airport": flight.arrival_airport,
        "available_seats": available,
    }


@router.get("/search", response_model=list[FlightResponse])
def search_flights(
    origin: str | None = Query(None),
    destination: str | None = Query(None),
    date: str | None = Query(None),
    min_price: float | None = Query(None),
    max_price: float | None = Query(None),
    db: Session = Depends(get_db),
) -> list[dict]:
    query = db.query(Flight).options(
        joinedload(Flight.departure_airport),
        joinedload(Flight.arrival_airport),
    )
    if origin:
        query = query.join(
            Airport, Flight.departure_airport_id == Airport.id
        ).filter(Airport.code == origin.upper())
    if destination:
        query = query.join(
            Airport, Flight.arrival_airport_id == Airport.id
        ).filter(Airport.code == destination.upper())
    if date:
        target_date = datetime.strptime(date, "%Y-%m-%d").date()
        query = query.filter(func.date(Flight.departure_time) == target_date)
    if min_price is not None:
        query = query.filter(Flight.price >= min_price)
    if max_price is not None:
        query = query.filter(Flight.price <= max_price)
    flights = query.all()
    return [_enrich_flight(f, db) for f in flights]


@router.get("/", response_model=list[FlightResponse])
def list_flights(db: Session = Depends(get_db)) -> list[dict]:
    flights = (
        db.query(Flight)
        .options(
            joinedload(Flight.departure_airport),
            joinedload(Flight.arrival_airport),
        )
        .order_by(Flight.departure_time)
        .all()
    )
    return [_enrich_flight(f, db) for f in flights]


@router.get("/{flight_id}", response_model=FlightResponse)
def get_flight(flight_id: int, db: Session = Depends(get_db)) -> dict:
    flight = (
        db.query(Flight)
        .options(
            joinedload(Flight.departure_airport),
            joinedload(Flight.arrival_airport),
        )
        .filter(Flight.id == flight_id)
        .first()
    )
    if not flight:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail="Flight not found"
        )
    return _enrich_flight(flight, db)


@router.post(
    "/", response_model=FlightResponse, status_code=status.HTTP_201_CREATED
)
def create_flight(
    data: FlightCreate,
    db: Session = Depends(get_db),
    _admin: User = Depends(require_admin),
) -> dict:
    flight = Flight(
        flight_number=data.flight_number,
        departure_airport_id=data.departure_airport_id,
        arrival_airport_id=data.arrival_airport_id,
        departure_time=data.departure_time,
        arrival_time=data.arrival_time,
        price=data.price,
        total_seats=data.total_seats,
        status=FlightStatus.SCHEDULED,
    )
    db.add(flight)
    db.flush()
    _create_seats_for_flight(db, flight)
    db.commit()
    db.refresh(flight)
    flight = (
        db.query(Flight)
        .options(
            joinedload(Flight.departure_airport),
            joinedload(Flight.arrival_airport),
        )
        .filter(Flight.id == flight.id)
        .first()
    )
    return _enrich_flight(flight, db)


@router.put("/{flight_id}", response_model=FlightResponse)
def update_flight(
    flight_id: int,
    data: FlightUpdate,
    db: Session = Depends(get_db),
    _admin: User = Depends(require_admin),
) -> dict:
    flight = db.query(Flight).filter(Flight.id == flight_id).first()
    if not flight:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail="Flight not found"
        )
    if data.departure_time is not None:
        flight.departure_time = data.departure_time
    if data.arrival_time is not None:
        flight.arrival_time = data.arrival_time
    if data.price is not None:
        flight.price = data.price
    if data.status is not None:
        flight.status = FlightStatus(data.status)
    db.commit()
    db.refresh(flight)
    flight = (
        db.query(Flight)
        .options(
            joinedload(Flight.departure_airport),
            joinedload(Flight.arrival_airport),
        )
        .filter(Flight.id == flight.id)
        .first()
    )
    return _enrich_flight(flight, db)


@router.delete("/{flight_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_flight(
    flight_id: int,
    db: Session = Depends(get_db),
    _admin: User = Depends(require_admin),
) -> None:
    flight = db.query(Flight).filter(Flight.id == flight_id).first()
    if not flight:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail="Flight not found"
        )
    db.delete(flight)
    db.commit()
