package conciliummodern;

import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Host adapters install real spell components; absent hosts never expose an unusable research. */
@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID)
public final class ConciliumSpellIntegration {
    public record StrokeSet(int level,List<Integer> strokes){public StrokeSet{if(level<1||strokes.isEmpty()||strokes.stream().anyMatch(stroke->stroke<0||stroke>3))throw new IllegalArgumentException("Invalid Witchery strokes");strokes=List.copyOf(strokes);}}
    public record WitcherySpell(String key,String name,int legacyId,int level,boolean charging,List<StrokeSet> strokes){public WitcherySpell{strokes=List.copyOf(strokes);}}
    @FunctionalInterface public interface WitcheryCast {boolean cast(ServerPlayer player,int spellLevel);}
    public interface WitcheryHost {
        /** Install into the host's actual symbol/spell registry, with its own mana and casting restrictions. */
        boolean registerSpell(WitcherySpell definition,WitcheryCast action);
        int infusion(ServerPlayer player);
        boolean grantKnowledge(ServerPlayer player,List<String> spellKeys);
    }
    public record RaidDefinition(String key,int mana,int burnout,int lifetime,int darkPower,float affinityShift,String skillTree,int treeX,int treeY,String pointType,String titleKey,String textKey,List<String> affinities,List<String> commonIngredients,List<String> hostEssences){public RaidDefinition{affinities=List.copyOf(affinities);commonIngredients=List.copyOf(commonIngredients);hostEssences=List.copyOf(hostEssences);}}
    @FunctionalInterface public interface RaidCast {boolean cast(ServerPlayer caster,ItemStack spell,Vec3 target,Entity targetedEntity);}
    public interface ArsHost {
        String modId();
        boolean registerRaid(RaidDefinition definition,RaidCast action);
        boolean canSummon(ServerPlayer player);
        boolean isSummon(Entity entity);
        /** Set owner/duration and the original first stage on this pending entity, without committing host summon bookkeeping. */
        void prepareSummon(ServerPlayer player,ItemStack spell,LesserPortal portal,int lifetime);
        /** Called only after successful insertion; commit the host's summon count here. */
        default void summonAdded(ServerPlayer player,LesserPortal portal){}
        void unlockRaid(ServerPlayer player);
        default List<BiConsumer<ServerLevel,BlockPos>> meteorEffects(){return List.of();}
    }
    private static final List<String> SPELLS=List.of("incarcerous","arrowspell","spiderspell","spellundead","conjunctivitis");
    private static volatile WitcheryHost witchery;
    private static volatile ArsHost ars;
    public static synchronized boolean registerWitchery(WitcheryHost host){
        Objects.requireNonNull(host);if(witchery!=null)throw new IllegalStateException("Witchery spell provider already registered");
        var definitions=List.of(
            new WitcherySpell("incarcerous","tc.spell.incarcerous",90,2,true,List.of(new StrokeSet(1,List.of(2,2,2,0,3,3,1)))),
            new WitcherySpell("arrowspell","tc.spell.arrow",91,2,false,List.of(new StrokeSet(1,List.of(1,1,1,3)))),
            new WitcherySpell("spiderspell","tc.spell.spider",92,2,false,List.of(new StrokeSet(1,List.of(0,0,0,2,1,1,0,0)))),
            new WitcherySpell("spellundead","tc.spell.undead",93,2,false,List.of(new StrokeSet(1,List.of(1,1,1,0,3,2,2)),new StrokeSet(2,List.of(1,1,1,1,1,0,0,3,3,2,2,2,2)))),
            new WitcherySpell("conjunctivitis","tc.spell.blind",94,2,false,List.of(new StrokeSet(1,List.of(0,0,3,3,1,1,2,2)))));
        for(var definition:definitions)if(!host.registerSpell(definition,(player,power)->castWitchery(definition.key,player,power)))return false;
        witchery=host;return true;
    }
    public static synchronized boolean registerArs(ArsHost host){
        Objects.requireNonNull(host);if(ars!=null)throw new IllegalStateException("Ars spell provider already registered");
        if(!net.minecraftforge.fml.ModList.get().isLoaded(host.modId()))return false;
        var definition=new RaidDefinition("Crimson_Raid",3000,300,400,10000,.01f,"None",25,175,"SILVER","am2.spell.crimson_raid","am2.entry.crimson_raid",List.of("ENDER","ARCANE"),List.of("thaumcraft:primordial_pearl","thaumcraft:crimson_rites","thaumcraft:focus_portable_hole","thaumicconcilium:crimson_annales","minecraft:black_banner"),List.of("ENDER","ARCANE","LIFE"));
        if(!host.registerRaid(definition,ConciliumSpellIntegration::castRaid))return false;ars=host;return true;
    }
    public static boolean witcheryAvailable(){return witchery!=null&&CrimsonContent.available();}
    public static boolean arsAvailable(){return ars!=null&&CrimsonContent.available();}
    public static List<BiConsumer<ServerLevel,BlockPos>> meteors(){return ars==null?List.of():List.copyOf(ars.meteorEffects());}
    public static boolean repairWitchery(ServerPlayer player){return witcheryAvailable()&&thaumcraft.api.ThaumcraftApiHelper.isResearchComplete(player,"thaumicconcilium:CRIMSONSPELLS")&&witchery.grantKnowledge(player,SPELLS);}
    @SubscribeEvent public static void research(com.ilya3point999k.thaumicconcilium.api.event.ThaumcraftResearchCompletedEvent event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        if(event.getUnlockedResearch().equals("thaumicconcilium:CRIMSONSPELLS"))repairWitchery(player);
        else if(event.getUnlockedResearch().equals("thaumicconcilium:CRIMSONRAID")&&arsAvailable())ars.unlockRaid(player);
    }
    private static boolean castWitchery(String key,ServerPlayer player,int spellLevel){
        if(!witcheryAvailable()||!player.isAlive()||player.isSpectator()||!thaumcraft.api.ThaumcraftApiHelper.isResearchComplete(player,"thaumicconcilium:CRIMSONSPELLS"))return false;
        var level=player.serverLevel();int infusion=witchery.infusion(player);var eye=player.getEyePosition();var look=player.getLookAngle();
        if(key.equals("incarcerous")){
            if(infusion==4){var shackles=new EtherealShackles(player);shackles.shoot(look.x,look.y,look.z,2,2);if(!level.addFreshEntity(shackles))return false;shackles.playSound(ConciliumSounds.SHACKLES_THROW.get(),.5f,1);}
            else for(int i=0,count=2+level.random.nextInt(3);i<count;i++){var powder=new ShardPowder(player,PowderType.SLOTH,eye.add(look).add(0,-.5,0));powder.setDeltaMovement(look.add(level.random.nextGaussian()*.09,level.random.nextGaussian()*.09,level.random.nextGaussian()*.09));level.addFreshEntity(powder);}
            return true;
        }
        if(key.equals("arrowspell")){
            var arrow=new Arrow(level,player);arrow.setCritArrow(true);arrow.setBaseDamage(infusion==2?1+Math.sqrt(pool(player,"telum"))*1.5:arrow.getBaseDamage()+5);
            arrow.pickup=AbstractArrow.Pickup.CREATIVE_ONLY;arrow.shootFromRotation(player,player.getXRot(),player.getYRot(),0,2,1);return level.addFreshEntity(arrow);
        }
        var target=pointed(player);if(target==null||target.isSpectator())return false;
        if(key.equals("spiderspell")||key.equals("spellundead")){
            if(!(target instanceof Monster)||target.getMobType()!=(key.equals("spiderspell")?MobType.ARTHROPOD:MobType.UNDEAD))return false;
            float damage=infusion==1?(float)(1+Math.sqrt(pool(player,"lux"))*2.5):level.random.nextInt(30)+Math.max(0,spellLevel)*2;
            if(!target.hurt(level.damageSources().indirectMagic(player,player),damage))return false;
            ConciliumArcNetwork.emit(level,player.position().add(0,player.getBbHeight()-.5,0),target.position().add(0,target.getBbHeight()*.5,0),0xffffff,.02f);player.playSound(tc4revival.content.TCSounds.CRAFT_FAIL.get(),1,1);return true;
        }
        if(key.equals("conjunctivitis")&&target instanceof Mob){
            int duration=200+(infusion==4?Math.min(Integer.MAX_VALUE-400,pool(player,"tenebrae")):0);
            var blur=new MobEffectInstance(tc4revival.content.TCEffects.BLURRED_VISION.get(),duration+200,60);blur.setCurativeItems(List.of());target.addEffect(blur,player);
            if(infusion==4){var blind=new MobEffectInstance(MobEffects.BLINDNESS,duration,60);blind.setCurativeItems(List.of());target.addEffect(blind,player);}return true;
        }
        return false;
    }
    private static int pool(ServerPlayer player,String aspect){return tc4revival.research.KnowledgeSavedData.get(player.server).snapshot(player.getUUID()).map(k->k.poolAmount(tc4revival.aspect.TCAspects.catalog().require(aspect))).orElse(0);}
    private static LivingEntity pointed(ServerPlayer player){
        var from=player.getEyePosition();var to=from.add(player.getLookAngle().scale(32));var block=player.level().clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));if(block.getType()!=HitResult.Type.MISS)to=block.getLocation();
        var hit=ProjectileUtil.getEntityHitResult(player.level(),player,from,to,player.getBoundingBox().expandTowards(to.subtract(from)).inflate(1),entity->entity instanceof LivingEntity&&entity.isAlive()&&entity.isPickable());return hit!=null&&hit.getEntity() instanceof LivingEntity living?living:null;
    }
    private static boolean castRaid(ServerPlayer player,ItemStack spell,Vec3 position,Entity target){
        if(!arsAvailable()||!player.isAlive()||!PontifexRobe.fullSet(player)||!thaumcraft.api.ThaumcraftApiHelper.isResearchComplete(player,"thaumicconcilium:CRIMSONRAID")||target!=null&&ars.isSummon(target)||!ars.canSummon(player))return false;
        var level=player.serverLevel();var at=BlockPos.containing(position);if(!level.hasChunkAt(at)||!level.getWorldBorder().isWithinBounds(at)||level.isOutsideBuildHeight(at)||!level.mayInteract(player,at))return false;
        var portal=CrimsonContent.PORTAL.get().create(level);if(portal==null)return false;portal.setPos(position);portal.activateImmediately();
        ars.prepareSummon(player,spell,portal,400);if(!level.addFreshEntity(portal))return false;ars.summonAdded(player,portal);return true;
    }
    @Mod.EventBusSubscriber(modid=ThaumicConcilium.ID,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class SpellLifecycle {
        @SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event){event.enqueueWork(()->{
            thaumcraft.api.research.ResearchCategories.registerEnablement("thaumicconcilium:CRIMSONSPELLS",ConciliumSpellIntegration::witcheryAvailable);
            thaumcraft.api.research.ResearchCategories.registerEnablement("thaumicconcilium:CRIMSONRAID",ConciliumSpellIntegration::arsAvailable);
        });}
    }
    private ConciliumSpellIntegration(){}
}
