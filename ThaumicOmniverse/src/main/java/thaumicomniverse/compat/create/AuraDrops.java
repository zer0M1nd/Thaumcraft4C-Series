package thaumicomniverse.compat.create;

import com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import tc4revival.content.AspectOrbEntity;

/** Converts only drops actually rolled by TC4. Remainders remain their original primal. */
public final class AuraDrops {
    private static final ThreadLocal<BlockPos> CRUSHER=new ThreadLocal<>();
    public static boolean crush(BlockPos pos,Entity entity,net.minecraft.world.damagesource.DamageSource damage,float amount){
        var previous=CRUSHER.get();CRUSHER.set(pos.immutable());
        try{return entity.hurt(damage,amount);}finally{if(previous==null)CRUSHER.remove();else CRUSHER.set(previous);}
    }
    public static void setup(){
        thaumcraft.api.aspects.AspectDropHandlers.registerHandler("thaumicomniverse:aura",0,drop->{
            var player=drop.victim().getKillCredit() instanceof DeployerFakePlayer deployer?deployer:null;
            var crusher=CRUSHER.get();
            if((player==null&&crusher==null)||!thaumicomniverse.RevivalFeatures.Group.AURA.enabled())return java.util.Optional.empty();
            var tag=new net.minecraft.nbt.CompoundTag();if(crusher!=null)tag.put("BypassCrushingWheel",NbtUtils.writeBlockPos(crusher));
            return java.util.Optional.of(new thaumcraft.api.aspects.AspectDropHandlers.Conversion(new ItemStack(AuraContent.SOLID.get()),SolidAuraItem.VIS,crusher==null&&player!=null?player.getInventory():null,tag));
        });
    }
    public static boolean drop(ServerLevel level,Entity entity,LivingEntity victim){
        return entity instanceof AspectOrbEntity orb?thaumcraft.api.aspects.AspectDropHandlers.spawn(level,victim,orb):level.addFreshEntity(entity);
    }
    private AuraDrops(){}
}
