# Flight Booking System - Design Document

## Overview

A full-stack flight booking management system with user authentication, flight search, seat selection, and booking management capabilities.

## Tech Stack

### Backend
- **Framework**: FastAPI (Python)
- **Database**: SQLite with SQLAlchemy ORM
- **Authentication**: JWT (JSON Web Tokens)
- **Password Hashing**: bcrypt

### Frontend
- **Framework**: React 18 with TypeScript
- **Build Tool**: Vite
- **Routing**: React Router DOM
- **State Management**: Zustand
- **Styling**: Tailwind CSS
- **UI Components**: Headless UI
- **Maps**: Leaflet with React-Leaflet
- **Notifications**: React Hot Toast

## Architecture

### Backend Structure
```
backend/
├── app/
│   ├── database.py          # Database configuration
│   ├── dependencies.py      # Dependency injection
│   ├── main.py              # Application entry point
│   ├── models/              # SQLAlchemy models
│   │   ├── airport.py
│   │   ├── booking.py
│   │   ├── flight.py
│   │   ├── loyalty.py
│   │   ├── notification.py
│   │   ├── passenger.py
│   │   ├── saved_passenger.py
│   │   ├── seat.py
│   │   └── user.py
│   ├── routers/             # API route handlers
│   │   ├── airports.py
│   │   ├── auth.py
│   │   ├── bookings.py      # Booking management with duplicate validation
│   │   ├── flights.py
│   │   ├── loyalty.py
│   │   ├── notifications.py
│   │   ├── passengers.py    # Saved passenger CRUD
│   │   └── seats.py
│   ├── schemas/             # Pydantic schemas
│   └── services/            # Business logic
│       └── auth.py          # Authentication service
```

### Frontend Structure
```
frontend/src/
├── components/
│   └── ui/                  # Reusable UI components
├── pages/
│   ├── customer/            # Customer-facing pages
│   │   ├── BookingsPage.tsx
│   │   ├── FlightDetailPage.tsx    # Booking with passenger selection
│   │   ├── FlightsPage.tsx
│   │   ├── LoyaltyPage.tsx
│   │   ├── NotificationsPage.tsx
│   │   ├── PassengersPage.tsx      # Manage saved passengers
│   │   └── RouteMapPage.tsx        # Interactive map with fallback
│   └── admin/               # Admin pages
├── services/                # API service layer
│   ├── api.ts               # Axios instance with interceptors
│   ├── auth.service.ts
│   ├── booking.service.ts
│   ├── passenger.service.ts
│   └── ...
├── stores/                  # Zustand stores
│   └── auth.store.ts
└── types/                   # TypeScript type definitions
```

## Key Features

### 1. User Authentication
- JWT-based authentication
- Role-based access control (Customer/Admin)
- Password hashing with bcrypt
- Token expiration handling

### 2. Flight Search & Booking
- Search flights by airports and dates
- Real-time seat availability
- Interactive seat selection map
- Price calculation per passenger

### 3. Passenger Management
- Save frequently used passengers
- Select passengers from dropdown list
- Prevent duplicate bookings for same passenger on same flight
- Display booking error with seat information

### 4. Booking System
- Create bookings with multiple passengers
- Automatic seat assignment tracking
- Duplicate booking validation
- Booking confirmation with modal popup

### 5. Route Map Visualization
- Interactive Leaflet map
- Airport markers with popups
- Flight route polylines
- Automatic tile source fallback (OpenStreetMap → CartoDB)

## Design Patterns

### Backend Patterns

#### Repository Pattern
Models encapsulate database operations through SQLAlchemy ORM.

```python
# Example: Booking creation with validation
@router.post("/", response_model=BookingDetail)
def create_booking(data: BookingCreate, ...):
    # Check for existing bookings
    existing_passengers = get_existing_passengers(db, current_user.id, data.flight_id)
    
    # Validate no duplicates
    for passenger in data.passengers:
        if passenger.email.lower() in existing_passengers:
            raise HTTPException(
                status_code=400,
                detail=f"Passenger already booked (Seat: {existing_passengers[email]})")
    
    # Create booking
    ...
```

#### Dependency Injection
FastAPI dependencies for database sessions and authentication.

```python
async def get_current_user(token: str = Depends(oauth2_scheme)):
    # JWT validation logic
    ...
```

### Frontend Patterns

