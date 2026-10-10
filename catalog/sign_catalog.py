#!/usr/bin/env python3
"""Sign a reviewed catalog with a dedicated RSA private key (never commit the key)."""
import argparse
import base64
import json
import pathlib
import subprocess
import tempfile
from validate_catalog import validate_catalog

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--catalog",required=True)
    p.add_argument("--private-key",required=True,help="Path to separate RSA PEM signing key")
    p.add_argument("--output",required=True)
    args=p.parse_args()
    source=json.loads(pathlib.Path(args.catalog).read_text(encoding="utf-8"))
    if source.get("schema")!=1 or not isinstance(source.get("version"),int) or source["version"]<1:
        p.error("Expected schema=1 and positive integer version")
    features=source.get("features")
    if not isinstance(features,list) or not features or len(features)>500:
        p.error("Only reviewed, nonempty feature lists may be signed")
    validate_catalog(source)
    # Sign the exact compact payload bytes, independent of Android JSON reserialization.
    compact=json.dumps(features,ensure_ascii=False,separators=(",",":"))
    payload=("MAZDA6GH-CATALOG-V1\n"+str(source["version"])+"\n"+compact).encode("utf-8")
    with tempfile.TemporaryDirectory() as d:
        input_path=pathlib.Path(d)/"payload.bin"
        signature_path=pathlib.Path(d)/"signature.bin"
        input_path.write_bytes(payload)
        subprocess.run(["openssl","dgst","-sha256","-sign",args.private_key,"-out",str(signature_path),str(input_path)],check=True)
        signature=base64.b64encode(signature_path.read_bytes()).decode("ascii")
    source["features_payload"]=compact
    source["signature"]=signature
    pathlib.Path(args.output).write_text(json.dumps(source,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print("Signed catalog version",source["version"],"with",len(features),"features")

if __name__=="__main__":
    main()
