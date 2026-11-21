package automagy.content;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class RedcrystalBlockEntity extends BlockEntity {
    public GlobalPos mirror;
    public boolean mirrorValid,mirrorInput;
    public float rotation,previousRotation,floating=-.2f,previousFloating=-.2f;
    private boolean floatingUp;
    @Override public void onLoad(){super.onLoad();MirrorSignalLinks.loaded(this);}
    @Override public void setRemoved(){MirrorSignalLinks.removed(this);super.setRemoved();}
    public int connections = 63, setting = 1, power;
    public long solvedAt = Long.MIN_VALUE;
    public RedcrystalBlockEntity(BlockPos pos, BlockState state) {
        super(Content.REDCRYSTAL_TILE.get(), pos, state);
        if (kind().equals("amp")) connections = 1 << (face().getAxis()==Direction.Axis.Z ? Direction.UP : Direction.NORTH).ordinal();
        if (kind().equals("dense")) setting = 2;
    }
    public String kind() { return ((RedcrystalBlock)getBlockState().getBlock()).kind; }
    public Direction face() { return getBlockState().getValue(RedcrystalBlock.FACE); }
    public boolean connected(Direction direction) { return (connections & 1 << direction.ordinal()) != 0; }
    public boolean output(Direction direction) {
        return kind().equals("amp") ? direction.getAxis()!=face().getAxis() && connected(direction)
                : direction.getAxis()==face().getAxis() || connected(direction);
    }
    public boolean input(Direction direction) {
        if (direction == face().getOpposite()) return true;
        if (direction == face()) return false;
        return connected(kind().equals("amp") ? direction.getOpposite() : direction);
    }
    public static void tick(Level level, BlockPos pos, BlockState state, RedcrystalBlockEntity be) {
        if(level.isClientSide){
            be.previousRotation=be.rotation;be.rotation+=be.power>0?10:3;if(be.rotation>=3240){be.rotation-=3240;be.previousRotation-=3240;}
            be.previousFloating=be.floating;float bottom=be.power>0?-.1f:-.3f,top=be.power>0?.1f:-.2f;
            if(be.floating<bottom)be.floatingUp=true;else if(be.floating>top)be.floatingUp=false;
            float speed=.0005f*(be.power>0?(be.floating<bottom?12:3):be.floating>top?7:1);be.floating+=be.floatingUp?speed:-speed;return;
        }
        if (be.solvedAt != level.getGameTime()) RedcrystalNetwork.update((ServerLevel)level, pos);
    }
    public void changed() {
        setChanged(); solvedAt = Long.MIN_VALUE;
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);if(mirror!=null)tag.put("Mirror",MirrorSignalLinks.write(mirror));tag.putBoolean("MirrorValid",mirrorValid);tag.putBoolean("MirrorInput",mirrorInput); tag.putInt("Connections", connections); tag.putInt("Setting", setting);
        tag.putInt("Signal", power);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);mirror=tag.contains("Mirror")?MirrorSignalLinks.read(tag.getCompound("Mirror")):null;mirrorValid=tag.getBoolean("MirrorValid");mirrorInput=tag.getBoolean("MirrorInput"); if (tag.contains("Connections")) connections=tag.getInt("Connections")&63;
        setting=Math.max(kind().equals("dense")?2:1, Math.min(kind().equals("dense")?15:14, tag.getInt("Setting")));
        power=Math.max(0, Math.min(32767, tag.getInt("Signal")));
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
