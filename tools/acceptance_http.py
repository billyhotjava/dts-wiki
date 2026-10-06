"""Bounded same-origin HTTP for acceptance tools; never expose response bodies."""
from dataclasses import dataclass
from http.cookiejar import CookieJar
import json
import math
import os
import re
import urllib.error
import urllib.parse
import urllib.request


class ProbeFailure(Exception):
    """Only constant, credential-free messages may be supplied."""


def require(condition, message="Unexpected API response"):
    if not condition:
        raise ProbeFailure(message)


def origin_url(value):
    try:
        parsed = urllib.parse.urlsplit(value)
        port = parsed.port
        require(parsed.scheme in ("http", "https") and parsed.hostname
                and not parsed.username and not parsed.password
                and not parsed.query and not parsed.fragment
                and parsed.path in ("", "/"), "Use an origin URL without credentials or a path")
        require(port is None or 1 <= port <= 65535, "Invalid origin port")
    except ValueError:
        raise ProbeFailure("Invalid origin URL") from None
    return value.rstrip("/")


def external_token(name):
    require(isinstance(name, str) and re.fullmatch(r"WIKI_(?:ACCEPTANCE_[A-Z0-9_]+|BENCHMARK_TOKEN)", name),
            "Invalid external token variable name")
    token = os.environ.get(name, "").strip()
    require(token and not any(character.isspace() for character in token),
            "Set the requested token in external configuration")
    return token


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, response, code, message, headers, new_url):
        return None


@dataclass
class Reply:
    status: int
    data: object


class Client:
    def __init__(self, origin, token=None):
        self.origin = origin_url(origin)
        self.token = token
        self.cookies = CookieJar()
        self.opener = urllib.request.build_opener(NoRedirect(), urllib.request.HTTPCookieProcessor(self.cookies))

    def request(self, method, path, payload=None, headers=None):
        require(path.startswith("/") and not path.startswith("//"), "Invalid request path")
        request_headers = {"Accept": "application/json"}
        request_headers.update(headers or {})
        if self.token:
            request_headers["Authorization"] = "Bearer " + self.token
        if method not in ("GET", "HEAD"):
            for cookie in self.cookies:
                if cookie.name == "XSRF-TOKEN" and not cookie.is_expired():
                    request_headers["X-XSRF-TOKEN"] = urllib.parse.unquote(cookie.value)
        data = None
        if payload is not None:
            data = json.dumps(payload).encode()
            request_headers["Content-Type"] = "application/json"
        request = urllib.request.Request(self.origin + path, data=data, headers=request_headers, method=method)
        try:
            try:
                response = self.opener.open(request, timeout=20)
            except urllib.error.HTTPError as error:
                response = error
            with response:
                body = response.read(20_000_001)
                require(len(body) <= 20_000_000, "Response limit exceeded")
                try:
                    document = json.loads(body) if body else None
                except (ValueError, UnicodeError):
                    document = None
                return Reply(response.status, document)
        except OSError:
            raise ProbeFailure("HTTP request failed") from None

    def json(self, method, path, payload=None, expected=200, headers=None):
        reply = self.request(method, path, payload, headers)
        require(reply.status == expected, "Unexpected HTTP status")
        require(reply.data is not None, "Expected a JSON response")
        return reply.data


def percentile(samples, budget, endpoint):
    require(samples, "No completed measurement samples")
    ordered = sorted(samples)
    p95 = ordered[math.ceil(len(ordered) * 0.95) - 1]
    return {"endpoint": endpoint, "samples": len(samples), "p95Ms": round(p95, 2),
            "budgetMs": budget, "metric": "httpRoundTrip", "result": "PASS" if p95 < budget else "FAIL"}
