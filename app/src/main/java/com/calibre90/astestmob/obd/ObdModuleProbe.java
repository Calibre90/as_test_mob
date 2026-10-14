package com.calibre90.astestmob.obd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Conservative read-only physical ECU probing for Mazda 6 GH.
 * Address mappings are candidates, NOT a verified exhaustive module map.
 * Never send service 10, 2E, 27, 31, 34, 36 or configuration writes.
 */
public final class ObdModuleProbe {
  public interface Commands { String command(String text) throws IOException; }
  public static final class Result {
    public final String name, requestId, responseId, status, response;
    Result(String n,String req,String resp,String s,String raw) {
      name=n;requestId=req;responseId=resp;status=s;response=raw;
    }
  }
  private static final String[][] HS_CANDIDATES = {
    {"PCM","7E0","7E8"},
    {"TCM","7E1","7E9"},
    {"ABS","760","768"},
    {"IC","720","728"},
    {"BCM (HS кандидат)","726","72E"},
    {"RKE (HS кандидат)","731","739"},
    {"HVAC/EATC (HS кандидат)","733","73B"}
  };
  private ObdModuleProbe() {}
  public static List<Result> probeHsCan(Commands link) throws IOException {
    List<Result> results = new ArrayList<>();
    // 11-bit CAN, 500 kbps. Only query HS-CAN when the adapter is physically on HS.
    String protocol = link.command("ATSP6");
    if (isError(protocol)) throw new IOException("ELM rejected HS-CAN protocol: "+protocol);
    try {
      for (String[] ecu:HS_CANDIDATES) {
        String req=ecu[1], resp=ecu[2];
        try {
          // Keep replies constrained to the expected ECU.
          if (isError(link.command("ATSH"+req)) ||
              isError(link.command("ATCRA"+resp))) {
            results.add(new Result(ecu[0],req,resp,"Настройка адреса не поддерживается",""));
            continue;
          }
          // UDS/ISO 14229 ReadDataByIdentifier F190 (VIN), read-only.
          // Negative response 7F 22 is NOT proof that this ECU exists.
          String raw=link.command("22F190");
          // A negative response to VIN DID confirms ECU communication; NO DATA does not.
          // Do not send potentially state-changing session/control commands.
          String normalized=raw.toUpperCase(Locale.US).replaceAll("[^0-9A-F]","");
          boolean positive=normalized.contains("62F190");
          // A correctly addressed negative response (7F 22 xx) also proves
          // that a diagnostic ECU answered, even when VIN DID is unsupported.
          boolean negative=normalized.matches("(?s).*7F22(11|12|13|22|31|33|78).*");
          // F187 = ECU spare-part number (read-only UDS DID). Try it only
          // when VIN DID gave no diagnostic answer; keep both raw replies.
          if (!positive && !negative) {
            String spare = link.command("22F187");
            String spareHex = spare == null ? "" : spare.toUpperCase(Locale.US).replaceAll("[^0-9A-F]", "");
            if (spareHex.contains("62F187")) {
              positive = true;
              raw = "F190: " + raw + " | F187: " + spare;
            } else if (spareHex.matches("(?s).*7F22(11|12|13|22|31|33|78).*")) {
              negative = true;
              raw = "F190: " + raw + " | F187: " + spare;
            } else {
              raw = "F190: " + raw + " | F187: " + spare;
            }
          }
          String status=positive ? (normalized.contains("62F190") ? "Ответ подтверждён (VIN DID)" : "Ответ подтверждён (DID F187)") :
              negative ? "Блок ответил: VIN DID не поддержан/отклонён" : "Не подтверждён";
          results.add(new Result(ecu[0],req,resp,status,raw));
        } catch(IOException ex) {
          results.add(new Result(ecu[0],req,resp,"Нет ответа: "+ex.getMessage(),""));
        }
      }
    } finally {
      try { link.command("ATCRA"); } catch(Exception ignored) {}
      try { link.command("ATSH7DF"); } catch(Exception ignored) {}
      try { link.command("ATSP0"); } catch(Exception ignored) {}
    }
    return results;
  }
  /**
   * Experimental Mazda/Ford 11-bit MS-CAN address candidates, not a verified
   * GH module map. Read-only DID F190 only; no broadcast writes or coding.
   */
  public static List<Result> probeMsCan(Commands link) throws IOException {
    final String[][] candidates = {
      {"BCM (кандидат)", "726", "72E"},
      {"RKE (кандидат)", "731", "739"},
      {"HVAC/EATC (кандидат)", "733", "73B"}
    };
    List<Result> results = new ArrayList<>();
    try {
      for (String[] ecu : candidates) {
        String request = ecu[1], response = ecu[2];
        try {
          String header = link.command("ATSH" + request);
          String filter = link.command("ATCRA" + response);
          if (isError(header) || isError(filter)) {
            results.add(new Result(ecu[0], request, response, "ELM отклонил адрес/фильтр", header + " | " + filter));
            continue;
          }
          String raw = link.command("22F190");
          String normalized = raw == null ? "" : raw.toUpperCase(Locale.US).replaceAll("[^0-9A-F]", "");
          boolean positive = normalized.contains("62F190");
          boolean negative = normalized.matches("(?s).*7F22(11|12|13|22|31|33|78).*");
          String state = positive ? "Получен положительный ответ (кандидат адреса)" :
              negative ? "Получен отрицательный ответ ECU (кандидат адреса)" :
              "Ответ не подтверждён (не означает отсутствие блока)";
          results.add(new Result(ecu[0], request, response, state, raw));
        } catch (IOException e) {
          results.add(new Result(ecu[0], request, response, "Ошибка связи: " + e.getMessage(), ""));
          if (e.getMessage() != null && e.getMessage().contains("connection")) throw e;
        }
      }
    } finally {
      try { link.command("ATCRA"); } catch (Exception ignored) {}
      try { link.command("ATSH7DF"); } catch (Exception ignored) {}
    }
    return results;
  }

  /** Decode an ELM ISO-TP numbered reply containing positive DID F190. */
  public static String decodeVin(String raw) {
    if (raw == null) return null;
    StringBuilder payload = new StringBuilder();
    boolean found = false;
    for (String segment : raw.toUpperCase(Locale.US).split("[\\r\\n|]+")) {
      String line = segment.trim();
      int colon = line.indexOf(':');
      if (colon >= 0 && line.substring(0, colon).trim().matches("[0-9A-F]+"))
        line = line.substring(colon + 1);
      String hex = line.replaceAll("[^0-9A-F]", "");
      int marker = hex.indexOf("62F190");
      if (marker >= 0) {
        payload.setLength(0);
        payload.append(hex.substring(marker + 6));
        found = true;
      } else if (found && colon >= 0) {
        payload.append(hex);
      }
      if (payload.length() >= 34) break;
    }
    if (!found || payload.length() < 34) return null;
    StringBuilder vin = new StringBuilder(17);
    for (int i = 0; i < 34; i += 2)
      vin.append((char) Integer.parseInt(payload.substring(i, i + 2), 16));
    String result = vin.toString();
    return result.matches("[A-HJ-NPR-Z0-9]{17}") ? result : null;
  }

  private static boolean isError(String response) {
    if(response==null)return true;
    String s=response.toUpperCase(Locale.US);
    return s.contains("?")||s.contains("ERROR")||s.contains("UNABLE");
  }
}
