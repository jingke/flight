import enum

from sqlalchemy import Boolean, Enum, ForeignKey, Integer, String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base


class SeatClass(str, enum.Enum):
    ECONOMY = "economy"
    BUSINESS = "business"
    FIRST = "first"


class Seat(Base):
    __tablename__ = "seats"

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    flight_id: Mapped[int] = mapped_column(ForeignKey("flights.id"), nullable=False)
    row: Mapped[int] = mapped_column(Integer, nullable=False)
    column: Mapped[str] = mapped_column(String(2), nullable=False)
    seat_class: Mapped[SeatClass] = mapped_column(Enum(SeatClass), default=SeatClass.ECONOMY, nullable=False)
    is_available: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)

    flight = relationship("Flight", back_populates="seats")
