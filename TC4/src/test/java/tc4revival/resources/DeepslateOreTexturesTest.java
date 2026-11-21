package tc4revival.resources;

import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DeepslateOreTexturesTest {
    private byte[] png(BufferedImage image)throws Exception{var bytes=new ByteArrayOutputStream();ImageIO.write(image,"png",bytes);return bytes.toByteArray();}
    @Test void maskRemovesOnlySelectedStonePixelsWithoutChangingOreColor()throws Exception{
        var original=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<16;y++)for(int x=0;x<16;x++)original.setRGB(x,y,0xff8f372a);
        var output=ImageIO.read(new ByteArrayInputStream(DeepslateOreTextures.cut(png(original),"8"+"0".repeat(63))));
        assertEquals(0xff8f372a,output.getRGB(0,0));
        assertEquals(0,output.getRGB(1,0));assertEquals(0,output.getRGB(15,15));
    }
    @Test void sharedStoneIsRemovedInEveryAnimationFrame()throws Exception{
        var stone=new BufferedImage(2,2,BufferedImage.TYPE_INT_ARGB);stone.setRGB(0,0,0xff888888);
        var frames=new BufferedImage(2,4,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<4;y++)for(int x=0;x<2;x++)frames.setRGB(x,y,0xffcccccc);
        var output=ImageIO.read(new ByteArrayInputStream(DeepslateOreTextures.infused(png(frames),png(stone))));
        assertEquals(0,output.getRGB(0,0)>>>24);assertEquals(0,output.getRGB(0,2)>>>24);
        assertEquals(0xffcccccc,output.getRGB(1,0));assertEquals(0xffcccccc,output.getRGB(1,2));
        assertEquals(4,output.getHeight());
    }
}
