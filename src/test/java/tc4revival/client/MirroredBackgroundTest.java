package tc4revival.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MirroredBackgroundTest {
    @Test void fillsAnyViewportWithoutStretchingOrDiscontinuousSeams() {
        for(int start:new int[]{-517,-256,-3,0,144,255,256,999})
            for(int length:new int[]{1,112,256,700,1920}) {
                var spans=MirroredBackground.spans(start,length,256);
                int offset=0;
                Integer previous=null;
                for(var span:spans) {
                    assertEquals(offset,span.offset());
                    assertEquals(span.length(),Math.abs(span.textureLength()));
                    if(previous!=null)assertEquals(previous.intValue(),span.textureStart());
                    previous=span.textureStart()+span.textureLength();
                    assertTrue(previous>=0 && previous<=256);
                    offset+=span.length();
                }
                assertEquals(length,offset);
            }
    }
}
