from tests.conftest import auth_header


def test_get_loyalty(client, customer_token):
    resp = client.get("/api/loyalty/", headers=auth_header(customer_token))
    assert resp.status_code == 200
    data = resp.json()
    assert data["balance"] == 500
    assert data["earned"] == 500
    assert data["redeemed"] == 0


def test_loyalty_increases_on_booking(client, customer_token):
    initial = client.get("/api/loyalty/", headers=auth_header(customer_token)).json()
    flights = client.get("/api/flights/").json()
    client.post("/api/bookings/", json={
        "flight_id": flights[0]["id"],
        "passengers": [{"name": "A", "email": "a@b.com"}],
    }, headers=auth_header(customer_token))
    updated = client.get("/api/loyalty/", headers=auth_header(customer_token)).json()
    assert updated["earned"] > initial["earned"]
    assert updated["balance"] > initial["balance"]


def test_redeem_points(client, customer_token):
    resp = client.post("/api/loyalty/redeem", json={
        "points": 100,
    }, headers=auth_header(customer_token))
    assert resp.status_code == 200
    data = resp.json()
    assert data["redeemed"] == 100
    assert data["balance"] == 400


def test_redeem_too_many_points(client, customer_token):
    resp = client.post("/api/loyalty/redeem", json={
        "points": 9999,
    }, headers=auth_header(customer_token))
    assert resp.status_code == 400
    assert "Insufficient" in resp.json()["detail"]


def test_redeem_zero_points(client, customer_token):
    resp = client.post("/api/loyalty/redeem", json={
        "points": 0,
    }, headers=auth_header(customer_token))
    assert resp.status_code == 400


def test_redeem_negative_points(client, customer_token):
    resp = client.post("/api/loyalty/redeem", json={
        "points": -10,
    }, headers=auth_header(customer_token))
    assert resp.status_code == 400


def test_get_loyalty_unauthenticated(client):
    resp = client.get("/api/loyalty/")
    assert resp.status_code == 401
