package com.calibre90.astestmob.obd;

import java.util.Locale;

/** Strict parser for single-frame standard OBD-II mode 01 responses. */
public final class ObdPidParser {
  private ObdPidParser() {}

  public static Double decode(String raw, String request) {
    if (raw == null || request == null || !request.matches("01[0-9A-Fa-f]{2}")) return null;
    String pid = request.substring(2).toUpperCase(Locale.US);
    String marker = "41" + pid;
    for (String line : raw.toUpperCase(Locale.US).split("[\\r\\n]+")) {
      // Strip a CAN ID or ELM line counter only when separated by whitespace.
      String cleaned = line.trim().replaceAll("^(?:[0-9A-F]{3}|[0-9A-F]{8})\\s+", "")
          .replaceAll("^[0-9A-F]:\\s*", "")
          .replaceAll("\\s+", "");
      if (!cleaned.matches("[0-9A-F]+")) continue;
      int at = cleaned.indexOf(marker);
      if (at < 0 || (at % 2) != 0) continue;
      String bytes = cleaned.substring(at + marker.length());
      int needed = "0C".equals(pid) ? 4 : 2;
      if (bytes.length() < needed) continue;
      try {
        int a = Integer.parseInt(bytes.substring(0, 2), 16);
        switch (pid) {
          case "05": return (double) a - 40;
          case "0D": return (double) a;
          case "04": return a * 100.0 / 255.0;
          case "0C":
            int b = Integer.parseInt(bytes.substring(2, 4), 16);
            return (a * 256.0 + b) / 4.0;
          default: return null;
        }
      } catch (NumberFormatException ignored) {}
    }
    return null;
  }
}
