package conciliummodern;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.*;
import net.minecraftforge.common.world.*;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.*;

/** Independent registration boundary for the remaining original creatures. */
public final class RemainingMobs implements BiomeModifier {
    public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,ThaumicConcilium.ID);
    public static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> PARTICLES=DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES,ThaumicConcilium.ID);
    public static final RegistryObject<net.minecraft.core.particles.SimpleParticleType> LEAF=PARTICLES.register("leaf",()->new net.minecraft.core.particles.SimpleParticleType(false));
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,ThaumicConcilium.ID);
    public static final DeferredRegister<net.minecraft.sounds.SoundEvent> SOUNDS=DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,ThaumicConcilium.ID);
    public static final DeferredRegister<Codec<? extends BiomeModifier>> CODECS=DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS,ThaumicConcilium.ID);
    public static final RegistryObject<Codec<RemainingMobs>> CODEC=CODECS.register("remaining_mobs",()->Codec.unit(RemainingMobs::new));
    public static final RegistryObject<EntityType<Chort>> CHORT=entity("chort",Chort::new,MobCategory.MONSTER,.6f,1.8f,true);
    public static final RegistryObject<EntityType<SloppyAlchemist>> SLOPPY_ALCHEMIST=entity("sloppy_alchemist",SloppyAlchemist::new,MobCategory.MONSTER,1,.8f,true);
    public static final RegistryObject<EntityType<BurnedWitch>> BURNED_WITCH=entity("burned_witch",BurnedWitch::new,MobCategory.MONSTER,1,2,true);
    public static final RegistryObject<EntityType<ChortSpit>> CHORT_SPIT=entity("chort_spit",ChortSpit::new,MobCategory.MISC,.25f,.25f,true);
    public static final RegistryObject<EntityType<Dissolved>> DISSOLVED=entity("dissolved",Dissolved::new,MobCategory.MONSTER,.6f,1.8f,true);
    public static final RegistryObject<EntityType<Samurai>> SAMURAI=entity("samurai",Samurai::new,MobCategory.MONSTER,1,1.8f,false);
    public static final RegistryObject<EntityType<Overanimated>> OVERANIMATED=entity("overanimated",Overanimated::new,MobCategory.MONSTER,.6f,1.8f,false);
    public static final RegistryObject<EntityType<ThaumGib>> THAUM_GIB=entity("thaum_gib",ThaumGib::new,MobCategory.MONSTER,.6f,.5f,false);
    public static final RegistryObject<EntityType<DopeSquid>> DOPE_SQUID=entity("dope_squid",DopeSquid::new,MobCategory.CREATURE,.95f,.95f,false);
    public static final RegistryObject<EntityType<WitheredBotanist>> WITHERED_BOTANIST=entity("withered_botanist",WitheredBotanist::new,MobCategory.CREATURE,2,.3f,false);
    public static final RegistryObject<EntityType<UpcomingHole>> UPCOMING_HOLE=entity("upcoming_hole",UpcomingHole::new,MobCategory.MISC,.1f,.1f,true);
    public static final RegistryObject<net.minecraft.sounds.SoundEvent> GARGLE_MANY=sound("gargle_many"),GARGLE_ONE=sound("gargle_one"),GARGLE_PAIN=sound("gargle_pain"),SPEECH=sound("speech"),UAGH=sound("uagh");
    private static RegistryObject<net.minecraft.sounds.SoundEvent> sound(String name){return SOUNDS.register(name,()->net.minecraft.sounds.SoundEvent.createVariableRangeEvent(ResourceLocation.parse(ThaumicConcilium.ID+":"+name)));}
    private static <T extends Entity> RegistryObject<EntityType<T>> entity(String id,EntityType.EntityFactory<T> factory,MobCategory category,float width,float height,boolean fire){return ENTITIES.register(id,()->{var b=EntityType.Builder.of(factory,category).sized(width,height).clientTrackingRange(10);if(fire)b.fireImmune();return b.build(ThaumicConcilium.ID+":"+id);});}
    public static final java.util.Map<String,RegistryObject<Item>> EGGS=new java.util.LinkedHashMap<>();
    static {for(var entry:java.util.Map.of("dissolved",DISSOLVED,"samurai",SAMURAI,"overanimated",OVERANIMATED,"thaum_gib",THAUM_GIB,"dope_squid",DOPE_SQUID,"withered_botanist",WITHERED_BOTANIST,"chort",CHORT,"sloppy_alchemist",SLOPPY_ALCHEMIST,"burned_witch",BURNED_WITCH).entrySet())EGGS.put(entry.getKey(),ITEMS.register(entry.getKey()+"_spawn_egg",()->new ForgeSpawnEggItem(entry.getValue(),0x00ffff,0x00008b,new Item.Properties())));}
    public static boolean available(String id){return switch(id){case "chort","sloppy_alchemist"->WitcheryBridge.alchemistAvailable();case "burned_witch"->WitcheryBridge.available();case "samurai"->ModList.get().isLoaded("taintedmagic");case "overanimated","thaum_gib"->ModList.get().isLoaded("thaumichorizons");case "dope_squid"->ModList.get().isLoaded("thaumichorizons")&&ModList.get().isLoaded("thaumicbases")&&tobaccoEnabled();case "withered_botanist"->ModList.get().isLoaded("botania");default->true;};}
    public static boolean tobaccoEnabled(){if(!ModList.get().isLoaded("thaumicbases"))return false;try{return ((ForgeConfigSpec.BooleanValue)Class.forName("tbmodern.Configuration").getField("TOBACCO").get(null)).get();}catch(ReflectiveOperationException e){throw new IllegalStateException("Thaumic Bases configuration API changed",e);}}
    public static final ForgeConfigSpec SPEC;
    public static final java.util.Map<String,ForgeConfigSpec.IntValue> WEIGHTS=new java.util.LinkedHashMap<>();
    public static final java.util.Map<String,ForgeConfigSpec.ConfigValue<java.util.List<? extends String>>> BLACKLISTS=new java.util.LinkedHashMap<>();
    static{var b=new ForgeConfigSpec.Builder();for(String id:new String[]{"dissolved","samurai","overanimated","withered_botanist","sloppy_alchemist","burned_witch"}){b.push(id);WEIGHTS.put(id,b.comment("自然生成权重，0禁用，默认5。Spawn weight; zero disables, default 5.").defineInRange("weight",5,0,1000000));BLACKLISTS.put(id,b.comment("禁止生成的群系注册名。Excluded biome registry names.").defineListAllowEmpty("biomeBlacklist",java.util.List.of(),v->v instanceof String s&&ResourceLocation.tryParse(s)!=null));b.pop();}SPEC=b.build();}
    public static void register(IEventBus bus){ConciliumArcNetwork.init();bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(()->thaumcraft.api.research.ResearchCategories.registerEnablement("thaumicconcilium:DOPESQUID",()->available("dope_squid"))));ENTITIES.register(bus);ITEMS.register(bus);PARTICLES.register(bus);SOUNDS.register(bus);CODECS.register(bus);net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON,SPEC,"thaumicconcilium-remaining-mobs.toml");bus.addListener(RemainingMobs::attributes);bus.addListener(RemainingMobs::placements);bus.addListener((net.minecraftforge.event.BuildCreativeModeTabContentsEvent e)->{if(e.getTab()==ConciliumItems.TAB.get())EGGS.forEach((id,item)->{if(available(id))e.accept(item.get());});});}
    private static void attributes(EntityAttributeCreationEvent e){e.put(CHORT.get(),Chort.attributes().build());e.put(SLOPPY_ALCHEMIST.get(),SloppyAlchemist.attributes().build());e.put(BURNED_WITCH.get(),BurnedWitch.attributes().build());e.put(DISSOLVED.get(),Dissolved.attributes().build());e.put(SAMURAI.get(),Samurai.attributes().build());e.put(OVERANIMATED.get(),Overanimated.attributes().build());e.put(THAUM_GIB.get(),ThaumGib.attributes().build());e.put(DOPE_SQUID.get(),DopeSquid.attributes().build());e.put(WITHERED_BOTANIST.get(),WitheredBotanist.attributes().build());}
    private static void placements(SpawnPlacementRegisterEvent e){e.register(BURNED_WITCH.get(),SpawnPlacements.Type.NO_RESTRICTIONS,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(type,level,reason,pos,random)->WitcheryBridge.available()&&level.getDifficulty()!=net.minecraft.world.Difficulty.PEACEFUL&&Mob.checkMobSpawnRules(type,level,reason,pos,random),SpawnPlacementRegisterEvent.Operation.REPLACE);for(var t:java.util.List.of(DISSOLVED.get(),SAMURAI.get(),OVERANIMATED.get(),SLOPPY_ALCHEMIST.get()))e.register(t,SpawnPlacements.Type.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,Monster::checkMonsterSpawnRules,SpawnPlacementRegisterEvent.Operation.REPLACE);e.register(WITHERED_BOTANIST.get(),SpawnPlacements.Type.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,WitheredBotanist::spawnRules,SpawnPlacementRegisterEvent.Operation.REPLACE);}
    @Override public Codec<? extends BiomeModifier> codec(){return CODEC.get();}
    @Override public void modify(Holder<Biome> biome,Phase phase,ModifiableBiomeInfo.BiomeInfo.Builder builder){if(phase!=Phase.ADD)return;for(String id:WEIGHTS.keySet()){int weight=WEIGHTS.get(id).get();if(weight<=0||!available(id)||((id.equals("sloppy_alchemist")||id.equals("burned_witch"))&&(!WitcheryBridge.alchemistAvailable()||!biome.is(net.minecraft.tags.BiomeTags.IS_NETHER)))||biome.unwrapKey().map(k->BLACKLISTS.get(id).get().contains(k.location().toString())).orElse(false))continue;EntityType<?> type=switch(id){case "sloppy_alchemist"->SLOPPY_ALCHEMIST.get();case "burned_witch"->BURNED_WITCH.get();case "samurai"->SAMURAI.get();case "overanimated"->OVERANIMATED.get();case "withered_botanist"->WITHERED_BOTANIST.get();default->DISSOLVED.get();};builder.getMobSpawnSettings().addSpawn(type.getCategory(),new MobSpawnSettings.SpawnerData(type,weight,id.equals("samurai")?3:1,id.equals("samurai")?5:(id.equals("withered_botanist")||id.equals("sloppy_alchemist")||id.equals("burned_witch"))?1:2));}}
}
