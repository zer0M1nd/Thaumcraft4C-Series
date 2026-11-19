package thaumicomniverse.compat.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.*;
import thaumcraft.api.fx.LegacyEffects;

final class FocusEffects {
    static void power(KineticBlockEntity tile,Direction face,float speed){
        if(speed==0)return;
        var normal=Vec3.atLowerCornerOf(face.getNormal());
        var u=normal.cross(face.getAxis()==Direction.Axis.Y?new Vec3(0,0,1):new Vec3(0,1,0)).normalize();
        var v=normal.cross(u);
        // Create rotates around the positive world axis: negative faces invert
        // our face-local basis. One RPM is 0.3 degrees per game tick.
        double angle=Math.toRadians((tile.getLevel().getGameTime()*tile.getSpeed()*.3)%360)
                *face.getAxisDirection().getStep();
        var radial=u.scale(Math.cos(angle)*.32).add(v.scale(Math.sin(angle)*.32));
        var center=tile.getBlockPos().getCenter().add(normal.scale(.57));
        for(int sign:new int[]{-1,1})LegacyEffects.emit(tile.getLevel(),"glow",center.add(radial.scale(sign)),Vec3.ZERO,0xA4DEFF,.34f,4,1,0,0);
    }
    static void outline(AbstractContraptionEntity entity){
        if(entity.level().getGameTime()%2!=0||entity.getContraption()==null)return;
        var box=entity.getContraption().bounds.inflate(.04);
        double t=(entity.level().getGameTime()%40)/40d;
        for(int axis=0;axis<3;axis++)for(int corner=0;corner<4;corner++){
            double[] a={box.minX,box.minY,box.minZ},b={box.maxX,box.maxY,box.maxZ};
            double[] p={0,0,0};p[axis]=a[axis]+(b[axis]-a[axis])*t;
            int i=(axis+1)%3,j=(axis+2)%3;
            p[i]=(corner&1)==0?a[i]:b[i];p[j]=(corner&2)==0?a[j]:b[j];
            // The same TC sparkle and magenta tint as Tinkerer's Telekinesis focus.
            LegacyEffects.emit(entity.level(),"sparkle",entity.toGlobalVector(new Vec3(p[0],p[1],p[2]),1),Vec3.ZERO,0xDF60DF,.45f,10,1,0,0);
        }
    }
    static void formation(AbstractContraptionEntity entity){
        if(entity.getContraption()==null)return;
        var blocks=entity.getContraption().getBlocks().values();int stride=Math.max(1,blocks.size()/64),index=0;
        for(var block:blocks){if(index++%stride!=0)continue;
            var at=entity.toGlobalVector(block.pos().getCenter(),1);
            tc4revival.network.FocusEffectPacket.blockSparkle(entity.level(),net.minecraft.core.BlockPos.containing(at),11960575,2);
        }
        entity.level().playSound(null,entity.blockPosition(),tc4revival.content.TCSounds.WAND.get(),net.minecraft.sounds.SoundSource.PLAYERS,.7f,1);
    }
    private FocusEffects(){}
}
