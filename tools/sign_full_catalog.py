#!/usr/bin/env python3
"""Create a signed Studio publication from a locally exported admin JSON.

Requires Python 3 and OpenSSL on the trusted publisher machine. Never ship
the RSA private key, a GitHub token, or this signing environment in an APK.
"""
import argparse
import base64
import json
import pathlib
import subprocess
import tempfile


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True, help="Admin export JSON")
    parser.add_argument("--output", required=True, help="Signed publication JSON")
    parser.add_argument("--version", required=True, type=int)
    parser.add_argument("--private-key", required=True, help="Publisher RSA PEM path")
    args = parser.parse_args()

    if args.version < 1:
        parser.error("Version must be positive")
    source = pathlib.Path(args.input).read_text(encoding="utf-8")
    snapshot = json.loads(source)
    if snapshot.get("schema") != 2 or snapshot.get("kind") != "mazda6gh-admin-settings":
        parser.error("Expected a schema-2 admin settings export")
    stores = snapshot.get("stores")
    if not isinstance(stores, dict) or any(not isinstance(stores.get(k), dict) for k in
        ("run35_settings", "studio_admin_rows", "studio_custom_modules")):
        parser.error("Missing required settings stores")
    if len(source) > 262144:
        parser.error("Settings payload exceeds the client size limit")

    message = ("MAZDA6GH-SETTINGS-V2\n" + str(args.version) + "\n" + source).encode("utf-8")
    with tempfile.TemporaryDirectory() as directory:
        message_file = pathlib.Path(directory) / "message.bin"
        signature_file = pathlib.Path(directory) / "signature.bin"
        message_file.write_bytes(message)
        subprocess.run(["openssl", "dgst", "-sha256", "-sign", args.private_key,
                        "-out", str(signature_file), str(message_file)], check=True)
        signature = base64.b64encode(signature_file.read_bytes()).decode("ascii")

    envelope = {
        "schema": 2,
        "kind": "mazda6gh-published-settings",
        "version": args.version,
        "snapshot_payload": source,
        "signature": signature,
    }
    pathlib.Path(args.output).write_text(
        json.dumps(envelope, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("Signed publication created:", args.output)


if __name__ == "__main__":
    main()
