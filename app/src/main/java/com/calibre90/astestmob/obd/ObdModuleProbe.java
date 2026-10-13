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
    {"IC","720","728"}
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
          String normalized=raw.toUpperCase(Locale.US).replaceAll("[^0-9A-F]","");
          boolean positive=normalized.contains("62F190");
          // A correctly addressed negative response (7F 22 xx) also proves
          // that a diagnostic ECU answered, even when VIN DID is unsupported.
          boolean negative=normalized.matches("(?s).*7F22(11|12|13|22|31|33|78).*");
          String status=positive ? "Ответ подтверждён (VIN DID)" :
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
  private static boolean isError(String response) {
    if(response==null)return true;
    String s=response.toUpperCase(Locale.US);
    return s.contains("?")||s.contains("ERROR")||s.contains("UNABLE");
  }
}
