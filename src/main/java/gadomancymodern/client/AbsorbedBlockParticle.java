package gadomancymodern.client;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
/** TC4 FXBoreParticles movement and terrain UVs, used without destroying its source block. */
public class AbsorbedBlockParticle extends TerrainParticle {
    protected final Vec3 target;
    public AbsorbedBlockParticle(ClientLevel level,Vec3 from,Vec3 to,BlockState state,BlockPos source){super(level,from.x,from.y,from.z,0,0,0,state,source);target=to;quadSize=(random.nextFloat()*.3f+.4f)*.1f;int base=Math.max(1,(int)(from.distanceTo(to)*3));lifetime=base/2+random.nextInt(base);xd=random.nextGaussian()*.01;yd=random.nextGaussian()*.01;zd=random.nextGaussian()*.01;gravity=.2f;hasPhysics=true;}
    @Override public void tick(){xo=x;yo=y;zo=z;if(age++>=lifetime||BlockPos.containing(x,y,z).equals(BlockPos.containing(target))){remove();return;}escapeSource();move(xd,yd,zd);xd*=.985;yd*=.985;zd*=.985;var delta=target.subtract(x,y,z);double distance=delta.length();if(distance<1e-8){remove();return;}double acceleration=distance<4?.6:.3;if(distance<4)quadSize*=.9f;delta=delta.scale(acceleration/distance);xd=Mth.clamp(xd+delta.x,-.35,.35);yd=Mth.clamp(yd+delta.y,-.35,.35);zd=Mth.clamp(zd+delta.z,-.35,.35);}
    protected void escapeSource(){var pos=BlockPos.containing(x,y,z);if(level.isEmptyBlock(pos)||!level.getFluidState(pos).isEmpty())return;double[] distances={x-pos.getX(),1-(x-pos.getX()),y-pos.getY(),1-(y-pos.getY()),z-pos.getZ(),1-(z-pos.getZ())};var directions=new net.minecraft.core.Direction[]{net.minecraft.core.Direction.WEST,net.minecraft.core.Direction.EAST,net.minecraft.core.Direction.DOWN,net.minecraft.core.Direction.UP,net.minecraft.core.Direction.NORTH,net.minecraft.core.Direction.SOUTH};int choice=-1;double nearest=9999;for(int i=0;i<6;i++){var neighbor=pos.relative(directions[i]);if(!level.getBlockState(neighbor).isSolidRender(level,neighbor)&&distances[i]<nearest){nearest=distances[i];choice=i;}}double strength=random.nextFloat()*.05f+.025f,jitter=(random.nextFloat()-random.nextFloat())*.1f;switch(choice){case 0,1->{xd=choice==0?-strength:strength;yd=zd=jitter;}case 2,3->{yd=choice==2?-strength:strength;xd=zd=jitter;}case 4,5->{zd=choice==4?-strength:strength;xd=yd=jitter;}}}
}
