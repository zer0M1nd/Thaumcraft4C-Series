package tc4revival.aspect;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Immutable aspect metadata; construction alone does not register it in the game. */
public record Aspect(String tag, int color, List<String> components, String chatColor, int blendFactor, String icon) {
    public Aspect(String tag,int color,List<String> components,String chatColor,int blendFactor) {
        this(tag,color,components,chatColor,blendFactor,null);
    }
    public Aspect {
        Objects.requireNonNull(tag);
        if (tag.length()>64 || !tag.matches("(?:[a-z0-9_.-]+:)?[a-z0-9_/.-]+")) throw new IllegalArgumentException("Invalid aspect tag: " + tag);
        if (color < 0 || color > 0xffffff) throw new IllegalArgumentException("Invalid RGB color");
        components = List.copyOf(components);
        if (!components.isEmpty() && components.size() != 2) {
            throw new IllegalArgumentException("An aspect needs either zero or two components");
        }
        Objects.requireNonNull(chatColor);
        if(icon!=null)net.minecraft.resources.ResourceLocation.parse(icon);
    }

    public boolean isPrimal() { return components.isEmpty(); }
    public String displayName() { var name=tag.substring(tag.indexOf(':')+1);return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1); }
    public String descriptionKey() { return "tc.aspect." + tag; }
    public net.minecraft.resources.ResourceLocation texture() { if(icon!=null)return net.minecraft.resources.ResourceLocation.parse(icon);var id=net.minecraft.resources.ResourceLocation.parse(tag.contains(":")?tag:"thaumcraft:"+tag);return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(id.getNamespace(),"textures/aspects/"+id.getPath()+".png"); }
    public static net.minecraft.resources.ResourceLocation texture(String tag) { return TCAspects.catalog().require(tag).texture(); }
    public String texturePath() { return "textures/aspects/" + tag + ".png"; }
}
