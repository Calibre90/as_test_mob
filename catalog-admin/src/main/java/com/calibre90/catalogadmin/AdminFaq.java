package com.calibre90.catalogadmin;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.Gravity;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;

/** Separate Admin Release FAQ draft editor; no implicit remote publication. */
public final class AdminFaq {
  private AdminFaq(){}
  public static void show(Activity a){
    JSONArray entries;
    try{entries=new JSONArray(a.getSharedPreferences("admin_faq",0).getString("entries","[]"));}
    catch(Exception e){entries=new JSONArray();}
    final JSONArray rows=entries;
    LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(16,8,16,8);
    TextView note=new TextView(a);note.setText("FAQ — локальный черновик. Онлайн-публикация пока не подключена.");
    root.addView(note);
    Button add=new Button(a);add.setText("Добавить FAQ");root.addView(add);
    LinearLayout list=new LinearLayout(a);list.setOrientation(LinearLayout.VERTICAL);
    ScrollView scroll=new ScrollView(a);scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    final Runnable[] refresh=new Runnable[1];
    refresh[0]=()->{list.removeAllViews();for(int i=0;i<rows.length();i++){
      JSONObject item=rows.optJSONObject(i);if(item==null)continue;
      final int index=i;Button b=new Button(a);b.setText((i+1)+". "+item.optString("name"));
      b.setAllCaps(false);
      LinearLayout line=new LinearLayout(a);line.setOrientation(LinearLayout.HORIZONTAL);
      line.addView(b,new LinearLayout.LayoutParams(0,-2,1));
      Button up=new Button(a);up.setText("↑");up.setEnabled(index>0);
      line.addView(up,new LinearLayout.LayoutParams(0,-2,.20f));
      up.setOnClickListener(v->move(a,rows,index,index-1,refresh[0]));
      Button down=new Button(a);down.setText("↓");down.setEnabled(index<rows.length()-1);
      line.addView(down,new LinearLayout.LayoutParams(0,-2,.20f));
      down.setOnClickListener(v->move(a,rows,index,index+1,refresh[0]));
      list.addView(line);b.setOnClickListener(v->edit(a,rows,index,refresh[0]));
    }};
    add.setOnClickListener(v->edit(a,rows,-1,refresh[0]));refresh[0].run();
    AlertDialog dialog=new AlertDialog.Builder(a).setTitle("FAQ · Admin Release").setView(root)
      .setPositiveButton("Закрыть",null).create();dialog.show();
  }
  private static void save(Activity a,JSONArray rows){
    boolean ok=a.getSharedPreferences("admin_faq",0).edit().putString("entries",rows.toString()).commit();
    if(!ok)Toast.makeText(a,"Ошибка сохранения",Toast.LENGTH_LONG).show();
  }
  private static void move(Activity a,JSONArray rows,int from,int to,Runnable refresh){
    if(from<0||to<0||from>=rows.length()||to>=rows.length())return;
    JSONObject first=rows.optJSONObject(from),second=rows.optJSONObject(to);
    try{rows.put(from,second);rows.put(to,first);save(a,rows);refresh.run();}
    catch(Exception e){Toast.makeText(a,"Ошибка порядка FAQ",Toast.LENGTH_LONG).show();}
  }
  private static void edit(Activity a,JSONArray rows,int index,Runnable refresh){
    JSONObject old=index<0?new JSONObject():rows.optJSONObject(index);
    LinearLayout fields=new LinearLayout(a);fields.setOrientation(LinearLayout.VERTICAL);
    EditText title=new EditText(a);title.setSingleLine(true);title.setHint("Название функции");
    title.setText(old==null?"":old.optString("name"));fields.addView(title);
    EditText desc=new EditText(a);desc.setSingleLine(false);desc.setMinLines(6);desc.setGravity(Gravity.TOP);
    desc.setHint("Описание");desc.setText(old==null?"":old.optString("description"));fields.addView(desc);
    ScrollView scroll=new ScrollView(a);scroll.addView(fields);
    AlertDialog.Builder builder=new AlertDialog.Builder(a).setTitle(index<0?"Новая запись FAQ":"Изменить FAQ")
      .setView(scroll).setNegativeButton("Отмена",null);
    if(index>=0)builder.setNeutralButton("Удалить",(d,w)->{
      rows.remove(index);save(a,rows);refresh.run();
    });
    AlertDialog dialog=builder.setPositiveButton("Сохранить",null).create();
    dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b->{
      String name=title.getText().toString().trim();if(name.isEmpty()){title.setError("Введите название");return;}
      try{JSONObject item=new JSONObject();item.put("name",name);item.put("description",desc.getText().toString());
        if(index<0)rows.put(item);else rows.put(index,item);
        save(a,rows);refresh.run();dialog.dismiss();
      }catch(Exception e){Toast.makeText(a,"Ошибка FAQ: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }));dialog.show();
  }
}
