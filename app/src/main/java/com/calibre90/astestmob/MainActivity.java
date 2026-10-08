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
    features.addAll(StudioSettings.load(this));view=new StudioView();setContentView(view);
  }
  class StudioView extends View {
    Paint p=new Paint(3); HashMap<String,Bitmap> bitmaps=new HashMap<>();
    float sx=1,sy=1,offX=0,offY=0;
    StudioView(){super(MainActivity.this);setBackgroundColor(Color.BLACK);
      String[] keys={"header_logo","header_mazda_no_lock","admin_lock_button","active_red","inactive_1","module_info","features_panel","row_01","checkbox_empty","checkbox_checked","open_abt","save_abt","creator_link","settings","gauge_round","feature_left","feature_right","admin_dialog","about_dialog"};
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
    void card(Canvas c,float x,float y,float w,float h,float radius,boolean red){
      p.setStyle(Paint.Style.FILL);p.setShader(null);p.setColor(Color.rgb(85,87,92));c.drawRoundRect(x+1,y+3,x+w+1,y+h+3,radius,radius,p);
      p.setShader(new LinearGradient(x,y,x,y+h,Color.WHITE,Color.rgb(231,235,242),Shader.TileMode.CLAMP));
      c.drawRoundRect(x,y,x+w,y+h,radius,radius,p);p.setShader(null);
      p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(red?1.25f:1.15f);p.setColor(red?Color.rgb(225,40,48):Color.rgb(195,202,211));
      c.drawRoundRect(x+1,y+1,x+w-1,y+h-1,radius,radius,p);p.setStyle(Paint.Style.FILL);
    }
    void centered(Canvas c,String s,float x,float y,float w,float h,float size,int color){
      p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setTextSize(size);p.setColor(color);p.setStyle(Paint.Style.FILL);
      c.drawText(s,x+(w-p.measureText(s))/2,y+h/2-(p.ascent()+p.descent())/2,p);
    }
    @Override protected void onDraw(Canvas actual){super.onDraw(actual);
      sx=getWidth()/400f;sy=getHeight()/860f;actual.save();actual.scale(sx,sy);
      Canvas c=actual;c.drawColor(Color.BLACK);
      img(c,"header_mazda_no_lock",8,4,384,106);
      card(c,351,12,33,34,9,false);
      // Centered lock drawn as geometry: no emoji font baseline or glyph offsets.
      p.setShader(null);p.setColor(Color.rgb(35,40,48));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.8f);
      c.drawRoundRect(361.5f,18.5f,373.5f,34.5f,6,6,p);
      p.setStyle(Paint.Style.FILL);c.drawRoundRect(359.5f,27,375.5f,40,2.5f,2.5f,p);
      p.setColor(Color.WHITE);c.drawCircle(367.5f,32,1.3f,p);c.drawRect(366.8f,32,368.2f,36,p);
      String[] tabs={"IC","BCM","RKE","ABS"};
      card(c,10,125,380,57,13,false);
      for(int i=0;i<4;i++){
        float x=15+i*94;
        if(i==active){p.setShader(new LinearGradient(x,132,x,175,Color.rgb(255,74,79),Color.rgb(176,0,10),Shader.TileMode.CLAMP));p.setStyle(Paint.Style.FILL);c.drawRoundRect(x,131,x+89,175,9,9,p);p.setShader(null);}
        else card(c,x,131,89,44,9,false);
        centered(c,tabs[i],x,131,89,44,16,i==active?Color.WHITE:Color.BLACK);
      }
      card(c,10,190,380,82,13,true);
      txt(c,modules[active]+": "+names[active],25,222,18,Color.BLACK,true);
      p.setColor(Color.rgb(224,57,64));p.setStrokeWidth(1);c.drawLine(25,232,316,232,p);
      txt(c,"ID: "+ids[active]+"  |  Ver: "+(loaded[active]?"ABT загружен":"Загрузите файл ABT"),25,252,12,Color.rgb(91,103,119),false);
      card(c,10,279,380,91,12,true);
      p.setColor(Color.rgb(207,213,221));p.setStrokeWidth(1);c.drawLine(203,284,203,365,p);c.drawLine(15,325,203,325,p);
      String[] labels={"","",""};
      ArrayList<FeatureEngine.Feature> shown=moduleFeatures(active);
      int page=featurePage[active];
      for(int i=0;i<3&&page+i<shown.size();i++)labels[i]=shown.get(page+i).id;
      if(shown.size()>3)txt(c,(page+1)+"–"+Math.min(page+3,shown.size())+"/"+shown.size()+"  ›",288,360,10,Color.rgb(150,0,0),true);
      for(int i=0;i<3;i++){
        float x=i==2?213:20,y=i==0?288:i==1?333:288;
        card(c,x,y,25,25,5,false);
        if(page+i<shown.size()&&isChecked(shown.get(page+i))){
          rect(c,Color.rgb(210,28,37),x+4,y+4,17,17,3);
          p.setColor(Color.WHITE);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.3f);
          Path mark=new Path();mark.moveTo(x+5,y+12);mark.lineTo(x+10,y+17);mark.lineTo(x+20,y+6);c.drawPath(mark,p);p.setStyle(Paint.Style.FILL);
        }
        txt(c,labels[i],x+30,y+17,i==0?10.5f:11.5f,Color.BLACK,false);
      }
      card(c,10,376,380,337,13,true);
      int count=Math.min(Math.max(0,rows[active].size()-scrollOffset[active]),9);
      for(int i=0;i<count;i++){
        int rowIndex=i+scrollOffset[active];float y=380+i*36.5f;card(c,15,y,370,35,8,false);
        p.setColor(Color.rgb(202,209,219));p.setStrokeWidth(1);c.drawLine(165,y+4,165,y+31,p);
        String index=rowIndex<abtRows[active].size()?abtRows[active].get(rowIndex).address:ids[active]+"-"+String.format(java.util.Locale.US,"01-%02d",rowIndex+1);
        txt(c,index,28,y+23,14,Color.BLACK,false);
        String value=rows[active].get(rowIndex);
        AbtCodec.Row row=rowIndex<abtRows[active].size()?abtRows[active].get(rowIndex):null;
        String before=row==null?"":original.get(key(row));
        Set<Integer> changed=FeatureEngine.changedPositions(before==null?"":before,value);
        p.setTypeface(Typeface.create("monospace",Typeface.NORMAL));p.setTextSize(13);p.setStyle(Paint.Style.FILL);
        float px=177;int hexIndex=0;
        for(int j=0;j<value.length();j++){
          char ch=value.charAt(j);p.setColor(ch!=' '&&changed.contains(hexIndex)?Color.rgb(210,25,35):Color.BLACK);
          c.drawText(String.valueOf(ch),px,y+23,p);px+=p.measureText(String.valueOf(ch));if(ch!=' ')hexIndex++;
        }
      }
      card(c,10,720,185,52,12,true);centered(c,"Открыть ABT",10,720,185,52,15,Color.BLACK);
      card(c,205,720,185,52,12,true);centered(c,"Сохранить ABT",205,720,185,52,15,Color.BLACK);
      card(c,10,782,380,57,12,true);
      txt(c,"↗",27,818,25,Color.BLACK,true);
      txt(c,"›",355,821,34,Color.BLACK,true);
      actual.restore();
    }
    float startY;
    @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN){startY=e.getY()/sy;return true;}if(e.getAction()!=MotionEvent.ACTION_UP)return true;float x=e.getX()/sx,y=e.getY()/sy;
      if(startY>=376&&startY<=713&&y>=376&&y<=713&&Math.abs(y-startY)>18){int delta=Math.round((startY-y)/36.5f);scrollOffset[active]=Math.max(0,Math.min(Math.max(0,rowCount(active)-9),scrollOffset[active]+delta));invalidate();return true;}
      if(y>=127&&y<=183){active=Math.min(3,Math.max(0,(int)((x-12)/95)));invalidate();return true;}
      if(x>=346&&y>=8&&y<=70){admin();return true;}
      if(y>=277&&y<=367){if(y>351&&x>280&&moduleFeatures(active).size()>3){featurePage[active]=(featurePage[active]+3)%moduleFeatures(active).size();invalidate();return true;}int i=x>200?2:y>324?1:0;toggleFeature(featurePage[active]+i);return true;}
      if(y>=380&&y<=713&&Math.abs(y-startY)<=18){int n=scrollOffset[active]+(int)((y-380)/36.5f);if(n>=0&&n<abtRows[active].size())editHexRow(n);return true;}
      if(y>=717&&y<=777){if(x<200)open();else save();return true;}
      if(y>=779){about();return true;}
      return true;
    }
  }
  final FeatureEngine engine=new FeatureEngine();
  final ArrayList<AbtCodec.Row>[] abtRows=new ArrayList[]{new ArrayList<>(),new ArrayList<>(),new ArrayList<>(),new ArrayList<>()};
  final HashMap<String,String> original=new HashMap<>();
  final int[] scrollOffset={0,0,0,0};
  final int[] featurePage={0,0,0,0};
  final HashMap<String,Boolean> checkedFeatures=new HashMap<>();
  String featureKey(FeatureEngine.Feature f){return f.module+"|"+f.id;}
  boolean isChecked(FeatureEngine.Feature f){Boolean b=checkedFeatures.get(featureKey(f));return b!=null&&b;}
  void syncFeatureChecks(int module){for(FeatureEngine.Feature f:moduleFeatures(module)){boolean on=engine.state(f,findRowValueFor(module,f.address));checkedFeatures.put(featureKey(f),on);engine.seed(f,on);}}
  String findRowValueFor(int module,String address){AbtCodec.Row r=findRow(module,address);return r==null?"":r.value;}
  ArrayList<FeatureEngine.Feature> moduleFeatures(int module){ArrayList<FeatureEngine.Feature> result=new ArrayList<>();for(FeatureEngine.Feature f:features)if(f.module.equals(modules[module]))result.add(f);return result;}
  int rowCount(int module){return abtRows[module].size();}
  final ArrayList<FeatureEngine.Feature> features=new ArrayList<>();
  String key(AbtCodec.Row r){return r.module+"|"+r.address;}
  AbtCodec.Row findRow(int module,String address){for(AbtCodec.Row r:abtRows[module])if(r.address.equals(address))return r;return null;}
  void initFeatures(){
    features.add(new FeatureEngine.Feature("rvm","IC","720-01-02","HEX","0","8",0));
    features.add(new FeatureEngine.Feature("new_feature","IC","720-01-01","HEX","2,3","40",0));
    features.add(new FeatureEngine.Feature("keyless","IC","720-01-01","HEX","0,1","2B",0));
  }
  void toggleFeature(int index){
    ArrayList<FeatureEngine.Feature> available=moduleFeatures(active);
    if(index>=available.size()){Toast.makeText(this,"Функция для этого блока ещё не настроена",Toast.LENGTH_SHORT).show();return;}
    FeatureEngine.Feature f=available.get(index);AbtCodec.Row row=findRow(active,f.address);
    if(row==null){Toast.makeText(this,"Загрузите ABT со строкой "+f.address,Toast.LENGTH_LONG).show();return;}
    try{boolean next=!isChecked(f);engine.apply(row,f,next,features);checkedFeatures.put(featureKey(f),next);refreshRows(active);view.invalidate();}
    catch(Exception ex){Toast.makeText(this,"Ошибка функции: "+ex.getMessage(),Toast.LENGTH_LONG).show();}
  }
  void editHexRow(int index){
    if(index<0||index>=abtRows[active].size())return;
    final AbtCodec.Row row=abtRows[active].get(index);
    final EditText edit=new EditText(this);edit.setSingleLine(false);edit.setMinLines(2);
    edit.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
    edit.setTypeface(Typeface.MONOSPACE);edit.setText(row.value);edit.selectAll();
    LinearLayout container=new LinearLayout(this);container.setOrientation(LinearLayout.VERTICAL);container.setPadding(dp(16),dp(8),dp(16),0);
    TextView hint=new TextView(this);hint.setText("Введите HEX-байты. Последний байт — контрольная сумма, она пересчитывается автоматически.");
    container.addView(hint);container.addView(edit);
    new AlertDialog.Builder(this).setTitle("Редактор "+row.address).setView(container)
      .setNegativeButton("Отмена",null)
      .setPositiveButton("Сохранить",(d,w)->{
        try{
          String input=edit.getText().toString().replaceAll("\\s+","");
          if(input.isEmpty()||input.length()%2!=0||!input.matches("[0-9A-Fa-f]+"))throw new IllegalArgumentException("Допустимы только полные HEX-байты");
          if(input.length()!=AbtCodec.norm(row.value).length())throw new IllegalArgumentException("Длина строки должна остаться прежней");
          String updated=AbtCodec.recalc(row.address,input);
          row.value=updated;engine.reset();checkedFeatures.clear();syncFeatureChecks(active);
          refreshRows(active);view.invalidate();
          Toast.makeText(this,"Строка сохранена, checksum пересчитан",Toast.LENGTH_SHORT).show();
        }catch(Exception ex){Toast.makeText(this,"Ошибка HEX: "+ex.getMessage(),Toast.LENGTH_LONG).show();}
      }).show();
  }
  void refreshRows(int module){rows[module].clear();for(AbtCodec.Row row:abtRows[module])rows[module].add(row.value);}
  void loadRows(String content){
    HashMap<String,String> byPrefix=new HashMap<>();for(int i=0;i<4;i++)byPrefix.put(ids[i],modules[i]);
    List<AbtCodec.Row> parsed=AbtCodec.parse(content,byPrefix);
    if(parsed.isEmpty())throw new IllegalArgumentException("Формат ABT не распознан");
    ArrayList<AbtCodec.Row> selected=new ArrayList<>();
    for(AbtCodec.Row row:parsed)if(row.module.equals(modules[active]))selected.add(row);
    if(selected.isEmpty())throw new IllegalArgumentException("В файле нет строк блока "+modules[active]);
    for(AbtCodec.Row old:abtRows[active])original.remove(key(old));
    abtRows[active].clear();abtRows[active].addAll(selected);scrollOffset[active]=0;engine.reset();checkedFeatures.clear();
    for(AbtCodec.Row row:selected)original.put(key(row),AbtCodec.norm(row.value));
    ArrayList<FeatureEngine.Feature> available=moduleFeatures(active);
    featurePage[active]=0;syncFeatureChecks(active);
    refreshRows(active);loaded[active]=true;view.invalidate();
  }
  String findRowValue(String address){AbtCodec.Row row=findRow(active,address);return row==null?"":row.value;}
  boolean[] loaded=new boolean[4];
  void open(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,10);}
  void save(){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/octet-stream");i.putExtra(Intent.EXTRA_TITLE,modules[active]+".abt");startActivityForResult(i,11);}
  @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;
    Uri uri=data.getData();
    try{
      if(req==10){
        ByteArrayOutputStream buf=new ByteArrayOutputStream();byte[] chunk=new byte[4096];int n;
        try(InputStream in=getContentResolver().openInputStream(uri)){while((n=in.read(chunk))!=-1)buf.write(chunk,0,n);}
        loadRows(new String(buf.toByteArray(),"UTF-8"));
        Toast.makeText(this,"Загружено строк: "+abtRows[active].size(),Toast.LENGTH_SHORT).show();
      }else if(req==11){
        if(abtRows[active].isEmpty())throw new IllegalArgumentException("Сначала откройте ABT");
        String text=AbtCodec.write(abtRows[active],modules[active]);
        try(OutputStream out=getContentResolver().openOutputStream(uri)){out.write(text.getBytes("US-ASCII"));}
        Toast.makeText(this,"ABT блока "+modules[active]+" сохранён",Toast.LENGTH_SHORT).show();
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
    TextView close=caption("×",31,Color.BLACK,true);close.setGravity(Gravity.CENTER);close.setIncludeFontPadding(false);close.setPadding(0,0,0,0);
    close.setBackground(panel(Color.WHITE,Color.rgb(235,238,242),8,Color.rgb(200,205,212)));
    place(root,close,.82f,.045f,.125f,.105f,width,height);
    close.setOnClickListener(v->dialog.dismiss());
    if(isAdmin){
      TextView help=caption("Вход администратора",15,Color.DKGRAY,false);
      place(root,help,.085f,.22f,.80f,.07f,width,height);
      EditText login=new EditText(this),password=new EditText(this);
      login.setSingleLine(true);login.setTextColor(Color.BLACK);login.setHintTextColor(Color.DKGRAY);login.setHint("Логин");login.setTextSize(16);login.setPadding(dp(12),0,dp(12),0);
      login.setBackground(panel(Color.WHITE,Color.rgb(247,248,250),8,Color.rgb(192,200,209)));
      place(root,login,.08f,.33f,.84f,.125f,width,height);
      password.setSingleLine(true);password.setTextColor(Color.BLACK);password.setHintTextColor(Color.DKGRAY);password.setHint("Пароль");password.setInputType(129);password.setTextSize(16);
      password.setPadding(dp(12),0,dp(12),0);
      password.setBackground(panel(Color.WHITE,Color.rgb(247,248,250),8,Color.rgb(192,200,209)));
      place(root,password,.08f,.49f,.84f,.125f,width,height);
      TextView cancel=caption("Отмена",15,Color.BLACK,true);cancel.setGravity(Gravity.CENTER);
      cancel.setBackground(panel(Color.WHITE,Color.rgb(218,224,231),9,Color.rgb(185,193,201)));
      place(root,cancel,.08f,.75f,.39f,.13f,width,height);cancel.setOnClickListener(v->dialog.dismiss());
      TextView enter=caption("Войти",15,Color.WHITE,true);enter.setGravity(Gravity.CENTER);
      enter.setBackground(panel(Color.rgb(240,66,67),Color.rgb(169,0,8),9,Color.rgb(255,108,112)));
      place(root,enter,.53f,.75f,.39f,.13f,width,height);
      enter.setOnClickListener(v->{if(login.getText().toString().equals("admin")&&password.getText().toString().equals("admin")){dialog.dismiss();showFeatureAdmin();}else Toast.makeText(this,"Неверный логин или пароль",Toast.LENGTH_SHORT).show();});
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
  void showRowsAdmin(){
    final ArrayList<AbtCodec.Row> list=abtRows[active];
    if(!loaded[active]){Toast.makeText(this,"Сначала откройте ABT текущего блока",Toast.LENGTH_LONG).show();return;}
    LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(14),dp(8),dp(14),dp(8));
    Spinner selector=new Spinner(this);ArrayList<String> names=new ArrayList<>();
    for(AbtCodec.Row r:list)names.add(r.address);
    ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names);
    selector.setAdapter(adapter);root.addView(selector);
    EditText address=new EditText(this);address.setSingleLine(true);address.setHint("Адрес: "+ids[active]+"-01-01");root.addView(address);
    EditText value=new EditText(this);value.setSingleLine(false);value.setMinLines(2);value.setHint("HEX, включая checksum");root.addView(value);
    selector.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
      public void onItemSelected(android.widget.AdapterView<?> parent,View v,int position,long id){
        if(position>=0&&position<list.size()){address.setText(list.get(position).address);value.setText(list.get(position).value);}
      }
      public void onNothingSelected(android.widget.AdapterView<?> parent){}
    });
    LinearLayout buttons=new LinearLayout(this);root.addView(buttons);
    Button add=new Button(this);add.setText("Добавить");buttons.addView(add);
    Button change=new Button(this);change.setText("Изменить");buttons.addView(change);
    Button remove=new Button(this);remove.setText("Удалить");buttons.addView(remove);
    final boolean[] creating={false};
    add.setOnClickListener(v->{creating[0]=true;address.setText(ids[active]+"-01-01");value.setText("");Toast.makeText(this,"Введите новый адрес и HEX, затем нажмите Изменить",Toast.LENGTH_LONG).show();});
    change.setOnClickListener(v->{
      try{
        String addr=address.getText().toString().trim().toUpperCase(Locale.US);
        String hex=value.getText().toString().replaceAll("\\s+","").toUpperCase(Locale.US);
        if(!addr.matches(ids[active]+"-[0-9]{2}-[0-9]{2}"))throw new IllegalArgumentException("Адрес должен принадлежать текущему блоку");
        if(hex.length()<4||hex.length()%2!=0||!hex.matches("[0-9A-F]+"))throw new IllegalArgumentException("Неверные HEX-байты");
        String updated=AbtCodec.recalc(addr,hex);
        int selected=selector.getSelectedItemPosition();
        if(creating[0]){
          for(AbtCodec.Row r:list)if(r.address.equals(addr))throw new IllegalArgumentException("Строка уже существует");
          list.add(new AbtCodec.Row(modules[active],addr,updated,Integer.parseInt(addr.split("-")[1])));
          creating[0]=false;
        }else{
          if(selected<0||selected>=list.size())throw new IllegalArgumentException("Выберите строку");
          AbtCodec.Row old=list.get(selected);
          if(!old.address.equals(addr))throw new IllegalArgumentException("Адрес существующей строки менять нельзя — добавьте новую");
          if(AbtCodec.norm(old.value).length()!=hex.length())throw new IllegalArgumentException("Длина существующей строки должна остаться прежней");
          old.value=updated;
        }
        engine.reset();checkedFeatures.clear();syncFeatureChecks(active);refreshRows(active);
        names.clear();for(AbtCodec.Row r:list)names.add(r.address);adapter.notifyDataSetChanged();view.invalidate();
        Toast.makeText(this,"Строка обновлена; сохраните ABT в файл",Toast.LENGTH_SHORT).show();
      }catch(Exception ex){Toast.makeText(this,"Ошибка: "+ex.getMessage(),Toast.LENGTH_LONG).show();}
    });
    remove.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Удалить строку?")
      .setMessage("Удаление будет записано только при сохранении ABT.")
      .setNegativeButton("Отмена",null).setPositiveButton("Удалить",(d,w)->{
        int n=selector.getSelectedItemPosition();if(n<0||n>=list.size())return;
        list.remove(n);engine.reset();checkedFeatures.clear();syncFeatureChecks(active);refreshRows(active);
        names.clear();for(AbtCodec.Row r:list)names.add(r.address);adapter.notifyDataSetChanged();view.invalidate();
      }).show());
    new AlertDialog.Builder(this).setTitle("Админка · строки "+modules[active]).setView(root).setPositiveButton("Закрыть",null).show();
  }
  void showFeatureAdmin(){
    LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(14),dp(8),dp(14),dp(8));
    ScrollView scroll=new ScrollView(this);scroll.addView(root);
    final String[] fields={"ID","Блок IC/BCM/RKE/ABS","Адрес строки","Режим HEX/BITS","Индексы HEX или биты","Значение ON","Значение OFF","Номер байта BITS"};
    final EditText[] edits=new EditText[fields.length];
    Spinner selector=new Spinner(this);ArrayList<String> labels=new ArrayList<>();
    for(FeatureEngine.Feature f:features)labels.add(f.id+" · "+f.module+" · "+f.address);
    ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels);
    selector.setAdapter(adapter);root.addView(selector);
    for(int i=0;i<fields.length;i++){EditText ed=new EditText(this);ed.setSingleLine(true);ed.setHint(fields[i]);root.addView(ed);edits[i]=ed;}
    Runnable fill=()->{int n=selector.getSelectedItemPosition();if(n<0||n>=features.size())return;
      FeatureEngine.Feature f=features.get(n);String[] v={f.id,f.module,f.address,f.mode,f.indices,f.on,f.off,String.valueOf(f.byteIndex)};
      for(int i=0;i<v.length;i++)edits[i].setText(v[i]);
    };
    selector.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
      public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){fill.run();}
      public void onNothingSelected(android.widget.AdapterView<?> p){}
    });
    final boolean[] creating={false};
    final Runnable[] saveEntry=new Runnable[1];
    saveEntry[0]=()->{
      try{
        String id=edits[0].getText().toString().trim(),module=edits[1].getText().toString().trim().toUpperCase(Locale.US);
        String address=edits[2].getText().toString().trim().toUpperCase(Locale.US),mode=edits[3].getText().toString().trim().toUpperCase(Locale.US);
        if(id.isEmpty()||!Arrays.asList(modules).contains(module)||!address.matches("[0-9A-F]{3}-[0-9]{2}-[0-9]{2}")||!Arrays.asList("HEX","BITS").contains(mode))throw new IllegalArgumentException("Проверьте ID, блок, адрес и режим");
        int bi=Integer.parseInt(edits[7].getText().toString().trim());
        FeatureEngine.Feature f=new FeatureEngine.Feature(id,module,address,mode,edits[4].getText().toString(),edits[5].getText().toString(),edits[6].getText().toString(),bi);
        int n=selector.getSelectedItemPosition();if(!creating[0]&&n>=0&&n<features.size())features.set(n,f);else {features.add(f);creating[0]=false;}
        StudioSettings.save(this,features);engine.reset();checkedFeatures.clear();
        labels.clear();for(FeatureEngine.Feature item:features)labels.add(item.id+" · "+item.module+" · "+item.address);adapter.notifyDataSetChanged();Toast.makeText(this,"Функция сохранена",Toast.LENGTH_SHORT).show();view.invalidate();
      }catch(Exception ex){Toast.makeText(this,"Ошибка: "+ex.getMessage(),Toast.LENGTH_LONG).show();}
    };
    LinearLayout actions=new LinearLayout(this);
    Button save=new Button(this);save.setText("Изменить");actions.addView(save);save.setOnClickListener(v->saveEntry[0].run());
    Button add=new Button(this);add.setText("Добавить");actions.addView(add);add.setOnClickListener(v->{
      creating[0]=true;for(EditText ed:edits)ed.setText("");edits[7].setText("0");
      new AlertDialog.Builder(this).setTitle("Новая функция").setMessage("Заполните поля и нажмите Изменить для сохранения.").setPositiveButton("OK",null).show();
    });
    Button remove=new Button(this);remove.setText("Удалить");actions.addView(remove);remove.setOnClickListener(v->{
      int n=selector.getSelectedItemPosition();if(n<0||n>=features.size())return;
      features.remove(n);StudioSettings.save(this,features);engine.reset();for(boolean[] c:checks)Arrays.fill(c,false);
      labels.clear();for(FeatureEngine.Feature f:features)labels.add(f.id+" · "+f.module+" · "+f.address);adapter.notifyDataSetChanged();view.invalidate();
    });
    root.addView(actions);
    Button rowsAdmin=new Button(this);rowsAdmin.setText("Редактор строк ABT");root.addView(rowsAdmin);rowsAdmin.setOnClickListener(v->showRowsAdmin());
    new AlertDialog.Builder(this).setTitle("Админка · функции и биты").setView(scroll).setPositiveButton("Закрыть",null).show();
  }
  void admin(){modal(true);}
  void about(){modal(false);}
}
