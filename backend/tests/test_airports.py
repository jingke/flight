def test_list_airports(client):
    resp = client.get("/api/airports/")
    assert resp.status_code == 200
    data = resp.json()
    assert len(data) == 12
    codes = [a["code"] for a in data]
    assert "JFK" in codes
    assert "LAX" in codes
    assert "LHR" in codes


def test_get_airport_by_id(client):
    airports = client.get("/api/airports/").json()
    airport_id = airports[0]["id"]
    resp = client.get(f"/api/airports/{airport_id}")
    assert resp.status_code == 200
    data = resp.json()
    assert data["id"] == airport_id
    assert "latitude" in data
    assert "longitude" in data


def test_get_airport_not_found(client):
    resp = client.get("/api/airports/9999")
    assert resp.status_code == 404
