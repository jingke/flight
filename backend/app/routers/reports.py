from fastapi import APIRouter, Depends
from sqlalchemy import extract, func
from sqlalchemy.orm import Session

from app.database import get_db
from app.dependencies import require_admin
from app.models.airport import Airport
from app.models.booking import Booking
from app.models.flight import Flight
from app.models.user import User
from app.schemas.report import (
    BookingsPerFlightReport,
    PeakTimeReport,
    PopularRouteReport,
)

router = APIRouter(prefix="/api/reports", tags=["reports"])


@router.get("/bookings-per-flight", response_model=list[BookingsPerFlightReport])
def bookings_per_flight(
    db: Session = Depends(get_db),
    _admin: User = Depends(require_admin),
) -> list[dict]:
    dep_airport = db.query(Airport).subquery()
    arr_airport = db.query(Airport).subquery()
    results = (
        db.query(
            Flight.id.label("flight_id"),
            Flight.flight_number,
            dep_airport.c.code.label("departure"),
            arr_airport.c.code.label("arrival"),
            func.count(Booking.id).label("booking_count"),
        )
        .outerjoin(Booking, Booking.flight_id == Flight.id)
        .join(dep_airport, Flight.departure_airport_id == dep_airport.c.id)
        .join(arr_airport, Flight.arrival_airport_id == arr_airport.c.id)
        .group_by(Flight.id)
        .order_by(func.count(Booking.id).desc())
        .all()
    )
    return [
        {
            "flight_id": r.flight_id,
            "flight_number": r.flight_number,
            "departure": r.departure,
            "arrival": r.arrival,
            "booking_count": r.booking_count,
        }
        for r in results
    ]


@router.get("/popular-routes", response_model=list[PopularRouteReport])
def popular_routes(
    db: Session = Depends(get_db),
    _admin: User = Depends(require_admin),
) -> list[dict]:
    dep = db.query(Airport).subquery()
    arr = db.query(Airport).subquery()
    results = (
        db.query(
            dep.c.code.label("origin_code"),
            dep.c.city.label("origin_city"),
            arr.c.code.label("destination_code"),
            arr.c.city.label("destination_city"),
            func.count(Booking.id).label("booking_count"),
        )
        .select_from(Booking)
        .join(Flight, Booking.flight_id == Flight.id)
        .join(dep, Flight.departure_airport_id == dep.c.id)
        .join(arr, Flight.arrival_airport_id == arr.c.id)
        .group_by(dep.c.code, dep.c.city, arr.c.code, arr.c.city)
        .order_by(func.count(Booking.id).desc())
        .limit(10)
        .all()
    )
    return [
        {
            "origin_code": r.origin_code,
            "origin_city": r.origin_city,
            "destination_code": r.destination_code,
            "destination_city": r.destination_city,
            "booking_count": r.booking_count,
        }
        for r in results
    ]


@router.get("/peak-times", response_model=list[PeakTimeReport])
def peak_booking_times(
    db: Session = Depends(get_db),
    _admin: User = Depends(require_admin),
) -> list[dict]:
    results = (
        db.query(
            extract("hour", Booking.created_at).label("hour"),
            func.count(Booking.id).label("booking_count"),
        )
        .group_by(extract("hour", Booking.created_at))
        .order_by(func.count(Booking.id).desc())
        .all()
    )
    return [
        {"hour": int(r.hour) if r.hour is not None else 0, "booking_count": r.booking_count}
        for r in results
    ]
