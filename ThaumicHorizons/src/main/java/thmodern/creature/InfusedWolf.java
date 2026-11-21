package thmodern.creature;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.*;
import tc4revival.content.*;
import tc4revival.aspect.TCAspects;
import thmodern.Horizons;

public final class InfusedWolf extends Wolf {
    private long nextFireSound;
    public InfusedWolf(EntityType<? extends Wolf> type,Level level){super(type,level);if(type==Horizons.SEAWOLF.get())setPathfindingMalus(BlockPathTypes.WATER,0);}
    public boolean lunar(){return getType()==Horizons.LUNAR_WOLF.get();}
    @Override public boolean fireImmune(){return getType()==Horizons.NETHER_HOUND.get()||super.fireImmune();}
    public static int lunarInterval(float moon){return Math.max(3,Math.min(7,(int)(7-Math.max(0,Math.min(1,moon))*4)));}
    public static void chargePlayer(Player player){
        if(player.level().isClientSide)return;
        charge(player.getMainHandItem());
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(inventory->inventory.getStacksHandler("necklace").ifPresent(handler->{if(handler.getStacks().getSlots()>0)chargeAmulet(handler.getStacks().getStackInSlot(0));}));
        player.getInventory().setChanged();
    }
    private static void charge(ItemStack stack){if(stack.getItem() instanceof CastingWandItem wand)for(var primal:TCAspects.catalog().primals())wand.addVis(stack,primal,1,false,true);}
    private static void chargeAmulet(ItemStack stack){if(stack.getItem() instanceof VisAmuletItem amulet)for(var primal:TCAspects.catalog().primals())amulet.addVis(stack,primal,1,false,true);}
    @Override public void aiStep(){
        super.aiStep();if(!isAlive())return;
        if(getType()==Horizons.SEAWOLF.get()&&isInWater()){moveRelative(.025f,new Vec3(0,0,1));setAirSupply(300);}
        if(level().isClientSide)return;
        if(lunar()&&!level().isDay()&&tickCount%lunarInterval(level().getMoonBrightness())==0)
            for(var player:level().getEntitiesOfClass(Player.class,new AABB(getX()-5,getY()-5,getZ()-5,getX()+5,getY()+5,getZ()+5)))chargePlayer(player);
        if(getType()==Horizons.NETHER_HOUND.get())breatheFire();
    }
    public boolean breatheFire(){
        if(level().isClientSide||!isAlive()||getType()!=Horizons.NETHER_HOUND.get())return false;
        var target=getTarget()!=null?getTarget():getLastHurtByMob();if(target==null||!target.isAlive()||isAlliedTo(target))return false;
        var start=getEyePosition();var end=start.add(getLookAngle().scale(7));
        var block=level().clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));if(block.getType()!=HitResult.Type.MISS)end=block.getLocation();
        var hit=net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(level(),this,start,end,getBoundingBox().expandTowards(end.subtract(start)).inflate(1),entity->entity instanceof LivingEntity&&entity.isPickable());
        if(hit==null||hit.getEntity()!=target)return false;
        var ember=new EmberEntity(level(),this,8,1,1,20);ember.setPos(ember.position().add(ember.getDeltaMovement()));
        if(!level().addFreshEntity(ember))return false;
        if(level().getGameTime()>=nextFireSound){playSound(TCSounds.FIRE_LOOP.get(),.33f,2);nextFireSound=level().getGameTime()+10;}return true;
    }
}
