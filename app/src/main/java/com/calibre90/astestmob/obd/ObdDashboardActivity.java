package com.calibre90.astestmob.obd;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

/** White/red Mazda Studio inspired home. Original As-Built editor remains separate. */
public final class ObdDashboardActivity extends Activity {
  private static final int RED=Color.rgb(211,23,35), DARK=Color.rgb(30,30,34), GREY=Color.rgb(94,98,107);
  private LinearLayout content;
  private TextView liveStatus, liveAction;
  private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
  private GradientDrawable panel(int stroke,int radius){
    GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
      new int[]{Color.WHITE,Color.rgb(246,247,250),Color.rgb(218,223,232)});
    d.setCornerRadius(dp(radius));d.setStroke(dp(2),stroke);return d;
  }
  private GradientDrawable redButton(){
    GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
      new int[]{Color.rgb(250,88,94),RED,Color.rgb(145,12,26)});
    d.setCornerRadius(dp(12));d.setStroke(dp(1),Color.rgb(130,18,26));return d;
  }
  private TextView text(String value,int size,int color,boolean bold){
    TextView v=new TextView(this);v.setText(value);v.setTextColor(color);v.setTextSize(size);
    if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;
  }
  @Override public void onCreate(Bundle state){
    super.onCreate(state);
    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    getWindow().setStatusBarColor(Color.rgb(242,243,246));
    getWindow().setNavigationBarColor(Color.BLACK);
    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    showDashboard();
  }
  private void showDashboard(){
    ScrollView scroll=new ScrollView(this);
    scroll.setFillViewport(true);scroll.setBackgroundColor(Color.rgb(238,240,244));
    content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);
    content.setPadding(dp(12),dp(12),dp(12),dp(22));
    scroll.addView(content);setContentView(scroll);

    // Existing Studio splash artwork reused as a branded header.
    ImageView banner=new ImageView(this);
    banner.setImageResource(com.calibre90.astestmob.R.drawable.splash_studio);
    banner.setScaleType(ImageView.ScaleType.CENTER_CROP);
    LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(112));
    content.addView(banner,bp);

    TextView subtitle=text("OBD STUDIO  •  Диагностика и As-Built",13,RED,true);
    subtitle.setGravity(Gravity.CENTER);
    LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);
    sp.setMargins(0,dp(7),0,dp(11));content.addView(subtitle,sp);

    LinearLayout status=new LinearLayout(this);
    status.setGravity(Gravity.CENTER_VERTICAL);status.setPadding(dp(15),dp(14),dp(12),dp(14));
    status.setBackground(panel(RED,13));
    LinearLayout.LayoutParams stp=new LinearLayout.LayoutParams(-1,-2);
    stp.setMargins(0,0,0,dp(12));content.addView(status,stp);
    TextView statusText=text("●  Адаптер не подключён",14,DARK,true);
    liveStatus=statusText;
    status.addView(statusText,new LinearLayout.LayoutParams(0,-2,1));
    TextView settings=text("НАСТРОИТЬ ›",12,RED,true);settings.setPadding(dp(4),dp(8),dp(4),dp(8));
    status.addView(settings);status.setOnClickListener(v->openConnection());

    LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER_VERTICAL);
    LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,-2);ap.setMargins(0,0,0,dp(3));
    content.addView(actions,ap);
    TextView heading=text("ПАНЕЛЬ УПРАВЛЕНИЯ",15,DARK,true);
    actions.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
    TextView connect=text("ПОДКЛЮЧИТЬ  ›",12,Color.WHITE,true);
    liveAction=connect;
    connect.setGravity(Gravity.CENTER);connect.setPadding(dp(12),dp(13),dp(12),dp(13));
    connect.setBackground(redButton());actions.addView(connect);
    connect.setOnClickListener(v->openConnection());

    addGridRow(new Tile("⚠","Ошибки DTC","Чтение и очистка"),new Tile("▥","Параметры","Датчики в реальном времени"));
    addGridRow(new Tile("⌁","Графики","Показания и история"),new Tile("▣","Модули","PCM, IC, BCM, ABS…"));
    addGridRow(new Tile("⚙","Тесты","Исполнительные механизмы"),new Tile("🔧","Сервис","Процедуры и сбросы"));
    addGridRow(new Tile("▦","As-Built Studio","Редактор ABT"),new Tile("◉","Адаптер","Wi-Fi / Bluetooth"));

    TextView foot=text("ТЕСТОВАЯ ВЕРСИЯ  •  Диагностика в разработке  •  Запись в ЭБУ отключена",11,GREY,false);
    updateConnectionState();
    foot.setGravity(Gravity.CENTER);
    LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,-2);fp.setMargins(0,dp(14),0,0);content.addView(foot,fp);
  }
  @Override protected void onResume() {
    super.onResume();
    updateConnectionState();
  }
  private void updateConnectionState() {
    if (liveStatus == null || liveAction == null) return;
    boolean adapter = ObdSession.adapterReady();
    boolean vehicle = ObdSession.vehicleReady();
    liveStatus.setText("●  " + ObdSession.status());
    liveAction.setText(adapter ? (vehicle ? "СВЯЗЬ ЕСТЬ  ›" : "АДАПТЕР  ›") : "ПОДКЛЮЧИТЬ  ›");
  }
  private static final class Tile {
    final String icon,name,detail;
    Tile(String i,String n,String d){icon=i;name=n;detail=d;}
  }
  private void addGridRow(Tile a,Tile b){
    LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
    LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.setMargins(0,dp(10),0,0);content.addView(row,rp);
    addTile(row,a,true);addTile(row,b,false);
  }
  private void addTile(LinearLayout row,Tile tile,boolean left){
    LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);
    box.setPadding(dp(13),dp(13),dp(9),dp(11));box.setMinimumHeight(dp(130));
    box.setBackground(panel(RED,14));
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);
    if(left)lp.setMargins(0,0,dp(5),0);else lp.setMargins(dp(5),0,0,0);
    row.addView(box,lp);
    box.addView(text(tile.icon,27,RED,true));
    TextView name=text(tile.name,15,DARK,true);
    LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);
    np.setMargins(0,dp(7),0,dp(4));box.addView(name,np);
    box.addView(text(tile.detail,12,GREY,false));
    box.setOnClickListener(v->{
      if(tile.name.equals("As-Built Studio"))
        startActivity(new Intent(this,AsBuiltEcuActivity.class));
      else if(tile.name.equals("Адаптер"))openConnection();
      else if(tile.name.equals("Ошибки DTC") || tile.name.equals("Параметры"))
        startActivity(new Intent(this,ObdDiagnosticsActivity.class));
      else if(tile.name.equals("Модули"))openConnection();
      else showPreview(tile.name,tile.detail);
    });
  }
  private void openConnection(){startActivity(new Intent(this,ObdConnectionActivity.class));}
  private void showPreview(String name,String detail){
    String state = ObdSession.status();
    String message = detail + "\n\n" + state + "\n\nРаздел диагностики пока не реализован. Даже при успешной связи OBD-II чтение данных отдельных модулей здесь ещё недоступно.";
    new AlertDialog.Builder(this).setTitle(name)
      .setMessage(message)
      .setPositiveButton("Понятно",null)
      .setNeutralButton("Подключение",(d,w)->openConnection()).show();
  }
}
