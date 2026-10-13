package com.calibre90.astestmob.obd;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import com.calibre90.astestmob.R;
import com.calibre90.astestmob.FeatureEngine;
import com.calibre90.astestmob.StudioSettings;
import com.calibre90.astestmob.StudioFaq;
import com.calibre90.astestmob.ObdStudioShared;
import java.util.*;

/** Studio-styled ECU editor. All ECU I/O and writing remain disabled until verified. */
public final class AsBuiltEcuActivity extends Activity {
  private static final int RED=0xffd01f2d, DARK=0xff1b1b20, GREY=0xff606776;
  private final AsBuiltSafetySession session=new AsBuiltSafetySession();
  private final String[] modules={"IC","BCM","RKE","ABS","AFS"};
  private LinearLayout root, options, tabs;
  private TextView status, header, count;
  private Button read, write;
  private String selected="IC";
  private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
  private GradientDrawable metal(boolean active,int radius){
    GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
      active?new int[]{0xffff646b,0xffeb2634,0xffaf0015}:new int[]{Color.WHITE,0xfff2f4f8,0xffdfe3ea});
    d.setCornerRadius(dp(radius));d.setStroke(dp(1.5f),active?0xffa20d19:0xffaeb6c1);return d;
  }
  private GradientDrawable panel(){
    GradientDrawable d=metal(false,14);d.setStroke(dp(1.5f),RED);return d;
  }
  private TextView label(String s,int size,boolean bold){
    TextView t=new TextView(this);t.setText(s);t.setTextColor(DARK);t.setTextSize(size);
    if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
  }
  private void add(LinearLayout parent,View v,int top){
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(top);parent.addView(v,lp);
  }
  private TextView action(String s){
    TextView t=label(s,15,true);t.setGravity(Gravity.CENTER);
    t.setBackground(metal(false,13));t.setPadding(dp(9),dp(16),dp(9),dp(16));return t;
  }
  @Override public void onCreate(Bundle state){
    super.onCreate(state);
    getWindow().setStatusBarColor(Color.BLACK);
    getWindow().setNavigationBarColor(Color.BLACK);
    getWindow().getDecorView().setSystemUiVisibility(0);
    ScrollView scroll=new ScrollView(this);
    scroll.setFillViewport(true);scroll.setBackgroundColor(Color.BLACK);
    root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(9),dp(7),dp(9),dp(15));scroll.addView(root);setContentView(scroll);

    ImageView banner=new ImageView(this);banner.setImageResource(R.drawable.splash_studio);
    banner.setScaleType(ImageView.ScaleType.CENTER_CROP);
    root.addView(banner,new LinearLayout.LayoutParams(-1,dp(112)));

    tabs=new LinearLayout(this);tabs.setOrientation(LinearLayout.HORIZONTAL);
    tabs.setPadding(dp(4),dp(5),dp(4),dp(5));tabs.setBackground(panel());add(root,tabs,12);

    LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);
    info.setPadding(dp(13),dp(13),dp(13),dp(13));info.setBackground(panel());add(root,info,10);
    header=label("",19,true);info.addView(header);
    status=label("",13,false);add(info,status,7);
    read=new Button(this);read.setText("ПОДКЛЮЧИТЬСЯ К БЛОКУ");read.setTextSize(14);
    read.setTextColor(DARK);read.setAllCaps(false);read.setBackground(metal(false,11));
    add(info,read,10);read.setOnClickListener(v->showBusDialog());

    options=new LinearLayout(this);options.setOrientation(LinearLayout.VERTICAL);
    options.setPadding(dp(8),dp(12),dp(8),dp(12));options.setBackground(panel());add(root,options,10);
    count=label("",15,true);options.addView(count);
    TextView note=label("Функции из каталога Studio. Галочки станут доступны после чтения блока.",12,false);
    add(options,note,5);

    write=new Button(this);write.setText("ЗАПИСАТЬ В БЛОК");write.setAllCaps(false);
    write.setTextSize(16);write.setTextColor(Color.WHITE);write.setBackground(metal(true,12));write.setEnabled(false);add(root,write,12);

    LinearLayout footer=new LinearLayout(this);footer.setOrientation(LinearLayout.HORIZONTAL);add(root,footer,12);
    TextView about=action("О программе");TextView faq=action("FAQ");
    LinearLayout.LayoutParams l=new LinearLayout.LayoutParams(0,dp(58),1);l.rightMargin=dp(5);
    footer.addView(about,l);
    LinearLayout.LayoutParams r=new LinearLayout.LayoutParams(0,dp(58),1);r.leftMargin=dp(5);
    footer.addView(faq,r);
    about.setOnClickListener(v->ObdStudioShared.about(this));
    faq.setOnClickListener(v->StudioFaq.show(this));
    select("IC");
    com.calibre90.astestmob.StudioLicenseEntry.requireActivation(this,()->ObdStudioShared.refresh(this,()->select(selected)));
  }
  private void select(String ecu){
    selected=ecu;session.clear();
    tabs.removeAllViews();
    for(String module:modules){
      TextView t=label(module,13,true);boolean active=module.equals(selected);
      t.setTextColor(active?Color.WHITE:DARK);t.setGravity(Gravity.CENTER);
      t.setBackground(metal(active,10));
      LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(47),1);
      p.leftMargin=dp(2);p.rightMargin=dp(2);tabs.addView(t,p);
      t.setOnClickListener(v->select(module));
    }
    String fallback=ecu.equals("IC")?"Instrument Cluster":ecu.equals("BCM")?"Body Control Module":
      ecu.equals("RKE")?"Remote Keyless Entry":ecu.equals("ABS")?"Anti-lock Braking System":"Adaptive Front-lighting System";
    header.setText(ecu+": "+StudioSettings.moduleName(this,ecu,fallback));
    status.setText("Состояние: не подключён. Конфигурация не прочитана.");
    write.setEnabled(false);
    // Do not display hard-coded features for every module: use published Studio definitions.
    while(options.getChildCount()>2)options.removeViewAt(2);
    int total=0;
    for(FeatureEngine.Feature feature:StudioSettings.load(this)){
      if(!ecu.equalsIgnoreCase(feature.module))continue;
      total++;
      LinearLayout item=new LinearLayout(this);item.setGravity(Gravity.CENTER_VERTICAL);
      item.setPadding(dp(9),dp(8),dp(7),dp(8));item.setBackground(metal(false,9));
      LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(-1,dp(64));ip.topMargin=dp(7);
      options.addView(item,ip);
      // Large visible empty box; disabled until a verified ECU read provides actual state.
      TextView check=label("□",31,true);check.setTextColor(0xff777f8a);
      item.addView(check,new LinearLayout.LayoutParams(dp(49),-2));
      TextView title=label(feature.label,15,true);item.addView(title,new LinearLayout.LayoutParams(0,-2,1));
      item.setAlpha(0.76f);
    }
    count.setText("ОПЦИИ БЛОКА "+ecu+"  •  "+total);
    if(total==0){
      TextView empty=label("Для этого блока в текущем каталоге нет настроенных опций. Добавить их можно через админку Studio.",14,false);
      add(options,empty,14);
    }
  }
  private void showBusDialog(){
    new AlertDialog.Builder(this).setTitle("Подключение к "+selected)
      .setMessage("Необходимо определить нужную шину HS/MS-CAN по проверенной карте автомобиля и получить ответ выбранного ЭБУ.\n\nПока протокол чтения As-Built не реализован, кнопки изменения и записи остаются заблокированы.")
      .setNegativeButton("Закрыть",null)
      .setNeutralButton("Настроить адаптер",(d,w)->startActivity(new Intent(this,ObdConnectionActivity.class)))
      .show();
  }
}
