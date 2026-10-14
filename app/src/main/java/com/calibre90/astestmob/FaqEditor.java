package com.calibre90.astestmob;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;

/** Local FAQ editor. Remote publishing is intentionally not implied by saving a draft. */
public final class FaqEditor {
  private FaqEditor(){}
  public static void open(Activity activity){
    final JSONArray entries=StudioFaq.localEntries(activity);
    final LinearLayout list=new LinearLayout(activity);list.setOrientation(LinearLayout.VERTICAL);
    final Runnable[] refresh=new Runnable[1];
    refresh[0]=()->{
      list.removeAllViews();
      for(int i=0;i<entries.length();i++){
        JSONObject item=entries.optJSONObject(i);if(item==null)continue;
        final int index=i;
        Button button=new Button(activity);
        button.setAllCaps(false);button.setText((i+1)+". "+item.optString("name","Без названия"));
        LinearLayout row=new LinearLayout(activity);row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(button,new LinearLayout.LayoutParams(0,-2,1));
        Button up=new Button(activity);up.setText("↑");up.setEnabled(index>0);
        row.addView(up,new LinearLayout.LayoutParams(0,-2,.20f));
        up.setOnClickListener(v->move(activity,entries,index,index-1,refresh[0]));
        Button down=new Button(activity);down.setText("↓");down.setEnabled(index<entries.length()-1);
        row.addView(down,new LinearLayout.LayoutParams(0,-2,.20f));
        down.setOnClickListener(v->move(activity,entries,index,index+1,refresh[0]));
        list.addView(row);button.setOnClickListener(v->edit(activity,entries,index,refresh[0]));
      }
    };
    LinearLayout root=new LinearLayout(activity);root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(16,8,16,8);
    TextView notice=new TextView(activity);
    notice.setText("FAQ входит в общие настройки Studio и отправляется на сервер вместе с ними.");
    notice.setTextColor(Color.DKGRAY);root.addView(notice);
    Button add=new Button(activity);add.setText("Добавить запись FAQ");add.setAllCaps(false);
    root.addView(add);add.setOnClickListener(v->edit(activity,entries,-1,refresh[0]));
    ScrollView scroll=new ScrollView(activity);scroll.addView(list);
    root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    refresh[0].run();
    AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("FAQ · редактор")
      .setView(root).setPositiveButton("Закрыть",null).create();
    dialog.show();
  }
  private static boolean persist(Activity a,JSONArray entries){
    boolean ok=a.getSharedPreferences("studio_faq",Activity.MODE_PRIVATE).edit()
      .putString("entries",entries.toString()).commit();
    if(!ok)Toast.makeText(a,"Ошибка сохранения FAQ",Toast.LENGTH_LONG).show();
    return ok;
  }
  private static void edit(Activity a,JSONArray entries,int index,Runnable refresh){
    JSONObject existing=index<0?new JSONObject():entries.optJSONObject(index);
    LinearLayout fields=new LinearLayout(a);fields.setOrientation(LinearLayout.VERTICAL);fields.setPadding(20,8,20,8);
    EditText name=new EditText(a);name.setSingleLine(true);name.setHint("Название функции");
    name.setText(existing==null?"":existing.optString("name"));fields.addView(name);
    EditText description=new EditText(a);description.setSingleLine(false);description.setMinLines(6);
    description.setGravity(android.view.Gravity.TOP);description.setHint("Подробное описание");
    description.setText(existing==null?"":existing.optString("description"));fields.addView(description);
    EditText extra=new EditText(a);extra.setSingleLine(false);extra.setMinLines(2);
    extra.setHint("Дополнительный текст между красными линиями");
    extra.setText(existing==null?"":existing.optString("extra_text",""));
    fields.addView(extra);
    ScrollView scroll=new ScrollView(a);scroll.addView(fields);
    AlertDialog.Builder builder=new AlertDialog.Builder(a).setTitle(index<0?"Добавить FAQ":"Редактировать FAQ")
      .setView(scroll).setNegativeButton("Отмена",null);
    if(index>=0)builder.setNeutralButton("Удалить",(d,w)->{
      JSONArray next=new JSONArray();for(int i=0;i<entries.length();i++)if(i!=index)next.put(entries.optJSONObject(i));
      if(persist(a,next)){replace(entries,next);refresh.run();}
    });
    AlertDialog dialog=builder.setPositiveButton("Сохранить",null).create();
    dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b->{
      String title=name.getText().toString().trim();
      if(title.isEmpty()){name.setError("Введите название");return;}
      if(title.length()>120){name.setError("Не более 120 символов");return;}
      if(description.getText().length()>12000){description.setError("Не более 12000 символов");return;}
      if(index<0 && entries.length()>=200){
        Toast.makeText(a,"Максимум 200 записей FAQ",Toast.LENGTH_LONG).show();return;
      }
      for(int i=0;i<entries.length();i++){
        JSONObject other=entries.optJSONObject(i);
        if(i!=index && other!=null && title.equalsIgnoreCase(other.optString("name","").trim())){
          name.setError("Название FAQ уже существует");return;
        }
      }
      try{
        JSONObject row=new JSONObject();row.put("name",title);row.put("description",description.getText().toString());row.put("extra_text",extra.getText().toString());
        JSONArray next=new JSONArray();
        for(int i=0;i<entries.length();i++)next.put(i==index?row:entries.getJSONObject(i));
        if(index<0)next.put(row);
        if(persist(a,next)){replace(entries,next);refresh.run();dialog.dismiss();}
      }catch(Exception e){Toast.makeText(a,"Ошибка: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }));
    dialog.show();
  }
  private static void move(Activity a,JSONArray rows,int from,int to,Runnable refresh){
    if(from<0||to<0||from>=rows.length()||to>=rows.length())return;
    JSONObject first=rows.optJSONObject(from),second=rows.optJSONObject(to);
    try{
      JSONArray next=new JSONArray();
      for(int i=0;i<rows.length();i++)
        next.put(i==from?second:i==to?first:rows.getJSONObject(i));
      if(persist(a,next)){replace(rows,next);refresh.run();}
    }catch(Exception e){Toast.makeText(a,"Ошибка порядка FAQ",Toast.LENGTH_LONG).show();}
  }
  private static void replace(JSONArray target,JSONArray source){
    while(target.length()>0)target.remove(target.length()-1);
    for(int i=0;i<source.length();i++)target.put(source.optJSONObject(i));
  }
}
