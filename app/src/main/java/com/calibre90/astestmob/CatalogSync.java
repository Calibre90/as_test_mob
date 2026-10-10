package com.calibre90.astestmob;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Optional HTTPS catalog download; the last valid catalog remains available offline. */
final class CatalogSync {
  // Must be configured to an owner-controlled HTTPS published JSON URL before release.
  private static final String CATALOG_URL = "";
  private static final int MAX_BYTES = 256 * 1024;
  private CatalogSync(){}

  static void refresh(Context context,Runnable onUpdated){
    if(CATALOG_URL.isEmpty())return;
    Context app=context.getApplicationContext();
    new Thread(()->{
      HttpURLConnection connection=null;
      try{
        URL url=new URL(CATALOG_URL);
        if(!"https".equalsIgnoreCase(url.getProtocol()))return;
        connection=(HttpURLConnection)url.openConnection();
        connection.setConnectTimeout(7000);
        connection.setReadTimeout(7000);
        connection.setInstanceFollowRedirects(false);
        if(connection.getResponseCode()!=200)return;
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        try(InputStream in=connection.getInputStream()){
          byte[] buffer=new byte[4096];int count;
          while((count=in.read(buffer))!=-1){
            if(output.size()+count>MAX_BYTES)return;
            output.write(buffer,0,count);
          }
        }
        JSONObject document=new JSONObject(new String(output.toByteArray(),StandardCharsets.UTF_8));
        if(!CatalogSignature.verify(document))return; // Untrusted catalog must never reach local storage.
        if(document.getInt("schema")!=1)return;
        int version=document.getInt("version");
        if(version<1)return;
        JSONArray features=new JSONArray(document.getString("features_payload"));
        if(features.length()==0||features.length()>500)return; // Never replace built-ins with an empty draft catalog.
        java.util.HashSet<String> seenIds=new java.util.HashSet<>();
        for(int i=0;i<features.length();i++){
          JSONObject f=features.getJSONObject(i);
          String id=f.getString("id"),module=f.getString("module");
          String address=f.getString("row"),mode=f.getString("mode");
          String indices=f.getString("indices"),on=f.getString("on");
          if(id.isEmpty()||id.length()>80||!seenIds.add(id)||!(module.equals("IC")||module.equals("BCM")||module.equals("RKE")||module.equals("ABS"))
            ||!address.matches("[0-9A-Fa-f]{3}-[0-9A-Fa-f]{2}-[0-9A-Fa-f]{2}")
            ||!(mode.equals("HEX")||mode.equals("BITS"))||indices.length()>80
            ||!indices.matches("[0-9]+(,[0-9]+)*")||!on.matches("[0-9A-Fa-f]{1,64}"))return;
          String off=f.getString("off");
          if(!off.isEmpty()&&!off.matches("[0-9A-Fa-f]{1,64}"))return;
          if(mode.equals("HEX") && on.length()!=indices.split(",").length)return;
          if(mode.equals("HEX") && !off.isEmpty() && off.length()!=indices.split(",").length)return;
          if(!f.has("byte")||f.getInt("byte")<0||f.getInt("byte")>63)return;
          if(!f.has("label")||f.getString("label").trim().isEmpty()||f.getString("label").length()>120)return;
          String prefix=module.equals("IC")?"720":module.equals("BCM")?"726":module.equals("RKE")?"731":"760";
          if(!address.toUpperCase(java.util.Locale.US).startsWith(prefix+"-"))return;
          String[] positions=indices.split(",");
          java.util.HashSet<Integer> seenPositions=new java.util.HashSet<>();
          for(String position:positions){int n=Integer.parseInt(position);
            if(n>127||(mode.equals("BITS")&&n>7)||!seenPositions.add(n))return;
          }
        }
        int current=app.getSharedPreferences("studio_published_catalog",0).getInt("version",0);
        if(version<=current)return;
        boolean saved=app.getSharedPreferences("studio_published_catalog",0).edit()
          .putInt("version",version).putString("features",features.toString()).commit();
        if(saved && onUpdated!=null)new Handler(Looper.getMainLooper()).post(onUpdated);
      }catch(Exception ignored){
        // Network failures must never prevent offline editing.
      }finally{if(connection!=null)connection.disconnect();}
    },"MazdaCatalogSync").start();
  }
}
