package com.calibre90.astestmob;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.provider.Settings;
import android.util.Base64;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import org.json.JSONObject;
import java.util.Locale;

/** Offline, device-bound lifetime license verification. The signing private key is NEVER packaged in this APK. */
final class LicenseGate {
  private static final String PUBLIC_KEY="MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnvIOfc7yWryj6RAP2tNG+K5wbh74J+Kpq7YxnBQFnypdR8sUXIqgfgDQka/jLidtiW4SIDlnoe7zgc7yeiQsxGGzTTLGSbuHigLxMNBSVqTPwtGLxs4PWX3uLbyjMX0zlScatwuuzbausW8kRcS20kg3fdXKGJnKE+zzckKwyN2OrT+vkLFUGGlvI+GaVqHa76N3+6VE08FrA9u8am4rF9wLG8lw4yjEx8euNYI2sWAfHjVS/Yb3G28gwnScGPhUVk1EK9retTDI1Yo8pMDnCbINcG39yZLI5l5yCiZHIWqoNX6224RtWEjYOb9rJkqrOkLGcPpt6vR5YP6uub/iswIDAQAB";
  private static final String STORE="mazda_lifetime_license_v1";
  private static final String CODE="signed_code";
  private LicenseGate(){}

  static String deviceId(Activity activity){
    try{
      String androidId=Settings.Secure.getString(activity.getContentResolver(),Settings.Secure.ANDROID_ID);
      if(androidId==null || androidId.isEmpty()) throw new IllegalStateException("No Android ID");
      byte[] hash=MessageDigest.getInstance("SHA-256").digest(androidId.getBytes(StandardCharsets.UTF_8));
      StringBuilder id=new StringBuilder(16);
      for(int i=0;i<8;i++) id.append(String.format(Locale.US,"%02X",hash[i]&255));
      return id.toString();
    }catch(Exception e){return "UNAVAILABLE";}
  }

  static boolean valid(Activity activity,String entered){
    try{
      String code=entered.replaceAll("\\s+","");
      String[] parts=code.split("\\.",-1);
      if(parts.length!=3 || !"M6L1".equals(parts[0]) || parts[1].length()>2048 || parts[2].length()>1024)return false;
      byte[] payload=Base64.decode(parts[1],Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING);
      JSONObject data=new JSONObject(new String(payload,StandardCharsets.UTF_8));
      if(data.getInt("v")!=1 || !"lifetime".equals(data.getString("type")) || !deviceId(activity).equals(data.getString("device")))return false;
      byte[] der=Base64.decode(PUBLIC_KEY,Base64.DEFAULT);
      PublicKey publicKey=KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
      Signature verifier=Signature.getInstance("SHA256withRSA");
      verifier.initVerify(publicKey);
      verifier.update(parts[1].getBytes(StandardCharsets.US_ASCII));
      return verifier.verify(Base64.decode(parts[2],Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING));
    }catch(Exception e){return false;}
  }

  static void enforce(Activity activity){
    String saved=activity.getSharedPreferences(STORE,Context.MODE_PRIVATE).getString(CODE,"");
    if(valid(activity,saved))return;
    String id=deviceId(activity);
    LinearLayout body=new LinearLayout(activity);
    body.setOrientation(LinearLayout.VERTICAL);
    int pad=(int)(20*activity.getResources().getDisplayMetrics().density);
    body.setPadding(pad,pad,pad,pad);
    TextView instructions=new TextView(activity);
    instructions.setText("Для активации отправьте этот Device ID продавцу. Затем введите полученный бессрочный ключ:");
    instructions.setTextSize(15);
    body.addView(instructions);
    TextView device=new TextView(activity);
    device.setText(id);device.setTextSize(22);device.setTextIsSelectable(true);
    device.setPadding(0,pad/2,0,pad/2);
    body.addView(device);
    Button copy=new Button(activity);
    copy.setText("КОПИРОВАТЬ DEVICE ID");
    copy.setOnClickListener(v->{
      ((ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Mazda Device ID",id));
      Toast.makeText(activity,"Device ID скопирован",Toast.LENGTH_SHORT).show();
    });
    body.addView(copy);
    EditText input=new EditText(activity);
    input.setHint("M6L1. ...");
    input.setSingleLine(false);
    input.setMinLines(2);
    input.setMaxLines(5);
    body.addView(input,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
    AlertDialog dialog=new AlertDialog.Builder(activity)
      .setTitle("Активация Mazda 6 GH Studio")
      .setView(body)
      .setPositiveButton("АКТИВИРОВАТЬ",null)
      .setNegativeButton("ВЫХОД",(d,w)->activity.finish())
      .setCancelable(false).create();
    dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
      String candidate=input.getText().toString().replaceAll("\\s+","");
      if(!valid(activity,candidate)){
        input.setError("Неверный ключ или ключ от другого телефона");
        Toast.makeText(activity,"Лицензия не подтверждена",Toast.LENGTH_LONG).show();
        return;
      }
      boolean stored=activity.getSharedPreferences(STORE,Context.MODE_PRIVATE).edit().putString(CODE,candidate).commit();
      if(!stored){Toast.makeText(activity,"Ошибка сохранения лицензии",Toast.LENGTH_LONG).show();return;}
      dialog.dismiss();
      Toast.makeText(activity,"Бессрочная лицензия активирована",Toast.LENGTH_LONG).show();
    }));
    dialog.show();
  }
}
