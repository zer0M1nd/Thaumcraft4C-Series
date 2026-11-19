package tc4revival.content;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.aspect.TCAspects;
import tc4revival.config.TCConfig;
import tc4revival.network.*;
import tc4revival.research.*;

/** Timed warp encounters and the normal route into eldritch research. */
@Mod.EventBusSubscriber(modid="thaumcraft")
public  class WarpEvents {
    /** Fortress armor supplies this when its mask equipment system is installed. */
    public interface MaskProtection { boolean protectsFromWarp(ItemStack stack); }
    private WarpEvents() {}
    @SubscribeEvent public static void finishEating(net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var hunger=player.getEffect(TCEffects.UNNATURAL_HUNGER.get()); if (hunger==null) return;
        var food=event.getItem();
        if (food.is(Items.ROTTEN_FLESH) || food.is(TCItems.ZOMBIE_BRAIN.get())) {
            int amplifier=hunger.getAmplifier()-1,duration=hunger.getDuration()-600;
            player.removeEffect(TCEffects.UNNATURAL_HUNGER.get());
            if (amplifier>=0 && duration>0) {
                var next=new MobEffectInstance(TCEffects.UNNATURAL_HUNGER.get(),duration,amplifier,true,true);
                next.setCurativeItems(new ArrayList<>(List.of(new ItemStack(Items.ROTTEN_FLESH),new ItemStack(TCItems.ZOMBIE_BRAIN.get())))); player.addEffect(next);
            }
            player.sendSystemMessage(Component.translatable("warp.text.hunger.2").withStyle(ChatFormatting.DARK_GREEN,ChatFormatting.ITALIC));
        } else if (food.getFoodProperties(player)!=null)
            player.sendSystemMessage(Component.translatable("warp.text.hunger.1").withStyle(ChatFormatting.DARK_RED,ChatFormatting.ITALIC));
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase!=TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || player.tickCount<=0 || player.tickCount%2000!=0 || TCConfig.SERVER.disableWarp.get()
                || player.hasEffect(TCEffects.WARP_WARD.get())) return;
        check(player);
    }
    public static void check(ServerPlayer player) {
        if (!effectsAllowed(player)) return;
        var random=player.serverLevel().random;
        var data=KnowledgeSavedData.get(player.server);
        ItemStack helmet=player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        boolean mask=helmet.getItem() instanceof MaskProtection protection && protection.protectsFromWarp(helmet);
        var roll=data.update(player.getUUID(),random::nextInt,
                knowledge->WarpEventRules.roll(knowledge,WarpingGear.equipped(player),mask,random::nextInt));
        if (roll.triggered()) {
            visual(player,FocusEffectPacket.Kind.WARP_EVENT);
            apply(player,roll);
            unlock(player,roll.lastingWarp());
        }
        data.update(player.getUUID(),random::nextInt,knowledge->{ knowledge.changeWarp(PlayerKnowledge.WarpKind.TEMPORARY,-1); return null; });
    }
    /** Shared execution gate, also for addons that retain queued warp encounters. */
    public static boolean effectsAllowed(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && !player.getAbilities().instabuild
                && !TCConfig.SERVER.disableWarp.get() && !player.hasEffect(TCEffects.WARP_WARD.get())
                && KnowledgeSavedData.get(player.server).snapshot(player.getUUID())
                    .map(k -> k.totalWarp()>0).orElse(false);
    }
    /** Debug entry point: bypass chance and ward, but never trigger an event at zero warp. */
    public static void force(ServerPlayer player, WarpEventRules.Event event, int strength) {
        if(KnowledgeSavedData.get(player.server).snapshot(player.getUUID()).map(k->k.totalWarp()<=0).orElse(true))return;
        apply(player,new WarpEventRules.Roll(true,Math.max(10,Math.min(100,strength)),0,event));
    }
    private static void message(ServerPlayer player,int number) {
        player.sendSystemMessage(Component.translatable("warp.text."+number).withStyle(ChatFormatting.DARK_PURPLE,ChatFormatting.ITALIC));
    }
    private static void visual(ServerPlayer player,FocusEffectPacket.Kind kind) {
        TCNetwork.send(player,new FocusEffectPacket(player.level().dimension().location(),kind,player.position(),player.position(),1,0));
    }
    private static void potion(ServerPlayer player,MobEffect effect,int ticks,int amplifier,boolean ambient,boolean incurable,boolean hunger) {
        var instance=new MobEffectInstance(effect,ticks,amplifier,ambient,true);
        if (incurable) instance.setCurativeItems(hunger ? new ArrayList<>(List.of(new ItemStack(Items.ROTTEN_FLESH),new ItemStack(TCItems.ZOMBIE_BRAIN.get()))) : new ArrayList<>());
        player.addEffect(instance);
    }
    private static void apply(ServerPlayer player,WarpEventRules.Roll roll) {
        int warp=roll.strength(),amplifier=Math.min(3,warp/15);
        switch (roll.event()) {
            case NONE -> { }
            case INSIGHT -> { insight(player,1); message(player,3); }
            case GREAT_INSIGHT -> { insight(player,warp/10); message(player,3); }
            case PARANOIA -> message(player,11);
            case WHISPERS -> message(player,12);
            case DREAD -> message(player,13);
            case EXHAUSTION -> { potion(player,TCEffects.VIS_EXHAUST.get(),5000,amplifier,true,true,false); message(player,1); }
            case THAUMARHIA -> { potion(player,TCEffects.THAUMARHIA.get(),Math.min(32000,10*warp),0,true,true,false); message(player,15); }
            case HUNGER,LONG_HUNGER -> { potion(player,TCEffects.UNNATURAL_HUNGER.get(),roll.event()==WarpEventRules.Event.HUNGER ? 5000 : 6000,amplifier,true,true,true); message(player,2); }
            case MIST -> mist(player,1);
            case MANY_GUARDIANS -> mist(player,warp/30);
            case MOST_GUARDIANS -> mist(player,warp/15);
            case BLURRED -> potion(player,TCEffects.BLURRED_VISION.get(),Math.min(32000,10*warp),0,true,false,false);
            case SUN -> { potion(player,TCEffects.SUN_SCORNED.get(),5000,amplifier,true,true,false); message(player,5); }
            case FATIGUE -> { potion(player,MobEffects.DIG_SLOWDOWN,1200,amplifier,true,false,false); message(player,9); }
            case INFECTIOUS -> { potion(player,TCEffects.INFECTIOUS_VIS_EXHAUST.get(),6000,amplifier,false,true,false); message(player,1); }
            case NIGHT_VISION -> { potion(player,MobEffects.NIGHT_VISION,Math.min(40*warp,6000),0,true,false,false); message(player,10); }
            case GAZE -> { potion(player,TCEffects.DEATH_GAZE.get(),6000,amplifier,true,true,false); message(player,4); }
            case SPIDERS,REAL_SPIDERS -> spiders(player,warp,roll.event()==WarpEventRules.Event.REAL_SPIDERS);
            case BLINDNESS -> potion(player,MobEffects.BLINDNESS,Math.min(32000,5*warp),0,true,false,false);
            case RELIEF -> {
                boolean reduced=KnowledgeSavedData.get(player.server).update(player.getUUID(),player.serverLevel().random::nextInt,knowledge->{
                    if (knowledge.warp(PlayerKnowledge.WarpKind.STICKY)<=0) return false;
                    knowledge.changeWarp(PlayerKnowledge.WarpKind.STICKY,-1); return true;
                });
                if (reduced) visual(player,FocusEffectPacket.Kind.WARP_RELIEF);
                message(player,14);
            }
        }
    }
    private static void insight(ServerPlayer player,int times) {
        var random=player.serverLevel().random;
        var rewards=KnowledgeSavedData.get(player.server).update(player.getUUID(),random::nextInt,knowledge->{
            var result=new ArrayList<ScanFeedbackPacket.Reward>(); var primals=TCAspects.catalog().primals();
            for (int count=1+random.nextInt(times);count>0;count--) {
                var aspect=primals.get(random.nextInt(6)); knowledge.changePool(aspect,(short)1);
                result.add(new ScanFeedbackPacket.Reward(aspect.tag(),(short)1,false));
            }
            return result;
        });
        TCNetwork.send(player,new ScanFeedbackPacket(ScanRules.Status.SUCCESS,"",rewards));
    }
    private static void unlock(ServerPlayer player,int lasting) {
        var knowledge=KnowledgeSavedData.get(player.server).snapshot(player.getUUID()).orElseThrow();
        if (lasting>10 && !knowledge.researchMarkers().contains("BATHSALTS") && !knowledge.researchMarkers().contains("@BATHSALTS")) {
            complete(player,"@BATHSALTS"); message(player,8); TCNetwork.send(player,new CluePacket("BATHSALTS"));
        }
        if (lasting>25 && !knowledge.researchMarkers().contains("ELDRITCHMINOR")) { insight(player,10); complete(player,"ELDRITCHMINOR"); }
        if (lasting>50 && !knowledge.researchMarkers().contains("ELDRITCHMAJOR")) { insight(player,20); complete(player,"ELDRITCHMAJOR"); }
    }
    private static void complete(ServerPlayer player,String key) {
        var result=KnowledgeSavedData.get(player.server).update(player.getUUID(),player.serverLevel().random::nextInt,
                knowledge->ResearchCatalogs.progression().completeTrusted(knowledge,key,TCConfig.SERVER.disableWarp.get()));
        if (!key.startsWith("@")) result.ifPresent(completion->TCNetwork.send(player,ResearchCompletionPacket.from(completion)));
    }
    private static boolean position(ServerPlayer player,Mob mob) {
        var level=player.serverLevel(); var random=level.random; var center=player.blockPosition();
        for (int attempt=0;attempt<50;attempt++) {
            var feet=center.offset((7+random.nextInt(18))*(random.nextInt(3)-1),
                    (7+random.nextInt(18))*(random.nextInt(3)-1),(7+random.nextInt(18))*(random.nextInt(3)-1));
            if (!level.getBlockState(feet.below()).isFaceSturdy(level,feet.below(),Direction.UP)) continue;
            mob.setPos(feet.getX(),feet.getY(),feet.getZ());
            if (level.isUnobstructed(mob) && level.noCollision(mob) && !level.containsAnyLiquid(mob.getBoundingBox())) return true;
        }
        return false;
    }
    private static void mist(ServerPlayer player,int count) {
        visual(player,FocusEffectPacket.Kind.MIST_LONG);
        for (int i=0;i<Math.min(8,count);i++) {
            var guardian=TCEntities.ELDRITCH_GUARDIAN.get().create(player.serverLevel());
            if (guardian!=null && position(player,guardian)) { guardian.setTarget(player); player.serverLevel().addFreshEntity(guardian); }
        }
        message(player,6);
    }
    private static void spiders(ServerPlayer player,int warp,boolean real) {
        for (int i=0;i<Math.min(50,warp);i++) {
            var spider=TCEntities.MIND_SPIDER.get().create(player.serverLevel());
            if (spider==null || !position(player,spider)) continue;
            spider.setTarget(player);
            if (!real) { spider.setViewer(player.getGameProfile().getName()); spider.setHarmless(true); }
            player.serverLevel().addFreshEntity(spider);
        }
        message(player,7);
    }
}
