package com.calibre90.astestmob.obd;

import java.util.*;

/**
 * Safety state for a single ECU As-Built editing session.
 * No network writes occur in this class. Callers must verify ECU identity,
 * transport bus, complete read, backups, checksums and write/readback separately.
 */
public final class AsBuiltSafetySession {
  public enum State { DISCONNECTED, BUS_CONFIRMATION_REQUIRED, READING, READY, DIRTY, WRITING, VERIFYING, SUCCESS, FAILED }
  public enum Bus { HS_CAN, MS_CAN }
  private State state = State.DISCONNECTED;
  private String ecuId;
  private Bus requiredBus;
  private final LinkedHashMap<String, Boolean> detected = new LinkedHashMap<>();
  private final LinkedHashMap<String, Boolean> staged = new LinkedHashMap<>();
  private byte[] original;
  private boolean identityVerified, readComplete, backupSaved;

  public synchronized State state(){return state;}
  public synchronized Bus requiredBus(){return requiredBus;}
  public synchronized Map<String,Boolean> detectedOptions(){return Collections.unmodifiableMap(new LinkedHashMap<>(detected));}
  public synchronized Map<String,Boolean> stagedOptions(){return Collections.unmodifiableMap(new LinkedHashMap<>(staged));}
  public synchronized String ecuId(){return ecuId;}
  public synchronized boolean canEdit(){return state==State.READY||state==State.DIRTY;}
  public synchronized boolean canWrite(){return state==State.DIRTY&&identityVerified&&readComplete&&backupSaved&&original!=null;}

  public synchronized void selectEcu(String id, Bus bus){
    if(id==null||id.trim().isEmpty()||bus==null)throw new IllegalArgumentException("ECU and bus required");
    clear();
    ecuId=id;
    requiredBus=bus;
    state=State.BUS_CONFIRMATION_REQUIRED;
  }
  /** User acknowledgment does not prove hardware switch position; ECU response must verify it. */
  public synchronized void beginRead(){
    if(state!=State.BUS_CONFIRMATION_REQUIRED)throw new IllegalStateException("Confirm required CAN bus");
    state=State.READING;
  }
  /**
   * Only call after verified ECU identity, successful full read and durable backup.
   * Option values must be derived from actual read bytes, never UI defaults.
   */
  public synchronized void finishRead(String verifiedEcuId,byte[] fullBytes,Map<String,Boolean> decoded,boolean durableBackupSaved){
    if(state!=State.READING)throw new IllegalStateException("Not reading");
    if(!ecuId.equals(verifiedEcuId)||fullBytes==null||fullBytes.length==0||decoded==null||!durableBackupSaved){
      fail();throw new IllegalStateException("Identity, read or backup verification failed");
    }
    original=fullBytes.clone();
    detected.clear();staged.clear();
    for(Map.Entry<String,Boolean> entry:decoded.entrySet()){
      if(entry.getKey()==null||entry.getValue()==null)continue;
      detected.put(entry.getKey(),entry.getValue());
      staged.put(entry.getKey(),entry.getValue());
    }
    identityVerified=true;readComplete=true;backupSaved=true;
    state=State.READY;
  }
  public synchronized void toggle(String option,boolean enabled){
    if(!canEdit()||!staged.containsKey(option))throw new IllegalStateException("Option unavailable or block not read");
    staged.put(option,enabled);
    state=staged.equals(detected)?State.READY:State.DIRTY;
  }
  /** Write operation must be separately confirmed by the user. */
  public synchronized void beginWrite(boolean explicitUserConfirmation,boolean voltageSafe,boolean protocolVerified){
    if(!canWrite()||!explicitUserConfirmation||!voltageSafe||!protocolVerified)
      throw new IllegalStateException("Unsafe write request");
    state=State.WRITING;
  }
  public synchronized void beginVerification(){
    if(state!=State.WRITING)throw new IllegalStateException("Not writing");
    state=State.VERIFYING;
  }
  /** Readback must match exact expected bytes, not merely an ELM OK response. */
  public synchronized void completeVerification(boolean matchesExpectedBytes){
    if(state!=State.VERIFYING)throw new IllegalStateException("Not verifying");
    if(!matchesExpectedBytes){fail();return;}
    detected.clear();detected.putAll(staged);
    state=State.SUCCESS;
  }
  public synchronized void fail(){state=State.FAILED;}
  public synchronized void clear(){
    state=State.DISCONNECTED;ecuId=null;requiredBus=null;original=null;
    identityVerified=false;readComplete=false;backupSaved=false;
    detected.clear();staged.clear();
  }
}
