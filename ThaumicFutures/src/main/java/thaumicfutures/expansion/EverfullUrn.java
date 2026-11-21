package thaumicfutures.expansion;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler;
import tc4revival.aspect.TCAspects;
import tc4revival.content.VisNetwork;

public class EverfullUrn extends BaseEntityBlock {
    public EverfullUrn(){super(Properties.copy(Blocks.TERRACOTTA).strength(2).noOcclusion());}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return box(3,0,3,13,16,13);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new Tile(pos,state);}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        return FluidUtil.interactWithFluidHandler(player,hand,level,pos,Direction.UP)?InteractionResult.sidedSuccess(level.isClientSide):InteractionResult.PASS;
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        return level.isClientSide?null:createTickerHelper(type,RestoredContent.URN_ENTITY.get(),(world,pos,block,tile)->tile.tick());
    }
    public static class Tile extends BlockEntity implements IFluidHandler {
        private static final int CAPACITY=2000;
        private int water,cursor;
        private LazyOptional<IFluidHandler> capability=LazyOptional.of(()->this);
        public Tile(BlockPos pos,BlockState state){super(RestoredContent.URN_ENTITY.get(),pos,state);}
        public void tick(){
            if(level==null||level.isClientSide||!thaumicfutures.FutureFeatures.Group.RESTORATION.enabled())return;
            boolean demand=water<CAPACITY||hasRecipient();
            boolean boosted=demand&&VisNetwork.drain(level,worldPosition,TCAspects.catalog().require("aqua"),1)==1;
            if(!boosted&&level.getGameTime()%5!=0)return;
            int before=water;
            water=Math.min(CAPACITY,water+(boosted?2000:100));
            int budget=boosted?500:25;
            for(int attempt=0;attempt<75&&water>0&&budget>0;attempt++){
                int index=Math.floorMod(cursor++,75);
                var pos=worldPosition.offset(index/15-2,index/5%3-1,index%5-2);
                if(pos.equals(worldPosition)||!level.hasChunkAt(pos))continue;
                var entity=level.getBlockEntity(pos);
                int offered=Math.min(water,budget);
                if(UrnRecipients.fill(entity,offered,false)==0&&(boosted||level.getGameTime()%20==0))offered=water;
                int accepted=UrnRecipients.fill(entity,offered,true);
                if(accepted>0){water-=accepted;budget=Math.max(0,budget-accepted);}
                else{
                    var state=level.getBlockState(pos);
                    if(water>=333&&(boosted||level.getGameTime()%20==0)){
                        if(state.is(Blocks.CAULDRON)){
                            level.setBlockAndUpdate(pos,Blocks.WATER_CAULDRON.defaultBlockState());water-=333;budget=0;
                        }else if(state.is(Blocks.WATER_CAULDRON)&&state.getValue(LayeredCauldronBlock.LEVEL)<3){
                            level.setBlockAndUpdate(pos,state.cycle(LayeredCauldronBlock.LEVEL));water-=333;budget=0;
                        }
                    }
                }
            }
            if(water!=before)changed();
        }
        private boolean hasRecipient(){
            for(var pos:BlockPos.betweenClosed(worldPosition.offset(-2,-1,-2),worldPosition.offset(2,1,2))){
                if(pos.equals(worldPosition)||!level.hasChunkAt(pos))continue;
                var tile=level.getBlockEntity(pos);
                if(UrnRecipients.fill(tile,CAPACITY,false)>0)return true;
                var state=level.getBlockState(pos);
                if(state.is(Blocks.CAULDRON)||(state.is(Blocks.WATER_CAULDRON)&&state.getValue(LayeredCauldronBlock.LEVEL)<3))return true;
            }
            return false;
        }
        private void changed(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
        @Override public int getTanks(){return 1;}
        @Override public FluidStack getFluidInTank(int tank){return tank==0?new FluidStack(Fluids.WATER,water):FluidStack.EMPTY;}
        @Override public int getTankCapacity(int tank){return tank==0?CAPACITY:0;}
        @Override public boolean isFluidValid(int tank,FluidStack fluid){return false;}
        @Override public int fill(FluidStack fluid,FluidAction action){return 0;}
        @Override public FluidStack drain(FluidStack fluid,FluidAction action){return fluid.getFluid()==Fluids.WATER?drain(fluid.getAmount(),action):FluidStack.EMPTY;}
        @Override public FluidStack drain(int amount,FluidAction action){
            int taken=Math.min(water,Math.max(0,amount));if(taken<=0)return FluidStack.EMPTY;
            if(action.execute()){water-=taken;changed();}
            return new FluidStack(Fluids.WATER,taken);
        }
        @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction face){return !isRemoved()&&cap==ForgeCapabilities.FLUID_HANDLER&&(face==null||face==Direction.UP)?capability.cast():super.getCapability(cap,face);}
        @Override public void invalidateCaps(){super.invalidateCaps();capability.invalidate();}
        @Override public void reviveCaps(){super.reviveCaps();capability=LazyOptional.of(()->this);}
        @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putInt("Water",water);tag.putInt("Cursor",cursor);}
        @Override public void load(CompoundTag tag){super.load(tag);water=Math.max(0,Math.min(CAPACITY,tag.getInt("Water")));cursor=Math.floorMod(tag.getInt("Cursor"),75);}
        @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
        @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    }
}
