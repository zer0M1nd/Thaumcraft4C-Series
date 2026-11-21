package ttmodern;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;

public final class InfusedCropBlock extends CropBlock implements EntityBlock {
    public static final IntegerProperty FORM=IntegerProperty.create("form",0,6);
    public InfusedCropBlock() { super(Properties.copy(Blocks.WHEAT));registerDefaultState(stateDefinition.any().setValue(AGE,0).setValue(FORM,0)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { super.createBlockStateDefinition(builder);builder.add(FORM); }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new InfusedCropEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) { return !level.isClientSide&&type==Tinkerer.INFUSED_CROP_ENTITY.get()?(world,pos,s,tile)->((InfusedCropEntity)tile).tick():null; }
    @Override protected boolean mayPlaceOn(BlockState state,BlockGetter world,BlockPos pos) { return state.is(Tinkerer.INFUSED_FARMLAND.get())||super.mayPlaceOn(state,world,pos); }
    @Override public void randomTick(BlockState state,ServerLevel world,BlockPos pos,RandomSource random) {}
    @Override protected net.minecraft.world.level.ItemLike getBaseSeedId() { return Tinkerer.INFUSED_SEEDS.get(); }
    @Override public boolean isValidBonemealTarget(LevelReader world,BlockPos pos,BlockState state,boolean client) { return TinkererConfig.CROP_BONEMEAL.get()&&super.isValidBonemealTarget(world,pos,state,client); }
    @Override public List<ItemStack> getDrops(BlockState state,LootParams.Builder params) {
        if(!(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof InfusedCropEntity crop))return List.of(InfusedSeedItem.seed("aer",Map.of()));
        var world=params.getLevel();boolean mature=state.getValue(AGE)>=7;var result=new ArrayList<ItemStack>();var seed=InfusedSeedItem.seed(mature?crop.offspring(world):crop.aspect(),crop.traits());
        int count=1;while(count<64&&world.random.nextInt(10000)<crop.trait("perditio")*crop.trait("perditio"))count++;seed.setCount(count);result.add(seed);
        if(mature)for(int rolls=0;rolls<64;rolls++) {
            var table=world.getServer().getLootData().getLootTable(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Tinkerer.ID,"crop_loot/"+crop.aspect().replace(':','/')));
            result.addAll(table.getRandomItems(new LootParams.Builder(world).withParameter(LootContextParams.ORIGIN,crop.getBlockPos().getCenter()).create(LootContextParamSets.CHEST)));
            if(world.random.nextInt(75)>=crop.trait("ordo"))break;
        }
        return result;
    }
    @Override public void playerDestroy(Level world,net.minecraft.world.entity.player.Player player,BlockPos pos,BlockState state,BlockEntity entity,ItemStack tool) {
        super.playerDestroy(world,player,pos,state,entity,tool);if(world instanceof ServerLevel server&&state.getValue(AGE)>=7&&entity instanceof InfusedCropEntity crop)crop.fertilize(server);
    }
    @Override public void spawnAfterBreak(BlockState state,ServerLevel world,BlockPos pos,ItemStack tool,boolean dropXp) {
        if(state.getValue(AGE)>=7&&world.getBlockEntity(pos) instanceof InfusedCropEntity crop)crop.fertilize(world);
    }
    @Override public ItemStack getCloneItemStack(BlockGetter world,BlockPos pos,BlockState state) { return world.getBlockEntity(pos) instanceof InfusedCropEntity crop?InfusedSeedItem.seed(crop.aspect(),crop.traits()):InfusedSeedItem.seed("aer",Map.of()); }
}
