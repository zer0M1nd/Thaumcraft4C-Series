package tc4revival.data;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;
import net.minecraft.data.*;

/** Original geometric glyphs, generated from source rather than copied from the old resource JAR. */
public  class ModernVisualDataProvider implements DataProvider {
    private final PackOutput output;
    public ModernVisualDataProvider(PackOutput output){this.output=output;}
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        try {
            var image=new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);g.setStroke(new BasicStroke(2.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            g.drawOval(6,6,20,20);g.drawLine(5,27,27,5);
            g.setStroke(new BasicStroke(1));g.drawLine(2,16,5,16);g.drawLine(27,16,30,16);g.drawLine(16,2,16,5);g.drawLine(16,27,16,30);g.dispose();
            var bytes=new ByteArrayOutputStream();ImageIO.write(image,"PNG",bytes);byte[] data=bytes.toByteArray();
            cache.writeIfNeeded(output.getOutputFolder().resolve("assets/thaumcraft/textures/aspects/_empty.png"),data,com.google.common.hash.Hashing.sha1().hashBytes(data));
            var tag=new com.google.gson.JsonObject();tag.addProperty("replace",false);var values=new com.google.gson.JsonArray();values.add("thaumcraft:crimson");tag.add("values",values);
            return DataProvider.saveStable(cache,tag,output.getOutputFolder().resolve("data/minecraft/tags/banner_pattern/no_item_required.json"));

        }catch(java.io.IOException error){return CompletableFuture.failedFuture(error);}
    }
    @Override public String getName(){return "Original modern UI glyphs";}
}
