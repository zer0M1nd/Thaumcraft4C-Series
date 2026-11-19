package tc4revival.resources;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Map;
import javax.imageio.ImageIO;

/** Local foreground conversion; all original pixels remain outside the distributed JAR. */
public class DeepslateOreTextures {
    public static final String PREFIX="assets/thaumcraft/textures/block/deepslate_";
    // Pixel-selection masks from exact comparison with Minecraft 1.7.10 stone.
    // These are selectors only, not copied stone or ore pixel values.
    private static final String CINNABAR="000000001f001f381f381e381e00001c007c007c007c1f181f001f001e000000";
    private static final String AMBER="00000e007f007f007f787f787f783f000e38003c007e1c7e1c7e1c7c1c380000";
    public static java.util.Set<String> paths(){return java.util.Set.of(PREFIX+"cinnabar_overlay.png",PREFIX+"amber_overlay.png",PREFIX+"infused_overlay.png",PREFIX+"infused_overlay.png.mcmeta");}
    public static void append(Map<String,byte[]> source,Map<String,byte[]> output){
        String old="assets/thaumcraft/textures/blocks/";
        output.put(PREFIX+"cinnabar_overlay.png",cut(source.get(old+"cinnibar.png"),CINNABAR));
        output.put(PREFIX+"amber_overlay.png",cut(source.get(old+"amberore.png"),AMBER));
        output.put(PREFIX+"infused_overlay.png",infused(source.get(old+"infusedore.png"),source.get(old+"infusedorestone.png")));
        byte[] animation=source.get(old+"infusedore.png.mcmeta");
        if(animation!=null)output.put(PREFIX+"infused_overlay.png.mcmeta",animation);
    }
    public static byte[] cut(byte[] source,String hex){
        var image=read(source);var mask=new java.math.BigInteger(hex,16);
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){
            int pixel=(y%image.getWidth())*16/image.getWidth()*16+x*16/image.getWidth();
            if(!mask.testBit(255-pixel))image.setRGB(x,y,0);
        }
        return png(image);
    }
    public static byte[] infused(byte[] source,byte[] sharedStone){
        var image=read(source);var stone=read(sharedStone);
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){
            int p=image.getRGB(x,y),base=stone.getRGB(x*stone.getWidth()/image.getWidth(),(y%image.getWidth())*stone.getHeight()/image.getWidth());
            // Opaque stone is identical across all six variants; only exposed colored veins survive.
            int alpha=(p>>>24)*(255-(base>>>24))/255;
            if((p&0xffffff)==0)alpha=0; // Black is also identical under all six tint colors.
            image.setRGB(x,y,(p&0xffffff)|(alpha<<24));
        }
        return png(image);
    }
    private static BufferedImage read(byte[] bytes){try{var image=ImageIO.read(new ByteArrayInputStream(bytes));if(image==null)throw new IOException("Invalid ore PNG");var copy=new BufferedImage(image.getWidth(),image.getHeight(),BufferedImage.TYPE_INT_ARGB);var graphics=copy.createGraphics();graphics.drawImage(image,0,0,null);graphics.dispose();return copy;}catch(IOException e){throw new UncheckedIOException(e);}}
    private static byte[] png(BufferedImage image){try{var out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);return out.toByteArray();}catch(IOException e){throw new UncheckedIOException(e);}}
}
