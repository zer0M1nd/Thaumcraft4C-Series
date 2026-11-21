package tc4revival.resources;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import tc4revival.data.LegacyLanguageTranslations;

/** Private local conversion output, never a build resource directory. */
public  class ExternalResourceCache {
    public static final String VERSION = "6";
    public static final String RESEARCH_BUTTON="assets/thaumcraft/textures/gui/research_button.png";
    public static final String CRIMSON_BANNER="assets/thaumcraft/textures/entity/banner/crimson.png";
    public static final String CRIMSON_SHIELD="assets/thaumcraft/textures/entity/shield/crimson.png";
    private ExternalResourceCache() {}
    private static byte[] crimsonPattern(byte[] source,int width,int height) {
        try {
            var atlas=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(source));
            var mask=new java.awt.image.BufferedImage(128,128,java.awt.image.BufferedImage.TYPE_INT_ARGB);
            // ModelBanner: 14x28 cloth, texture offset 0,0 and depth 1 on a 128x64 atlas.
            // Preserve the supplied cult emblem, removing only its red cloth background.
            for(int y=0;y<height*2;y++)for(int x=0;x<width*2;x++) {
                int sx=(int)((1+(x+.5)*14/(width*2))*atlas.getWidth()/128d);
                int sy=(int)((1+(y+.5)*28/(height*2))*atlas.getHeight()/64d);
                int color=atlas.getRGB(sx,sy),r=color>>16&255,g=color>>8&255,b=color&255;
                int alpha=g>r*.55 && g>b*1.3 ? color>>>24 : 0;
                int pixel=alpha<<24|0xffffff;
                mask.setRGB(2+x,2+y,pixel);
                mask.setRGB((width+2)*2+x,2+y,pixel);
            }
            var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(mask,"png",bytes);return bytes.toByteArray();
        }catch(IOException failure){throw new java.io.UncheckedIOException("Cannot derive cult banner",failure);}
    }
    private static byte[] researchButton(byte[] atlasBytes) {
        try {
            var atlas=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(atlasBytes));
            var button=new java.awt.image.BufferedImage(8,8,java.awt.image.BufferedImage.TYPE_INT_ARGB);
            // Preserve the border, but use one light sample throughout the stretchable interior.
            for(int y=0;y<8;y++)for(int x=0;x<8;x++) {
                int sourceX=x<2?184+x:x>=6?206+x-6:203;
                button.setRGB(x,y,x>=2 && x<6 && y>=2 && y<6 ? atlas.getRGB(203,210) : atlas.getRGB(sourceX,208+y));
            }
            var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(button,"png",bytes);return bytes.toByteArray();
        }catch(IOException failure){throw new java.io.UncheckedIOException("Cannot derive research button",failure);}
    }

    public static Map<String, byte[]> convert(OriginalResources.Snapshot source, Map<String, String> mapping) {
        var output = new TreeMap<String, byte[]>();
        DeepslateOreTextures.append(source.resources(),output);
        mapping.forEach((target, original) -> output.put(target, source.resources().get(original)));
        mapping.forEach((target, original) -> {
            byte[] metadata=source.resources().get(original+".mcmeta");
            if(target.endsWith(".png") && metadata!=null)output.put(target+".mcmeta",metadata);
        });
        output.put(RESEARCH_BUTTON,researchButton(source.resources().get("assets/thaumcraft/textures/gui/guiresearchtable2.png")));
        var cult=source.resources().get("assets/thaumcraft/textures/models/banner_cultist.png");
        output.put(CRIMSON_BANNER,crimsonPattern(cult,20,40));
        output.put(CRIMSON_SHIELD,crimsonPattern(cult,12,22));
        var gson = new GsonBuilder().disableHtmlEscaping().create();
        for (String locale : List.of("en_us", "zh_cn", "zh_tw")) {
            var language = new JsonObject();
            String oldLocale = switch(locale){case "zh_cn" -> "zh_CN";case "zh_tw" -> "zh_TW";default -> "en_US";};
            var lines = new String(source.resources().get("assets/thaumcraft/lang/" + oldLocale + ".lang"), StandardCharsets.UTF_8).lines().toList();
            LegacyLanguageTranslations.append(lines, locale.startsWith("zh_"), (key,value)->{
                // Avoid manufacturing Simplified-Chinese spawn-egg suffixes for the newly imported locale.
                if(!locale.equals("zh_tw")||!key.endsWith("_spawn_egg"))language.addProperty(key,value);
            });
            output.put("assets/thaumcraft/lang/" + locale + ".json", gson.toJson(language).getBytes(StandardCharsets.UTF_8));
        }
        output.put("pack.mcmeta", ("{\"pack\":{\"pack_format\":15,\"description\":\"Local Thaumcraft 4 resources\"}}").getBytes(StandardCharsets.UTF_8));
        return output;
    }

    public static Path prepare(Path gameDirectory, OriginalResources.Snapshot source, Map<String, String> mapping) throws IOException {
        var output = convert(source, mapping);
        String mappingHash = OriginalResources.sha256(mapping.toString().getBytes(StandardCharsets.UTF_8));
        var directory = gameDirectory.resolve(".thaumcraft-cache").resolve(source.sha256() + "-" + VERSION + "-" + mappingHash.substring(0, 16));
        // Validate bytes, not just a completion flag; damaged or interrupted caches are repaired.
        for (var entry : output.entrySet()) {
            var file = directory.resolve(entry.getKey());
            if (Files.isRegularFile(file) && Files.size(file) == entry.getValue().length
                    && Arrays.equals(Files.readAllBytes(file), entry.getValue())) continue;
            Files.createDirectories(file.getParent());
            var temporary = Files.createTempFile(file.getParent(), ".writing-", ".tmp");
            try {
                Files.write(temporary, entry.getValue());
                try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
                catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
            } finally { Files.deleteIfExists(temporary); }
        }
        Files.writeString(directory.resolve("complete.txt"), VERSION + "\n" + source.sha256(), StandardCharsets.UTF_8);
        return directory;
    }
}
