"""
End-to-end integration tests that exercise complete user journeys
across multiple API endpoints, validating that the web and Android
clients can perform full workflows against the backend.
"""
import pytest
from tests.conftest import auth_header, login_as


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _register_user(client, email, name="Test User", password="testpass123"):
    return client.post("/api/auth/register", json={
        "email": email,
        "password": password,
        "name": name,
    })


def _login(client, email, password="testpass123"):
    resp = client.post("/api/auth/login", json={
        "email": email,
        "password": password,
    })
    assert resp.status_code == 200
    return resp.json()["access_token"]


def _first_flight(client):
    flights = client.get("/api/flights/").json()
    assert len(flights) > 0
    return flights[0]


def _available_seat(client, flight_id):
    seats = client.get(f"/api/seats/flight/{flight_id}").json()
    return next(s for s in seats if s["is_available"])


# ---------------------------------------------------------------------------
# 1. Customer Booking Journey (mirrors web & Android flows)
#    Register → Login → Search → Detail → Seat map → Book → View → Cancel
# ---------------------------------------------------------------------------

class TestCustomerBookingJourney:
    """Full customer journey from registration through booking cancellation."""

    def test_complete_booking_lifecycle(self, client):
        # --- Register ---
        reg = _register_user(client, "journey@test.com", "Journey User")
        assert reg.status_code == 201
        user = reg.json()
        assert user["email"] == "journey@test.com"
        assert user["role"] == "customer"

        # --- Login ---
        token = _login(client, "journey@test.com")
        headers = auth_header(token)

        # --- Verify identity ---
        me = client.get("/api/auth/me", headers=headers)
        assert me.status_code == 200
        assert me.json()["email"] == "journey@test.com"

        # --- Browse airports ---
        airports = client.get("/api/airports/").json()
        assert len(airports) >= 2
        jfk = next(a for a in airports if a["code"] == "JFK")
        lax = next(a for a in airports if a["code"] == "LAX")

        # --- Search flights ---
        search = client.get("/api/flights/search", params={"origin": "JFK"})
        assert search.status_code == 200
        jfk_flights = search.json()
        assert len(jfk_flights) > 0
        assert all(
            f["departure_airport"]["code"] == "JFK" for f in jfk_flights
        )

        flight = jfk_flights[0]
        flight_id = flight["id"]

        # --- Flight detail ---
        detail = client.get(f"/api/flights/{flight_id}")
        assert detail.status_code == 200
        assert detail.json()["flight_number"] == flight["flight_number"]

        # --- Seat map ---
        seats = client.get(f"/api/seats/flight/{flight_id}").json()
        assert len(seats) > 0
        avail_seat = next(s for s in seats if s["is_available"])

        # --- Create booking with seat ---
        booking_resp = client.post("/api/bookings/", json={
            "flight_id": flight_id,
            "passengers": [
                {"name": "Journey User", "email": "journey@test.com", "seat_id": avail_seat["id"]},
            ],
        }, headers=headers)
        assert booking_resp.status_code == 201
        booking = booking_resp.json()
        booking_id = booking["id"]
        assert booking["status"] == "confirmed"
        assert booking["payment_status"] == "paid"
        assert booking["passengers"][0]["seat_assignment"] is not None

        # --- Verify seat now taken ---
        seats_after = client.get(f"/api/seats/flight/{flight_id}").json()
        booked_seat = next(s for s in seats_after if s["id"] == avail_seat["id"])
        assert booked_seat["is_available"] is False

        # --- View booking ---
        detail_resp = client.get(f"/api/bookings/{booking_id}", headers=headers)
        assert detail_resp.status_code == 200
        assert detail_resp.json()["flight_number"] is not None

        # --- List bookings ---
        bookings_list = client.get("/api/bookings/", headers=headers).json()
        assert any(b["id"] == booking_id for b in bookings_list)

        # --- Booking created a notification ---
        notifs = client.get("/api/notifications/", headers=headers).json()
        assert any(n["title"] == "Booking Confirmed" for n in notifs)

        # --- Cancel booking ---
        cancel = client.post(f"/api/bookings/{booking_id}/cancel", headers=headers)
        assert cancel.status_code == 200
        assert cancel.json()["status"] == "cancelled"
        assert cancel.json()["payment_status"] == "refunded"

        # --- Seat freed ---
        seats_final = client.get(f"/api/seats/flight/{flight_id}").json()
        freed = next(s for s in seats_final if s["id"] == avail_seat["id"])
        assert freed["is_available"] is True

        # --- Cancellation notification ---
        notifs_after = client.get("/api/notifications/", headers=headers).json()
        assert any(n["title"] == "Booking Cancelled" for n in notifs_after)

    def test_multi_passenger_booking(self, client, customer_token):
        flight = _first_flight(client)
        headers = auth_header(customer_token)
        seats = client.get(f"/api/seats/flight/{flight['id']}").json()
        avail = [s for s in seats if s["is_available"]][:3]
        passengers = [
            {"name": "Alice A", "email": "alice@test.com", "seat_id": avail[0]["id"]},
            {"name": "Bob B", "email": "bob@test.com", "seat_id": avail[1]["id"]},
            {"name": "Carol C", "email": "carol@test.com", "seat_id": avail[2]["id"]},
        ]
        resp = client.post("/api/bookings/", json={
            "flight_id": flight["id"],
            "passengers": passengers,
        }, headers=headers)
        assert resp.status_code == 201
        data = resp.json()
        assert data["total_price"] == flight["price"] * 3
        assert len(data["passengers"]) == 3
        assigned = [p["seat_assignment"] for p in data["passengers"]]
        assert all(a is not None for a in assigned)
        assert len(set(assigned)) == 3  # all different seats


