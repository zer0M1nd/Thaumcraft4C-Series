package thaumicfutures.expansion;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import tc4revival.content.EssentiaJarBlock;

public final class CreativeEssentiaTankBlock extends EssentiaJarBlock {
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new CreativeEssentiaTankEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        return level.isClientSide?null:createTickerHelper(type,RestoredContent.CREATIVE_TANK_ENTITY.get(),(world,pos,block,jar)->jar.tick());
    }
    @Override public List<ItemStack> getDrops(BlockState state,LootParams.Builder params){
        var entity=params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        return List.of(entity instanceof CreativeEssentiaTankEntity tank?tank.asItem():new ItemStack(RestoredContent.CREATIVE_TANK_ITEM.get()));
    }
}
