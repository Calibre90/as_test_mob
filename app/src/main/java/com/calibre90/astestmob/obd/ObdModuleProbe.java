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
          // Read-only identification DIDs; preserve each reply independently.
          // 7F 22 31 is a real negative diagnostic response, not a timeout.
          String vinReply = link.command("22F190");
          String raw = "F190: " + vinReply;
          String positiveDid = positiveDid(vinReply, "F190") ? "F190" : null;
          boolean negative = negativeDid(vinReply);
          // Additional read-only identification DIDs. They can be unsupported;
          // that does not mean the ECU is absent.
          if (positiveDid == null) {
            String spare = link.command("22F187");
            raw += " | F187: " + spare;
            if (positiveDid(spare, "F187")) positiveDid = "F187";
            negative |= negativeDid(spare);
          }
          if (positiveDid == null) {
            String serial = link.command("22F18C");
            raw += " | F18C: " + serial;
            if (positiveDid(serial, "F18C")) positiveDid = "F18C";
            negative |= negativeDid(serial);
          }
          String status = positiveDid != null
              ? "Диагностический ответ подтверждён (DID " + positiveDid + ")"
              : negative ? "ECU ответил отказом на запрос идентификации"
              : "Не подтверждён (нет диагностического ответа)";
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
          String f190 = link.command("22F190");
          String raw = "F190: " + f190;
          String matched = positiveDid(f190, "F190") ? "F190" : null;
          boolean rejected = negativeDid(f190);
          // Read-only fallbacks mirror HS-CAN discovery; each DID is optional.
          if (matched == null) {
            String f187 = link.command("22F187");
            raw += " | F187: " + f187;
            if (positiveDid(f187, "F187")) matched = "F187";
            rejected |= negativeDid(f187);
          }
          if (matched == null) {
            String f18c = link.command("22F18C");
            raw += " | F18C: " + f18c;
            if (positiveDid(f18c, "F18C")) matched = "F18C";
            rejected |= negativeDid(f18c);
          }
          String state = matched != null
              ? "Диагностический ответ подтверждён (DID " + matched + "; адрес-кандидат)"
              : rejected ? "Получен отрицательный ответ ECU (адрес-кандидат)"
              : "Ответ не подтверждён (не означает отсутствие блока)";
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

  private static boolean positiveDid(String response, String did) {
    return response != null && response.toUpperCase(Locale.US)
        .replaceAll("[^0-9A-F]", "").contains("62" + did);
  }

  private static boolean negativeDid(String response) {
    return response != null && response.toUpperCase(Locale.US)
        .replaceAll("[^0-9A-F]", "")
        .matches("(?s).*7F22(10|11|12|13|21|22|24|31|33|35|36|37|78).*");
  }

  private static boolean isError(String response) {
    if(response==null)return true;
    String s=response.toUpperCase(Locale.US);
    return s.contains("?")||s.contains("ERROR")||s.contains("UNABLE");
  }
}
