package com.calibre90.astestmob;

import java.util.*;

/** Feature application logic based on Run89: multiple active features share a baseline. */
public final class FeatureEngine {
  public static final class Feature {
    public final String id,module,address,mode,indices,on,off;
    public final int byteIndex;
    public Feature(String id,String module,String address,String mode,String indices,String on,int byteIndex){
      this(id,module,address,mode,indices,on,"",byteIndex);
    }
    public Feature(String id,String module,String address,String mode,String indices,String on,String off,int byteIndex){
      this.id=id;this.module=module;this.address=address;this.mode=mode;this.indices=indices;this.on=on;this.off=off;this.byteIndex=byteIndex;
    }
  }
  private final Map<String,String> baselines=new HashMap<>();
  private final Set<String> enabled=new HashSet<>();
  private final Set<String> initiallyEnabled=new HashSet<>();
  private String featureKey(Feature f){return f.module+"|"+f.address+"|"+f.id;}
  public void seed(Feature f,boolean on){if(on)initiallyEnabled.add(featureKey(f));}
  public static List<Integer> indices(String text){
    ArrayList<Integer> out=new ArrayList<>();
    for(String p:text.trim().split("[,;\\s]+")){
      if(p.isEmpty())continue;
      if(p.contains("-")){
        String[] pair=p.split("-",2);int a=Integer.parseInt(pair[0]),b=Integer.parseInt(pair[1]);
        for(int n=Math.min(a,b);n<=Math.max(a,b);n++)out.add(n);
      }else out.add(Integer.parseInt(p));
    }
    return out;
  }
  public boolean state(Feature f,String value){
    String raw=AbtCodec.norm(value);
    try{
      if("BITS".equalsIgnoreCase(f.mode)){
        List<Integer> bits=indices(f.indices);int b=Integer.parseInt(raw.substring(f.byteIndex*2,f.byteIndex*2+2),16);
        if(bits.isEmpty())return false;for(int bit:bits)if((b&(1<<bit))==0)return false;return true;
      }
      List<Integer> idx=indices(f.indices);String target=AbtCodec.norm(f.on);
      if(idx.isEmpty()||idx.size()!=target.length())return false;
      for(int i=0;i<idx.size();i++)if(raw.charAt(idx.get(i))!=target.charAt(i))return false;
      return true;
    }catch(RuntimeException ex){return false;}
  }
  public String apply(AbtCodec.Row row,Feature changed,boolean active,List<Feature> features){
    String key=row.module+"|"+row.address;
    if(!baselines.containsKey(key))baselines.put(key,AbtCodec.norm(row.value));
    String changedKey=featureKey(changed);
    Set<String> next=new HashSet<>(enabled);
    if(active)next.add(changedKey);else next.remove(changedKey);
    char[] raw=baselines.get(key).toCharArray();boolean any=false;
    for(Feature f:features){
      if(!next.contains(featureKey(f))||!f.module.equals(row.module)||!f.address.equals(row.address))continue;
      any=true;
      if("BITS".equalsIgnoreCase(f.mode)){
        int p=f.byteIndex*2;if(p<0||p+2>raw.length)throw new IllegalArgumentException("Byte out of range");
        int val=Integer.parseInt(new String(raw,p,2),16);
        for(int bit:indices(f.indices)){if(bit<0||bit>7)throw new IllegalArgumentException("Bit out of range");val|=1<<bit;}
        String hex=String.format(Locale.US,"%02X",val);raw[p]=hex.charAt(0);raw[p+1]=hex.charAt(1);
      }else{
        List<Integer> idx=indices(f.indices);String target=AbtCodec.norm(f.on);
        if(idx.size()!=target.length())throw new IllegalArgumentException("HEX target/index mismatch");
        for(int i=0;i<idx.size();i++){int p=idx.get(i);if(p<0||p>=raw.length)throw new IllegalArgumentException("HEX index out of range");raw[p]=target.charAt(i);}
      }
    }
    if(!active&&initiallyEnabled.contains(changedKey)&&!changed.off.isEmpty()){
      if("BITS".equalsIgnoreCase(changed.mode)){
        int p=changed.byteIndex*2;if(p<0||p+2>raw.length)throw new IllegalArgumentException("Byte out of range");
        int val=Integer.parseInt(new String(raw,p,2),16);
        for(int bit:indices(changed.indices)){if(bit<0||bit>7)throw new IllegalArgumentException("Bit out of range");val&=~(1<<bit);}
        String hex=String.format(Locale.US,"%02X",val);raw[p]=hex.charAt(0);raw[p+1]=hex.charAt(1);
      }else{
        List<Integer> idx=indices(changed.indices);String off=AbtCodec.norm(changed.off);
        if(idx.size()!=off.length())throw new IllegalArgumentException("OFF target/index mismatch");
        for(int i=0;i<idx.size();i++){int p=idx.get(i);if(p<0||p>=raw.length)throw new IllegalArgumentException("OFF index out of range");raw[p]=off.charAt(i);}
      }
      any=true;
    }
    String result=any?AbtCodec.recalc(row.address,new String(raw)):AbtCodec.spaced(baselines.get(key));
    row.value=result;enabled.clear();enabled.addAll(next);
    return row.value;
  }
  public static Set<Integer> changedPositions(String original,String current){
    String a=AbtCodec.norm(original),b=AbtCodec.norm(current);Set<Integer> changed=new HashSet<>();
    for(int i=0;i<Math.max(a.length(),b.length());i++)if(i>=a.length()||i>=b.length()||a.charAt(i)!=b.charAt(i))changed.add(i);
    return changed;
  }
  public void reset(){baselines.clear();enabled.clear();initiallyEnabled.clear();}
}
