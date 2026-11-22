"""운영 사이트를 크롤링해서 Cloudflare Pages에 올릴 정적 스냅샷(dist)을 만든다.

사용법: python scripts/bake_snapshot.py [출력 폴더]
환경변수 SNAPSHOT_BASE로 대상 주소를 바꾼다(기본 https://logos-api.com).
원본이 아니라 Worker 폴백(X-Served-By 헤더)이 응답한 페이지는 실패로 보고, 하나라도 실패하면 종료 코드 1로 끝난다.
Cloudflare 속도 제한(429)에 걸리면 차단이 풀릴 때까지 기다렸다 다시 시도한다.
"""
import os
import re
import shutil
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

BASE = os.environ.get("SNAPSHOT_BASE", "https://logos-api.com").rstrip("/")
OUT = Path(sys.argv[1] if len(sys.argv) > 1 else "dist")
WORKERS = 3
REQUEST_GAP = 0.15          # 요청 사이 간격(초). 워커 3개와 합쳐 초당 10건 안쪽으로 유지한다
RETRIES = 5
RATE_LIMIT_WAIT = 12        # 429에 Retry-After가 없을 때 쉬는 시간(초)
RATE_LIMIT_WAIT_MAX = 60    # Retry-After가 너무 길어도 CI가 멈추지 않게 자른다
MIN_PAGES = 1000
# 사이트맵에는 없지만(색인 제외) 모든 화면의 메뉴가 가리키는 페이지. 서버가 꺼진 시간에 메뉴를 눌러도 404가 되지 않게 굽는다.
# 검색은 서버가 있어야 하므로 /read/search는 검색 폼만 구워진다.
MENU_PAGES = ["/read/search", "/read/plan"]
WAIT_SECONDS = 180
UA = "logos-snapshot/1.0"
ASSET_RE = re.compile(r'''(?:href|src)=["'](/[^"'#]+?)["']''')
CSS_URL_RE = re.compile(r'url\(["\']?(/[^"\')]+)["\']?\)')
ASSET_EXT = (".css", ".js", ".svg", ".png", ".jpg", ".jpeg", ".webp", ".ico", ".woff", ".woff2", ".txt")
ROBOTS_MANAGED_RE = re.compile(r"(?s)# As a condition of accessing.*?# END Cloudflare Managed Content\s*")

errors: list[str] = []


def rate_limit_wait(headers) -> int:
    """429가 알려 준 대기 시간. 숫자가 아니거나 없으면 기본값을 쓴다."""
    value = headers.get("Retry-After", "")
    return min(int(value), RATE_LIMIT_WAIT_MAX) if value.isdigit() else RATE_LIMIT_WAIT


def fetch(path: str) -> tuple[int, bytes]:
    """원본이 응답한 결과만 돌려준다. 폴백 응답, 네트워크 오류, 속도 제한은 다시 시도하고 끝내 안 되면 실패로 기록한다."""
    req = urllib.request.Request(BASE + path, headers={"User-Agent": UA})
    for attempt in range(RETRIES):
        time.sleep(REQUEST_GAP)
        try:
            with urllib.request.urlopen(req, timeout=30) as resp:
                status, headers, body = resp.status, resp.headers, resp.read()
        except urllib.error.HTTPError as exc:
            status, headers, body = exc.code, exc.headers, exc.read()
        except Exception:
            time.sleep(1 + attempt)
            continue
        if status == 429:
            time.sleep(rate_limit_wait(headers))
            continue
        if headers.get("X-Served-By") or status >= 500:
            time.sleep(2 + attempt)
            continue
        return status, body
    errors.append(f"origin not serving: {path}")
    return 0, b""


def wait_for_origin() -> None:
    """배포 직후 앱이 뜰 때까지 원본이 응답하기를 기다린다."""
    deadline = time.time() + WAIT_SECONDS
    while time.time() < deadline:
        req = urllib.request.Request(BASE + "/", headers={"User-Agent": UA})
        try:
            with urllib.request.urlopen(req, timeout=15) as resp:
                if resp.status == 200 and not resp.headers.get("X-Served-By"):
                    return
        except Exception:
            pass
        time.sleep(5)
    sys.exit("원본이 응답하지 않아 스냅샷을 만들지 않는다")


def page_file(path: str) -> Path:
    clean = urllib.parse.unquote(path).strip("/")
    return OUT / ("index.html" if not clean else clean + ".html")


def asset_file(path: str) -> Path:
    return OUT / urllib.parse.unquote(path).lstrip("/")


def bake_page(path: str) -> tuple[str, bytes]:
    status, body = fetch(path)
    if status == 0:
        return path, b""
    if status != 200:
        errors.append(f"{status} {path}")
        return path, b""
    page_file(path).parent.mkdir(parents=True, exist_ok=True)
    page_file(path).write_bytes(body)
    return path, body


def bake_asset(path: str) -> bytes:
    status, body = fetch(path)
    if status == 0:
        return b""
    if status != 200:
        errors.append(f"{status} asset {path}")
        return b""
    target = asset_file(path.split("?")[0])
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(body)
    return body


def main() -> None:
    wait_for_origin()
    if OUT.exists():
        shutil.rmtree(OUT)
    OUT.mkdir(parents=True)

    _, sitemap = fetch("/sitemap.xml")
    (OUT / "sitemap.xml").write_bytes(sitemap)
    paths = [urllib.parse.urlparse(u).path for u in re.findall(r"<loc>([^<]+)</loc>", sitemap.decode("utf-8"))]
    print(f"sitemap urls: {len(paths)}")

    with ThreadPoolExecutor(WORKERS) as pool:
        results = list(pool.map(bake_page, paths + MENU_PAGES))
    saved = sum(1 for _, body in results if body)
    print(f"pages saved: {saved}")

    status, body = fetch("/__snapshot_404_probe__")
    if status == 404:
        (OUT / "404.html").write_bytes(body)
        results.append(("/404", body))
    else:
        errors.append(f"404 probe returned {status}")

    assets: set[str] = set()
    for _, body in results:
        for ref in ASSET_RE.findall(body.decode("utf-8", "ignore")):
            if not ref.startswith("/cdn-cgi/") and ref.split("?")[0].lower().endswith(ASSET_EXT):
                assets.add(ref)
    assets |= {"/robots.txt", "/ads.txt", "/favicon.ico"}

    with ThreadPoolExecutor(WORKERS) as pool:
        bodies = dict(zip(sorted(assets), pool.map(bake_asset, sorted(assets))))

    nested = {
        sub
        for ref, body in bodies.items()
        if ref.split("?")[0].lower().endswith(".css")
        for sub in CSS_URL_RE.findall(body.decode("utf-8", "ignore"))
        if sub not in assets
    }
    with ThreadPoolExecutor(WORKERS) as pool:
        list(pool.map(bake_asset, sorted(nested)))
    print(f"assets saved: {len(assets) + len(nested)}")

    robots = OUT / "robots.txt"
    if robots.exists():
        robots.write_text(ROBOTS_MANAGED_RE.sub("", robots.read_text("utf-8")), "utf-8")

    files = [p for p in OUT.rglob("*") if p.is_file()]
    print(f"files: {len(files)}, size: {sum(p.stat().st_size for p in files) / 1e6:.1f} MB")
    print(f"errors: {len(errors)}")
    for e in errors[:20]:
        print("  ", e)
    if errors or saved < MIN_PAGES or len(paths) < MIN_PAGES:
        sys.exit("검증 실패: 스냅샷을 배포하지 않는다")


main()