# ---------------------------------------------------------------------------
# 2. Admin Management Journey
#    Login → Create flight → View bookings/reports → Manage complaints
# ---------------------------------------------------------------------------

class TestAdminManagementJourney:
    """Full admin workflow: flight management, reports, complaints."""

    def test_admin_creates_flight_and_views_reports(self, client, admin_token):
        headers = auth_header(admin_token)
        airports = client.get("/api/airports/").json()
        dep_id = airports[0]["id"]
        arr_id = airports[1]["id"]

        flight_resp = client.post("/api/flights/", json={
            "flight_number": "INT-001",
            "departure_airport_id": dep_id,
            "arrival_airport_id": arr_id,
            "departure_time": "2026-06-15T10:00:00",
            "arrival_time": "2026-06-15T15:00:00",
            "price": 500.00,
            "total_seats": 120,
        }, headers=headers)
        assert flight_resp.status_code == 201
        new_flight = flight_resp.json()
        assert new_flight["flight_number"] == "INT-001"
        assert new_flight["available_seats"] == 120

        seats = client.get(f"/api/seats/flight/{new_flight['id']}").json()
        assert len(seats) == 120

        updated = client.put(f"/api/flights/{new_flight['id']}", json={
            "price": 550.00,
        }, headers=headers)
        assert updated.status_code == 200
        assert updated.json()["price"] == 550.00

        bpf = client.get("/api/reports/bookings-per-flight", headers=headers)
        assert bpf.status_code == 200
        assert isinstance(bpf.json(), list)

        routes = client.get("/api/reports/popular-routes", headers=headers)
        assert routes.status_code == 200

        peaks = client.get("/api/reports/peak-times", headers=headers)
        assert peaks.status_code == 200

    def test_admin_views_all_reservations(self, client, admin_token, customer_token):
        flight = _first_flight(client)
        client.post("/api/bookings/", json={
            "flight_id": flight["id"],
            "passengers": [{"name": "Customer X", "email": "cx@test.com"}],
        }, headers=auth_header(customer_token))

        admin_bookings = client.get("/api/bookings/", headers=auth_header(admin_token))
        assert admin_bookings.status_code == 200
        assert len(admin_bookings.json()) >= 1

    def test_admin_cannot_be_created_via_register(self, client):
        resp = _register_user(client, "sneaky@admin.com")
        assert resp.status_code == 201
        assert resp.json()["role"] == "customer"


