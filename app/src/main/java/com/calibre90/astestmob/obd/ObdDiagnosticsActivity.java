package com.calibre90.astestmob.obd;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

/** Read-only generic OBD-II diagnostics. No mode 04, ECU programming or security access. */
public final class ObdDiagnosticsActivity extends Activity {
  private final ExecutorService worker=Executors.newSingleThreadExecutor();
  private final Handler ui=new Handler(Looper.getMainLooper());
  private TextView output;
  private Button pids, dtcs;
  private volatile boolean busy;
  @Override public void onCreate(Bundle state){
    super.onCreate(state);
    getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
    getWindow().setStatusBarColor(Color.rgb(20,24,30));
    LinearLayout root=new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(20,20,20,20);
    TextView title=new TextView(this);
    title.setText("OBD STUDIO — диагностика (только чтение)");
    title.setTextSize(20);
    root.addView(title);
    TextView warning=new TextView(this);
    warning.setText("Данные стандартного OBD-II двигателя. ABS/IC/BCM требуют отдельной реализации. Очистка ошибок и запись отключены.");
    root.addView(warning);
    pids=new Button(this);pids.setText("Считать параметры двигателя");root.addView(pids);
    dtcs=new Button(this);dtcs.setText("Прочитать ошибки двигателя");root.addView(dtcs);
    output=new TextView(this);output.setTextIsSelectable(true);
    output.setText("Состояние: "+ObdSession.status());
    ScrollView scroll=new ScrollView(this);scroll.addView(output);
    root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    setContentView(root);
    pids.setOnClickListener(v->runRead(false));
    dtcs.setOnClickListener(v->runRead(true));
  }
  private void runRead(boolean errors){
    if(busy)return;
    if(!ObdSession.vehicleReady()){
      output.setText("Сначала подтвердите связь с автомобилем в разделе «Адаптер».");return;
    }
    busy=true;pids.setEnabled(false);dtcs.setEnabled(false);
    output.setText("Выполняется чтение…");
    worker.execute(()->{
      StringBuilder result=new StringBuilder();
      try{
        // Explicitly restrict to the standard functional OBD header.
        ObdSession.command("ATSH7DF");
        if(errors){
          String raw=ObdSession.command("03");
          result.append("Режим 03 — сохранённые ошибки двигателя\n");
          result.append("Ответ ELM: ").append(raw).append("\n");
          result.append("Неочищенные данные. Интерпретация кодов будет добавлена после проверки ответов на автомобиле.\n");
        }else{
          String supported=ObdSession.command("0100");
          result.append("Поддерживаемые PID (0100): ").append(supported).append("\n\n");
          String[] commands={"010C","0105","010D","0104"};
          String[] labels={"Обороты","Температура ОЖ","Скорость","Нагрузка"};
          for(int i=0;i<commands.length;i++){
            String raw=ObdSession.command(commands[i]);
            result.append(labels[i]).append(" (").append(commands[i]).append("): ");
            Double value=decode(raw,commands[i]);
            if(value==null)result.append("Нет подтверждённых данных; ответ: ").append(raw.trim());
            else result.append(String.format(Locale.US,"%.1f",value)).append(i==0?" об/мин":i==1?" °C":i==2?" км/ч":" %");
            result.append("\n");
          }
        }
      }catch(Exception ex){result.append("Ошибка чтения: ").append(ex.getClass().getSimpleName()).append(" — ").append(ex.getMessage());}
      final String text=result.toString();
      ui.post(()->{
        if(!isFinishing()&&!isDestroyed()){
          output.setText(text);
          pids.setEnabled(true);dtcs.setEnabled(true);
        }
        busy=false;
      });
    });
  }
  /** Only accepts a complete, recognizable mode 01 response; ignores SEARCHING/NO DATA. */
  private static Double decode(String raw,String request){
    if(raw==null)return null;
    String marker="41"+request.substring(2);
    for(String line:raw.toUpperCase(Locale.US).split("[\\r\\n]+")){
      String hex=line.replaceAll("[^0-9A-F]","");
      int at=hex.indexOf(marker);
      if(at<0)continue;
      String payload=hex.substring(at+marker.length());
      if(payload.length()<2)continue;
      try{
        int a=Integer.parseInt(payload.substring(0,2),16);
        switch(request){
          case "0105":return (double)(a-40);
          case "010D":return (double)a;
          case "0104":return a*100.0/255.0;
          case "010C":
            if(payload.length()<4)return null;
            int b=Integer.parseInt(payload.substring(2,4),16);
            return (256.0*a+b)/4.0;
          default:return null;
        }
      }catch(NumberFormatException ignored){}
    }
    return null;
  }
  @Override protected void onDestroy(){worker.shutdownNow();super.onDestroy();}
}
