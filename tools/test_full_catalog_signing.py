#!/usr/bin/env python3
"""Smoke-test the Studio full-catalog signing format without real publisher secrets."""
import base64
import json
import pathlib
import subprocess
import sys
import tempfile

ROOT = pathlib.Path(__file__).resolve().parents[1]


def run(*args):
    subprocess.run(args, check=True, stdout=subprocess.DEVNULL)


def main():
    with tempfile.TemporaryDirectory() as temp:
        root = pathlib.Path(temp)
        private = root / "test-private.pem"
        public = root / "test-public.pem"
        source = root / "admin.json"
        signed = root / "published.json"
        message = root / "message.bin"
        signature = root / "signature.bin"

        run("openssl", "genpkey", "-algorithm", "RSA", "-pkeyopt",
            "rsa_keygen_bits:2048", "-out", str(private))
        run("openssl", "pkey", "-in", str(private), "-pubout", "-out", str(public))
        source.write_text(json.dumps({
            "schema": 2, "kind": "mazda6gh-admin-settings",
            "stores": {"run35_settings": {"features": "[]"},
                       "studio_admin_rows": {},
                       "studio_custom_modules": {"catalog": "[]"},
                       "studio_admin_custom_rows": {}}
        }), encoding="utf-8")

        run(sys.executable, str(ROOT / "tools/sign_full_catalog.py"),
            "--input", str(source), "--output", str(signed),
            "--version", "1", "--private-key", str(private))
        doc = json.loads(signed.read_text(encoding="utf-8"))
        assert doc["schema"] == 2
        assert doc["kind"] == "mazda6gh-published-settings"
        assert doc["version"] == 1
        payload = ("MAZDA6GH-SETTINGS-V2\n1\n" + doc["snapshot_payload"]).encode("utf-8")
        message.write_bytes(payload)
        signature.write_bytes(base64.b64decode(doc["signature"], validate=True))
        run("openssl", "dgst", "-sha256", "-verify", str(public),
            "-signature", str(signature), str(message))

        # A changed payload must fail verification.
        message.write_bytes(payload + b" ")
        result = subprocess.run(
            ["openssl", "dgst", "-sha256", "-verify", str(public),
             "-signature", str(signature), str(message)],
            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        assert result.returncode != 0, "Tampered payload unexpectedly verified"
        print("PASS: signature verified; tampered payload rejected")


if __name__ == "__main__":
    main()
