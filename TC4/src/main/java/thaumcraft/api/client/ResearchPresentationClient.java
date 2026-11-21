package thaumcraft.api.client;

import java.util.*;
import java.util.function.*;
import net.minecraft.resources.ResourceLocation;
import tc4revival.research.ResearchDefinition;
import thaumcraft.api.util.ExtensionRegistry;

/** Client setup providers for item icons, overlays and narrowly keyed book text substitutions. */
public final class ResearchPresentationClient {
    public record IconContext(String research,long milliseconds,boolean locked) {}
    public record Icon(ResearchDefinition.Stack stack,List<ResourceLocation> overlays) {public Icon {overlays=List.copyOf(overlays);}}
    @FunctionalInterface public interface IconProvider {Icon select(IconContext context,Icon current);}
    @FunctionalInterface public interface TextToken {String patch(String locale,String current);}
    private record Text(String key,TextToken token) {}
    private static final ExtensionRegistry<IconProvider> ICONS=new ExtensionRegistry<>();
    private static final ExtensionRegistry<Text> TEXT=new ExtensionRegistry<>();
    private ResearchPresentationClient() {}
    public static void registerIcon(String id,int priority,IconProvider provider){ICONS.register(id,priority,provider);}
    public static void registerTextToken(String id,int priority,String key,TextToken token){TEXT.register(id,priority,new Text(key,token));}
    public static Icon icon(IconContext context,ResearchDefinition.Stack fallback){
        var result=new Icon(fallback,List.of());for(var entry:ICONS.entries())result=Objects.requireNonNull(entry.value().select(context,result));return result;
    }
    public static String text(String locale,String key,String value){
        for(var entry:TEXT.entries())if(entry.value().key().equals(key))value=Objects.requireNonNull(entry.value().token().patch(locale,value));return value;
    }
}
