package com.calibre90.astestmob;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
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
public final class CatalogAdminActivity extends Activity {
  private final ArrayList<JSONObject> draft=new ArrayList<>();
  private LinearLayout list;
  private TextView status;

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
    Button add=new Button(this);add.setText("Добавить функцию");root.addView(add);
    add.setOnClickListener(v->edit(-1));
    ScrollView scroll=new ScrollView(this);
    list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);
    scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    Button preview=new Button(this);preview.setText("Показать JSON черновика");root.addView(preview);
    preview.setOnClickListener(v->showJson());
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
    list.removeAllViews();
    status.setText("Функций в локальном черновике: "+draft.size()+"\nПубликация в интернет пока отключена.");
    for(int i=0;i<draft.size();i++){
      final int index=i;
      JSONObject f=draft.get(i);
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
      input.setText(existing.optString(keys[i],keys[i].equals("byte")?"0":""));
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
        if(index<0)draft.add(obj);else draft.set(index,obj);
        save();redraw();dialog.dismiss();
      }catch(Exception ex){Toast.makeText(this,"Ошибка: "+ex.getMessage(),Toast.LENGTH_LONG).show();}
    }));
    dialog.show();
  }
  private void showJson(){
    JSONArray arr=new JSONArray();for(JSONObject f:draft)arr.put(f);
    new AlertDialog.Builder(this).setTitle("Локальный черновик")
      .setMessage(arr.toString()).setPositiveButton("Закрыть",null).show();
  }
}
