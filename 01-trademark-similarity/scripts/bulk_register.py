"""
폴더 안의 상표 이미지를 웹(/trademarks/new)에 한꺼번에 등록한다.

파일명 규칙:  <출원번호>_<상표명>.png   예) 4020240001234_사과마트.png
             '_' 가 없으면 파일명이 출원번호와 상표명에 모두 들어간다.

실행 (ai-server 가상환경의 httpx 사용):
  ai-server\\.venv\\Scripts\\python scripts\\bulk_register.py <이미지폴더>
  옵션: --url http://localhost:8081  --user user  --password user1234  --nice-class 9  --applicant 홍길동

이미 등록된 출원번호는 건너뛰고(중복), 마지막에 성공/건너뜀/실패 개수를 출력한다.
"""
import argparse
import html
import re
import sys
from pathlib import Path

import httpx

IMAGE_EXTS = {".png", ".jpg", ".jpeg", ".gif", ".bmp", ".webp"}
CSRF_RE = re.compile(r'name="_csrf"\s+value="([^"]+)"')
ERROR_RE = re.compile(r'class="invalid-feedback"[^>]*>([^<]+)<')


def parse_filename(path: Path) -> tuple[str, str]:
    """'출원번호_상표명.ext' → (출원번호, 상표명). '_' 가 없으면 둘 다 파일명."""
    no, sep, name = path.stem.partition("_")
    return (no, name) if sep and no and name else (path.stem, path.stem)


def csrf(client: httpx.Client, url: str) -> str:
    m = CSRF_RE.search(client.get(url).text)
    if not m:
        sys.exit(f"CSRF 토큰을 찾을 수 없습니다: {url}")
    return m.group(1)


def login(client: httpx.Client, user: str, password: str) -> None:
    r = client.post("/login", data={"username": user, "password": password, "_csrf": csrf(client, "/login")})
    if r.status_code != 302 or "error" in r.headers.get("location", ""):
        sys.exit("로그인 실패: 아이디/비밀번호를 확인하세요.")


def register(client: httpx.Client, image: Path, args) -> tuple[str, str]:
    """반환: (결과, 메시지)  결과 = ok | skip | fail"""
    no, name = parse_filename(image)
    data = {"applicationNo": no, "name": name, "_csrf": csrf(client, "/trademarks/new")}
    if args.applicant:
        data["applicant"] = args.applicant
    if args.nice_class:
        data["niceClass"] = str(args.nice_class)
    with image.open("rb") as f:
        r = client.post("/trademarks/new", data=data, files={"image": (image.name, f)})
    if r.status_code == 302 and re.search(r"/trademarks/\d+$", r.headers.get("location", "")):
        return "ok", r.headers["location"]
    errors = [html.unescape(e).strip() for e in ERROR_RE.findall(r.text) if e.strip()]
    msg = "; ".join(errors) or f"HTTP {r.status_code}"
    return ("skip" if "이미" in msg or "중복" in msg else "fail"), msg


def main() -> None:
    p = argparse.ArgumentParser(description="상표 이미지 일괄 등록")
    p.add_argument("folder", type=Path)
    p.add_argument("--url", default="http://localhost:8081")
    p.add_argument("--user", default="user")
    p.add_argument("--password", default="user1234")
    p.add_argument("--applicant")
    p.add_argument("--nice-class", type=int)
    args = p.parse_args()

    images = sorted(f for f in args.folder.iterdir() if f.suffix.lower() in IMAGE_EXTS)
    if not images:
        sys.exit(f"이미지가 없습니다: {args.folder}")

    counts = {"ok": 0, "skip": 0, "fail": 0}
    with httpx.Client(base_url=args.url, timeout=60) as client:
        login(client, args.user, args.password)
        for i, image in enumerate(images, 1):
            result, msg = register(client, image, args)
            counts[result] += 1
            print(f"[{i}/{len(images)}] {result.upper():4} {image.name} - {msg}")
    print(f"\n완료: 성공 {counts['ok']}, 건너뜀 {counts['skip']}, 실패 {counts['fail']}")


if __name__ == "__main__":
    assert parse_filename(Path("40201_사과마트.png")) == ("40201", "사과마트")
    assert parse_filename(Path("logo.png")) == ("logo", "logo")
    assert parse_filename(Path("_x.png")) == ("_x", "_x")
    main()
