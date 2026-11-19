package fmmodern;

import java.util.*;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.*;

/** Data-driven allowlist: arbitrary crystal NBT cannot turn a cage into a boss/entity factory. */
public final class CageMobs extends SimpleJsonResourceReloadListener {
    private static Map<ResourceLocation,String> mobs=Map.of();
    private static Map<ResourceLocation,String> clientMobs=Map.of();
    private static Map<ResourceLocation,String> active(){return net.minecraftforge.fml.util.thread.EffectiveSide.get().isClient()?clientMobs:mobs;}
    public static net.minecraft.nbt.CompoundTag snapshot(){var tag=new net.minecraft.nbt.CompoundTag();mobs.forEach((id,aspect)->tag.putString(id.toString(),aspect));return tag;}
    public static void receive(net.minecraft.nbt.CompoundTag tag){var next=new LinkedHashMap<ResourceLocation,String>();for(String key:tag.getAllKeys()){var id=ResourceLocation.tryParse(key);if(id!=null)next.put(id,tag.getString(key));}clientMobs=Map.copyOf(next);}
    public static java.util.List<net.minecraft.world.item.ItemStack> crystals(){return active().keySet().stream().filter(CageMobs::allowed).sorted().map(id->{var stack=new net.minecraft.world.item.ItemStack(Content.MOB_CRYSTAL.get());stack.getOrCreateTag().putString("mob",id.toString());return stack;}).toList();}
    public CageMobs(){super(new Gson(),"forbidden_magic/cage_mobs");}
    @Override protected void apply(Map<ResourceLocation,JsonElement> files,ResourceManager manager,ProfilerFiller profiler){
        Map<ResourceLocation,String> loaded=new LinkedHashMap<>();
        files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(file->file.getValue().getAsJsonObject().entrySet().forEach(entry->{
            var id=ResourceLocation.tryParse(entry.getKey());String aspect=entry.getValue().getAsString();
            if(id==null)throw new IllegalArgumentException("Invalid cage mob "+entry.getKey());
            if(ForgeRegistries.ENTITY_TYPES.containsKey(id)){TCAspects.catalog().require(aspect);loaded.put(id,aspect);}
        }));mobs=Map.copyOf(loaded);
    }
    public static boolean allowed(ResourceLocation id){if(id==null||!fmmodern.ForbiddenConfig.value(ForbiddenConfig.WRATH_CAGE))return false;
        if(!integrationEnabled(id))return false;
        if(active().containsKey(id))return true;
        if(!fmmodern.ForbiddenConfig.value(ForbiddenConfig.WRATH_CRAZY)||!ForgeRegistries.ENTITY_TYPES.containsKey(id))return false;
        var type=ForgeRegistries.ENTITY_TYPES.getValue(id);
        return type!=EntityType.PLAYER&&type!=EntityType.ENDER_DRAGON&&type!=EntityType.WITHER&&!type.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,ResourceLocation.fromNamespaceAndPath("forge","bosses")));}
    private static boolean integrationEnabled(ResourceLocation id){
        return switch(id.getNamespace()){
            case "twilightforest" -> fmmodern.ForbiddenConfig.value(ForbiddenConfig.CROSS_MOD)&&fmmodern.ForbiddenConfig.value(ForbiddenConfig.TWILIGHT);
            case "specialmobs" -> fmmodern.ForbiddenConfig.value(ForbiddenConfig.CROSS_MOD)&&fmmodern.ForbiddenConfig.value(ForbiddenConfig.SPECIAL_MOBS);
            default -> true;
        };
    }
    public static Aspect aspect(ResourceLocation id){
        if(id!=null&&id.toString().equals("twilightforest:pinch_beetle")&&fmmodern.ForbiddenConfig.value(ForbiddenConfig.NO_LUST))return TCAspects.catalog().require("bestia");
        if(id!=null&&(id.toString().equals("minecraft:silverfish")||id.getNamespace().equals("specialmobs")&&id.getPath().endsWith("silverfish"))&&!fmmodern.ForbiddenConfig.value(ForbiddenConfig.SILVERFISH_EMERALDS))return TCAspects.catalog().require("bestia");return TCAspects.catalog().require(id==null?"lucrum":active().getOrDefault(id,"lucrum"));}
    public static EntityType<?> type(ResourceLocation id){return allowed(id)?ForgeRegistries.ENTITY_TYPES.getValue(id):null;}
}
