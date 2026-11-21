package thmodern.soul;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thmodern.Horizons;
public final class SoulBeaconEntity extends BlockEntity {
    public SoulBeaconEntity(BlockPos pos,BlockState state){super(Horizons.SOUL_BEACON_ENTITY.get(),pos,state);}
    public boolean bind(ServerPlayer player){
        if(player.level()!=level||level==null||player.distanceToSqr(worldPosition.getCenter())>64||level.getBlockEntity(worldPosition)!=this||player.isSpectator())return false;
        var tag=new net.minecraft.nbt.CompoundTag();tag.putString("Dimension",level.dimension().location().toString());tag.putLong("Position",worldPosition.asLong());player.getPersistentData().put(SoulReturn.BINDING,tag);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("thaumichorizons.setBeacon").withStyle(net.minecraft.ChatFormatting.GRAY,net.minecraft.ChatFormatting.ITALIC),false);
        player.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.EFFECT,worldPosition.getX()+.5,worldPosition.getY()+.75,worldPosition.getZ()+.5,10,.4,0,.4,.1);
        level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.BEACON_POWER_SELECT,net.minecraft.sounds.SoundSource.BLOCKS,1,1);return true;
    }
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(){return INFINITE_EXTENT_AABB;}
}
