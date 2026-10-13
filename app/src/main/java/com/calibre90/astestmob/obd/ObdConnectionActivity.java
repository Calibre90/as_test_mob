package com.calibre90.astestmob.obd;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

/** Independent read-only connection screen. Launch with Intent(context, ObdConnectionActivity.class). */
public final class ObdConnectionActivity extends Activity {
  private static final int BT_PERMISSION = 701;
  private static final String[] BT_PERMISSIONS = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN};
  private final Handler ui = new Handler(Looper.getMainLooper());
  private final java.util.concurrent.ExecutorService worker = java.util.concurrent.Executors.newSingleThreadExecutor();
  private volatile boolean busy;
  private volatile boolean cancelled;
  private volatile boolean vehicleConfirmed;
  private volatile boolean adapterConfirmed;
  private ElmWifiClient wifi;
  private ElmBluetoothClient bluetooth;
  private Spinner mode, msCanMode;
  private EditText host, port;
  private TextView status, log;
  private Button connect, disconnect;
  private BluetoothDevice selected;
  private final ArrayList<BluetoothDevice> paired = new ArrayList<>();

  @Override public void onCreate(Bundle saved) {
    super.onCreate(saved);
    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    getWindow().setStatusBarColor(android.graphics.Color.rgb(17,22,30));
    getWindow().setNavigationBarColor(android.graphics.Color.rgb(17,22,30));
    getWindow().getDecorView().setSystemUiVisibility(0);
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(22,48,22,20);
    ScrollView scroll = new ScrollView(this);
    scroll.addView(root);
    setContentView(scroll);
    TextView title = new TextView(this);
    title.setText("Mazda 6 GH — OBD подключение");
    title.setTextSize(22);
    root.addView(title);
    TextView hint = new TextView(this);
    hint.setText("Только чтение. Для MS-CAN переключайте HS/MS на адаптере вручную.");
    root.addView(hint);
    mode = new Spinner(this);
    ArrayAdapter<String> types = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
        new String[]{"Wi-Fi", "Bluetooth"});
    mode.setAdapter(types);
    root.addView(mode);
    TextView msLabel = new TextView(this);
    msLabel.setText("Поддержка MS-CAN");
    msLabel.setTextSize(16);
    msLabel.setPadding(0, 16, 0, 0);
    root.addView(msLabel);
    msCanMode = new Spinner(this);
    ArrayAdapter<String> msOptions = new ArrayAdapter<>(this,
        android.R.layout.simple_spinner_dropdown_item,
        new String[]{"Авто", "Нет", "Переключатель HS/MS"});
    msCanMode.setAdapter(msOptions);
    int savedMsMode = getPreferences(0).getInt("obd_ms_can_mode", 2);
    msCanMode.setSelection(Math.max(0, Math.min(2, savedMsMode)));
    root.addView(msCanMode);
    msCanMode.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
      public void onNothingSelected(android.widget.AdapterView<?> parent) {}
      public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
        getPreferences(0).edit().putInt("obd_ms_can_mode", position).apply();
      }
    });
    host = new EditText(this);
    host.setSingleLine(true);
    host.setHint("IP адаптера");
    host.setText(getPreferences(0).getString("obd_host","192.168.0.10"));
    root.addView(host);
    port = new EditText(this);
    port.setSingleLine(true);
    port.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    port.setHint("TCP порт");
    port.setText(getPreferences(0).getString("obd_port","35000"));
    root.addView(port);
    Button choose = new Button(this);
    choose.setText("Выбрать сопряжённый Bluetooth адаптер");
    root.addView(choose);
    choose.setOnClickListener(v->chooseBluetooth());
    mode.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
      public void onNothingSelected(android.widget.AdapterView<?> parent) {}
      public void onItemSelected(android.widget.AdapterView<?> parent,View view,int position,long id) {
        boolean isWifi = position == 0;
        host.setVisibility(isWifi ? View.VISIBLE : View.GONE);
        port.setVisibility(isWifi ? View.VISIBLE : View.GONE);
        choose.setVisibility(isWifi ? View.GONE : View.VISIBLE);
      }
    });
    connect = new Button(this);
    connect.setText("Подключить");
    root.addView(connect);
    disconnect = new Button(this);
    disconnect.setText("Отключить");
    root.addView(disconnect);
    Button scan = new Button(this);
    scan.setText("Пробный опрос HS-CAN (только чтение)");
    root.addView(scan);
    scan.setOnClickListener(v->probeModules());
    status = new TextView(this);
    status.setText("Не подключено");
    root.addView(status);
    log = new TextView(this);
    log.setText("Журнал связи (без записи в блоки)\n");
    log.setTextIsSelectable(true);
    root.addView(log);
    adapterConfirmed = ObdSession.adapterReady();
    vehicleConfirmed = ObdSession.vehicleReady();
    status.setText(ObdSession.status());
    refreshButtons(adapterConfirmed);
    connect.setOnClickListener(v->startConnect());
    disconnect.setOnClickListener(v->stopConnection());
  }

  private void append(String message) {
    ui.post(()->{
      if(isFinishing() || isDestroyed())return;
      String line = new java.text.SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date())+"  "+message+"\n";
      String current = log.getText().toString();
      log.setText((current.length()>14000?current.substring(current.length()-9000):current)+line);
      status.setText(message);
    });
  }

  private boolean bluetoothPermission() {
    if (Build.VERSION.SDK_INT >= 31 &&
        (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
         checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)) {
      requestPermissions(BT_PERMISSIONS, BT_PERMISSION);
      return false;
    }
    return true;
  }

  @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode != BT_PERMISSION) return;
    if (bluetoothPermissionGranted()) {
      append("Разрешения Bluetooth получены. Повторите выбор адаптера или подключение.");
    } else {
      append("Для подключения ELM327 разрешите приложению доступ к устройствам поблизости в настройках Android.");
    }
  }

  private boolean bluetoothPermissionGranted() {
    return Build.VERSION.SDK_INT < 31 ||
        (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
         checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED);
  }

  @SuppressWarnings("MissingPermission")
  private void chooseBluetooth() {
    if(!bluetoothPermission())return;
    BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    if(adapter==null || !adapter.isEnabled()) { append("Включите Bluetooth и сопрягите ELM327 в настройках Android"); return; }
    paired.clear();
    paired.addAll(adapter.getBondedDevices());
    if(paired.isEmpty()) { append("Нет сопряжённых устройств Bluetooth"); return; }
    String[] names = new String[paired.size()];
    for(int i=0;i<paired.size();i++) names[i] = paired.get(i).getName()+" ("+paired.get(i).getAddress()+")";
    new AlertDialog.Builder(this).setTitle("Выберите ELM327").setItems(names,(dialog,index)->{
      selected=paired.get(index);
      append("Выбран Bluetooth адаптер: "+selected.getName());
    }).show();
  }

  private void refreshButtons(boolean connected) {
    if (connect != null) connect.setEnabled(!busy && !connected);
    if (disconnect != null) disconnect.setEnabled(busy || connected);
  }

  private void publishConnection(boolean adapterReady, boolean ecuReady) {
    vehicleConfirmed = ecuReady;
    adapterConfirmed = adapterReady;
    // Never persist live connection state: sockets cannot survive process death.
    ui.post(() -> { if (!isFinishing() && !isDestroyed()) refreshButtons(adapterReady); });
  }

  private void startConnect() {
    if(busy || adapterConfirmed)return;
    final boolean useWifi=mode.getSelectedItemPosition()==0;
    final String ip=host.getText().toString().trim();
    final int tcpPort;
    if(useWifi) {
      try {tcpPort=Integer.parseInt(port.getText().toString().trim());}
      catch(NumberFormatException e){append("Укажите правильный TCP порт");return;}
      if(ip.isEmpty()||tcpPort<1||tcpPort>65535){append("Неверный IP/порт");return;}
      getPreferences(0).edit().putString("obd_host",ip).putString("obd_port",String.valueOf(tcpPort)).apply();
    } else {
      tcpPort=0;
      if(!bluetoothPermission())return;
      if(selected==null){append("Сначала выберите Bluetooth адаптер");return;}
    }
    final int msSelection = msCanMode.getSelectedItemPosition();
    if(msSelection == 0) {
      append("MS-CAN: Авто — аппаратное автопереключение пока не реализовано.");
    } else if(msSelection == 1) {
      append("MS-CAN отключена в настройках.");
    } else {
      append("MS-CAN: переключайте HS/MS на адаптере вручную при запросе.");
    }
    busy=true;cancelled=false;
    publishConnection(false, false);
    refreshButtons(false);
    append("Подключение к адаптеру…");
    worker.execute(()->{
      try {
        String id;
        if(useWifi) {
          wifi=new ElmWifiClient();
          wifi.connect(ip,tcpPort,5000);
          if(cancelled)return;
          id=wifi.initializeReadOnly();
        } else {
          bluetooth=new ElmBluetoothClient();
          bluetooth.connect(this,selected);
          if(cancelled)return;
          id=bluetooth.initializeReadOnly();
        }
        append("ELM ответил: "+id.replaceAll("[\\r\\n]+"," ").trim());
        if(cancelled)return;
        ObdSession.attach(useWifi ? wifi : bluetooth);
        publishConnection(true, false);
        append("Связь с ELM327 установлена. Проверяем связь с автомобилем (зажигание ON)…");
        String protocolBefore = useWifi ? wifi.command("ATDPN") : bluetooth.command("ATDPN");
        if(!cancelled)append("Протокол до запроса: "+protocolBefore.replaceAll("[\\r\\n]+"," ").trim());
        append("Отправляем OBD-II запрос 0100…");
        String pids;
        try {
          pids=useWifi?wifi.readSupportedEnginePids():bluetooth.command("0100");
        } catch (java.net.SocketTimeoutException timeout) {
          if(!cancelled)append("ELM отвечает, но автомобиль не ответил вовремя на 0100. Проверьте зажигание ON, HS-CAN и протокол. "+timeout.getMessage());
          throw timeout;
        }
        if(!cancelled)append("Ответ PID 0100: "+pids.replaceAll("[\\r\\n]+"," ").trim());
        String protocolAfter = useWifi ? wifi.command("ATDP") : bluetooth.command("ATDP");
        if(!cancelled)append("Определённый протокол: "+protocolAfter.replaceAll("[\\r\\n]+"," ").trim());
        String normalized = pids.toUpperCase(Locale.US);
        if(normalized.contains("NO DATA") || normalized.contains("UNABLE TO CONNECT") || normalized.contains("ERROR") || normalized.trim().isEmpty()) {
          if(!cancelled)append("Адаптер доступен, но ответ ECU не подтверждён. Запись отключена.");
        } else if(normalized.contains("41 00") || normalized.contains("4100")) {
          if(!cancelled) {
            ObdSession.confirmVehicle(null);
            publishConnection(true, true);
            append("Автомобиль ответил на OBD-II запрос. Проверяем VIN (09 02)…");
            try {
              String vinResponse = useWifi ? wifi.command("0902") : bluetooth.command("0902");
              String vin = parseVin(vinResponse);
              if (!cancelled) append(vin == null
                  ? "VIN не получен стандартным запросом 0902 (это не ошибка подключения)."
                  : "VIN автомобиля: " + vin);
              if (vin != null) ObdSession.confirmVehicle(vin);
            } catch (Exception vinError) {
              if (!cancelled) append("VIN пока не прочитан: " + vinError.getMessage());
            }
            if (!cancelled) append("Связь с автомобилем подтверждена. Опрос отдельных модулей пока не реализован. Запись отключена.");
          }
        } else {
          if(!cancelled)append("Получен нестандартный ответ. Связь с ECU не подтверждена, запись отключена.");
        }
      } catch(Exception e) {
        if(!cancelled)append("Ошибка подключения: "+e.getClass().getSimpleName()+" — "+e.getMessage());
        ObdSession.disconnect();
        closeAdapters();
        publishConnection(false, false);
      } finally {
        busy=false;
        ui.post(()->{if(!isFinishing()&&!isDestroyed())refreshButtons((wifi!=null && wifi.isConnected()) || bluetooth!=null);});
      }
    });
  }

  /** Decode standard OBD mode 09 PID 02 responses, including ELM CAN line prefixes. */
  private static String parseVin(String response) {
    if (response == null) return null;
    StringBuilder ascii = new StringBuilder();
    String[] lines = response.toUpperCase(Locale.US).split("[\\r\\n]+");
    for (String line : lines) {
      String hex = line.replaceAll("[^0-9A-F]", "");
      int marker = hex.indexOf("4902");
      if (marker < 0) continue;
      String payload = hex.substring(marker + 4);
      // VIN frame index follows the 49 02 header.
      if (payload.length() >= 2) payload = payload.substring(2);
      for (int i = 0; i + 1 < payload.length(); i += 2) {
        int b = Integer.parseInt(payload.substring(i, i + 2), 16);
        if (b >= 0x20 && b <= 0x7E) ascii.append((char)b);
      }
    }
    String vin = ascii.toString().trim();
    return vin.matches("[A-HJ-NPR-Z0-9]{17}") ? vin : null;
  }

  private void probeModules() {
    if (busy) { append("Дождитесь завершения текущего запроса"); return; }
    if (!ObdSession.vehicleReady()) { append("Сначала подтвердите связь с автомобилем"); return; }
    new AlertDialog.Builder(this)
      .setTitle("Пробный опрос HS-CAN")
      .setMessage("Только чтение. Используются предварительные адреса ECU, ещё не подтверждённые для вашей комплектации Mazda 6 GH. Отсутствие ответа не означает отсутствие блока. Убедитесь, что адаптер переключён на HS-CAN.")
      .setNegativeButton("Отмена",null)
      .setPositiveButton("Проверить",(d,w)->{
        busy=true;
        refreshButtons(ObdSession.adapterReady());
        worker.execute(()->{
          try {
            append("Начат пробный опрос HS-CAN; запись запрещена.");
            for (ObdModuleProbe.Result result: ObdModuleProbe.probeHsCan(ObdSession::command)) {
              append(result.name+" ["+result.requestId+"/"+result.responseId+"]: "+result.status);
            }
            append("Пробный опрос завершён. Обнаружение не подтверждает возможность чтения As-Built.");
          } catch(Exception e) { append("Ошибка опроса: "+e.getMessage()); }
          finally { busy=false;ui.post(()->refreshButtons(ObdSession.adapterReady())); }
        });
      }).show();
  }

  private void stopConnection() {
    cancelled=true;
    busy=false;
    publishConnection(false, false);
    append("Отключение…");
    // Closing sockets on a separate thread can unblock in-flight reads.
    new Thread(()->{ObdSession.disconnect();closeAdapters();},"OBD-disconnect").start();
  }

  private synchronized void closeAdapters() {
    try{if(wifi!=null)wifi.close();}catch(Exception ignored){}
    try{if(bluetooth!=null)bluetooth.close();}catch(Exception ignored){}
    wifi=null;bluetooth=null;
  }

  @Override protected void onResume() {
    super.onResume();
    adapterConfirmed = ObdSession.adapterReady();
    vehicleConfirmed = ObdSession.vehicleReady();
    if(status != null)status.setText(ObdSession.status());
    refreshButtons(adapterConfirmed);
  }

  @Override protected void onDestroy() {
    // Keep an established session alive when returning to the main menu.
    // A connection still in progress is cancelled when its screen is destroyed.
    if(busy && !ObdSession.adapterReady()) {
      cancelled=true;
      new Thread(this::closeAdapters,"OBD-close-pending").start();
    }
    worker.shutdownNow();
    super.onDestroy();
  }
}
