package tc4revival.content;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import tc4revival.TC4Revival;

public  class TCEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TC4Revival.MOD_ID);
    public static final RegistryObject<EntityType<TaintBottleEntity>> TAINT_BOTTLE=ENTITIES.register("taint_bottle",()->EntityType.Builder.<TaintBottleEntity>of(TaintBottleEntity::new,MobCategory.MISC).sized(.25f,.25f).clientTrackingRange(4).updateInterval(3).build("thaumcraft:taint_bottle"));
    public static final RegistryObject<EntityType<PrimalArrowEntity>> PRIMAL_ARROW=ENTITIES.register("primal_arrow",
            ()->EntityType.Builder.<PrimalArrowEntity>of(PrimalArrowEntity::new,MobCategory.MISC).sized(.5f,.5f).clientTrackingRange(4).updateInterval(20).build("thaumcraft:primal_arrow"));
    public static final RegistryObject<EntityType<TravelingTrunkEntity>> TRAVELING_TRUNK=ENTITIES.register("traveling_trunk",
            ()->EntityType.Builder.<TravelingTrunkEntity>of(TravelingTrunkEntity::new,MobCategory.CREATURE).sized(.8f,.8f).clientTrackingRange(8).updateInterval(3).build("thaumcraft:traveling_trunk"));
    public static final RegistryObject<EntityType<GolemDartEntity>> GOLEM_DART=ENTITIES.register("golem_dart",
            ()->EntityType.Builder.<GolemDartEntity>of(GolemDartEntity::new,MobCategory.MISC).sized(.5f,.5f).clientTrackingRange(4).updateInterval(3).build("thaumcraft:golem_dart"));
    public static final RegistryObject<EntityType<GolemEntity>> GOLEM=ENTITIES.register("golem",
            ()->EntityType.Builder.<GolemEntity>of(GolemEntity::new,MobCategory.CREATURE).sized(.4f,.95f).clientTrackingRange(8).updateInterval(3).build("thaumcraft:golem"));
    public static final RegistryObject<EntityType<GolemBobberEntity>> GOLEM_BOBBER=ENTITIES.register("golem_bobber",
            ()->EntityType.Builder.<GolemBobberEntity>of(GolemBobberEntity::new,MobCategory.MISC).sized(.25f,.25f).clientTrackingRange(4).updateInterval(3).build("thaumcraft:golem_bobber"));
    public static final RegistryObject<EntityType<FollowingItemEntity>> FOLLOWING_ITEM=ENTITIES.register("following_item",
            ()->EntityType.Builder.<FollowingItemEntity>of(FollowingItemEntity::new,MobCategory.MISC)
                .sized(.25f,.25f).clientTrackingRange(6).updateInterval(3).build("thaumcraft:following_item"));
    public static final RegistryObject<EntityType<SpecialItemEntity>> SPECIAL_ITEM = ENTITIES.register("special_item",
            () -> EntityType.Builder.<SpecialItemEntity>of(SpecialItemEntity::new, MobCategory.MISC)
                    .sized(.25f, .25f).clientTrackingRange(6).updateInterval(20).build("thaumcraft:special_item"));
    public static final RegistryObject<EntityType<GrateItemEntity>> GRATE_ITEM=ENTITIES.register("grate_item",
            ()->EntityType.Builder.<GrateItemEntity>of(GrateItemEntity::new,MobCategory.MISC)
                    .sized(.25f,.25f).clientTrackingRange(4).updateInterval(20).build("thaumcraft:grate_item"));
    public static final RegistryObject<EntityType<PechEntity>> PECH=ENTITIES.register("pech",()->EntityType.Builder.<PechEntity>of(PechEntity::new,MobCategory.MONSTER).sized(.6f,1.8f).clientTrackingRange(4).updateInterval(3).build("thaumcraft:pech"));
    public static final RegistryObject<EntityType<MindSpiderEntity>> MIND_SPIDER=ENTITIES.register("mind_spider",
            ()->EntityType.Builder.<MindSpiderEntity>of(MindSpiderEntity::new,MobCategory.MONSTER).sized(.3f,.3f).clientTrackingRange(6).updateInterval(3).build("thaumcraft:mind_spider"));
    public static final RegistryObject<EntityType<BrainyZombieEntity>> BRAINY_ZOMBIE=ENTITIES.register("brainy_zombie",()->EntityType.Builder.<BrainyZombieEntity>of(BrainyZombieEntity::new,MobCategory.MONSTER).sized(.6f,1.8f).clientTrackingRange(4).updateInterval(3).build("thaumcraft:brainy_zombie"));
    public static final RegistryObject<EntityType<GiantBrainyZombieEntity>> GIANT_BRAINY_ZOMBIE=ENTITIES.register("giant_brainy_zombie",()->EntityType.Builder.<GiantBrainyZombieEntity>of(GiantBrainyZombieEntity::new,MobCategory.MONSTER).sized(.6f,1.8f).clientTrackingRange(4).updateInterval(3).build("thaumcraft:giant_brainy_zombie"));
    public static final RegistryObject<EntityType<WispEntity>> WISP=ENTITIES.register("wisp",()->EntityType.Builder.<WispEntity>of(WispEntity::new,MobCategory.MONSTER).sized(.9f,.9f).clientTrackingRange(4).updateInterval(3).build("thaumcraft:wisp"));
    public static final RegistryObject<EntityType<TaintedCreeperEntity>> TAINTED_CREEPER=ENTITIES.register("tainted_creeper",()->EntityType.Builder.<TaintedCreeperEntity>of(TaintedCreeperEntity::new,MobCategory.MONSTER).sized(.6f,1.8f).clientTrackingRange(4).updateInterval(3).build("thaumcraft:tainted_creeper"));
    public static final RegistryObject<EntityType<TaintedAnimalEntity>> TAINTED_CHICKEN=tainted(TaintedAnimalEntity.Kind.CHICKEN);
    public static final RegistryObject<EntityType<TaintedAnimalEntity>> TAINTED_COW=tainted(TaintedAnimalEntity.Kind.COW);
    public static final RegistryObject<EntityType<TaintedAnimalEntity>> TAINTED_PIG=tainted(TaintedAnimalEntity.Kind.PIG);
    public static final RegistryObject<EntityType<TaintedAnimalEntity>> TAINTED_SHEEP=tainted(TaintedAnimalEntity.Kind.SHEEP);
    public static final RegistryObject<EntityType<TaintedAnimalEntity>> TAINTED_VILLAGER=tainted(TaintedAnimalEntity.Kind.VILLAGER);
    private static RegistryObject<EntityType<TaintedAnimalEntity>> tainted(TaintedAnimalEntity.Kind kind) {
        return ENTITIES.register(kind.id(),()->EntityType.Builder.<TaintedAnimalEntity>of((type,level)->new TaintedAnimalEntity(type,level,kind),MobCategory.MONSTER)
                .sized(kind.width,kind.height).clientTrackingRange(4).updateInterval(3).build("thaumcraft:"+kind.id()));
    }
    public static EntityType<? extends TaintedAnimalEntity> taintedAnimal(TaintedAnimalEntity.Kind kind) {
        return switch(kind) {case CHICKEN->TAINTED_CHICKEN.get();case COW->TAINTED_COW.get();case PIG->TAINTED_PIG.get();
            case SHEEP->TAINTED_SHEEP.get();case VILLAGER->TAINTED_VILLAGER.get();case CREEPER->TAINTED_CREEPER.get();};
    }
    public static final RegistryObject<EntityType<TaintHiveEntity>> TAINT_HIVE=ENTITIES.register("taint_hive",()->EntityType.Builder.<TaintHiveEntity>of(TaintHiveEntity::new,MobCategory.MONSTER).sized(1,1).clientTrackingRange(4).updateInterval(20).setShouldReceiveVelocityUpdates(false).build("thaumcraft:taint_hive"));
    public static final RegistryObject<EntityType<TaintSwarmEntity>> TAINT_SWARM=ENTITIES.register("taint_swarm",()->EntityType.Builder.<TaintSwarmEntity>of(TaintSwarmEntity::new,MobCategory.MONSTER).sized(2,2).clientTrackingRange(4).updateInterval(3).build("thaumcraft:taint_swarm"));
    private TCEntities() {}
    public static final RegistryObject<EntityType<PermanentItemEntity>> PERMANENT_ITEM=ENTITIES.register("permanent_item",
            ()->EntityType.Builder.<PermanentItemEntity>of(PermanentItemEntity::new,MobCategory.MISC).sized(.25f,.25f).clientTrackingRange(6).updateInterval(20).build("thaumcraft:permanent_item"));
    public static final RegistryObject<EntityType<TaintacleEntity>> TAINTACLE=ENTITIES.register("taintacle",
            ()->EntityType.Builder.<TaintacleEntity>of(TaintacleEntity::new,MobCategory.MONSTER).sized(.66f,3).clientTrackingRange(8).updateInterval(3).build("thaumcraft:taintacle"));
    public static final RegistryObject<EntityType<SmallTaintacleEntity>> TAINTACLE_SMALL=ENTITIES.register("taintacle_small",
            ()->EntityType.Builder.<SmallTaintacleEntity>of(SmallTaintacleEntity::new,MobCategory.MONSTER).sized(.22f,1).clientTrackingRange(6).updateInterval(3).build("thaumcraft:taintacle_small"));
    public static final RegistryObject<EntityType<GiantTaintacleEntity>> TAINTACLE_GIANT=ENTITIES.register("taintacle_giant",
            ()->EntityType.Builder.<GiantTaintacleEntity>of(GiantTaintacleEntity::new,MobCategory.MONSTER).sized(1.1f,6).clientTrackingRange(10).updateInterval(3).build("thaumcraft:taintacle_giant"));
    public static final RegistryObject<EntityType<CultistPortalEntity>> CULTIST_PORTAL=ENTITIES.register("cultist_portal",
            ()->EntityType.Builder.<CultistPortalEntity>of(CultistPortalEntity::new,MobCategory.MONSTER).sized(1.5f,3).fireImmune().clientTrackingRange(8).updateInterval(3).build("thaumcraft:cultist_portal"));
    public static final RegistryObject<EntityType<CultistKnightEntity>> CULTIST_KNIGHT=ENTITIES.register("cultist_knight",
            ()->EntityType.Builder.<CultistKnightEntity>of(CultistKnightEntity::new,MobCategory.MONSTER).sized(.6f,1.8f).clientTrackingRange(6).updateInterval(3).build("thaumcraft:cultist_knight"));
    public static final RegistryObject<EntityType<CultistClericEntity>> CULTIST_CLERIC=ENTITIES.register("cultist_cleric",
            ()->EntityType.Builder.<CultistClericEntity>of(CultistClericEntity::new,MobCategory.MONSTER).sized(.6f,1.8f).clientTrackingRange(6).updateInterval(3).build("thaumcraft:cultist_cleric"));
    public static final RegistryObject<EntityType<CultistLeaderEntity>> CULTIST_LEADER=ENTITIES.register("cultist_leader",
            ()->EntityType.Builder.<CultistLeaderEntity>of(CultistLeaderEntity::new,MobCategory.MONSTER).sized(.75f,2.25f).clientTrackingRange(6).updateInterval(3).build("thaumcraft:cultist_leader"));
    public static final RegistryObject<EntityType<InhabitedZombieEntity>> INHABITED_ZOMBIE=ENTITIES.register("inhabited_zombie",
            ()->EntityType.Builder.<InhabitedZombieEntity>of(InhabitedZombieEntity::new,MobCategory.MONSTER).sized(.6f,1.8f).clientTrackingRange(6).updateInterval(3).build("thaumcraft:inhabited_zombie"));
    public static final RegistryObject<EntityType<EldritchGolemEntity>> ELDRITCH_GOLEM=ENTITIES.register("eldritch_golem",
            ()->EntityType.Builder.<EldritchGolemEntity>of(EldritchGolemEntity::new,MobCategory.MONSTER).sized(1.75f,3.5f).fireImmune().clientTrackingRange(8).updateInterval(3).build("thaumcraft:eldritch_golem"));
    public static final RegistryObject<EntityType<GolemOrbEntity>> GOLEM_ORB=ENTITIES.register("golem_orb",
            ()->EntityType.Builder.<GolemOrbEntity>of(GolemOrbEntity::new,MobCategory.MISC).sized(.25f,.25f).clientTrackingRange(4).updateInterval(3).build("thaumcraft:golem_orb"));
    public static final RegistryObject<EntityType<EldritchWardenEntity>> ELDRITCH_WARDEN=ENTITIES.register("eldritch_warden",
            ()->EntityType.Builder.<EldritchWardenEntity>of(EldritchWardenEntity::new,MobCategory.MONSTER).sized(1.5f,3.5f).clientTrackingRange(8).updateInterval(3).build("thaumcraft:eldritch_warden"));
    public static final RegistryObject<EntityType<EldritchCrabEntity>> ELDRITCH_CRAB=ENTITIES.register("eldritch_crab",
            ()->EntityType.Builder.<EldritchCrabEntity>of(EldritchCrabEntity::new,MobCategory.MONSTER).sized(.8f,.6f).clientTrackingRange(5).updateInterval(3).build("thaumcraft:eldritch_crab"));
    public static final RegistryObject<EntityType<EldritchGuardianEntity>> ELDRITCH_GUARDIAN=ENTITIES.register("eldritch_guardian",
            ()->EntityType.Builder.<EldritchGuardianEntity>of(EldritchGuardianEntity::new,MobCategory.MONSTER).sized(.8f,2.25f).clientTrackingRange(6).updateInterval(3).build("thaumcraft:eldritch_guardian"));
    public static final RegistryObject<EntityType<EldritchOrbEntity>> ELDRITCH_ORB=ENTITIES.register("eldritch_orb",
            ()->EntityType.Builder.<EldritchOrbEntity>of(EldritchOrbEntity::new,MobCategory.MISC).sized(.25f,.25f).clientTrackingRange(4).updateInterval(3).build("thaumcraft:eldritch_orb"));
    public static final RegistryObject<EntityType<AlumentumEntity>> ALUMENTUM = ENTITIES.register("alumentum",
            ()->EntityType.Builder.<AlumentumEntity>of(AlumentumEntity::new,MobCategory.MISC).sized(.25f,.25f).clientTrackingRange(4).updateInterval(20).build("thaumcraft:alumentum"));
    public static final RegistryObject<EntityType<PrimalOrbEntity>> PRIMAL_ORB = ENTITIES.register("primal_orb",
            () -> EntityType.Builder.<PrimalOrbEntity>of(PrimalOrbEntity::new,MobCategory.MISC)
                    .sized(.25f,.25f).clientTrackingRange(4).updateInterval(3).build("thaumcraft:primal_orb"));
    public static final RegistryObject<EntityType<FallingTaintEntity>> FALLING_TAINT = ENTITIES.register("falling_taint",
            () -> EntityType.Builder.<FallingTaintEntity>of(FallingTaintEntity::new,MobCategory.MISC)
                    .sized(.98f,.98f).clientTrackingRange(5).updateInterval(3).build("thaumcraft:falling_taint"));
    public static final RegistryObject<EntityType<TaintSporeEntity>> TAINT_SPORE = ENTITIES.register("taint_spore",
            () -> EntityType.Builder.<TaintSporeEntity>of(TaintSporeEntity::new,MobCategory.MONSTER)
                    .sized(.5f,.5f).clientTrackingRange(5).updateInterval(3).build("thaumcraft:taint_spore"));
    public static final RegistryObject<EntityType<TaintSpiderEntity>> TAINT_SPIDER = ENTITIES.register("taint_spider",
            () -> EntityType.Builder.<TaintSpiderEntity>of(TaintSpiderEntity::new,MobCategory.MONSTER)
                    .sized(.4f,.3f).clientTrackingRange(5).updateInterval(3).build("thaumcraft:taint_spider"));
    public static final RegistryObject<EntityType<ThaumicSlimeEntity>> THAUMIC_SLIME = ENTITIES.register("thaumic_slime",
            () -> EntityType.Builder.<ThaumicSlimeEntity>of(ThaumicSlimeEntity::new, MobCategory.MONSTER)
                    .sized(.5f,.5f).clientTrackingRange(5).updateInterval(3).build("thaumcraft:thaumic_slime"));
    public static final RegistryObject<EntityType<FireBatEntity>> FIRE_BAT = ENTITIES.register("fire_bat",
            () -> EntityType.Builder.<FireBatEntity>of(FireBatEntity::new, MobCategory.MONSTER)
                    .sized(.5f, .9f).fireImmune().clientTrackingRange(5).updateInterval(3).build("thaumcraft:fire_bat"));
    public static final RegistryObject<EntityType<PechBlastEntity>> PECH_BLAST = ENTITIES.register("pech_blast",
            () -> EntityType.Builder.<PechBlastEntity>of(PechBlastEntity::new, MobCategory.MISC)
                    .sized(.25f, .25f).clientTrackingRange(4).updateInterval(3).build("thaumcraft:pech_blast"));
    public static final RegistryObject<EntityType<FrostShardEntity>> FROST_SHARD = ENTITIES.register("frost_shard",
            () -> EntityType.Builder.<FrostShardEntity>of(FrostShardEntity::new, MobCategory.MISC)
                    .sized(.25f, .25f).clientTrackingRange(4).updateInterval(3).build("thaumcraft:frost_shard"));
    public static final RegistryObject<EntityType<EmberEntity>> EMBER = ENTITIES.register("ember",
            () -> EntityType.Builder.<EmberEntity>of(EmberEntity::new, MobCategory.MISC)
                    .sized(.25f, .25f).clientTrackingRange(4).updateInterval(3).build("thaumcraft:ember"));
    public static final RegistryObject<EntityType<ExplosiveOrbEntity>> EXPLOSIVE_ORB = ENTITIES.register("explosive_orb",
            () -> EntityType.Builder.<ExplosiveOrbEntity>of(ExplosiveOrbEntity::new, MobCategory.MISC)
                    .sized(.25f, .25f).clientTrackingRange(4).updateInterval(3).build("thaumcraft:explosive_orb"));
    public static final RegistryObject<EntityType<ShockOrbEntity>> SHOCK_ORB = ENTITIES.register("shock_orb",
            () -> EntityType.Builder.<ShockOrbEntity>of(ShockOrbEntity::new, MobCategory.MISC)
                    .sized(.25f, .25f).clientTrackingRange(4).updateInterval(3).build("thaumcraft:shock_orb"));
    public static final RegistryObject<EntityType<AspectOrbEntity>> ASPECT_ORB = ENTITIES.register("aspect_orb",
            () -> EntityType.Builder.<AspectOrbEntity>of(AspectOrbEntity::new, MobCategory.MISC)
                    .sized(.125f, .125f).clientTrackingRange(4).updateInterval(20).build("thaumcraft:aspect_orb"));
    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event)->{event.put(TAINT_HIVE.get(),TaintHiveEntity.attributes().build());event.put(TAINT_SWARM.get(),TaintSwarmEntity.attributes().build());});
        bus.addListener((net.minecraftforge.event.entity.SpawnPlacementRegisterEvent event)->event.register(TAINT_SWARM.get(),SpawnPlacements.Type.NO_RESTRICTIONS,net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,TaintSwarmEntity::maySpawn,net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE));
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event)-> {
            for(var kind:TaintedAnimalEntity.Kind.values())event.put(taintedAnimal(kind),TaintedAnimalEntity.attributes(kind).build());
        });
        bus.addListener((net.minecraftforge.event.entity.SpawnPlacementRegisterEvent event)->event.register(TAINTACLE.get(),SpawnPlacements.Type.NO_RESTRICTIONS,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,TaintacleSpawning::maySpawn,net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE));
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event)->{ event.put(TAINTACLE.get(),TaintacleEntity.attributes().build()); event.put(TAINTACLE_SMALL.get(),SmallTaintacleEntity.attributes().build()); event.put(TAINTACLE_GIANT.get(),GiantTaintacleEntity.attributes().build()); });
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event)->event.put(CULTIST_PORTAL.get(),CultistPortalEntity.attributes().build()));
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event)->event.put(CULTIST_KNIGHT.get(),CultistKnightEntity.attributes().build()));
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event)->event.put(CULTIST_CLERIC.get(),CultistClericEntity.attributes().build()));
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event)->event.put(CULTIST_LEADER.get(),CultistLeaderEntity.attributes().build()));
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event)->event.put(INHABITED_ZOMBIE.get(),InhabitedZombieEntity.attributes().build()));
        bus.addListener((net.minecraftforge.event.entity.SpawnPlacementRegisterEvent event)->event.register(INHABITED_ZOMBIE.get(),SpawnPlacements.Type.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type,level,reason,pos,random)->EerieSpawning.legacyDarkness(level,pos,random) && net.minecraft.world.entity.Mob.checkMobSpawnRules(type,level,reason,pos,random)
                        && level.getLevel().getEntitiesOfClass(InhabitedZombieEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(32,16,32)).isEmpty(),
                net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE));
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event)->event.put(ELDRITCH_GOLEM.get(),EldritchGolemEntity.attributes().build()));
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event)->event.put(GOLEM.get(),GolemEntity.attributes().build()));
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event)->event.put(TRAVELING_TRUNK.get(),TravelingTrunkEntity.attributes().build()));
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event)->event.put(ELDRITCH_WARDEN.get(),EldritchWardenEntity.attributes().build()));
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event)->event.put(ELDRITCH_CRAB.get(),EldritchCrabEntity.attributes().build()));
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event)->event.put(ELDRITCH_GUARDIAN.get(),EldritchGuardianEntity.attributes().build()));
        bus.addListener((net.minecraftforge.event.entity.SpawnPlacementRegisterEvent event)->event.register(ELDRITCH_GUARDIAN.get(),net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(type,level,reason,pos,random)->level.getDifficulty()!=net.minecraft.world.Difficulty.PEACEFUL
                        && net.minecraft.world.entity.Mob.checkMobSpawnRules(type,level,reason,pos,random)
                        && level.getLevel().getEntitiesOfClass(EldritchGuardianEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(32,16,32)).isEmpty(),
                net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE));
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event)->{event.put(BRAINY_ZOMBIE.get(),BrainyZombieEntity.attributes().build());event.put(GIANT_BRAINY_ZOMBIE.get(),GiantBrainyZombieEntity.attributes().build());event.put(WISP.get(),WispEntity.attributes().build());});
        bus.addListener((net.minecraftforge.event.entity.SpawnPlacementRegisterEvent event)->{
            var height=net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;var op=net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE;
            event.register(BRAINY_ZOMBIE.get(),SpawnPlacements.Type.ON_GROUND,height,EerieSpawning::zombie,op);
            event.register(GIANT_BRAINY_ZOMBIE.get(),SpawnPlacements.Type.ON_GROUND,height,EerieSpawning::zombie,op);
            event.register(WISP.get(),SpawnPlacements.Type.NO_RESTRICTIONS,height,EerieSpawning::wisp,op);
            event.register(FIRE_BAT.get(),SpawnPlacements.Type.ON_GROUND,height,FireBatSpawning::maySpawn,op);
        });
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event)->event.put(PECH.get(),PechEntity.attributes().build()));
        bus.addListener((net.minecraftforge.event.entity.SpawnPlacementRegisterEvent event)->event.register(PECH.get(),net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,PechSpawning::maySpawn,net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE));
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event) -> event.put(TAINT_SPORE.get(),TaintSporeEntity.attributes().build()));
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event) -> event.put(TAINT_SPIDER.get(),TaintSpiderEntity.attributes().build()));
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event) -> event.put(MIND_SPIDER.get(),MindSpiderEntity.attributes().build()));
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event) -> event.put(THAUMIC_SLIME.get(), ThaumicSlimeEntity.attributes().build()));
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event) -> event.put(FIRE_BAT.get(), FireBatEntity.attributes().build()));
    }
}
