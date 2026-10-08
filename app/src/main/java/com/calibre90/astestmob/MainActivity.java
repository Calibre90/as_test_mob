package com.calibre90.astestmob;

import android.app.*;
import android.os.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
  int red=Color.rgb(225,18,24), panel=Color.rgb(24,24,24), edge=Color.rgb(75,75,75);
  LinearLayout root,tabs,rows; ArrayList<View> tabViews=new ArrayList<>();
  GradientDrawable bg(int stroke, int fill, float radius){
    GradientDrawable g=new GradientDrawable(); g.setColor(fill); g.setCornerRadius(radius); g.setStroke(2,stroke); return g;
  }
  View box(int h, int stroke){
    View v=new View(this); v.setBackground(bg(stroke,panel,12)); v.setLayoutParams(new LinearLayout.LayoutParams(0,h,1)); return v;
  }
  @Override public void onCreate(Bundle b){ super.onCreate(b); build(); }
  void build(){
    ScrollView sc=new ScrollView(this); root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(18,18,18,18); root.setBackgroundColor(Color.rgb(6,6,6)); sc.addView(root);

    LinearLayout head=new LinearLayout(this); head.setGravity(Gravity.CENTER_VERTICAL);
    TextView title=new TextView(this); title.setText("MAZDA 6 GH\nAS-BUILT STUDIO"); title.setTextColor(Color.WHITE);
    title.setTextSize(22); title.setTypeface(null,1); head.addView(title,new LinearLayout.LayoutParams(0,120,1));
    TextView gear=new TextView(this); gear.setText("⚙"); gear.setTextSize(38); gear.setGravity(17); gear.setTextColor(Color.LTGRAY);
    gear.setBackground(bg(edge,panel,18)); head.addView(gear,new LinearLayout.LayoutParams(90,90)); gear.setOnClickListener(v->admin());
    root.addView(head);

    tabs=new LinearLayout(this); tabs.setPadding(0,8,0,8);
    for(int i=0;i<4;i++){ final int n=i; View t=box(66,i==0?red:edge); LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)t.getLayoutParams(); lp.setMargins(4,0,4,0); tabs.addView(t,lp); tabViews.add(t); t.setOnClickListener(v->select(n));}
    root.addView(tabs,new LinearLayout.LayoutParams(-1,82));

    LinearLayout info=new LinearLayout(this); info.setPadding(8,8,8,8); info.setBackground(bg(edge,Color.rgb(14,14,14),12));
    for(int i=0;i<3;i++){ View p=box(78,edge); LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)p.getLayoutParams(); lp.setMargins(4,0,4,0); info.addView(p,lp);}
    root.addView(info,new LinearLayout.LayoutParams(-1,100));

    LinearLayout checks=new LinearLayout(this); checks.setPadding(4,12,4,12);
    for(int i=0;i<2;i++){ CheckBox c=new CheckBox(this); c.setButtonTintList(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{red,Color.GRAY})); checks.addView(c,new LinearLayout.LayoutParams(0,70,1));}
    root.addView(checks);

    rows=new LinearLayout(this); rows.setOrientation(LinearLayout.VERTICAL);
    for(int i=0;i<10;i++){ LinearLayout row=new LinearLayout(this); row.setPadding(6,5,6,5); for(int j=0;j<4;j++){ View cell=box(48,edge); LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)cell.getLayoutParams(); lp.setMargins(2,0,2,0); row.addView(cell,lp);} rows.addView(row); }
    root.addView(rows);

    LinearLayout actions=new LinearLayout(this); actions.setPadding(0,14,0,10);
    View open=box(86,edge), save=box(86,red); LinearLayout.LayoutParams a=(LinearLayout.LayoutParams)open.getLayoutParams(); a.setMargins(4,0,4,0); actions.addView(open,a);
    LinearLayout.LayoutParams s=(LinearLayout.LayoutParams)save.getLayoutParams(); s.setMargins(4,0,4,0); actions.addView(save,s);
    open.setOnClickListener(v->Toast.makeText(this,"",Toast.LENGTH_SHORT).show()); save.setOnClickListener(v->Toast.makeText(this,"",Toast.LENGTH_SHORT).show());
    root.addView(actions);

    View about=box(96,edge); root.addView(about,new LinearLayout.LayoutParams(-1,96)); about.setOnClickListener(v->about());
    setContentView(sc);
  }
  void select(int n){ for(int i=0;i<tabViews.size();i++) tabViews.get(i).setBackground(bg(i==n?red:edge,panel,12)); }
  void admin(){ dialog(true); }
  void about(){ dialog(false); }
  void dialog(boolean admin){
    Dialog d=new Dialog(this); LinearLayout p=new LinearLayout(this); p.setOrientation(LinearLayout.VERTICAL); p.setPadding(24,24,24,24); p.setBackground(bg(red,Color.rgb(12,12,12),20));
    if(admin){ for(int i=0;i<2;i++){ EditText e=new EditText(this); e.setTextColor(Color.WHITE); e.setBackground(bg(edge,panel,10)); p.addView(e,new LinearLayout.LayoutParams(520,80)); }}
    View b=box(82,red); p.addView(b,new LinearLayout.LayoutParams(520,82)); b.setOnClickListener(v->d.dismiss()); d.setContentView(p); d.show();
  }
}
