from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.database import get_db
from app.dependencies import get_current_user
from app.models.saved_passenger import SavedPassenger
from app.models.user import User
from app.schemas.saved_passenger import (
    SavedPassengerCreate,
    SavedPassengerResponse,
    SavedPassengerUpdate,
)

router = APIRouter(prefix="/api/passengers", tags=["passengers"])


@router.get("", response_model=list[SavedPassengerResponse])
def list_saved_passengers(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> list[SavedPassenger]:
    passengers = (
        db.query(SavedPassenger)
        .filter(SavedPassenger.user_id == current_user.id)
        .order_by(SavedPassenger.id)
        .all()
    )
    return passengers


@router.post("", response_model=SavedPassengerResponse, status_code=status.HTTP_201_CREATED)
def create_saved_passenger(
    data: SavedPassengerCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> SavedPassenger:
    passenger = SavedPassenger(
        user_id=current_user.id,
        name=data.name,
        email=data.email,
    )
    db.add(passenger)
    db.commit()
    db.refresh(passenger)
    return passenger


@router.get("/{passenger_id}", response_model=SavedPassengerResponse)
def get_saved_passenger(
    passenger_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> SavedPassenger:
    passenger = (
        db.query(SavedPassenger)
        .filter(
            SavedPassenger.id == passenger_id,
            SavedPassenger.user_id == current_user.id,
        )
        .first()
    )
    if not passenger:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Passenger not found",
        )
    return passenger


@router.put("/{passenger_id}", response_model=SavedPassengerResponse)
def update_saved_passenger(
    passenger_id: int,
    data: SavedPassengerUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> SavedPassenger:
    passenger = (
        db.query(SavedPassenger)
        .filter(
            SavedPassenger.id == passenger_id,
            SavedPassenger.user_id == current_user.id,
        )
        .first()
    )
    if not passenger:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Passenger not found",
        )
    if data.name is not None:
        passenger.name = data.name
    if data.email is not None:
        passenger.email = data.email
    db.commit()
    db.refresh(passenger)
    return passenger


@router.delete("/{passenger_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_saved_passenger(
    passenger_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> None:
    passenger = (
        db.query(SavedPassenger)
        .filter(
            SavedPassenger.id == passenger_id,
            SavedPassenger.user_id == current_user.id,
        )
        .first()
    )
    if not passenger:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Passenger not found",
        )
    db.delete(passenger)
    db.commit()
