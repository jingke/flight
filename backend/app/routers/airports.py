from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.database import get_db
from app.models.airport import Airport
from app.schemas.airport import AirportResponse

router = APIRouter(prefix="/api/airports", tags=["airports"])


@router.get("/", response_model=list[AirportResponse])
def list_airports(db: Session = Depends(get_db)) -> list[Airport]:
    return db.query(Airport).order_by(Airport.code).all()


@router.get("/{airport_id}", response_model=AirportResponse)
def get_airport(airport_id: int, db: Session = Depends(get_db)) -> Airport:
    airport = db.query(Airport).filter(Airport.id == airport_id).first()
    if not airport:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail="Airport not found"
        )
    return airport
