package com.calibre90.astestmob.obd;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

/**
 * Bluetooth Classic SPP transport for paired ELM327 adapters.
 * UI must request BLUETOOTH_CONNECT on Android 12+ before calling connect.
 * BLE-only adapters are not supported by this class.
 */
public final class ElmBluetoothClient implements Closeable {
  private static final UUID SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
  private BluetoothSocket socket;
  private InputStream input;
  private OutputStream output;

  @SuppressLint("MissingPermission")
  public synchronized void connect(Context context, BluetoothDevice pairedDevice) throws IOException {
    if (pairedDevice == null) throw new IllegalArgumentException("Select a paired Bluetooth device");
    if (Build.VERSION.SDK_INT >= 31 &&
        context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
      throw new SecurityException("BLUETOOTH_CONNECT permission required");
    close();
    BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    if (adapter == null || !adapter.isEnabled()) throw new IOException("Bluetooth disabled");
    adapter.cancelDiscovery();
    BluetoothSocket candidate = pairedDevice.createRfcommSocketToServiceRecord(SPP);
    try {
      candidate.connect();
      socket = candidate;
      input = candidate.getInputStream();
      output = candidate.getOutputStream();
    } catch (IOException e) {
      try { candidate.close(); } catch (IOException ignored) {}
      throw e;
    }
  }

  public synchronized String command(String command) throws IOException {
    if (socket == null || !socket.isConnected()) throw new IOException("Bluetooth ELM not connected");
    if (command == null || !command.matches("[A-Za-z0-9 ]{1,24}"))
      throw new IllegalArgumentException("Invalid ELM command");
    output.write((command.trim().toUpperCase(Locale.US) + "\r").getBytes(StandardCharsets.US_ASCII));
    output.flush();
    ByteArrayOutputStream result = new ByteArrayOutputStream();
    long deadline = System.nanoTime() + 12_000_000_000L;
    while (System.nanoTime() < deadline && result.size() < 16384) {
      if (input.available() == 0) {
        try { Thread.sleep(20); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IOException("Interrupted", e); }
        continue;
      }
      int c = input.read();
      if (c == -1) throw new EOFException("Bluetooth disconnected");
      if (c == '>') return new String(result.toByteArray(), StandardCharsets.US_ASCII).trim();
      result.write(c);
    }
    throw new IOException("Bluetooth ELM response timeout or too long");
  }

  public synchronized String initializeReadOnly() throws IOException {
    String id = command("ATZ");
    command("ATE0");
    command("ATL0");
    command("ATS0");
    command("ATSP0");
    if (id.isEmpty()) throw new IOException("No ELM adapter identification");
    return id;
  }

  @Override public synchronized void close() throws IOException {
    BluetoothSocket s = socket;
    socket = null; input = null; output = null;
    if (s != null) s.close();
  }
}
