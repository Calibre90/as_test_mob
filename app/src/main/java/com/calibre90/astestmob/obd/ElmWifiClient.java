package com.calibre90.astestmob.obd;

import java.io.*;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Minimal read-only ELM327 transport for TCP Wi-Fi adapters.
 * Do not call this class from the Android UI thread.
 * This transport does not support ECU programming or automatic HS/MS switching.
 */
public final class ElmWifiClient implements Closeable {
  private Socket socket;
  private InputStream input;
  private OutputStream output;
  private final Object lock = new Object();

  public void connect(Context context, String host, int port, int timeoutMs) throws IOException {
    if (host == null || host.trim().isEmpty() || port < 1 || port > 65535)
      throw new IllegalArgumentException("Invalid adapter host/port");
    if (timeoutMs < 500 || timeoutMs > 30000)
      throw new IllegalArgumentException("Timeout must be 500..30000 ms");
    synchronized (lock) {
      close();
      ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
      Network wifiNetwork = null;
      if (cm != null) {
        for (Network n : cm.getAllNetworks()) {
          NetworkCapabilities caps = cm.getNetworkCapabilities(n);
          if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            wifiNetwork = n;
            break;
          }
        }
      }
      if (wifiNetwork == null) throw new IOException("Wi-Fi сеть ELM недоступна Android. Подключитесь к Wi-Fi адаптера и подтвердите использование сети без интернета.");
      // Explicit socket factory prevents Android/VPN/mobile-data routing of ELM TCP traffic.
      Socket s = wifiNetwork.getSocketFactory().createSocket();
      try {
        s.connect(new InetSocketAddress(host.trim(), port), timeoutMs);
        s.setSoTimeout(3000);
        s.setTcpNoDelay(true);
        socket = s;
        input = s.getInputStream();
        output = s.getOutputStream();
      } catch (IOException ex) {
        try { s.close(); } catch (IOException ignored) {}
        throw ex;
      }
    }
  }

  public boolean isConnected() {
    synchronized (lock) { return socket != null && socket.isConnected() && !socket.isClosed(); }
  }

  public String command(String asciiCommand) throws IOException {
    if (asciiCommand == null || !asciiCommand.matches("[A-Za-z0-9 ]{1,24}"))
      throw new IllegalArgumentException("Invalid ELM command");
    synchronized (lock) {
      if (!isConnected()) throw new IOException("ELM adapter not connected");
      output.write((asciiCommand.trim().toUpperCase(Locale.US) + "\r").getBytes(StandardCharsets.US_ASCII));
      output.flush();
      ByteArrayOutputStream response = new ByteArrayOutputStream();
      int idleTimeouts = 0;
      long deadline = System.nanoTime() + 20_000_000_000L;
      while (System.nanoTime() < deadline && response.size() < 16384) {
        int b;
        try { b = input.read(); }
        catch (SocketTimeoutException ex) {
          if (++idleTimeouts < 3) continue;
          throw new SocketTimeoutException("ELM response timeout for " + asciiCommand + " (partial: " + response.toString("US-ASCII") + ")");
        }
        idleTimeouts = 0;
        if (b == -1) throw new EOFException("ELM disconnected");
        if (b == '>') return new String(response.toByteArray(), StandardCharsets.US_ASCII).trim();
        response.write(b);
      }
      throw new IOException("ELM prompt missing or response too long");
    }
  }

  /** Adapter handshake; no vehicle configuration is changed. */
  public String initializeReadOnly() throws IOException {
    // Some inexpensive Wi-Fi bridges need a short startup pause after TCP opens.
    try { Thread.sleep(350); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IOException("ELM initialization interrupted", ex); }
    String reset;
    try { reset = command("ATZ"); }
    catch (SocketTimeoutException first) {
      // Recover once from a delayed bridge boot without hiding a persistent failure.
      reset = command("ATZ");
    }
    String echo = command("ATE0");
    String linefeed = command("ATL0");
    String spaces = command("ATS0");
    String protocol = command("ATSP0");
    if (reset.isEmpty() || echo.toUpperCase(Locale.US).contains("ERROR")
        || linefeed.toUpperCase(Locale.US).contains("ERROR")
        || spaces.toUpperCase(Locale.US).contains("ERROR")
        || protocol.toUpperCase(Locale.US).contains("ERROR"))
      throw new IOException("Adapter did not accept ELM initialization");
    return reset;
  }

  /** OBD-II mode 01 PID 00: supported PIDs. No ECU writes. */
  public String readSupportedEnginePids() throws IOException {
    return command("0100");
  }

  /** OBD-II mode 01 PID 0C: engine RPM; support must be checked first. */
  public String readEngineRpmRaw() throws IOException {
    return command("010C");
  }

  @Override public void close() throws IOException {
    synchronized (lock) {
      Socket s = socket;
      socket = null; input = null; output = null;
      if (s != null) s.close();
    }
  }
}
