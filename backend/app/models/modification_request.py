import enum
from datetime import datetime

from sqlalchemy import DateTime, Enum, ForeignKey, String, Text, func
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base


class ModificationType(str, enum.Enum):
    DATE_CHANGE = "date_change"
    SEAT_CHANGE = "seat_change"
    PASSENGER_CHANGE = "passenger_change"
    CANCELLATION = "cancellation"


class ModificationStatus(str, enum.Enum):
    PENDING = "pending"
    APPROVED = "approved"
    REJECTED = "rejected"


class ModificationRequest(Base):
    __tablename__ = "modification_requests"

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), nullable=False)
    booking_id: Mapped[int] = mapped_column(ForeignKey("bookings.id"), nullable=False)
    type: Mapped[ModificationType] = mapped_column(Enum(ModificationType), nullable=False)
    details: Mapped[str] = mapped_column(Text, nullable=False)
    status: Mapped[ModificationStatus] = mapped_column(Enum(ModificationStatus), default=ModificationStatus.PENDING, nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now(), nullable=False)

    user = relationship("User", back_populates="modification_requests")
    booking = relationship("Booking", back_populates="modification_requests")
