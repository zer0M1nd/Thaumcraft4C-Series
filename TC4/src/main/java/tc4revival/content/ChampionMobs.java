package tc4revival.content;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.*;
import tc4revival.config.TCConfig;

/** Shared champion selection, attribute persistence and combat effects. */
@Mod.EventBusSubscriber(modid="thaumcraft")
public  class ChampionMobs {
    public enum Kind { BOLD,SPINED,ARMORED,MIGHTY,GRIM,WARDED,WARP,UNDYING,FIERY,SICKLY,VENOMOUS,VAMPIRIC,INFESTED }
    private static final String[] NAMES={"bold","spine","armor","mighty","grim","warded","warp","undying","fiery","sickly","venomous","vampiric","infested"};
    private static final DeferredRegister<Attribute> ATTRIBUTES=DeferredRegister.create(ForgeRegistries.ATTRIBUTES,"thaumcraft");
    public static final RegistryObject<Attribute> CHAMPION=ATTRIBUTES.register("champion",()->new RangedAttribute("attribute.name.thaumcraft.champion",-2,-2,100).setSyncable(true));
    private static final AttributeModifier HEALTH=modifier("a62bef38-48cc-42a6-ac5e-ef913841c4fd","Champion health",30,AttributeModifier.Operation.ADDITION);
    private static final AttributeModifier DAMAGE=modifier("a340d2db-d881-4c25-ac62-f0ad14cd63b0","Champion damage",2,AttributeModifier.Operation.MULTIPLY_TOTAL);
    private static final AttributeModifier BOLD=modifier("4b1edd33-caa9-47ae-a702-d86c05701037","Bold",.3,AttributeModifier.Operation.MULTIPLY_BASE);
    private static final AttributeModifier MIGHTY=modifier("7163897f-07f5-49b3-9ce4-b74beb83d2d3","Mighty",3,AttributeModifier.Operation.MULTIPLY_TOTAL);
    private static final String[] IDS={"40289aa1-907f-4ac6-ad79-e6681efe2cbc","365eead5-3f15-42a8-9e68-36100faef945","4e23758d-348e-42a8-8de6-08ae0a59033c","6d2ffe79-f034-4a06-b288-e1916c21e385","0f23321e-f921-4246-90b8-21ef202de224","b622c4d8-abc6-4db7-b3ee-5cf71b8e5286","107da049-af7a-4409-989a-6de23c8fe036","cb9484d3-6255-4893-a4f2-3ecc375692ee","6b567fdf-9245-48f5-8314-f93fe5db1427","b5718868-9ab0-424c-af1f-8b35e836b46e","ab9a132e-c619-4c0a-a103-10cbbcfba1a2","3412251e-af81-4c3c-93ba-2e1c33b049ea","9c577fbe-ddbc-4ea2-a661-770ea775f43b"};
    public static void register(IEventBus bus) {
        ATTRIBUTES.register(bus);
        bus.addListener((EntityAttributeModificationEvent event)->{
            for (var type:event.getTypes()) if (event.has(type,Attributes.ATTACK_DAMAGE)) event.add(type,CHAMPION.get());
        });
    }
    private static AttributeModifier modifier(String id,String name,double amount,AttributeModifier.Operation operation) { return new AttributeModifier(UUID.fromString(id),name,amount,operation); }
    private static void replace(AttributeInstance attribute,AttributeModifier modifier) { if (attribute!=null) { attribute.removeModifier(modifier); attribute.addPermanentModifier(modifier); } }
    public static int id(LivingEntity entity) { var value=entity.getAttribute(CHAMPION.get()); return value==null ? -2 : (int)value.getValue(); }
    public static Kind kind(LivingEntity entity) { int id=id(entity); return id>=0 && id<Kind.values().length ? Kind.values()[id] : null; }
    public static Component name(Kind kind) { return Component.translatable("champion.mod."+NAMES[kind.ordinal()]); }
    public static void make(Monster entity,boolean persistent) {
        int selected=entity.getRandom().nextInt(Kind.values().length);
        if (entity instanceof net.minecraft.world.entity.monster.Creeper) selected=0;
        make(entity,Kind.values()[selected],persistent);
    }
    public static void make(Monster entity,Kind kind,boolean persistent) {
        if (entity.level().isClientSide) return;
        replace(entity.getAttribute(CHAMPION.get()),modifier(IDS[kind.ordinal()],NAMES[kind.ordinal()],kind.ordinal()+2,AttributeModifier.Operation.ADDITION));
        if (entity instanceof ThaumcraftBossEntity boss) boss.generateChampionName(kind);
        else {
            replace(entity.getAttribute(Attributes.MAX_HEALTH),HEALTH); replace(entity.getAttribute(Attributes.ATTACK_DAMAGE),DAMAGE); entity.heal(25);
            entity.setCustomName(name(kind).copy().append(" ").append(entity.getName()));
        }
        if (persistent) entity.setPersistenceRequired();
        if (kind==Kind.BOLD) replace(entity.getAttribute(Attributes.MOVEMENT_SPEED),BOLD);
        if (kind==Kind.MIGHTY) replace(entity.getAttribute(Attributes.ATTACK_DAMAGE),MIGHTY);
        if (kind==Kind.WARDED) entity.setAbsorptionAmount(entity.getAbsorptionAmount()+(int)entity.getAttribute(Attributes.MAX_HEALTH).getBaseValue()/2);
    }
    private static int whitelist(Monster entity) {
        if (entity instanceof ThaumcraftBossEntity) return 200;
        var key=ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()); if (key==null) return -1;
        if (key.getNamespace().equals("minecraft")) return switch (key.getPath()) {
            case "zombie","zombified_piglin","spider","cave_spider","blaze","enderman","skeleton","wither_skeleton" -> 0;
            case "witch" -> 1; default -> -1;
        };
        if (!key.getNamespace().equals("thaumcraft")) return -1;
        if (entity instanceof CultistEntity) return 1;
        return switch (key.getPath()) {
            case "eldritch_crab","brainy_zombie","giant_brainy_zombie","taint_spider" -> 0;
            case "taintacle","taintacle_small","taintacle_giant","watcher" -> 2;
            case "wisp","pech" -> 1; case "inhabited_zombie" -> 3; default -> -1;
        };
    }
    @SubscribeEvent public static void joined(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Monster entity) || id(entity)>=-1) return;
        int chance=level.random.nextInt(100); boolean enabled=TCConfig.SERVER.championMobs.get();
        if (level.getDifficulty()==Difficulty.EASY || !enabled) chance+=2;
        if (level.getDifficulty()==Difficulty.HARD && enabled) chance-=2;
        boolean outer=level.dimension().equals(TCEldritch.OUTER_LANDS); if (outer) chance-=3;
        var pos=new BlockPos(net.minecraft.util.Mth.ceil(entity.getX()),net.minecraft.util.Mth.ceil(entity.getY()),net.minecraft.util.Mth.ceil(entity.getZ()));
        var biome=level.getBiome(pos);
        if (biome.is(net.minecraftforge.common.Tags.Biomes.IS_SPOOKY) || biome.is(net.minecraft.tags.BiomeTags.IS_NETHER) || biome.is(net.minecraft.tags.BiomeTags.IS_END)) chance-=enabled ? 2 : 1;
        if (outer && EldritchMazeSavedData.get(level.getServer()).cell(pos.getX()>>4,pos.getZ()>>4).map(cell->cell.feature()==6 || cell.feature()==8).orElse(false)) chance-=enabled ? 10 : 3;
        int bonus=whitelist(entity); if (bonus>=0 && (enabled || entity instanceof ThaumcraftBossEntity)) chance-=Math.max(0,bonus-1);
        if (bonus>=0 && chance<=0 && entity.getAttribute(Attributes.MAX_HEALTH).getBaseValue()>=10) make(entity,false);
        else replace(entity.getAttribute(CHAMPION.get()),modifier("1e645a3d-9115-4807-a61c-705172839f87","Normal",1,AttributeModifier.Operation.ADDITION));
    }
    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent event) {
        var entity=event.getEntity(); if (!(entity instanceof Monster) || !entity.isAlive()) return;
        Kind kind=kind(entity);
        if (kind==Kind.WARDED && entity.invulnerableTime<=0 && entity.tickCount%25==0
                && entity.getAbsorptionAmount()<(int)entity.getAttribute(Attributes.MAX_HEALTH).getBaseValue()/2) entity.setAbsorptionAmount(entity.getAbsorptionAmount()+1);
        if (kind==Kind.UNDYING && entity.tickCount%20==0) entity.heal(1);
    }
    // TC4 resolved the player's runic charge before champion on-hit effects in the same handler.
    @SubscribeEvent(priority=EventPriority.LOW) public static void hurt(LivingHurtEvent event) {
        var entity=event.getEntity(); if (entity.level().isClientSide) return;
        var source=event.getSource(); var direct=source.getDirectEntity(); Kind kind=kind(entity);
        if (entity instanceof Monster && (kind!=null || entity instanceof EldritchMob)) {
            if ((kind==Kind.WARDED || entity instanceof EldritchMob) && entity.getAbsorptionAmount()>0) shield(entity,source);
            else if (direct instanceof LivingEntity attacker) {
                if (kind==Kind.SPINED && !source.is(DamageTypes.THORNS)) {
                    attacker.hurt(entity.damageSources().thorns(entity),1+entity.getRandom().nextInt(3)); attacker.playSound(net.minecraft.sounds.SoundEvents.THORNS_HIT,.5f,1);
                }
                if (kind==Kind.ARMORED && !source.is(DamageTypeTags.BYPASSES_ARMOR)) event.setAmount(event.getAmount()*19/25);
                if (kind==Kind.INFESTED && entity.getRandom().nextFloat()<.4f) {
                    var spider=TCEntities.TAINT_SPIDER.get().create(entity.level());
                    if (spider!=null) { spider.moveTo(entity.getX(),entity.getY()+entity.getBbHeight()/2,entity.getZ(),entity.getRandom().nextFloat()*360,0); entity.level().addFreshEntity(spider); entity.playSound(TCSounds.GORE.get(),.5f,1); }
                }
            }
        }
        if (event.getAmount()<=0 || !(direct instanceof Monster attacker)) return;
        Kind offense=kind(attacker); if (offense==null) return; var random=attacker.getRandom();
        switch (offense) {
            case GRIM -> { if (random.nextFloat()<.4f) entity.addEffect(new MobEffectInstance(MobEffects.WITHER,200)); }
            case WARP -> { if (random.nextFloat()<.33f && entity instanceof ServerPlayer player) EldritchGuardianEntity.addTemporaryWarp(player,1+random.nextInt(3)); }
            case FIERY -> { if (random.nextFloat()<.4f) entity.setSecondsOnFire(4); }
            case SICKLY -> { if (random.nextFloat()<.4f) entity.addEffect(new MobEffectInstance(MobEffects.HUNGER,500)); }
            case VENOMOUS -> { if (random.nextFloat()<.4f) entity.addEffect(new MobEffectInstance(MobEffects.POISON,100)); }
            case VAMPIRIC -> attacker.heal(Math.max(2,event.getAmount()/2));
            default -> {}
        }
    }
    private static void shield(LivingEntity entity,net.minecraft.world.damagesource.DamageSource source) {
        int target=source.is(DamageTypes.FALL) ? -2 : source.is(DamageTypes.FALLING_BLOCK) ? -3 : source.getEntity()==null ? -1 : source.getEntity().getId();
        tc4revival.network.TCNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.NEAR.with(()->new net.minecraftforge.network.PacketDistributor.TargetPoint(
                entity.getX(),entity.getY(),entity.getZ(),32,entity.level().dimension())),new tc4revival.network.RunicHitPacket(entity.getId(),target));
        entity.playSound(TCSounds.RUNIC_SHIELD_EFFECT.get(),.66f,1.1f+entity.getRandom().nextFloat()*.1f);
    }
    public static void drops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Monster entity) || entity instanceof ThaumcraftBossEntity || !(entity.level() instanceof ServerLevel level)
                || !event.isRecentlyHit() || kind(entity)==null || event.getSource().getEntity() instanceof net.minecraftforge.common.util.FakePlayer) return;
        ExperienceOrb.award(level,entity.position(),5+level.random.nextInt(3));
        LegacyEntityDrops.addTable(event,"legacy_mob_bonus/champion");
    }
    private ChampionMobs() {}
}
