package com.calibre90.astestmob;
import android.content.Context;
import org.json.*;
import java.util.*;
/** Local Run89-style editable feature settings. */
public final class StudioSettings {
  private static final String PREF="run35_settings";
  public static ArrayList<FeatureEngine.Feature> load(Context ctx){
    String data=ctx.getSharedPreferences(PREF,0).getString("features","");
    if(data.isEmpty())return defaults();
    try{
      JSONArray a=new JSONArray(data);ArrayList<FeatureEngine.Feature> out=new ArrayList<>();
      for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);
        out.add(new FeatureEngine.Feature(o.getString("id"),o.getString("module"),o.getString("row"),o.getString("mode"),o.getString("indices"),o.getString("on"),o.optString("off",""),o.optInt("byte",0)));
      }return out;
    }catch(Exception e){return defaults();}
  }
  public static ArrayList<FeatureEngine.Feature> defaults(){
    ArrayList<FeatureEngine.Feature> out=new ArrayList<>();
    out.add(new FeatureEngine.Feature("rvm","IC","720-01-02","HEX","0","8","0",0));
    out.add(new FeatureEngine.Feature("new_feature","IC","720-01-01","HEX","2,3","40","00",0));
    out.add(new FeatureEngine.Feature("keyless","IC","720-01-01","HEX","0,1","2B","1F",0));
    return out;
  }
  public static void save(Context ctx,List<FeatureEngine.Feature> list){
    JSONArray a=new JSONArray();
    try{
      for(FeatureEngine.Feature f:list){
        JSONObject o=new JSONObject();o.put("id",f.id);o.put("module",f.module);o.put("row",f.address);
        o.put("mode",f.mode);o.put("indices",f.indices);o.put("on",f.on);o.put("off",f.off);o.put("byte",f.byteIndex);a.put(o);
      }
    }catch(JSONException e){throw new IllegalStateException(e);}
    if(!ctx.getSharedPreferences(PREF,0).edit().putString("features",a.toString()).commit())throw new IllegalStateException("Settings not saved");
  }
}
