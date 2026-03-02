from sqlalchemy import ForeignKey, String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base


class Passenger(Base):
    __tablename__ = "passengers"

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    booking_id: Mapped[int] = mapped_column(ForeignKey("bookings.id"), nullable=False)
    name: Mapped[str] = mapped_column(String(255), nullable=False)
    email: Mapped[str] = mapped_column(String(255), nullable=False)
    seat_assignment: Mapped[str | None] = mapped_column(String(10), nullable=True)

    booking = relationship("Booking", back_populates="passengers")
