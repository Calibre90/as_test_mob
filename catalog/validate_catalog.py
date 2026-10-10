#!/usr/bin/env python3
"""Validate a draft catalog before signing or publishing it."""
import re

MODULES={"IC":"720","BCM":"726","RKE":"731","ABS":"760"}
HEX=re.compile(r"^[0-9A-Fa-f]+$")
ADDRESS=re.compile(r"^([0-9A-Fa-f]{3})-[0-9A-Fa-f]{2}-[0-9A-Fa-f]{2}$")
INDICES=re.compile(r"^[0-9]+(?:,[0-9]+)*$")

def validate_catalog(document):
    if type(document.get("schema")) is not int or document["schema"]!=1:
        raise ValueError("Unsupported catalog schema")
    if type(document.get("version")) is not int or document["version"]<1:
        raise ValueError("Invalid catalog version")
    features=document.get("features")
    if not isinstance(features,list) or not 1<=len(features)<=500:
        raise ValueError("Expected 1..500 published features")
    seen=set()
    for n,feature in enumerate(features):
        if not isinstance(feature,dict):
            raise ValueError(f"Feature {n}: expected object")
        try:
            fid=feature["id"]
            module=feature["module"]
            address=feature["row"]
            mode=feature["mode"]
            indices=feature["indices"]
            on=feature["on"]
            off=feature["off"]
            byte=feature["byte"]
            label=feature["label"]
        except KeyError as ex:
            raise ValueError(f"Feature {n}: missing field {ex}") from ex
        if not isinstance(fid,str) or not 1<=len(fid)<=80 or fid in seen:
            raise ValueError(f"Feature {n}: empty, duplicate or oversized id")
        seen.add(fid)
        if module not in MODULES or not isinstance(address,str):
            raise ValueError(f"Feature {fid}: invalid module/address")
        match=ADDRESS.fullmatch(address)
        if not match or match.group(1).upper()!=MODULES[module]:
            raise ValueError(f"Feature {fid}: address prefix does not match module")
        if mode not in ("HEX","BITS") or not isinstance(indices,str) or not INDICES.fullmatch(indices):
            raise ValueError(f"Feature {fid}: invalid mode/indices")
        if len(indices)>80:
            raise ValueError(f"Feature {fid}: indices too long")
        numbers=[int(i) for i in indices.split(",")]
        if len(numbers)!=len(set(numbers)) or any(i>127 for i in numbers):
            raise ValueError(f"Feature {fid}: duplicate/out-of-range index")
        if type(byte) is not int or not 0<=byte<=63:
            raise ValueError(f"Feature {fid}: invalid byte index")
        if not isinstance(label,str) or not 1<=len(label)<=120 or not label.strip():
            raise ValueError(f"Feature {fid}: invalid label")
        if not isinstance(on,str) or not HEX.fullmatch(on) or len(on)>64:
            raise ValueError(f"Feature {fid}: invalid on value")
        if not isinstance(off,str) or (off and (not HEX.fullmatch(off) or len(off)>64)):
            raise ValueError(f"Feature {fid}: invalid off value")
        if mode=="HEX":
            if len(on)!=len(numbers) or (off and len(off)!=len(numbers)):
                raise ValueError(f"Feature {fid}: HEX value length/index mismatch")
        elif any(i>7 for i in numbers):
            raise ValueError(f"Feature {fid}: BITS mode only supports bit indices 0..7")
    return True
