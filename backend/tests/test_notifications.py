from tests.conftest import auth_header


def _trigger_notification(client, customer_token):
    """Creating a booking triggers a notification."""
    flights = client.get("/api/flights/").json()
    client.post("/api/bookings/", json={
        "flight_id": flights[0]["id"],
        "passengers": [{"name": "A", "email": "a@b.com"}],
    }, headers=auth_header(customer_token))


def test_list_notifications(client, customer_token):
    _trigger_notification(client, customer_token)
    resp = client.get("/api/notifications/", headers=auth_header(customer_token))
    assert resp.status_code == 200
    data = resp.json()
    assert len(data) >= 1
    assert data[0]["title"] == "Booking Confirmed"
    assert data[0]["is_read"] is False


def test_mark_notification_read(client, customer_token):
    _trigger_notification(client, customer_token)
    notifs = client.get("/api/notifications/", headers=auth_header(customer_token)).json()
    notif_id = notifs[0]["id"]
    resp = client.put(f"/api/notifications/{notif_id}/read", headers=auth_header(customer_token))
    assert resp.status_code == 200
    assert resp.json()["is_read"] is True


def test_mark_notification_not_found(client, customer_token):
    resp = client.put("/api/notifications/9999/read", headers=auth_header(customer_token))
    assert resp.status_code == 404


def test_notifications_isolated_per_user(client, customer_token, admin_token):
    _trigger_notification(client, customer_token)
    admin_notifs = client.get("/api/notifications/", headers=auth_header(admin_token)).json()
    customer_notifs = client.get("/api/notifications/", headers=auth_header(customer_token)).json()
    admin_ids = {n["id"] for n in admin_notifs}
    customer_ids = {n["id"] for n in customer_notifs}
    assert admin_ids.isdisjoint(customer_ids)
