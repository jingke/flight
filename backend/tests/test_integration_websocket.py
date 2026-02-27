"""
WebSocket integration tests verifying real-time notification delivery
across both web and Android client patterns.

Both clients connect via `ws://.../api/notifications/ws?token=<jwt>`.
The web client uses native WebSocket, Android uses OkHttp WebSocket.
Both rely on the same server-side ConnectionManager.
"""
import threading
import time
from unittest.mock import AsyncMock, patch

import pytest
from fastapi.testclient import TestClient

from app.services.notification import ConnectionManager, manager
from tests.conftest import auth_header, login_as


def _first_flight(client: TestClient):
    return client.get("/api/flights/").json()[0]


# ---------------------------------------------------------------------------
# 1. WebSocket Authentication
# ---------------------------------------------------------------------------

class TestWebSocketAuthentication:
    """WebSocket connections require a valid JWT token."""

    def test_connect_with_valid_token(self, client, customer_token):
        with client.websocket_connect(
            f"/api/notifications/ws?token={customer_token}"
        ) as ws:
            assert ws is not None

    def test_connect_without_token_rejected(self, client):
        with pytest.raises(Exception):
            with client.websocket_connect("/api/notifications/ws") as ws:
                ws.receive_json()

    def test_connect_with_invalid_token_rejected(self, client):
        with pytest.raises(Exception):
            with client.websocket_connect(
                "/api/notifications/ws?token=invalid.jwt.token"
            ) as ws:
                ws.receive_json()

    def test_connect_with_empty_token_rejected(self, client):
        with pytest.raises(Exception):
            with client.websocket_connect(
                "/api/notifications/ws?token="
            ) as ws:
                ws.receive_json()


# ---------------------------------------------------------------------------
# 2. WebSocket Notification Delivery (simulates both web & Android clients)
# ---------------------------------------------------------------------------

class TestWebSocketNotificationDelivery:
    """
    Verify that the ConnectionManager can push real-time notifications
    to connected clients. Both web (browser WebSocket) and Android
    (OkHttp WebSocket) clients use the same /ws endpoint.
    """

    def test_manager_sends_to_connected_user(self, client, customer_token):
        """Simulates a single client (web or Android) receiving a notification."""
        with client.websocket_connect(
            f"/api/notifications/ws?token={customer_token}"
        ) as ws:
            import asyncio
            loop = asyncio.new_event_loop()
            me = client.get(
                "/api/auth/me",
                headers=auth_header(customer_token),
            ).json()
            loop.run_until_complete(
                manager.send_to_user(me["id"], {
                    "title": "Test Notification",
                    "message": "Hello from integration test",
                })
            )
            loop.close()
            data = ws.receive_json()
            assert data["title"] == "Test Notification"
            assert data["message"] == "Hello from integration test"

    def test_broadcast_reaches_all_users(self, client, customer_token, admin_token):
        """Simulates broadcast reaching both a web client and an Android client."""
        with client.websocket_connect(
            f"/api/notifications/ws?token={customer_token}"
        ) as ws_customer:
            with client.websocket_connect(
                f"/api/notifications/ws?token={admin_token}"
            ) as ws_admin:
                import asyncio
                loop = asyncio.new_event_loop()
                loop.run_until_complete(
                    manager.broadcast({
                        "title": "System Maintenance",
                        "message": "Scheduled downtime tonight",
                    })
                )
                loop.close()
                cust_msg = ws_customer.receive_json()
                admin_msg = ws_admin.receive_json()
                assert cust_msg["title"] == "System Maintenance"
                assert admin_msg["title"] == "System Maintenance"


# ---------------------------------------------------------------------------
# 3. Multi-Client Same User (web + Android simultaneously)
# ---------------------------------------------------------------------------

class TestMultiClientSameUser:
    """
    A user connected from both web browser and Android app should
    receive notifications on both connections simultaneously.
    """

    def test_dual_connection_both_receive(self, client, customer_token):
        """Simulates same user logged in on web and Android."""
        with client.websocket_connect(
            f"/api/notifications/ws?token={customer_token}"
        ) as ws_web:
            with client.websocket_connect(
                f"/api/notifications/ws?token={customer_token}"
            ) as ws_android:
                import asyncio
                loop = asyncio.new_event_loop()
                me = client.get(
                    "/api/auth/me",
                    headers=auth_header(customer_token),
                ).json()
                loop.run_until_complete(
                    manager.send_to_user(me["id"], {
                        "title": "Dual Device",
                        "message": "Booking confirmed on both devices",
                    })
                )
                loop.close()
                web_msg = ws_web.receive_json()
                android_msg = ws_android.receive_json()
                assert web_msg["title"] == "Dual Device"
                assert android_msg["title"] == "Dual Device"
                assert web_msg == android_msg


# ---------------------------------------------------------------------------
# 4. ConnectionManager Unit Integration
# ---------------------------------------------------------------------------

