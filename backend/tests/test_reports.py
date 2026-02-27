from tests.conftest import auth_header


def _seed_booking(client, customer_token):
    flights = client.get("/api/flights/").json()
    client.post("/api/bookings/", json={
        "flight_id": flights[0]["id"],
        "passengers": [{"name": "R", "email": "r@t.com"}],
    }, headers=auth_header(customer_token))


def test_bookings_per_flight_admin(client, admin_token, customer_token):
    _seed_booking(client, customer_token)
    resp = client.get("/api/reports/bookings-per-flight", headers=auth_header(admin_token))
    assert resp.status_code == 200
    data = resp.json()
    assert len(data) >= 1
    assert "flight_number" in data[0]
    assert "booking_count" in data[0]
    assert "departure" in data[0]
    assert "arrival" in data[0]


def test_bookings_per_flight_customer_forbidden(client, customer_token):
    resp = client.get("/api/reports/bookings-per-flight", headers=auth_header(customer_token))
    assert resp.status_code == 403


def test_popular_routes_admin(client, admin_token, customer_token):
    _seed_booking(client, customer_token)
    resp = client.get("/api/reports/popular-routes", headers=auth_header(admin_token))
    assert resp.status_code == 200
    data = resp.json()
    assert len(data) >= 1
    assert "origin_code" in data[0]
    assert "destination_code" in data[0]
    assert "booking_count" in data[0]


def test_popular_routes_customer_forbidden(client, customer_token):
    resp = client.get("/api/reports/popular-routes", headers=auth_header(customer_token))
    assert resp.status_code == 403


def test_peak_times_admin(client, admin_token, customer_token):
    _seed_booking(client, customer_token)
    resp = client.get("/api/reports/peak-times", headers=auth_header(admin_token))
    assert resp.status_code == 200
    data = resp.json()
    assert len(data) >= 1
    assert "hour" in data[0]
    assert "booking_count" in data[0]


def test_peak_times_customer_forbidden(client, customer_token):
    resp = client.get("/api/reports/peak-times", headers=auth_header(customer_token))
    assert resp.status_code == 403


def test_reports_unauthenticated(client):
    assert client.get("/api/reports/bookings-per-flight").status_code == 401
    assert client.get("/api/reports/popular-routes").status_code == 401
    assert client.get("/api/reports/peak-times").status_code == 401
