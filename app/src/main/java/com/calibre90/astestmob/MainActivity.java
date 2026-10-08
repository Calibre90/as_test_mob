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
    for(int i=0;i<4;i++)rows[i]=new ArrayList<>();
    features.addAll(StudioSettings.load(this));seedBuiltInRows();restoreAdminRows();
    setTitle(StudioSettings.appearance(this,"title","Mazda 6 GH As-Built Studio"));view=new StudioView();setContentView(view);
  }
  /** Built-in sample configuration: available before importing any vehicle ABT. */
  void seedBuiltInRows(){
    for(int n=0;n<defaults.length;n++){
      String address="720-"+(n<8?"01-"+String.format(Locale.US,"%02d",n+1):"02-01");
      AbtCodec.Row row=new AbtCodec.Row("IC",address,defaults[n],n<8?1:2);
      abtRows[0].add(row);original.put(key(row),AbtCodec.norm(row.value));
    }
    refreshRows(0);syncFeatureChecks(0);
  }
  void saveAdminRows(int module){
    org.json.JSONArray array=new org.json.JSONArray();
    for(AbtCodec.Row row:abtRows[module]){
      org.json.JSONObject item=new org.json.JSONObject();
      try{item.put("address",row.address);item.put("value",row.value);array.put(item);}
      catch(org.json.JSONException ex){throw new IllegalStateException(ex);}
    }
    getSharedPreferences("studio_admin_rows",MODE_PRIVATE).edit().putString(modules[module],array.toString()).apply();
  }
  /** On an imported ABT, persist only the administrator's explicit change,
      never the rest of the vehicle file as the built-in catalog. */
  void saveAdminRowChange(int module,String address,boolean deleted){
    if(!loaded[module]){saveAdminRows(module);return;}
    android.content.SharedPreferences prefs=getSharedPreferences("studio_admin_rows",MODE_PRIVATE);
    String saved=prefs.getString(modules[module],null);
    try{
      org.json.JSONArray source=new org.json.JSONArray(saved==null?"[]":saved);
      org.json.JSONArray result=new org.json.JSONArray();
      if(saved==null&&module==0){
        for(int n=0;n<defaults.length;n++){
          String a="720-"+(n<8?"01-"+String.format(Locale.US,"%02d",n+1):"02-01");
          org.json.JSONObject item=new org.json.JSONObject();
          item.put("address",a);item.put("value",defaults[n]);result.put(item);
        }
        source=result;result=new org.json.JSONArray();
      }
      for(int i=0;i<source.length();i++){
        org.json.JSONObject item=source.getJSONObject(i);
        if(!address.equals(item.getString("address")))result.put(item);
      }
      if(!deleted){
        AbtCodec.Row row=findRow(module,address);
        if(row!=null){
          org.json.JSONObject item=new org.json.JSONObject();
          item.put("address",row.address);item.put("value",row.value);result.put(item);
        }
      }
      prefs.edit().putString(modules[module],result.toString()).apply();
    }catch(org.json.JSONException ex){throw new IllegalStateException("Cannot persist admin row",ex);}
  }
  void restoreAdminRows(){
    android.content.SharedPreferences prefs=getSharedPreferences("studio_admin_rows",MODE_PRIVATE);
    for(int m=0;m<modules.length;m++){
      String saved=prefs.getString(modules[m],null);
      if(saved==null)continue;
      try{
        org.json.JSONArray array=new org.json.JSONArray(saved);
        ArrayList<AbtCodec.Row> restored=new ArrayList<>();
        for(int i=0;i<array.length();i++){
          org.json.JSONObject item=array.getJSONObject(i);
          String address=item.getString("address"),value=item.getString("value");
          String hex=AbtCodec.norm(value);
          if(!address.matches(ids[m]+"-[0-9]{2}-[0-9]{2}")||!hex.matches("[0-9A-F]{4,}")||hex.length()%2!=0)throw new IllegalArgumentException("Invalid saved row");
          for(AbtCodec.Row previous:restored)if(previous.address.equals(address))throw new IllegalArgumentException("Duplicate saved row "+address);
          restored.add(new AbtCodec.Row(modules[m],address,value,Integer.parseInt(address.split("-")[1])));
        }
        for(AbtCodec.Row old:abtRows[m])original.remove(key(old));
        abtRows[m].clear();abtRows[m].addAll(restored);
        for(AbtCodec.Row row:restored)original.put(key(row),AbtCodec.norm(row.value));
        refreshRows(m);syncFeatureChecks(m);
      }catch(Exception ex){android.util.Log.e("ASBuilt","Cannot restore admin rows for "+modules[m],ex);}
    }
  }
  int appearanceColor(String key,int fallback){
    String value=StudioSettings.appearance(this,key,"").trim();
    if(value.isEmpty())return fallback;
    try{
      if(value.matches("(?i)[0-9a-f]{6}"))value="#"+value;
      return Color.parseColor(value);
    }catch(IllegalArgumentException ex){return fallback;}
  }
  int featureColumns(){
    try{return Math.max(1,Math.min(4,Integer.parseInt(StudioSettings.appearance(this,"feature_columns","3").trim())));}
    catch(Exception ignored){return 3;}
  }
  int featureSlots(){return featureColumns()==1?3:featureColumns();}
  float featureX(int index){
    int columns=featureColumns();
    if(columns==3)return index==2?213:20;
    return 20+(index%columns)*(360f/columns);
  }
  float featureY(int index){
    int columns=featureColumns();
    if(columns==3)return index==1?333:288;
    return columns==1?285+index*28:288;
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
    void txtFit(Canvas c,String value,float x,float y,float size,int color,float maxWidth){
      String display=value==null?"":value;
      p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
      p.setTextSize(size);
      if(p.measureText(display)>maxWidth){
        String ellipsis="…";
        while(!display.isEmpty()&&p.measureText(display+ellipsis)>maxWidth)display=display.substring(0,display.length()-1);
        display+=ellipsis;
      }
      txt(c,display,x,y,size,color,false);
    }
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
      Canvas c=actual;c.drawColor(appearanceColor("background",Color.BLACK));
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
      txt(c,modules[active]+": "+StudioSettings.moduleName(MainActivity.this,modules[active],names[active]),25,222,18,Color.BLACK,true);
      p.setColor(Color.rgb(224,57,64));p.setStrokeWidth(1);c.drawLine(25,232,316,232,p);
      String configuredVersion=StudioSettings.moduleVersion(MainActivity.this,modules[active]);
      txt(c,"ID: "+ids[active]+"  |  Ver: "+(!configuredVersion.isEmpty()?configuredVersion:(loaded[active]?"ABT загружен":"Образец")),25,252,12,Color.rgb(91,103,119),false);
      card(c,10,279,380,91,12,true);
      p.setColor(Color.rgb(207,213,221));p.setStrokeWidth(1);c.drawLine(203,284,203,365,p);c.drawLine(15,325,203,325,p);
      ArrayList<FeatureEngine.Feature> shown=moduleFeatures(active);
      int page=featurePage[active],slots=featureSlots(),columns=featureColumns();
      if(shown.size()>slots){rect(c,Color.rgb(250,235,236),286,343,96,24,6);txt(c,(page+1)+"–"+Math.min(page+slots,shown.size())+"/"+shown.size()+"  ›",294,359,11,Color.rgb(150,0,0),true);}
      for(int i=0;i<slots&&page+i<shown.size();i++){
        float x=featureX(i),y=featureY(i);
        card(c,x,y,22,22,5,false);
        if(isChecked(shown.get(page+i))){
          rect(c,Color.rgb(210,28,37),x+4,y+4,14,14,3);
          p.setColor(Color.WHITE);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);
          Path mark=new Path();mark.moveTo(x+4,y+11);mark.lineTo(x+8,y+15);mark.lineTo(x+17,y+5);c.drawPath(mark,p);p.setStyle(Paint.Style.FILL);
        }
        float available=columns==3?(i==2?135:140):(340f/columns-27);
        txtFit(c,shown.get(page+i).label,x+26,y+15,columns==4?9:11,Color.BLACK,available);
      }
      card(c,10,376,380,337,13,true);
      int hexPanelColor=appearanceColor("panel",Color.TRANSPARENT);
      if(hexPanelColor!=Color.TRANSPARENT){p.setColor(hexPanelColor);p.setStyle(Paint.Style.FILL);c.drawRoundRect(13,379,387,710,10,10,p);}
      int count=Math.min(Math.max(0,rows[active].size()-scrollOffset[active]),9);
      if(rows[active].isEmpty())txt(c,"Нет данных — откройте ABT блока "+modules[active],22,420,12,Color.DKGRAY,false);
      for(int i=0;i<count;i++){
        int rowIndex=i+scrollOffset[active];float y=380+i*36.5f;card(c,15,y,370,35,8,false);
        p.setColor(Color.rgb(202,209,219));p.setStrokeWidth(1);c.drawLine(165,y+4,165,y+31,p);
        String index=rowIndex<abtRows[active].size()?abtRows[active].get(rowIndex).address:ids[active]+"-"+String.format(java.util.Locale.US,"01-%02d",rowIndex+1);
        txt(c,index,28,y+23,14,Color.BLACK,false);
        String value=rows[active].get(rowIndex);
        AbtCodec.Row row=rowIndex<abtRows[active].size()?abtRows[active].get(rowIndex):null;
        String before=row==null?"":original.get(key(row));
        // Only checked (enabled) features may highlight HEX. An unchecked feature must never show blue OFF values.
        Set<Integer> changed=row==null?new HashSet<>():activeFeaturePositions(active,row);
        // When an enabled feature changes the checksum, highlight its changed checksum digits as well.
        if(!changed.isEmpty()&&before!=null){
          String oldHex=AbtCodec.norm(before),currentHex=AbtCodec.norm(value);
          int checksumStart=Math.max(0,currentHex.length()-2);
          for(int k=checksumStart;k<currentHex.length()&&k<oldHex.length();k++)
            if(currentHex.charAt(k)!=oldHex.charAt(k))changed.add(k);
        }
        p.setTypeface(Typeface.create("monospace",Typeface.NORMAL));p.setTextSize(13);p.setStyle(Paint.Style.FILL);
        float px=177;int hexIndex=0;
        c.save();c.clipRect(175,y+2,381,y+33);
        for(int j=0;j<value.length();j++){
          char ch=value.charAt(j);boolean marked=ch!=' '&&changed.contains(hexIndex);
          p.setColor(marked?Color.rgb(20,105,220):Color.BLACK);
          p.setTypeface(Typeface.create("monospace",marked?Typeface.BOLD:Typeface.NORMAL));
          if(px>381)break;
          c.drawText(String.valueOf(ch),px,y+23,p);px+=p.measureText(String.valueOf(ch));if(ch!=' ')hexIndex++;
        }
        c.restore();
      }
      card(c,10,720,185,52,12,true);centered(c,StudioSettings.appearance(MainActivity.this,"open_text","Открыть ABT"),10,720,185,52,15,Color.BLACK);
      card(c,205,720,185,52,12,true);centered(c,StudioSettings.appearance(MainActivity.this,"save_text","Сохранить ABT"),205,720,185,52,15,Color.BLACK);
      card(c,10,782,319,57,12,true);
      String status="Изменений нет";
      int differences=0;
      for(AbtCodec.Row row:abtRows[active]){
        String old=original.get(key(row));String now=AbtCodec.norm(row.value);
        if(old!=null&&!old.equals(now)){differences++;if(differences==1)status=row.address+": "+old+" → "+now;}
      }
      if(differences>1)status="Изменено строк: "+differences;
      p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));p.setTextSize(10);p.setColor(Color.BLACK);
      if(status.length()>44)status=status.substring(0,41)+"…";
      c.drawText(status,22,814,p);
      card(c,336,782,54,57,12,true);
      txt(c,"♙",350,821,32,Color.BLACK,true);
      actual.restore();
    }
    float startY;
    @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN){startY=e.getY()/sy;return true;}if(e.getAction()!=MotionEvent.ACTION_UP)return true;float x=e.getX()/sx,y=e.getY()/sy;
      if(startY>=376&&startY<=713&&y>=376&&y<=713&&Math.abs(y-startY)>18){int delta=Math.round((startY-y)/36.5f);scrollOffset[active]=Math.max(0,Math.min(Math.max(0,rowCount(active)-9),scrollOffset[active]+delta));invalidate();return true;}
      if(y>=127&&y<=183){if(x<12||x>=392)return true;int selected=(int)((x-12)/95);if(selected>=0&&selected<modules.length){active=selected;invalidate();}return true;}
      if(x>=346&&y>=8&&y<=70){admin();return true;}
      if(y>=277&&y<=367){int slots=featureSlots();if(y>=343&&x>=286&&moduleFeatures(active).size()>slots){featurePage[active]=(featurePage[active]+slots>=moduleFeatures(active).size())?0:featurePage[active]+slots;invalidate();return true;}for(int i=0;i<slots;i++){float fx=featureX(i),fy=featureY(i);float width=featureColumns()==3?(i==2?175:180):(360f/featureColumns());if(x>=fx&&x<fx+width&&y>=fy-2&&y<fy+25){if(featurePage[active]+i<moduleFeatures(active).size())toggleFeature(featurePage[active]+i);return true;}}return true;}
      // HEX rows on the main screen are read-only; edit via features or admin panel.
      if(y>=380&&y<=713&&Math.abs(y-startY)<=18)return true;
      if(y>=717&&y<=777){if(x<200)open();else save();return true;}
      if(y>=779){if(x>=336)about();return true;}
      return true;
    }
  }
  final FeatureEngine[] engines={new FeatureEngine(),new FeatureEngine(),new FeatureEngine(),new FeatureEngine()};
  void resetAllEngines(){for(FeatureEngine e:engines)e.reset();checkedFeatures.clear();for(int m=0;m<4;m++)if(!abtRows[m].isEmpty())syncFeatureChecks(m);}
  void resetCurrentEngine(){resetEngineForModule(active);}
  void resetEngineForModule(int module){engines[module].reset();for(FeatureEngine.Feature f:moduleFeatures(module))checkedFeatures.remove(featureKey(f));}
  final ArrayList<AbtCodec.Row>[] abtRows=new ArrayList[]{new ArrayList<>(),new ArrayList<>(),new ArrayList<>(),new ArrayList<>()};
  final HashMap<String,String> original=new HashMap<>();
  final AbtSnapshot[] importedSnapshots=new AbtSnapshot[4];
  final int[] scrollOffset={0,0,0,0};
  final int[] featurePage={0,0,0,0};
  final HashMap<String,Boolean> checkedFeatures=new HashMap<>();
  String featureKey(FeatureEngine.Feature f){return f.module+"|"+f.address+"|"+f.id;}
  boolean isChecked(FeatureEngine.Feature f){Boolean b=checkedFeatures.get(featureKey(f));return b!=null&&b;}
  void syncFeatureChecks(int module){for(FeatureEngine.Feature f:moduleFeatures(module)){boolean on=engines[module].state(f,findRowValueFor(module,f.address));checkedFeatures.put(featureKey(f),on);engines[module].seed(f,on);}}
  Set<Integer> activeFeaturePositions(int module,AbtCodec.Row row){
    Set<Integer> positions=new HashSet<>();
    String raw=AbtCodec.norm(row.value);
    int payloadLength=Math.max(0,raw.length()-2); // Last byte is the checksum.
    for(FeatureEngine.Feature f:moduleFeatures(module)){
      if(!f.address.equalsIgnoreCase(row.address)||!isChecked(f))continue;
      try{
        if("BITS".equalsIgnoreCase(f.mode)){
          int offset=f.byteIndex*2;
          if(offset>=0&&offset+1<payloadLength){positions.add(offset);positions.add(offset+1);}
        }else{
          for(int index:FeatureEngine.indices(f.indices))
            if(index>=0&&index<payloadLength)positions.add(index);
        }
      }catch(RuntimeException ignored){/* Ignore invalid admin configuration while drawing. */}
    }
    return positions;
  }
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
    if(row==null){Toast.makeText(this,"Строка "+f.address+" отсутствует в блоке "+modules[active]+". Добавьте её через админку или загрузите ABT.",Toast.LENGTH_LONG).show();return;}
    try{boolean next=!isChecked(f);engines[active].apply(row,f,next,features);checkedFeatures.put(featureKey(f),next);refreshRows(active);view.invalidate();}
    catch(Exception ex){Toast.makeText(this,"Ошибка функции: "+ex.getMessage(),Toast.LENGTH_LONG).show();}
  }
  void inspectHexRow(int index){
    if(index<0||index>=abtRows[active].size())return;
    final int inspectedModule=active;
    AbtCodec.Row row=abtRows[inspectedModule].get(index);
    boolean checksumMatches=AbtCodec.checksumValid(row.address,row.value);
    String checksumInfo=checksumMatches?"совпадает":"не совпадает. Это диагностическая проверка редактора, а не доказательство ошибки исходного ABT";
    new AlertDialog.Builder(this).setTitle(modules[inspectedModule]+" · "+row.address)
      .setMessage("Полное значение HEX:\n"+row.value+"\n\nChecksum (алгоритм редактора): "+checksumInfo+"\n\nДля изменения выберите «Редактировать».")
      .setNegativeButton("Закрыть",null)
      .setPositiveButton("Редактировать",(d,w)->{
        if(active!=inspectedModule||index>=abtRows[inspectedModule].size()||abtRows[inspectedModule].get(index)!=row){
          Toast.makeText(this,"Строка изменилась. Откройте её повторно.",Toast.LENGTH_LONG).show();return;
        }
        editHexRow(index);
      }).show();
  }
  void editHexRow(int index){
    if(index<0||index>=abtRows[active].size())return;
    final int editingModule=active;
    final AbtCodec.Row row=abtRows[editingModule].get(index);
    final String startingValue=row.value;
    final EditText edit=new EditText(this);edit.setSingleLine(false);edit.setMinLines(2);
    edit.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
    edit.setTypeface(Typeface.MONOSPACE);edit.setText(row.value);edit.selectAll();
    LinearLayout container=new LinearLayout(this);container.setOrientation(LinearLayout.VERTICAL);container.setPadding(dp(16),dp(8),dp(16),0);
    TextView hint=new TextView(this);hint.setText("Введите HEX-байты. Последний байт — контрольная сумма, она пересчитывается автоматически.");
    container.addView(hint);container.addView(edit);
    AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Редактор "+row.address).setView(container)
      .setNegativeButton("Отмена",null)
      .setPositiveButton("Сохранить",null).create();
    dialog.setOnShowListener(ignored->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button->{
        try{
          if(active!=editingModule||!abtRows[editingModule].contains(row)||!row.value.equals(startingValue)){
            edit.setError("Строка или блок изменились. Откройте редактор повторно.");return;
          }
          String input=edit.getText().toString().replaceAll("\\s+","");
          if(input.isEmpty()||input.length()%2!=0||!input.matches("[0-9A-Fa-f]+"))throw new IllegalArgumentException("Допустимы только полные HEX-байты");
          if(input.length()!=AbtCodec.norm(row.value).length())throw new IllegalArgumentException("Длина строки должна остаться прежней");
          if(input.length()<=2)throw new IllegalArgumentException("В строке нет байтов данных для редактирования");
          String originalHex=AbtCodec.norm(row.value);
          if(input.substring(0,input.length()-2).equalsIgnoreCase(originalHex.substring(0,originalHex.length()-2))){
            Toast.makeText(this,"Байты данных не изменились; checksum редактировать не нужно",Toast.LENGTH_LONG).show();return;
          }
          String updated=AbtCodec.recalc(row.address,input);
          new AlertDialog.Builder(this).setTitle("Подтвердите изменение "+row.address)
            .setMessage("Было: "+row.value+"\nСтанет: "+updated+"\n\nКонтрольная сумма будет пересчитана.")
            .setNegativeButton("Отмена",null)
            .setPositiveButton("Применить",(confirm,choice)->{
              if(active!=editingModule||!abtRows[editingModule].contains(row)||!row.value.equals(startingValue)){
                Toast.makeText(this,"Данные изменились. Откройте строку повторно.",Toast.LENGTH_LONG).show();return;
              }
              row.value=updated;dialog.dismiss();resetCurrentEngine();syncFeatureChecks(editingModule);
              refreshRows(editingModule);view.invalidate();
              Toast.makeText(this,"Строка сохранена, checksum пересчитан",Toast.LENGTH_SHORT).show();
            }).show();
        }catch(Exception ex){edit.setError(ex.getMessage());edit.requestFocus();}
      }));
    dialog.show();
  }
  void refreshRows(int module){rows[module].clear();for(AbtCodec.Row row:abtRows[module])rows[module].add(row.value);}
  void loadRows(String content){
    HashMap<String,String> byPrefix=new HashMap<>();for(int i=0;i<4;i++)byPrefix.put(ids[i],modules[i]);
    List<AbtCodec.Row> parsed=AbtCodec.parseChecked(content,byPrefix);
    if(parsed.isEmpty())throw new IllegalArgumentException("Формат ABT не распознан");
    ArrayList<AbtCodec.Row> selected=AbtCodec.requireModule(parsed,modules[active]);
    HashSet<String> addresses=new HashSet<>();
    for(AbtCodec.Row row:selected){
      if(!addresses.add(row.address))throw new IllegalArgumentException("Повтор адреса "+row.address+" в ABT");
      String hex=AbtCodec.norm(row.value);
      if(hex.length()<4||hex.length()%2!=0)throw new IllegalArgumentException("Неверная длина строки "+row.address);
    }
    for(AbtCodec.Row old:abtRows[active])original.remove(key(old));
    importedSnapshots[active]=new AbtSnapshot(selected);
    abtRows[active].clear();abtRows[active].addAll(selected);scrollOffset[active]=0;
    for(AbtCodec.Row row:selected)original.put(key(row),AbtCodec.norm(row.value));
    resetCurrentEngine();
    featurePage[active]=0;syncFeatureChecks(active);
    refreshRows(active);loaded[active]=true;view.invalidate();
  }
  String findRowValue(String address){AbtCodec.Row row=findRow(active,address);return row==null?"":row.value;}
  boolean[] loaded=new boolean[4];
  int pendingModule=-1;
  void open(){pendingModule=active;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,10);}
  void save(){
    if(abtRows[active].isEmpty()){Toast.makeText(this,"В блоке "+modules[active]+" пока нет строк. Добавьте их через админку или откройте ABT.",Toast.LENGTH_LONG).show();return;}
    int suspect=0;
    for(AbtCodec.Row row:abtRows[active])if(!AbtCodec.checksumValid(row.address,row.value))suspect++;
    if(suspect>0){
      final int count=suspect;
      new AlertDialog.Builder(this).setTitle("Внимание: контрольные суммы")
        .setMessage("У "+count+" строк блока "+modules[active]+" контрольные суммы не совпадают. Исходные HEX и контрольные суммы будут сохранены без автоматического исправления. Продолжить сохранение?")
        .setNegativeButton("Отмена",null)
        .setPositiveButton("Продолжить",(d,w)->launchSavePicker()).show();
      return;
    }
    launchSavePicker();
  }
  void launchSavePicker(){
    pendingModule=active;
    Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);
    i.setType("application/octet-stream");i.putExtra(Intent.EXTRA_TITLE,modules[active]+".abt");
    startActivityForResult(i,11);
  }
  @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req!=10&&req!=11)return;if(result!=RESULT_OK||data==null||data.getData()==null){pendingModule=-1;return;}
    Uri uri=data.getData();
    int previous=active;
    if(pendingModule>=0&&pendingModule<modules.length)active=pendingModule;
    pendingModule=-1;
    try{
      if(req==10){
        ByteArrayOutputStream buf=new ByteArrayOutputStream();byte[] chunk=new byte[4096];int n;
        try(InputStream in=getContentResolver().openInputStream(uri)){
          if(in==null)throw new IOException("Файл недоступен");
          while((n=in.read(chunk))!=-1){if(buf.size()+n>4*1024*1024)throw new IOException("ABT слишком большой");buf.write(chunk,0,n);}
        }
        loadRows(new String(buf.toByteArray(),"UTF-8"));
        int suspect=0;for(AbtCodec.Row row:abtRows[active])if(!AbtCodec.checksumValid(row.address,row.value))suspect++;
        if(suspect>0)new AlertDialog.Builder(this).setTitle("Проверка ABT")
          .setMessage("Загружено строк: "+abtRows[active].size()+". Контрольная сумма не совпала у "+suspect+" строк. Исходные значения не изменены. Проверьте формат и файл перед записью в автомобиль.")
          .setPositiveButton("Понятно",null).show();
        else Toast.makeText(this,"Загружено строк: "+abtRows[active].size(),Toast.LENGTH_SHORT).show();
      }else if(req==11){
        if(abtRows[active].isEmpty())throw new IllegalArgumentException("Сначала откройте ABT");
        String text=AbtCodec.write(abtRows[active],modules[active]);
        if(text.isEmpty())throw new IOException("Нет строк для сохранения");
        try(OutputStream out=getContentResolver().openOutputStream(uri)){
          if(out==null)throw new IOException("Невозможно открыть файл для записи");
          out.write(text.getBytes("US-ASCII"));out.flush();
        }
        Toast.makeText(this,"ABT блока "+modules[active]+" сохранён",Toast.LENGTH_SHORT).show();
      }
    }catch(Exception ex){Toast.makeText(this,"Ошибка ABT: "+ex.getMessage(),Toast.LENGTH_LONG).show();}
    finally{active=previous;view.invalidate();}
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
    TextView title=caption(isAdmin?StudioSettings.appearance(this,"admin_text","Админка"):"О программе",20,Color.BLACK,true);
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
      enter.setOnClickListener(v->{if(login.getText().toString().equals("admin")&&password.getText().toString().equals("admin")){dialog.dismiss();showAdminTabs();}else Toast.makeText(this,"Неверный логин или пароль",Toast.LENGTH_SHORT).show();});
    }else{
      // Information-only layout without duplicated Mazda logo/banner.
      TextView version=caption(StudioSettings.appearance(this,"ready_text","Версия: тестовая сборка"),15,Color.DKGRAY,false);
      place(root,version,.09f,.245f,.82f,.11f,width,height);
      View divider=new View(this);divider.setBackgroundColor(Color.rgb(220,50,55));
      place(root,divider,.09f,.375f,.82f,.004f,width,height);
      TextView description=caption("Редактор As-Built для Mazda 6 GH",15,Color.BLACK,false);
      place(root,description,.09f,.42f,.84f,.12f,width,height);
      TextView author=caption(StudioSettings.appearance(this,"author_text","Разработчик: Dim304"),15,Color.BLACK,true);
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
  void restoreCurrentModule(){
    if(!loaded[active]){Toast.makeText(this,"Сначала откройте ABT блока "+modules[active],Toast.LENGTH_LONG).show();return;}
    final int module=active;
    final int[] diff=importedSnapshots[module].differences(abtRows[module]);
    new AlertDialog.Builder(this).setTitle("Восстановить исходный ABT?")
      .setMessage("Блок "+modules[module]+": изменено HEX — "+diff[0]+", добавлено строк — "+diff[1]+", удалено строк — "+diff[2]+". Восстановить исходный файл целиком? Остальные блоки не затрагиваются.")
      .setNegativeButton("Отмена",null)
      .setPositiveButton("Восстановить",(d,w)->{
        for(AbtCodec.Row row:abtRows[module])original.remove(key(row));
        abtRows[module].clear();
        for(AbtCodec.Row row:importedSnapshots[module].restore()){
          AbtCodec.Row copy=new AbtCodec.Row(row.module,row.address,row.value,row.block);
          abtRows[module].add(copy);
          original.put(key(copy),AbtCodec.norm(copy.value));
        }
        engines[module].reset();
        for(FeatureEngine.Feature f:moduleFeatures(module))checkedFeatures.remove(featureKey(f));
        scrollOffset[module]=0;featurePage[module]=0;
        syncFeatureChecks(module);refreshRows(module);view.invalidate();
        Toast.makeText(this,"Исходный ABT блока "+modules[module]+" восстановлен",Toast.LENGTH_SHORT).show();
      }).show();
  }
  void showModuleAdmin(){
    final String module=modules[active];
    LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(12),dp(18),0);
    TextView fixed=new TextView(this);fixed.setText("Блок: "+module+"  |  адрес: "+ids[active]+" (не изменяется)");root.addView(fixed);
    EditText name=new EditText(this);name.setSingleLine(true);name.setHint("Название блока");
    name.setText(StudioSettings.moduleName(this,module,names[active]));root.addView(name);
    EditText version=new EditText(this);version.setSingleLine(true);version.setHint("Версия / обозначение");
    version.setText(StudioSettings.moduleVersion(this,module));root.addView(version);
    new AlertDialog.Builder(this).setTitle("Админка · блок "+module).setView(root)
      .setNegativeButton("Отмена",null)
      .setPositiveButton("Сохранить",(d,w)->{
        String newName=name.getText().toString().trim();
        if(newName.isEmpty()){Toast.makeText(this,"Название не может быть пустым",Toast.LENGTH_LONG).show();return;}
        StudioSettings.saveModule(this,module,newName,version.getText().toString().trim());
        view.invalidate();Toast.makeText(this,"Настройки блока сохранены",Toast.LENGTH_SHORT).show();
      }).show();
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
        resetCurrentEngine();syncFeatureChecks(active);refreshRows(active);
        names.clear();for(AbtCodec.Row r:list)names.add(r.address);adapter.notifyDataSetChanged();view.invalidate();
        Toast.makeText(this,"Строка обновлена; сохраните ABT в файл",Toast.LENGTH_SHORT).show();
      }catch(Exception ex){Toast.makeText(this,"Ошибка: "+ex.getMessage(),Toast.LENGTH_LONG).show();}
    });
    remove.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Удалить строку?")
      .setMessage("Удаление будет записано только при сохранении ABT.")
      .setNegativeButton("Отмена",null).setPositiveButton("Удалить",(d,w)->{
        int n=selector.getSelectedItemPosition();if(n<0||n>=list.size())return;
        list.remove(n);resetCurrentEngine();syncFeatureChecks(active);refreshRows(active);
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
    for(FeatureEngine.Feature f:features)labels.add(f.label+" · "+f.module+" · "+f.address);
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
        StudioSettings.save(this,features);resetAllEngines();
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
      features.remove(n);StudioSettings.save(this,features);resetAllEngines();
      labels.clear();for(FeatureEngine.Feature f:features)labels.add(f.label+" · "+f.module+" · "+f.address);adapter.notifyDataSetChanged();view.invalidate();
    });
    root.addView(actions);
    Button rowsAdmin=new Button(this);rowsAdmin.setText("Редактор строк ABT");root.addView(rowsAdmin);rowsAdmin.setOnClickListener(v->showRowsAdmin());
    Button restore=new Button(this);restore.setText("Восстановить исходный ABT");root.addView(restore);restore.setOnClickListener(v->restoreCurrentModule());
    Button moduleAdmin=new Button(this);moduleAdmin.setText("Настройки блока");root.addView(moduleAdmin);moduleAdmin.setOnClickListener(v->showModuleAdmin());
    Button templates=new Button(this);templates.setText("Добавить шаблоны функций");root.addView(templates);
    templates.setOnClickListener(v->new AlertDialog.Builder(this)
      .setTitle("Шаблоны из Run #89")
      .setMessage("Добавить отсутствующие функции IC, BCM, RKE, ABS? Существующие настройки сохранятся. Кодировки требуют проверки на автомобиле.")
      .setNegativeButton("Отмена",null)
      .setPositiveButton("Добавить",(dialog,which)->{
        int added=0;
        for(FeatureEngine.Feature item:StudioSettings.defaults()){
          boolean exists=false;
          for(FeatureEngine.Feature current:features)
            if(current.module.equals(item.module)&&current.id.equals(item.id)){exists=true;break;}
          if(!exists){features.add(item);added++;}
        }
        StudioSettings.save(this,features);resetAllEngines();
        labels.clear();for(FeatureEngine.Feature item:features)labels.add(item.id+" · "+item.module+" · "+item.address);
        adapter.notifyDataSetChanged();view.invalidate();
        Toast.makeText(this,"Добавлено шаблонов: "+added,Toast.LENGTH_LONG).show();
      }).show());
    new AlertDialog.Builder(this).setTitle("Админка · функции и биты").setView(scroll).setPositiveButton("Закрыть",null).show();
  }
  int adminRowsModule=0;
  void showAdminTabs(){
    final Dialog dialog=new Dialog(this);
    final Runnable[] commitCurrent={null};
    LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(8),dp(8),dp(8),dp(8));root.setBackgroundColor(Color.rgb(44,44,44));
    TextView title=new TextView(this);title.setText("Администрирование интерфейса");
    title.setTextSize(18);title.setTextColor(Color.WHITE);title.setPadding(dp(8),dp(8),0,dp(14));root.addView(title);
    View line=new View(this);line.setBackgroundColor(Color.rgb(40,170,210));
    root.addView(line,new LinearLayout.LayoutParams(-1,dp(2)));
    android.widget.HorizontalScrollView horizontal=new android.widget.HorizontalScrollView(this);
    horizontal.setHorizontalScrollBarEnabled(true);
    LinearLayout tabs=new LinearLayout(this);tabs.setOrientation(LinearLayout.HORIZONTAL);
    horizontal.addView(tabs);root.addView(horizontal);
    FrameLayout body=new FrameLayout(this);
    LinearLayout.LayoutParams bodyParams=new LinearLayout.LayoutParams(-1,0,1);
    root.addView(body,bodyParams);
    final String[] names={"Функции и биты","Строки As-Built","Блоки и доступ","Оформление"};
    final Button[] buttons=new Button[4];
    final View[] underlines=new View[4];
    final int[] selected={0};
    final Runnable[] redrawRef=new Runnable[1];
    Runnable redraw=()->{
      commitCurrent[0]=null;
      body.removeAllViews();
      LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);
      panel.setPadding(dp(3),dp(5),dp(3),dp(5));
      body.addView(panel,new FrameLayout.LayoutParams(-1,-1));
      TextView heading=new TextView(this);heading.setText("Выберите запись");
      heading.setTextSize(20);heading.setTextColor(Color.WHITE);heading.setGravity(Gravity.CENTER);
      heading.setBackgroundColor(Color.rgb(84,84,84));heading.setPadding(0,dp(15),0,dp(15));
      panel.addView(heading,new LinearLayout.LayoutParams(-1,-2));
      if(selected[0]==0){
        final String[] labels={"ID функции","Модуль","Надпись / функция","Строка","Режим HEX/BITS","HEX индексы","Byte (BITS)","Биты","ВКЛ","ВЫКЛ"};
        final EditText[] values=new EditText[labels.length];
        final LinearLayout[] fieldRows=new LinearLayout[labels.length];
        final int[] chosen={-1};
        final int[] selectedFeature={-1};
        Button featureSelector=new Button(this);
        featureSelector.setAllCaps(false);featureSelector.setTextSize(13);
        featureSelector.setText("Выберите функцию…");
        // The heading is the single feature selector; avoid a duplicate button.
        Runnable refreshFeatureChoices=()->{
          int n=chosen[0];
          heading.setText(n>=0&&n<features.size()?(n+1)+" · "+features.get(n).label:"Выберите запись");
        };
        heading.setOnClickListener(v->{
          String[] options=new String[features.size()];
          for(int n=0;n<features.size();n++)options[n]=(n+1)+" · "+features.get(n).label;
          new AlertDialog.Builder(this).setTitle("Выберите функцию")
            .setSingleChoiceItems(options,chosen[0],(choiceDialog,position)->{
              chosen[0]=position;
              FeatureEngine.Feature f=features.get(position);
              heading.setText((position+1)+" · "+f.label);
              String[] info={f.id,f.module,f.label,f.address,f.mode,
                "HEX".equalsIgnoreCase(f.mode)?f.indices:"",
                String.valueOf(f.byteIndex),"BITS".equalsIgnoreCase(f.mode)?f.indices:"",f.on,f.off};
              for(int j=0;j<values.length;j++)values[j].setText(info[j]);
              refreshFeatureChoices.run();choiceDialog.dismiss();
            }).setNegativeButton("Закрыть",null).show();
        });
        android.widget.ScrollView featureScroll=new android.widget.ScrollView(this);
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);
        featureScroll.addView(content);panel.addView(featureScroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout fields=new LinearLayout(this);fields.setOrientation(LinearLayout.VERTICAL);
        for(int j=0;j<labels.length;j++){
          LinearLayout lineRow=new LinearLayout(this);lineRow.setOrientation(LinearLayout.HORIZONTAL);
          TextView label=new TextView(this);label.setText(labels[j]);label.setTextSize(12);label.setTextColor(Color.WHITE);
          label.setGravity(Gravity.CENTER_VERTICAL);lineRow.addView(label,new LinearLayout.LayoutParams(0,dp(42),1));
          EditText value=new EditText(this);value.setSingleLine(true);value.setTextColor(Color.BLACK);value.setTextSize(13);
          value.setGravity(Gravity.CENTER_VERTICAL);value.setPadding(dp(6),0,dp(5),0);
          value.setBackgroundColor(Color.WHITE);lineRow.addView(value,new LinearLayout.LayoutParams(0,dp(42),1));
          lineRow.setPadding(dp(4),dp(3),dp(4),dp(3));
          values[j]=value;fieldRows[j]=lineRow;fields.addView(lineRow);
        }
        Runnable updateModeFields=()->{
          boolean bits="BITS".equalsIgnoreCase(values[4].getText().toString().trim());
          fieldRows[5].setVisibility(bits?View.GONE:View.VISIBLE);
          fieldRows[6].setVisibility(bits?View.VISIBLE:View.GONE);
          fieldRows[7].setVisibility(bits?View.VISIBLE:View.GONE);
          fieldRows[8].setVisibility(bits?View.GONE:View.VISIBLE);
          fieldRows[9].setVisibility(bits?View.GONE:View.VISIBLE);
        };
        values[5].setHint("Например: 0,1 или 2-4");
        values[6].setHint("Номер байта с 0");
        values[7].setHint("Например: 0,2,7");
        values[8].setHint("HEX, например 8F");
        values[9].setHint("HEX, например 0F");
        values[1].setFocusable(false);
        values[1].setClickable(true);
        values[1].setHint("Выберите модуль");
        values[1].setOnClickListener(v->new AlertDialog.Builder(this)
          .setTitle("Модуль As-Built")
          .setItems(new String[]{"IC","BCM","RKE","ABS"},(d,which)->values[1].setText(new String[]{"IC","BCM","RKE","ABS"}[which]))
          .show());
        values[4].setFocusable(false);
        values[4].setClickable(true);
        values[4].setHint("Нажмите: HEX или BITS");
        values[4].setOnClickListener(v->new AlertDialog.Builder(this)
          .setTitle("Режим функции")
          .setItems(new String[]{"HEX — изменение символов","BITS — изменение битов"},(d,which)->values[4].setText(which==0?"HEX":"BITS"))
          .show());
        values[4].addTextChangedListener(new android.text.TextWatcher(){
          public void beforeTextChanged(CharSequence s,int start,int count,int after){}
          public void onTextChanged(CharSequence s,int start,int before,int count){updateModeFields.run();}
          public void afterTextChanged(android.text.Editable e){}
        });
        updateModeFields.run();
        content.addView(fields);
        TextView bitHelp=new TextView(this);
        bitHelp.setText("HEX: индексы символов и значения ВКЛ/ВЫКЛ. BITS: номер байта и номера битов 0–7; поля ВКЛ/ВЫКЛ не используются.");
        bitHelp.setTextColor(Color.rgb(190,218,228));bitHelp.setTextSize(12);
        bitHelp.setPadding(dp(6),dp(10),dp(6),dp(10));content.addView(bitHelp);
        LinearLayout actions=new LinearLayout(this);panel.addView(actions);
        final String[] actionNames={"Добавить","Изменить","Удалить"};
        final Button[] featureActions=new Button[3];
        for(int action=0;action<3;action++){
          final int kind=action;Button actionButton=new Button(this);featureActions[action]=actionButton;actionButton.setAllCaps(false);
          actionButton.setText(actionNames[action]);actionButton.setTextSize(12);
          actionButton.setMinWidth(0);actionButton.setPadding(dp(3),0,dp(3),0);
          actions.addView(actionButton,new LinearLayout.LayoutParams(0,dp(48),1));
          actionButton.setOnClickListener(v->{
            if(kind==2){
              if(chosen[0]<0||chosen[0]>=features.size()){Toast.makeText(this,"Выберите запись",Toast.LENGTH_SHORT).show();return;}
              final int index=chosen[0];
              new AlertDialog.Builder(this).setTitle("Удалить функцию?")
                .setMessage(features.get(index).id)
                .setNegativeButton("Отмена",null)
                .setPositiveButton("Удалить",(d,w)->{
                  features.remove(index);chosen[0]=-1;StudioSettings.save(this,features);resetAllEngines();view.invalidate();refreshFeatureChoices.run();redrawRef[0].run();
                }).show();return;
            }
            if(kind==1&&(chosen[0]<0||chosen[0]>=features.size())){
              Toast.makeText(this,"Выберите запись для изменения",Toast.LENGTH_SHORT).show();return;
            }
            try{
              String[] vls=new String[values.length];
              for(int j=0;j<values.length;j++)vls[j]=values[j].getText().toString().trim();
              String id=vls[0],module=vls[1].toUpperCase(java.util.Locale.ROOT),address=vls[3].toUpperCase(java.util.Locale.ROOT);
              String mode=vls[4].toUpperCase(java.util.Locale.ROOT);
              if(id.isEmpty()||address.isEmpty()||!(module.equals("IC")||module.equals("BCM")||module.equals("RKE")||module.equals("ABS"))||!(mode.equals("HEX")||mode.equals("BITS")))
                throw new IllegalArgumentException("Укажите ID, модуль, строку и режим HEX/BITS");
              if(!address.matches("[0-9A-F]{3}-[0-9]{2}-[0-9]{2}"))
                throw new IllegalArgumentException("Адрес строки должен иметь вид 720-01-01");
              try{AbtCodec.checksum(address,"0000");}
              catch(RuntimeException badAddress){throw new IllegalArgumentException("Неверный адрес строки As-Built");}
              String indices=mode.equals("BITS")?vls[7]:vls[5];
              int byteIndex=mode.equals("BITS")?Integer.parseInt(vls[6]):0;
              java.util.List<Integer> positions=FeatureEngine.indices(indices);
              if(byteIndex<0||positions.isEmpty()||new java.util.HashSet<>(positions).size()!=positions.size())
                throw new IllegalArgumentException("Проверьте индексы: повторы недопустимы");
              if(mode.equals("BITS")){
                for(int bit:positions)if(bit<0||bit>7)throw new IllegalArgumentException("Биты должны быть от 0 до 7");
                if(!vls[8].isEmpty()||!vls[9].isEmpty())
                  throw new IllegalArgumentException("В режиме BITS поля ВКЛ/ВЫКЛ оставьте пустыми: приложение устанавливает/снимает указанные биты");
              }else{
                for(int position:positions)if(position<0)throw new IllegalArgumentException("HEX индекс не может быть отрицательным");
                if(!vls[8].matches("(?i)[0-9a-f]+")||(!vls[9].isEmpty()&&!vls[9].matches("(?i)[0-9a-f]+")))
                  throw new IllegalArgumentException("ВКЛ и ВЫКЛ должны содержать только HEX-символы 0–9 и A–F");
                String on=AbtCodec.norm(vls[8]),off=AbtCodec.norm(vls[9]);
                if(!on.matches("[0-9A-F]+")||positions.size()!=on.length())
                  throw new IllegalArgumentException("Количество HEX индексов должно совпадать с длиной ВКЛ");
                if(!off.isEmpty()&&(!off.matches("[0-9A-F]+")||positions.size()!=off.length()))
                  throw new IllegalArgumentException("Количество HEX индексов должно совпадать с длиной ВЫКЛ");
              }
              int moduleIndex=java.util.Arrays.asList(modules).indexOf(module);
              if(moduleIndex>=0&&!abtRows[moduleIndex].isEmpty()){
                AbtCodec.Row matching=null;
                for(AbtCodec.Row candidate:abtRows[moduleIndex])if(candidate.address.equalsIgnoreCase(address)){matching=candidate;break;}
                if(matching==null)throw new IllegalArgumentException("Строка "+address+" отсутствует в загруженном ABT блока "+module);
                int hexLength=AbtCodec.norm(matching.value).length();
                int payloadLength=hexLength-2; // Последний байт — контрольная сумма FORScan.
                if(mode.equals("BITS")){
                  if(byteIndex*2+2>payloadLength)throw new IllegalArgumentException("Byte (BITS) выходит за данные или затрагивает checksum строки "+address);
                }else{
                  for(int position:positions)if(position>=payloadLength)throw new IllegalArgumentException("HEX индекс "+position+" выходит за данные или затрагивает checksum строки "+address);
                }
              }
              for(int j=0;j<features.size();j++)if((kind==0||j!=chosen[0])&&features.get(j).id.equals(id)&&features.get(j).module.equals(module)&&features.get(j).address.equals(address))
                throw new IllegalArgumentException("Функция с таким ID и адресом уже есть");
              FeatureEngine.Feature updated=new FeatureEngine.Feature(id,module,address,mode,indices,vls[8],vls[9],byteIndex,vls[2]);
              if(kind==0){features.add(updated);chosen[0]=features.size()-1;}else features.set(chosen[0],updated);
              StudioSettings.save(this,features);resetAllEngines();view.invalidate();refreshFeatureChoices.run();
              heading.setText((chosen[0]+1)+" · "+updated.label);
              // Keep the selected feature and all edited fields visible after saving.
              Toast.makeText(this,"Настройки функции сохранены",Toast.LENGTH_SHORT).show();
            }catch(Exception ex){Toast.makeText(this,"Ошибка: "+ex.getMessage(),Toast.LENGTH_LONG).show();}
          });
        }
        commitCurrent[0]=()->featureActions[chosen[0]>=0?1:0].performClick();
      }
      if(selected[0]==1){
        final int rowsModule=adminRowsModule;
        final ArrayList<AbtCodec.Row> rows=abtRows[rowsModule];
        final int[] chosen={-1};
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);
        panel.addView(form);
        final EditText[] inputs=new EditText[3];
        String[] labels={"Модуль","Строка","Значение"};
        for(int k=1;k<3;k++){
          LinearLayout field=new LinearLayout(this);field.setOrientation(LinearLayout.HORIZONTAL);
          TextView label=new TextView(this);label.setText(labels[k]);label.setTextColor(Color.WHITE);
          label.setGravity(Gravity.CENTER_VERTICAL);label.setTextSize(12);field.addView(label,new LinearLayout.LayoutParams(0,dp(44),1));
          EditText input=new EditText(this);input.setSingleLine(true);input.setTextSize(14);input.setTextColor(Color.BLACK);
          input.setBackgroundColor(Color.WHITE);input.setPadding(dp(7),0,dp(5),0);
          field.addView(input,new LinearLayout.LayoutParams(0,dp(44),1));field.setPadding(dp(4),dp(3),dp(4),dp(3));form.addView(field);inputs[k]=input;
        }
        heading.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Выберите блок")
          .setItems(modules,(d,which)->{adminRowsModule=which;redrawRef[0].run();}).show());
        android.widget.ScrollView listScroll=new android.widget.ScrollView(this);
        LinearLayout entries=new LinearLayout(this);entries.setOrientation(LinearLayout.VERTICAL);
        listScroll.addView(entries);panel.addView(listScroll,new LinearLayout.LayoutParams(-1,0,1));
        {
          for(int n=0;n<rows.size();n++){
            final int index=n;AbtCodec.Row row=rows.get(n);
            Button entry=new Button(this);entry.setAllCaps(false);entry.setText((n+1)+" · "+row.address);
            entries.addView(entry,new LinearLayout.LayoutParams(-1,dp(48)));
            entry.setOnClickListener(v->{chosen[0]=index;
              inputs[1].setText(row.address);inputs[2].setText(row.value);});
          }
        }
        LinearLayout actions=new LinearLayout(this);panel.addView(actions);
        String[] labelsActions={"Добавить","Изменить","Удалить"};
        final Button[] rowActions=new Button[3];
        for(int action=0;action<3;action++){
          final int kind=action;Button button=new Button(this);rowActions[action]=button;button.setText(labelsActions[action]);button.setAllCaps(false);
          button.setTextSize(12);button.setMinWidth(0);button.setPadding(dp(3),0,dp(3),0);actions.addView(button,new LinearLayout.LayoutParams(0,dp(48),1));
          button.setOnClickListener(v->{
            if(kind==2){
              if(chosen[0]<0||chosen[0]>=rows.size()){Toast.makeText(this,"Выберите строку",Toast.LENGTH_SHORT).show();return;}
              final int index=chosen[0];
              new AlertDialog.Builder(this).setTitle("Удалить строку "+rows.get(index).address+"?")
                .setNegativeButton("Отмена",null).setPositiveButton("Удалить",(d,w)->{
                  AbtCodec.Row removed=rows.remove(index);original.remove(key(removed));saveAdminRowChange(rowsModule,removed.address,true);resetEngineForModule(rowsModule);syncFeatureChecks(rowsModule);refreshRows(rowsModule);view.invalidate();redrawRef[0].run();
                }).show();return;
            }
            try{
              String addr=inputs[1].getText().toString().trim().toUpperCase(Locale.US);
              String hex=inputs[2].getText().toString().replaceAll("\\s+","").toUpperCase(Locale.US);
              if(!addr.matches(ids[rowsModule]+"-[0-9]{2}-[0-9]{2}"))throw new IllegalArgumentException("Адрес не принадлежит текущему блоку");
              if(hex.length()<4||hex.length()%2!=0||!hex.matches("[0-9A-F]+"))throw new IllegalArgumentException("Неверное значение HEX");
              String updated=AbtCodec.recalc(addr,hex);
              if(kind==0){
                for(AbtCodec.Row existing:rows)if(existing.address.equals(addr))throw new IllegalArgumentException("Строка уже существует");
                rows.add(new AbtCodec.Row(modules[rowsModule],addr,updated,Integer.parseInt(addr.split("-")[1])));
              }else{
                if(chosen[0]<0||chosen[0]>=rows.size())throw new IllegalArgumentException("Выберите строку");
                AbtCodec.Row existing=rows.get(chosen[0]);
                if(!existing.address.equals(addr))throw new IllegalArgumentException("Адрес менять нельзя — добавьте новую строку");
                if(AbtCodec.norm(existing.value).length()!=hex.length())throw new IllegalArgumentException("Длина HEX должна остаться прежней");
                existing.value=updated;
              }
              for(AbtCodec.Row current:rows)original.put(key(current),AbtCodec.norm(current.value));
              saveAdminRowChange(rowsModule,addr,false);resetEngineForModule(rowsModule);syncFeatureChecks(rowsModule);refreshRows(rowsModule);view.invalidate();redrawRef[0].run();
              Toast.makeText(this,"Строки сохранены в приложении",Toast.LENGTH_LONG).show();
            }catch(Exception ex){Toast.makeText(this,"Ошибка: "+ex.getMessage(),Toast.LENGTH_LONG).show();}
          });
        }
        commitCurrent[0]=()->rowActions[chosen[0]>=0?1:0].performClick();
      }
      if(selected[0]==2){
        final int[] moduleSelection={active};
        LinearLayout fields=new LinearLayout(this);fields.setOrientation(LinearLayout.VERTICAL);
        panel.addView(fields);
        final EditText[] inputs=new EditText[4];
        String[] labels={"ID","Название блока","Адрес","Версия"};
        for(int k=0;k<4;k++){
          LinearLayout field=new LinearLayout(this);field.setOrientation(LinearLayout.HORIZONTAL);
          TextView label=new TextView(this);label.setText(labels[k]);label.setTextColor(Color.WHITE);
          label.setGravity(Gravity.CENTER_VERTICAL);label.setTextSize(12);field.addView(label,new LinearLayout.LayoutParams(0,dp(44),1));
          EditText input=new EditText(this);input.setSingleLine(true);input.setTextColor(Color.BLACK);
          input.setTextSize(14);input.setPadding(dp(8),0,dp(5),0);input.setBackgroundColor(Color.WHITE);
          field.addView(input,new LinearLayout.LayoutParams(0,dp(44),1));field.setPadding(dp(4),dp(3),dp(4),dp(3));fields.addView(field);inputs[k]=input;
        }
        inputs[0].setEnabled(false);inputs[2].setEnabled(false);
        Runnable fill=()->{
          int m=moduleSelection[0];inputs[0].setText(modules[m]);
          inputs[1].setText(StudioSettings.moduleName(this,modules[m],names[m]));
          inputs[2].setText(ids[m]);inputs[3].setText(StudioSettings.moduleVersion(this,modules[m]));
          heading.setText("Блок "+modules[m]);
        };
        fill.run();
        LinearLayout choices=new LinearLayout(this);choices.setOrientation(LinearLayout.VERTICAL);
        panel.addView(choices,new LinearLayout.LayoutParams(-1,0,1));
        for(int m=0;m<modules.length;m++){
          final int index=m;Button choose=new Button(this);choose.setAllCaps(false);
          choose.setText((m+1)+" · "+modules[m]+" · "+names[m]);
          choose.setTextSize(12);choose.setMinWidth(0);choose.setPadding(dp(5),0,dp(5),0);
          choices.addView(choose,new LinearLayout.LayoutParams(-1,dp(42)));
          choose.setOnClickListener(v->{moduleSelection[0]=index;fill.run();});
        }
        TextView warning=new TextView(this);
        warning.setText("ID и адрес блока защищены: они используются для распознавания файлов ABT.");
        warning.setTextColor(Color.LTGRAY);warning.setTextSize(12);panel.addView(warning);
        Button saveModule=new Button(this);saveModule.setAllCaps(false);saveModule.setText("Сохранить настройки блока");
        saveModule.setTextSize(12);saveModule.setMinWidth(0);saveModule.setPadding(dp(4),0,dp(4),0);
        panel.addView(saveModule,new LinearLayout.LayoutParams(-1,dp(46)));
        saveModule.setOnClickListener(v->{
          String newName=inputs[1].getText().toString().trim();
          if(newName.isEmpty()){inputs[1].setError("Название не может быть пустым");return;}
          int m=moduleSelection[0];
          StudioSettings.saveModule(this,modules[m],newName,inputs[3].getText().toString().trim());
          view.invalidate();Toast.makeText(this,"Блок "+modules[m]+" сохранён",Toast.LENGTH_SHORT).show();
        });
      }
      if(selected[0]==3){
        final String[] keys={"title","background","panel","feature_columns","open_text","save_text","admin_text","author_text","ready_text"};
        final String[] labels={"Название окна","Фон приложения","Фон HEX-блока","Колонки функций","Кнопка открытия","Кнопка сохранения","Кнопка Admin","Кто сделал приложение","Текст статуса"};
        final String[] defaults={"Mazda 6 GH As-Built Studio","","","3","Открыть ABT","Сохранить ABT","Админка","Кто сделал приложение","Готово"};
        android.widget.ScrollView appearanceScroll=new android.widget.ScrollView(this);
        LinearLayout appearanceFields=new LinearLayout(this);appearanceFields.setOrientation(LinearLayout.VERTICAL);
        appearanceScroll.addView(appearanceFields);panel.addView(appearanceScroll,new LinearLayout.LayoutParams(-1,0,1));
        final EditText[] appearanceInputs=new EditText[keys.length];
        for(int k=0;k<keys.length;k++){
          TextView label=new TextView(this);label.setText(labels[k]);label.setTextColor(Color.WHITE);label.setTextSize(13);
          label.setPadding(dp(5),dp(7),dp(5),dp(3));appearanceFields.addView(label);
          EditText input=new EditText(this);input.setSingleLine(true);input.setTextSize(14);
          input.setText(StudioSettings.appearance(this,keys[k],defaults[k]));
          input.setBackgroundColor(Color.WHITE);input.setTextColor(Color.BLACK);input.setPadding(dp(8),0,dp(6),0);
          appearanceFields.addView(input,new LinearLayout.LayoutParams(-1,dp(42)));appearanceInputs[k]=input;
        }
        TextView note=new TextView(this);note.setText("Цвета: #RRGGBB, пустое поле — стандартный цвет. Количество колонок функций: 1–4. Название, кнопки, админка, автор и статус применяются после сохранения.");
        note.setTextColor(Color.LTGRAY);appearanceFields.addView(note);
        Button saveAppearance=new Button(this);saveAppearance.setText("Сохранить оформление");saveAppearance.setAllCaps(false);
        saveAppearance.setTextSize(12);saveAppearance.setMinWidth(0);saveAppearance.setPadding(dp(4),0,dp(4),0);
        panel.addView(saveAppearance,new LinearLayout.LayoutParams(-1,dp(48)));
        saveAppearance.setOnClickListener(v->{
          try{
            java.util.HashMap<String,String> changes=new java.util.HashMap<>();
            for(int k=0;k<keys.length;k++)changes.put(keys[k],appearanceInputs[k].getText().toString());
            int columns=Integer.parseInt(changes.get("feature_columns").trim());
            if(columns<1||columns>4)throw new IllegalArgumentException("Колонки функций: от 1 до 4");
            StudioSettings.saveAppearance(this,changes);
            setTitle(StudioSettings.appearance(this,"title","Mazda 6 GH As-Built Studio"));
            view.invalidate();
            Toast.makeText(this,"Оформление сохранено",Toast.LENGTH_SHORT).show();
          }catch(Exception ex){Toast.makeText(this,"Ошибка оформления: "+ex.getMessage(),Toast.LENGTH_LONG).show();}
        });
      }
      for(int i=0;i<buttons.length;i++){buttons[i].setAlpha(i==selected[0]?1f:.72f);if(underlines[i]!=null)underlines[i].setVisibility(i==selected[0]?View.VISIBLE:View.INVISIBLE);}
    };
    redrawRef[0]=redraw;
    for(int i=0;i<names.length;i++){
      final int tab=i;Button b=new Button(this);b.setAllCaps(false);b.setText(names[i]);
      b.setTextSize(10);b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(dp(2),0,dp(2),0);
      LinearLayout tabBox=new LinearLayout(this);tabBox.setOrientation(LinearLayout.VERTICAL);
      tabBox.addView(b,new LinearLayout.LayoutParams(-1,dp(45)));
      View underline=new View(this);underline.setBackgroundColor(Color.rgb(40,170,210));underline.setVisibility(i==0?View.VISIBLE:View.INVISIBLE);
      tabBox.addView(underline,new LinearLayout.LayoutParams(-1,dp(3)));
      int tabWidth=Math.max(dp(72),(getResources().getDisplayMetrics().widthPixels-dp(24))/4);
      tabs.addView(tabBox,new LinearLayout.LayoutParams(tabWidth,dp(49)));
      buttons[i]=b;underlines[i]=underline;b.setOnClickListener(v->{selected[0]=tab;redrawRef[0].run();});
    }
    LinearLayout footer=new LinearLayout(this);
    Button save=new Button(this);save.setText("Сохранить изменения");save.setAllCaps(false);
    footer.addView(save,new LinearLayout.LayoutParams(0,dp(52),1));
    save.setOnClickListener(v->{if(commitCurrent[0]!=null)commitCurrent[0].run();else Toast.makeText(this,"Для сохранения используйте кнопку в выбранной вкладке",Toast.LENGTH_LONG).show();});
    Button close=new Button(this);close.setText("Закрыть");close.setAllCaps(false);
    footer.addView(close,new LinearLayout.LayoutParams(0,dp(52),1));close.setOnClickListener(v->dialog.dismiss());
    root.addView(footer);
    dialog.setContentView(root);
    android.view.Window window=dialog.getWindow();
    if(window!=null){window.setBackgroundDrawableResource(android.R.color.transparent);window.setLayout(-1,-1);}
    redrawRef[0].run();dialog.show();
    window=dialog.getWindow();if(window!=null)window.setLayout((int)(getResources().getDisplayMetrics().widthPixels*.97f),(int)(getResources().getDisplayMetrics().heightPixels*.87f));
  }
  void showAppearanceAdmin(){
    new AlertDialog.Builder(this).setTitle("Оформление")
      .setMessage("Раздел оформления из Run #89 переносится отдельно. Действующие настройки функций и ABT остаются без изменений.")
      .setPositiveButton("Назад к вкладкам",(d,w)->showAdminTabs()).show();
  }
  void admin(){modal(true);}
  void about(){modal(false);}
}
