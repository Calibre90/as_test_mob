package com.calibre90.astestmob;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;

/** FAQ reader, styled after the Studio About dialog. */
public final class StudioFaq {
  private StudioFaq(){}
  private static int dp(Activity a,float n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
  private static GradientDrawable bg(Activity a,int top,int bottom,int stroke,int radius){
    GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{top,bottom});
    g.setCornerRadius(dp(a,radius));g.setStroke(dp(a,1.5f),stroke);return g;
  }
  private static TextView text(Activity a,String value,int size,int color,boolean bold){
    TextView t=new TextView(a);t.setText(value);t.setTextColor(color);t.setTextSize(size);
    if(bold)t.setTypeface(null,1);return t;
  }
  public static JSONArray entries(Activity a){
    String json=a.getSharedPreferences("studio_faq",Activity.MODE_PRIVATE).getString("entries","[]");
    try{return new JSONArray(json);}catch(Exception ignored){return new JSONArray();}
  }
  public static void show(Activity a){
    final Dialog dialog=new Dialog(a);
    int screenW=a.getResources().getDisplayMetrics().widthPixels;
    int screenH=a.getResources().getDisplayMetrics().heightPixels;
    LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(a,10),dp(a,12),dp(a,10),dp(a,12));
    root.setBackground(bg(a,Color.rgb(252,253,254),Color.rgb(234,239,244),Color.rgb(225,50,51),14));
    LinearLayout header=new LinearLayout(a);header.setGravity(Gravity.CENTER_VERTICAL);
    header.setBackground(bg(a,Color.WHITE,Color.rgb(235,239,244),Color.rgb(215,222,230),9));
    TextView heading=text(a,"FAQ",20,Color.BLACK,true);heading.setGravity(Gravity.CENTER);
    header.addView(heading,new LinearLayout.LayoutParams(0,dp(a,52),1));
    TextView x=text(a,"×",30,Color.BLACK,true);x.setGravity(Gravity.CENTER);
    x.setBackground(bg(a,Color.WHITE,Color.rgb(235,238,242),Color.rgb(200,205,212),8));
    header.addView(x,new LinearLayout.LayoutParams(dp(a,48),dp(a,46)));
    x.setOnClickListener(v->dialog.dismiss());
    root.addView(header,new LinearLayout.LayoutParams(-1,dp(a,56)));
    LinearLayout columns=new LinearLayout(a);columns.setOrientation(LinearLayout.HORIZONTAL);
    LinearLayout left=new LinearLayout(a);left.setOrientation(LinearLayout.VERTICAL);
    ScrollView namesScroll=new ScrollView(a);namesScroll.setFillViewport(false);
    LinearLayout names=new LinearLayout(a);names.setOrientation(LinearLayout.VERTICAL);
    namesScroll.addView(names);left.addView(namesScroll,new LinearLayout.LayoutParams(-1,0,1));
    TextView up=text(a,"⌃",17,Color.rgb(160,160,160),true),down=text(a,"⌄",17,Color.rgb(160,160,160),true);
    up.setGravity(Gravity.CENTER);down.setGravity(Gravity.CENTER);
    left.addView(up,0,new LinearLayout.LayoutParams(-1,dp(a,19)));
    left.addView(down,new LinearLayout.LayoutParams(-1,dp(a,19)));
    columns.addView(left,new LinearLayout.LayoutParams(0,-1,0.36f));
    LinearLayout right=new LinearLayout(a);right.setOrientation(LinearLayout.VERTICAL);right.setPadding(dp(a,10),0,dp(a,2),0);
    TextView name=text(a,"FAQ",18,Color.BLACK,true);right.addView(name,new LinearLayout.LayoutParams(-1,dp(a,44)));
    View red=new View(a);red.setBackgroundColor(Color.rgb(220,50,55));right.addView(red,new LinearLayout.LayoutParams(-1,dp(a,2)));
    ScrollView descriptionScroll=new ScrollView(a);
    TextView description=text(a,"",14,Color.BLACK,false);description.setPadding(0,dp(a,12),dp(a,4),dp(a,8));
    descriptionScroll.addView(description);right.addView(descriptionScroll,new LinearLayout.LayoutParams(-1,0,1));
    columns.addView(right,new LinearLayout.LayoutParams(0,-1,0.64f));
    LinearLayout.LayoutParams colsLp=new LinearLayout.LayoutParams(-1,0,1);colsLp.setMargins(0,dp(a,14),0,dp(a,12));root.addView(columns,colsLp);
    JSONArray entries=entries(a);
    if(entries.length()==0){name.setText("Справочник");description.setText("Информация пока не опубликована администратором.");}
    final java.util.ArrayList<TextView> items=new java.util.ArrayList<>();
    final int[] selected={0};
    Runnable redraw=()->{
      for(int i=0;i<items.size();i++){
        TextView item=items.get(i);
        item.setBackground(bg(a,i==selected[0]?Color.rgb(179,182,187):Color.rgb(46,48,52),
          i==selected[0]?Color.rgb(144,148,154):Color.rgb(22,24,28),Color.rgb(116,118,122),6));
      }
      JSONObject row=entries.optJSONObject(selected[0]);
      if(row!=null){name.setText(row.optString("name","FAQ"));description.setText(row.optString("description",""));descriptionScroll.scrollTo(0,0);}
    };
    for(int i=0;i<entries.length();i++){
      JSONObject row=entries.optJSONObject(i);if(row==null)continue;
      final int index=i;
      TextView item=text(a,row.optString("name","Функция"),13,Color.WHITE,true);
      item.setGravity(Gravity.CENTER);item.setSingleLine(true);item.setEllipsize(android.text.TextUtils.TruncateAt.END);
      LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(a,42));lp.setMargins(0,0,dp(a,4),dp(a,5));names.addView(item,lp);
      items.add(item);item.setOnClickListener(v->{selected[0]=index;redraw.run();});
    }
    Runnable arrows=()->{
      boolean scrollable=namesScroll.getChildAt(0)!=null&&namesScroll.getChildAt(0).getHeight()>namesScroll.getHeight();
      up.setVisibility(scrollable&&namesScroll.getScrollY()>0?View.VISIBLE:View.INVISIBLE);
      down.setVisibility(scrollable&&namesScroll.getScrollY()<namesScroll.getChildAt(0).getHeight()-namesScroll.getHeight()?View.VISIBLE:View.INVISIBLE);
    };
    namesScroll.setOnScrollChangeListener((View.OnScrollChangeListener)(v,sx,sy,ox,oy)->arrows.run());
    namesScroll.post(arrows);
    up.setOnClickListener(v->namesScroll.smoothScrollBy(0,-dp(a,150)));
    down.setOnClickListener(v->namesScroll.smoothScrollBy(0,dp(a,150)));
    if(!items.isEmpty())redraw.run();
    TextView close=text(a,"Закрыть",16,Color.WHITE,true);close.setGravity(Gravity.CENTER);
    close.setBackground(bg(a,Color.rgb(245,69,69),Color.rgb(170,0,10),Color.rgb(255,103,109),9));
    root.addView(close,new LinearLayout.LayoutParams(-1,dp(a,52)));close.setOnClickListener(v->dialog.dismiss());
    dialog.setContentView(root);dialog.show();
    Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawableResource(android.R.color.transparent);
      window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);window.setDimAmount(.65f);
      window.setLayout(Math.round(screenW*.96f),Math.round(screenH*.88f));}
  }
}
