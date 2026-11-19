package conciliummodern;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.Connection;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Original hungryNodeFX/FXBoreParticles: forty textured fragments accelerate toward the caster. */
final class GemCaptureEffects {
    static void receive(GemCaptureNetwork.Capture packet,Connection connection){
        var mc=Minecraft.getInstance();
        if(mc.level==null||mc.getConnection()==null||mc.getConnection().getConnection()!=connection||!connection.isConnected()
                ||!mc.level.dimension().location().equals(packet.dimension())||packet.state().isAir()||!mc.level.hasChunkAt(packet.source()))return;
        double distance=packet.source().getCenter().distanceToSqr(packet.target());if(!Double.isFinite(distance)||distance>65536)return;
        int range=Minecraft.useFancyGraphics()?64:32;
        if(mc.getCameraEntity()==null||mc.getCameraEntity().position().distanceToSqr(packet.source().getCenter())>range*range)return;
        for(int i=0;i<40;i++)mc.particleEngine.add(new Fragment(mc.level,packet.source(),packet.state(),packet.target()));
    }
    private static final class Fragment extends TerrainParticle {
        private final Vec3 target;
        Fragment(ClientLevel level,BlockPos source,BlockState state,Vec3 target){
            super(level,source.getX()+level.random.nextFloat(),source.getY()+level.random.nextFloat(),source.getZ()+level.random.nextFloat(),0,0,0,state,source);
            this.target=target;hasPhysics=true;
            quadSize=.1f*(random.nextFloat()*.3f+.4f);
            int base=Math.max(1,(int)(target.distanceTo(new Vec3(x,y,z))*3));lifetime=base/2+random.nextInt(base);
            xd=level.random.nextGaussian()*.01;yd=level.random.nextGaussian()*.01;zd=level.random.nextGaussian()*.01;
            var side=Direction.from3DDataValue(random.nextInt(6));var mc=Minecraft.getInstance();var model=mc.getBlockRenderer().getBlockModel(state);
            var quads=model.getQuads(state,side,random);setSprite(quads.isEmpty()?model.getParticleIcon():quads.get(0).getSprite());
            rCol=gCol=bCol=.6f;
            if(!state.is(Blocks.GRASS_BLOCK)||side==Direction.UP){int tint=mc.getBlockColors().getColor(state,level,source,0);rCol*=(tint>>16&255)/255f;gCol*=(tint>>8&255)/255f;bCol*=(tint&255)/255f;}
        }
        @Override public void tick(){
            xo=x;yo=y;zo=z;
            if(age++>=lifetime||BlockPos.containing(x,y,z).equals(BlockPos.containing(target))){remove();return;}
            escape();move(xd,yd,zd);xd*=.985;yd*=.985;zd*=.985;
            var delta=target.subtract(x,y,z);double distance=delta.length();if(distance<1e-8){remove();return;}
            double force=distance<4?.6:.3;if(distance<4)quadSize*=.9f;
            xd=Mth.clamp(xd+delta.x/distance*force,-.35,.35);yd=Mth.clamp(yd+delta.y/distance*force,-.35,.35);zd=Mth.clamp(zd+delta.z/distance*force,-.35,.35);
        }
        // Legacy fragments may escape after collision; modern Particle's permanent stopped flag cannot model that.
        @Override public void move(double mx,double my,double mz){
            var moved=net.minecraft.world.entity.Entity.collideBoundingBox(null,new Vec3(mx,my,mz),getBoundingBox(),level,List.of());
            setBoundingBox(getBoundingBox().move(moved));setLocationFromBoundingbox();onGround=my!=moved.y&&my<0;
            if(mx!=moved.x)xd=0;if(my!=moved.y)yd=0;if(mz!=moved.z)zd=0;
        }
        private void escape(){
            var pos=BlockPos.containing(x,y,z);if(level.getBlockState(pos).isAir()||!level.getFluidState(pos).isEmpty())return;
            Direction chosen=null;double nearest=9999;
            for(var side:List.of(Direction.WEST,Direction.EAST,Direction.DOWN,Direction.UP,Direction.NORTH,Direction.SOUTH)){
                var next=pos.relative(side);if(level.getBlockState(next).isRedstoneConductor(level,next))continue;
                double fraction=switch(side.getAxis()){case X->x-pos.getX();case Y->y-pos.getY();case Z->z-pos.getZ();};
                double distance=side.getAxisDirection()==Direction.AxisDirection.NEGATIVE?fraction:1-fraction;
                if(distance<nearest){nearest=distance;chosen=side;}
            }
            float force=random.nextFloat()*.05f+.025f,other=(random.nextFloat()-random.nextFloat())*.1f;
            if(chosen!=null){xd=chosen.getStepX()==0?other:chosen.getStepX()*force;yd=chosen.getStepY()==0?other:chosen.getStepY()*force;zd=chosen.getStepZ()==0?other:chosen.getStepZ()*force;}
        }
    }
    private GemCaptureEffects(){}
}
