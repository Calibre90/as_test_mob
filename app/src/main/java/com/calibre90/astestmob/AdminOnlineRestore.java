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
