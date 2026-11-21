package tc4revival.resources;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class I18nDownloadRetryTest {
    static int attempts;
    public static String recoverable(String ignored)throws IOException {if(++attempts<3)throw new java.net.SocketTimeoutException("test timeout");return "downloaded";}
    public static String invalid(String ignored) {attempts++;throw new IllegalArgumentException("incompatible API");}
    @Test void transportFailureIsRetriedButApiFailureIsNot()throws Exception {
        attempts=0;assertEquals("downloaded",I18nUpdateBridge.invokeDownload(getClass().getMethod("recoverable",String.class),"fixture"));assertEquals(3,attempts);
        attempts=0;assertThrows(java.lang.reflect.InvocationTargetException.class,()->I18nUpdateBridge.invokeDownload(getClass().getMethod("invalid",String.class),"fixture"));assertEquals(1,attempts);
    }
}
