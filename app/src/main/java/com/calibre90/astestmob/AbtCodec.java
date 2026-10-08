package com.calibre90.astestmob;

import java.util.*;
import java.util.regex.*;

/** FORScan ABT codec ported from the Run89 Python implementation. */
public final class AbtCodec {
  private AbtCodec(){}
  public static final class Row {
    public final String module,address;
    public String value;
    public final int block;
    public Row(String module,String address,String value,int block){
      this.module=module;this.address=address;this.value=spaced(norm(value));this.block=block;
    }
  }
  public static String norm(String s){return s==null?"":s.replaceAll("[^0-9A-Fa-f]","").toUpperCase(Locale.US);}
  public static String spaced(String s){StringBuilder b=new StringBuilder();for(int i=0;i<s.length();i+=4){if(i>0)b.append(' ');b.append(s.substring(i,Math.min(i+4,s.length())));}return b.toString();}
  public static int decodeIndex(String s){
    s=s.toUpperCase(Locale.US);
    if(s.matches("[0-9]{2}"))return Integer.parseInt(s);
    if(!s.matches("[G-V][0-9A-F]"))throw new IllegalArgumentException("Bad ABT index: "+s);
    return ((s.charAt(0)-'G')<<4)+Character.digit(s.charAt(1),16);
  }
  public static String encodeIndex(int n){if(n<0||n>255)throw new IllegalArgumentException("Index range");return ""+(char)('G'+(n>>4))+Integer.toHexString(n&15).toUpperCase(Locale.US);}
  public static String checksum(String address,String data){
    String[] parts=address.split("-");
    int sum=Integer.parseInt(parts[0].substring(0,1),16)+Integer.parseInt(parts[0].substring(1),16)+Integer.parseInt(parts[1])+Integer.parseInt(parts[2]);
    String raw=norm(data);
    for(int i=0;i+1<raw.length();i+=2)sum+=Integer.parseInt(raw.substring(i,i+2),16);
    return String.format(Locale.US,"%02X",sum&255);
  }
  public static String recalc(String address,String value){
    String raw=norm(value);if(raw.length()<4||raw.length()%2!=0)return spaced(raw);
    String payload=raw.substring(0,raw.length()-2);
    return spaced(payload+checksum(address,payload));
  }
  private static final Pattern ROW=Pattern.compile("^([0-9A-F]{3})[- ]?([0-9A-Z]{2})[- ]?([0-9A-Z]{2})\\s+([0-9A-F][0-9A-F ]*)$");
  private static final Pattern COMPACT=Pattern.compile("^([0-9A-F]{3})([0-9A-Z]{2})([0-9A-Z]{2})([0-9A-F]+)$");
  private static final Pattern BLOCK=Pattern.compile("^;\\s*Block\\s+(\\d+)",Pattern.CASE_INSENSITIVE);
  public static List<Row> parse(String content,Map<String,String> modules){
    ArrayList<Row> out=new ArrayList<>();int current=-1;
    for(String source:content.split("\\r\\n|\\n|\\r")){
      String line=source.trim().replace("\uFEFF","").toUpperCase(Locale.US);
      Matcher bm=BLOCK.matcher(line);if(bm.find()){current=Integer.parseInt(bm.group(1));continue;}
      if(line.isEmpty()||line.startsWith(";")||line.startsWith("#")||line.startsWith("//"))continue;
      String clean=line.replaceAll("[,:=\\t]+"," ").replaceAll("\\s+"," ").trim();
      Matcher m=ROW.matcher(clean);if(!m.matches()){m=COMPACT.matcher(clean);if(!m.matches())continue;}
      try {
        int b=decodeIndex(m.group(2)),l=decodeIndex(m.group(3));
        String raw=norm(m.group(4));if(raw.length()<4||raw.length()%2!=0)continue;
        String prefix=m.group(1);
        out.add(new Row(modules.containsKey(prefix)?modules.get(prefix):"OTHER",String.format(Locale.US,"%s-%02d-%02d",prefix,b,l),raw,current>=0?current:b));
      }catch(RuntimeException ignored){}
    }
    return out;
  }
  public static String write(List<Row> rows,String module){
    TreeMap<Integer,List<Row>> grouped=new TreeMap<>();
    for(Row row:rows)if(row.module.equals(module))grouped.computeIfAbsent(row.block,k->new ArrayList<>()).add(row);
    StringBuilder out=new StringBuilder();
    for(Map.Entry<Integer,List<Row>> group:grouped.entrySet()){
      out.append(";Block ").append(group.getKey()).append("\r\n");
      group.getValue().sort(Comparator.comparing(r->r.address));
      for(Row row:group.getValue()){
        String[] parts=row.address.split("-");
        String value=recalc(row.address,row.value);
        out.append(parts[0]).append(encodeIndex(Integer.parseInt(parts[1]))).append(encodeIndex(Integer.parseInt(parts[2]))).append(norm(value)).append("\r\n");
      }
    }
    return out.toString();
  }
}
