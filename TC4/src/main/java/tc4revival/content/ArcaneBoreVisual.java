package tc4revival.content;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

/** Client tick state follows the original aim interpolation, including its overshoot. */
public  class ArcaneBoreVisual {
    public final ArcaneBoreRotation rotation=new ArcaneBoreRotation();
    public float aimX,aimZ;
    public int topRotation,beamLength,paused=100,maxPause=100;
    public int beamAge,beamFade=4;
    private long nextSound;
    private float targetX,targetZ,stepX,stepZ;
    private int sequence;
    private Direction direction;
    public void tick(ArcaneBoreBlockEntity bore) {
        if (direction!=bore.facing()) {
            boolean initial=direction==null; direction=bore.facing(); rotation.face(direction,initial);
            paused=100; targetX=targetZ=stepX=stepZ=0;
        }
        rotation.tick(); var level=bore.getLevel();
        var support=bore.getBlockPos().offset(0,-bore.getBlockState().getValue(ArcaneBoreBlock.MOUNT).getStepY(),0);
        var equipment=ArcaneBoreEquipment.inspect(bore.focus(),bore.pick());
        boolean active=(level.hasNeighborSignal(bore.getBlockPos()) || level.hasNeighborSignal(support)) && equipment.canDig(bore.pick()) && rotation.settled();
        if (!active) {
            beamFade=Math.min(4,beamFade+1);
            if (topRotation%90!=0) topRotation+=Math.min(10,90-topRotation%90);
            aimX*=.9f; aimZ*=.9f; return;
        }
        if (beamFade>=4) beamAge=0;
        beamFade=0; beamAge++;
        paused++; if (beamLength>0 && paused>maxPause) beamLength--;
        if (paused<maxPause && System.currentTimeMillis()>nextSound) {
            nextSound=System.currentTimeMillis()+1200+level.random.nextInt(100);
            tc4revival.client.ArcaneBoreSound.play(bore,.9f+level.random.nextFloat()*.2f);
        }
        if (sequence!=bore.visualSequence() && bore.visualTarget()!=null) {
            sequence=bore.visualSequence(); paused=0; beamLength=64;
            var target=bore.visualTarget(); var origin=bore.getBlockPos();
            float hardness=level.getBlockState(target).getDestroySpeed(level,target);
            maxPause=10+Math.max(10-equipment.speed(),(int)(hardness*2)-equipment.speed()*2);
            if (bore.work().acceleration()<=0) maxPause*=4;
            double x=origin.getX()-target.getX(),y=origin.getY()-target.getY(),z=origin.getZ()-target.getZ();
            targetX=Mth.wrapDegrees((float)rotation.x())+(float)(Math.atan2(z,x)*180/Math.PI);
            if (direction==Direction.EAST) targetX=Mth.wrapDegrees(targetX);
            targetZ=(float)(-Math.atan2(y,Math.sqrt(x*x+z*z))*180/Math.PI)+90-rotation.z();
            if (direction.getAxis()==Direction.Axis.Y) {
                targetZ+=180;
                if (aimX-targetX>=180) aimX-=360;
                if (aimX-targetX<=-180) aimX+=360;
            }
            stepX=Math.abs((aimX-targetX)/6); stepZ=Math.abs((aimZ-targetZ)/6);
        }
        if (paused<maxPause) {
            if (aimX<targetX) aimX+=stepX; else if (aimX>targetX) aimX-=stepX;
            if (aimZ<targetZ) aimZ+=stepZ; else if (aimZ>targetZ) aimZ-=stepZ;
        } else { aimX*=.9f; aimZ*=.9f; }
        stepX*=.9f; stepZ*=.9f; topRotation+=beamLength/6;
        float yaw=(rotation.x()+90-aimX)/180f*(float)Math.PI,pitch=(rotation.z()+90-aimZ)/180f*(float)Math.PI;
        var ray=new net.minecraft.world.phys.Vec3(Mth.sin(yaw)*Mth.cos(pitch),Mth.sin(pitch),Mth.cos(yaw)*Mth.cos(pitch));
        var origin=net.minecraft.world.phys.Vec3.atCenterOf(bore.getBlockPos());
        var hit=level.clip(new net.minecraft.world.level.ClipContext(origin.add(ray),origin.add(ray.scale(beamLength)),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,null));
        if (hit.getType()!=net.minecraft.world.phys.HitResult.Type.MISS) {
            var target=net.minecraft.core.BlockPos.containing(hit.getLocation());
            tc4revival.client.ArcaneBoreDebris.spawn(level,target,bore.getBlockPos().relative(direction),level.getBlockState(target));
        }
    }
}
