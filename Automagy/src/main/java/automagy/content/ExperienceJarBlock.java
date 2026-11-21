package automagy.content;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.*;
public final class ExperienceJarBlock extends BaseEntityBlock {
 public ExperienceJarBlock(){super(Properties.of().strength(.3f).sound(SoundType.GLASS).noOcclusion());}
 @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
 @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new ExperienceJarBlockEntity(p,s);}
 @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Shapes.or(box(3,0,3,13,12,13),box(5,12,5,11,14,11));}
 @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.LivingEntity placer,ItemStack stack){super.setPlacedBy(level,pos,state,placer,stack);if(!level.isClientSide&&level.getBlockEntity(pos) instanceof ExperienceJarBlockEntity jar&&jar.experience()==0)level.setBlockAndUpdate(pos,tc4revival.content.TCArcane.ESSENTIA_JAR.get().defaultBlockState());}
 @Override public boolean hasAnalogOutputSignal(BlockState s){return true;}
 @Override public int getAnalogOutputSignal(BlockState s,Level l,BlockPos p){return l.getBlockEntity(p) instanceof ExperienceJarBlockEntity t?t.comparator():0;}
 @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){
  if(!player.mayBuild())return InteractionResult.PASS;
  if(!l.isClientSide&&l.getBlockEntity(p) instanceof ExperienceJarBlockEntity jar){ItemStack held=player.getItemInHand(hand);ItemStack result=ItemStack.EMPTY;boolean changed=false;
   if(held.is(Content.EXPERIENCE_PHIAL.get())){changed=jar.insertExact(ExperiencePhialItem.value());result=ExperiencePhialItem.empty();}
   else if(held.is(tc4revival.content.TCItems.EMPTY_PHIAL.get())){changed=jar.extractExact(ExperiencePhialItem.value());result=new ItemStack(Content.EXPERIENCE_PHIAL.get());}
   else if(held.is(Items.GLASS_BOTTLE)){changed=jar.extractExact(8);result=new ItemStack(Items.EXPERIENCE_BOTTLE);}
   if(changed){player.setItemInHand(hand,ItemUtils.createFilledResult(held,player,result));l.playSound(null,p,net.minecraft.sounds.SoundEvents.GENERIC_SWIM,net.minecraft.sounds.SoundSource.BLOCKS,.25f,1);}
  }return InteractionResult.sidedSuccess(l.isClientSide);
 }
 @Override public ItemStack getCloneItemStack(BlockState s,net.minecraft.world.phys.HitResult hit,BlockGetter l,BlockPos p,Player player){return l.getBlockEntity(p) instanceof ExperienceJarBlockEntity jar?jar.asItem():new ItemStack(this);}
}
