import enum
from datetime import datetime

from sqlalchemy import DateTime, Enum, Float, ForeignKey, Integer, String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base


class FlightStatus(str, enum.Enum):
    SCHEDULED = "scheduled"
    DELAYED = "delayed"
    CANCELLED = "cancelled"
    COMPLETED = "completed"


class Flight(Base):
    __tablename__ = "flights"

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    flight_number: Mapped[str] = mapped_column(String(20), unique=True, index=True, nullable=False)
    departure_airport_id: Mapped[int] = mapped_column(ForeignKey("airports.id"), nullable=False)
    arrival_airport_id: Mapped[int] = mapped_column(ForeignKey("airports.id"), nullable=False)
    departure_time: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    arrival_time: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    price: Mapped[float] = mapped_column(Float, nullable=False)
    total_seats: Mapped[int] = mapped_column(Integer, nullable=False)
    status: Mapped[FlightStatus] = mapped_column(Enum(FlightStatus), default=FlightStatus.SCHEDULED, nullable=False)

    departure_airport = relationship("Airport", back_populates="departing_flights", foreign_keys=[departure_airport_id])
    arrival_airport = relationship("Airport", back_populates="arriving_flights", foreign_keys=[arrival_airport_id])
    bookings = relationship("Booking", back_populates="flight", cascade="all, delete-orphan")
    seats = relationship("Seat", back_populates="flight", cascade="all, delete-orphan")