#### Component Composition
Pages composed of smaller, reusable components.

```tsx
// FlightDetailPage structure
<ErrorModal />
<FlightInfoCard />
<SeatMap />
<PassengerForm>
  <PassengerSelector />
  <PassengerDetails />
  <SeatAssignment />
</PassengerForm>
<BookingSummary />
```

#### Custom Hooks Pattern
Service layer functions as reusable data fetching logic.

```tsx
// Service function pattern
export async function createBooking(payload: BookingCreate): Promise<BookingDetail> {
  const { data } = await api.post<BookingDetail>('/bookings/', payload);
  return data;
}
```

#### State Management
Zustand for global state (auth), React useState for local state.

```tsx
// Auth store
const useAuthStore = create<AuthState>((set) => ({
  isAuthenticated: false,
  user: null,
  login: (user) => set({ isAuthenticated: true, user }),
  logout: () => set({ isAuthenticated: false, user: null }),
}));
```

## API Design

### Authentication Endpoints
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login
- `POST /api/auth/logout` - User logout

### Flight Endpoints
- `GET /api/flights` - List/search flights
- `GET /api/flights/{id}` - Get flight details
- `GET /api/flights/{id}/seats` - Get seat map

### Booking Endpoints
- `GET /api/bookings` - List user bookings
- `POST /api/bookings` - Create booking (with duplicate validation)
- `GET /api/bookings/{id}` - Get booking details
- `POST /api/bookings/{id}/cancel` - Cancel booking

### Passenger Endpoints
- `GET /api/passengers` - List saved passengers
- `POST /api/passengers` - Create saved passenger
- `PUT /api/passengers/{id}` - Update saved passenger
- `DELETE /api/passengers/{id}` - Delete saved passenger

## Database Schema

### Core Tables
- **users** - User accounts and authentication
- **flights** - Flight information
- **airports** - Airport data
- **bookings** - Booking records
- **passengers** - Passenger details per booking
- **saved_passengers** - User's saved passenger profiles
- **seats** - Seat inventory per flight
- **loyalty_points** - User loyalty program data
- **notifications** - User notifications

### Relationships
```
User 1:N Bookings
User 1:N SavedPassengers
User 1:1 LoyaltyPoints
User 1:N Notifications
Flight 1:N Bookings
Flight 1:N Seats
Flight N:2 Airports (departure/arrival)
Booking 1:N Passengers
```

## Error Handling

### Backend
- HTTPException with appropriate status codes
- Detailed error messages for client display
- Validation errors return 400 Bad Request

### Frontend
- API interceptors for global error handling
- Modal popup for booking errors with context
- Toast notifications for success/feedback
- Graceful degradation (e.g., map tile fallback)

## Security Considerations

1. **Authentication**: JWT tokens with expiration
2. **Authorization**: Role-based access control
3. **Password Security**: bcrypt hashing
4. **Input Validation**: Pydantic schemas
5. **SQL Injection Prevention**: SQLAlchemy ORM
6. **CORS**: Configured for frontend origin

## Performance Optimizations

1. **Database**: Indexed queries on frequently accessed columns
2. **Frontend**: 
   - Code splitting with React.lazy
   - Memoization with useMemo/useCallback
   - Optimistic UI updates
3. **API**: Efficient query patterns, avoiding N+1 queries

## UI/UX Design Principles

1. **Responsive Design**: Mobile-first with Tailwind CSS
2. **Accessibility**: ARIA labels, keyboard navigation
3. **Feedback**: Loading states, error messages, success toasts
4. **Consistency**: Reusable component library
5. **Error Recovery**: Clear error messages with action guidance

## Recent Updates

### Booking Error Handling Enhancement
- Added modal popup for booking errors
- Display flight number and passenger seat information
- Show original booking seat in error message

### Passenger Selection Improvement
- Changed from input fields to dropdown selection
- Made passenger info read-only after selection
- Added link to passenger management page

### Map Reliability
- Implemented automatic tile source fallback
- Custom TileLayer component with error handling
- Switches to CartoDB when OpenStreetMap fails

### Duplicate Booking Prevention
- Backend validation prevents same passenger booking same flight twice
- Error message includes existing seat assignment
- Case-insensitive email matching

## Future Enhancements

1. Payment integration
2. Email notifications
3. Real-time seat availability updates
4. Mobile app
5. Multi-language support
6. Advanced search filters
