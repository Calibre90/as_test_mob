#!/usr/bin/env python3
"""Validate a proposed signed Studio catalog against the currently published one.

This tool does not publish, sign or handle credentials.
"""
import argparse
import base64
import json
import pathlib
import subprocess
import tempfile


def load(path):
    return json.loads(pathlib.Path(path).read_text(encoding="utf-8"))


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--current", required=True)
    p.add_argument("--candidate", required=True)
    p.add_argument("--public-key", required=True, help="Trusted publisher public key PEM")
    args = p.parse_args()
    current = load(args.current)
    candidate = load(args.candidate)
    if current.get("kind") != "mazda6gh-published-settings":
        p.error("Current catalog has an unexpected kind")
    if candidate.get("schema") != 2 or candidate.get("kind") != "mazda6gh-published-settings":
        p.error("Candidate catalog has an unexpected format")
    old = current.get("version")
    new = candidate.get("version")
    if type(old) is not int or type(new) is not int or new <= old:
        p.error("Candidate version must be strictly greater than the published version")
    payload = candidate.get("snapshot_payload")
    if not isinstance(payload, str) or len(payload.encode("utf-8")) > 262144:
        p.error("Invalid snapshot payload")
    snapshot = json.loads(payload)
    if snapshot.get("schema") != 2 or snapshot.get("kind") != "mazda6gh-admin-settings":
        p.error("Invalid admin snapshot")
    stores = snapshot.get("stores")
    if not isinstance(stores, dict) or any(not isinstance(stores.get(k), dict)
        for k in ("run35_settings", "studio_admin_rows", "studio_custom_modules")):
        p.error("Missing required stores")
    message = ("MAZDA6GH-SETTINGS-V2\n" + str(new) + "\n" + payload).encode("utf-8")
    signature = base64.b64decode(candidate["signature"], validate=True)
    with tempfile.TemporaryDirectory() as tmp:
        root = pathlib.Path(tmp)
        (root / "message").write_bytes(message)
        (root / "signature").write_bytes(signature)
        subprocess.run(["openssl", "dgst", "-sha256", "-verify", args.public_key,
                        "-signature", str(root / "signature"), str(root / "message")],
                       check=True)
    print(f"Validated signed catalog transition: v{old} -> v{new}")


if __name__ == "__main__":
    main()
