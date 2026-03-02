from datetime import datetime

from pydantic import BaseModel, ConfigDict


class PassengerInput(BaseModel):
    name: str
    email: str
    seat_id: int | None = None


class BookingCreate(BaseModel):
    flight_id: int
    passengers: list[PassengerInput]


class PassengerResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    name: str
    email: str
    seat_assignment: str | None = None


class BookingResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    user_id: int
    flight_id: int
    status: str
    total_price: float
    payment_status: str
    created_at: datetime
    passengers: list[PassengerResponse] = []


class BookingDetail(BookingResponse):
    flight_number: str | None = None
    departure_airport: str | None = None
    arrival_airport: str | None = None
    departure_time: datetime | None = None
    arrival_time: datetime | None = None
