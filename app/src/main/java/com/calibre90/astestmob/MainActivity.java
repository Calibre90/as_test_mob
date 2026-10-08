package com.calibre90.astestmob;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
  LinearLayout root,tabs,table; final ArrayList<ImageView> tabViews=new ArrayList<>();
  int activeTab=0; boolean leftChecked=false,rightChecked=false;
  int img(String n){ return getResources().getIdentifier(n,"drawable",getPackageName()); }
  ImageView image(String n, ImageView.ScaleType scale){
    ImageView v=new ImageView(this); v.setScaleType(scale); int id=img(n); if(id!=0)v.setImageResource(id); return v;
  }
  LinearLayout.LayoutParams lp(int w,int h){ return new LinearLayout.LayoutParams(w,h); }
  @Override public void onCreate(Bundle b){ super.onCreate(b); getWindow().setStatusBarColor(Color.BLACK); build(); }

  void build(){
    ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true);
    root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(18,12,18,18); root.setBackgroundColor(Color.rgb(5,5,5));
    scroll.addView(root);

    FrameLayout header=new FrameLayout(this);
    ImageView h=image("header_mazda6gh_asbuilt_speedometer",ImageView.ScaleType.FIT_XY); header.addView(h,new FrameLayout.LayoutParams(-1,120));
    View gear=new View(this); FrameLayout.LayoutParams gp=new FrameLayout.LayoutParams(90,100,Gravity.RIGHT|Gravity.TOP); header.addView(gear,gp); gear.setOnClickListener(v->showAdmin());
    root.addView(header,new LinearLayout.LayoutParams(-1,120));

    tabs=new LinearLayout(this); tabs.setOrientation(LinearLayout.HORIZONTAL);
    for(int i=0;i<4;i++){ final int n=i; ImageView t=image(i==0?"tab_active_red":"tab_inactive_gray_1",ImageView.ScaleType.FIT_XY);
      LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,62,1); p.setMargins(2,0,2,0); tabs.addView(t,p); tabViews.add(t); t.setOnClickListener(v->selectTab(n));}
    root.addView(tabs,new LinearLayout.LayoutParams(-1,68));

    root.addView(image("module_info_panel_with_gauge",ImageView.ScaleType.FIT_XY),new LinearLayout.LayoutParams(-1,105));

    LinearLayout options=new LinearLayout(this);
    FrameLayout left=option("option_row_left",false); FrameLayout right=option("option_row_right",true);
    options.addView(left,new LinearLayout.LayoutParams(0,92,1)); options.addView(right,new LinearLayout.LayoutParams(0,92,1)); root.addView(options);

    table=new LinearLayout(this); table.setOrientation(LinearLayout.VERTICAL);
    table.addView(image("table_header_red",ImageView.ScaleType.FIT_XY),new LinearLayout.LayoutParams(-1,42));
    for(int i=0;i<9;i++) table.addView(image("table_row_gray",ImageView.ScaleType.FIT_XY),new LinearLayout.LayoutParams(-1,47));
    root.addView(table);

    LinearLayout actions=new LinearLayout(this); actions.setPadding(0,10,0,8);
    ImageView open=image("open_file_button",ImageView.ScaleType.FIT_XY), save=image("save_file_button",ImageView.ScaleType.FIT_XY);
    LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,76,1); ap.setMargins(2,0,3,0); actions.addView(open,ap);
    LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,76,1); sp.setMargins(3,0,2,0); actions.addView(save,sp);
    open.setOnClickListener(v->pickFile()); save.setOnClickListener(v->createFile()); root.addView(actions);

    ImageView about=image("creator_link_panel",ImageView.ScaleType.FIT_XY); root.addView(about,new LinearLayout.LayoutParams(-1,96)); about.setOnClickListener(v->showAbout());
    setContentView(scroll);
  }

  FrameLayout option(String background, boolean right){
    FrameLayout f=new FrameLayout(this); f.addView(image(background,ImageView.ScaleType.FIT_XY),new FrameLayout.LayoutParams(-1,-1));
    ImageView check=image("checkbox_empty",ImageView.ScaleType.FIT_CENTER); FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(58,58,Gravity.LEFT|Gravity.CENTER_VERTICAL); cp.leftMargin=8; f.addView(check,cp);
    f.setOnClickListener(v->{ if(right) rightChecked=!rightChecked; else leftChecked=!leftChecked; check.setImageResource(img((right?rightChecked:leftChecked)?"checkbox_checked_red":"checkbox_empty")); }); return f;
  }
  void selectTab(int n){ activeTab=n; for(int i=0;i<4;i++) tabViews.get(i).setImageResource(img(i==n?"tab_active_red":"tab_inactive_gray_1")); }
  void pickFile(){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("*/*"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i,10); }
  void createFile(){ Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT); i.setType("application/octet-stream"); i.putExtra(Intent.EXTRA_TITLE,"block.abt"); startActivityForResult(i,11); }

  void showAdmin(){
    Dialog d=new Dialog(this); FrameLayout f=new FrameLayout(this); ImageView bg=image("admin_modal_full",ImageView.ScaleType.FIT_XY); f.addView(bg,new FrameLayout.LayoutParams(760,448));
    View close=new View(this); FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(100,90,Gravity.RIGHT|Gravity.TOP); f.addView(close,cp); close.setOnClickListener(v->d.dismiss());
    View cancel=new View(this); FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(270,90,Gravity.LEFT|Gravity.BOTTOM); bp.leftMargin=65; bp.bottomMargin=12; f.addView(cancel,bp); cancel.setOnClickListener(v->d.dismiss());
    d.setContentView(f); d.show();
  }
  void showAbout(){
    Dialog d=new Dialog(this); FrameLayout f=new FrameLayout(this); ImageView bg=image("about_modal_full",ImageView.ScaleType.FIT_XY); f.addView(bg,new FrameLayout.LayoutParams(700,480));
    View close=new View(this); FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(100,90,Gravity.RIGHT|Gravity.TOP); f.addView(close,cp); close.setOnClickListener(v->d.dismiss());
    View ok=new View(this); FrameLayout.LayoutParams op=new FrameLayout.LayoutParams(520,100,Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM); op.bottomMargin=12; f.addView(ok,op); ok.setOnClickListener(v->d.dismiss());
    d.setContentView(f); d.show();
  }
}
