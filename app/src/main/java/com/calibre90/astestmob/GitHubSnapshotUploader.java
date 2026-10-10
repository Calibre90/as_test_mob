package com.calibre90.astestmob;

import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import android.util.Base64;

/** Uploads a validated Admin snapshot to a review branch. No token is persisted. */
final class GitHubSnapshotUploader {
  static final String BRANCH="feature/studio-catalog-integration";
  static final String API="https://api.github.com/repos/Calibre90/as_test_mob/contents/publication/admin-settings.json";
  private GitHubSnapshotUploader(){}

  static String upload(JSONObject snapshot, String token) throws Exception {
    AdminSnapshot.validate(snapshot);
    if(token==null||token.trim().length()<12)throw new IOException("Нужен GitHub-токен с правом Contents: Read and write");
    String existingSha=null;
    HttpURLConnection get=connect(API+"?ref="+URLEncoder.encode(BRANCH,"UTF-8"),"GET",token);
    try{
      int status=get.getResponseCode();
      if(status==200)existingSha=readJson(get).getString("sha");
      else if(status!=404)throw new IOException("GitHub: HTTP "+status+" при чтении файла");
    }finally{get.disconnect();}
    JSONObject payload=new JSONObject();
    payload.put("message","Update Admin Studio snapshot for review");
    payload.put("branch",BRANCH);
    payload.put("content",Base64.encodeToString(snapshot.toString(2).getBytes(StandardCharsets.UTF_8),Base64.NO_WRAP));
    if(existingSha!=null)payload.put("sha",existingSha);
    HttpURLConnection put=connect(API,"PUT",token);
    try{
      put.setDoOutput(true);
      put.setRequestProperty("Content-Type","application/json");
      byte[] bytes=payload.toString().getBytes(StandardCharsets.UTF_8);
      try(OutputStream out=put.getOutputStream()){out.write(bytes);}
      int status=put.getResponseCode();
      if(status!=200&&status!=201)throw new IOException("GitHub: HTTP "+status+" при загрузке (проверь права токена и ветку)");
      return readJson(put).getJSONObject("commit").getString("sha");
    }finally{put.disconnect();}
  }
  /** Request the protected release workflow. A successful 204 means queued, not published. */
  static void requestRelease(String token, String snapshotSha, int version) throws Exception {
    if(snapshotSha==null||!snapshotSha.matches("[0-9a-fA-F]{40}"))throw new IOException("Неверный SHA снимка");
    if(version<1)throw new IOException("Неверная версия каталога");
    JSONObject inputs=new JSONObject();
    inputs.put("snapshot_sha",snapshotSha);
    inputs.put("version",Integer.toString(version));
    JSONObject payload=new JSONObject();
    payload.put("ref","main");
    payload.put("inputs",inputs);
    HttpURLConnection post=connect("https://api.github.com/repos/Calibre90/as_test_mob/actions/workflows/studio-release.yml/dispatches","POST",token);
    try{
      post.setDoOutput(true);
      post.setRequestProperty("Content-Type","application/json");
      try(OutputStream out=post.getOutputStream()){
        out.write(payload.toString().getBytes(StandardCharsets.UTF_8));
      }
      int status=post.getResponseCode();
      if(status!=204)throw new IOException("GitHub: HTTP "+status+" при запуске публикации. Проверь workflow в main и Actions: write.");
    }finally{post.disconnect();}
  }

  private static HttpURLConnection connect(String url,String method,String token) throws Exception{
    HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
    c.setRequestMethod(method);
    c.setConnectTimeout(15000);
    c.setReadTimeout(20000);
    c.setRequestProperty("Accept","application/vnd.github+json");
    c.setRequestProperty("X-GitHub-Api-Version","2022-11-28");
    c.setRequestProperty("Authorization","Bearer "+token.trim());
    c.setUseCaches(false);
    return c;
  }
  private static JSONObject readJson(HttpURLConnection c) throws Exception{
    try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
      byte[] b=new byte[4096];int n;
      while((n=in.read(b))!=-1){if(out.size()+n>2*1024*1024)throw new IOException("Слишком большой ответ GitHub");out.write(b,0,n);}
      return new JSONObject(out.toString("UTF-8"));
    }
  }
}
