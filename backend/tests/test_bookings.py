from tests.conftest import auth_header


def _get_first_flight(client):
    return client.get("/api/flights/").json()[0]


def _create_booking(client, token, flight_id, passengers=None):
    if passengers is None:
        passengers = [{"name": "John Doe", "email": "john@test.com"}]
    return client.post("/api/bookings/", json={
        "flight_id": flight_id,
        "passengers": passengers,
    }, headers=auth_header(token))


def test_create_booking(client, customer_token):
    flight = _get_first_flight(client)
    resp = _create_booking(client, customer_token, flight["id"])
    assert resp.status_code == 201
    data = resp.json()
    assert data["status"] == "confirmed"
    assert data["payment_status"] == "paid"
    assert data["total_price"] == flight["price"]
    assert len(data["passengers"]) == 1
    assert data["passengers"][0]["name"] == "John Doe"
    assert data["flight_number"] is not None


def test_create_booking_multiple_passengers(client, customer_token):
    flight = _get_first_flight(client)
    passengers = [
        {"name": "Alice", "email": "alice@test.com"},
        {"name": "Bob", "email": "bob@test.com"},
    ]
    resp = _create_booking(client, customer_token, flight["id"], passengers)
    assert resp.status_code == 201
    data = resp.json()
    assert data["total_price"] == flight["price"] * 2
    assert len(data["passengers"]) == 2


def test_create_booking_with_seat(client, customer_token):
    flight = _get_first_flight(client)
    seats = client.get(f"/api/seats/flight/{flight['id']}").json()
    available_seat = next(s for s in seats if s["is_available"])
    resp = _create_booking(client, customer_token, flight["id"], [
        {"name": "Seated Passenger", "email": "seated@test.com", "seat_id": available_seat["id"]}
    ])
    assert resp.status_code == 201
    assert resp.json()["passengers"][0]["seat_assignment"] is not None


def test_create_booking_no_passengers(client, customer_token):
    flight = _get_first_flight(client)
    resp = _create_booking(client, customer_token, flight["id"], [])
    assert resp.status_code == 400


def test_create_booking_invalid_flight(client, customer_token):
    resp = _create_booking(client, customer_token, 9999)
    assert resp.status_code == 404


def test_create_booking_unauthenticated(client):
    resp = client.post("/api/bookings/", json={
        "flight_id": 1,
        "passengers": [{"name": "A", "email": "a@b.com"}],
    })
    assert resp.status_code == 401


def test_list_bookings_customer(client, customer_token):
    flight = _get_first_flight(client)
    _create_booking(client, customer_token, flight["id"])
    resp = client.get("/api/bookings/", headers=auth_header(customer_token))
    assert resp.status_code == 200
    data = resp.json()
    assert len(data) >= 1


def test_list_bookings_admin_sees_all(client, admin_token, customer_token):
    flight = _get_first_flight(client)
    _create_booking(client, customer_token, flight["id"])
    resp = client.get("/api/bookings/", headers=auth_header(admin_token))
    assert resp.status_code == 200
    assert len(resp.json()) >= 1


def test_get_booking_detail(client, customer_token):
    flight = _get_first_flight(client)
    create_resp = _create_booking(client, customer_token, flight["id"])
    booking_id = create_resp.json()["id"]
    resp = client.get(f"/api/bookings/{booking_id}", headers=auth_header(customer_token))
    assert resp.status_code == 200
    data = resp.json()
    assert data["id"] == booking_id
    assert data["departure_airport"] is not None


def test_get_booking_not_found(client, customer_token):
    resp = client.get("/api/bookings/9999", headers=auth_header(customer_token))
    assert resp.status_code == 404


def test_cancel_booking(client, customer_token):
    flight = _get_first_flight(client)
    create_resp = _create_booking(client, customer_token, flight["id"])
    booking_id = create_resp.json()["id"]
    resp = client.post(f"/api/bookings/{booking_id}/cancel", headers=auth_header(customer_token))
    assert resp.status_code == 200
    data = resp.json()
    assert data["status"] == "cancelled"
    assert data["payment_status"] == "refunded"


def test_cancel_booking_already_cancelled(client, customer_token):
    flight = _get_first_flight(client)
    create_resp = _create_booking(client, customer_token, flight["id"])
    booking_id = create_resp.json()["id"]
    client.post(f"/api/bookings/{booking_id}/cancel", headers=auth_header(customer_token))
    resp = client.post(f"/api/bookings/{booking_id}/cancel", headers=auth_header(customer_token))
    assert resp.status_code == 400


def test_cancel_booking_frees_seats(client, customer_token):
    flight = _get_first_flight(client)
    seats = client.get(f"/api/seats/flight/{flight['id']}").json()
    seat = next(s for s in seats if s["is_available"])
    create_resp = _create_booking(client, customer_token, flight["id"], [
        {"name": "Test", "email": "test@test.com", "seat_id": seat["id"]}
    ])
    booking_id = create_resp.json()["id"]
    updated_seats = client.get(f"/api/seats/flight/{flight['id']}").json()
    booked_seat = next(s for s in updated_seats if s["id"] == seat["id"])
    assert not booked_seat["is_available"]
    client.post(f"/api/bookings/{booking_id}/cancel", headers=auth_header(customer_token))
    final_seats = client.get(f"/api/seats/flight/{flight['id']}").json()
    freed_seat = next(s for s in final_seats if s["id"] == seat["id"])
    assert freed_seat["is_available"]
