package automagy.client;
import automagy.content.InfinityJarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
/** Old jar's 3/10 rune and 4/10 edge sparkle branches, using the public rune API and modern dust. */
public final class InfinityJarEffects {
 public static void animate(Level level,BlockPos pos,RandomSource random){
  int branch=random.nextInt(10);
  if(branch<3){
   var aspect=level.getBlockEntity(pos) instanceof InfinityJarBlockEntity jar?jar.aspect():null;
   int color=aspect==null?0xb882ff:aspect.color();
   tc4revival.client.EldritchWisps.rune(level,pos.getX(),pos.getY(),pos.getZ(),(color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,7,random.nextFloat()*.1f-.05f);
  }else if(branch<7){
   int side=random.nextInt(4);double along=random.nextFloat()*.9;
   double x=side<2?along:side==2?.125:.875,z=side>=2?along:side==0?.125:.875;
   thaumcraft.api.fx.LegacyEffects.emit(level,"dark_sparkle",new net.minecraft.world.phys.Vec3(pos.getX()+x,pos.getY()+.1,pos.getZ()+z),net.minecraft.world.phys.Vec3.ZERO,0x101010,1,18,1,0,-.02f);
  }
 }
 private InfinityJarEffects(){}
}
