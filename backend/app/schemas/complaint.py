from datetime import datetime

from pydantic import BaseModel, ConfigDict


class ComplaintCreate(BaseModel):
    booking_id: int | None = None
    subject: str
    description: str


class ComplaintUpdate(BaseModel):
    status: str | None = None
    admin_response: str | None = None


class ComplaintResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    user_id: int
    booking_id: int | None = None
    subject: str
    description: str
    status: str
    admin_response: str | None = None
    created_at: datetime