# ---------------------------------------------------------------------------
# 3. Cross-Role Interactions
#    Customer books → files complaint → admin resolves → notifications
# ---------------------------------------------------------------------------

class TestCrossRoleInteractions:
    """Customer ↔ Admin interactions across booking, complaint, and modification flows."""

    def test_complaint_lifecycle(self, client, customer_token, admin_token):
        cust_headers = auth_header(customer_token)
        admin_headers = auth_header(admin_token)

        # Customer books a flight
        flight = _first_flight(client)
        booking = client.post("/api/bookings/", json={
            "flight_id": flight["id"],
            "passengers": [{"name": "Complainer", "email": "comp@test.com"}],
        }, headers=cust_headers).json()

        # Customer submits complaint
        complaint_resp = client.post("/api/complaints/", json={
            "booking_id": booking["id"],
            "subject": "Delayed departure",
            "description": "Flight was delayed 2 hours without notice.",
        }, headers=cust_headers)
        assert complaint_resp.status_code == 201
        complaint = complaint_resp.json()
        assert complaint["status"] == "open"

        # Admin sees complaint
        admin_complaints = client.get("/api/complaints/", headers=admin_headers).json()
        assert any(c["id"] == complaint["id"] for c in admin_complaints)

        # Admin resolves complaint
        resolve = client.put(f"/api/complaints/{complaint['id']}", json={
            "status": "resolved",
            "admin_response": "We apologize. Compensation issued.",
        }, headers=admin_headers)
        assert resolve.status_code == 200
        assert resolve.json()["status"] == "resolved"
        assert resolve.json()["admin_response"] == "We apologize. Compensation issued."

        # Customer sees resolution notification
        notifs = client.get("/api/notifications/", headers=cust_headers).json()
        assert any(
            n["title"] == "Complaint Updated" for n in notifs
        )

        # Customer can view the updated complaint
        updated = client.get(f"/api/complaints/{complaint['id']}", headers=cust_headers)
        assert updated.status_code == 200
        assert updated.json()["admin_response"] is not None

    def test_modification_request_lifecycle(self, client, customer_token, admin_token):
        cust_headers = auth_header(customer_token)
        admin_headers = auth_header(admin_token)

        flight = _first_flight(client)
        booking = client.post("/api/bookings/", json={
            "flight_id": flight["id"],
            "passengers": [{"name": "Modifier", "email": "mod@test.com"}],
        }, headers=cust_headers).json()

        # Customer requests modification
        mod_resp = client.post("/api/modifications/", json={
            "booking_id": booking["id"],
            "type": "date_change",
            "details": "Move to April 20 please",
        }, headers=cust_headers)
        assert mod_resp.status_code == 201
        mod = mod_resp.json()
        assert mod["status"] == "pending"

        # Admin sees it
        admin_mods = client.get("/api/modifications/", headers=admin_headers).json()
        assert any(m["id"] == mod["id"] for m in admin_mods)

        # Admin approves
        approve = client.put(f"/api/modifications/{mod['id']}", json={
            "status": "approved",
        }, headers=admin_headers)
        assert approve.status_code == 200
        assert approve.json()["status"] == "approved"

        # Customer gets notification
        notifs = client.get("/api/notifications/", headers=cust_headers).json()
        assert any("approved" in n["message"] for n in notifs)

    def test_admin_rejects_modification(self, client, customer_token, admin_token):
        cust_headers = auth_header(customer_token)
        admin_headers = auth_header(admin_token)

        flight = _first_flight(client)
        booking = client.post("/api/bookings/", json={
            "flight_id": flight["id"],
            "passengers": [{"name": "Reject Test", "email": "reject@test.com"}],
        }, headers=cust_headers).json()

        mod = client.post("/api/modifications/", json={
            "booking_id": booking["id"],
            "type": "seat_change",
            "details": "Want first class upgrade",
        }, headers=cust_headers).json()

        reject = client.put(f"/api/modifications/{mod['id']}", json={
            "status": "rejected",
        }, headers=admin_headers)
        assert reject.status_code == 200
        assert reject.json()["status"] == "rejected"

        notifs = client.get("/api/notifications/", headers=cust_headers).json()
        assert any("rejected" in n["message"] for n in notifs)


