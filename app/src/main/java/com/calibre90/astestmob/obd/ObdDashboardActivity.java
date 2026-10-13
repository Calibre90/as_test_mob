package com.calibre90.astestmob.obd;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

public final class ObdDashboardActivity extends Activity {
  private static final int BG=Color.rgb(17,22,30), PANEL=Color.rgb(31,39,51), ACCENT=Color.rgb(221,51,57);
  private LinearLayout content;
  private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
  private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
  private TextView text(String value,int size,int color,boolean bold){TextView v=new TextView(this);v.setText(value);v.setTextColor(color);v.setTextSize(size);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
  @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);getWindow().getDecorView().setSystemUiVisibility(0);showDashboard();}
  private void showDashboard(){
    ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
    content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(18),dp(22),dp(18),dp(24));scroll.addView(content);setContentView(scroll);
    TextView heading=text("MAZDA 6 GH",27,Color.WHITE,true);content.addView(heading);
    TextView subtitle=text("OBD STUDIO  •  Диагностика и As-Built",13,Color.LTGRAY,false);content.addView(subtitle);
    TextView connection=text("●  Адаптер не подключён",14,Color.rgb(245,175,95),true);
    connection.setPadding(dp(14),dp(16),dp(12),dp(16));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,dp(20),0,dp(16));content.addView(connection,cp);connection.setBackground(bg(PANEL,12));
    LinearLayout header=new LinearLayout(this);header.setOrientation(LinearLayout.HORIZONTAL);header.setGravity(Gravity.CENTER_VERTICAL);content.addView(header);
    TextView title=text("ПАНЕЛЬ УПРАВЛЕНИЯ",15,Color.WHITE,true);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
    TextView connect=text("ПОДКЛЮЧИТЬ  ›",13,Color.WHITE,true);connect.setPadding(dp(10),dp(12),dp(10),dp(12));connect.setBackground(bg(ACCENT,9));header.addView(connect);connect.setOnClickListener(v->startActivity(new Intent(this,ObdConnectionActivity.class)));
    addGridRow(new Tile("⚠","Ошибки DTC","Чтение и очистка"),new Tile("▥","Параметры","Датчики в реальном времени"));
    addGridRow(new Tile("⌁","Графики","Показания и история"),new Tile("▣","Модули","PCM, IC, BCM, ABS…"));
    addGridRow(new Tile("⚙","Тесты","Исполнительные механизмы"),new Tile("🔧","Сервис","Процедуры и сбросы"));
    addGridRow(new Tile("▦","As-Built Studio","Твой редактор ABT"),new Tile("◉","Адаптер","Wi-Fi / Bluetooth"));
    TextView foot=text("Диагностические разделы пока в режиме предварительного просмотра. Запись в автомобиль отключена.",12,Color.LTGRAY,false);
    LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,-2);fp.setMargins(0,dp(20),0,0);content.addView(foot,fp);
  }
  private static final class Tile {final String icon,name,detail;Tile(String i,String n,String d){icon=i;name=n;detail=d;}}
  private void addGridRow(Tile a,Tile b){
    LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
    LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.setMargins(0,dp(13),0,0);content.addView(row,rp);
    addTile(row,a,true);addTile(row,b,false);
  }
  private void addTile(LinearLayout row,Tile tile,boolean left){
    LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(13),dp(14),dp(10),dp(14));box.setMinimumHeight(dp(142));box.setBackground(bg(PANEL,14));
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);if(left)lp.setMargins(0,0,dp(6),0);else lp.setMargins(dp(6),0,0,0);row.addView(box,lp);
    box.addView(text(tile.icon,30,Color.rgb(245,100,100),true));
    TextView name=text(tile.name,15,Color.WHITE,true);LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);np.setMargins(0,dp(10),0,dp(4));box.addView(name,np);
    box.addView(text(tile.detail,12,Color.LTGRAY,false));
    box.setOnClickListener(v->{
      if(tile.name.equals("As-Built Studio"))startActivity(new Intent(this,com.calibre90.astestmob.MainActivity.class));
      else if(tile.name.equals("Адаптер"))startActivity(new Intent(this,ObdConnectionActivity.class));
      else showPreview(tile.name,tile.detail);
    });
  }
  private void showPreview(String name,String detail){
    new AlertDialog.Builder(this).setTitle(name).setMessage(detail+"\n\nРаздел интерфейса подготовлен. Реальная диагностика появится после интеграции протоколов и проверки на автомобиле.").setPositiveButton("Понятно",null).setNeutralButton("Подключение",(d,w)->startActivity(new Intent(this,ObdConnectionActivity.class))).show();
  }
}
