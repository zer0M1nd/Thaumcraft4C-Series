package gadomancymodern.client;
import gadomancymodern.compressor.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
/** Original black fractal bolts, hungry-node block fragments and paired essentia streams. */
public class CompressorEffects {
    protected static final java.util.Map<EssentiaCompressorEntity,ColorCycle> COLORS=new java.util.WeakHashMap<>();
    public static void darkness(DarknessElementEntity entity){var random=entity.level().random;if(random.nextInt(4)==0)FamiliarVisuals.blackBolt(entity.position().add(0,.2,0),entity.position().add((random.nextFloat()-random.nextFloat())*.5,(random.nextFloat()-random.nextFloat())*.5,(random.nextFloat()-random.nextFloat())*.5));}
    public static void tick(EssentiaCompressorEntity tile){var level=Minecraft.getInstance().level;if(level==null)return;var pos=tile.getBlockPos();var center=pos.getCenter().add(0,1,0);var random=level.random;if(tile.stage>=6||(tile.stage<2?random.nextInt(6)==0:random.nextBoolean()))FamiliarVisuals.blackBolt(center,new Vec3(pos.getX()+.4+random.nextFloat()*.2,pos.getY()+.4+(random.nextBoolean()?2.2:0),pos.getZ()+.4+random.nextFloat()*.2));
        var aim=pos.offset(random.nextInt(4)-random.nextInt(4),1+random.nextInt(4)-random.nextInt(4),random.nextInt(4)-random.nextInt(4));int surface=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,aim.getX(),aim.getZ());if(aim.getY()>surface)aim=aim.atY(surface);var direction=aim.getCenter().subtract(center).normalize();var hit=level.clip(new ClipContext(center.add(direction.scale(.51)),aim.getCenter(),ClipContext.Block.COLLIDER,ClipContext.Fluid.ANY,null));if(hit.getType()==HitResult.Type.BLOCK&&!level.getBlockState(hit.getBlockPos()).is(gadomancymodern.Gadomancy.COMPRESSOR.get())&&hit.getBlockPos().distSqr(pos)<16)absorb(hit.getBlockPos(),center,level.getBlockState(hit.getBlockPos()));
        if(tile.pedestal!=null)for(int i=0;i<5;i++)absorb(tile.pedestal.above(),center,tc4revival.content.TCArcane.THAUMIUM_BLOCK.get().defaultBlockState());
        if(!tile.contents.isEmpty()){int color=COLORS.computeIfAbsent(tile,t->new ColorCycle()).update(tile);LegacyEssentiaTrails.add(pos.getCenter().add(0,-.1,0),center,color,.8f,1);LegacyEssentiaTrails.add(pos.getCenter().add(0,2.1,0),center,color,.8f,1);}
    }
    public static void absorb(BlockPos from,Vec3 target,net.minecraft.world.level.block.state.BlockState state){var mc=Minecraft.getInstance();var level=mc.level;if(level==null||state.isAir()||mc.gameRenderer.getMainCamera().getPosition().distanceToSqr(from.getCenter())>gadomancymodern.ClientConfig.limitSquared(Minecraft.useFancyGraphics()?4096:1024))return;var r=level.random;mc.particleEngine.add(new AbsorbedBlockParticle(level,new Vec3(from.getX()+r.nextFloat(),from.getY()+r.nextFloat(),from.getZ()+r.nextFloat()),target,state,from));}
    public static class ColorCycle {public float r=1,g=1,b=1,dr,dg,db;public int update(EssentiaCompressorEntity tile){if(tile.getLevel().getGameTime()%20==0){var aspects=tile.contents.aspects();int color=aspects.get((int)(tile.getLevel().getGameTime()/20%aspects.size())).color();dr=(r-(color>>16&255)/255f)/20;dg=(g-(color>>8&255)/255f)/20;db=(b-(color&255)/255f)/20;}r=net.minecraft.util.Mth.clamp(r-dr,0,1);g=net.minecraft.util.Mth.clamp(g-dg,0,1);b=net.minecraft.util.Mth.clamp(b-db,0,1);return (int)(r*255)<<16|(int)(g*255)<<8|(int)(b*255);}}
}
