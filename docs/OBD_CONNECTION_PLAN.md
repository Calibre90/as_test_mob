# Mazda 6 GH OBD Studio — connection implementation

Baseline: branch `obd-studio` created from `release-post476`. Never modify `release-post476` or its Post 476 #9 release workflow.

## Reference behavior
FORScan Lite Android user manual: https://docs.xn--80aajcuv3afm.xn--p1ai/installation/elm/forscan/manuals/documentation-android/

## Phase 1: transport + read-only discovery
- Dedicated connection screen with Wi-Fi TCP, Bluetooth Classic SPP and (later) USB OTG; remember adapter selection per device.
- Wi-Fi: user enters host/port (manual's example 192.168.0.10:35000, configurable, NOT hardcoded). Bind Android process/socket to Wi-Fi network where needed; do not require Internet access.
- Connection state machine: DISCONNECTED -> CONNECTING_ADAPTER -> IDENTIFYING_ADAPTER -> CONNECTING_VEHICLE -> SCANNING_MODULES -> CONNECTED; explicit ERROR, CANCELLED and DISCONNECT.
- Show progress, elapsed time, actionable errors and a timestamped connection log with sensitive data redaction.
- Identify adapter and capabilities first. Validate ELM-compatible AT replies and supported CAN commands. Reject unsupported adapters with a clear message; do not assume every ELM clone supports MS-CAN.
- Read-only vehicle identification and module discovery via verified, model-specific diagnostic requests. Never infer module presence from a static catalog. Record module IDs, responding network and protocol, supported capabilities, read errors.
- HS/MS-CAN modes: auto when adapter capability is verified; manual switch with an explicit user prompt; HS-only. No unsupported auto-switch claims.
- Save a local read-only vehicle profile, with VIN shown only with user consent and no transmission by default.
- Android: permissions and networking for supported OS versions; lifecycle, timeouts, reconnect and cancellation tests.
- Add a mock adapter/ECU fixture to exercise successful, partial, timeout and malformed-response scenarios without a car.

## Phase 2: safe As-Built read
- Build a separate diagnostic transport and protocol stack. Existing ABT codec and feature engine must not be treated as vehicle-writing protocols.
- Read raw configuration only from positively identified compatible modules. Keep a verified, timestamped immutable backup; validate length/checksums and module identity.
- Keep HEX and address mapping hidden in normal UI, but available in restricted diagnostic logs/admin tools.

## Phase 3: write (NOT ENABLED until verified)
- Do not expose write controls until module-specific security/session/transport and exact Mazda 6 GH 2008 configuration semantics are confirmed on real hardware.
- Require user confirmation, stable voltage and communication, supported adapter, complete backup, strict allowlist and post-write readback/verification. Reject partial or unknown module configurations.
- RVM, Keyless, AFS and other options may require hardware; never claim an option was activated based solely on a bit change.
- ABS/other safety-critical modules stay read-only until separately validated.

## Independent builds
- New workflow must check out `obd-studio` explicitly. Existing `build-release-post476.yml` checks out `release-post476` intentionally and must remain untouched.
- New Android application ID is required to coexist with existing Studio. Avoid sharing production signing or cloud publication endpoints by default.
