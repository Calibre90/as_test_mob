#!/usr/bin/env python3
"""Local integration test: real RSA signature, signed payload, and tamper rejection."""
import base64
import json
import pathlib
import subprocess
import sys
import tempfile
from validate_catalog import validate_catalog

ROOT=pathlib.Path(__file__).resolve().parent

def run(*args):
    subprocess.run(args,check=True,stdout=subprocess.DEVNULL)

def main():
    with tempfile.TemporaryDirectory() as d:
        d=pathlib.Path(d)
        key=d/"private.pem"
        pub=d/"public.pem"
        source=d/"draft.json"
        signed=d/"signed.json"
        payload=d/"payload.bin"
        signature=d/"signature.bin"
        run("openssl","genpkey","-algorithm","RSA","-pkeyopt","rsa_keygen_bits:2048","-out",str(key))
        run("openssl","pkey","-in",str(key),"-pubout","-out",str(pub))
        draft={"schema":1,"version":7,"features":[{
            "id":"test_only","module":"IC","row":"720-01-01","mode":"HEX",
            "indices":"0","on":"8","off":"0","byte":0,"label":"Signature test only"
        }]}
        validate_catalog(draft)
        source.write_text(json.dumps(draft),encoding="utf-8")
        run(sys.executable,str(ROOT/"sign_catalog.py"),"--catalog",str(source),
            "--private-key",str(key),"--output",str(signed))
        doc=json.loads(signed.read_text(encoding="utf-8"))
        payload.write_bytes(("MAZDA6GH-CATALOG-V1\n"+str(doc["version"])+"\n"+doc["features_payload"]).encode("utf-8"))
        signature.write_bytes(base64.b64decode(doc["signature"]))
        run("openssl","dgst","-sha256","-verify",str(pub),"-signature",str(signature),str(payload))
        payload.write_bytes(payload.read_bytes()+b"tampered")
        tampered=subprocess.run(["openssl","dgst","-sha256","-verify",str(pub),
            "-signature",str(signature),str(payload)],capture_output=True)
        if tampered.returncode==0:
            raise AssertionError("Tampered payload was accepted")
        for broken in (
            dict(draft, features=[]),
            dict(draft, features=[dict(draft["features"][0], row="731-01-01")]),
            dict(draft, features=[dict(draft["features"][0], indices="0,0")]),
        ):
            try:
                validate_catalog(broken)
            except ValueError:
                continue
            raise AssertionError("Invalid catalog passed validation")
        print("PASS: RSA signature, tamper rejection, and draft validation")

if __name__=="__main__":
    main()
