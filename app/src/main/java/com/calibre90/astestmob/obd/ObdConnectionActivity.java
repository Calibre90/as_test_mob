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
  private TextView status, log, moduleReport;
  private Button connect, disconnect;
  private boolean physicalMsSelected;
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
    // Let Android place content below the system bars on every supported API.
    // The previous edge-to-edge listener applied insets to the inner root and
    // still allowed the scroll viewport to overlap the status bar.
    root.setPadding(22, 24, 22, 20);
    ScrollView scroll = new ScrollView(this);
    scroll.addView(root);
    setContentView(scroll);
    if (Build.VERSION.SDK_INT >= 30) {
      getWindow().setDecorFitsSystemWindows(true);
    }
    TextView title = new TextView(this);
    title.setText("Mazda 6 GH — OBD подключение");
    title.setTextSize(22);
    root.addView(title);
    TextView hint = new TextView(this);
    hint.setText("Только чтение. Для адаптера с переключателем приложение запросит смену HS/MS перед опросом.");
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
    scan.setText("Опрос блоков автомобиля (только чтение)");
    root.addView(scan);
    scan.setOnClickListener(v->ensureHsCanThenProbe());
    Button results = new Button(this);
    results.setText("Результаты обнаружения блоков");
    root.addView(results);
    results.setOnClickListener(v->showModuleResults());
    Button diagnostics = new Button(this);
    diagnostics.setText("Параметры и ошибки двигателя (только чтение)");
    root.addView(diagnostics);
    diagnostics.setOnClickListener(v->startActivity(new Intent(this,ObdDiagnosticsActivity.class)));
    status = new TextView(this);
    status.setText("Не подключено");
    root.addView(status);
    log = new TextView(this);
    log.setText("Журнал связи (без записи в блоки)\n");
    log.setTextIsSelectable(true);
    log.setText("Журнал связи (без записи в блоки)\n" + ObdSession.journal());
    log.setVisibility(View.GONE);
    Button journalButton = new Button(this);
    journalButton.setText("Показать журнал связи");
    root.addView(journalButton);
    journalButton.setOnClickListener(v -> {
      boolean show = log.getVisibility() != View.VISIBLE;
      log.setVisibility(show ? View.VISIBLE : View.GONE);
      journalButton.setText(show ? "Скрыть журнал связи" : "Показать журнал связи");
    });
    moduleReport = new TextView(this);
    moduleReport.setText(ObdSession.moduleReport());
    moduleReport.setVisibility(View.GONE);
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
      ObdSession.log(line.trim());
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
    if(busy || ObdSession.adapterReady())return;
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
    physicalMsSelected = false;
    final int msSelection = msCanMode.getSelectedItemPosition();
    if(msSelection == 0) {
      append("MS-CAN: Авто — возможность программного переключения адаптера не подтверждена; переключение не выполняется.");
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
          wifi.connect(getApplicationContext(),ip,tcpPort,5000);
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
          // Mazda 6 GH HS-CAN: first try ISO 15765-4, 11-bit / 500 kbit.
          String can = ObdSession.command("ATSP6");
          append("HS-CAN ATSP6: "+can.trim());
          ObdSession.command("ATCRA");
          ObdSession.command("ATSH7DF");
          pids = ObdSession.command("0100");
          if (!hasSupportedPids(pids)) {
            append("HS-CAN did not confirm PID 0100; retrying automatic protocol.");
            ObdSession.command("ATSP0");
            ObdSession.command("ATCRA");
            ObdSession.command("ATSH7DF");
            pids = ObdSession.command("0100");
          }
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
            } catch (IOException vinError) {
              // A transport error invalidates the session: never advertise a live car on a dead socket.
              ObdSession.disconnect();
              publishConnection(false, false);
              throw new IOException("Связь с ELM потеряна при чтении VIN: " + vinError.getMessage(), vinError);
            }
            if (!cancelled) append("Связь с автомобилем подтверждена. Для проверки блоков запустите пробный опрос HS-CAN. Запись отключена.");
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
        ui.post(()->{if(!isFinishing()&&!isDestroyed()) { adapterConfirmed=ObdSession.adapterReady(); vehicleConfirmed=ObdSession.vehicleReady(); refreshButtons(adapterConfirmed); }});
      }
    });
  }

  private static boolean hasSupportedPids(String response) {
    if (response == null) return false;
    String value = response.toUpperCase(Locale.US).replaceAll("[^0-9A-F]", "");
    int at = value.indexOf("4100");
    return at >= 0 && value.length() >= at + 12;
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

  private void showModuleResults() {
    TextView details = new TextView(this);
    details.setPadding(32, 20, 32, 20);
    details.setTextIsSelectable(true);
    details.setText(ObdSession.detailedModuleReport());
    ScrollView scroller = new ScrollView(this);
    scroller.addView(details);
    new AlertDialog.Builder(this).setTitle("Обнаруженные блоки Mazda 6 GH")
        .setView(scroller).setPositiveButton("Закрыть", null).show();
  }

  private void retryHsVehicleHandshake() {
    if (busy || !ObdSession.adapterReady()) { append("Сначала подключите ELM327"); return; }
    busy = true;
    refreshButtons(true);
    worker.execute(() -> {
      try {
        append("Повторная проверка HS-CAN: 11 бит / 500 кбит/с.");
        ObdSession.command("ATSP6");
        ObdSession.command("ATCRA");
        ObdSession.command("ATSH7DF");
        String pids = ObdSession.command("0100");
        append("Повторный ответ 0100: " + pids.replaceAll("[\\r\\n]+", " | "));
        if (hasSupportedPids(pids)) {
          ObdSession.confirmVehicle(null);
          publishConnection(true, true);
          append("HS-CAN подтверждён. Теперь можно запустить опрос блоков.");
          ui.post(() -> { if (!isFinishing() && !isDestroyed()) probeModules(); });
        } else {
          append("HS-CAN не ответил. Проверьте положение HS переключателя и зажигание ON.");
        }
      } catch (Exception e) {
        append("Ошибка повторной проверки HS-CAN: " + e.getMessage());
      } finally {
        busy = false;
        ui.post(() -> refreshButtons(ObdSession.adapterReady()));
      }
    });
  }

  private void ensureHsCanThenProbe() {
    if (busy) { append("Дождитесь завершения текущего запроса"); return; }
    if (msCanMode.getSelectedItemPosition() == 2 && physicalMsSelected) {
      new AlertDialog.Builder(this)
          .setTitle("Переключите адаптер на HS-CAN")
          .setMessage("Переведите физический переключатель ELM327 в положение HS-CAN. После переключения нажмите «ОК». Опрос начнётся только после подтверждения.")
          .setNegativeButton("Отмена", null)
          .setPositiveButton("ОК, переключил", (dialog, which) -> {
            physicalMsSelected = false;
            append("Пользователь подтвердил положение HS-CAN.");
            probeModules();
          }).show();
      return;
    }
    if (!ObdSession.vehicleReady() && ObdSession.adapterReady()) {
      new AlertDialog.Builder(this)
          .setTitle("Проверить связь HS-CAN")
          .setMessage("ELM327 подключён, но ECU не ответил на 0100. Проверьте зажигание ON и физический переключатель HS-CAN. Повторить проверку без отключения Wi-Fi?")
          .setNegativeButton("Отмена", null)
          .setPositiveButton("Проверить HS-CAN", (dialog, which) -> retryHsVehicleHandshake())
          .show();
      return;
    }
    probeModules();
  }

  private static boolean elmOk(String response) {
    if (response == null) return false;
    String value = response.toUpperCase(Locale.US);
    return value.contains("OK") && !value.contains("?") && !value.contains("ERROR");
  }

  private void requestMsCan() {
    if (busy) { append("Дождитесь завершения текущего запроса"); return; }
    if (!ObdSession.adapterReady()) { append("Сначала подключитесь к ELM327"); return; }
    int setting = msCanMode.getSelectedItemPosition();
    if (setting == 1) {
      new AlertDialog.Builder(this).setTitle("MS-CAN отключена")
          .setMessage("В настройках выбрано «Нет». Выберите тип адаптера с поддержкой MS-CAN.")
          .setPositiveButton("Понятно", null).show();
      return;
    }
    if (setting == 0) {
      new AlertDialog.Builder(this).setTitle("Автоматическое переключение MS-CAN")
          .setMessage("Режим «Авто» выбран, но поддержка команд автоматического переключения именно этим адаптером пока не определена. Программа не будет отправлять неподтверждённые команды и выдавать переключение за успешное.")
          .setPositiveButton("Понятно", null).show();
      append("MS-CAN: автоматическое переключение не подтверждено.");
      return;
    }
    new AlertDialog.Builder(this)
        .setTitle("Переключите адаптер на MS-CAN")
        .setMessage("Переведите переключатель HS/MS на адаптере в положение MS-CAN и нажмите «ОК». После подтверждения начнётся экспериментальный опрос MS-CAN по предварительным адресам BCM/RKE/HVAC (только чтение). Отсутствие ответа не означает отсутствие блока.")
        .setNegativeButton("Отмена", null)
        .setPositiveButton("ОК, переключил", (dialog, which) -> {
          physicalMsSelected = true;
          append("Пользователь подтвердил положение MS-CAN. Настраиваем протокол 11-bit/125 кбит/с.");
          busy = true;
          refreshButtons(ObdSession.adapterReady());
          worker.execute(() -> {
            try {
              // ELM327 user protocol B: 11-bit ISO-TP CAN, divisor 04 = 125 kbit/s.
              String config = ObdSession.command("ATPB C1 04");
              if (!elmOk(config)) throw new IOException("ATPB C1 04: " + config);
              String selectedProtocol = ObdSession.command("ATSPB");
              if (!elmOk(selectedProtocol)) throw new IOException("ATSPB: " + selectedProtocol);
              // ATDPN reports the currently selected ELM protocol; B is user protocol B.
              String protocolNumber = ObdSession.command("ATDPN").trim();
              String protocolName = ObdSession.command("ATDP").trim();
              append("MS-CAN: контроль протокола ATDPN=" + protocolNumber
                  + "; ATDP=" + protocolName + ".");
              if (!protocolNumber.equalsIgnoreCase("B")) {
                append("MS-CAN: ВНИМАНИЕ — ELM не подтвердил пользовательский протокол B. Опрос MS-CAN остановлен.");
                return;
              }
              append("MS-CAN: ELM подтвердил выбор протокола B (настройка 125 кбит/с). Это НЕ подтверждает физическое переключение HS/MS или ответы блоков.");
              append("MS-CAN: проверяем также IC [720/728] как адрес-кандидат на MS-CAN.");
              append("MS-CAN: выполняем экспериментальный опрос адресов-кандидатов (только чтение).");
              for (ObdModuleProbe.Result result : ObdModuleProbe.probeMsCan(ObdSession::command)) {
                String key = "MS-CAN " + result.name + " [" + result.requestId + "/" + result.responseId + "]";
                ObdSession.saveModuleResult(key, result.status);
                ObdSession.saveModuleRaw(key, result.response);
                append(key + ": " + result.status);
                if (result.response != null && !result.response.trim().isEmpty())
                  append("Сырой ответ " + key + ": " + result.response.replaceAll("[\\r\\n]+", " | "));
              }
              ui.post(() -> {
                if (!isFinishing() && !isDestroyed()) moduleReport.setText(ObdSession.moduleReport());
              });
              append("MS-CAN: опрос адресов-кандидатов завершён. Названия блоков требуют подтверждения.");
            } catch (Exception e) {
              append("MS-CAN: настройка не удалась: " + e.getMessage());
              if (!ObdSession.adapterReady()) publishConnection(false, false);
            } finally {
              busy = false;
              ui.post(() -> { if (!isFinishing() && !isDestroyed()) refreshButtons(ObdSession.adapterReady()); });
            }
          });
        }).show();
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
            ObdSession.clearModuleResults();
            for (ObdModuleProbe.Result result: ObdModuleProbe.probeHsCan(ObdSession::command)) {
              if ("PCM".equals(result.name)) {
                String detectedVin = ObdModuleProbe.decodeVin(result.response);
                if (detectedVin != null) {
                  ObdSession.confirmVehicle(detectedVin);
                  append("VIN автомобиля (PCM): " + detectedVin);
                }
              }
              String key = result.name+" ["+result.requestId+"/"+result.responseId+"]";
              ObdSession.saveModuleResult(key, result.status);
              ObdSession.saveModuleRaw(key, result.response);
              append(result.name+" ["+result.requestId+"/"+result.responseId+"]: "+result.status);
              if (result.response != null && !result.response.trim().isEmpty()) {
                append("Сырой ответ "+result.name+": "+result.response.replaceAll("[\\r\\n]+"," | ").trim());
              }
            }
            ui.post(() -> { if (!isFinishing() && !isDestroyed()) moduleReport.setText(ObdSession.moduleReport()); });
            append("Пробный опрос HS-CAN завершён. Обнаружение не подтверждает возможность чтения As-Built.");
            ui.post(() -> { if (!isFinishing() && !isDestroyed()) offerNextMsScan(); });
          } catch(Exception e) {
            append("Ошибка опроса: "+e.getMessage());
            if (!ObdSession.adapterReady()) {
              publishConnection(false, false);
              append("Соединение с ELM потеряно. Для повторного опроса необходимо переподключиться.");
              ui.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                new AlertDialog.Builder(this)
                    .setTitle("Потеряна связь с ELM327")
                    .setMessage("TCP/Bluetooth-соединение прервано. Проверьте Wi-Fi адаптера и зажигание. Подключиться заново?")
                    .setNegativeButton("Позже", null)
                    .setPositiveButton("Переподключить", (dialog, which) -> startConnect())
                    .show();
              });
            }
          }
          finally { busy=false;ui.post(()->{ adapterConfirmed=ObdSession.adapterReady(); vehicleConfirmed=ObdSession.vehicleReady(); refreshButtons(adapterConfirmed); }); }
        });
      }).show();
  }

  private void offerNextMsScan() {
    if (!ObdSession.adapterReady() || cancelled) return;
    if (msCanMode.getSelectedItemPosition() == 1) return;
    new AlertDialog.Builder(this)
        .setTitle("Продолжить проверку MS-CAN?")
        .setMessage("HS-CAN проверена. Для дополнительных блоков Mazda 6 GH требуется MS-CAN. Перейти к настройке MS-CAN? После выбора приложение само запросит переключение адаптера.")
        .setNegativeButton("Остаться на HS-CAN", null)
        .setPositiveButton("Продолжить", (dialog, which) -> requestMsCan())
        .show();
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
    if(log != null)log.setText("Журнал связи (без записи в блоки)\n"+ObdSession.journal());
    if(moduleReport != null)moduleReport.setText(ObdSession.moduleReport());
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
