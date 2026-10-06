"""Exercise operator tools against an isolated HTTP server, never a business DB."""
import contextlib
import hashlib
import json
import os
from pathlib import Path
import subprocess
import threading
import unittest
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from tempfile import TemporaryDirectory


TOOLS = Path(__file__).resolve().parents[2] / "tools"
TOKEN = "private-fixture-token"


@contextlib.contextmanager
def server(mode="normal"):
    state = {"requests": [], "pages": {}, "next_id": 100, "deleted": [], "save_bodies": []}

    class Handler(BaseHTTPRequestHandler):
        def log_message(self, *args):
            pass

        def reply(self, status, body=None, **headers):
            self.send_response(status)
            self.send_header("Content-Type", "application/json")
            self.send_header("Set-Cookie", "XSRF-TOKEN=fixture-csrf; Path=/")
            for name, value in headers.items():
                self.send_header(name, value)
            self.end_headers()
            if body is not None:
                self.wfile.write(json.dumps(body).encode())

        def handle_request(self):
            with lock:
                state["requests"].append((self.command, self.path))
                if self.path.startswith("/management/health"):
                    return self.reply(200, {"status": "DOWN" if mode == "unhealthy" else "UP"})
                if self.path == "/management/info":
                    return self.reply(200, {"git": {"commit": {"id": {"full": "a" * 40,
                                           "describe": "aaaaaaa-dirty" if mode == "dirty" else "aaaaaaa"}}}})
                if self.headers.get("Authorization") != "Bearer " + TOKEN:
                    return self.reply(401, {"private": TOKEN})
                if mode == "redirect":
                    return self.reply(302, None, Location="/redirect-target")
                if self.path == "/api/wiki/spaces":
                    return self.reply(200, [{"slug": "team", "pageCount": 2 if mode == "small" else 10000}])
                if self.path == "/api/wiki/spaces/team":
                    return self.reply(200, {"slug": "team", "editable": True})
                if self.path == "/api/wiki/spaces/hidden":
                    return self.reply(200 if mode == "leak" else 404, {"private": TOKEN})
                if self.path.startswith("/api/wiki/search?"):
                    return self.reply(200, {"items": [], "total": 0})
                if self.path == "/api/wiki/pages/1":
                    return self.reply(200, {"id": 1, "spaceSlug": "team", "gitReadOnly": True, "editable": False, "versionNo": 1})
                if self.path == "/api/wiki/pages/1/versions?size=1":
                    return self.reply(200, {"items": [{"versionNo": 1, "contentMd": None}], "total": 1})
                if self.path == "/api/wiki/spaces/team/pages" and self.command == "POST":
                    payload = json.loads(self.rfile.read(int(self.headers["Content-Length"])))
                    state["save_bodies"].append(payload["contentMd"])
                    state["next_id"] += 1
                    page_id = state["next_id"]
                    state["pages"][page_id] = 1
                    return self.reply(201, {"id": 1 if mode == "foreign-create" else page_id,
                                           "versionNo": 1, "gitReadOnly": False, "kind": "NATIVE",
                                           "spaceSlug": "team", "title": payload["title"]})
                if self.path.endswith("/content") and self.command == "PUT":
                    page_id = int(self.path.split("/")[4])
                    payload = json.loads(self.rfile.read(int(self.headers["Content-Length"])))
                    state["save_bodies"].append(payload["contentMd"])
                    if mode == "save-fail":
                        return self.reply(409, {"private": TOKEN})
                    if payload["baseVersionNo"] != state["pages"][page_id]:
                        return self.reply(409)
                    if self.headers.get("X-XSRF-TOKEN") != "fixture-csrf":
                        return self.reply(403)
                    state["pages"][page_id] += 1
                    return self.reply(200, {"versionNo": state["pages"][page_id]})
                if self.command == "DELETE":
                    if mode == "cleanup-fail":
                        return self.reply(403, {"private": TOKEN})
                    page_id = int(self.path.split("/")[4])
                    state["deleted"].append(page_id)
                    return self.reply(204)
                if self.path == "/mcp":
                    message = json.loads(self.rfile.read(int(self.headers["Content-Length"])))
                    if message["method"] == "initialize":
                        result = {"protocolVersion": "2025-11-25", "capabilities": {"tools": {}}}
                    elif message["method"] == "tools/list":
                        result = {"tools": [{"name": "wiki_search"}, {"name": "wiki_get_page"}]}
                    else:
                        result = {"isError": mode == "mcp-deny", "structuredContent": {"id": 1, "spaceSlug": "team"}}
                    return self.reply(200, {"jsonrpc": "2.0", "id": message["id"], "result": result})
                return self.reply(404)

        do_GET = handle_request
        do_POST = handle_request
        do_PUT = handle_request
        do_DELETE = handle_request

    lock = threading.Lock()
    http = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
    worker = threading.Thread(target=http.serve_forever, daemon=True)
    worker.start()
    try:
        yield f"http://127.0.0.1:{http.server_port}", state
    finally:
        http.shutdown()
        http.server_close()
        worker.join()


