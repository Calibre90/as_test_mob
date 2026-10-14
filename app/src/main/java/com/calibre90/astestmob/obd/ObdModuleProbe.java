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
  // ISO 14229 identification range. Support is ECU-specific, not guaranteed.
  private static final String[] EXTENDED_IDENTIFICATION_DIDS = new String[32];
  static {
    for (int i = 0; i < 32; i++)
      EXTENDED_IDENTIFICATION_DIDS[i] = String.format(Locale.US, "F1%02X", 0x80 + i);
  }

  /** Bounded, read-only DID survey for IC address candidate on selected bus. */
  public static Result probeIcExtended(Commands link, boolean msCan) throws IOException {
    String name = msCan ? "IC (MS-CAN, расширенный DID)" : "IC (HS-CAN, расширенный DID)";
    StringBuilder raw = new StringBuilder();
    int positive = 0, negative = 0, noData = 0;
    String first = null;
    try {
      String header = link.command("ATSH720");
      String filter = link.command("ATCRA728");
      if (isError(header) || isError(filter))
        return new Result(name, "720", "728", "ELM отклонил адрес/фильтр", header + " | " + filter);
      // Show CAN response headers to distinguish a real ECU reply from ELM NO DATA.
      String headers = link.command("ATH1");
      if (isError(headers)) throw new IOException("ELM не поддерживает ATH1: " + headers);
      int tested = 0;
      for (String did : EXTENDED_IDENTIFICATION_DIDS) {
        if (Thread.currentThread().isInterrupted()) throw new IOException("Опрос прерван");
        String reply = link.command("22" + did);
        tested++;
        if (raw.length() > 0) raw.append(" | ");
        raw.append(did).append(": ").append(reply.replaceAll("[\\r\\n]+", " ").trim());
        if (positiveDid(reply, did)) {
          positive++;
          if (first == null) first = did;
        } else if (negativeDid(reply)) {
          negative++;
        } else {
          noData++;
        }
        // After eight unanswered reads there is no evidence of an ECU at
        // this candidate address. Do not flood the same address with 24 more DIDs.
        if (tested == 8 && positive == 0 && negative == 0) {
          raw.append(" | ОСТАНОВЛЕНО: 8 DID без ответа; проверьте CAN-адрес и шину");
          break;
        }
      }
    } finally {
      try { link.command("ATCRA"); } catch (Exception ignored) {}
      try { link.command("ATSH7DF"); } catch (Exception ignored) {}
    }
    String status = "DID F180–F19F: положительных " + positive
        + ", отказов " + negative + ", без подтверждения " + noData
        + (first == null ? "; адрес IC не подтверждён, дальнейший опрос требует проверки адреса/шины" : "; первый ответ " + first);
    return new Result(name, "720", "728", status, raw.toString());
  }
  /**
   * MS-CAN address discovery diagnostic: send only to existing IC candidate
   * 720, but do not constrain the response to 728. A response from another
   * address can then be recorded with ATH1; this is NOT a bus-wide scan.
   */
  public static Result probeIcUnfilteredMs(Commands link) throws IOException {
    StringBuilder raw = new StringBuilder();
    int positives = 0, negatives = 0;
    String resetMode = "";
    try {
      // ATCRA without an address returns '?' on some ELM327 clones.
      // ATAR asks ELM to restore automatic receive addressing.
      String headers = link.command("ATH1");
      resetMode = link.command("ATAR");
      String request = link.command("ATSH720");
      raw.append("SETUP ATH1=").append(headers.replaceAll("[\\r\\n]+", " ").trim())
          .append("; ATAR=").append(resetMode.replaceAll("[\\r\\n]+", " ").trim())
          .append("; ATSH720=").append(request.replaceAll("[\\r\\n]+", " ").trim());
      if (isError(headers) || isError(resetMode) || isError(request))
        return new Result("IC MS-CAN (автоматический приём)", "720", "авто",
            "ELM не принял настройку; проверка DID не выполнена", raw.toString());
      for (String did : new String[]{"F190", "F187", "F18C"}) {
        if (Thread.currentThread().isInterrupted()) throw new IOException("Опрос прерван");
        String response = link.command("22" + did);
        raw.append(" | ").append(did).append(": ")
            .append(response.replaceAll("[\\r\\n]+", " ").trim());
        if (positiveDid(response, did)) positives++;
        else if (negativeDid(response)) negatives++;
      }
    } finally {
      try { link.command("ATH0"); } catch (Exception ignored) {}
      try { link.command("ATAR"); } catch (Exception ignored) {}
      try { link.command("ATSH7DF"); } catch (Exception ignored) {}
    }
    String status = "Автоматический приём ATAR: положительных " + positives
        + ", отказов " + negatives
        + "; при NO DATA адрес IC всё ещё не подтверждён";
    return new Result("IC MS-CAN (автоматический приём)", "720", "авто", status, raw.toString());
  }

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
          // Extra non-mutating identification reads only for the elusive IC.
          // These are exploratory DIDs, not proof of the ECU's bus or address.
          if ("IC".equals(ecu[0]) && positiveDid == null) {
            for (String did : new String[]{"F180", "F181", "F188"}) {
              String extra = link.command("22" + did);
              raw += " | " + did + ": " + extra;
              if (positiveDid(extra, did)) { positiveDid = did; break; }
              negative |= negativeDid(extra);
            }
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
   * GH module map. Read-only identification DIDs F190/F187/F18C; no broadcast writes or coding.
   */
  public static List<Result> probeMsCan(Commands link) throws IOException {
    final String[][] candidates = {
      {"IC (кандидат MS-CAN)", "720", "728"},
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
          // Same conservative IC-only fallback on the physically selected MS bus.
          if (ecu[0].startsWith("IC ") && matched == null) {
            for (String did : new String[]{"F180", "F181", "F188"}) {
              String extra = link.command("22" + did);
              raw += " | " + did + ": " + extra;
              if (positiveDid(extra, did)) { matched = did; break; }
              rejected |= negativeDid(extra);
            }
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
