package thaumicomniverse.compat.create;

import com.simibubi.create.content.redstone.thresholdSwitch.*;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.AttachFace;
import thaumcraft.api.aspects.*;
import tc4revival.aspect.TCAspects;

public class EssentiaSwitch extends ThresholdSwitchBlock {
    public EssentiaSwitch(){super(Properties.copy(Blocks.OAK_PLANKS).strength(2).noOcclusion());}
    @Override public BlockEntityType<? extends ThresholdSwitchBlockEntity> getBlockEntityType(){return ArcaneCreateContent.SWITCH_TILE.get();}
    @Override @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    protected void displayScreen(ThresholdSwitchBlockEntity tile,net.minecraft.world.entity.player.Player player){
        if(player instanceof net.minecraft.client.player.LocalPlayer&&tile instanceof Tile detector)
            net.createmod.catnip.gui.ScreenOpener.open(new EssentiaSwitchScreen(detector));
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){
        Direction target=context.getClickedFace().getOpposite();
        return defaultBlockState().setValue(TARGET,target.getAxis()==Direction.Axis.Y?(target==Direction.UP?AttachFace.CEILING:AttachFace.FLOOR):AttachFace.WALL)
            .setValue(FACING,target.getAxis()==Direction.Axis.Y?context.getHorizontalDirection():target);
    }
    public static class Tile extends ThresholdSwitchBlockEntity {
        private boolean active;
        private com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour essentiaFilter;
        public Tile(BlockPos pos,BlockState state){super(ArcaneCreateContent.SWITCH_TILE.get(),pos,state);onWhenAbove=48;offWhenBelow=16;}
        @Override public void tick(){
            if(level!=null&&!level.isClientSide&&!isVirtual()){
                var old=getBlockState();
                if(!old.is(ArcaneCreateContent.SWITCH.get()))return;
                var saved=saveWithoutMetadata();
                // The legacy subclass kept its hysteresis state outside Create's
                // Powered field. Carry that state into the native implementation.
                saved.putBoolean("Powered",active);saved.remove("EssentiaActive");
                var next=com.simibubi.create.AllBlocks.THRESHOLD_SWITCH.getDefaultState()
                    .setValue(FACING,old.getValue(FACING)).setValue(TARGET,old.getValue(TARGET))
                    .setValue(LEVEL,old.getValue(LEVEL));
                // A filter item is owned by the new tile. Prevent removal hooks
                // from also dropping it while replacing the old block entity.
                essentiaFilter.read(new CompoundTag(),false);
                if(!level.setBlock(worldPosition,next,3)){load(saved);return;}
                if(level.getBlockEntity(worldPosition) instanceof ThresholdSwitchBlockEntity replacement){
                    replacement.load(saved);replacement.setChanged();replacement.notifyUpdate();
                    level.scheduleTick(worldPosition,next.getBlock(),2);
                }
                return;
            }
            super.tick();
        }
        // No item/fluid observation behaviours: this detector only sees essentia.
        @Override public void addBehaviours(java.util.List<BlockEntityBehaviour> behaviours){
            essentiaFilter=new com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour(this,new com.simibubi.create.content.redstone.FilteredDetectorFilterSlot(true)).withCallback(stack->updateCurrentLevel());
            behaviours.add(essentiaFilter);
        }
        private IEssentiaStorage storage(){
            if(!(level instanceof net.minecraft.server.level.ServerLevel server)||!ArcaneContraptions.enabled())return null;
            var face=ThresholdSwitchBlock.getTargetDirection(getBlockState());var target=worldPosition.relative(face);
            // Observation is independent of the jar's pipe connection face.
            return level.hasChunkAt(target)&&EssentiaStorageApi.supports(level.getBlockEntity(target),face.getOpposite())?EssentiaStorageApi.atInventory(server,target):null;
        }
        @Override public void updateCurrentLevel(){
            if(level==null||level.isClientSide)return;
            int old=currentLevel,oldMax=currentMaxLevel;boolean oldActive=active;
            var storage=storage();currentMinLevel=0;currentLevel=storage==null?-1:(int)Math.min(Integer.MAX_VALUE,storage.contents().view().entrySet().stream().filter(e->EssentiaFilter.test(essentiaFilter.getFilter(),e.getKey())).mapToLong(e->e.getValue()).sum());
            currentMaxLevel=storage==null?-1:storage.totalCapacity();
            if(currentLevel<0)active=false;else if(active&&currentLevel<=offWhenBelow)active=false;else if(!active&&currentLevel>=onWhenAbove)active=true;
            int bar=currentLevel<=0?0:Math.min(5,1+(int)(4d*currentLevel/Math.max(1,currentMaxLevel)));
            if(getBlockState().getValue(LEVEL)!=bar)level.setBlock(worldPosition,getBlockState().setValue(LEVEL,bar),3);
            if(oldActive!=active)scheduleBlockTick();
            if(old!=currentLevel||oldMax!=currentMaxLevel||oldActive!=active){notifyUpdate();com.simibubi.create.content.redstone.displayLink.DisplayLinkBlock.notifyGatherers(level,worldPosition);}
        }
        @Override public boolean getState(){return active;}
        @Override public boolean shouldBePowered(){return isInverted()!=active;}
        @Override public ThresholdType getTypeOfCurrentTarget(){return currentLevel<0?ThresholdType.UNSUPPORTED:ThresholdType.CUSTOM;}
        @Override public MutableComponent format(int value,boolean stacks){return Component.translatable("thaumicomniverse.essentia_amount",value);}
        @Override public void write(CompoundTag tag,boolean client){super.write(tag,client);tag.putBoolean("EssentiaActive",active);}
        @Override protected void read(CompoundTag tag,boolean client){super.read(tag,client);active=tag.getBoolean("EssentiaActive");}
    }
}
