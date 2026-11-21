package thmodern.containment;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.*;

public final class CreatureJarBlock extends BaseEntityBlock {
    public CreatureJarBlock(){super(Properties.of().strength(.3f,1).sound(SoundType.GLASS).lightLevel(s->10).noOcclusion());}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return box(3,0,3,13,12,13);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new CreatureJarEntity(pos,state);}
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack){if(level.getBlockEntity(pos) instanceof CreatureJarEntity jar&&stack.hasTag())jar.contents(stack.getTag());}
    @Override public List<ItemStack> getDrops(BlockState state,LootParams.Builder builder){return builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof CreatureJarEntity jar?List.of(jar.item()):List.of();}
    @Override public void playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player){if(!level.isClientSide&&player.isCreative()&&level.getBlockEntity(pos) instanceof CreatureJarEntity jar)popResource(level,pos,jar.item());super.playerWillDestroy(level,pos,state,player);}
    @Override public ItemStack getCloneItemStack(BlockGetter level,BlockPos pos,BlockState state){return level.getBlockEntity(pos) instanceof CreatureJarEntity jar?jar.item():ItemStack.EMPTY;}
}
