package play.modules.pushplay;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.digest.DigestUtils;

import play.mvc.Http.Request;

public class PushPlayUtil {

	public static String authToken(String socket_id, String channel, String channel_data) {
		
		String string_to_sign = socket_id + ":" + channel;
		
		if (channel_data != null) {
			string_to_sign = string_to_sign + ":" + channel_data;
		}
		return PushPlayUtil.sha256(string_to_sign, PushPlayPlugin.getSecret());
	}
	
	public static boolean isRequestValid(Request request,
			TriggerEventMessage tem) {
		
		String signature = "POST\n" + request.path + "\n" + tem.toString();
		
		return (tem.auth_signature.equals(sha256(signature, PushPlayPlugin.getSecret())));
	}
	
	public static String sha256(String string, String secret) {
		
        try {
            SecretKeySpec signingKey = new SecretKeySpec( secret.getBytes(), "HmacSHA256");

            final Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);

            byte[] digest = mac.doFinal(string.getBytes());

            BigInteger bigInteger = new BigInteger(1,digest);
            return String.format("%0" + (digest.length << 1) + "x", bigInteger);

        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException("No HMac SHA256 algorithm");
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Invalid key exception while converting to HMac SHA256");
        }
    }

	public static boolean isMD5Valid(String message, TriggerEventMessage tem) {
		
		return (DigestUtils.md5Hex(message).equals(tem.body_md5));
	}
}
