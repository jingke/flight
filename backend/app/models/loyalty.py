from sqlalchemy import ForeignKey, Integer
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base


class LoyaltyPoints(Base):
    __tablename__ = "loyalty_points"

    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), unique=True, nullable=False)
    earned: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    redeemed: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    balance: Mapped[int] = mapped_column(Integer, default=0, nullable=False)

    user = relationship("User", back_populates="loyalty_points")
