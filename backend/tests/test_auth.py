from tests.conftest import auth_header


def test_register_success(client):
    resp = client.post("/api/auth/register", json={
        "email": "newuser@test.com",
        "password": "password123",
        "name": "New User",
    })
    assert resp.status_code == 201
    data = resp.json()
    assert data["email"] == "newuser@test.com"
    assert data["name"] == "New User"
    assert data["role"] == "customer"
    assert "id" in data


def test_register_duplicate_email(client):
    resp = client.post("/api/auth/register", json={
        "email": "demo@flightbooking.com",
        "password": "password123",
        "name": "Duplicate",
    })
    assert resp.status_code == 400
    assert "already registered" in resp.json()["detail"]


def test_login_success(client):
    resp = client.post("/api/auth/login", json={
        "email": "demo@flightbooking.com",
        "password": "demo123",
    })
    assert resp.status_code == 200
    data = resp.json()
    assert "access_token" in data
    assert data["token_type"] == "bearer"


def test_login_wrong_password(client):
    resp = client.post("/api/auth/login", json={
        "email": "demo@flightbooking.com",
        "password": "wrong",
    })
    assert resp.status_code == 401


def test_login_nonexistent_user(client):
    resp = client.post("/api/auth/login", json={
        "email": "nonexistent@test.com",
        "password": "password",
    })
    assert resp.status_code == 401


def test_get_me(client, customer_token):
    resp = client.get("/api/auth/me", headers=auth_header(customer_token))
    assert resp.status_code == 200
    data = resp.json()
    assert data["email"] == "demo@flightbooking.com"
    assert data["role"] == "customer"


def test_get_me_admin(client, admin_token):
    resp = client.get("/api/auth/me", headers=auth_header(admin_token))
    assert resp.status_code == 200
    assert resp.json()["role"] == "admin"


def test_get_me_no_token(client):
    resp = client.get("/api/auth/me")
    assert resp.status_code == 401


def test_get_me_invalid_token(client):
    resp = client.get("/api/auth/me", headers=auth_header("invalid.token.here"))
    assert resp.status_code == 401
