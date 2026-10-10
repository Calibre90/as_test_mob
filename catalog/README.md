# Mazda 6 GH published feature catalog (draft)

The consumer app uses `StudioSettings` for local administrator drafts and a separate cached, versioned published catalog. This folder contains the **publication source**, not a customer's vehicle ABT.

## Format

`catalog.json` uses `schema: 1`, a monotonically increasing integer `version`, and `features` (an array of at most 500 definitions). Each definition follows the existing StudioSettings format:

```json
{
  "id": "example_only",
  "module": "IC",
  "row": "720-01-01",
  "mode": "HEX",
  "indices": "0",
  "on": "8",
  "off": "0",
  "byte": 0,
  "label": "Example — not a verified Mazda configuration"
}
```

The sample above is **documentation only**. No Mazda option address or value should be published until checked against actual compatible vehicle configurations.

## Publishing rules

1. Prepare a draft in a private administrator workflow, validate HEX/BITS positions and the target block, and test on a representative ABT. Never upload a user's ABT to this repository.
2. Review and commit a new `catalog.json` with an increased version number.
3. Distribute it from a stable owner-controlled HTTPS endpoint only after adding **cryptographic signature verification** in the consumer application. HTTPS alone does not protect against a compromised publishing account.
4. The consumer retains its last valid cached catalog for offline use. Rollbacks must be implemented as a new, higher catalog version rather than lowering `version`.
5. The current `CatalogSync.CATALOG_URL` is intentionally empty; automatic remote updates are **not enabled yet**.

Do not put license private keys, signing secrets, admin passwords, or device identifiers into published JSON, Android source, or GitHub Actions logs.

## Current status

- Initial catalog is empty to avoid distributing unverified Mazda configuration data.
- The legacy built-in functions are preserved in the Android application.
- Separate admin Android app, authentication, signature verification, deployment URL, and end-to-end release testing are still pending.
