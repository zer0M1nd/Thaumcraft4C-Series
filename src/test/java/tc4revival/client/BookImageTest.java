package tc4revival.client;

import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.*;
import javax.imageio.metadata.IIOMetadataNode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BookImageTest {
    @Test void decodesFramesAndTheirTimingAndRejectsInvalidData() throws Exception {
        var bytes=new ByteArrayOutputStream();var writer=ImageIO.getImageWritersByFormatName("gif").next();
        try(var output=ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(output);writer.prepareWriteSequence(null);
            for(int i=0;i<3;i++) {
                var image=new BufferedImage(3,2,BufferedImage.TYPE_INT_RGB);
                for(int y=0;y<2;y++)for(int x=0;x<3;x++)image.setRGB(x,y,i==0?0xff0000:i==1?0x00ff00:0x0000ff);
                var metadata=writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(image),writer.getDefaultWriteParam());
                var tree=(IIOMetadataNode)metadata.getAsTree("javax_imageio_gif_image_1.0");
                var control=(IIOMetadataNode)tree.getElementsByTagName("GraphicControlExtension").item(0);
                control.setAttribute("delayTime",Integer.toString(10+i));control.setAttribute("disposalMethod","restoreToPrevious");
                metadata.setFromTree("javax_imageio_gif_image_1.0",tree);
                writer.writeToSequence(new IIOImage(image,null,metadata),null);
            }
            writer.endWriteSequence();
        }finally{writer.dispose();}
        var frames=BookImage.decode(new ByteArrayInputStream(bytes.toByteArray()));
        assertEquals(3,frames.size());assertEquals(0xffff0000,frames.get(0).image().getRGB(0,0));
        assertEquals(0xff00ff00,frames.get(1).image().getRGB(0,0));assertEquals(0xff0000ff,frames.get(2).image().getRGB(0,0));
        assertEquals(java.util.List.of(100,110,120),frames.stream().map(BookImage.Frame::millis).toList());
        assertThrows(IOException.class,()->BookImage.decode(new ByteArrayInputStream(new byte[]{1,2,3})));
    }
}
