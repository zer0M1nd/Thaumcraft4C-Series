package thaumicfutures.expansion;

import java.util.Map;
import java.nio.charset.StandardCharsets;

/** Transform only external legacy art; no original bitmap or model is shipped in the addon. */
public final class RestoredResources {
    public static void append(Map<String,byte[]> output){
        for(var entry:output.entrySet()){
            String path=entry.getKey();
            if(path.startsWith("assets/thaumicfutures/models/block/restored/")&&path.endsWith(".json")){
                var model=com.google.gson.JsonParser.parseString(new String(entry.getValue(),StandardCharsets.UTF_8)).getAsJsonObject();
                var textures=model.getAsJsonObject("textures");
                if(textures!=null)for(var texture:textures.entrySet()){
                    String value=texture.getValue().getAsString();
                    if(value.startsWith("thaumcraft:blocks/"))
                        texture.setValue(new com.google.gson.JsonPrimitive(value.replace("thaumcraft:blocks/","thaumicfutures:block/tc6/")));
                }
                // Geometry-less vanilla children must keep their cube parent.
                if(!model.has("parent"))model.addProperty("parent","minecraft:block/block");
                else if(model.get("parent").getAsString().startsWith("block/"))
                    model.addProperty("parent","minecraft:"+model.get("parent").getAsString());
                else if(model.get("parent").getAsString().startsWith("thaumcraft:block/"))
                    model.addProperty("parent",model.get("parent").getAsString().replace("thaumcraft:block/","thaumicfutures:block/restored/"));
                model.addProperty("render_type","minecraft:cutout");
                entry.setValue(model.toString().getBytes(StandardCharsets.UTF_8));
            }
            if(path.equals("assets/thaumicfutures/textures/block/creative_jar_side.png"))entry.setValue(purple(entry.getValue(),false));
            if(path.equals("assets/thaumicfutures/textures/block/creative_jar_top.png"))entry.setValue(purple(entry.getValue(),true));
        }
    }
    private static byte[] purple(byte[] original,boolean all){
        try{
            var source=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(original));
            if(source==null)throw new java.io.IOException("Invalid jar texture");
            var image=new java.awt.image.BufferedImage(source.getWidth(),source.getHeight(),java.awt.image.BufferedImage.TYPE_INT_ARGB);
            for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){
                int pixel=source.getRGB(x,y),r=pixel>>16&255,g=pixel>>8&255,b=pixel&255;
                // Preserve glass highlights and alpha; turn the blue runes purple.
                if(all||b-r>18){
                    double l=(r*.2126+g*.7152+b*.0722)/255;
                    r=(int)(40+176*l);g=(int)(18+118*l);b=(int)(65+185*l);
                    pixel=(pixel&0xff000000)|(r<<16)|(g<<8)|b;
                }
                image.setRGB(x,y,pixel);
            }
            var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",bytes);return bytes.toByteArray();
        }catch(java.io.IOException e){throw new java.io.UncheckedIOException("Cannot derive creative jar texture",e);}
    }
    private RestoredResources(){}
}
