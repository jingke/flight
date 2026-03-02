from pydantic import BaseModel


class BookingsPerFlightReport(BaseModel):
    flight_id: int
    flight_number: str
    departure: str
    arrival: str
    booking_count: int


class PopularRouteReport(BaseModel):
    origin_code: str
    origin_city: str
    destination_code: str
    destination_city: str
    booking_count: int


class PeakTimeReport(BaseModel):
    hour: int
    booking_count: int
