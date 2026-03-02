from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.database import get_db
from app.dependencies import get_current_user, require_admin
from app.models.booking import Booking
from app.models.modification_request import (
    ModificationRequest,
    ModificationStatus,
    ModificationType,
)
from app.models.notification import Notification
from app.models.user import User, UserRole
from app.schemas.modification_request import (
    ModificationCreate,
    ModificationResponse,
    ModificationUpdate,
)

router = APIRouter(prefix="/api/modifications", tags=["modifications"])


@router.post(
    "/", response_model=ModificationResponse, status_code=status.HTTP_201_CREATED
)
def create_modification(
    data: ModificationCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> ModificationRequest:
    booking = db.query(Booking).filter(Booking.id == data.booking_id).first()
    if not booking:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail="Booking not found"
        )
    if booking.user_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN, detail="Access denied"
        )
    mod = ModificationRequest(
        user_id=current_user.id,
        booking_id=data.booking_id,
        type=ModificationType(data.type),
        details=data.details,
        status=ModificationStatus.PENDING,
    )
    db.add(mod)
    db.commit()
    db.refresh(mod)
    return mod


@router.get("/", response_model=list[ModificationResponse])
def list_modifications(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> list[ModificationRequest]:
    if current_user.role == UserRole.ADMIN:
        return (
            db.query(ModificationRequest)
            .order_by(ModificationRequest.created_at.desc())
            .all()
        )
    return (
        db.query(ModificationRequest)
        .filter(ModificationRequest.user_id == current_user.id)
        .order_by(ModificationRequest.created_at.desc())
        .all()
    )


@router.put("/{mod_id}", response_model=ModificationResponse)
def update_modification(
    mod_id: int,
    data: ModificationUpdate,
    db: Session = Depends(get_db),
    _admin: User = Depends(require_admin),
) -> ModificationRequest:
    mod = (
        db.query(ModificationRequest)
        .filter(ModificationRequest.id == mod_id)
        .first()
    )
    if not mod:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Modification request not found",
        )
    mod.status = ModificationStatus(data.status)
    status_label = "approved" if mod.status == ModificationStatus.APPROVED else "rejected"
    db.add(Notification(
        user_id=mod.user_id,
        title="Modification Request Updated",
        message=f"Your modification request #{mod.id} has been {status_label}.",
    ))
    db.commit()
    db.refresh(mod)
    return mod
