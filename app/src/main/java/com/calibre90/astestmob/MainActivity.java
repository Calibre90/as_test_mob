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
  @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.BLACK);getWindow().setNavigationBarColor(Color.BLACK);
    for(int i=0;i<4;i++){rows[i]=new ArrayList<>();for(String s:defaults)rows[i].add(s);}
    view=new StudioView();setContentView(view);
  }
  class StudioView extends View {
    Paint p=new Paint(3); HashMap<String,Bitmap> bitmaps=new HashMap<>();
    float sx=1,sy=1,offX=0,offY=0;
    StudioView(){super(MainActivity.this);setBackgroundColor(Color.rgb(239,241,244));
      String[] keys={"header_logo","active_red","inactive_1","module_info","features_panel","row_01","checkbox_empty","checkbox_checked","open_abt","save_abt","creator_link","settings","gauge_round","main_screen_content","feature_left","feature_right"};
      for(String key:keys){int id=getResources().getIdentifier(key,"drawable",getPackageName());if(id!=0)bitmaps.put(key,BitmapFactory.decodeResource(getResources(),id));}
    }
    void img(Canvas c,String key,float x,float y,float w,float h){Bitmap b=bitmaps.get(key);if(b!=null){p.setColor(Color.WHITE);p.setAlpha(255);c.drawBitmap(b,null,new RectF(x,y,x+w,y+h),p);}}
    void rect(Canvas c,int color,float x,float y,float w,float h,float r){p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRoundRect(x,y,x+w,y+h,r,r,p);}
    void txt(Canvas c,String s,float x,float y,float size,int color,boolean bold){p.setColor(color);p.setTypeface(bold?Typeface.create("sans-serif",Typeface.BOLD):Typeface.create("sans-serif",Typeface.NORMAL));p.setTextSize(size);p.setStyle(Paint.Style.FILL);c.drawText(s,x,y,p);}
    @Override protected void onDraw(Canvas actual){super.onDraw(actual);
      float w=getWidth(),h=getHeight();sx=w/400f;sy=h/860f;actual.save();actual.scale(sx,sy);
      Canvas c=actual; c.drawColor(Color.rgb(242,243,246));
      img(c,"main_screen_content",0,0,400,860);
      rect(c,Color.rgb(243,245,247),7,0,386,137,4);
      img(c,"header_logo",8,8,384,125);
      // System status icons are intentionally not painted into the application.
      String[] tabs={"IC","BCM","RKE","ABS"};
      for(int i=0;i<4;i++){float x=12+i*95;img(c,i==active?"active_red":"inactive_1",x,143,92,48);txt(c,tabs[i],x+30,174,16,i==active?Color.WHITE:Color.BLACK,true);}
      img(c,"module_info",11,201,378,79);
      txt(c,modules[active]+": "+names[active],23,231,17,Color.BLACK,true);
      txt(c,"ID: "+ids[active]+"  |  Ver: "+(loaded[active]?"ABT загружен":"Загрузите файл ABT"),23,257,12,Color.DKGRAY,false);
      img(c,"features_panel",11,288,378,91);
      String[] labels={"RVM / контроль слепых зон","Новая функция","Keyless ON/OFF"};
      for(int i=0;i<3;i++){float x=i==2?209:20,y=i==0?302:i==1?341:302;img(c,checks[active][i]?"checkbox_checked":"checkbox_empty",x,y,26,27);txt(c,labels[i],x+31,y+19,i==0?11:12,Color.BLACK,false);}
      int count=Math.min(rows[active].size(),9);
      for(int i=0;i<count;i++){float y=389+i*37;img(c,"row_01",11,y,378,36);
        String index=ids[active]+"-"+(i==8?"02-01":String.format(java.util.Locale.US,"01-%02d",i+1));
        txt(c,index,29,y+24,15,Color.BLACK,false);
        txt(c,rows[active].get(i),174,y+24,15,Color.BLACK,false);
      }
      img(c,"open_abt",12,730,181,55);img(c,"save_abt",205,730,183,55);
      txt(c,"Открыть ABT",55,763,13,Color.BLACK,true);txt(c,"Сохранить ABT",244,763,13,Color.BLACK,true);
      img(c,"creator_link",11,792,378,61);txt(c,"Dim304",134,826,16,Color.BLACK,true);
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
  void admin(){final EditText user=new EditText(this),pass=new EditText(this);user.setHint("Логин");pass.setHint("Пароль");pass.setInputType(129);
    LinearLayout box=new LinearLayout(this);box.setPadding(28,10,28,10);box.setOrientation(1);box.addView(user);box.addView(pass);
    new AlertDialog.Builder(this).setTitle("Админка").setView(box).setNegativeButton("Отмена",null).setPositiveButton("Войти",(d,w)->Toast.makeText(this,"Настройки администратора пока не перенесены",Toast.LENGTH_LONG).show()).show();
  }
  void about(){new AlertDialog.Builder(this).setTitle("О программе").setMessage("MAZDA 6 GH AS-BUILT STUDIO\\nDim304").setPositiveButton("OK",null).show();}
}
