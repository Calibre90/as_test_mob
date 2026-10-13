package com.calibre90.astestmob.obd;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/** Parses standard OBD-II mode 03 stored DTC responses, without clearing codes. */
public final class ObdDtcParser {
  private ObdDtcParser() {}

  public static List<String> parse(String raw) {
    LinkedHashSet<String> codes = new LinkedHashSet<>();
    if (raw == null) return new ArrayList<>(codes);
    for (String line : raw.toUpperCase(Locale.US).split("[\\r\\n]+")) {
      String hex = line.trim().replaceAll("^(?:[0-9A-F]{3}|[0-9A-F]{8})\\s+", "")
          .replaceAll("^[0-9A-F]:\\s*", "").replaceAll("\\s+", "");
      if (!hex.matches("[0-9A-F]+")) continue;
      int pos = hex.indexOf("43");
      if (pos < 0 || pos % 2 != 0) continue;
      // Single-frame CAN responses may have a PCI length byte preceding 43.
      int cursor = pos + 2;
      while (cursor + 4 <= hex.length()) {
        int first = Integer.parseInt(hex.substring(cursor, cursor + 2), 16);
        int second = Integer.parseInt(hex.substring(cursor + 2, cursor + 4), 16);
        cursor += 4;
        if (first == 0 && second == 0) continue;
        char type = "PCBU".charAt((first >>> 6) & 3);
        String code = String.format(Locale.US, "%c%01X%01X%02X", type,
            (first >>> 4) & 3, first & 0x0F, second);
        codes.add(code);
      }
    }
    return new ArrayList<>(codes);
  }
}
