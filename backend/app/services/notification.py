import asyncio
from fastapi import WebSocket


class ConnectionManager:
    def __init__(self) -> None:
        self._connections: dict[int, list[WebSocket]] = {}

    async def connect(self, user_id: int, websocket: WebSocket) -> None:
        await websocket.accept()
        self._connections.setdefault(user_id, []).append(websocket)

    def disconnect(self, user_id: int, websocket: WebSocket) -> None:
        user_conns = self._connections.get(user_id, [])
        if websocket in user_conns:
            user_conns.remove(websocket)
        if not user_conns:
            self._connections.pop(user_id, None)

    async def send_to_user(self, user_id: int, data: dict) -> None:
        for ws in list(self._connections.get(user_id, [])):
            try:
                await ws.send_json(data)
            except Exception:
                self.disconnect(user_id, ws)

    async def broadcast(self, data: dict) -> None:
        tasks = [
            self.send_to_user(uid, data) for uid in list(self._connections)
        ]
        await asyncio.gather(*tasks, return_exceptions=True)


manager = ConnectionManager()
