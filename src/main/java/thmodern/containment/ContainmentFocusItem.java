package thmodern.containment;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import tc4revival.content.*;
import tc4revival.aspect.*;
import thaumcraft.api.wands.ItemFocusBasic;

public final class ContainmentFocusItem extends ItemFocusBasic {
    private record Channel(UUID target,float progress,long tick){}
    private final Map<ServerPlayer,Channel> channels=new WeakHashMap<>();
    @Override public int color(ItemStack focus){return 29631;}
    @Override public String depthTexture(){return "thaumichorizons:focus_containment_depth";}
    @Override public String sortingPrefix(){return "CN";}
    @Override public boolean costPerTick(ItemStack focus){return true;}
    @Override public Animation animation(ItemStack focus){return Animation.CHARGE;}
    @Override public Map<Aspect,Integer> costs(ItemStack focus){return Map.of(TCAspects.catalog().require("aer"),10,TCAspects.catalog().require("perditio"),10);}
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus,int rank){
        if(rank<1||rank>5)return List.of();var list=new ArrayList<>(List.of(FocusUpgrade.FRUGAL,FocusUpgrade.POTENCY));if(rank==2||rank==4)list.add(FocusUpgrade.require("thaumichorizons:slow"));return list;
    }
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack,Level level,Player player,InteractionHand hand,BlockHitResult hit){
        if(player instanceof ServerPlayer server){channels.remove(server);if(!CreatureJar.room(server))return InteractionResultHolder.fail(stack);}
        player.startUsingItem(hand);return InteractionResultHolder.consume(stack);
    }
    public static LivingEntity target(Player player){return target(player,10);}
    public static LivingEntity target(Player player,double range){
        var start=player.getEyePosition();var end=start.add(player.getLookAngle().scale(range));
        var block=player.level().clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));if(block.getType()!=HitResult.Type.MISS)end=block.getLocation();
        var hit=net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(player.level(),player,start,end,player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.1),entity->entity instanceof LivingEntity living&&CreatureJar.eligible(living)&&entity.isPickable());
        return hit!=null&&hit.getEntity() instanceof LivingEntity living?living:null;
    }
    @Override public void usingTick(ItemStack stack,Player player,int remaining){
        if(!(player instanceof ServerPlayer server)||!(stack.getItem() instanceof CastingWandItem wand))return;
        if(!CreatureJar.room(server)){channels.remove(server);player.stopUsingItem();player.displayClientMessage(net.minecraft.network.chat.Component.translatable("thaumichorizons.noJar"),true);return;}
        var focus=WandFocusItem.attached(stack);if(!wand.consumeForPlayer(stack,player,costs(focus),false,false,false)){channels.remove(server);player.stopUsingItem();return;}
        var target=target(player);long tick=player.level().getGameTime();var channel=channels.get(server);
        if(target==null){channels.remove(server);return;}
        if(channel!=null&&channel.tick==tick)return;
        if(!wand.consumeForPlayer(stack,player,costs(focus),false,false,true)){channels.remove(server);player.stopUsingItem();return;}
        float potency=wand.focusPotency(stack,player);int slow=getUpgradeLevel(focus,"thaumichorizons:slow");
        if(slow>0)target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,40,slow-1));
        float progress=channel==null||!channel.target.equals(target.getUUID())||channel.tick!=tick-1?2+potency/2f:channel.progress+2+potency/3;
        channels.put(server,new Channel(target.getUUID(),progress,tick));
        if(tick%3==0)thaumcraft.api.fx.LegacyEffects.wandBeam(player,target.getBoundingBox().getCenter(),2,4482815,2,true);
        if(progress>target.getHealth()*20&&CreatureJar.capture(server,target)){channels.remove(server);thaumcraft.api.fx.LegacyEffects.emit(server.serverLevel(),"containment",target.position(),net.minecraft.world.phys.Vec3.ZERO,0xffffff,1,40,1,0,0);}
    }
    @Override public void release(ItemStack stack,Level level,Player player,int remaining){if(player instanceof ServerPlayer server)channels.remove(server);}
}
