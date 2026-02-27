from pydantic import BaseModel, ConfigDict


class SeatResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    flight_id: int
    row: int
    column: str
    seat_class: str
    is_available: bool


class SeatAssignment(BaseModel):
    passenger_id: int
    seat_id: int
