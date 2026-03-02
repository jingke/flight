from pydantic import BaseModel, ConfigDict


class AirportResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    code: str
    name: str
    city: str
    country: str
    latitude: float
    longitude: float


class AirportBrief(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    code: str
    name: str
    city: str
    country: str
