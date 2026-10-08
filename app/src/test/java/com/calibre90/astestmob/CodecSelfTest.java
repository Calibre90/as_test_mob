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
    Map<String,String> allModules=new HashMap<>();
    allModules.put("720","IC");allModules.put("726","BCM");allModules.put("731","RKE");allModules.put("760","ABS");
    List<AbtCodec.Row> allRows=new ArrayList<>();
    String[] prefixes={"720","726","731","760"};
    String[] moduleNames={"IC","BCM","RKE","ABS"};
    for(int i=0;i<prefixes.length;i++){
      String address=prefixes[i]+"-01-01";
      String payload="000E F255 E700";
      String valid=AbtCodec.recalc(address,payload);
      AbtCodec.Row row=new AbtCodec.Row(moduleNames[i],address,valid,1);
      allRows.add(row);
      String single=AbtCodec.write(Arrays.asList(row),moduleNames[i]);
      List<AbtCodec.Row> parsedSingle=AbtCodec.parse(single,allModules);
      check(parsedSingle.size()==1&&parsedSingle.get(0).module.equals(moduleNames[i]),"module parse "+moduleNames[i]);
      check(AbtCodec.norm(parsedSingle.get(0).value).equals(AbtCodec.norm(valid)),"checksum roundtrip "+moduleNames[i]);
      FeatureEngine local=new FeatureEngine();
      FeatureEngine.Feature feature=new FeatureEngine.Feature("test"+i,moduleNames[i],address,"HEX","0","A",0);
      String initial=row.value;
      local.apply(row,feature,true,Arrays.asList(feature));
      Set<Integer> changed=FeatureEngine.changedPositions(initial,row.value);
      check(changed.contains(0),"highlight modified nibble "+moduleNames[i]);
      check(changed.size()>=1,"highlight changed digits "+moduleNames[i]);
      check(AbtCodec.norm(row.value).endsWith(AbtCodec.checksum(address,AbtCodec.norm(row.value).substring(0,AbtCodec.norm(row.value).length()-2))),"recalculated checksum "+moduleNames[i]);
      local.apply(row,feature,false,Arrays.asList(feature));
      check(row.value.equals(initial),"restore original row "+moduleNames[i]);
    }
    String combinedAbt=AbtCodec.write(allRows,"IC")+AbtCodec.write(allRows,"BCM")+AbtCodec.write(allRows,"RKE")+AbtCodec.write(allRows,"ABS");
    check(AbtCodec.parse(combinedAbt,allModules).size()==4,"all module rows survive serialization");
    for(String moduleName:moduleNames){
      List<AbtCodec.Row> selected=new ArrayList<>();
      for(AbtCodec.Row row:AbtCodec.parse(combinedAbt,allModules))if(row.module.equals(moduleName))selected.add(row);
      check(selected.size()==1,"filter only selected module "+moduleName);
      check(AbtCodec.parse(AbtCodec.write(selected,moduleName),allModules).size()==1,"save only selected module "+moduleName);
    }
    String crOnly=";Block 1\r720G1G11F407126809F\r";
    check(AbtCodec.parse(crOnly,modules).size()==1,"classic Mac CR line endings");
    String mixed=";Block 1\n720-G1-G1: 1F40 7126 809F\r726-G1-G1 = 000E F255 E766\r\n";
    check(AbtCodec.parse(mixed,allModules).size()==2,"mixed delimiters and line endings");
    check(AbtCodec.parse("720G1G1A0\n",modules).isEmpty(),"reject one-byte rows");
    check(AbtCodec.parse("; comment\n# comment\ninvalid\n",modules).isEmpty(),"ignore comments and invalid lines");
    check(AbtCodec.parseChecked(mixed,allModules).size()==2,"strict parser accepts valid mixed ABT");
    boolean malformedRejected=false;
    try{AbtCodec.parseChecked("720G1G11F407126809F\n720G1G1ZZZZ\n",allModules);}
    catch(IllegalArgumentException ex){malformedRejected=ex.getMessage().contains("2");}
    check(malformedRejected,"strict parser reports malformed line number");
    String validChecksum=AbtCodec.recalc("720-01-01","1F40 7126 8000");
    check(AbtCodec.checksumValid("720-01-01",validChecksum),"checksum valid");
    String corrupted=AbtCodec.norm(validChecksum);
    corrupted=corrupted.substring(0,corrupted.length()-2)+"00";
    if(corrupted.equals(AbtCodec.norm(validChecksum)))corrupted=corrupted.substring(0,corrupted.length()-2)+"FF";
    check(!AbtCodec.checksumValid("720-01-01",corrupted),"checksum mismatch detected");
    check(AbtCodec.norm(corrupted).endsWith(corrupted.substring(corrupted.length()-2)),"original corrupt value preserved");
    ArrayList<AbtCodec.Row> initial=new ArrayList<>();
    initial.add(new AbtCodec.Row("IC","720-01-01","1F40 7126 809F",1));
    initial.add(new AbtCodec.Row("IC","720-01-02","000E F255 E766",1));
    AbtSnapshot snapshot=new AbtSnapshot(initial);
    initial.get(0).value="FFFF FFFF FFFF";
    initial.remove(1);
    initial.add(new AbtCodec.Row("IC","720-01-03","1234 5678 90AB",1));
    check(Arrays.equals(snapshot.differences(initial),new int[]{1,1,1}),"snapshot counts edited added removed");
    ArrayList<AbtCodec.Row> restored=snapshot.restore();
    check(restored.size()==2&&restored.get(0).address.equals("720-01-01")&&restored.get(1).address.equals("720-01-02"),"snapshot restores original row order and membership");
    check(restored.get(0).value.equals("1F40 7126 809F"),"snapshot restores original HEX");
    restored.get(0).value="0000";
    check(snapshot.restore().get(0).value.equals("1F40 7126 809F"),"snapshot remains immutable across restores");
    check(Arrays.equals(snapshot.differences(snapshot.restore()),new int[]{0,0,0}),"snapshot clean after restore");
    String nonstandard="720G1G11F4071268000\r\n";
    List<AbtCodec.Row> checksumRows=AbtCodec.parse(nonstandard,modules);
    check(checksumRows.size()==1,"nonstandard checksum row parsed");
    check(AbtCodec.write(checksumRows,"IC").contains("720G1G11F4071268000"),"export preserves unedited checksum bytes");
    String expectedChanged=AbtCodec.recalc("720-01-01","2B4071268000");
    checksumRows.get(0).value=expectedChanged;
    check(AbtCodec.write(checksumRows,"IC").contains("720G1G1"+AbtCodec.norm(expectedChanged)),"export writes already recalculated edited row");
    List<AbtCodec.Row> cycle=AbtCodec.parse("720G1G11F407126809F\r\n720G1G2000EF255E766\r\n",modules);
    AbtSnapshot cycleSnapshot=new AbtSnapshot(cycle);
    FeatureEngine cycleEngine=new FeatureEngine();
    FeatureEngine.Feature cycleFeature=new FeatureEngine.Feature("cycle-keyless","IC","720-01-01","HEX","0,1","2B",0);
    cycleEngine.apply(cycle.get(0),cycleFeature,true,Arrays.asList(cycleFeature));
    String modifiedHex=AbtCodec.norm(cycle.get(0).value);
    check(modifiedHex.startsWith("2B"),"full cycle feature modifies HEX");
    String cycleExport=AbtCodec.write(cycle,"IC");
    List<AbtCodec.Row> cycleReopened=AbtCodec.parseChecked(cycleExport,modules);
    check(cycleReopened.size()==2,"full cycle reimport row count");
    check(AbtCodec.norm(cycleReopened.get(0).value).equals(modifiedHex),"full cycle reimport retains modified HEX");
    check(Arrays.equals(cycleSnapshot.differences(cycleReopened),new int[]{1,0,0}),"full cycle snapshot detects modification");
    List<AbtCodec.Row> cycleRestored=cycleSnapshot.restore();
    check(AbtCodec.norm(cycleRestored.get(0).value).equals("1F407126809F"),"full cycle restores original HEX");
    check(Arrays.equals(cycleSnapshot.differences(cycleRestored),new int[]{0,0,0}),"full cycle restored clean");
    List<AbtCodec.Row> icOnly=AbtCodec.parseChecked("720G1G11F407126809F\r\n",modules);
    check(AbtCodec.requireModule(icOnly,"IC").size()==1,"IC file accepted in IC tab");
    boolean wrongModule=false;
    try{AbtCodec.requireModule(icOnly,"BCM");}
    catch(IllegalArgumentException ex){wrongModule=ex.getMessage().contains("IC")&&ex.getMessage().contains("BCM");}
    check(wrongModule,"IC file rejected in BCM tab");
    List<AbtCodec.Row> mixedModules=AbtCodec.parseChecked("720G1G11F407126809F\r\n726G1G1000EF255E766\r\n",modules);
    boolean mixedRejected=false;
    try{AbtCodec.requireModule(mixedModules,"IC");}
    catch(IllegalArgumentException ex){mixedRejected=ex.getMessage().contains("BCM");}
    check(mixedRejected,"mixed IC BCM file rejected");
    check(AbtCodec.requireModule(AbtCodec.parseChecked("726G1G1000EF255E766\r\n",modules),"BCM").size()==1,"BCM file accepted in BCM tab");
    System.out.println("PASS: ABT parse, roundtrip, checksum, index, HEX toggle, restore");
  }
}
