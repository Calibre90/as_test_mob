package com.calibre90.astestmob.obd;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Read-only ECU configuration UI scaffold. Never simulates successful ECU reads. */
public final class AsBuiltEcuActivity extends Activity {
  private static final int RED=0xffce202d, DARK=0xff22242a, MUTED=0xff666b75;
  private final AsBuiltSafetySession session=new AsBuiltSafetySession();
  private LinearLayout root, options;
  private TextView status, header;
  private Button read, write;
  private String selected="IC";
  private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
  private GradientDrawable panel(){
    GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xffffffff,0xffe2e5eb});
    d.setCornerRadius(dp(13));d.setStroke(dp(2),RED);return d;
  }
  private TextView label(String value,int size,boolean bold){
    TextView t=new TextView(this);t.setText(value);t.setTextColor(DARK);t.setTextSize(size);
    if(bold)t.setTypeface(null,1);return t;
  }
  private void block(View v,int top){
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(top),0,0);
    root.addView(v,lp);
  }
  @Override public void onCreate(Bundle state){
    super.onCreate(state);
    getWindow().setStatusBarColor(0xfff2f3f6);
    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(0xffeff0f4);
    root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(12),dp(18),dp(12),dp(24));
    scroll.addView(root);setContentView(scroll);
    TextView title=label("AS-BUILT STUDIO  •  ЭБУ",23,true);block(title,0);
    TextView intro=label("Подключение к блоку, чтение опций и подготовка изменений",13,false);block(intro,5);
    LinearLayout tabs=new LinearLayout(this);tabs.setOrientation(LinearLayout.HORIZONTAL);
    block(tabs,16);
    for(String ecu:new String[]{"IC","BCM","RKE","ABS","AFS"}){
      Button b=new Button(this);b.setText(ecu);b.setTextSize(11);b.setAllCaps(false);
      tabs.addView(b,new LinearLayout.LayoutParams(0,dp(52),1));
      b.setOnClickListener(v->select(ecu));
    }
    LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);
    info.setPadding(dp(13),dp(15),dp(13),dp(15));info.setBackground(panel());block(info,12);
    header=label("",19,true);info.addView(header);
    status=label("",13,false);LinearLayout.LayoutParams st=new LinearLayout.LayoutParams(-1,-2);st.setMargins(0,dp(8),0,dp(8));info.addView(status,st);
    read=new Button(this);read.setText("ПОДКЛЮЧИТЬСЯ К БЛОКУ");info.addView(read);
    read.setOnClickListener(v->showBusDialog());
    options=new LinearLayout(this);options.setOrientation(LinearLayout.VERTICAL);
    options.setPadding(dp(12),dp(14),dp(12),dp(14));options.setBackground(panel());block(options,12);
    TextView label=label("ОПЦИИ БЛОКА",17,true);options.addView(label);
    TextView note=label("Галочки активируются только после настоящего чтения конфигурации блока. Значения по умолчанию не выдаются за данные автомобиля.",12,false);
    options.addView(note);
    // These are only display labels. No option is decoded without real ECU bytes.
    for(String option:new String[]{"RVM","KEYLESS / KLS","TPMS","AFS","Датчик дождя","Датчик света"}){
      CheckBox cb=new CheckBox(this);cb.setText(option);cb.setTextSize(16);cb.setTextColor(DARK);cb.setEnabled(false);
      cb.setPadding(dp(4),dp(10),dp(4),dp(10));options.addView(cb);
    }
    write=new Button(this);write.setText("ЗАПИСАТЬ В ЭБУ");write.setEnabled(false);block(write,12);
    write.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Запись недоступна")
      .setMessage("Сначала требуется реальное чтение, резервная копия, проверка протокола и подтверждение изменений.")
      .setPositiveButton("Понятно",null).show());
    TextView footer=label("Без подключения к автомобилю запись заблокирована. Это безопасный интерфейс, а не имитация чтения.",12,false);block(footer,12);
    select("IC");
  }
  private void select(String ecu){
    selected=ecu;
    session.clear();
    String name=ecu.equals("IC")?"Instrument Cluster":ecu.equals("BCM")?"Body Control Module":ecu.equals("ABS")?"Anti-lock Braking System":ecu.equals("AFS")?"Adaptive Front-lighting System":"Remote Keyless Entry";
    header.setText(ecu+": "+name);
    status.setText("Состояние: не подключён. Конфигурация не прочитана.");
    read.setEnabled(true);write.setEnabled(false);
    for(int i=2;i<options.getChildCount();i++){
      View v=options.getChildAt(i);
      if(v instanceof CheckBox){((CheckBox)v).setChecked(false);v.setEnabled(false);}
    }
  }
  private void showBusDialog(){
    new AlertDialog.Builder(this).setTitle("Выбор шины HS/MS-CAN")
      .setMessage("Для блока "+selected+" необходимо определить нужную шину по проверенной карте автомобиля. Положение ручного переключателя ELM327 приложение не может достоверно определить без ответа блока.\n\nРеальный протокол чтения As-Built пока не реализован: соединение с блоком и запись отключены.")
      .setNegativeButton("Закрыть",null)
      .setNeutralButton("Настройки адаптера",(d,w)->startActivity(new Intent(this,ObdConnectionActivity.class)))
      .show();
  }
}
