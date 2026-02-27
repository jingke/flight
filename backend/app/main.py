from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.database import Base, SessionLocal, engine
from app.models import *  # noqa: F401, F403 – ensure all models are registered
from app.routers import (
    airports,
    auth,
    bookings,
    complaints,
    flights,
    loyalty,
    modifications,
    notifications,
    reports,
    seats,
)
from app.seed import seed_database


@asynccontextmanager
async def lifespan(_app: FastAPI):
    Base.metadata.create_all(bind=engine)
    db = SessionLocal()
    try:
        seed_database(db)
    finally:
        db.close()
    yield


app = FastAPI(
    title=settings.APP_NAME,
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth.router)
app.include_router(airports.router)
app.include_router(flights.router)
app.include_router(bookings.router)
app.include_router(seats.router)
app.include_router(complaints.router)
app.include_router(modifications.router)
app.include_router(notifications.router)
app.include_router(loyalty.router)
app.include_router(reports.router)


@app.get("/health")
async def health_check():
    return {"status": "ok", "app": settings.APP_NAME}
