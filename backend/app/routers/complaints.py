from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.database import get_db
from app.dependencies import get_current_user, require_admin
from app.models.complaint import Complaint, ComplaintStatus
from app.models.notification import Notification
from app.models.user import User, UserRole
from app.schemas.complaint import ComplaintCreate, ComplaintResponse, ComplaintUpdate

router = APIRouter(prefix="/api/complaints", tags=["complaints"])


@router.post(
    "/", response_model=ComplaintResponse, status_code=status.HTTP_201_CREATED
)
def create_complaint(
    data: ComplaintCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> Complaint:
    complaint = Complaint(
        user_id=current_user.id,
        booking_id=data.booking_id,
        subject=data.subject,
        description=data.description,
        status=ComplaintStatus.OPEN,
    )
    db.add(complaint)
    db.commit()
    db.refresh(complaint)
    return complaint


@router.get("/", response_model=list[ComplaintResponse])
def list_complaints(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> list[Complaint]:
    if current_user.role == UserRole.ADMIN:
        return db.query(Complaint).order_by(Complaint.created_at.desc()).all()
    return (
        db.query(Complaint)
        .filter(Complaint.user_id == current_user.id)
        .order_by(Complaint.created_at.desc())
        .all()
    )


@router.get("/{complaint_id}", response_model=ComplaintResponse)
def get_complaint(
    complaint_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> Complaint:
    complaint = (
        db.query(Complaint).filter(Complaint.id == complaint_id).first()
    )
    if not complaint:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Complaint not found",
        )
    if (
        current_user.role != UserRole.ADMIN
        and complaint.user_id != current_user.id
    ):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN, detail="Access denied"
        )
    return complaint


@router.put("/{complaint_id}", response_model=ComplaintResponse)
def update_complaint(
    complaint_id: int,
    data: ComplaintUpdate,
    db: Session = Depends(get_db),
    _admin: User = Depends(require_admin),
) -> Complaint:
    complaint = (
        db.query(Complaint).filter(Complaint.id == complaint_id).first()
    )
    if not complaint:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Complaint not found",
        )
    if data.status is not None:
        complaint.status = ComplaintStatus(data.status)
    if data.admin_response is not None:
        complaint.admin_response = data.admin_response
    db.add(Notification(
        user_id=complaint.user_id,
        title="Complaint Updated",
        message=f"Your complaint '{complaint.subject}' has been updated.",
    ))
    db.commit()
    db.refresh(complaint)
    return complaint
