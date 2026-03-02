from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    APP_NAME: str = "Flight Booking System"
    DEBUG: bool = True
    DATABASE_URL: str = "sqlite:///./flight_booking.db"
    SECRET_KEY: str = "change-me-in-production-use-a-real-secret"
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60

    class Config:
        env_file = ".env"


settings = Settings()
