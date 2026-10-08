package com.calibre90.astestmob;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.view.*;
import android.graphics.Insets;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    getWindow().setStatusBarColor(Color.BLACK);
    getWindow().setNavigationBarColor(Color.BLACK);
    if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
    StudioView studio = new StudioView(this);
    setContentView(studio);
  }

  class StudioView extends View {
    final float DW=400f,DH=919f, CROP_L=21f, CROP_R=381f;
    final HashMap<String,Bitmap> bm=new HashMap<>();
    int active=0; boolean left=false,right=true;
    int safeLeft=0,safeTop=0,safeRight=0,safeBottom=0;
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    StudioView(Context c){ super(c); setBackgroundColor(Color.BLACK);
      setOnApplyWindowInsetsListener((v,insets)->{
        if(Build.VERSION.SDK_INT>=30){
          android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());
          safeLeft=bars.left; safeTop=bars.top; safeRight=bars.right; safeBottom=bars.bottom;
        } else {
          safeLeft=insets.getSystemWindowInsetLeft(); safeTop=insets.getSystemWindowInsetTop();
          safeRight=insets.getSystemWindowInsetRight(); safeBottom=insets.getSystemWindowInsetBottom();
        }
        invalidate(); return insets;
      });
      String[] names={"header_mazda6gh_asbuilt_speedometer","tab_active_red","tab_inactive_gray_1",
      "module_info_panel_with_gauge","option_row_left","option_row_right","checkbox_empty","checkbox_checked_red",
      "table_header_red","table_row_gray","open_file_button","save_file_button","creator_link_panel"};
      for(String n:names){ int id=getResources().getIdentifier(n,"drawable",getPackageName()); if(id!=0) bm.put(n,BitmapFactory.decodeResource(getResources(),id));}
    }
    void draw(Canvas c,String n,float l,float t,float r,float b){
      Bitmap x=bm.get(n); if(x!=null)c.drawBitmap(x,null,new RectF(l,t,r,b),p);
    }
    @Override protected void onDraw(Canvas raw){
      super.onDraw(raw);
      float aw=getWidth()-safeLeft-safeRight, ah=getHeight()-safeTop-safeBottom;
      float visibleW=CROP_R-CROP_L;
      float sx=aw/visibleW, sy=ah/DH;
      raw.save();
      raw.clipRect(safeLeft,safeTop,safeLeft+aw,safeTop+ah);
      raw.translate(safeLeft-CROP_L*sx,safeTop); raw.scale(sx,sy);
      draw(raw,"header_mazda6gh_asbuilt_speedometer",10,0,397,132);
      float[] xs={21,112,202,291,381};
      for(int i=0;i<4;i++) draw(raw,i==active?"tab_active_red":"tab_inactive_gray_1",xs[i],135,xs[i+1]-2,174);
      draw(raw,"module_info_panel_with_gauge",21,178,382,234);
      draw(raw,"option_row_left",22,235,202,315); draw(raw,"option_row_right",202,235,381,315);
      draw(raw,left?"checkbox_checked_red":"checkbox_empty",27,239,59,273);
      draw(raw,right?"checkbox_checked_red":"checkbox_empty",210,239,247,276);
      draw(raw,"table_header_red",21,315,381,348);
      for(int i=0;i<9;i++) draw(raw,"table_row_gray",21,348+i*42,381,390+i*42);
      draw(raw,"open_file_button",21,729,199,790); draw(raw,"save_file_button",202,729,381,790);
      draw(raw,"creator_link_panel",12,800,390,888);
      raw.restore();
    }
    boolean hit(float x,float y,float l,float t,float r,float b){return x>=l&&x<=r&&y>=t&&y<=b;}
    @Override public boolean onTouchEvent(android.view.MotionEvent e){
      if(e.getAction()!=MotionEvent.ACTION_UP)return true;
      float aw=getWidth()-safeLeft-safeRight, ah=getHeight()-safeTop-safeBottom;
      float visibleW=CROP_R-CROP_L;
      float sx=aw/visibleW, sy=ah/DH;
      float x=CROP_L+(e.getX()-safeLeft)/sx, y=(e.getY()-safeTop)/sy;
      if(y>=135&&y<=174){ if(x>=21&&x<112)active=0; else if(x<202)active=1; else if(x<291)active=2; else if(x<=381)active=3; invalidate(); return true;}
      if(hit(x,y,330,20,397,125)){ showAdmin(); return true;}
      if(hit(x,y,22,235,202,315)){left=!left;invalidate();return true;}
      if(hit(x,y,202,235,381,315)){right=!right;invalidate();return true;}
      if(hit(x,y,21,729,199,790)){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,10);return true;}
      if(hit(x,y,202,729,381,790)){ Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/octet-stream");i.putExtra(Intent.EXTRA_TITLE,"block.abt");startActivityForResult(i,11);return true;}
      if(hit(x,y,12,800,390,888)){showAbout();return true;}
      return true;
    }
  }

  ImageView image(String n){ ImageView v=new ImageView(this); v.setScaleType(ImageView.ScaleType.FIT_XY); int id=getResources().getIdentifier(n,"drawable",getPackageName()); if(id!=0)v.setImageResource(id); return v; }
  void showAdmin(){
    Dialog d=new Dialog(this); d.getWindow();
    FrameLayout f=new FrameLayout(this); f.addView(image("admin_modal_full"),new FrameLayout.LayoutParams(-1,-1));
    View close=new View(this); FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(90,80,Gravity.RIGHT|Gravity.TOP); f.addView(close,cp); close.setOnClickListener(v->d.dismiss());
    d.setContentView(f); Window w=d.getWindow(); if(w!=null){w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));w.setDimAmount(.58f);w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);w.setLayout((int)(getResources().getDisplayMetrics().widthPixels*.88f),WindowManager.LayoutParams.WRAP_CONTENT);} d.show();
    if(w!=null)w.setLayout((int)(getResources().getDisplayMetrics().widthPixels*.88f),(int)(getResources().getDisplayMetrics().heightPixels*.34f));
  }
  void showAbout(){
    Dialog d=new Dialog(this); FrameLayout f=new FrameLayout(this); f.addView(image("about_modal_full"),new FrameLayout.LayoutParams(-1,-1));
    View close=new View(this); FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(90,80,Gravity.RIGHT|Gravity.TOP); f.addView(close,cp); close.setOnClickListener(v->d.dismiss());
    d.setContentView(f); Window w=d.getWindow(); d.show(); if(w!=null){w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));w.setDimAmount(.58f);w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);w.setLayout((int)(getResources().getDisplayMetrics().widthPixels*.88f),(int)(getResources().getDisplayMetrics().heightPixels*.39f));}
  }
}
