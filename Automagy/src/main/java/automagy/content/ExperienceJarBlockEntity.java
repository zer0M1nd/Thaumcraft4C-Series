package automagy.content;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import tc4revival.content.*;
public final class ExperienceJarBlockEntity extends InventoryBlockEntity implements ExperienceContainer {
 public static final int CAPACITY=10545;
 private final automagy.logic.ExperienceBuffer xp=new automagy.logic.ExperienceBuffer(CAPACITY);
 public ExperienceJarBlockEntity(BlockPos pos,BlockState state){super(Content.EXPERIENCE_JAR_TILE.get(),pos,state,0);}
 public int experience(){return xp.stored();}public int capacity(){return CAPACITY;}
 public int comparator(){return xp.comparator();}
 public int insertExperience(int amount,boolean simulate){int n=xp.insert(amount,CAPACITY,simulate);if(n>0&&!simulate)changed();return n;}
 public int extractExperience(int amount,boolean simulate){int n=xp.extract(amount,CAPACITY,simulate);if(n>0&&!simulate)changed();return n;}
 public boolean insertExact(int amount){if(amount<=0||insertExperience(amount,true)!=amount)return false;insertExperience(amount,false);return true;}
 public boolean extractExact(int amount){if(amount<=0||extractExperience(amount,true)!=amount)return false;extractExperience(amount,false);return true;}
 private void changed(){sync();if(level!=null&&!level.isClientSide){level.updateNeighbourForOutputSignal(worldPosition,getBlockState().getBlock());if(experience()==0)level.setBlockAndUpdate(worldPosition,TCArcane.ESSENTIA_JAR.get().defaultBlockState());}}
 public static boolean convertible(Level level,BlockPos pos){return level.hasChunkAt(pos)&&level.getBlockState(pos).is(TCArcane.ESSENTIA_JAR.get())&&level.getBlockEntity(pos) instanceof EssentiaJarBlockEntity jar&&jar.amount()==0&&jar.filter()==null;}
 /** Only empty, unlabelled ordinary warded jars convert; void jars never do. */
 public static ExperienceJarBlockEntity getOrConvert(Level level,BlockPos pos){if(level.isClientSide||!level.hasChunkAt(pos))return null;if(level.getBlockEntity(pos) instanceof ExperienceJarBlockEntity jar)return jar;if(!convertible(level,pos)||!level.setBlockAndUpdate(pos,Content.EXPERIENCE_JAR.get().defaultBlockState()))return null;return level.getBlockEntity(pos) instanceof ExperienceJarBlockEntity jar?jar:null;}
 public ItemStack asItem(){ItemStack stack=new ItemStack(experience()>0?Content.EXPERIENCE_JAR.get():TCArcane.ESSENTIA_JAR.get());if(experience()>0)stack.getOrCreateTagElement("BlockEntityTag").putInt("XP",experience());return stack;}
 @Override protected void saveAdditional(net.minecraft.nbt.CompoundTag tag){super.saveAdditional(tag);tag.putInt("XP",experience());}
 @Override public void load(net.minecraft.nbt.CompoundTag tag){super.load(tag);xp.restore(tag.getInt("XP"));}
}
