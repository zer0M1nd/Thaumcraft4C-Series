package tc4revival.content;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.*;

public  class AlchemyCentrifugeBlockEntity extends BlockEntity implements EssentiaTransport,EssentiaVessel,RevealedAspectContainer {
    private Aspect input,output;private int count,process;public float rotation,rotationSpeed;
    public AlchemyCentrifugeBlockEntity(BlockPos pos,BlockState state){super(TCArcane.ALCHEMY_CENTRIFUGE_ENTITY.get(),pos,state);}
    public Aspect input(){return input;}public int process(){return process;}
    private boolean powered(){return level!=null&&level.hasNeighborSignal(worldPosition);}
    private void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
    @Override public Aspect aspect(){return output;}@Override public int amount(){return output==null?0:1;}@Override public int capacity(){return 1;}
    @Override public boolean accepts(Aspect aspect){return true;}@Override public boolean acceptsPhials(){return false;}
    @Override public AspectAmounts revealedAspects(){return output==null?new AspectAmounts():new AspectAmounts().add(output,1);}
    @Override public boolean contains(Aspect aspect,int amount){return output!=null&&output.equals(aspect)&&amount==1;}
    @Override public int addToContainer(Aspect aspect,int amount){if(aspect!=null&&amount>0&&output==null){output=aspect;changed();return amount-1;}return amount;}
    @Override public boolean takeFromContainer(Aspect aspect,int amount){if(amount==1&&output!=null&&output.equals(aspect)){output=null;changed();return true;}return false;}
    @Override public boolean connectable(Direction face){return face==Direction.UP||face==Direction.DOWN;}
    @Override public boolean canInput(Direction face){return face==Direction.DOWN;}@Override public boolean canOutput(Direction face){return face==Direction.UP;}
    @Override public Aspect suctionType(Direction face){return null;}@Override public int suction(Direction face){return face==Direction.DOWN?(powered()?0:input==null?128:64):0;}
    @Override public int minimumSuction(){return 0;}@Override public Aspect essentiaType(Direction face){return output;}@Override public int essentiaAmount(Direction face){return amount();}
    @Override public int takeEssentia(Aspect aspect,int amount,Direction face){return canOutput(face)&&takeFromContainer(aspect,amount)?1:0;}
    @Override public int addEssentia(Aspect aspect,int amount,Direction face){if(canInput(face)&&amount>0&&input==null&&aspect!=null&&!aspect.isPrimal()){input=aspect;process=39;changed();return 1;}return 0;}
    public void tick(){
        if(level==null)return;
        if(level.isClientSide){
            if(input!=null&&!powered()&&rotationSpeed<20)rotationSpeed+=2;
            if((input==null||powered())&&rotationSpeed>0)rotationSpeed-=.5f;
            int previous=(int)rotation;rotation+=rotationSpeed;
            if(rotation%180<=20&&previous%180>=160&&rotationSpeed>0)level.playLocalSound(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,TCSounds.PUMP.get(),net.minecraft.sounds.SoundSource.BLOCKS,1,1,false);return;
        }
        if(powered())return;
        if(output==null&&input==null&&++count%5==0)draw();
        if(process>0){process--;setChanged();}
        if(output==null&&input!=null&&process==0){output=TCAspects.catalog().require(input.components().get(level.random.nextInt(2)));input=null;changed();}
    }
    private void draw(){
        var pos=worldPosition.below();if(!level.hasChunkAt(pos)||!(level.getBlockEntity(pos) instanceof EssentiaTransport source)||!source.connectable(Direction.UP)||!source.canOutput(Direction.UP))return;
        var aspect=source.essentiaType(Direction.UP);int suction=suction(Direction.DOWN);
        if(aspect!=null&&!aspect.isPrimal()&&source.essentiaAmount(Direction.UP)>0&&source.suction(Direction.UP)<suction&&suction>=source.minimumSuction()&&source.takeEssentia(aspect,1,Direction.UP)==1){input=aspect;process=39;changed();}
    }
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);if(input!=null)tag.putString("aspectIn",input.tag());if(output!=null)tag.putString("aspectOut",output.tag());tag.putInt("Process",process);tag.putInt("TickPhase",count%5);}
    @Override public void load(CompoundTag tag){super.load(tag);input=TCAspects.catalog().find(tag.getString("aspectIn")).filter(aspect->!aspect.isPrimal()).orElse(null);output=TCAspects.catalog().find(tag.getString("aspectOut")).orElse(null);count=Math.floorMod(tag.getInt("TickPhase"),5);process=input==null?0:tag.contains("Process")?Math.max(0,Math.min(39,tag.getInt("Process"))):39;}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
