from tests.conftest import auth_header


def _create_booking_and_mod(client, customer_token):
    flights = client.get("/api/flights/").json()
    booking_resp = client.post("/api/bookings/", json={
        "flight_id": flights[0]["id"],
        "passengers": [{"name": "A", "email": "a@b.com"}],
    }, headers=auth_header(customer_token))
    booking_id = booking_resp.json()["id"]
    mod_resp = client.post("/api/modifications/", json={
        "booking_id": booking_id,
        "type": "date_change",
        "details": "Please change to next week.",
    }, headers=auth_header(customer_token))
    return booking_id, mod_resp


def test_create_modification(client, customer_token):
    _, resp = _create_booking_and_mod(client, customer_token)
    assert resp.status_code == 201
    data = resp.json()
    assert data["type"] == "date_change"
    assert data["status"] == "pending"
    assert data["details"] == "Please change to next week."


def test_create_modification_invalid_booking(client, customer_token):
    resp = client.post("/api/modifications/", json={
        "booking_id": 9999,
        "type": "date_change",
        "details": "test",
    }, headers=auth_header(customer_token))
    assert resp.status_code == 404


def test_list_modifications_customer(client, customer_token):
    _create_booking_and_mod(client, customer_token)
    resp = client.get("/api/modifications/", headers=auth_header(customer_token))
    assert resp.status_code == 200
    assert len(resp.json()) >= 1


def test_list_modifications_admin(client, admin_token, customer_token):
    _create_booking_and_mod(client, customer_token)
    resp = client.get("/api/modifications/", headers=auth_header(admin_token))
    assert resp.status_code == 200
    assert len(resp.json()) >= 1


def test_approve_modification(client, admin_token, customer_token):
    _, mod_resp = _create_booking_and_mod(client, customer_token)
    mod_id = mod_resp.json()["id"]
    resp = client.put(f"/api/modifications/{mod_id}", json={
        "status": "approved",
    }, headers=auth_header(admin_token))
    assert resp.status_code == 200
    assert resp.json()["status"] == "approved"


def test_reject_modification(client, admin_token, customer_token):
    _, mod_resp = _create_booking_and_mod(client, customer_token)
    mod_id = mod_resp.json()["id"]
    resp = client.put(f"/api/modifications/{mod_id}", json={
        "status": "rejected",
    }, headers=auth_header(admin_token))
    assert resp.status_code == 200
    assert resp.json()["status"] == "rejected"


def test_update_modification_customer_forbidden(client, customer_token):
    _, mod_resp = _create_booking_and_mod(client, customer_token)
    mod_id = mod_resp.json()["id"]
    resp = client.put(f"/api/modifications/{mod_id}", json={
        "status": "approved",
    }, headers=auth_header(customer_token))
    assert resp.status_code == 403


def test_update_modification_not_found(client, admin_token):
    resp = client.put("/api/modifications/9999", json={
        "status": "approved",
    }, headers=auth_header(admin_token))
    assert resp.status_code == 404
