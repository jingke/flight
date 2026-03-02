from pydantic import BaseModel, ConfigDict


class SavedPassengerCreate(BaseModel):
    name: str
    email: str


class SavedPassengerUpdate(BaseModel):
    name: str | None = None
    email: str | None = None


class SavedPassengerResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    user_id: int
    name: str
    email: str