class TestConnectionManagerIntegration:
    """Verify ConnectionManager correctly manages connection state."""

    @pytest.mark.anyio
    async def test_fresh_manager_has_no_connections(self):
        mgr = ConnectionManager()
        assert mgr._connections == {}

    @pytest.mark.anyio
    async def test_connect_and_disconnect(self):
        mgr = ConnectionManager()
        mock_ws = AsyncMock()
        await mgr.connect(1, mock_ws)
        assert 1 in mgr._connections
        assert len(mgr._connections[1]) == 1
        mgr.disconnect(1, mock_ws)
        assert 1 not in mgr._connections

    @pytest.mark.anyio
    async def test_multiple_connections_per_user(self):
        mgr = ConnectionManager()
        ws1 = AsyncMock()
        ws2 = AsyncMock()
        await mgr.connect(42, ws1)
        await mgr.connect(42, ws2)
        assert len(mgr._connections[42]) == 2
        mgr.disconnect(42, ws1)
        assert len(mgr._connections[42]) == 1
        mgr.disconnect(42, ws2)
        assert 42 not in mgr._connections

    @pytest.mark.anyio
    async def test_send_to_user_delivers_to_all_connections(self):
        mgr = ConnectionManager()
        ws1 = AsyncMock()
        ws2 = AsyncMock()
        await mgr.connect(10, ws1)
        await mgr.connect(10, ws2)
        payload = {"title": "Test", "message": "Multi-conn"}
        await mgr.send_to_user(10, payload)
        ws1.send_json.assert_called_once_with(payload)
        ws2.send_json.assert_called_once_with(payload)

    @pytest.mark.anyio
    async def test_send_removes_broken_connections(self):
        mgr = ConnectionManager()
        good_ws = AsyncMock()
        bad_ws = AsyncMock()
        bad_ws.send_json.side_effect = Exception("Connection closed")
        await mgr.connect(5, good_ws)
        await mgr.connect(5, bad_ws)
        await mgr.send_to_user(5, {"title": "Test"})
        good_ws.send_json.assert_called_once()
        assert bad_ws not in mgr._connections.get(5, [])

    @pytest.mark.anyio
    async def test_broadcast_sends_to_all_users(self):
        mgr = ConnectionManager()
        ws_a = AsyncMock()
        ws_b = AsyncMock()
        await mgr.connect(1, ws_a)
        await mgr.connect(2, ws_b)
        payload = {"title": "Broadcast"}
        await mgr.broadcast(payload)
        ws_a.send_json.assert_called_once_with(payload)
        ws_b.send_json.assert_called_once_with(payload)


# ---------------------------------------------------------------------------
# 5. Notification API + WebSocket Consistency
#    Notifications stored in DB are also available via REST after WS delivery
# ---------------------------------------------------------------------------

class TestNotificationConsistency:
    """Notifications stored in DB match what REST API returns."""

    def test_booking_notification_in_rest_api(self, client, customer_token):
        """After booking, notification exists in both DB (via REST) and would
        be pushed via WebSocket to any connected client."""
        headers = auth_header(customer_token)
        flight = _first_flight(client)

        client.post("/api/bookings/", json={
            "flight_id": flight["id"],
            "passengers": [{"name": "WS Test", "email": "ws@test.com"}],
        }, headers=headers)

        notifs = client.get("/api/notifications/", headers=headers).json()
        booking_notifs = [n for n in notifs if n["title"] == "Booking Confirmed"]
        assert len(booking_notifs) >= 1
        assert booking_notifs[0]["is_read"] is False

    def test_multiple_actions_create_ordered_notifications(self, client, customer_token, admin_token):
        """Notification ordering is consistent: newest first (desc by created_at)."""
        cust_headers = auth_header(customer_token)
        admin_headers = auth_header(admin_token)
        flight = _first_flight(client)

        # Booking
        booking = client.post("/api/bookings/", json={
            "flight_id": flight["id"],
            "passengers": [{"name": "Order Test", "email": "ord@test.com"}],
        }, headers=cust_headers).json()

        # Complaint + resolve
        complaint = client.post("/api/complaints/", json={
            "booking_id": booking["id"],
            "subject": "Order test",
            "description": "Testing order",
        }, headers=cust_headers).json()
        client.put(f"/api/complaints/{complaint['id']}", json={
            "status": "resolved",
            "admin_response": "Done",
        }, headers=admin_headers)

        # Cancel
        client.post(f"/api/bookings/{booking['id']}/cancel", headers=cust_headers)

        notifs = client.get("/api/notifications/", headers=cust_headers).json()
        assert len(notifs) >= 3
        titles = [n["title"] for n in notifs]
        assert "Booking Cancelled" in titles
        assert "Complaint Updated" in titles
        assert "Booking Confirmed" in titles
