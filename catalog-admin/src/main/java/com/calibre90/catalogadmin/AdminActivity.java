package com.calibre90.catalogadmin;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;

/**
 * Separate local administrator draft editor.
 * Does not publish remotely and cannot bypass server-side publishing authorization.
 */
public final class AdminActivity extends Activity {
  private final ArrayList<JSONObject> draft=new ArrayList<>();
  private LinearLayout list;
  private TextView status;
  private String activeModule="IC";
  private LinearLayout tabs;
  private static final int EXPORT_DRAFT=4001;

  @Override public void onCreate(Bundle saved){
    super.onCreate(saved);
    LinearLayout root=new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(24,24,24,24);
    TextView heading=new TextView(this);
    heading.setText("Mazda 6 GH Studio Admin — черновик каталога");
    heading.setTextSize(20);
    root.addView(heading);
    status=new TextView(this);
    root.addView(status);
    tabs=new LinearLayout(this);tabs.setOrientation(LinearLayout.HORIZONTAL);
    root.addView(tabs);
    Button add=new Button(this);add.setText("Добавить функцию");root.addView(add);
    add.setOnClickListener(v->edit(-1));
    ScrollView scroll=new ScrollView(this);
    list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);
    scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    Button preview=new Button(this);preview.setText("Показать JSON черновика");root.addView(preview);
    preview.setOnClickListener(v->showJson());
    Button export=new Button(this);export.setText("Экспорт JSON черновика");root.addView(export);
    export.setOnClickListener(v->exportDraft());
    setContentView(root);
    load();redraw();
  }

  private void load(){
    try{
      String stored=getSharedPreferences("catalog_admin_draft",0).getString("features","[]");
      JSONArray arr=new JSONArray(stored);
      for(int i=0;i<arr.length();i++)draft.add(arr.getJSONObject(i));
    }catch(Exception ignored){draft.clear();}
  }
  private void save(){
    JSONArray arr=new JSONArray();for(JSONObject f:draft)arr.put(f);
    if(!getSharedPreferences("catalog_admin_draft",0).edit().putString("features",arr.toString()).commit())
      Toast.makeText(this,"Ошибка сохранения",Toast.LENGTH_LONG).show();
  }
  private void redraw(){
    tabs.removeAllViews();
    for(String module:new String[]{"IC","BCM","RKE","ABS"}){
      Button tab=new Button(this);
      tab.setAllCaps(false);tab.setText(module);tab.setTextSize(11);
      tab.setEnabled(!module.equals(activeModule));
      tab.setOnClickListener(v->{activeModule=module;redraw();});
      tabs.addView(tab,new LinearLayout.LayoutParams(0,-2,1));
    }
    list.removeAllViews();
    status.setText("Блок "+activeModule+" | Всего функций: "+draft.size()+"\nПубликация в интернет пока отключена.");
    for(int i=0;i<draft.size();i++){
      final int index=i;
      JSONObject f=draft.get(i);
      if(!activeModule.equals(f.optString("module")))continue;
      Button button=new Button(this);
      button.setAllCaps(false);
      button.setText(f.optString("module")+" | "+f.optString("label")+" | "+f.optString("row"));
      button.setOnClickListener(v->edit(index));
      list.addView(button);
    }
  }
  private void edit(int index){
    String[] keys={"id","module","row","mode","indices","on","off","byte","label"};
    String[] hints={"ID","IC / BCM / RKE / ABS","720-01-01","HEX / BITS","0 или 0,1","HEX включить","HEX выключить","0","Название функции"};
    LinearLayout fields=new LinearLayout(this);fields.setOrientation(LinearLayout.VERTICAL);
    ScrollView scroller=new ScrollView(this);scroller.addView(fields);
    EditText[] inputs=new EditText[keys.length];
    JSONObject existing=index<0?new JSONObject():draft.get(index);
    for(int i=0;i<keys.length;i++){
      EditText input=new EditText(this);inputs[i]=input;
      input.setSingleLine(true);input.setHint(hints[i]);
      input.setText(existing.optString(keys[i],keys[i].equals("byte")?"0":keys[i].equals("module")?activeModule:""));
      if(keys[i].equals("byte"))input.setInputType(InputType.TYPE_CLASS_NUMBER);
      fields.addView(input);
    }
    AlertDialog.Builder builder=new AlertDialog.Builder(this).setTitle(index<0?"Новая функция":"Изменить функцию")
      .setView(scroller).setNegativeButton("Отмена",null);
    if(index>=0)builder.setNeutralButton("Удалить",(d,w)->{draft.remove(index);save();redraw();});
    AlertDialog dialog=builder.setPositiveButton("Сохранить",null).create();
    dialog.setOnShowListener(ignored->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
      try{
        JSONObject obj=new JSONObject();
        for(int i=0;i<keys.length;i++){
          String value=inputs[i].getText().toString().trim();
          if(keys[i].equals("byte"))obj.put(keys[i],Integer.parseInt(value));
          else obj.put(keys[i],value);
        }
        if(!obj.getString("row").matches("[0-9A-Fa-f]{3}-[0-9A-Fa-f]{2}-[0-9A-Fa-f]{2}"))
          throw new IllegalArgumentException("Неверный адрес строки");
        if(obj.getString("id").isEmpty()||obj.getString("label").isEmpty())
          throw new IllegalArgumentException("Нужны ID и название");
        String module=obj.getString("module");
        if(!(module.equals("IC")||module.equals("BCM")||module.equals("RKE")||module.equals("ABS")))
          throw new IllegalArgumentException("Неизвестный блок");
        String prefix=module.equals("IC")?"720":module.equals("BCM")?"726":module.equals("RKE")?"731":"760";
        if(!obj.getString("row").toUpperCase(java.util.Locale.US).startsWith(prefix+"-"))
          throw new IllegalArgumentException("Адрес не соответствует блоку "+module);
        String mode=obj.getString("mode");
        String indices=obj.getString("indices");
        String on=obj.getString("on"),off=obj.getString("off");
        if(!(mode.equals("HEX")||mode.equals("BITS"))||!indices.matches("[0-9]+(,[0-9]+)*"))
          throw new IllegalArgumentException("Неверный режим или индексы");
        String[] positions=indices.split(",");
        java.util.HashSet<String> unique=new java.util.HashSet<>(java.util.Arrays.asList(positions));
        if(unique.size()!=positions.length)throw new IllegalArgumentException("Повтор индекса");
        for(String position:positions){
          int n=Integer.parseInt(position);
          if(n>127||(mode.equals("BITS")&&n>7))throw new IllegalArgumentException("Индекс вне диапазона");
        }
        if(!on.matches("[0-9A-Fa-f]{1,64}")||(!off.isEmpty()&&!off.matches("[0-9A-Fa-f]{1,64}")))
          throw new IllegalArgumentException("Неверное HEX значение");
        if(mode.equals("HEX")&&(on.length()!=positions.length||(!off.isEmpty()&&off.length()!=positions.length)))
          throw new IllegalArgumentException("Число HEX символов не совпадает с индексами");
        if(obj.getInt("byte")<0||obj.getInt("byte")>63)throw new IllegalArgumentException("Неверный индекс байта");
        for(int i=0;i<draft.size();i++)
          if(i!=index&&draft.get(i).optString("id").equals(obj.getString("id")))
            throw new IllegalArgumentException("ID функции уже существует");
        if(index<0)draft.add(obj);else draft.set(index,obj);
        activeModule=module;
        save();redraw();dialog.dismiss();
      }catch(Exception ex){Toast.makeText(this,"Ошибка: "+ex.getMessage(),Toast.LENGTH_LONG).show();}
    }));
    dialog.show();
  }
  private JSONObject draftDocument() throws org.json.JSONException {
    JSONArray arr=new JSONArray();for(JSONObject f:draft)arr.put(f);
    JSONObject doc=new JSONObject();doc.put("schema",1);doc.put("version",1);doc.put("features",arr);
    return doc;
  }
  private void exportDraft(){
    if(draft.isEmpty()){
      Toast.makeText(this,"Сначала добавьте функцию",Toast.LENGTH_SHORT).show();return;
    }
    Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("application/json");
    intent.putExtra(Intent.EXTRA_TITLE,"mazda6gh-catalog-draft.json");
    startActivityForResult(intent,EXPORT_DRAFT);
  }
  @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
    super.onActivityResult(requestCode,resultCode,data);
    if(requestCode!=EXPORT_DRAFT||resultCode!=RESULT_OK||data==null)return;
    Uri uri=data.getData();if(uri==null)return;
    try(OutputStream output=getContentResolver().openOutputStream(uri)){
      if(output==null)throw new java.io.IOException("Нет доступа к файлу");
      output.write(draftDocument().toString(2).getBytes(StandardCharsets.UTF_8));
      Toast.makeText(this,"Черновик сохранён (не опубликован)",Toast.LENGTH_LONG).show();
    }catch(Exception ex){Toast.makeText(this,"Ошибка экспорта: "+ex.getMessage(),Toast.LENGTH_LONG).show();}
  }
  private void showJson(){
    JSONArray arr=new JSONArray();for(JSONObject f:draft)arr.put(f);
    new AlertDialog.Builder(this).setTitle("Локальный черновик")
      .setMessage(arr.toString()).setPositiveButton("Закрыть",null).show();
  }
}
