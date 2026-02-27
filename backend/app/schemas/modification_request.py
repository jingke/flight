from datetime import datetime

from pydantic import BaseModel, ConfigDict


class ModificationCreate(BaseModel):
    booking_id: int
    type: str
    details: str


class ModificationUpdate(BaseModel):
    status: str


class ModificationResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    user_id: int
    booking_id: int
    type: str
    details: str
    status: str
    created_at: datetime
