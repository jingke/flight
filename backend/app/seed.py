from datetime import datetime, timedelta

from passlib.context import CryptContext
from sqlalchemy.orm import Session

from app.models.airport import Airport
from app.models.flight import Flight, FlightStatus
from app.models.seat import Seat, SeatClass
from app.models.user import User, UserRole
from app.models.loyalty import LoyaltyPoints

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

AIRPORTS = [
    {"code": "JFK", "name": "John F. Kennedy International Airport", "city": "New York", "country": "United States", "latitude": 40.6413, "longitude": -73.7781},
    {"code": "LAX", "name": "Los Angeles International Airport", "city": "Los Angeles", "country": "United States", "latitude": 33.9425, "longitude": -118.4081},
    {"code": "ORD", "name": "O'Hare International Airport", "city": "Chicago", "country": "United States", "latitude": 41.9742, "longitude": -87.9073},
    {"code": "LHR", "name": "Heathrow Airport", "city": "London", "country": "United Kingdom", "latitude": 51.4700, "longitude": -0.4543},
    {"code": "CDG", "name": "Charles de Gaulle Airport", "city": "Paris", "country": "France", "latitude": 49.0097, "longitude": 2.5479},
    {"code": "DXB", "name": "Dubai International Airport", "city": "Dubai", "country": "United Arab Emirates", "latitude": 25.2532, "longitude": 55.3657},
    {"code": "HND", "name": "Haneda Airport", "city": "Tokyo", "country": "Japan", "latitude": 35.5494, "longitude": 139.7798},
    {"code": "SIN", "name": "Changi Airport", "city": "Singapore", "country": "Singapore", "latitude": 1.3644, "longitude": 103.9915},
    {"code": "FRA", "name": "Frankfurt Airport", "city": "Frankfurt", "country": "Germany", "latitude": 50.0379, "longitude": 8.5622},
    {"code": "SYD", "name": "Sydney Kingsford Smith Airport", "city": "Sydney", "country": "Australia", "latitude": -33.9461, "longitude": 151.1772},
    {"code": "IST", "name": "Istanbul Airport", "city": "Istanbul", "country": "Turkey", "latitude": 41.2753, "longitude": 28.7519},
    {"code": "GRU", "name": "São Paulo–Guarulhos Airport", "city": "São Paulo", "country": "Brazil", "latitude": -23.4356, "longitude": -46.4731},
]

BASE_DATE = datetime(2026, 4, 1, 8, 0)

FLIGHTS = [
    {"flight_number": "FB101", "from": "JFK", "to": "LAX", "hours": 5.5, "price": 320.00, "seats": 180},
    {"flight_number": "FB102", "from": "LAX", "to": "JFK", "hours": 5.0, "price": 310.00, "seats": 180},
    {"flight_number": "FB201", "from": "JFK", "to": "LHR", "hours": 7.0, "price": 650.00, "seats": 250},
    {"flight_number": "FB202", "from": "LHR", "to": "JFK", "hours": 8.0, "price": 680.00, "seats": 250},
    {"flight_number": "FB301", "from": "ORD", "to": "CDG", "hours": 8.5, "price": 720.00, "seats": 220},
    {"flight_number": "FB302", "from": "CDG", "to": "ORD", "hours": 9.0, "price": 700.00, "seats": 220},
    {"flight_number": "FB401", "from": "LHR", "to": "DXB", "hours": 7.0, "price": 550.00, "seats": 300},
    {"flight_number": "FB402", "from": "DXB", "to": "SIN", "hours": 7.5, "price": 480.00, "seats": 300},
    {"flight_number": "FB501", "from": "SIN", "to": "HND", "hours": 7.0, "price": 520.00, "seats": 280},
    {"flight_number": "FB502", "from": "HND", "to": "LAX", "hours": 10.0, "price": 890.00, "seats": 280},
    {"flight_number": "FB601", "from": "FRA", "to": "IST", "hours": 3.0, "price": 280.00, "seats": 200},
    {"flight_number": "FB602", "from": "IST", "to": "DXB", "hours": 4.0, "price": 350.00, "seats": 200},
    {"flight_number": "FB701", "from": "SYD", "to": "SIN", "hours": 8.0, "price": 610.00, "seats": 260},
    {"flight_number": "FB702", "from": "GRU", "to": "JFK", "hours": 10.0, "price": 750.00, "seats": 240},
    {"flight_number": "FB801", "from": "LAX", "to": "SYD", "hours": 15.0, "price": 1250.00, "seats": 300},
]

SEAT_LAYOUT = {"columns": ["A", "B", "C", "D", "E", "F"]}


def _create_seats_for_flight(db: Session, flight: Flight) -> None:
    total_rows = flight.total_seats // len(SEAT_LAYOUT["columns"])
    first_class_rows = max(1, total_rows // 10)
    business_rows = max(2, total_rows // 5)
    for row_num in range(1, total_rows + 1):
        if row_num <= first_class_rows:
            seat_class = SeatClass.FIRST
        elif row_num <= first_class_rows + business_rows:
            seat_class = SeatClass.BUSINESS
        else:
            seat_class = SeatClass.ECONOMY
        for col in SEAT_LAYOUT["columns"]:
            seat = Seat(
                flight_id=flight.id,
                row=row_num,
                column=col,
                seat_class=seat_class,
                is_available=True,
            )
            db.add(seat)


def seed_database(db: Session) -> None:
    if db.query(Airport).first() is not None:
        return

    airport_map: dict[str, Airport] = {}
    for data in AIRPORTS:
        airport = Airport(**data)
        db.add(airport)
        airport_map[data["code"]] = airport
    db.flush()

    for i, data in enumerate(FLIGHTS):
        dep_airport = airport_map[data["from"]]
        arr_airport = airport_map[data["to"]]
        departure_time = BASE_DATE + timedelta(days=i, hours=i % 12)
        arrival_time = departure_time + timedelta(hours=data["hours"])
        flight = Flight(
            flight_number=data["flight_number"],
            departure_airport_id=dep_airport.id,
            arrival_airport_id=arr_airport.id,
            departure_time=departure_time,
            arrival_time=arrival_time,
            price=data["price"],
            total_seats=data["seats"],
            status=FlightStatus.SCHEDULED,
        )
        db.add(flight)
        db.flush()
        _create_seats_for_flight(db, flight)

    admin_user = User(
        email="admin@flightbooking.com",
        password_hash=pwd_context.hash("admin123"),
        name="Admin User",
        role=UserRole.ADMIN,
    )
    db.add(admin_user)

    demo_user = User(
        email="demo@flightbooking.com",
        password_hash=pwd_context.hash("demo123"),
        name="Demo Customer",
        role=UserRole.CUSTOMER,
    )
    db.add(demo_user)
    db.flush()

    db.add(LoyaltyPoints(user_id=admin_user.id, earned=0, redeemed=0, balance=0))
    db.add(LoyaltyPoints(user_id=demo_user.id, earned=500, redeemed=0, balance=500))

    db.commit()
