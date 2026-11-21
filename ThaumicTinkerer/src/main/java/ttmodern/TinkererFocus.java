package ttmodern;

import java.util.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import tc4revival.content.*;
import tc4revival.aspect.*;

/** Shared costs/upgrades and old item motion for the early Tinkerer focus progression. */
public abstract class TinkererFocus extends WandFocusItem {
    private final String name,prefix;private final int tint;
    protected TinkererFocus(String name,String prefix,int tint){this.name=name;this.prefix=prefix;this.tint=tint;}
    @Override public net.minecraft.world.item.Rarity getRarity(ItemStack stack){return net.minecraft.world.item.Rarity.RARE;}
    @Override public int color(ItemStack focus){return tint;}
    @Override public String sortingPrefix(){return prefix;}
    @Override public String ornamentTexture(){return Tinkerer.ID+":item/"+name+"_orn";}
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus,int rank){return rank>=1&&rank<=5?List.of(FocusUpgrade.FRUGAL):List.of();}
    protected static Map<Aspect,Integer> cost(String first,int amount){return Map.of(TCAspects.catalog().require(first),amount);}
    protected static Map<Aspect,Integer> cost(String first,int amount,String second,int other){return Map.of(TCAspects.catalog().require(first),amount,TCAspects.catalog().require(second),other);}
    protected static void pull(net.minecraft.world.entity.Entity entity,Vec3 target,double speed){
        speed=thaumcraft.api.wands.SpellParameters.limited("telekinesis.speed",speed,4);
        var delta=target.subtract(entity.position().add(0,entity.getBbHeight()/2d,0));if(delta.length()>1)delta=delta.normalize();entity.setDeltaMovement(delta.scale(speed));entity.hasImpulse=true;
    }
    protected static void dust(net.minecraft.server.level.ServerLevel level,Vec3 at,int color,int count,double spread){
        thaumcraft.api.fx.LegacyEffects.sparkle(level,at,color,1.5f,count,(float)spread);
    }
    public static final class Flight extends TinkererFocus {
        public Flight(){super("focus_flight","TTFL",0x9ef2ff);}
        @Override public Map<Aspect,Integer> costs(ItemStack focus){return cost("aer",15);}
        @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack,Level level,Player player,InteractionHand hand,BlockHitResult hit){
            if(!TinkererConfig.FLIGHT.get())return InteractionResultHolder.pass(stack);
            var wand=(CastingWandItem)stack.getItem();
            if(wand.consumeForPlayer(stack,player,costs(attached(stack)),false,false,true)){
                double force=thaumcraft.api.wands.SpellParameters.limited("flight.speed",(1/1.5)*(1+wand.focusPotency(stack,player)*.2),4);player.setDeltaMovement(player.getLookAngle().scale(force));player.fallDistance=0;player.hasImpulse=true;
                if(player instanceof net.minecraft.server.level.ServerPlayer server){
                    ElementalSwordCombat.resetFloating(server);
                    if(server.connection!=null)server.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
                }
                if(level instanceof net.minecraft.server.level.ServerLevel server){thaumcraft.api.fx.LegacyEffects.smokeSpiral(server,player.position().subtract(0,player.getDeltaMovement().y,0),2,player.getY(),0x09e2ff,5);level.playSound(null,player.blockPosition(),TCSounds.WIND.get(),net.minecraft.sounds.SoundSource.PLAYERS,.4f,1);}
                player.swing(hand);
            }
            return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
        }
    }
    public static final class Telekinesis extends TinkererFocus {
        public Telekinesis(){super("focus_telekinesis","TTTE",0x9c00be);}
        @Override public Map<Aspect,Integer> costs(ItemStack focus){return cost("aer",5,"perditio",5);}
        @Override public boolean costPerTick(ItemStack focus){return true;}
        @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack,Level level,Player player,InteractionHand hand,BlockHitResult hit){player.startUsingItem(hand);return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);}
        @Override public void usingTick(ItemStack stack,Player player,int remaining){
            if(!(player.level() instanceof net.minecraft.server.level.ServerLevel level))return;
            var target=player.position().add(0,player.getBbHeight()/2d+.5,0);if(!player.isShiftKeyDown())target=target.add(player.getLookAngle().scale(5));
            var items=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(target,target).inflate(6),e->e.isAlive());
            if(items.isEmpty()||!((CastingWandItem)stack.getItem()).consumeForPlayer(stack,player,costs(attached(stack)),false,false,true))return;
            for(var item:items){pull(item,target,.3333f);dust(level,item.position(),0xdf60df,1,0);}
        }
    }
}
