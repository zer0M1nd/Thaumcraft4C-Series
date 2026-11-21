package tc4revival.aspect.entity;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.TC4Revival;
import tc4revival.aspect.TCAspects;

public  class EntityAspects {
    private static volatile EntityAspectResolver resolver;
    private static EntityAspectResolver authored;
    private static Map<ResourceLocation,JsonElement> authoredFiles=Map.of();
    public static void inference(java.util.List<EntityAspectRule> extra){
        resolver=authored.withAdditional(extra);var files=new java.util.TreeMap<ResourceLocation,JsonElement>(authoredFiles);
        files.put(ResourceLocation.parse("thaumcraft:advanced_inference"),EntityAspectJson.encode(extra));
        tc4revival.aspect.sync.ServerAspectData.capture("entity_aspects",files);
    }
    private EntityAspects() {}

    public static void addReloadListener(AddReloadListenerEvent event) { event.addListener(new Reload()); }
    public static void stopped(ServerStoppedEvent event) { resolver = null; }

    public static EntityAspectResolver resolver() {
        var current = resolver;
        if (current == null) throw new IllegalStateException("Entity aspect data has not loaded");
        return current;
    }

    public static Optional<EntityAspectResolver.Result> resolve(Entity entity) {
        var base=resolveOriginal(entity);
        var edited=tc4revival.aspect.editor.EditorResources.entity(entity.getType());
        return edited.isEmpty()?base:Optional.of(new EntityAspectResolver.Result(edited.get(),base.map(EntityAspectResolver.Result::legacyIdentity)
                .orElse("modern:entity:"+ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()))));
    }
    private static Optional<EntityAspectResolver.Result> resolveOriginal(Entity entity) {
        if(entity instanceof tc4revival.content.WispEntity wisp) return TCAspects.catalog().find(wisp.aspectTag()).map(a->new EntityAspectResolver.Result(new tc4revival.aspect.AspectAmounts().add(a,2).add(TCAspects.catalog().require("praecantatio"),1).add(TCAspects.catalog().require("aer"),1),"Thaumcraft.WispType"+a.tag())).map(base->resolver().contribute(facts(entity).orElseThrow(),base));
        return facts(entity).flatMap(resolver()::resolve);
    }
    public static Optional<String> legacyName(Entity entity) {
        if(entity instanceof tc4revival.content.WispEntity)return Optional.of("Thaumcraft.Wisp");
        return facts(entity).flatMap(resolver()::legacyName);
    }
    private static Optional<EntityFacts> facts(Entity entity) {
        var type = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (type == null) return Optional.empty();
        return Optional.of(new EntityFacts(type.toString(),
                entity instanceof Player player ? player.getGameProfile().getName() : null,
                entity instanceof LivingEntity living && living.isBaby(), entity instanceof ZombieVillager || entity instanceof tc4revival.content.BrainyZombieEntity zombie && zombie.villager(),
                entity instanceof Creeper creeper && creeper.getSwellDir() == 1,
                entity instanceof Creeper creeper && creeper.isPowered(),
                entity instanceof Boat boat && boat.getVariant() == Boat.Type.OAK,entity instanceof tc4revival.content.PechEntity pech ? pech.profession() : -1,
                entity instanceof tc4revival.content.GolemEntity golem?(golem.configuration().customMaterial()==null?golem.configuration().material().name():golem.configuration().customMaterial().toString()):""));
    }

    private static  class Reload extends SimpleJsonResourceReloadListener {
        Reload() { super(new Gson(), "entity_aspects"); }
        @Override protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            var rules = new ArrayList<EntityAspectRule>();
            files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> rules.addAll(EntityAspectJson.decode(entry.getValue())));
            for (var rule : rules) {
                if (!ForgeRegistries.ENTITY_TYPES.containsKey(ResourceLocation.tryParse(rule.entity()))) {
                    throw new IllegalArgumentException("Unknown entity in aspect table: " + rule.entity());
                }
            }
            var additions=new ArrayList<EntityAspectContributions.Entry>();
            var contributionFiles=new java.util.TreeMap<ResourceLocation,JsonElement>();
            manager.listResources("entity_aspect_contributions",id->id.getPath().endsWith(".json")).entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).forEach(entry->{
                    try(var reader=entry.getValue().openAsReader()) {
                        var json=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                        String path=entry.getKey().getPath();
                        contributionFiles.put(new ResourceLocation(entry.getKey().getNamespace(),path.substring("entity_aspect_contributions/".length(),path.length()-5)),json);
                        additions.addAll(EntityAspectContributions.decode(json,
                                id->ForgeRegistries.ENTITY_TYPES.containsKey(ResourceLocation.parse(id))));
                    } catch(Exception e){throw new IllegalArgumentException("Invalid entity contribution "+entry.getKey(),e);}
                });
            resolver = new EntityAspectResolver(rules, TCAspects.catalog(),new EntityAspectContributions(additions,TCAspects.catalog()));
            authored=resolver;authoredFiles=Map.copyOf(files);
            tc4revival.aspect.sync.ServerAspectData.capture("entity_aspects",files);
            tc4revival.aspect.sync.ServerAspectData.capture("entity_aspect_contributions",contributionFiles);
            TC4Revival.LOGGER.info("Loaded {} entity aspect rules", rules.size());
        }
    }
}
