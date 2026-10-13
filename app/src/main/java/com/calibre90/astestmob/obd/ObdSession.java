package com.calibre90.astestmob.obd;

import android.content.Context;
import java.io.Closeable;
import java.io.IOException;

/** Process-wide, read-only adapter session. Never persist a connected flag across process restarts. */
public final class ObdSession {
  private static Closeable transport;
  private static boolean adapterReady;
  private static boolean vehicleReady;
  private static String vin = "";
  private static final StringBuilder journal = new StringBuilder();
  public static synchronized void log(String line) {
    journal.append(line).append("\n");
    if (journal.length() > 18000) journal.delete(0, journal.length() - 12000);
  }
  public static synchronized String journal() { return journal.toString(); }
  private ObdSession() {}
  public static synchronized boolean adapterReady() { return adapterReady && transport != null; }
  public static synchronized boolean vehicleReady() { return adapterReady() && vehicleReady; }
  public static synchronized String vin() { return vin; }
  public static synchronized void attach(Closeable connection) throws IOException {
    if (connection == null) throw new IllegalArgumentException("Missing transport");
    disconnect();
    transport = connection;
    adapterReady = true;
    vehicleReady = false;
    vin = "";
  }
  public static synchronized void confirmVehicle(String detectedVin) {
    if (!adapterReady()) return;
    vehicleReady = true;
    vin = detectedVin == null ? "" : detectedVin;
  }
  public static synchronized void disconnect() {
    Closeable old = transport;
    transport = null;
    adapterReady = false;
    vehicleReady = false;
    vin = "";
    if (old != null) try { old.close(); } catch (IOException ignored) {}
  }
  public static synchronized String command(String ascii) throws IOException {
    if (!adapterReady()) throw new IOException("ELM не подключён");
    try {
      if (transport instanceof ElmWifiClient) return ((ElmWifiClient) transport).command(ascii);
      if (transport instanceof ElmBluetoothClient) return ((ElmBluetoothClient) transport).command(ascii);
      throw new IOException("Неподдерживаемый транспорт");
    } catch (IOException ex) {
      // A failed transport must not remain marked as an active vehicle session.
      disconnect();
      throw ex;
    }
  }
  public static synchronized String status() {
    if (!adapterReady()) return "Адаптер не подключён";
    if (!vehicleReady) return "ELM подключён; автомобиль не подтверждён";
    return vin.isEmpty() ? "Автомобиль ответил; модули не опрошены" :
        "Автомобиль ответил; VIN: " + vin + "; модули не опрошены";
  }
}
