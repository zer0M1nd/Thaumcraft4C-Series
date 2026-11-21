package thaumicomniverse.client;

import java.util.*;

/** Local resource conversion: retain TC4 pipe detail and alpha, remap luminance to warm brass. */
public final class BrassTubeTextures {
    public static final List<String> NAMES=List.of("pipe_1","pipe_2","pipe_3","pipe_filter","pipe_restrict");
    private static String path(String name){return "assets/thaumicomniverse/textures/block/brass_"+name+".png";}
    public static Set<String> paths(){var paths=new HashSet<String>();NAMES.forEach(n->paths.add(path(n)));return Set.copyOf(paths);}
    public static void append(Map<String,byte[]> output){for(String name:NAMES){var source=output.get("assets/thaumicomniverse/textures/block/brass_"+name+".png");if(source!=null)output.put(path(name),convert(source));}}
    public static byte[] convert(byte[] bytes){
        try{
            var source=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
            if(source==null)throw new java.io.IOException("Invalid pipe PNG");
            // Legacy PNGs may be indexed: setRGB on their palette image silently snaps brass back to old colours.
            var image=new java.awt.image.BufferedImage(source.getWidth(),source.getHeight(),java.awt.image.BufferedImage.TYPE_INT_ARGB);
            image.setRGB(0,0,source.getWidth(),source.getHeight(),source.getRGB(0,0,source.getWidth(),source.getHeight(),null,0,source.getWidth()),0,source.getWidth());
            for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){
                int pixel=image.getRGB(x,y);double light=((pixel>>16&255)*.2126+(pixel>>8&255)*.7152+(pixel&255)*.0722)/255;
                light=Math.sqrt(light);int red=(int)(50+190*light),green=(int)(31+166*light),blue=(int)(12+104*light);
                image.setRGB(x,y,(pixel&0xff000000)|(red<<16)|(green<<8)|blue);
            }
            var output=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",output);return output.toByteArray();
        }catch(java.io.IOException e){throw new java.io.UncheckedIOException("Cannot derive brass pipe texture",e);}
    }
    private BrassTubeTextures(){}
}
