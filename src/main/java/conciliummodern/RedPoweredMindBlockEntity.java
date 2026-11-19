package conciliummodern;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class RedPoweredMindBlockEntity extends BlockEntity {
    private int power;
    private float rotation,previousRotation,desired;
    public RedPoweredMindBlockEntity(BlockPos pos,BlockState state){super(ConciliumBlocks.RED_MIND_TILE.get(),pos,state);}
    public int power(){return power;}
    public float rotation(float partial){return previousRotation+Mth.wrapDegrees(rotation-previousRotation)*partial;}
    public void sample(){if(level==null||level.isClientSide)return;int value=level.getBestNeighborSignal(worldPosition);
        if(value!=power){power=value;setChanged();level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}}
    public static void tick(Level level,BlockPos pos,BlockState state,RedPoweredMindBlockEntity mind){
        if(!level.isClientSide){if(level.getGameTime()%20==0)mind.sample();return;}
        mind.previousRotation=mind.rotation;var player=level.getNearestPlayer(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,6,false);
        mind.desired=player==null?mind.desired+.5729578f:(float)Math.toDegrees(Math.atan2(player.getZ()-pos.getZ()-.5,player.getX()-pos.getX()-.5));
        mind.rotation=Mth.wrapDegrees(mind.rotation+Mth.wrapDegrees(mind.desired-mind.rotation)*.04f);
    }
    @Override public void onLoad(){super.onLoad();sample();}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putInt("Power",power);}
    @Override public void load(CompoundTag tag){super.load(tag);power=Mth.clamp(tag.getInt("Power"),0,15);}
    @Override public CompoundTag getUpdateTag(){var tag=new CompoundTag();tag.putInt("Power",power);return tag;}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public void onDataPacket(net.minecraft.network.Connection connection,ClientboundBlockEntityDataPacket packet){if(packet.getTag()!=null)load(packet.getTag());}
}
