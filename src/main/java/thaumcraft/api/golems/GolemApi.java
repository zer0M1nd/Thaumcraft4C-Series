package thaumcraft.api.golems;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import tc4revival.content.*;

/** Register on both sides during common setup. IDs, not ordinals, are saved and synchronized. */
public  class GolemApi {
    public record Material(int health,int armor,float speed,boolean fireResistant,int upgrades,int carry,int regenerationDelay,int strength,boolean sinks,ResourceLocation texture) {
        public Material { if(health<1||health>100000||armor<0||armor>1000||!Float.isFinite(speed)||speed<=0||speed>10||upgrades<0||upgrades>32||carry<1||carry>4096||regenerationDelay<1||strength<0||strength>100000)throw new IllegalArgumentException("Invalid golem material");Objects.requireNonNull(texture); }
        public static Material legacy(GolemMaterial m){return new Material(m.health,m.armor,m.speed,m.fireResistant,m.upgrades,m.carry,m.regenDelay,m.strength,m.sinks(),ResourceLocation.parse("thaumcraft:textures/models/golem_"+m.name().toLowerCase(Locale.ROOT)+".png"));}
    }
    @FunctionalInterface public interface Task { void install(GolemEntity golem,GoalSelector work,GoalSelector targets); }
    private static final Map<ResourceLocation,Material> MATERIALS=new LinkedHashMap<>();
    private static final Map<ResourceLocation,Task> TASKS=new LinkedHashMap<>();
    public static synchronized void registerMaterial(ResourceLocation id,Material material){if(MATERIALS.putIfAbsent(Objects.requireNonNull(id),Objects.requireNonNull(material))!=null)throw new IllegalArgumentException("Duplicate golem material "+id);}
    public static synchronized void registerTask(ResourceLocation id,Task task){if(TASKS.putIfAbsent(Objects.requireNonNull(id),Objects.requireNonNull(task))!=null)throw new IllegalArgumentException("Duplicate golem task "+id);}
    public static Optional<Material> material(ResourceLocation id){return Optional.ofNullable(MATERIALS.get(id));}
    public static Optional<Task> task(ResourceLocation id){return Optional.ofNullable(TASKS.get(id));}
    public static Map<ResourceLocation,Material> materials(){return Map.copyOf(MATERIALS);}
    public static void configure(GolemEntity golem,ResourceLocation material,ResourceLocation task,BlockState appearance){
        if(golem.level().isClientSide||!golem.level().getServer().isSameThread())throw new IllegalStateException("Server thread only");
        if(material!=null&&!MATERIALS.containsKey(material)||task!=null&&!TASKS.containsKey(task))throw new IllegalArgumentException("Unregistered golem definition");
        var config=golem.configuration();config.extension(material,task,appearance);golem.refreshConfiguration();golem.setHealth(Math.min(golem.getHealth(),golem.getMaxHealth()));
    }
    public static void data(GolemEntity golem,ResourceLocation namespace,net.minecraft.nbt.CompoundTag data){if(golem.level().isClientSide||!golem.level().getServer().isSameThread())throw new IllegalStateException("Server thread only");golem.configuration().addonData(namespace,data);golem.state().changed();}
    private GolemApi(){}
}
