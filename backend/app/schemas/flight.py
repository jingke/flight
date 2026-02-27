from datetime import datetime

from pydantic import BaseModel, ConfigDict

from app.schemas.airport import AirportBrief


class FlightCreate(BaseModel):
    flight_number: str
    departure_airport_id: int
    arrival_airport_id: int
    departure_time: datetime
    arrival_time: datetime
    price: float
    total_seats: int


class FlightUpdate(BaseModel):
    departure_time: datetime | None = None
    arrival_time: datetime | None = None
    price: float | None = None
    status: str | None = None


class FlightResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    flight_number: str
    departure_airport_id: int
    arrival_airport_id: int
    departure_time: datetime
    arrival_time: datetime
    price: float
    total_seats: int
    status: str
    departure_airport: AirportBrief | None = None
    arrival_airport: AirportBrief | None = None
    available_seats: int | None = None


class FlightSearch(BaseModel):
    origin: str | None = None
    destination: str | None = None
    date: str | None = None
    min_price: float | None = None
    max_price: float | None = None
