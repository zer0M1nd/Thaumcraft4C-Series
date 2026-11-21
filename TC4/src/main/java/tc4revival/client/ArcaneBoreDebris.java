package tc4revival.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.*;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Textured fragments accelerate into the bore instead of falling like vanilla mining debris. */
public class ArcaneBoreDebris extends TextureSheetParticle {
    private final Vec3 target;
    private final float jitterU,jitterV;
    protected ArcaneBoreDebris(ClientLevel level,BlockPos source,Vec3 target,BlockState state) {
        super(level,source.getX()+level.random.nextFloat(),source.getY()+level.random.nextFloat(),source.getZ()+level.random.nextFloat());
        this.target=target; hasPhysics=true; quadSize=.1f*(random.nextFloat()*.3f+.4f);
        setSprite(Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(state));
        int side=random.nextInt(6); int color=state.is(Blocks.GRASS_BLOCK) && side!=1?0xffffff:Minecraft.getInstance().getBlockColors().getColor(state,level,source,0);
        if (color==-1) color=0xffffff;
        rCol=.6f*(color>>16&255)/255f; gCol=.6f*(color>>8&255)/255f; bCol=.6f*(color&255)/255f;
        jitterU=random.nextFloat()*3; jitterV=random.nextFloat()*3;
        int base=Math.max(1,(int)(target.distanceTo(new Vec3(x,y,z))*3)); lifetime=base/2+random.nextInt(base);
        xd=(float)random.nextGaussian()*.01f; yd=(float)random.nextGaussian()*.01f; zd=(float)random.nextGaussian()*.01f;
    }
    public static void spawn(net.minecraft.world.level.Level world,BlockPos source,BlockPos destination,BlockState state) {
        if (!(world instanceof ClientLevel level) || state.isAir()) return;
        var camera=Minecraft.getInstance().getCameraEntity(); int range=Minecraft.useFancyGraphics()?64:32;
        if (camera==null || camera.distanceToSqr(Vec3.atCenterOf(source))>range*range) return;
        if (level.random.nextInt(10)==0) ArcaneBoreSparkles.add(level,source,destination,state);
        else Minecraft.getInstance().particleEngine.add(new ArcaneBoreDebris(level,source,Vec3.atCenterOf(destination),state));
    }
    public static int burstCount() { return switch(Minecraft.getInstance().options.particles().get()) { case MINIMAL->0; case DECREASED->10; default->20; }; }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.TERRAIN_SHEET; }
    @Override protected float getU0() { return sprite.getU(jitterU*4); }
    @Override protected float getU1() { return sprite.getU((jitterU+1)*4); }
    @Override protected float getV0() { return sprite.getV(jitterV*4); }
    @Override protected float getV1() { return sprite.getV((jitterV+1)*4); }
    @Override public void tick() {
        xo=x; yo=y; zo=z;
        if (age++>=lifetime || BlockPos.containing(x,y,z).equals(BlockPos.containing(target))) { remove(); return; }
        escape(); move(xd,yd,zd); xd*=.985; yd*=.985; zd*=.985;
        var delta=target.subtract(x,y,z); double length=delta.length();
        if (length<1e-8) { remove(); return; }
        double acceleration=.3; if (length<4) { quadSize*=.9f; acceleration=.6; }
        xd=Mth.clamp((float)(xd+delta.x/length*acceleration),-.35f,.35f);
        yd=Mth.clamp((float)(yd+delta.y/length*acceleration),-.35f,.35f);
        zd=Mth.clamp((float)(zd+delta.z/length*acceleration),-.35f,.35f);
    }
    private void escape() {
        var pos=BlockPos.containing(x,y,z); if (level.isEmptyBlock(pos) || level.containsAnyLiquid(getBoundingBox())) return;
        Direction chosen=null; double nearest=9999;
        for (var direction:new Direction[]{Direction.WEST,Direction.EAST,Direction.DOWN,Direction.UP,Direction.NORTH,Direction.SOUTH}) {
            var next=pos.relative(direction); if (level.getBlockState(next).isSolidRender(level,next)) continue;
            double fraction=switch(direction.getAxis()) { case X->x-pos.getX(); case Y->y-pos.getY(); case Z->z-pos.getZ(); };
            double distance=direction.getAxisDirection()==Direction.AxisDirection.POSITIVE?1-fraction:fraction;
            if (distance<nearest) { nearest=distance; chosen=direction; }
        }
        float force=random.nextFloat()*.05f+.025f,noise=(random.nextFloat()-random.nextFloat())*.1f;
        if (chosen!=null) {
            xd=chosen.getAxis()==Direction.Axis.X?chosen.getStepX()*force:noise;
            yd=chosen.getAxis()==Direction.Axis.Y?chosen.getStepY()*force:noise;
            zd=chosen.getAxis()==Direction.Axis.Z?chosen.getStepZ()*force:noise;
        }
    }
}
