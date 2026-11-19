package thmodern.containment;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.content.WandInteractable;
import thmodern.Horizons;

public final class CreatureJarEntity extends BlockEntity implements WandInteractable {
    private CompoundTag contents=new CompoundTag();
    private LivingEntity preview;
    public CreatureJarEntity(BlockPos pos,BlockState state){super(Horizons.CREATURE_JAR_ENTITY.get(),pos,state);}
    public void contents(CompoundTag tag){contents=tag.copy();preview=null;setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public ItemStack item(){var stack=new ItemStack(Horizons.CREATURE_JAR_ITEM.get());stack.setTag(contents.copy());return stack;}
    public LivingEntity preview(){if(preview==null&&level!=null&&level.isClientSide)preview=CreatureJar.readContents(contents,level);return preview;}
    public boolean release(ServerPlayer player){
        if(SoulJar.contains(contents)||contents.contains("VatRecovery"))return false;
        if(!(level instanceof ServerLevel server)||player.distanceToSqr(worldPosition.getCenter())>64||!player.mayBuild()||!level.mayInteract(player,worldPosition))return false;
        var entity=CreatureJar.readContents(contents,server);if(entity==null||server.getEntity(entity.getUUID())!=null)return false;
        entity.moveTo(worldPosition.getX()+.5,worldPosition.getY()+1,worldPosition.getZ()+.5,player.getYRot(),0);
        if(entity instanceof net.minecraft.world.entity.TamableAnimal tame&&tame.isTame()&&tame.getOwnerUUID()==null)tame.setOwnerUUID(player.getUUID());
        if(!server.noCollision(entity)||!server.addFreshEntity(entity))return false;
        if(server.getBlockEntity(worldPosition)!=this||!server.getBlockState(worldPosition).equals(getBlockState())||!server.setBlock(worldPosition,Blocks.AIR.defaultBlockState(),3)){entity.discard();return false;}
        server.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.GLASS_BREAK,net.minecraft.sounds.SoundSource.BLOCKS,1,1);return true;
    }
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.put("JarContents",contents.copy());}
    @Override public void load(CompoundTag tag){super.load(tag);contents=tag.getCompound("JarContents").copy();preview=null;}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    @Override public InteractionResult onWandUseFirst(ItemStack stack,UseOnContext context){if(context.getLevel().isClientSide)return InteractionResult.SUCCESS;return context.getPlayer() instanceof ServerPlayer player&&release(player)?InteractionResult.CONSUME:InteractionResult.FAIL;}
    @Override public Optional<ItemStack> onWandUse(Level level,ItemStack stack,Player player){return player instanceof ServerPlayer server&&release(server)?Optional.of(stack):Optional.empty();}
    @Override public void onWandUseTick(ItemStack stack,Player player,int remaining){}
    @Override public void onWandRelease(ItemStack stack,Level level,Player player,int remaining){}
}
