package conciliummodern;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** The TT beam used by the original alchemical conversion, including its damage configuration. */
final class ConciliumShadowbeam {
    static void cast(Player player,LivingEntity actor,float potency){
        if(!(actor.level() instanceof ServerLevel level))return;
        var position=actor.getEyePosition().add(0,-.1,0);var step=actor.getLookAngle().normalize().scale(.15);var points=new ArrayList<Vec3>();var hitIds=new HashSet<Integer>();points.add(position);
        float damage=(float)(ttmodern.TinkererConfig.SHADOW_DAMAGE.get()*(1+.2*potency));
        for(int i=0;i<300;i++){
            var end=position.add(step);var blockPos=BlockPos.containing(end);
            if(!level.hasChunkAt(blockPos) || !level.getWorldBorder().isWithinBounds(blockPos))break;
            var block=level.clip(new ClipContext(position,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,actor));
            boolean collide=block.getType()==HitResult.Type.BLOCK;var limit=collide?block.getLocation():end;
            for(var target:level.getEntities(actor,new AABB(position,limit).inflate(.3),e->e.isAlive() && e.isPickable() && !e.isSpectator())){
                if(hitIds.contains(target.getId()) || !thaumcraft.api.wands.WandActors.canTarget(player,target) || target instanceof Player other && (!level.getServer().isPvpAllowed() || !player.canHarmPlayer(other)))continue;
                var bounds=target.getBoundingBox().inflate(.3);
                if(bounds.contains(position) || bounds.clip(position,limit).isPresent()){hitIds.add(target.getId());target.hurt(level.damageSources().playerAttack(player),damage);}
            }
            if(collide){points.add(limit);var normal=Vec3.atLowerCornerOf(block.getDirection().getNormal());step=step.subtract(normal.scale(2*step.dot(normal)));position=limit.add(normal.scale(.001));}
            else position=end;
            if(i%2==0)points.add(position);
        }
        RuneFlow.shadow(actor,points);
    }
    private ConciliumShadowbeam(){}
}
