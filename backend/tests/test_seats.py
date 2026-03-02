from tests.conftest import auth_header


def test_get_seat_map(client):
    flights = client.get("/api/flights/").json()
    flight_id = flights[0]["id"]
    resp = client.get(f"/api/seats/flight/{flight_id}")
    assert resp.status_code == 200
    data = resp.json()
    assert len(data) > 0
    seat = data[0]
    assert "row" in seat
    assert "column" in seat
    assert "seat_class" in seat
    assert "is_available" in seat


def test_get_seat_map_invalid_flight(client):
    resp = client.get("/api/seats/flight/9999")
    assert resp.status_code == 404


def test_seat_map_has_classes(client):
    flights = client.get("/api/flights/").json()
    flight_id = flights[0]["id"]
    seats = client.get(f"/api/seats/flight/{flight_id}").json()
    classes = {s["seat_class"] for s in seats}
    assert "economy" in classes


def test_assign_seat(client, customer_token):
    flights = client.get("/api/flights/").json()
    flight = flights[0]
    create_resp = client.post("/api/bookings/", json={
        "flight_id": flight["id"],
        "passengers": [{"name": "Test", "email": "test@t.com"}],
    }, headers=auth_header(customer_token))
    passenger_id = create_resp.json()["passengers"][0]["id"]
    seats = client.get(f"/api/seats/flight/{flight['id']}").json()
    available_seat = next(s for s in seats if s["is_available"])
    resp = client.post("/api/seats/assign", json={
        "passenger_id": passenger_id,
        "seat_id": available_seat["id"],
    }, headers=auth_header(customer_token))
    assert resp.status_code == 200
    assert resp.json()["is_available"] is False


def test_assign_seat_unavailable(client, customer_token):
    flights = client.get("/api/flights/").json()
    flight = flights[0]
    create_resp = client.post("/api/bookings/", json={
        "flight_id": flight["id"],
        "passengers": [
            {"name": "P1", "email": "p1@t.com"},
            {"name": "P2", "email": "p2@t.com"},
        ],
    }, headers=auth_header(customer_token))
    passengers = create_resp.json()["passengers"]
    seats = client.get(f"/api/seats/flight/{flight['id']}").json()
    seat = next(s for s in seats if s["is_available"])
    client.post("/api/seats/assign", json={
        "passenger_id": passengers[0]["id"],
        "seat_id": seat["id"],
    }, headers=auth_header(customer_token))
    resp = client.post("/api/seats/assign", json={
        "passenger_id": passengers[1]["id"],
        "seat_id": seat["id"],
    }, headers=auth_header(customer_token))
    assert resp.status_code == 400


def test_assign_seat_not_found(client, customer_token):
    resp = client.post("/api/seats/assign", json={
        "passenger_id": 1,
        "seat_id": 99999,
    }, headers=auth_header(customer_token))
    assert resp.status_code == 404
