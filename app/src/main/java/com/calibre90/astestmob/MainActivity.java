package com.calibre90.astestmob;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.io.*;

public class MainActivity extends Activity {
  StudioView view;
  final String[] modules={"IC","BCM","RKE","ABS"};
  final String[] ids={"720","726","731","760"};
  final String[] names={"Instrument Cluster","Body Control Module","Remote Keyless Entry","Anti-lock Brake System"};
  final String[] defaults={"2B00 7126 806B","000E F255 E766","0F0E C236 CC0C","F0FD ECCC EEBF","A461 7C00 00AE","00A0 0102 10E1","0822 FF08 0868","5852 00DA","C834 385E"};
  ArrayList<String>[] rows=new ArrayList[4];
  boolean[][] checks=new boolean[4][3];
  int active=0;
  @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.BLACK);getWindow().setNavigationBarColor(Color.BLACK);getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
    for(int i=0;i<4;i++){rows[i]=new ArrayList<>();for(String s:defaults)rows[i].add(s);}
    view=new StudioView();setContentView(view);
  }
  class StudioView extends View {
    Paint p=new Paint(3); HashMap<String,Bitmap> bitmaps=new HashMap<>();
    float sx=1,sy=1,offX=0,offY=0;
    StudioView(){super(MainActivity.this);setBackgroundColor(Color.BLACK);
      String[] keys={"header_logo","active_red","inactive_1","module_info","features_panel","row_01","checkbox_empty","checkbox_checked","open_abt","save_abt","creator_link","settings","gauge_round","feature_left","feature_right","admin_dialog","about_dialog"};
      for(String key:keys){int id=getResources().getIdentifier(key,"drawable",getPackageName());if(id!=0)bitmaps.put(key,BitmapFactory.decodeResource(getResources(),id));}
    }
    void img(Canvas c,String key,float x,float y,float w,float h){Bitmap b=bitmaps.get(key);if(b!=null){p.setColor(Color.WHITE);p.setAlpha(255);c.drawBitmap(b,null,new RectF(x,y,x+w,y+h),p);}}
    void rect(Canvas c,int color,float x,float y,float w,float h,float r){p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRoundRect(x,y,x+w,y+h,r,r,p);}
    void txt(Canvas c,String s,float x,float y,float size,int color,boolean bold){p.setColor(color);p.setTypeface(bold?Typeface.create("sans-serif",Typeface.BOLD):Typeface.create("sans-serif",Typeface.NORMAL));p.setTextSize(size);p.setStyle(Paint.Style.FILL);c.drawText(s,x,y,p);}
    void cleanButton(Canvas c,float x,float y,float w,float h,String label){
      p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(x,y,x,y+h,new int[]{Color.WHITE,Color.rgb(247,248,250),Color.rgb(218,223,229)},null,Shader.TileMode.CLAMP));
      c.drawRoundRect(x,y,x+w,y+h,9,9,p);p.setShader(null);
      p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.6f);p.setColor(Color.rgb(179,187,196));
      c.drawRoundRect(x+1,y+1,x+w-1,y+h-1,9,9,p);p.setStyle(Paint.Style.FILL);
      p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setTextSize(15);p.setColor(Color.BLACK);
      c.drawText(label,x+(w-p.measureText(label))/2f,y+h/2f-(p.ascent()+p.descent())/2f,p);
    }
    @Override protected void onDraw(Canvas actual){super.onDraw(actual);
      float w=getWidth(),h=getHeight();sx=w/400f;sy=h/860f;actual.save();actual.scale(sx,sy);
      Canvas c=actual; c.drawColor(Color.BLACK);
      // No full-screen screenshot as a background: only isolated component assets.
      // Black backing matches the reference shell; do not paint a white background behind the header.
      img(c,"header_logo",8,8,384,113);
      // Hide the large icon baked into the header and draw a small, clean lock.
      // The lock is deliberately much smaller than the previous 51x57 badge.
      rect(c,Color.rgb(249,249,250),326,43,51,57,7);
      rect(c,Color.rgb(253,253,254),336,53,32,37,7);
      p.setColor(Color.rgb(180,187,196));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.0f);
      c.drawRoundRect(336,53,368,90,7,7,p);
      p.setColor(Color.rgb(35,39,47));p.setStrokeWidth(2.4f);
      c.drawRoundRect(346,60,358,76,6,6,p);p.setStyle(Paint.Style.FILL);
      rect(c,Color.rgb(35,39,47),343,71,18,14,3);
      rect(c,Color.WHITE,351,75,2,6,1);
      // System status icons are intentionally not painted into the application.
      String[] tabs={"IC","BCM","RKE","ABS"};
      for(int i=0;i<4;i++){float x=12+i*95;img(c,i==active?"active_red":"inactive_1",x,143,92,48);txt(c,tabs[i],x+30,174,16,i==active?Color.WHITE:Color.BLACK,true);}
      img(c,"module_info",11,201,378,79);
      txt(c,modules[active]+": "+names[active],23,231,17,Color.BLACK,true);
      txt(c,"ID: "+ids[active]+"  |  Ver: "+(loaded[active]?"ABT загружен":"Загрузите файл ABT"),23,257,12,Color.DKGRAY,false);
      // Build clean feature rows, rather than layering checkboxes on baked-in screenshots.
      rect(c,Color.rgb(218,222,228),11,288,378,94,11);
      rect(c,Color.WHITE,13,290,374,90,10);
      p.setColor(Color.rgb(215,219,224));p.setStrokeWidth(1);
      c.drawLine(203,291,203,379,p);c.drawLine(13,335,203,335,p);
      String[] labels={"RVM / контроль слепых зон","Новая функция","Keyless ON/OFF"};
      for(int i=0;i<3;i++){
        float x=i==2?214:22,y=i==0?300:i==1?346:300;
        p.setColor(Color.rgb(90,102,116));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.8f);
        c.drawRoundRect(x,y,x+24,y+24,4,4,p);p.setStyle(Paint.Style.FILL);
        if(checks[active][i]){
          rect(c,Color.rgb(44,55,67),x+3,y+3,18,18,3);
          p.setColor(Color.WHITE);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.4f);
          Path mark=new Path();mark.moveTo(x+5,y+12);mark.lineTo(x+10,y+17);mark.lineTo(x+20,y+6);c.drawPath(mark,p);p.setStyle(Paint.Style.FILL);
        }
        txt(c,labels[i],x+29,y+17,i==0?10.5f:11.5f,Color.BLACK,false);
      }
      int count=Math.min(rows[active].size(),9);
      for(int i=0;i<count;i++){float y=389+i*37;img(c,"row_01",11,y,378,36);
        String index=ids[active]+"-"+(i==8?"02-01":String.format(java.util.Locale.US,"01-%02d",i+1));
        txt(c,index,29,y+24,15,Color.BLACK,false);
        txt(c,rows[active].get(i),174,y+24,15,Color.BLACK,false);
      }
      // Icon-free buttons: the previous PNGs had folder/disk glyphs baked in.
      cleanButton(c,12,730,181,55,"Открыть ABT");
      cleanButton(c,205,730,183,55,"Сохранить ABT");
      img(c,"creator_link",11,792,378,61);
      actual.restore();
    }
    @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()!=MotionEvent.ACTION_UP)return true;float x=e.getX()/sx,y=e.getY()/sy;
      if(y>=140&&y<=196){active=Math.min(3,Math.max(0,(int)((x-12)/95)));invalidate();return true;}
      if(x>330&&y<135){admin();return true;}
      if(y>=290&&y<=380){int i=x>200?2:y>337?1:0;checks[active][i]=!checks[active][i];invalidate();return true;}
      if(y>=730&&y<=790){if(x<200)open();else save();return true;}
      if(y>=790){about();return true;}
      return true;
    }
  }
  boolean[] loaded=new boolean[4];
  void open(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,10);}
  void save(){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/octet-stream");i.putExtra(Intent.EXTRA_TITLE,modules[active]+".abt");startActivityForResult(i,11);}
  @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;
    Uri uri=data.getData();
    try{
      if(req==10){BufferedReader reader=new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)));
        ArrayList<String> values=new ArrayList<>();String line;
        while((line=reader.readLine())!=null){String u=line.toUpperCase(java.util.Locale.US).trim();
          if(!u.matches(".*[0-9A-F]{3}-[0-9A-F]{2}-[0-9A-F]{2}.*"))continue;
          if(!u.contains(ids[active]+"-"))continue;
          int pos=u.indexOf(ids[active]+"-");String rest=u.substring(pos+9).replaceAll("[^0-9A-F]"," ");
          StringBuilder b=new StringBuilder();for(String tok:rest.trim().split("\\s+"))if(tok.matches("[0-9A-F]{2,4}")){if(b.length()>0)b.append(" ");b.append(tok);}
          if(b.length()>0)values.add(b.toString());
        }
        reader.close();if(values.isEmpty()){Toast.makeText(this,"Нет строк блока "+modules[active]+" в файле",Toast.LENGTH_LONG).show();return;}
        rows[active]=values;loaded[active]=true;view.invalidate();Toast.makeText(this,"Загружено строк: "+values.size(),Toast.LENGTH_SHORT).show();
      } else if(req==11){OutputStream os=getContentResolver().openOutputStream(uri);if(os==null)return;
        for(int n=0;n<rows[active].size();n++){String id=ids[active]+"-"+(n==8?"02-01":String.format(java.util.Locale.US,"01-%02d",n+1));os.write((id+" "+rows[active].get(n)+"\\n").getBytes("UTF-8"));}
        os.close();Toast.makeText(this,"Файл сохранён",Toast.LENGTH_SHORT).show();
      }
    }catch(Exception ex){Toast.makeText(this,"Ошибка ABT: "+ex.getMessage(),Toast.LENGTH_LONG).show();}
  }

  int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
  android.graphics.drawable.GradientDrawable panel(int top,int bottom,int radius,int border){
    android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable(
      android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,new int[]{top,bottom});
    g.setCornerRadius(dp(radius));g.setStroke(dp(1.5f),border);return g;
  }
  void place(FrameLayout root,View child,float left,float top,float width,float height,int totalW,int totalH){
    FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(Math.round(totalW*width),Math.round(totalH*height));
    lp.leftMargin=Math.round(totalW*left);lp.topMargin=Math.round(totalH*top);root.addView(child,lp);
  }
  TextView caption(String s,int size,int color,boolean bold){
    TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);
    t.setGravity(Gravity.CENTER_VERTICAL);if(bold)t.setTypeface(null,android.graphics.Typeface.BOLD);return t;
  }
  void modal(boolean isAdmin){
    final Dialog dialog=new Dialog(this);
    final int width=Math.round(getResources().getDisplayMetrics().widthPixels*.87f);
    final int height=Math.round(width*(isAdmin?1.24f:.94f));
    final FrameLayout root=new FrameLayout(this);
    root.setBackground(panel(Color.rgb(252,253,254),Color.rgb(234,239,244),14,Color.rgb(225,50,51)));
    root.setClipToOutline(true);
    View header=new View(this);header.setBackground(panel(Color.WHITE,Color.rgb(235,239,244),10,Color.rgb(215,222,230)));
    place(root,header,.025f,.025f,.95f,.16f,width,height);
    TextView title=caption(isAdmin?"Админка":"О программе",20,Color.BLACK,true);
    place(root,title,.16f,.045f,.60f,.11f,width,height);
    if(isAdmin){
      TextView lock=caption("🔒",25,Color.rgb(190,15,22),true);
      place(root,lock,.065f,.045f,.11f,.11f,width,height);
    }
    TextView close=caption("×",35,Color.BLACK,false);close.setGravity(Gravity.CENTER);
    close.setBackground(panel(Color.WHITE,Color.rgb(235,238,242),8,Color.rgb(200,205,212)));
    place(root,close,.82f,.045f,.125f,.105f,width,height);
    close.setOnClickListener(v->dialog.dismiss());
    if(isAdmin){
      TextView help=caption("Вход администратора",15,Color.DKGRAY,false);
      place(root,help,.085f,.22f,.80f,.07f,width,height);
      EditText login=new EditText(this),password=new EditText(this);
      login.setSingleLine(true);login.setHint("Логин");login.setTextSize(16);login.setPadding(dp(12),0,dp(12),0);
      login.setBackground(panel(Color.WHITE,Color.rgb(247,248,250),8,Color.rgb(192,200,209)));
      place(root,login,.08f,.33f,.84f,.125f,width,height);
      password.setSingleLine(true);password.setHint("Пароль");password.setInputType(129);password.setTextSize(16);
      password.setPadding(dp(12),0,dp(12),0);
      password.setBackground(panel(Color.WHITE,Color.rgb(247,248,250),8,Color.rgb(192,200,209)));
      place(root,password,.08f,.49f,.84f,.125f,width,height);
      TextView cancel=caption("Отмена",15,Color.BLACK,true);cancel.setGravity(Gravity.CENTER);
      cancel.setBackground(panel(Color.WHITE,Color.rgb(218,224,231),9,Color.rgb(185,193,201)));
      place(root,cancel,.08f,.75f,.39f,.13f,width,height);cancel.setOnClickListener(v->dialog.dismiss());
      TextView enter=caption("Войти",15,Color.WHITE,true);enter.setGravity(Gravity.CENTER);
      enter.setBackground(panel(Color.rgb(240,66,67),Color.rgb(169,0,8),9,Color.rgb(255,108,112)));
      place(root,enter,.53f,.75f,.39f,.13f,width,height);
      enter.setOnClickListener(v->{Toast.makeText(this,"Функции админки ещё не перенесены",Toast.LENGTH_LONG).show();dialog.dismiss();});
    }else{
      // Information-only layout without duplicated Mazda logo/banner.
      TextView version=caption("Версия: тестовая сборка",15,Color.DKGRAY,false);
      place(root,version,.09f,.245f,.82f,.11f,width,height);
      View divider=new View(this);divider.setBackgroundColor(Color.rgb(220,50,55));
      place(root,divider,.09f,.375f,.82f,.004f,width,height);
      TextView description=caption("Редактор As-Built для Mazda 6 GH",15,Color.BLACK,false);
      place(root,description,.09f,.42f,.84f,.12f,width,height);
      TextView author=caption("Разработчик: Dim304",15,Color.BLACK,true);
      place(root,author,.09f,.555f,.84f,.12f,width,height);
      TextView ok=caption("Закрыть",16,Color.WHITE,true);ok.setGravity(Gravity.CENTER);
      ok.setBackground(panel(Color.rgb(245,69,69),Color.rgb(170,0,10),9,Color.rgb(255,103,109)));
      place(root,ok,.09f,.765f,.82f,.14f,width,height);ok.setOnClickListener(v->dialog.dismiss());
    }
    dialog.setContentView(root);dialog.show();
    Window window=dialog.getWindow();if(window!=null){
      window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
      window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
      window.setDimAmount(.65f);window.setLayout(width,height);
    }
  }
  void admin(){modal(true);}
  void about(){modal(false);}
}
