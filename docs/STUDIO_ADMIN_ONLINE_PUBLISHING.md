# Studio Admin: secure online publication contract

## Intended user flow
1. The owner edits modules, rows, features and appearance in the separate Admin APK.
2. The owner taps **Опубликовать онлайн** and authenticates with the publishing service.
3. Admin sends the validated schema-2 AdminSnapshot to the service over HTTPS.
4. The service checks the owner's identity and authorization, size/schema limits, and publication revision.
5. The service triggers an approved GitHub publication workflow using a **server-side GitHub App installation token**. Never embed a GitHub token, GitHub App private key or catalog RSA signing key in an APK.
6. GitHub Actions signs the snapshot with the existing `STUDIO_CATALOG_SIGNING_KEY_PEM` environment secret and publishes `publication/studio-published-settings.json` to the main branch. Publication must be serialized, version strictly increasing and protected by approval/branch rules.
7. Studio clients fetch the existing raw HTTPS URL and verify the RSA signature; customer ABT drafts must not be overwritten.

## Endpoint contract (not yet deployed)
- `POST /v1/catalog/publications`
- `Authorization: Bearer <short-lived owner session token>`
- `Content-Type: application/json`
- Request: `{"snapshot":{...schema-2...},"expectedVersion":3,"requestId":"uuid"}`
- Response: `202 {"publicationId":"...","status":"queued"}`
- `GET /v1/catalog/publications/{publicationId}` returns `queued|signing|published|failed` and published version.
- Reject unauthorized users (401/403), invalid snapshot (400), version conflict (409), payload too large (413), and rate limits (429).
- Do not log authorization headers, owner tokens, or raw private key material.
- Snapshot must be validated both at the service and in GitHub Actions; GitHub should use a protected environment with review requirements.
- A client-side success notification is permitted only after the service reports `published`, not merely after a request is accepted.

## Required setup before enabling Android upload
- Choose/deploy an HTTPS publishing backend and its owner login method.
- Install a GitHub App with minimum required repository permissions; keep its private key on the backend.
- Implement the authenticated dispatch + signing/publishing workflow, with branch protection and monotonic version checks.
- Add the backend base URL to Admin; never hardcode a reusable credential.
- Test unauthorized submissions, replay, duplicate request IDs, rollback attempts, tampered snapshots, and Studio RSA verification.

## Current state
The Admin button currently validates and previews the local snapshot but **does not upload**. Existing manual signed catalog workflow remains the functioning publication path. Run #303 is untouched.
