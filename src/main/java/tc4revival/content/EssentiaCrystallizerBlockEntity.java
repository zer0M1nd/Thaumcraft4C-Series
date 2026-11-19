package tc4revival.content;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.*;

public  class EssentiaCrystallizerBlockEntity extends BlockEntity implements EssentiaTransport,EssentiaVessel,RevealedAspectContainer {
    private Aspect aspect; private int count,progress,venting;
    public float spin,spinIncrement,red=1,green=1,blue=1;
    public EssentiaCrystallizerBlockEntity(BlockPos pos,BlockState state){super(TCArcane.ESSENTIA_CRYSTALLIZER_ENTITY.get(),pos,state);}
    public Direction facing(){return getBlockState().getValue(EssentiaCrystallizerBlock.FACING);}
    public int progress(){return progress;}
    public boolean powered(){return level!=null && level.hasNeighborSignal(worldPosition);}
    private void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
    @Override public AspectAmounts revealedAspects(){return aspect==null?new AspectAmounts():new AspectAmounts().add(aspect,1);}
    @Override public Aspect aspect(){return aspect;}
    @Override public int amount(){return aspect==null?0:1;}
    @Override public int capacity(){return 1;}
    @Override public boolean accepts(Aspect value){return true;}
    @Override public boolean acceptsPhials(){return false;}
    @Override public boolean contains(Aspect value,int amount){return amount==1 && aspect!=null && aspect.equals(value);}
    @Override public int addToContainer(Aspect value,int amount){if(value!=null && amount>0 && aspect==null){aspect=value;changed();return amount-1;}return amount;}
    @Override public boolean takeFromContainer(Aspect value,int amount){if(contains(value,amount)){aspect=null;progress=0;changed();return true;}return false;}
    @Override public boolean connectable(Direction face){return face==facing();}
    @Override public boolean canInput(Direction face){return connectable(face);}
    @Override public boolean canOutput(Direction face){return false;}
    @Override public Aspect suctionType(Direction face){return null;}
    @Override public int suction(Direction face){return powered()?0:face==facing() && aspect==null?128:64;}
    @Override public int minimumSuction(){return 0;}
    @Override public Aspect essentiaType(Direction face){return aspect;}
    @Override public int essentiaAmount(Direction face){return amount();}
    @Override public int takeEssentia(Aspect value,int amount,Direction face){return 0;}
    @Override public int addEssentia(Aspect value,int amount,Direction face){return canInput(face)?amount-addToContainer(value,amount):0;}
    public void tick(){
        if(level==null)return;
        if(level.isClientSide){
            int color=aspect==null?0xffffff:aspect.color();float divisor=aspect==null?255:220;
            red=approach(red,(color>>16&255)/divisor);green=approach(green,(color>>8&255)/divisor);blue=approach(blue,(color&255)/divisor);
            spin+=spinIncrement;if(spin>360)spin-=360;
            spinIncrement=aspect!=null&&!powered()?Math.min(20,spinIncrement+.1f):Math.max(0,spinIncrement-.2f);
            if(venting>0){venting--;tc4revival.client.EssentiaCrystallizerRenderer.vent(this);}return;
        }
        if(++count%5==0 && !powered()){
            if(aspect==null){fill();progress=0;}
            else progress+=1+VisNetwork.drain(level,worldPosition,TCAspects.catalog().require("terra"),Math.min(20,Math.max(1,(200-progress)/2)))*2;
        }
        if(aspect!=null && progress>=200 && eject()){aspect=null;progress=0;changed();}
    }
    private static float approach(float value,float target){if(value<target)value+=.05f;if(value>target)value-=.05f;return value;}
    private void fill(){
        var pos=worldPosition.relative(facing());if(!level.hasChunkAt(pos))return;
        if(!(level.getBlockEntity(pos) instanceof EssentiaTransport source))return;
        var side=facing().getOpposite();if(!source.connectable(side)||!source.canOutput(side))return;
        var value=source.essentiaType(side);int suction=suction(facing());
        if(value!=null&&source.essentiaAmount(side)>0&&source.suction(side)<suction&&suction>=source.minimumSuction())addToContainer(value,source.takeEssentia(value,1,side));
    }
    private boolean eject(){
        var stack=TCItems.ESSENCE_CRYSTAL.get().of(aspect,1);var out=facing().getOpposite();var targetPos=worldPosition.relative(out);
        var target=level.hasChunkAt(targetPos)?level.getBlockEntity(targetPos):null;
        if(target!=null){var inventory=target.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER,facing()).resolve();if(inventory.isPresent())stack=net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(inventory.get(),stack,false);}
        if(!stack.isEmpty()){
            var item=new net.minecraft.world.entity.item.ItemEntity(level,worldPosition.getX()+.5+out.getStepX()*.65,worldPosition.getY()+.5+out.getStepY()*.65,worldPosition.getZ()+.5+out.getStepZ()*.65,stack);
            item.setDeltaMovement(out.getStepX()*.04,out.getStepY()*.04,out.getStepZ()*.04);
            if(!level.addFreshEntity(item))return false;
            level.blockEvent(worldPosition,getBlockState().getBlock(),0,0);
        }
        level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,net.minecraft.sounds.SoundSource.BLOCKS,.25f,2.6f+(level.random.nextFloat()-level.random.nextFloat())*.8f);return true;
    }
    @Override public boolean triggerEvent(int id,int value){if(id>=0){if(level!=null&&level.isClientSide)venting=7;return true;}return super.triggerEvent(id,value);}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);if(aspect!=null)tag.putString("Aspect",aspect.tag());tag.putInt("Progress",progress);tag.putInt("TickPhase",count%5);}
    @Override public void load(CompoundTag tag){super.load(tag);aspect=TCAspects.catalog().find(tag.getString("Aspect")).orElse(null);progress=aspect==null?0:Math.max(0,Math.min(200,tag.getInt("Progress")));count=Math.floorMod(tag.getInt("TickPhase"),5);}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
