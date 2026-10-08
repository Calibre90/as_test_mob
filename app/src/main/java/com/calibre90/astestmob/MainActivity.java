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
      String[] names={"main_full","tab_active","tab_inactive_1","checkbox_empty","checkbox_checked","open_button","save_button","footer_link_bar"};
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
      draw(raw,"main_full",21,0,381,919);
      float[] xs={21,112,202,291,381};
      for(int i=0;i<4;i++) draw(raw,i==active?"tab_active":"tab_inactive_1",xs[i],180,xs[i+1]-2,250);
      draw(raw,left?"checkbox_checked":"checkbox_empty",31,343,66,379);
      draw(raw,right?"checkbox_checked":"checkbox_empty",31,416,66,452);
      draw(raw,"open_button",27,700,196,777);
      draw(raw,"save_button",203,700,375,777);
      raw.restore();
    }
    boolean hit(float x,float y,float l,float t,float r,float b){return x>=l&&x<=r&&y>=t&&y<=b;}
    @Override public boolean onTouchEvent(android.view.MotionEvent e){
      if(e.getAction()!=MotionEvent.ACTION_UP)return true;
      float aw=getWidth()-safeLeft-safeRight, ah=getHeight()-safeTop-safeBottom;
      float visibleW=CROP_R-CROP_L;
      float sx=aw/visibleW, sy=ah/DH;
      float x=CROP_L+(e.getX()-safeLeft)/sx, y=(e.getY()-safeTop)/sy;
      if(y>=180&&y<=250){ if(x>=21&&x<112)active=0; else if(x<202)active=1; else if(x<291)active=2; else if(x<=381)active=3; invalidate(); return true;}
      if(hit(x,y,327,20,397,180)){ showAdmin(); return true;}
      if(hit(x,y,21,325,381,398)){left=!left;invalidate();return true;}
      if(hit(x,y,21,399,381,477)){right=!right;invalidate();return true;}
      if(hit(x,y,21,700,199,777)){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,10);return true;}
      if(hit(x,y,202,700,381,777)){ Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/octet-stream");i.putExtra(Intent.EXTRA_TITLE,"block.abt");startActivityForResult(i,11);return true;}
      if(hit(x,y,21,800,381,919)){showAbout();return true;}
      return true;
    }
  }

  ImageView image(String n){ ImageView v=new ImageView(this); v.setScaleType(ImageView.ScaleType.FIT_XY); int id=getResources().getIdentifier(n,"drawable",getPackageName()); if(id!=0)v.setImageResource(id); return v; }
  void showAdmin(){
    Dialog d=new Dialog(this); d.getWindow();
    FrameLayout f=new FrameLayout(this); f.addView(image("admin_modal"),new FrameLayout.LayoutParams(-1,-1));
    View close=new View(this); FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(90,80,Gravity.RIGHT|Gravity.TOP); f.addView(close,cp); close.setOnClickListener(v->d.dismiss());
    d.setContentView(f); Window w=d.getWindow(); if(w!=null){w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));w.setDimAmount(.58f);w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);w.setLayout((int)(getResources().getDisplayMetrics().widthPixels*.88f),WindowManager.LayoutParams.WRAP_CONTENT);} d.show();
    if(w!=null)w.setLayout((int)(getResources().getDisplayMetrics().widthPixels*.88f),(int)(getResources().getDisplayMetrics().heightPixels*.34f));
  }
  void showAbout(){
    Dialog d=new Dialog(this); FrameLayout f=new FrameLayout(this); f.addView(image("about_modal"),new FrameLayout.LayoutParams(-1,-1));
    View close=new View(this); FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(90,80,Gravity.RIGHT|Gravity.TOP); f.addView(close,cp); close.setOnClickListener(v->d.dismiss());
    d.setContentView(f); Window w=d.getWindow(); d.show(); if(w!=null){w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));w.setDimAmount(.58f);w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);w.setLayout((int)(getResources().getDisplayMetrics().widthPixels*.88f),(int)(getResources().getDisplayMetrics().heightPixels*.39f));}
  }
}
