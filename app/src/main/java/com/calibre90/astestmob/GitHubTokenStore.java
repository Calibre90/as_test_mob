package com.calibre90.astestmob;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Stores only AES-GCM ciphertext in private preferences; AES key stays in Android Keystore. */
final class GitHubTokenStore {
  private static final String ALIAS="studio_admin_github_token_v1";
  private static final String PREFS="studio_admin_secure_github";
  private static final String IV="iv", DATA="ciphertext";
  private GitHubTokenStore(){}

  private static SharedPreferences prefs(Context context){
    return context.getApplicationContext().getSharedPreferences(PREFS,Context.MODE_PRIVATE);
  }

  static boolean hasToken(Context context){
    return prefs(context).contains(IV)&&prefs(context).contains(DATA);
  }

  private static SecretKey key(boolean create) throws Exception {
    KeyStore store=KeyStore.getInstance("AndroidKeyStore");
    store.load(null);
    if(store.containsAlias(ALIAS))
      return ((KeyStore.SecretKeyEntry)store.getEntry(ALIAS,null)).getSecretKey();
    if(!create)return null;
    KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
    generator.init(new KeyGenParameterSpec.Builder(ALIAS,
      KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
      .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
      .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
      .setRandomizedEncryptionRequired(true).build());
    return generator.generateKey();
  }

  static void save(Context context,String token) throws Exception {
    if(token==null||token.trim().length()<12)throw new IllegalArgumentException("Неверный токен");
    Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE,key(true));
    byte[] encrypted=cipher.doFinal(token.trim().getBytes(StandardCharsets.UTF_8));
    if(!prefs(context).edit()
      .putString(IV,Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP))
      .putString(DATA,Base64.encodeToString(encrypted,Base64.NO_WRAP))
      .commit())throw new IllegalStateException("Не удалось сохранить токен");
  }

  static String load(Context context) throws Exception {
    if(!hasToken(context))return null;
    SecretKey secret=key(false);
    if(secret==null)throw new IllegalStateException("Ключ Android Keystore недоступен. Удалите токен и введите новый.");
    Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.DECRYPT_MODE,secret,new GCMParameterSpec(128,
      Base64.decode(prefs(context).getString(IV,""),Base64.NO_WRAP)));
    return new String(cipher.doFinal(Base64.decode(
      prefs(context).getString(DATA,""),Base64.NO_WRAP)),StandardCharsets.UTF_8);
  }

  static void clear(Context context){
    prefs(context).edit().remove(IV).remove(DATA).commit();
    try{
      KeyStore store=KeyStore.getInstance("AndroidKeyStore");
      store.load(null);
      if(store.containsAlias(ALIAS))store.deleteEntry(ALIAS);
    }catch(Exception ignored){}
  }
}
