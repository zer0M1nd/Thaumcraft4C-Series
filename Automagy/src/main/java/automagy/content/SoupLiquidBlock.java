package automagy.content;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
public final class SoupLiquidBlock extends LiquidBlock {
 private final SoupFluids.Soup soup;
 public SoupLiquidBlock(SoupFluids.Soup soup,Properties properties){super(soup.source,properties);this.soup=soup;}
 @Override public void entityInside(BlockState state,Level level,BlockPos pos,Entity entity){super.entityInside(state,level,pos,entity);if(!level.isClientSide&&soup==SoupFluids.VISHROOM)VishroomEffects.touch(level,pos,state.getFluidState().isSource(),entity);}
 @Override public void animateTick(BlockState state,Level level,BlockPos pos,net.minecraft.util.RandomSource random){
  if(soup!=SoupFluids.VISHROOM||!state.getFluidState().isSource()||level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,new net.minecraft.world.phys.AABB(pos)).isEmpty())return;
  var p=new net.minecraft.world.phys.Vec3(pos.getX()+random.nextFloat(),pos.getY()+.1,pos.getZ()+random.nextFloat());
  thaumcraft.api.fx.LegacyEffects.emit(level,"slimy_bubble",p,net.minecraft.world.phys.Vec3.ZERO,0xff4787,.075f+random.nextFloat()*.075f,20,1,0,0);
  if(random.nextInt(8)==0)level.playLocalSound(p.x,pos.getY()+1,p.z,net.minecraft.sounds.SoundEvents.LAVA_POP,net.minecraft.sounds.SoundSource.BLOCKS,.1f+random.nextFloat()*.1f,.9f+random.nextFloat()*.15f,false);
 }
 @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){super.onPlace(state,level,pos,old,moving);if(soup==SoupFluids.VISHROOM)level.scheduleTick(pos,this,7);}
 @Override public void tick(BlockState state,net.minecraft.server.level.ServerLevel level,BlockPos pos,net.minecraft.util.RandomSource random){
  if(soup!=SoupFluids.VISHROOM||!state.getFluidState().isSource())return;
  if(random.nextInt(5)==0){int r=random.nextInt(10);BlockPos target=r<6?pos.relative(net.minecraft.core.Direction.from3DDataValue(r)):pos.offset(r<8?1:-1,0,r==6||r==9?1:-1);if(VishroomEffects.spread(level,target,0)&&random.nextInt(6)==0){VishroomEffects.poof(level,pos);return;}}
  level.scheduleTick(pos,this,7);
 }
}
