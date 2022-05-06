package com.autodesk.ece.utilities;

import com.autodesk.ece.constants.BICECEConstants;
import com.autodesk.testinghub.core.base.GlobalConstants;
import com.autodesk.testinghub.core.utils.ProtectedConfigFile;
import com.autodesk.testinghub.core.utils.YamlUtil;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class PelicanRequestSigner {
  private final String actorSecret;
  private final String xE2PartnerId;
  private final String xE2AppFamilyId;

  public PelicanRequestSigner() {
    String testFileKey = "BIC_ORDER_" + GlobalConstants.ENV.toUpperCase();
    Map<?, ?> loadYaml = YamlUtil.loadYmlUsingTestManifest(testFileKey);
    LinkedHashMap<String, String> defaultValues = (LinkedHashMap<String, String>) loadYaml
        .get("default");
    xE2PartnerId = defaultValues.get(BICECEConstants.GETPRICEDETAILS_X_E2_PARTNER_ID);
    xE2AppFamilyId = defaultValues.get(BICECEConstants.GETPRICEDETAILS_X_E2_APPFAMILY_ID);
    actorSecret = ProtectedConfigFile.decrypt(defaultValues.get("getPelicanActorSecret"));
  }

  private static String hex(byte[] bytes) {
    StringBuilder result = new StringBuilder();
    for (byte aByte : bytes) {
      result.append(String.format("%02x", aByte));
    }
    return result.toString();
  }

  public PelicanSignature generateSignature() {
    return new PelicanSignature(xE2AppFamilyId, xE2PartnerId, actorSecret);
  }

  public static class PelicanSignature {
    public final String xE2PartnerId;
    public final String xE2AppFamilyId;
    public final String xE2HMACTimestamp;
    public final String xRequestRef;
    public String xE2HMACSignature;

    public PelicanSignature(String xE2AppFamilyId, String xE2PartnerId, String actorSecret) {
      this.xE2AppFamilyId = xE2AppFamilyId;
      this.xE2PartnerId = xE2PartnerId;

      Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
      xE2HMACTimestamp = String.valueOf(cal.getTimeInMillis() / 1000);

      try {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(actorSecret.getBytes(), "HmacSHA256");
        mac.init(keySpec);

        String message = xE2PartnerId + xE2AppFamilyId + xE2HMACTimestamp;

        byte[] signatureBytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        xE2HMACSignature = hex(signatureBytes);
      } catch (Exception e) {
        e.printStackTrace();
      }
      xRequestRef = UUID.randomUUID().toString();
    }
  }
}
