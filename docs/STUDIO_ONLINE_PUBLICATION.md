# Studio full-configuration online publication (GitHub)

Status: implementation plan; NOT enabled in client builds.

## Existing confirmed baseline
- Work only on `feature/studio-catalog-integration`; preserve Run #303.
- `AdminSnapshot.exportLocal()` creates schema=2, kind=`mazda6gh-admin-settings`.
- Four exported stores: `run35_settings`, `studio_admin_rows`, `studio_custom_modules`, `studio_admin_custom_rows`.
- Admin-only snapshots must never include license keys, device IDs, activation state, or customer ABT files.
- Legacy `CatalogSync` accepts only four built-in modules and feature-only schema=1; do NOT feed it the full snapshot.

## Publication contract (planned)
A public HTTPS GitHub raw file will contain a *signed envelope*, not an editable administrator backup:

```json
{
  "schema": 2,
  "kind": "mazda6gh-published-settings",
  "version": 1,
  "snapshot_payload": "<exact UTF-8 JSON string produced by AdminSnapshot.exportLocal()>",
  "signature": "<base64 RSA SHA256 signature>"
}
```

Signature bytes: UTF-8(`MAZDA6GH-SETTINGS-V2\n` + decimal version + `\n` + exact snapshot_payload).
Do not reserialize snapshot_payload before verifying. Use a separate RSA signing key from the license key. Never embed a signing private key, GitHub write token, or credentials in an APK.

## Required implementation
1. Generate dedicated publisher signing keys in a trusted owner-controlled environment. Only the public key is compiled into Studio.
2. Add a secure publishing workflow (GitHub Actions with protected secrets and explicit owner approval), validating the admin JSON before signing and uploading it.
3. Add `verifyFull()` and a schema=2 client downloader with HTTPS, size limits, rollback protection, strict structure validation and atomic cache update.
4. Apply published features, module metadata, custom modules and row definitions in a separate read-only client configuration layer. Do NOT use `AdminSnapshot.importLocal()` in Studio: that writes administrator preferences and can overwrite user state.
5. Keep user ABT and local working HEX values separate from published row templates. Never overwrite an opened vehicle ABT during an online update.
6. On network/signature/validation failure retain the last known good published configuration and continue offline.
7. Test AFS `737-01-01`, HEX row values, ON/OFF values, module ordering and appearance after version bump; verify downgrade and invalid signature rejection.

## Release gate
Do not turn on the online URL until signature verification, safe merging and Android tests pass.