# ---------------------------------------------------------------------------
# 4. Loyalty Points Journey
#    Book → earn points → verify → redeem → verify balance
# ---------------------------------------------------------------------------

class TestLoyaltyPointsJourney:
    """Loyalty points accumulate from bookings and can be redeemed."""

    def test_earn_and_redeem_points(self, client, customer_token):
        headers = auth_header(customer_token)

        # Check initial balance (demo user starts with 500)
        initial = client.get("/api/loyalty/", headers=headers).json()
        initial_balance = initial["balance"]
        initial_earned = initial["earned"]

        # Book a flight to earn points
        flight = _first_flight(client)
        booking = client.post("/api/bookings/", json={
            "flight_id": flight["id"],
            "passengers": [{"name": "Loyal User", "email": "loyal@test.com"}],
        }, headers=headers).json()
        expected_points = int(booking["total_price"] * 0.1)

        # Verify points increased
        after_book = client.get("/api/loyalty/", headers=headers).json()
        assert after_book["earned"] == initial_earned + expected_points
        assert after_book["balance"] == initial_balance + expected_points

        # Redeem some points
        redeem_amount = 50
        redeem = client.post("/api/loyalty/redeem", json={
            "points": redeem_amount,
        }, headers=headers)
        assert redeem.status_code == 200
        redeemed = redeem.json()
        assert redeemed["balance"] == after_book["balance"] - redeem_amount

        # Redemption notification
        notifs = client.get("/api/notifications/", headers=headers).json()
        assert any(n["title"] == "Points Redeemed" for n in notifs)

    def test_redeem_more_than_balance_fails(self, client):
        _register_user(client, "lowpoints@test.com")
        token = _login(client, "lowpoints@test.com")
        headers = auth_header(token)

        loyalty = client.get("/api/loyalty/", headers=headers).json()
        assert loyalty["balance"] == 0

        resp = client.post("/api/loyalty/redeem", json={"points": 100}, headers=headers)
        assert resp.status_code == 400

    def test_points_accumulate_across_bookings(self, client, customer_token):
        headers = auth_header(customer_token)
        initial = client.get("/api/loyalty/", headers=headers).json()

        flights = client.get("/api/flights/").json()
        total_earned = 0
        for flight in flights[:3]:
            booking = client.post("/api/bookings/", json={
                "flight_id": flight["id"],
                "passengers": [{"name": "Accumulator", "email": "accum@test.com"}],
            }, headers=headers).json()
            total_earned += int(booking["total_price"] * 0.1)

        after = client.get("/api/loyalty/", headers=headers).json()
        assert after["earned"] == initial["earned"] + total_earned


# ---------------------------------------------------------------------------
# 5. Notification Management
#    Verify notifications from various actions, mark read
# ---------------------------------------------------------------------------

