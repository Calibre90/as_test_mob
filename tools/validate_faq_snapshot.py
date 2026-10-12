#!/usr/bin/env python3
"""Validate optional FAQ records in a schema-2 Studio snapshot.

Does not publish, sign, or read secrets.
"""
import json
import pathlib
import sys

def validate(snapshot):
    stores = snapshot.get("stores")
    if not isinstance(stores, dict):
        raise ValueError("Missing stores")
    faq = stores.get("studio_faq")
    if faq is None:
        return 0  # Older publications without FAQ remain valid.
    if not isinstance(faq, dict) or set(faq) != {"entries"}:
        raise ValueError("FAQ store must contain only entries")
    raw = faq["entries"]
    if not isinstance(raw, str) or len(raw) > 262144:
        raise ValueError("Invalid FAQ entries encoding")
    entries = json.loads(raw)
    if not isinstance(entries, list) or len(entries) > 200:
        raise ValueError("Too many FAQ entries")
    for index, entry in enumerate(entries):
        if not isinstance(entry, dict) or set(entry) != {"name", "description"}:
            raise ValueError(f"Invalid FAQ entry {index}")
        name, description = entry["name"], entry["description"]
        if not isinstance(name, str) or not 0 < len(name.strip()) <= 120:
            raise ValueError(f"Invalid FAQ title {index}")
        if not isinstance(description, str) or len(description) > 12000:
            raise ValueError(f"Invalid FAQ description {index}")
    return len(entries)

if __name__ == "__main__":
    try:
        doc = json.loads(pathlib.Path(sys.argv[1]).read_text(encoding="utf-8"))
        count = validate(doc)
        print(f"FAQ validated: {count} entries")
    except (ValueError, OSError, IndexError) as exc:
        raise SystemExit(str(exc))
