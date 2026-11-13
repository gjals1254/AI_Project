import io
import os

os.environ.setdefault("FEATURE_BACKEND", "lite")

from fastapi.testclient import TestClient  # noqa: E402
from PIL import Image, ImageDraw  # noqa: E402

from main import app  # noqa: E402

client = TestClient(app)


def make_logo(shape: str, color: str, shift: int = 0) -> bytes:
    img = Image.new("RGB", (200, 200), "white")
    d = ImageDraw.Draw(img)
    box = (40 + shift, 40 + shift, 160 + shift, 160 + shift)
    if shape == "circle":
        d.ellipse(box, fill=color)
    else:
        d.polygon([(100 + shift, 30), (170 + shift, 170), (30 + shift, 170)], fill=color)
    buf = io.BytesIO()
    img.save(buf, "PNG")
    return buf.getvalue()


def test_health():
    r = client.get("/health")
    assert r.status_code == 200
    assert r.json()["status"] == "UP"


def test_features_returns_normalized_vector():
    r = client.post("/api/v1/features", files={"file": ("a.png", make_logo("circle", "red"), "image/png")})
    assert r.status_code == 200
    body = r.json()
    assert body["dim"] == len(body["vector"])
    norm = sum(x * x for x in body["vector"]) ** 0.5
    assert abs(norm - 1.0) < 1e-3


def test_similar_logo_scores_higher_than_different_logo():
    base = make_logo("circle", "red")
    similar = make_logo("circle", "red", shift=6)
    different = make_logo("triangle", "blue")

    def sim(a, b):
        r = client.post(
            "/api/v1/similarity",
            files={"file1": ("a.png", a, "image/png"), "file2": ("b.png", b, "image/png")},
        )
        assert r.status_code == 200
        return r.json()["similarity"]

    assert sim(base, similar) > sim(base, different)


def test_rejects_non_image():
    r = client.post("/api/v1/features", files={"file": ("a.txt", b"hello", "text/plain")})
    assert r.status_code == 415