class TestNotificationManagement:
    """Notifications are created by booking, cancellation, complaints, etc."""

    def test_notification_lifecycle(self, client, customer_token, admin_token):
        cust_headers = auth_header(customer_token)
        admin_headers = auth_header(admin_token)

        # Book → Booking Confirmed notification
        flight = _first_flight(client)
        booking = client.post("/api/bookings/", json={
            "flight_id": flight["id"],
            "passengers": [{"name": "Notif Test", "email": "notif@test.com"}],
        }, headers=cust_headers).json()

        # Cancel → Booking Cancelled notification
        client.post(f"/api/bookings/{booking['id']}/cancel", headers=cust_headers)

        # Complaint + admin resolve → Complaint Updated notification
        booking2 = client.post("/api/bookings/", json={
            "flight_id": flight["id"],
            "passengers": [{"name": "Notif Test", "email": "notif@test.com"}],
        }, headers=cust_headers).json()
        complaint = client.post("/api/complaints/", json={
            "booking_id": booking2["id"],
            "subject": "Test",
            "description": "Test complaint",
        }, headers=cust_headers).json()
        client.put(f"/api/complaints/{complaint['id']}", json={
            "status": "resolved",
            "admin_response": "Fixed",
        }, headers=admin_headers)

        notifs = client.get("/api/notifications/", headers=cust_headers).json()
        titles = [n["title"] for n in notifs]
        assert "Booking Confirmed" in titles
        assert "Booking Cancelled" in titles
        assert "Complaint Updated" in titles

        # Mark first notification as read
        unread = [n for n in notifs if not n["is_read"]]
        assert len(unread) > 0
        mark = client.put(
            f"/api/notifications/{unread[0]['id']}/read",
            headers=cust_headers,
        )
        assert mark.status_code == 200
        assert mark.json()["is_read"] is True

    def test_notifications_isolated_between_users(self, client):
        _register_user(client, "userA@test.com", "User A")
        _register_user(client, "userB@test.com", "User B")
        token_a = _login(client, "userA@test.com")
        token_b = _login(client, "userB@test.com")

        flight = _first_flight(client)
        client.post("/api/bookings/", json={
            "flight_id": flight["id"],
            "passengers": [{"name": "A", "email": "a@test.com"}],
        }, headers=auth_header(token_a))

        notifs_a = client.get("/api/notifications/", headers=auth_header(token_a)).json()
        notifs_b = client.get("/api/notifications/", headers=auth_header(token_b)).json()
        assert len(notifs_a) > 0
        assert len(notifs_b) == 0


# ---------------------------------------------------------------------------
# 6. Search & Filtering
#    Validate search parameters work across the API (mirrors web search page)
# ---------------------------------------------------------------------------

class TestFlightSearchIntegration:
    """Flight search with various filter combinations."""

    def test_search_by_origin(self, client):
        resp = client.get("/api/flights/search", params={"origin": "JFK"})
        assert resp.status_code == 200
        for f in resp.json():
            assert f["departure_airport"]["code"] == "JFK"

    def test_search_by_price_range(self, client):
        resp = client.get("/api/flights/search", params={
            "min_price": 300, "max_price": 500,
        })
        assert resp.status_code == 200
        for f in resp.json():
            assert 300 <= f["price"] <= 500

    def test_search_returns_available_seats(self, client, customer_token):
        flights = client.get("/api/flights/search", params={"origin": "JFK"}).json()
        assert len(flights) > 0
        flight = flights[0]
        initial_avail = flight["available_seats"]

        client.post("/api/bookings/", json={
            "flight_id": flight["id"],
            "passengers": [
                {"name": "Seat Counter", "email": "sc@test.com",
                 "seat_id": _available_seat(client, flight["id"])["id"]},
            ],
        }, headers=auth_header(customer_token))

        updated = client.get(f"/api/flights/{flight['id']}").json()
        assert updated["available_seats"] == initial_avail - 1

    def test_search_no_results(self, client):
        resp = client.get("/api/flights/search", params={"min_price": 99999})
        assert resp.status_code == 200
        assert len(resp.json()) == 0


# ---------------------------------------------------------------------------
# 7. Seat Management Integration
#    Reassign seats, verify availability
# ---------------------------------------------------------------------------

