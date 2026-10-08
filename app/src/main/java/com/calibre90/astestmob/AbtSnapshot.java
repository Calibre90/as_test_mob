package com.calibre90.astestmob;
import java.util.*;
/** Immutable-by-copy snapshot of the imported rows for a single module. */
public final class AbtSnapshot {
  private final ArrayList<AbtCodec.Row> saved=new ArrayList<>();
  public AbtSnapshot(List<AbtCodec.Row> source){
    for(AbtCodec.Row r:source)saved.add(new AbtCodec.Row(r.module,r.address,r.value,r.block));
  }
  public ArrayList<AbtCodec.Row> restore(){
    ArrayList<AbtCodec.Row> result=new ArrayList<>();
    for(AbtCodec.Row r:saved)result.add(new AbtCodec.Row(r.module,r.address,r.value,r.block));
    return result;
  }
  public int[] differences(List<AbtCodec.Row> current){
    Map<String,String> values=new HashMap<>();
    for(AbtCodec.Row r:current)values.put(r.address,AbtCodec.norm(r.value));
    int changed=0,removed=0;
    for(AbtCodec.Row r:saved){
      String v=values.remove(r.address);
      if(v==null)removed++;
      else if(!v.equals(AbtCodec.norm(r.value)))changed++;
    }
    return new int[]{changed,values.size(),removed};
  }
}
