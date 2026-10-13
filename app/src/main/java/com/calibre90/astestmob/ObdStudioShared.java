package com.calibre90.astestmob;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;
import android.content.Intent;
import android.net.Uri;

/** Shared published Studio catalog and branded information window for the separate OBD package. */
public final class ObdStudioShared {
  private static final String URL="https://raw.githubusercontent.com/Calibre90/as_test_mob/main/publication/studio-published-settings.json";
  private ObdStudioShared(){}
  public static void refresh(Activity activity,Runnable onUpdated){
    new Thread(()->{
      boolean changed=FullCatalogSync.refresh(activity.getApplicationContext(),URL);
      if(changed)activity.runOnUiThread(()->{
        if(!activity.isFinishing()&&!activity.isDestroyed())onUpdated.run();
      });
    }).start();
  }
  private static int dp(Activity a,int n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
  private static GradientDrawable bg(Activity a,boolean red){
    GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
      red?new int[]{0xffff5960,0xffae0012}:new int[]{Color.WHITE,0xffe7eaf1});
    d.setCornerRadius(dp(a,12));d.setStroke(dp(a,2),0xffd02130);return d;
  }
  private static TextView text(Activity a,String s,int size,boolean bold){
    TextView t=new TextView(a);t.setText(s);t.setTextSize(size);t.setTextColor(Color.BLACK);
    if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
  }
  public static void about(Activity a){
    Dialog dialog=new Dialog(a);
    LinearLayout body=new LinearLayout(a);body.setOrientation(LinearLayout.VERTICAL);
    body.setPadding(dp(a,14),dp(a,14),dp(a,14),dp(a,14));body.setBackground(bg(a,false));
    TextView title=text(a,"О программе",22,true);title.setGravity(Gravity.CENTER);body.addView(title);
    TextView name=text(a,"Mazda 6 GH AS-Built Studio",20,true);name.setGravity(Gravity.CENTER);
    LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);np.topMargin=dp(a,20);body.addView(name,np);
    View line=new View(a);line.setBackgroundColor(0xffd02130);
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(a,2));lp.topMargin=dp(a,9);body.addView(line,lp);
    ScrollView scroll=new ScrollView(a);TextView description=text(a,StudioSettings.appearance(a,"about_description","Редактор As-Built для Mazda 6 GH"),15,false);
    description.setPadding(dp(a,5),dp(a,13),dp(a,5),dp(a,13));scroll.addView(description);
    LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,0,1);body.addView(scroll,sp);
    String[] keys={"creator1","creator2"};
    for(int i=0;i<keys.length;i++){
      String k=keys[i],def=i==0?"Dim304":"Wolis11";
      String role=StudioSettings.appearance(a,k+"_role",i==0?"Разработчик":"Помощник");
      String person=StudioSettings.appearance(a,k+"_name",def);
      String url=StudioSettings.appearance(a,k+"_url","https://www.drive2.ru/users/"+def.toLowerCase()+"/");
      TextView creator=text(a,role+": "+person+"  •  Drive2",14,true);
      creator.setPadding(dp(a,6),dp(a,9),dp(a,6),dp(a,9));body.addView(creator);
      creator.setOnClickListener(v->{try{a.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception ignored){}});
    }
    TextView close=text(a,"Закрыть",17,true);close.setGravity(Gravity.CENTER);
    close.setTextColor(Color.WHITE);close.setBackground(bg(a,true));
    LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(a,54));cp.topMargin=dp(a,15);body.addView(close,cp);
    close.setOnClickListener(v->dialog.dismiss());
    dialog.setContentView(body);
    Window w=dialog.getWindow();if(w!=null){
      w.setBackgroundDrawableResource(android.R.color.transparent);
      w.setLayout((int)(a.getResources().getDisplayMetrics().widthPixels*.94f),
        (int)(a.getResources().getDisplayMetrics().heightPixels*.78f));
    }
    dialog.show();
    w=dialog.getWindow();if(w!=null)w.setLayout((int)(a.getResources().getDisplayMetrics().widthPixels*.94f),
      (int)(a.getResources().getDisplayMetrics().heightPixels*.78f));
  }
}