class TestSeatManagementIntegration:
    """Seat assignment and reassignment across booking lifecycle."""

    def test_seat_reassignment(self, client, customer_token):
        headers = auth_header(customer_token)
        flight = _first_flight(client)
        seats = client.get(f"/api/seats/flight/{flight['id']}").json()
        avail = [s for s in seats if s["is_available"]]
        seat_a, seat_b = avail[0], avail[1]

        booking = client.post("/api/bookings/", json={
            "flight_id": flight["id"],
            "passengers": [
                {"name": "Reassigner", "email": "re@test.com", "seat_id": seat_a["id"]},
            ],
        }, headers=headers).json()

        passenger_id = booking["passengers"][0]["id"]

        # Reassign to seat_b
        reassign = client.post("/api/seats/assign", json={
            "seat_id": seat_b["id"],
            "passenger_id": passenger_id,
        }, headers=headers)
        assert reassign.status_code == 200

        # Verify seat_a is freed and seat_b is taken
        seats_after = client.get(f"/api/seats/flight/{flight['id']}").json()
        a_after = next(s for s in seats_after if s["id"] == seat_a["id"])
        b_after = next(s for s in seats_after if s["id"] == seat_b["id"])
        assert a_after["is_available"] is True
        assert b_after["is_available"] is False


# ---------------------------------------------------------------------------
# 8. Authorization & Access Control
#    Ensure correct role enforcement across endpoints
# ---------------------------------------------------------------------------

class TestAccessControl:
    """Verify auth guards prevent unauthorized access across all endpoints."""

    def test_unauthenticated_cannot_book(self, client):
        flight = _first_flight(client)
        resp = client.post("/api/bookings/", json={
            "flight_id": flight["id"],
            "passengers": [{"name": "Anon", "email": "anon@test.com"}],
        })
        assert resp.status_code == 401

    def test_customer_cannot_create_flight(self, client, customer_token):
        airports = client.get("/api/airports/").json()
        resp = client.post("/api/flights/", json={
            "flight_number": "HACK-01",
            "departure_airport_id": airports[0]["id"],
            "arrival_airport_id": airports[1]["id"],
            "departure_time": "2026-06-01T10:00:00",
            "arrival_time": "2026-06-01T15:00:00",
            "price": 100.00,
            "total_seats": 50,
        }, headers=auth_header(customer_token))
        assert resp.status_code == 403

    def test_customer_cannot_access_reports(self, client, customer_token):
        headers = auth_header(customer_token)
        for endpoint in [
            "/api/reports/bookings-per-flight",
            "/api/reports/popular-routes",
            "/api/reports/peak-times",
        ]:
            resp = client.get(endpoint, headers=headers)
            assert resp.status_code == 403

    def test_customer_cannot_resolve_complaints(self, client, customer_token):
        headers = auth_header(customer_token)
        flight = _first_flight(client)
        booking = client.post("/api/bookings/", json={
            "flight_id": flight["id"],
            "passengers": [{"name": "Guard", "email": "guard@test.com"}],
        }, headers=headers).json()
        complaint = client.post("/api/complaints/", json={
            "booking_id": booking["id"],
            "subject": "Test",
            "description": "Test",
        }, headers=headers).json()
        resp = client.put(f"/api/complaints/{complaint['id']}", json={
            "status": "resolved",
            "admin_response": "Self-resolve attempt",
        }, headers=headers)
        assert resp.status_code == 403

    def test_customer_cannot_see_others_booking(self, client, customer_token):
        _register_user(client, "other@test.com")
        other_token = _login(client, "other@test.com")
        flight = _first_flight(client)
        booking = client.post("/api/bookings/", json={
            "flight_id": flight["id"],
            "passengers": [{"name": "Other", "email": "other@test.com"}],
        }, headers=auth_header(other_token)).json()

        resp = client.get(
            f"/api/bookings/{booking['id']}",
            headers=auth_header(customer_token),
        )
        assert resp.status_code == 403


