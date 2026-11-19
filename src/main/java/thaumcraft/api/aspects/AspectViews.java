package thaumcraft.api.aspects;

import java.util.*;
import java.util.function.BiPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import tc4revival.aspect.*;
import tc4revival.aspect.Aspect;
import tc4revival.research.PlayerKnowledge;
import thaumcraft.api.util.ExtensionRegistry;

/** Reader projections never modify Aspect metadata, raw knowledge, costs, production or handshake fingerprints. */
public final class AspectViews {
    public enum Purpose { ICON, TOOLTIP, NODE, REVEALING, WAND, CATALOG }
    public record Reader(Set<String> research,Set<String> discovered){
        public Reader {research=Set.copyOf(research);discovered=Set.copyOf(discovered);}
        public static Reader empty(){return new Reader(Set.of(),Set.of());}
        public static Reader of(PlayerKnowledge knowledge){return knowledge==null?empty():new Reader(knowledge.researchMarkers(),knowledge.knownAspects().keySet().stream().map(Aspect::tag).collect(java.util.stream.Collectors.toSet()));}
    }
    public record Mask(String nameKey,String descriptionKey,String contentsKey,ResourceLocation texture,ResourceLocation identity,int color) {}
    public record Appearance(boolean visible,Component name,String descriptionKey,ResourceLocation texture,ResourceLocation identity,int color){
        public Appearance {name=name.copy();}
        @Override public Component name(){return name.copy();}
        public String displayName(){return name.getString();}
    }
    private record Policy(BiPredicate<Reader,String> visible,Mask mask) {}
    private static final ExtensionRegistry<Policy> POLICIES=new ExtensionRegistry<>();
    private AspectViews() {}
    public static void register(String id,int priority,BiPredicate<Reader,String> visible,Mask mask){POLICIES.register(id,priority,new Policy(visible,Objects.requireNonNull(mask)));}
    private static Optional<Mask> mask(Reader reader,String tag){
        return POLICIES.entries().stream().filter(e->!e.value().visible().test(reader,tag)).map(e->e.value().mask()).findFirst();
    }
    public static boolean visible(Reader reader,String tag){return mask(reader,tag).isEmpty();}
    public static Component unknownContents(Reader reader,Collection<String> tags){
        for(String tag:tags){var mask=mask(reader,tag);if(mask.isPresent())return Component.translatable(mask.get().contentsKey());}return Component.empty();
    }
    public static Appearance appearance(Reader reader,Aspect aspect){
        var mask=mask(reader,aspect.tag());if(mask.isPresent()){
            var m=mask.get();return new Appearance(false,Component.translatable(m.nameKey()),m.descriptionKey(),m.texture(),m.identity(),m.color());
        }
        return rawAppearance(aspect);
    }
    private static Appearance rawAppearance(Aspect aspect){return new Appearance(true,Component.literal(aspect.displayName()),aspect.descriptionKey(),aspect.texture(),ResourceLocation.parse(aspect.tag().contains(":")?aspect.tag():"thaumcraft:"+aspect.tag()),aspect.color());}
    /** Shared item tooltip/name code may run on a server; only client display uses its reader snapshot. */
    public static Appearance display(Aspect aspect){
        return net.minecraftforge.fml.loading.FMLEnvironment.dist==net.minecraftforge.api.distmarker.Dist.CLIENT
            &&!net.minecraftforge.fml.util.thread.EffectiveSide.get().isServer()?thaumcraft.api.client.ClientAspectViews.view(aspect):rawAppearance(aspect);
    }
    public static AspectAmounts project(Reader reader,AspectAmounts amounts,Purpose purpose){
        Objects.requireNonNull(purpose);var result=new AspectAmounts();
        amounts.view().forEach((aspect,n)->{if(visible(reader,aspect.tag()))result.add(aspect,n);});return result;
    }
    public static PlayerKnowledge knowledge(PlayerKnowledge raw){var reader=Reader.of(raw);return raw.aspectView(aspect->visible(reader,aspect.tag()));}
}
