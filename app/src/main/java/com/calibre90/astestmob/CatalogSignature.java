package com.calibre90.astestmob;

import android.util.Base64;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

/** Verifies the publisher's RSA signature, independently of license activation. */
final class CatalogSignature {
  // Set to a dedicated catalog-signing RSA public key after generating the publisher key pair.
  // Never reuse or embed the private license-generation key.
  private static final String CATALOG_PUBLIC_KEY_BASE64 = "";
  private CatalogSignature(){}

  static boolean verify(JSONObject document){
    if(CATALOG_PUBLIC_KEY_BASE64.isEmpty())return false; // Fail closed until configured.
    try{
      if(document.getInt("schema")!=1)return false;
      int version=document.getInt("version");
      if(version<1)return false;
      String payload="MAZDA6GH-CATALOG-V1\n"+version+"\n"+document.getJSONArray("features").toString();
      // Verify the exact published UTF-8 payload, not a reserialized JSON array.\n      if(!new org.json.JSONArray(document.getString("features_payload")).similar(document.getJSONArray("features")))return false;\n      byte[] publicDer=Base64.decode(CATALOG_PUBLIC_KEY_BASE64,Base64.DEFAULT);
      PublicKey publicKey=KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicDer));
      Signature verifier=Signature.getInstance("SHA256withRSA");
      verifier.initVerify(publicKey);
      verifier.update(payload.getBytes(StandardCharsets.UTF_8));
      return verifier.verify(Base64.decode(document.getString("signature"),Base64.DEFAULT));
    }catch(Exception ex){return false;}
  }
}