# ---------------------------------------------------------------------------
# 9. Data Consistency
#    Verify data integrity across related entities
# ---------------------------------------------------------------------------

class TestDataConsistency:
    """Cross-entity data integrity: bookings ↔ seats ↔ loyalty ↔ notifications."""

    def test_booking_creates_all_side_effects(self, client):
        _register_user(client, "sideeffect@test.com", "Side Effect")
        token = _login(client, "sideeffect@test.com")
        headers = auth_header(token)

        # Initial state
        loyalty_before = client.get("/api/loyalty/", headers=headers).json()
        notifs_before = client.get("/api/notifications/", headers=headers).json()

        flight = _first_flight(client)
        seat = _available_seat(client, flight["id"])

        # Book
        booking = client.post("/api/bookings/", json={
            "flight_id": flight["id"],
            "passengers": [
                {"name": "Side Effect", "email": "se@test.com", "seat_id": seat["id"]},
            ],
        }, headers=headers).json()

        # Loyalty updated
        loyalty_after = client.get("/api/loyalty/", headers=headers).json()
        expected_pts = int(booking["total_price"] * 0.1)
        assert loyalty_after["earned"] == loyalty_before["earned"] + expected_pts
        assert loyalty_after["balance"] == loyalty_before["balance"] + expected_pts

        # Notification created
        notifs_after = client.get("/api/notifications/", headers=headers).json()
        assert len(notifs_after) == len(notifs_before) + 1

        # Seat taken
        seat_after = client.get(f"/api/seats/flight/{flight['id']}").json()
        s = next(s for s in seat_after if s["id"] == seat["id"])
        assert s["is_available"] is False

    def test_cancellation_reverses_seat_but_keeps_points(self, client):
        _register_user(client, "cancel_test@test.com", "Cancel Test")
        token = _login(client, "cancel_test@test.com")
        headers = auth_header(token)

        flight = _first_flight(client)
        seat = _available_seat(client, flight["id"])

        booking = client.post("/api/bookings/", json={
            "flight_id": flight["id"],
            "passengers": [
                {"name": "Cancel Test", "email": "ct@test.com", "seat_id": seat["id"]},
            ],
        }, headers=headers).json()

        loyalty_after_book = client.get("/api/loyalty/", headers=headers).json()

        client.post(f"/api/bookings/{booking['id']}/cancel", headers=headers)

        # Seat freed
        seats = client.get(f"/api/seats/flight/{flight['id']}").json()
        freed = next(s for s in seats if s["id"] == seat["id"])
        assert freed["is_available"] is True

        # Loyalty points remain (no clawback on cancel)
        loyalty_after_cancel = client.get("/api/loyalty/", headers=headers).json()
        assert loyalty_after_cancel["earned"] == loyalty_after_book["earned"]


# ---------------------------------------------------------------------------
# 10. Admin Reports Reflect Booking Activity
# ---------------------------------------------------------------------------

class TestReportsIntegration:
    """Admin reports accurately reflect booking data."""

    def test_reports_reflect_new_booking(self, client, customer_token, admin_token):
        cust_headers = auth_header(customer_token)
        admin_headers = auth_header(admin_token)

        flight = _first_flight(client)
        client.post("/api/bookings/", json={
            "flight_id": flight["id"],
            "passengers": [{"name": "Reporter", "email": "rpt@test.com"}],
        }, headers=cust_headers)

        bpf = client.get("/api/reports/bookings-per-flight", headers=admin_headers).json()
        flight_report = next(
            (r for r in bpf if r["flight_id"] == flight["id"]), None
        )
        assert flight_report is not None
        assert flight_report["booking_count"] >= 1

        routes = client.get("/api/reports/popular-routes", headers=admin_headers).json()
        assert len(routes) >= 1

        peaks = client.get("/api/reports/peak-times", headers=admin_headers).json()
        assert len(peaks) >= 1
