from sqlalchemy import Float, String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base


class Airport(Base):
    __tablename__ = "airports"

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    code: Mapped[str] = mapped_column(String(10), unique=True, index=True, nullable=False)
    name: Mapped[str] = mapped_column(String(255), nullable=False)
    city: Mapped[str] = mapped_column(String(255), nullable=False)
    country: Mapped[str] = mapped_column(String(255), nullable=False)
    latitude: Mapped[float] = mapped_column(Float, nullable=False)
    longitude: Mapped[float] = mapped_column(Float, nullable=False)

    departing_flights = relationship("Flight", back_populates="departure_airport", foreign_keys="Flight.departure_airport_id")
    arriving_flights = relationship("Flight", back_populates="arrival_airport", foreign_keys="Flight.arrival_airport_id")
