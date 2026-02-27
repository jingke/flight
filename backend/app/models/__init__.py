from app.models.user import User
from app.models.airport import Airport
from app.models.flight import Flight
from app.models.booking import Booking
from app.models.passenger import Passenger
from app.models.seat import Seat
from app.models.complaint import Complaint
from app.models.modification_request import ModificationRequest
from app.models.notification import Notification
from app.models.loyalty import LoyaltyPoints

__all__ = [
    "User",
    "Airport",
    "Flight",
    "Booking",
    "Passenger",
    "Seat",
    "Complaint",
    "ModificationRequest",
    "Notification",
    "LoyaltyPoints",
]
