from pydantic import BaseModel, ConfigDict


class LoyaltyResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    user_id: int
    earned: int
    redeemed: int
    balance: int


class LoyaltyRedeem(BaseModel):
    points: int
