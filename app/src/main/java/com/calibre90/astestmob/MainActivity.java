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
      img(c,"header_logo",8,8,384,125);
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
  void modal(boolean isAdmin){
    final Dialog d=new Dialog(this);
    final FrameLayout frame=new FrameLayout(this);
    int sw=getResources().getDisplayMetrics().widthPixels;
    int width=Math.round(sw*.87f);
    int height=Math.round(width*(isAdmin?646f/478f:668f/458f));
    ImageView bg=new ImageView(this);bg.setScaleType(ImageView.ScaleType.FIT_XY);
    int res=getResources().getIdentifier(isAdmin?"admin_dialog":"about_dialog","drawable",getPackageName());
    if(res!=0)bg.setImageResource(res);
    frame.addView(bg,new FrameLayout.LayoutParams(width,height));
    View close=new View(this);FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(Math.round(width*.17f),Math.round(height*.12f),Gravity.RIGHT|Gravity.TOP);
    frame.addView(close,cp);close.setOnClickListener(v->d.dismiss());
    if(isAdmin){
      EditText user=new EditText(this),pass=new EditText(this);
      user.setSingleLine(true);pass.setSingleLine(true);pass.setInputType(129);
      user.setTextColor(Color.BLACK);pass.setTextColor(Color.BLACK);
      user.setTextSize(15);pass.setTextSize(15);
      user.setBackgroundColor(Color.TRANSPARENT);pass.setBackgroundColor(Color.TRANSPARENT);
      FrameLayout.LayoutParams up=new FrameLayout.LayoutParams(Math.round(width*.59f),Math.round(height*.12f));
      up.leftMargin=Math.round(width*.31f);up.topMargin=Math.round(height*.38f);frame.addView(user,up);
      FrameLayout.LayoutParams pp=new FrameLayout.LayoutParams(Math.round(width*.59f),Math.round(height*.12f));
      pp.leftMargin=Math.round(width*.31f);pp.topMargin=Math.round(height*.55f);frame.addView(pass,pp);
      View cancel=new View(this);FrameLayout.LayoutParams ca=new FrameLayout.LayoutParams(Math.round(width*.40f),Math.round(height*.16f));
      ca.leftMargin=Math.round(width*.09f);ca.topMargin=Math.round(height*.77f);frame.addView(cancel,ca);cancel.setOnClickListener(v->d.dismiss());
      View login=new View(this);FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(Math.round(width*.40f),Math.round(height*.16f));
      lp.leftMargin=Math.round(width*.53f);lp.topMargin=Math.round(height*.77f);frame.addView(login,lp);
      login.setOnClickListener(v->{Toast.makeText(this,"Функции админки ещё не перенесены",Toast.LENGTH_LONG).show();d.dismiss();});
    }else{
      TextView ver=new TextView(this);ver.setText("Версия: тестовая сборка");ver.setTextColor(Color.DKGRAY);ver.setTextSize(13);
      FrameLayout.LayoutParams vp=new FrameLayout.LayoutParams(-2,-2);vp.leftMargin=Math.round(width*.13f);vp.topMargin=Math.round(height*.59f);frame.addView(ver,vp);
      TextView credit=new TextView(this);credit.setText("Dim304");credit.setTextColor(Color.BLACK);credit.setTextSize(16);
      FrameLayout.LayoutParams cr=new FrameLayout.LayoutParams(-2,-2);cr.leftMargin=Math.round(width*.15f);cr.topMargin=Math.round(height*.69f);frame.addView(credit,cr);
      View ok=new View(this);FrameLayout.LayoutParams op=new FrameLayout.LayoutParams(Math.round(width*.75f),Math.round(height*.17f));
      op.leftMargin=Math.round(width*.12f);op.topMargin=Math.round(height*.79f);frame.addView(ok,op);ok.setOnClickListener(v->d.dismiss());
    }
    d.setContentView(frame);Window win=d.getWindow();d.show();win=d.getWindow();
    if(win!=null){win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));win.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);win.setDimAmount(.62f);win.setLayout(width,height);}
  }
  void admin(){modal(true);}
  void about(){modal(false);}
}
