# Studio Admin → customer Studio: integration contract

## Frozen administrator baseline
The confirmed working standalone original administrator is frozen at
`e154104cb96a01b0bdd644cdf13baa60e9a97e71` on
`checkpoint/original-admin-working`. Do not modify its four tabs, layout,
save logic, or launcher as part of catalog integration. Run #303 is also untouched.

## What the original administrator actually saves
The original administrator persists data in private Android SharedPreferences,
not in the old draft catalog editor:

- `run35_settings`: `features` JSON array; `module_name_<ID>`,
  `module_version_<ID>`, `appearance_<key>`.
- `studio_admin_rows`: saved ABT row arrays per built-in module.
- `studio_custom_positions`: selected module and scroll positions.
- Additional custom-module preferences are maintained by MainActivity.

A separate Android package cannot read these preferences directly.
A deliberate export/transfer mechanism is necessary. Never assume that a
newly added AFS block or its 737-* rows appear in `catalog/catalog.json`.

## Compatibility constraints
- Catalog v1 currently supports only IC/BCM/RKE/ABS with fixed 720/726/731/760
  prefixes. It does **not** represent arbitrary custom modules (e.g. AFS/737),
  module metadata, ABT rows, or appearance settings.
- Therefore publishing an admin snapshot through v1 would silently lose
  custom blocks and related functions. **Do not do this.**
- Implement a separately versioned, signed complete snapshot format and
  a validator before wiring it into either app. Include modules, rows,
  features, appearance and catalog version.
- Validate module IDs/prefixes and uniqueness; ensure each feature row belongs
  to an existing module; reject collisions and invalid HEX.
- Preserve existing customer ABT files, per-device local drafts and licenses.
  Downloaded data must never overwrite them without an explicit migration policy.
- Reject unsigned/untrusted content; retain the last valid offline snapshot.
  Signing private keys must remain outside APKs and repository.
- Test a full round trip using IC/BCM/RKE/ABS plus a custom AFS (737) block,
  a 737-01-01 row, and an AFS feature before enabling publication.

This is a contract only; no snapshot publishing or customer migration is enabled.
