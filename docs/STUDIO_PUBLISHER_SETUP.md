# Studio online publication: owner setup

The app intentionally rejects all online catalogs until a dedicated publisher
RSA public key and a fixed HTTPS URL are configured in the client.

## 1. Generate the dedicated publisher key pair (once)

On a trusted computer with OpenSSL installed:

```sh
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out studio-publisher-private.pem
openssl pkey -in studio-publisher-private.pem -pubout -out studio-publisher-public.pem
openssl pkey -pubin -in studio-publisher-public.pem -outform DER | openssl base64 -A
```

Keep `studio-publisher-private.pem` **outside GitHub repositories, APKs and
shared folders**. Back it up securely. The last command produces the Base64
X.509 public key for `CatalogSignature.CATALOG_PUBLIC_KEY_BASE64`.

## 2. Configure protected signing in GitHub

In repository Settings → Environments create `studio-publication`.
Require approval and restrict allowed deployment branches where supported.
Add environment secret `STUDIO_CATALOG_SIGNING_KEY_PEM` containing the full
private PEM file, including BEGIN/END lines. Never paste the private key in
chat, issues, workflow inputs or commits.

## 3. Review the admin export

Export settings from the **standalone administrator** app. Check the JSON for
unexpected data. Save the approved file to
`publication/admin-settings.json` in a restricted source branch. Do not use
an export containing personal data. The export must have schema 2 and kind
`mazda6gh-admin-settings`.

Use Actions → Prepare signed Studio catalog → Run workflow with a strictly
increasing version. Download the `studio-published-settings` artifact and
verify the publication before distributing it. The current workflow **only
creates an artifact**, not a public release.

## 4. Publish and enable client updates

Publish the reviewed signed JSON to an owner-controlled **public HTTPS URL**
whose contents are the exact artifact. GitHub Pages or a public raw file can
serve the catalog. A private repository raw URL requiring authentication will
not work in the client.

Configure the exact HTTPS address in `MainActivity.FULL_CATALOG_URL` and
the public key in `CatalogSignature.CATALOG_PUBLIC_KEY_BASE64`, then build
and test the client APK. Never include a GitHub access token in the APK.

For every update, increase the integer version; the client rejects old or
unsigned publications and keeps its last valid signed cache offline. Test on
a disposable device first with a non-production admin export and verify that
a customer's ABT rows are unchanged.

## Current limitations

- The signing workflow does not automatically push the signed JSON to Pages.
- Public key and URL are deliberately empty; no remote sync is enabled yet.
- The signed online catalog and client rendering require end-to-end phone tests.
- The Run #303 control build must not be modified.
