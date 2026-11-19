package conciliummodern;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.*;

/** Ars Magica: Legacy 1.20.1-1.5.0, with optional classes isolated in Linked. */
@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID)
public final class ArsLegacyIntegration {
    public static final String MOD="arsmagicalegacy",OWNER="ConciliumRaidOwner",EXPIRES="ConciliumRaidExpires",LEASE="ConciliumRaidLease";
    public static final ResourceLocation RAID=ResourceLocation.parse("thaumicconcilium:crimson_raid");
    private static final List<String> PARTS=List.of("witchwood_rod","witchwood_staff","vinteum_cap","base_essence_core","high_essence_core","pure_essence");
    public static boolean available(){return net.minecraftforge.fml.ModList.get().isLoaded(MOD);}
    public static boolean wandsAvailable(){return available()&&fmmodern.ForbiddenConfig.value(fmmodern.ForbiddenConfig.CROSS_MOD)&&fmmodern.ForbiddenConfig.value(fmmodern.ForbiddenConfig.CROSS_WAND);}
    private static long clock(ServerPlayer player){return player.server.overworld().getGameTime();}
    public static boolean canSummon(ServerPlayer player){
        var lease=player.getPersistentData().getCompound(LEASE);if(!lease.hasUUID("Entity")||lease.getLong(EXPIRES)<=clock(player))return true;
        for(var level:player.server.getAllLevels()){
            var entity=level.getEntity(lease.getUUID("Entity"));if(entity!=null)return !entity.isAlive();
        }
        // Unloaded portals keep their short lease; unloading cannot bypass the original one-summon limit.
        return false;
    }
    @SubscribeEvent public static void tick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event){
        if(event.getEntity() instanceof LesserPortal portal&&portal.level() instanceof ServerLevel level){
            var tag=portal.getPersistentData();if(tag.hasUUID(OWNER)&&tag.getLong(EXPIRES)<=level.getServer().overworld().getGameTime())portal.discard();
        }
    }
    @SubscribeEvent public static void clone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event){
        var lease=event.getOriginal().getPersistentData().getCompound(LEASE);if(!lease.isEmpty())event.getEntity().getPersistentData().put(LEASE,lease.copy());
    }
    @SubscribeEvent public static void login(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event){
        if(available()&&event.getEntity() instanceof ServerPlayer player&&ConciliumSpellIntegration.arsAvailable()
                &&thaumcraft.api.ThaumcraftApiHelper.isResearchComplete(player,"thaumicconcilium:CRIMSONRAID"))Linked.HOST.unlockRaid(player);
    }
    @Mod.EventBusSubscriber(modid=ThaumicConcilium.ID,bus=Mod.EventBusSubscriber.Bus.MOD)
    // EventBus 6.2.x generates listener names from the simple class name; keep it package-unique.
    public static final class ArsLifecycle {
        @SubscribeEvent public static void register(RegisterEvent event){
            // Startup wand definitions are unconditional, so their item IDs must also exist without AML.
            event.register(ForgeRegistries.Keys.ITEMS,helper->{for(var name:PARTS)helper.register(ResourceLocation.parse("thaumicconcilium:"+name),new Item(new Item.Properties()));});
            if(available())Linked.register(event);
        }
        @SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event){event.enqueueWork(()->{
            for(String key:List.of("ARS_SCHOOLS","ROD_witchwood","ROD_witchwood_staff","CAP_vinteum"))thaumcraft.api.research.ResearchCategories.registerEnablement("thaumicconcilium:"+key,ArsLegacyIntegration::wandsAvailable);
            if(available()){
                ConciliumSpellIntegration.registerArs(Linked.HOST);
                thaumcraft.api.wands.WandRod.registerUpdate("thaumicconcilium:witchwood",Linked::recharge);
                thaumcraft.api.wands.WandRod.registerUpdate("thaumicconcilium:witchwood_staff",Linked::recharge);
            }
        });}
        @SubscribeEvent public static void creative(net.minecraftforge.event.BuildCreativeModeTabContentsEvent event){
            if(wandsAvailable()&&event.getTabKey().location().equals(ResourceLocation.parse("thaumicconcilium:main")))for(String name:PARTS){var item=ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("thaumicconcilium:"+name));if(item!=null)event.accept(item);}
        }
    }
    private static final class Linked {
        private static final Host HOST=new Host();
        private static ConciliumSpellIntegration.RaidCast action;
        private record Context(com.github.minecraftschurlimods.arsmagicalegacy.api.spell.ISpell spell,Entity direct,int ticks){}
        private static final ThreadLocal<Context> CAST=new ThreadLocal<>();
        static void register(RegisterEvent event){event.register(com.github.minecraftschurlimods.arsmagicalegacy.api.spell.ISpellPart.REGISTRY_KEY,helper->helper.register(RAID,new CrimsonRaid()));}
        static com.github.minecraftschurlimods.arsmagicalegacy.api.ArsMagicaAPI api(){return com.github.minecraftschurlimods.arsmagicalegacy.api.ArsMagicaAPI.get();}
        static void recharge(ItemStack stack,Player player){
            if(player.level().isClientSide||!wandsAvailable()||!(stack.getItem() instanceof tc4revival.content.CastingWandItem wand))return;
            boolean staff=wand.rod(stack).staff();if(player.tickCount%(staff?40:100)!=0)return;
            // Original 0.575 ManaStaffUpdate did not require a hotbar slot.
            boolean vinteum=wand.cap(stack).tag.equals("thaumicconcilium:vinteum");float cost=staff?(vinteum?.5f:1f):(vinteum?.8f:1.4f);
            var mana=api().getManaHelper();for(var aspect:tc4revival.aspect.TCAspects.catalog().primals()){
                int accepted=wand.receiveVis(stack,aspect,100,true);float payment=accepted*cost;
                if(accepted>0&&mana.getMana(player)>payment&&mana.decreaseMana(player,payment))wand.receiveVis(stack,aspect,accepted,false);
            }
        }
        private static final class Host implements ConciliumSpellIntegration.ArsHost {
            public String modId(){return MOD;}
            public boolean registerRaid(ConciliumSpellIntegration.RaidDefinition definition,ConciliumSpellIntegration.RaidCast callback){
                if(!(api().getSpellPartRegistry().getValue(RAID) instanceof CrimsonRaid))return false;action=callback;return true;
            }
            public boolean canSummon(ServerPlayer player){return ArsLegacyIntegration.canSummon(player);}
            public boolean isSummon(Entity entity){return entity.getPersistentData().hasUUID(OWNER)||entity.getPersistentData().getBoolean("AM2_Entity_Is_Made_Summon");}
            public void prepareSummon(ServerPlayer player,ItemStack spell,LesserPortal portal,int lifetime){
                var tag=portal.getPersistentData();tag.putUUID(OWNER,player.getUUID());tag.putLong(EXPIRES,clock(player)+lifetime);tag.putBoolean("AM2_Entity_Is_Made_Summon",true);
            }
            public void summonAdded(ServerPlayer player,LesserPortal portal){
                var lease=new CompoundTag();lease.putUUID("Entity",portal.getUUID());lease.putLong(EXPIRES,portal.getPersistentData().getLong(EXPIRES));player.getPersistentData().put(LEASE,lease);
                var context=CAST.get();if(context==null)return;
                // AM2 applied every component of the current stage to the new summon and ignored each failure.
                // AML chains components recursively, so use a one-component spell for each effect, excluding Raid.
                // The original target's normal host continuation remains intact and cannot recursively summon here.
                for(var pair:context.spell.partsWithModifiers())if(pair.getFirst() instanceof com.github.minecraftschurlimods.arsmagicalegacy.api.spell.ISpellComponent&&!(pair.getFirst() instanceof CrimsonRaid)){
                    var parts=new ArrayList<com.github.minecraftschurlimods.arsmagicalegacy.api.spell.ISpellPart>();parts.add(pair.getFirst());parts.addAll(pair.getSecond());
                    var stage=api().makeSpell(List.of(),com.github.minecraftschurlimods.arsmagicalegacy.api.spell.SpellStack.of(parts),context.spell.additionalData().copy());
                    api().getSpellHelper().invoke(stage,player,context.direct,portal.level(),new EntityHitResult(portal),context.ticks,0,false);
                }
            }
            public void unlockRaid(ServerPlayer player){var skills=api().getSkillHelper();if(!skills.knows(player,RAID))skills.learn(player,RAID);}
            public List<java.util.function.BiConsumer<ServerLevel,net.minecraft.core.BlockPos>> meteorEffects(){return List.of(Linked::moonstone,Linked::star);}
        }
        private static Vec3 meteorPosition(ServerLevel level,net.minecraft.core.BlockPos target){return new Vec3(target.getX()-8+level.random.nextInt(16),level.getMaxBuildHeight()-1,target.getZ()-8+level.random.nextInt(16));}
        static void moonstone(ServerLevel level,net.minecraft.core.BlockPos target){var position=meteorPosition(level,target);if(!level.hasChunkAt(net.minecraft.core.BlockPos.containing(position))||!level.getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(position)))return;var meteor=ConciliumMoonstoneMeteor.TYPE.get().create(level);if(meteor!=null){meteor.setPos(position);meteor.target(target);level.addFreshEntity(meteor);}}
        static void star(ServerLevel level,net.minecraft.core.BlockPos target){var position=meteorPosition(level,target);if(!level.hasChunkAt(net.minecraft.core.BlockPos.containing(position))||!level.getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(position)))return;var star=ConciliumMoonstoneMeteor.TYPE.get().create(level);if(star!=null){star.setPos(position);star.target(target);star.shootingStar(true);level.addFreshEntity(star);}}
        private static final class CrimsonRaid implements com.github.minecraftschurlimods.arsmagicalegacy.api.spell.ISpellComponent {
            public Set<com.github.minecraftschurlimods.arsmagicalegacy.api.spell.ISpellPartStat> getStatsUsed(){return Set.of();}
            public com.github.minecraftschurlimods.arsmagicalegacy.api.spell.SpellCastResult invoke(com.github.minecraftschurlimods.arsmagicalegacy.api.spell.ISpell spell,LivingEntity caster,Entity direct,Level level,List<com.github.minecraftschurlimods.arsmagicalegacy.api.spell.ISpellModifier> modifiers,EntityHitResult target,int index,int ticks){return cast(spell,caster,direct,target.getEntity().position(),target.getEntity(),ticks);}
            public com.github.minecraftschurlimods.arsmagicalegacy.api.spell.SpellCastResult invoke(com.github.minecraftschurlimods.arsmagicalegacy.api.spell.ISpell spell,LivingEntity caster,Entity direct,Level level,List<com.github.minecraftschurlimods.arsmagicalegacy.api.spell.ISpellModifier> modifiers,BlockHitResult target,int index,int ticks){return cast(spell,caster,direct,target.getLocation(),null,ticks);}
            private com.github.minecraftschurlimods.arsmagicalegacy.api.spell.SpellCastResult cast(com.github.minecraftschurlimods.arsmagicalegacy.api.spell.ISpell spell,LivingEntity caster,Entity direct,Vec3 at,Entity target,int ticks){
                if(!(caster instanceof ServerPlayer player)||action==null)return com.github.minecraftschurlimods.arsmagicalegacy.api.spell.SpellCastResult.EFFECT_FAILED;
                var stack=new ItemStack(com.github.minecraftschurlimods.arsmagicalegacy.common.init.AMItems.SPELL.get());api().getSpellHelper().setSpell(stack,spell);
                var previous=CAST.get();CAST.set(new Context(spell,direct,ticks));try{
                    return action.cast(player,stack,at,target)?com.github.minecraftschurlimods.arsmagicalegacy.api.spell.SpellCastResult.SUCCESS:com.github.minecraftschurlimods.arsmagicalegacy.api.spell.SpellCastResult.EFFECT_FAILED;
                }finally{if(previous==null)CAST.remove();else CAST.set(previous);}
            }
        }
    }
    private ArsLegacyIntegration(){}
}
