from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.database import get_db
from app.dependencies import get_current_user
from app.models.loyalty import LoyaltyPoints
from app.models.notification import Notification
from app.models.user import User
from app.schemas.loyalty import LoyaltyRedeem, LoyaltyResponse

router = APIRouter(prefix="/api/loyalty", tags=["loyalty"])


@router.get("/", response_model=LoyaltyResponse)
def get_loyalty(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> LoyaltyPoints:
    loyalty = (
        db.query(LoyaltyPoints)
        .filter(LoyaltyPoints.user_id == current_user.id)
        .first()
    )
    if not loyalty:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Loyalty record not found",
        )
    return loyalty


@router.post("/redeem", response_model=LoyaltyResponse)
def redeem_points(
    data: LoyaltyRedeem,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> LoyaltyPoints:
    loyalty = (
        db.query(LoyaltyPoints)
        .filter(LoyaltyPoints.user_id == current_user.id)
        .first()
    )
    if not loyalty:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Loyalty record not found",
        )
    if data.points <= 0:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Points must be positive",
        )
    if data.points > loyalty.balance:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Insufficient loyalty points",
        )
    loyalty.redeemed += data.points
    loyalty.balance -= data.points
    db.add(Notification(
        user_id=current_user.id,
        title="Points Redeemed",
        message=f"You redeemed {data.points} loyalty points. Remaining balance: {loyalty.balance}",
    ))
    db.commit()
    db.refresh(loyalty)
    return loyalty
