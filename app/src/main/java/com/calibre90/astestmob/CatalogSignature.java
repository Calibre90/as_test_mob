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
  private static final String CATALOG_PUBLIC_KEY_BASE64 = "MIIBojANBgkqhkiG9w0BAQEFAAOCAY8AMIIBigKCAYEAxGvZWE8zmhI20vr8FeAJK5QPy7Tg0ks6uKSi4utGbsEaR/L+xDl7RnIl8pw6Xu31axAYXRzMipAoWxrVCvUZ5zMjhIVbxmxtFWAYu6Ie/QOG7RGPgw+Q5KMW29e03UC2Xaf7Us5r/PeIiM3ks5V5GNkRpIWDWlunjdjglREgYJkcDrDPRPpmqnShcbzTZpYy915suGBtDzx9fHVmB7XrxZgrVHGhu9Qpt6vCI5QiLI2ZgKiCsdLiLFL13F1d0qod8S1r2W4XOCgW8AADBJJi3XT7pJlX2SZrsxzcUQ2e/TCMRUHOIZYOoaO8bJH/flBADpDhifDNvUQFdhxL8tBp95BdHah3Vpf1zB0IU6K42WBKQ1lNi2dPH5BpEBQ/Fa/6uwCbW1GbW3+5ABzRAt/HgLkF1wT3NTOwPrSVub0POZ6y0bAfDWDvHfst5z2zjKF/+nje5D3lucsgihAG1VlqQzSubGBe5APtiFvp6h4SsEJOLCicCFPyqhXLhYCD2LhtAgMBAAE=";
  private CatalogSignature(){}

  /** Verify a full published configuration; never trust an unsigned admin export. */
  static boolean verifyFull(JSONObject document) {
    if (CATALOG_PUBLIC_KEY_BASE64.isEmpty()) return false;
    try {
      FullCatalogEnvelope envelope = FullCatalogEnvelope.parse(document);
      byte[] publicDer = Base64.decode(CATALOG_PUBLIC_KEY_BASE64, Base64.DEFAULT);
      PublicKey publicKey = KeyFactory.getInstance("RSA")
          .generatePublic(new X509EncodedKeySpec(publicDer));
      Signature verifier = Signature.getInstance("SHA256withRSA");
      verifier.initVerify(publicKey);
      verifier.update(envelope.signingMessage().getBytes(StandardCharsets.UTF_8));
      return verifier.verify(Base64.decode(document.getString("signature"), Base64.DEFAULT));
    } catch (Exception ex) {
      return false;
    }
  }

  static boolean verify(JSONObject document){
    if(CATALOG_PUBLIC_KEY_BASE64.isEmpty())return false; // Fail closed until configured.
    try{
      if(document.getInt("schema")!=1)return false;
      int version=document.getInt("version");
      if(version<1)return false;
      String payload="MAZDA6GH-CATALOG-V1\n"+version+"\n"+document.getString("features_payload");
      // Verify the exact published UTF-8 payload, not a reserialized JSON array.
      new org.json.JSONArray(document.getString("features_payload")); // Parse before verifying.
      byte[] publicDer=Base64.decode(CATALOG_PUBLIC_KEY_BASE64,Base64.DEFAULT);
      PublicKey publicKey=KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicDer));
      Signature verifier=Signature.getInstance("SHA256withRSA");
      verifier.initVerify(publicKey);
      verifier.update(payload.getBytes(StandardCharsets.UTF_8));
      return verifier.verify(Base64.decode(document.getString("signature"),Base64.DEFAULT));
    }catch(Exception ex){return false;}
  }
}
