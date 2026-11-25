#!/usr/bin/env python3
"""Check every government URL shipped inside the Android app.

Run before each release, from a home connection AND from a US server
(Play reviewers are in the US):
    python scripts/check_gov_links.py            # gov links only
    python scripts/check_gov_links.py --all      # every URL in the app
    python scripts/check_gov_links.py --list     # just print the gov URLs
    python scripts/check_gov_links.py --urls f   # check URLs from a file (one per line)

It extracts URLs from android/app/src/main (kt, json, xml), keeps the ones on
government hosts, and fetches each the way a store reviewer would: HTTPS GET,
desktop Chrome user agent, redirects followed, TLS verified. Exit code 1 if any
URL returns 404/410, fails TLS, or does not resolve. 403/429/5xx and timeouts
are reported as WARN (several gov sites block cloud IPs but load on a phone).

Needs Python 3.8+ and `requests`.
"""
import argparse
import concurrent.futures as cf
import pathlib
import re
import sys

import requests

ROOT = pathlib.Path(__file__).resolve().parent.parent
SRC = ROOT / "android" / "app" / "src" / "main"
UA = ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
URL_RE = re.compile(r"https?://[^\s\"'<>\\,]+")
BOT_WALL = re.compile(r"captcha|radware|just a moment|attention required|access denied", re.I)
NOT_FOUND = re.compile(r"page not found|pagina non trovata|página no encontrada|no se encuentra disponible"
                       r"|seite nicht gefunden|\b404\b", re.I)

# Government bodies (national, regional, consular, EU) linked from the app.
GOV_SUFFIXES = (
    # generic government TLD patterns are matched by GOV_PATTERN below
    "canada.ca", "gc.ca", "europa.eu",
    "esteri.it", "interno.it", "inps.it", "normattiva.it", "cortecostituzionale.it",
    "germany.info", "berlin.de", "auswaertiges-amt.de", "bundesarchiv.de", "bund.de",
    "gesetze-im-internet.de", "make-it-in-germany.com", "elster.de",
    "deutsche-rentenversicherung.de", "familienportal.de", "einfach-teilhaben.de",
    "antidiskriminierungsstelle.de", "elterngeld-digital.de", "bmfsfj.de", "integrationsaemter.de",
    "ireland.ie", "irishimmigration.ie", "dfa.ie", "hse.ie", "revenue.ie", "citizensinformation.ie",
    "irishstatutebook.ie", "irishgenealogy.ie", "nationalarchives.ie", "lawreform.ie", "ihrec.ie",
    "exteriores.gob.es", "boe.es", "seg-social.es", "congreso.es", "comunidad.madrid", "madrid.es",
    "euskadi.eus", "sergas.gal", "gva.es", "juntadeandalucia.es", "aragon.es", "zaragoza.es",
    "dnielectronico.es", "imserso.es",
    "min-saude.pt", "seg-social.pt", "sef.pt", "diariodarepublica.pt", "sesaram.pt", "inr.pt", "imt-ip.pt",
    "kormanyhivatal.hu", "kormanyablak.hu", "nyilvantarto.hu", "balassiintezet.hu",
    "zus.pl", "pfron.org.pl",
    "nhs.uk", "supremecourt.uk", "equalityhumanrights.com",
    "pami.org.ar", "cipsaintlucia.com", "banq.qc.ca", "aoda.ca",
)
GOV_PATTERN = re.compile(r"(^|\.)(gov|gob|gouv|mil)(\.[a-z]{2})?$|(^|\.)(gov|gob|gouv)\.[a-z]{2}$")


def host_of(url):
    parts = url.split("/")
    return parts[2].lower().split(":")[0] if len(parts) > 2 else ""


def is_gov(url):
    host = host_of(url)
    if GOV_PATTERN.search(host) or re.search(r"\.(gov|gob)\.", host):
        return True
    return any(host == s or host.endswith("." + s) for s in GOV_SUFFIXES)


