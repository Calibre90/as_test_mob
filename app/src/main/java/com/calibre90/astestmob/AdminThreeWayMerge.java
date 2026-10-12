package com.calibre90.astestmob;

import org.json.JSONObject;
import java.util.Iterator;

/** Merge only independently edited preference keys. Conflicts are never overwritten. */
final class AdminThreeWayMerge {
  private AdminThreeWayMerge(){}

  static JSONObject merge(JSONObject base, JSONObject local, JSONObject remote) throws Exception {
    AdminSnapshot.validate(base);
    AdminSnapshot.validate(local);
    AdminSnapshot.validate(remote);
    JSONObject merged=new JSONObject(remote.toString());
    JSONObject b=base.getJSONObject("stores"), l=local.getJSONObject("stores");
    JSONObject r=remote.getJSONObject("stores"), output=merged.getJSONObject("stores");
    Iterator<String> stores=l.keys();
    while(stores.hasNext()){
      String store=stores.next();
      JSONObject lv=l.getJSONObject(store);
      JSONObject bv=b.optJSONObject(store);
      JSONObject rv=r.optJSONObject(store);
      JSONObject dest=output.optJSONObject(store);
      if(dest==null)dest=new JSONObject();
      Iterator<String> keys=lv.keys();
      while(keys.hasNext()){
        String key=keys.next();
        Object localValue=lv.get(key);
        Object baseValue=bv==null?null:bv.opt(key);
        Object remoteValue=rv==null?null:rv.opt(key);
        if(equal(localValue,baseValue))continue;
        if(!equal(remoteValue,baseValue) && !equal(remoteValue,localValue))
          throw new IllegalStateException("Конфликт настроек: "+store+" / "+key+
            ". Сервер и админка изменили одно поле. Экспортируйте настройки и проверьте изменения.");
        dest.put(key,localValue);
      }
      // Also preserve intentional removal of keys relative to the baseline.
      if(bv!=null){
        Iterator<String> oldKeys=bv.keys();
        while(oldKeys.hasNext()){
          String key=oldKeys.next();
          if(lv.has(key))continue;
          Object remoteValue=rv==null?null:rv.opt(key);
          if(!equal(remoteValue,bv.opt(key)) && remoteValue!=null)
            throw new IllegalStateException("Конфликт удаления: "+store+" / "+key);
          dest.remove(key);
        }
      }
      output.put(store,dest);
    }
    AdminSnapshot.validate(merged);
    return merged;
  }
  private static boolean equal(Object a,Object b){
    if(a==null||a==JSONObject.NULL)return b==null||b==JSONObject.NULL;
    if(b==null||b==JSONObject.NULL)return false;
    return a.equals(b) || String.valueOf(a).equals(String.valueOf(b));
  }
}
