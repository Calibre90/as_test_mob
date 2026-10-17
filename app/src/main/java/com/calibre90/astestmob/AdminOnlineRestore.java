package com.calibre90.astestmob;

import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Restore only owner-signed published admin configuration, never ABT or licenses. */
final class AdminOnlineRestore {
  private static final String URL_STRING =
      "https://raw.githubusercontent.com/Calibre90/as_test_mob/main/publication/studio-published-settings.json";
  private AdminOnlineRestore() {}
  static FullCatalogEnvelope download(String token) throws Exception {
    if(token==null||token.trim().isEmpty())return download();
    String api="https://api.github.com/repos/Calibre90/as_test_mob/contents/publication/studio-published-settings.json?ref=main";
    HttpURLConnection connection=(HttpURLConnection)new URL(api).openConnection();
    connection.setConnectTimeout(10000);
    connection.setReadTimeout(10000);
    connection.setRequestProperty("Authorization","Bearer "+token.trim());
    connection.setRequestProperty("Accept","application/vnd.github+json");
    connection.setRequestProperty("X-GitHub-Api-Version","2022-11-28");
    try{
      int status=connection.getResponseCode();
      if(status!=200)throw new IllegalStateException("Чтение опубликованных настроек через GitHub API: HTTP "+status+
          " (проверьте доступ токена к as_test_mob)");
      ByteArrayOutputStream buffer=new ByteArrayOutputStream();
      try(InputStream input=connection.getInputStream()){
        byte[] chunk=new byte[4096];int n;
        while((n=input.read(chunk))!=-1){
          if(buffer.size()+n>1024*1024)throw new IllegalStateException("Слишком большой ответ GitHub");
          buffer.write(chunk,0,n);
        }
      }
      JSONObject file=new JSONObject(new String(buffer.toByteArray(),StandardCharsets.UTF_8));
      String encoded=file.getString("content").replace("\\n","").replace("\\r","");
      byte[] raw=android.util.Base64.decode(encoded,android.util.Base64.DEFAULT);
      if(raw.length>512*1024)throw new IllegalStateException("Слишком большой каталог");
      JSONObject document=new JSONObject(new String(raw,StandardCharsets.UTF_8));
      if(!CatalogSignature.verifyFull(document))throw new SecurityException("Подпись каталога не прошла проверку");
      return FullCatalogEnvelope.parse(document);
    }finally{connection.disconnect();}
  }
  static FullCatalogEnvelope download() throws Exception {
    HttpURLConnection connection=(HttpURLConnection)new URL(URL_STRING).openConnection();
    connection.setConnectTimeout(10000);
    connection.setReadTimeout(10000);
    connection.setInstanceFollowRedirects(false);
    try {
      if(connection.getResponseCode()!=200)
        throw new IllegalStateException("GitHub HTTP "+connection.getResponseCode());
      ByteArrayOutputStream buffer=new ByteArrayOutputStream();
      try(InputStream input=connection.getInputStream()){
        byte[] chunk=new byte[4096];
        int n;
        while((n=input.read(chunk))!=-1){
          if(buffer.size()+n>512*1024)throw new IllegalStateException("Слишком большой каталог");
          buffer.write(chunk,0,n);
        }
      }
      JSONObject document=new JSONObject(new String(buffer.toByteArray(),StandardCharsets.UTF_8));
      if(!CatalogSignature.verifyFull(document))
        throw new SecurityException("Подпись каталога не прошла проверку");
      return FullCatalogEnvelope.parse(document);
    }finally{connection.disconnect();}
  }
}
