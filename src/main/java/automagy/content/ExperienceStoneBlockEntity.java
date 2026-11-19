package automagy.content;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
public final class ExperienceStoneBlockEntity extends InventoryBlockEntity implements ExperienceContainer {
 public static final int CAPACITY=2000;
 private final automagy.logic.ExperienceBuffer xp=new automagy.logic.ExperienceBuffer(CAPACITY);private int ticks;
 public ExperienceStoneBlockEntity(BlockPos pos,BlockState state){super(Content.EXPERIENCE_STONE_TILE.get(),pos,state,0);}
 public int experience(){return xp.stored();}public int capacity(){return CAPACITY;}public int comparator(){return xp.comparator();}
 public int insertExperience(int amount,boolean simulate){int n=xp.insert(amount,10,simulate);if(n>0&&!simulate)changed();return n;}
 public int extractExperience(int amount,boolean simulate){int n=xp.extract(amount,10,simulate);if(n>0&&!simulate)changed();return n;}
 public void restoreUnemitted(int amount){if(amount>0){xp.insert(amount,CAPACITY,false);changed();}}
 public int quickDrain(){int n=xp.extract(CAPACITY,CAPACITY,false);if(n>0)changed();return n;}
 private void changed(){sync();if(level!=null&&!level.isClientSide)level.updateNeighbourForOutputSignal(worldPosition,getBlockState().getBlock());}
 public static void tick(Level level,BlockPos pos,BlockState state,ExperienceStoneBlockEntity tile){if(level.isClientSide)return;tile.ticks++;
  if(tile.ticks%10==0)for(var direction:net.minecraft.core.Direction.values()){if(direction==net.minecraft.core.Direction.DOWN)continue;var other=pos.relative(direction);if(level.hasChunkAt(other)&&level.getBlockEntity(other) instanceof AmnesiacStoneBlockEntity stone)stone.balance(tile);}
  if(tile.ticks%75!=0||tile.experience()==0)return;
  for(int depth=1;depth<=2;depth++){BlockPos below=pos.below(depth);if(below.getY()<level.getMinBuildHeight()||!level.hasChunkAt(below))return;
   var be=level.getBlockEntity(below);if(be instanceof ExperienceJarBlockEntity||be instanceof tc4revival.content.EssentiaJarBlockEntity){tile.drip(ExperienceJarBlockEntity.getOrConvert(level,below));return;}
   BlockState obstacle=level.getBlockState(below);boolean openGrate=depth==1&&obstacle.is(tc4revival.content.TCArcane.METAL_GRATE.get())&&!obstacle.getValue(tc4revival.content.MetalGrateBlock.CLOSED);
   if(!obstacle.isAir()&&!openGrate){if(depth==2||be instanceof tc4revival.content.BrainJarBlockEntity||be instanceof tc4revival.content.NodeJarBlockEntity)tile.drip(null);return;}
  }tile.drip(null);
 }
 private void drip(ExperienceJarBlockEntity jar){int amount=extractExperience(10,false);if(jar!=null)jar.insertExperience(amount,false);/* Legacy overflow/spilled droplets are lost, including full jars. */
  if(level instanceof net.minecraft.server.level.ServerLevel server)thaumcraft.api.fx.LegacyEffects.emit(level,"xp_drop",worldPosition.getCenter().add(0,-.52,0),net.minecraft.world.phys.Vec3.ZERO,0x9fcd31,1,128,1,.06f,0);
 }
 @Override protected void saveAdditional(net.minecraft.nbt.CompoundTag tag){super.saveAdditional(tag);tag.putInt("XP",experience());}
 @Override public void load(net.minecraft.nbt.CompoundTag tag){super.load(tag);xp.restore(tag.getInt("XP"));}
}
