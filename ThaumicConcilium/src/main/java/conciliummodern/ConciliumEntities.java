package conciliummodern;

import net.minecraft.world.entity.*;
import net.minecraftforge.registries.*;

public final class ConciliumEntities {
    public static final RegistryObject<EntityType<RedPoweredMind>> RED_MIND;
    public static final DeferredRegister<EntityType<?>> TYPES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,ThaumicConcilium.ID);
    static {RED_MIND=TYPES.register("red_powered_mind",()->EntityType.Builder.<RedPoweredMind>of(RedPoweredMind::new,MobCategory.MISC).sized(1,1).clientTrackingRange(8).updateInterval(3).build(ThaumicConcilium.ID+":red_powered_mind"));}
    public static final RegistryObject<EntityType<ClearWaterProjectile>> CLEAR_WATER=TYPES.register("clear_water_bottle",()->EntityType.Builder
            .<ClearWaterProjectile>of(ClearWaterProjectile::new,MobCategory.MISC).sized(.25f,.25f)
            .clientTrackingRange(4).updateInterval(10).build(ThaumicConcilium.ID+":clear_water_bottle"));
    private ConciliumEntities() {}
    public static final RegistryObject<EntityType<ProtobodyDrop>> PROTOBODY_DROP=TYPES.register("protobody_item",()->EntityType.Builder
            .<ProtobodyDrop>of(ProtobodyDrop::new,MobCategory.MISC).sized(.25f,.25f).clientTrackingRange(6).updateInterval(10).build(ThaumicConcilium.ID+":protobody_item"));
    public static final RegistryObject<EntityType<Protolimb>> PROTOLIMB=TYPES.register("protolimb",()->EntityType.Builder
            .<Protolimb>of(Protolimb::new,MobCategory.MONSTER).sized(.6f,.5f).fireImmune().clientTrackingRange(8).updateInterval(3).build(ThaumicConcilium.ID+":protolimb"));
    public static final RegistryObject<EntityType<QuicksilverElemental>> QUICKSILVER_ELEMENTAL=TYPES.register("quicksilver_elemental",()->EntityType.Builder
            .<QuicksilverElemental>of(QuicksilverElemental::new,MobCategory.MONSTER).sized(.6f,1.8f).clientTrackingRange(8).updateInterval(3).build(ThaumicConcilium.ID+":quicksilver_elemental"));
    public static final RegistryObject<EntityType<ShardPowder>> SHARD_POWDER=TYPES.register("shard_powder",()->EntityType.Builder
            .<ShardPowder>of(ShardPowder::new,MobCategory.MISC).sized(.125f,.125f).clientTrackingRange(8).updateInterval(1).build(ThaumicConcilium.ID+":shard_powder"));
    public static final RegistryObject<EntityType<DistortionField>> DISTORTION_FIELD=TYPES.register("distortion_field",()->EntityType.Builder
            .<DistortionField>of(DistortionField::new,MobCategory.MISC).sized(3,3).noSave().clientTrackingRange(8).updateInterval(1).build(ThaumicConcilium.ID+":distortion_field"));
    public static final RegistryObject<EntityType<WarpCharge>> WARP_CHARGE=TYPES.register("concentrated_warp_charge",()->EntityType.Builder
            .<WarpCharge>of(WarpCharge::new,MobCategory.MISC).sized(1,1).clientTrackingRange(8).updateInterval(1).build(ThaumicConcilium.ID+":concentrated_warp_charge"));
    public static final RegistryObject<EntityType<WrathGlyph>> WRATH_GLYPH=TYPES.register("wrath_glyph",()->EntityType.Builder
            .<WrathGlyph>of(WrathGlyph::new,MobCategory.MISC).sized(.1f,.1f).clientTrackingRange(8).updateInterval(1).build(ThaumicConcilium.ID+":wrath_glyph"));
    public static final RegistryObject<EntityType<SmartTaintSpider>> SMART_TAINT_SPIDER=TYPES.register("smart_taint_spider",()->EntityType.Builder
            .<SmartTaintSpider>of(SmartTaintSpider::new,MobCategory.MONSTER).sized(.6f,.5f).clientTrackingRange(8).updateInterval(3).build(ThaumicConcilium.ID+":smart_taint_spider"));
    public static final RegistryObject<EntityType<MaterialPeeler>> PEELER=TYPES.register("material_peeler",()->EntityType.Builder
            .<MaterialPeeler>of(MaterialPeeler::new,MobCategory.MISC).sized(1,1).fireImmune().clientTrackingRange(8).updateInterval(3).build(ThaumicConcilium.ID+":material_peeler"));
    public static final RegistryObject<EntityType<BrightestOne>> BRIGHTEST=TYPES.register("brightest_one",()->EntityType.Builder
            .<BrightestOne>of(BrightestOne::new,MobCategory.MONSTER).sized(3,3).fireImmune().clientTrackingRange(8).updateInterval(1).build(ThaumicConcilium.ID+":brightest_one"));
    public static final RegistryObject<EntityType<FireShardPowder>> FIRE_POWDER=TYPES.register("fire_shard_powder",()->EntityType.Builder
            .<FireShardPowder>of(FireShardPowder::new,MobCategory.MISC).sized(.125f,.125f).clientTrackingRange(8).updateInterval(1).build(ThaumicConcilium.ID+":fire_shard_powder"));
    public static final RegistryObject<EntityType<NetherExplorer>> NETHER_EXPLORER=TYPES.register("nether_explorer",()->EntityType.Builder
            .<NetherExplorer>of(NetherExplorer::new,MobCategory.MISC).sized(2,.3f).fireImmune()
            .clientTrackingRange(8).updateInterval(3).build(ThaumicConcilium.ID+":nether_explorer"));
    public static final RegistryObject<EntityType<RiftEntity>> RIFT=TYPES.register("rift",()->EntityType.Builder
            .<RiftEntity>of(RiftEntity::new,MobCategory.MISC).sized(2,2).fireImmune()
            .clientTrackingRange(10).updateInterval(3).build(ThaumicConcilium.ID+":rift"));
    public static final RegistryObject<EntityType<Thaumaturge>> THAUMATURGE=TYPES.register("thaumaturge",()->EntityType.Builder
            .<Thaumaturge>of(Thaumaturge::new,MobCategory.CREATURE).sized(.6f,1.8f)
            .clientTrackingRange(8).updateInterval(3).build(ThaumicConcilium.ID+":thaumaturge"));
    public static final RegistryObject<EntityType<PositiveBurstOrb>> POSITIVE_ORB=TYPES.register("positive_burst_orb",()->EntityType.Builder
            .<PositiveBurstOrb>of(PositiveBurstOrb::new,MobCategory.MISC).sized(.125f,.125f)
            .clientTrackingRange(8).updateInterval(1).build(ThaumicConcilium.ID+":positive_burst_orb"));
    public static final RegistryObject<EntityType<CompressedBlast>> COMPRESSED_BLAST=TYPES.register("compressed_blast",()->EntityType.Builder
            .<CompressedBlast>of(CompressedBlast::new,MobCategory.MISC).sized(.25f,.25f)
            .clientTrackingRange(8).updateInterval(1).build(ThaumicConcilium.ID+":compressed_blast"));
    public static final RegistryObject<EntityType<MadThaumaturge>> MAD_THAUMATURGE=TYPES.register("mad_thaumaturge",()->EntityType.Builder
            .<MadThaumaturge>of(MadThaumaturge::new,MobCategory.MONSTER).sized(.6f,1.8f)
            .clientTrackingRange(8).updateInterval(1).build(ThaumicConcilium.ID+":mad_thaumaturge"));
    public static final RegistryObject<EntityType<ChargedWisp>> CHARGED_WISP=TYPES.register("charged_wisp",()->EntityType.Builder
            .<ChargedWisp>of(ChargedWisp::new,MobCategory.MISC).sized(.25f,.25f)
            .clientTrackingRange(8).updateInterval(1).build(ThaumicConcilium.ID+":charged_wisp"));
    public static final RegistryObject<EntityType<ThickTaintProjectile>> THICK_TAINT=TYPES.register("thick_taint_bottle",()->EntityType.Builder
            .<ThickTaintProjectile>of(ThickTaintProjectile::new,MobCategory.MISC).sized(.25f,.25f)
            .clientTrackingRange(4).updateInterval(10).build(ThaumicConcilium.ID+":thick_taint_bottle"));
}
