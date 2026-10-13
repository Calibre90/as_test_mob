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
  private final Handler ui = new Handler(Looper.getMainLooper());
  private final java.util.concurrent.ExecutorService worker = java.util.concurrent.Executors.newSingleThreadExecutor();
  private volatile boolean busy;
  private volatile boolean cancelled;
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
    status = new TextView(this);
    status.setText("Не подключено");
    root.addView(status);
    log = new TextView(this);
    log.setText("Журнал связи (без записи в блоки)\n");
    log.setTextIsSelectable(true);
    root.addView(log);
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
    if (Build.VERSION.SDK_INT>=31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED) {
      requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},BT_PERMISSION);
      return false;
    }
    return true;
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

  private void startConnect() {
    if(busy)return;
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
    busy=true;cancelled=false;connect.setEnabled(false);
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
        append("Чтение списка поддерживаемых PID двигателя…");
        String pids=useWifi?wifi.readSupportedEnginePids():bluetooth.command("0100");
        if(!cancelled)append("Ответ PID 0100: "+pids.replaceAll("[\\r\\n]+"," ").trim());
        if(!cancelled)append("Подключение установлено. Запись отключена.");
      } catch(Exception e) {
        if(!cancelled)append("Ошибка подключения: "+e.getClass().getSimpleName()+" — "+e.getMessage());
        closeAdapters();
      } finally {
        busy=false;
        ui.post(()->{if(!isFinishing()&&!isDestroyed())connect.setEnabled(true);});
      }
    });
  }

  private void stopConnection() {
    cancelled=true;
    append("Отключение…");
    // Closing sockets on a separate thread can unblock in-flight reads.
    new Thread(this::closeAdapters,"OBD-disconnect").start();
  }

  private synchronized void closeAdapters() {
    try{if(wifi!=null)wifi.close();}catch(Exception ignored){}
    try{if(bluetooth!=null)bluetooth.close();}catch(Exception ignored){}
    wifi=null;bluetooth=null;
  }

  @Override protected void onDestroy() {
    cancelled=true;
    new Thread(this::closeAdapters,"OBD-close").start();
    worker.shutdownNow();
    super.onDestroy();
  }
}
