package automagy.content;

import automagy.logic.ScribePower;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.visnet.VisNetHandler;

public final class UnseenScribeBlockEntity extends BlockEntity implements InventariumNode {
    private BlockPos core;
    private final ScribePower power=new ScribePower();
    public final automagy.logic.ScribeAnimation animation=new automagy.logic.ScribeAnimation();
    public UnseenScribeBlockEntity(BlockPos pos,BlockState state){super(Content.SCRIBE_TILE.get(),pos,state);}
    @Override public BlockPos linkedCore(){return core;}
    @Override public void linkCore(BlockPos position){core=position==null?null:position.immutable();setChanged();}
    public boolean powered(){return power.powered();}
    @Override public List<InventariumContents.Source> sources(){
        if(level==null||level.isClientSide||!power.powered()||core==null)return List.of();
        Direction facing=getBlockState().getValue(UnseenScribeBlock.FACING);
        Set<Block> selected=new HashSet<>();int range=3;
        if(level.hasChunkAt(worldPosition.above())&&level.getBlockEntity(worldPosition.above()) instanceof ScribePointerBlockEntity pointer){
            range+=pointer.rangeBoosters();selected.addAll(pointer.selectedBlocks());
        }else for(int distance=1;distance<=range;distance++){
            BlockPos target=worldPosition.relative(facing,distance);
            if(!level.hasChunkAt(target))break;
            if(InventariumContents.inventory(level,new InventariumContents.Source(target,null)).isPresent()){selected.add(level.getBlockState(target).getBlock());break;}
        }
        if(selected.isEmpty())return List.of();
        List<InventariumContents.Source> sources=new ArrayList<>();
        for(BlockPos position:BlockPos.betweenClosed(worldPosition.offset(-range,-range,-range),worldPosition.offset(range,range,range)))
            if(level.hasChunkAt(position)&&selected.contains(level.getBlockState(position).getBlock()))sources.add(new InventariumContents.Source(position,null));
        return sources;
    }
    public static void tick(Level level,BlockPos pos,BlockState state,UnseenScribeBlockEntity scribe){
        if(level.isClientSide){
            if(scribe.animation.tick(scribe.powered(),bound->level.random.nextInt(bound)))
                level.playLocalSound(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,
                        net.minecraft.sounds.SoundEvent.createVariableRangeEvent(new net.minecraft.resources.ResourceLocation("thaumcraft","page")),
                        net.minecraft.sounds.SoundSource.BLOCKS,.1f,.9f+level.random.nextFloat()*.1f,false);
            return;
        }
        boolean connected=false;
        if(scribe.core!=null&&level.hasChunkAt(scribe.core)){
            if(level.getBlockEntity(scribe.core) instanceof InventariumBlockEntity core&&core.hasNode(pos))connected=core.capacity().loaded();
            else scribe.linkCore(null);
        }
        int previous=scribe.power.remaining();
        scribe.power.tick(connected,amount->VisNetHandler.drainVis(level,pos.above(),Aspect.ORDER,amount));
        if(previous!=scribe.power.remaining())scribe.setChanged();
        if((previous>0)!=scribe.powered())level.sendBlockUpdated(pos,state,state,3);
    }
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);if(core!=null)tag.put("Core",NbtUtils.writeBlockPos(core));tag.putInt("PoweredTicks",power.remaining());}
    @Override public void load(CompoundTag tag){super.load(tag);core=tag.contains("Core",Tag.TAG_COMPOUND)?NbtUtils.readBlockPos(tag.getCompound("Core")):null;power.restore(tag.getInt("PoweredTicks"));}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