EXTS = {".kt", ".json", ".xml", ".swift", ".xcstrings", ".strings", ".html"}


def extract(src=SRC):
    found = {}
    for f in sorted(src.rglob("*")):
        if not f.is_file() or f.suffix not in EXTS or "/build/" in f.as_posix():
            continue
        text = f.read_text(encoding="utf-8", errors="ignore")
        for m in URL_RE.findall(text):
            # Keep balanced parentheses inside a URL, drop a closing one from "(see https://...)".
            url = m.rstrip(".;:")
            while url.endswith(")") and url.count(")") > url.count("("):
                url = url[:-1].rstrip(".;:")
            if "." not in host_of(url):
                continue
            found.setdefault(url, set()).add(f.relative_to(src).as_posix())
    return found


def check(url, timeout=25):
    try:
        r = requests.get(url, headers={"User-Agent": UA, "Accept-Language": "en-US,en;q=0.9",
                                       "Accept": "text/html,application/xhtml+xml,*/*;q=0.8"},
                         timeout=timeout, allow_redirects=True)
        code = r.status_code
        title = ""
        m = re.search(r"<title[^>]*>(.*?)</title>", r.text[:300000], re.S | re.I)
        if m:
            title = re.sub(r"\s+", " ", m.group(1)).strip()[:80]
        status = "OK" if code < 400 else ("FAIL" if code in (404, 410) else "WARN")
        if status == "OK" and BOT_WALL.search(title):
            return "WARN", "BOT", r.url, title  # bot challenge page; check in a real browser
        if status == "OK" and NOT_FOUND.search(title):
            return "FAIL", "SOFT404", r.url, title  # 200 with an error page
        return status, str(code), r.url, title
    except requests.exceptions.SSLError as e:
        return "FAIL", "TLS", url, str(e)[:80]
    except requests.exceptions.ConnectionError as e:
        msg = str(e)
        if any(k in msg for k in ("NameResolution", "getaddrinfo", "Name or service")):
            return "FAIL", "DNS", url, msg[:80]
        return "WARN", "CONN", url, msg[:80]
    except requests.exceptions.Timeout:
        return "WARN", "TIMEOUT", url, ""
    except Exception as e:  # noqa: BLE001
        return "WARN", "ERR", url, str(e)[:80]


def main():
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    ap = argparse.ArgumentParser()
    ap.add_argument("--all", action="store_true", help="check every URL, not only gov hosts")
    ap.add_argument("--list", action="store_true", help="print the URLs and exit")
    ap.add_argument("--urls", help="file with URLs to check instead of extracting from the app")
    ap.add_argument("--workers", type=int, default=12)
    ap.add_argument("--src", help="folder to scan instead of android/app/src/main (for example the iOS app)")
    a = ap.parse_args()

    if a.urls:
        with open(a.urls, encoding="utf-8") as fh:
            urls = {u.strip(): {"(file)"} for u in fh if u.strip() and not u.startswith("#")}
    else:
        found = extract(pathlib.Path(a.src) if a.src else SRC)
        urls = found if a.all else {u: f for u, f in found.items() if is_gov(u)}
    if a.list:
        for u in sorted(urls):
            print(u)
        return 0

    with cf.ThreadPoolExecutor(a.workers) as ex:
        results = dict(zip(urls, ex.map(check, urls)))
    order = {"FAIL": 0, "WARN": 1, "OK": 2}
    fails = warns = 0
    for u in sorted(urls, key=lambda x: (order[results[x][0]], x)):
        status, code, final, note = results[u]
        moved = "" if final.rstrip("/") == u.rstrip("/") else final
        print("\t".join([status, code, u, moved, note, ",".join(sorted(urls[u]))]))
        fails += status == "FAIL"
        warns += status == "WARN"
    print(f"\n{len(urls)} checked, {fails} FAIL, {warns} WARN", file=sys.stderr)
    return 1 if fails else 0


if __name__ == "__main__":
    sys.exit(main())
