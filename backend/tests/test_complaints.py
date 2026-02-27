from tests.conftest import auth_header


def _create_complaint(client, token, booking_id=None):
    return client.post("/api/complaints/", json={
        "booking_id": booking_id,
        "subject": "Test complaint",
        "description": "This is a test complaint description.",
    }, headers=auth_header(token))


def test_create_complaint(client, customer_token):
    resp = _create_complaint(client, customer_token)
    assert resp.status_code == 201
    data = resp.json()
    assert data["subject"] == "Test complaint"
    assert data["status"] == "open"
    assert data["admin_response"] is None


def test_create_complaint_with_booking(client, customer_token):
    flights = client.get("/api/flights/").json()
    booking_resp = client.post("/api/bookings/", json={
        "flight_id": flights[0]["id"],
        "passengers": [{"name": "A", "email": "a@b.com"}],
    }, headers=auth_header(customer_token))
    booking_id = booking_resp.json()["id"]
    resp = _create_complaint(client, customer_token, booking_id)
    assert resp.status_code == 201
    assert resp.json()["booking_id"] == booking_id


def test_list_complaints_customer(client, customer_token):
    _create_complaint(client, customer_token)
    resp = client.get("/api/complaints/", headers=auth_header(customer_token))
    assert resp.status_code == 200
    assert len(resp.json()) >= 1


def test_list_complaints_admin_sees_all(client, admin_token, customer_token):
    _create_complaint(client, customer_token)
    resp = client.get("/api/complaints/", headers=auth_header(admin_token))
    assert resp.status_code == 200
    assert len(resp.json()) >= 1


def test_get_complaint(client, customer_token):
    create_resp = _create_complaint(client, customer_token)
    complaint_id = create_resp.json()["id"]
    resp = client.get(f"/api/complaints/{complaint_id}", headers=auth_header(customer_token))
    assert resp.status_code == 200
    assert resp.json()["id"] == complaint_id


def test_get_complaint_not_found(client, customer_token):
    resp = client.get("/api/complaints/9999", headers=auth_header(customer_token))
    assert resp.status_code == 404


def test_update_complaint_admin(client, admin_token, customer_token):
    create_resp = _create_complaint(client, customer_token)
    complaint_id = create_resp.json()["id"]
    resp = client.put(f"/api/complaints/{complaint_id}", json={
        "status": "resolved",
        "admin_response": "Issue has been resolved.",
    }, headers=auth_header(admin_token))
    assert resp.status_code == 200
    data = resp.json()
    assert data["status"] == "resolved"
    assert data["admin_response"] == "Issue has been resolved."


def test_update_complaint_customer_forbidden(client, customer_token):
    create_resp = _create_complaint(client, customer_token)
    complaint_id = create_resp.json()["id"]
    resp = client.put(f"/api/complaints/{complaint_id}", json={
        "status": "resolved",
    }, headers=auth_header(customer_token))
    assert resp.status_code == 403
