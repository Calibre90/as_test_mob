package com.calibre90.astestmob;
import java.util.*;
public final class CodecSelfTest {
  private static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
  public static void main(String[] args){
    Map<String,String> modules=new HashMap<>();modules.put("720","IC");modules.put("726","BCM");
    List<AbtCodec.Row> rows=AbtCodec.parse(";Block 1\r\n720G1G11F407126809F\r\n720-G1-G2 000E F255 E766\r\n",modules);
    check(rows.size()==2,"compact and spaced parse");
    check(rows.get(0).address.equals("720-01-01"),"first address");
    check(rows.get(1).address.equals("720-01-02"),"second address");
    check(rows.get(0).value.equals("1F40 7126 809F"),"data grouping");
    for(int n:new int[]{0,1,15,16,31,255})check(AbtCodec.decodeIndex(AbtCodec.encodeIndex(n))==n,"index "+n);
    String serialized=AbtCodec.write(rows,"IC");
    check(AbtCodec.parse(serialized,modules).size()==2,"round trip");
    check(serialized.contains(";Block 1"),"block grouping");
    check(FeatureEngine.indices("0,2-4;7").equals(Arrays.asList(0,2,3,4,7)),"index ranges");
    FeatureEngine engine=new FeatureEngine();
    FeatureEngine.Feature f=new FeatureEngine.Feature("keyless","IC","720-01-01","HEX","0,1","2B",0);
    String before=rows.get(0).value;
    engine.apply(rows.get(0),f,true,Arrays.asList(f));
    check(AbtCodec.norm(rows.get(0).value).startsWith("2B"),"feature ON");
    check(!FeatureEngine.changedPositions(before,rows.get(0).value).isEmpty(),"changed positions");
    engine.apply(rows.get(0),f,false,Arrays.asList(f));
    check(rows.get(0).value.equals(before),"feature OFF restores");
    List<AbtCodec.Row> bcm=AbtCodec.parse("726G1G10000",modules);
    check(bcm.size()==1&&bcm.get(0).module.equals("BCM"),"BCM module detection");
    FeatureEngine seeded=new FeatureEngine();
    AbtCodec.Row alreadyOn=new AbtCodec.Row("IC","720-01-01",AbtCodec.recalc("720-01-01","2B4071268000"),1);
    FeatureEngine.Feature toggle=new FeatureEngine.Feature("keyless","IC","720-01-01","HEX","0,1","2B","1F",0);
    seeded.seed(toggle,true);
    seeded.apply(alreadyOn,toggle,false,Arrays.asList(toggle));
    check(AbtCodec.norm(alreadyOn.value).startsWith("1F"),"initially ON -> explicit OFF");
    FeatureEngine shared=new FeatureEngine();
    AbtCodec.Row combined=new AbtCodec.Row("IC","720-01-01",AbtCodec.recalc("720-01-01","2B4071268000"),1);
    FeatureEngine.Feature keyless=new FeatureEngine.Feature("keyless","IC","720-01-01","HEX","0,1","2B","1F",0);
    FeatureEngine.Feature second=new FeatureEngine.Feature("second","IC","720-01-01","HEX","2,3","40","00",0);
    shared.seed(keyless,true);shared.seed(second,true);
    shared.apply(combined,keyless,false,Arrays.asList(keyless,second));
    check(AbtCodec.norm(combined.value).startsWith("1F40"),"OFF preserves another active feature");
    shared.apply(combined,second,false,Arrays.asList(keyless,second));
    check(AbtCodec.norm(combined.value).startsWith("1F00"),"OFF override persists after toggling second feature");
    shared.apply(combined,second,true,Arrays.asList(keyless,second));
    check(AbtCodec.norm(combined.value).startsWith("1F40"),"second feature ON preserves first OFF");
    shared.apply(combined,keyless,true,Arrays.asList(keyless,second));
    check(AbtCodec.norm(combined.value).startsWith("2B40"),"ON restores feature without clearing another");
    FeatureEngine invalid=new FeatureEngine();
    FeatureEngine.Feature bad=new FeatureEngine.Feature("bad","IC","720-01-01","HEX","99","A",0);
    String untouched=alreadyOn.value;boolean thrown=false;
    try{invalid.apply(alreadyOn,bad,true,Arrays.asList(bad));}catch(IllegalArgumentException ex){thrown=true;}
    check(thrown&&alreadyOn.value.equals(untouched),"failed apply leaves row intact");
    System.out.println("PASS: ABT parse, roundtrip, checksum, index, HEX toggle, restore");
  }
}
