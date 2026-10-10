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

## Signed envelope (schema 1)

The signing tool produces `features_payload` containing the exact compact JSON array as a string and `signature` as base64. The signature input is UTF-8 bytes of:

```
MAZDA6GH-CATALOG-V1\\n<version>\\n<features_payload>
```

Here `\\n` denotes a newline character, not a backslash followed by n. The consumer verifies that exact payload before parsing the functions from `features_payload`; the outer `features` array is informational only and must **not** be used by the app.

Generate a dedicated RSA-2048 or stronger key pair outside the repository, then set only the public X.509 DER key in `CatalogSignature.CATALOG_PUBLIC_KEY_BASE64`. Keep the private PEM in secure owner-controlled storage and never commit it.

Example local commands:

```sh
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out catalog-private.pem
openssl pkey -in catalog-private.pem -pubout -outform DER | openssl base64 -A
python3 catalog/sign_catalog.py --catalog catalog/catalog.json --private-key catalog-private.pem --output signed-catalog.json
```

The first command creates a sensitive private key: run locally in a secure location and never upload the PEM to GitHub. Publishing remains disabled until the URL, public key, and release tests are configured.
