# Mazda 6 GH OBD Studio — full diagnostic scope

Development branch: `obd-studio`, forked from `release-post476`. Preserve production release and workflow unmodified.

## Modules and feature gates
1. Adapter and vehicle connection: Wi-Fi TCP first, Bluetooth Classic SPP next, USB OTG later; adapter capability detection and connection log.
2. Module discovery: discover only responding ECUs across confirmed supported CAN networks, label module and network, collect IDs and supported capabilities; gracefully handle partial scans.
3. DTC: read stored/pending/permanent codes where supported, report module/status/freeze frame when supported; clearing DTC requires a separate confirmation and warning.
4. Live Data: standards-based OBD-II PIDs (RPM 0C, coolant 05, engine load 04, vehicle speed 0D, fuel trims 06/07/08/09, supported oxygen sensor PIDs) with capability checks; manufacturer-specific Mazda PIDs only after validation. Show units, refresh rate, sensor support, min/max, plots and CSV export.
5. ECU information: supported calibration IDs, VIN if available, software/hardware IDs and supported monitor information; privacy-conscious storage.
6. Active tests and service: strict allowlist by exact module ID, protocol, firmware, supported procedure and preconditions. No universal 'test everything' promise; reject unknown procedures.
7. As-Built: reuse existing editor for offline ABT operations. For online operation add ECU-specific verified read/backup/validation; hide HEX in normal mode, show named features. Do not permit writing until explicit compatibility verification, backup, power preflight, confirmation and post-write readback. Keep safety-critical systems read-only until separately verified.
8. Offline demo/simulator with test fixtures; automated protocol parsing, timeouts, cancellation, DTC and PID tests.

## Suggested navigation
Connection | Diagnostics | Live Data | Modules | Service | As-Built | Logs

## Release sequence
- V0.1 independent Android application ID, separate OBD APK workflow checked out from `obd-studio`; UI connection screen and mock adapter.
- V0.2 real Wi-Fi ELM327 transport, AT handshake, standard OBD-II vehicle ID and read-only PIDs.
- V0.3 DTC scan/clear with confirmation, plots, exports.
- V0.4 verified Mazda module discovery and supported diagnostics.
- V0.5 validated service operations and As-Built read with backup.
- V1.0 selective As-Built write only after vehicle-specific validation.

No claim that all FORScan features can be reproduced with arbitrary ELM327 clones.