class AcceptanceToolsTest(unittest.TestCase):
    def invoke(self, tool, *args, token=TOKEN):
        environment = dict(os.environ, WIKI_BENCHMARK_TOKEN=token, WIKI_ACCEPTANCE_READER_TOKEN=token)
        return subprocess.run([str(TOOLS / tool), *args], env=environment, capture_output=True, text=True, timeout=15)

    def benchmark(self, origin, *extra):
        return self.invoke("acceptance-benchmark", "--base-url", origin, "--page-id", "1", "--search", "approved", "--concurrency", "2", "--requests-per-worker", "2", *extra)

    def test_default_benchmark_is_read_only(self):
        with server() as (origin, state):
            result = self.benchmark(origin)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            self.assertEqual(json.loads(result.stdout)["result"], "PASS")
            self.assertTrue(all(method == "GET" for method, _ in state["requests"]))

    def test_small_inventory_never_creates_pages(self):
        with server("small") as (origin, state):
            result = self.benchmark(origin, "--save-space", "team")
            self.assertEqual(result.returncode, 1)
            self.assertEqual(json.loads(result.stdout)["result"], "GAP")
            self.assertEqual(state["pages"], {})

    def test_save_uses_owned_pages_exact_bases_and_csrf(self):
        with server() as (origin, state):
            result = self.benchmark(origin, "--save-space", "team")
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            report = json.loads(result.stdout)
            save = report["measurements"][-1]
            self.assertEqual(save["endpoint"], "save")
            self.assertEqual(save["samples"], 4)
            self.assertEqual(save["budgetMs"], 1000)
            self.assertEqual(set(state["deleted"]), set(state["pages"]))
            self.assertTrue(all(version == 3 for version in state["pages"].values()))
            self.assertNotIn(("DELETE", "/api/wiki/pages/1"), state["requests"])

    def test_failed_save_still_cleans_owned_pages(self):
        with server("save-fail") as (origin, state):
            result = self.benchmark(origin, "--save-space", "team")
            self.assertEqual(result.returncode, 1)
            self.assertEqual(set(state["deleted"]), set(state["pages"]))
            self.assertNotIn(TOKEN, result.stdout + result.stderr)

    def test_representative_utf8_content_is_measured_without_printing_content_or_path(self):
        content = "# Approved acceptance fixture\n" + "代表性文本\n" * 6000
        with TemporaryDirectory() as directory, server() as (origin, state):
            path = Path(directory) / "private-content.md"
            path.write_text(content, encoding="utf-8")
            result = self.benchmark(origin, "--save-space", "team", "--save-content-file", str(path))
            self.assertEqual(0, result.returncode, result.stdout + result.stderr)
            report = json.loads(result.stdout)
            self.assertEqual("externalFile", report["saveContent"]["source"])
            self.assertEqual(len(content.encode()), report["saveContent"]["bodyBytes"])
            self.assertEqual(hashlib.sha256(content.encode()).hexdigest(), report["saveContent"]["bodySha256"])
            self.assertEqual(6, len(state["save_bodies"]))
            self.assertTrue(all(body.startswith(content) for body in state["save_bodies"]))
            self.assertEqual(set(state["deleted"]), set(state["pages"]))
            self.assertNotIn("代表性文本", result.stdout + result.stderr)
            self.assertNotIn(str(path), result.stdout + result.stderr)

    def test_content_file_without_write_authorization_never_reaches_network(self):
        with TemporaryDirectory() as directory, server() as (origin, state):
            path = Path(directory) / "approved.md"
            path.write_text("Approved fixture")
            result = self.benchmark(origin, "--save-content-file", str(path))
            self.assertEqual(1, result.returncode)
            self.assertEqual([], state["requests"])

    def test_invalid_content_fails_before_network(self):
        with TemporaryDirectory() as directory, server() as (origin, state):
            path = Path(directory) / "approved.md"
            for data in (b"", b" \n", b"\xff", b"a" * 1_000_001):
                with self.subTest(size=len(data)):
                    path.write_bytes(data)
                    result = self.benchmark(origin, "--save-space", "team", "--save-content-file", str(path))
                    self.assertEqual(1, result.returncode)
                    self.assertEqual([], state["requests"])
            path.unlink()
            result = self.benchmark(origin, "--save-space", "team", "--save-content-file", str(path))
            self.assertEqual(1, result.returncode)
            self.assertEqual([], state["requests"])

    def test_cleanup_failure_cannot_report_pass(self):
        with server("cleanup-fail") as (origin, state):
            result = self.benchmark(origin, "--save-space", "team")
            report = json.loads(result.stdout)
            self.assertEqual(result.returncode, 1)
            self.assertEqual(report["result"], "FAIL")
            self.assertEqual(set(report["retainedPageIds"]), set(state["pages"]))

    def test_malformed_create_never_deletes_existing_page(self):
        with server("foreign-create") as (origin, state):
            result = self.benchmark(origin, "--save-space", "team")
            self.assertEqual(result.returncode, 1)
            self.assertTrue(json.loads(result.stdout)["unacknowledgedCreation"])
            self.assertEqual(state["deleted"], [])

    def test_redirects_never_receive_another_request(self):
        with server("redirect") as (origin, state):
            result = self.benchmark(origin)
            self.assertEqual(result.returncode, 1)
            self.assertEqual(state["requests"], [("GET", "/api/wiki/spaces")])
            self.assertNotIn(TOKEN, result.stdout + result.stderr)

    def test_missing_token_fails_before_network(self):
        with server() as (origin, state):
            result = self.invoke("acceptance-benchmark", "--base-url", origin, "--page-id", "1", "--search", "approved", token="")
            self.assertNotEqual(result.returncode, 0)
            self.assertEqual(state["requests"], [])

    def smoke(self, origin, plan, *extra):
        with TemporaryDirectory() as directory:
            path = Path(directory) / "plan.json"
            path.write_text(json.dumps(plan))
            return self.invoke("acceptance-smoke", "--base-url", origin, "--plan", str(path), *extra)

    def plan(self):
        return {"identities": [{"label": "reader", "tokenEnv": "WIKI_ACCEPTANCE_READER_TOKEN", "allow": ["team"], "deny": ["hidden"], "pages": [{"id": 1, "space": "team", "gitReadOnly": True}], "mcp": True}]}

    def test_smoke_proves_permission_matrix_and_personal_mcp_without_writes(self):
        with server() as (origin, state):
            result = self.smoke(origin, self.plan())
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            self.assertEqual(json.loads(result.stdout)["result"], "PASS")
            self.assertTrue(all(method == "GET" or path == "/mcp" for method, path in state["requests"]))
            self.assertNotIn(TOKEN, result.stdout + result.stderr)

    def test_visible_denied_space_fails(self):
        with server("leak") as (origin, _):
            result = self.smoke(origin, self.plan())
            self.assertEqual(result.returncode, 1)
            self.assertEqual(json.loads(result.stdout)["result"], "FAIL")
            self.assertNotIn(TOKEN, result.stdout + result.stderr)

    def test_personal_mcp_tool_error_fails(self):
        with server("mcp-deny") as (origin, _):
            result = self.smoke(origin, self.plan())
            self.assertEqual(result.returncode, 1)
            self.assertIn({"check": "reader:personal-mcp", "result": "FAIL"}, json.loads(result.stdout)["checks"])

    def test_health_cannot_substitute_for_correct_release(self):
        with server() as (origin, _):
            result = self.smoke(origin, self.plan(), "--expected-commit", "bbbbbbb")
            self.assertEqual(result.returncode, 1)
            self.assertIn({"check": "deployed-commit", "result": "FAIL"}, json.loads(result.stdout)["checks"])

    def test_dirty_matching_commit_cannot_pass_release_check(self):
        with server("dirty") as (origin, _):
            result = self.smoke(origin, self.plan(), "--expected-commit", "aaaaaaa")
            self.assertEqual(1, result.returncode)
            self.assertIn({"check": "deployed-commit", "result": "FAIL"}, json.loads(result.stdout)["checks"])

    def test_public_probe_requires_no_token_and_keeps_identity_gap(self):
        with server() as (origin, state):
            result = self.invoke("acceptance-smoke", "--base-url", origin, "--public-only", "--expected-commit", "aaaaaaa", token="")
            self.assertEqual(0, result.returncode, result.stdout + result.stderr)
            report = json.loads(result.stdout)
            self.assertEqual("PARTIAL", report["result"])
            self.assertEqual("GAP", report["identityAcceptance"])
            self.assertEqual("publicOnly", report["mode"])
            self.assertEqual(4, len(state["requests"]))
            self.assertTrue(all(method == "GET" for method, _ in state["requests"]))

    def test_public_probe_fails_for_unhealthy_runtime(self):
        with server("unhealthy") as (origin, _):
            result = self.invoke("acceptance-smoke", "--base-url", origin, "--public-only", token="")
            self.assertEqual(1, result.returncode)
            self.assertEqual("FAIL", json.loads(result.stdout)["result"])

    def test_unhealthy_instance_fails(self):
        with server("unhealthy") as (origin, _):
            result = self.smoke(origin, self.plan())
            self.assertEqual(result.returncode, 1)

    def test_incomplete_plan_is_gap_not_pass(self):
        with server() as (origin, state):
            result = self.smoke(origin, {"identities": []})
            self.assertNotEqual(result.returncode, 0)
            self.assertEqual(state["requests"], [])

    def test_denied_page_sample_cannot_be_sent_to_mcp(self):
        plan = self.plan()
        plan["identities"][0]["pages"][0]["space"] = "hidden"
        with server() as (origin, state):
            result = self.smoke(origin, plan)
            self.assertNotEqual(result.returncode, 0)
            self.assertEqual(state["requests"], [])

    def test_origin_with_embedded_credentials_is_rejected(self):
        result = self.benchmark("http://private-user:private-password@localhost")
        self.assertNotEqual(result.returncode, 0)
        self.assertNotIn("private-password", result.stdout + result.stderr)


if __name__ == "__main__":
    unittest.main()
