from tests.conftest import auth_header


def test_list_flights(client):
    resp = client.get("/api/flights/")
    assert resp.status_code == 200
    data = resp.json()
    assert len(data) == 15
    assert "departure_airport" in data[0]
    assert "arrival_airport" in data[0]
    assert "available_seats" in data[0]


def test_get_flight_by_id(client):
    flights = client.get("/api/flights/").json()
    flight_id = flights[0]["id"]
    resp = client.get(f"/api/flights/{flight_id}")
    assert resp.status_code == 200
    data = resp.json()
    assert data["id"] == flight_id
    assert data["departure_airport"] is not None


def test_get_flight_not_found(client):
    resp = client.get("/api/flights/9999")
    assert resp.status_code == 404


def test_search_flights_by_origin(client):
    resp = client.get("/api/flights/search", params={"origin": "JFK"})
    assert resp.status_code == 200
    data = resp.json()
    assert len(data) >= 1
    for f in data:
        assert f["departure_airport"]["code"] == "JFK"


def test_search_flights_by_price_range(client):
    resp = client.get("/api/flights/search", params={"min_price": 500, "max_price": 700})
    assert resp.status_code == 200
    data = resp.json()
    for f in data:
        assert 500 <= f["price"] <= 700


def test_search_flights_no_results(client):
    resp = client.get("/api/flights/search", params={"origin": "XXX"})
    assert resp.status_code == 200
    assert resp.json() == []


def test_create_flight_admin(client, admin_token):
    airports = client.get("/api/airports/").json()
    dep_id = airports[0]["id"]
    arr_id = airports[1]["id"]
    resp = client.post("/api/flights/", json={
        "flight_number": "FB999",
        "departure_airport_id": dep_id,
        "arrival_airport_id": arr_id,
        "departure_time": "2026-06-01T10:00:00",
        "arrival_time": "2026-06-01T15:00:00",
        "price": 450.0,
        "total_seats": 120,
    }, headers=auth_header(admin_token))
    assert resp.status_code == 201
    data = resp.json()
    assert data["flight_number"] == "FB999"
    assert data["price"] == 450.0
    assert data["available_seats"] == 120


def test_create_flight_customer_forbidden(client, customer_token):
    resp = client.post("/api/flights/", json={
        "flight_number": "FB998",
        "departure_airport_id": 1,
        "arrival_airport_id": 2,
        "departure_time": "2026-06-01T10:00:00",
        "arrival_time": "2026-06-01T15:00:00",
        "price": 450.0,
        "total_seats": 120,
    }, headers=auth_header(customer_token))
    assert resp.status_code == 403


def test_update_flight_admin(client, admin_token):
    flights = client.get("/api/flights/").json()
    flight_id = flights[0]["id"]
    resp = client.put(f"/api/flights/{flight_id}", json={
        "price": 999.99,
    }, headers=auth_header(admin_token))
    assert resp.status_code == 200
    assert resp.json()["price"] == 999.99


def test_delete_flight_admin(client, admin_token):
    airports = client.get("/api/airports/").json()
    create_resp = client.post("/api/flights/", json={
        "flight_number": "FBDEL",
        "departure_airport_id": airports[0]["id"],
        "arrival_airport_id": airports[1]["id"],
        "departure_time": "2026-07-01T10:00:00",
        "arrival_time": "2026-07-01T15:00:00",
        "price": 100.0,
        "total_seats": 60,
    }, headers=auth_header(admin_token))
    flight_id = create_resp.json()["id"]
    resp = client.delete(f"/api/flights/{flight_id}", headers=auth_header(admin_token))
    assert resp.status_code == 204
    resp = client.get(f"/api/flights/{flight_id}")
    assert resp.status_code == 404
