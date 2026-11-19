package ttmodern;

import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class WarpGateEntity extends BlockEntity {
    private boolean locked;
    private long usedTick=Long.MIN_VALUE;
    private final java.util.Set<java.util.UUID> prompted=new java.util.HashSet<>();
    public final net.minecraftforge.items.ItemStackHandler items=new net.minecraftforge.items.ItemStackHandler(10){
        @Override public int getSlotLimit(int slot){return 1;}
        @Override public boolean isItemValid(int slot,ItemStack stack){return stack.is(Tinkerer.SKY_PEARL.get());}
        @Override protected void onContentsChanged(int slot){changed();}
    };
    public WarpGateEntity(BlockPos pos,BlockState state){super(Tinkerer.WARP_GATE_ENTITY.get(),pos,state);}
    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> itemCapability=net.minecraftforge.common.util.LazyOptional.of(()->items);
    @Override public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability,Direction side){if(capability==net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER&&!isRemoved())return itemCapability.cast();return super.getCapability(capability,side);}
    @Override public void invalidateCaps(){super.invalidateCaps();itemCapability.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();itemCapability=net.minecraftforge.common.util.LazyOptional.of(()->items);}
    @Override public AABB getRenderBoundingBox(){return new AABB(worldPosition).inflate(.5,1,.5);}
    public static boolean enabled(){return TinkererConfig.KAMI.get()&&tc4revival.config.TCConfig.SERVER.allowMirrors.get();}
    public boolean locked(){return locked;}
    public void locked(boolean value){locked=value;changed();}
    private void changed(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public boolean usable(Player player){return enabled()&&player.isAlive()&&!player.isSpectator()&&player.level()==level&&level.getBlockEntity(worldPosition)==this&&player.distanceToSqr(worldPosition.getCenter())<=64&&level.mayInteract(player,worldPosition);}
    public boolean onGate(Player player){return player.getBoundingBox().intersects(new AABB(worldPosition).move(0,1,0).contract(0,.5,0));}
    public void open(Player player,boolean destinations){if(usable(player)&&player instanceof ServerPlayer server)net.minecraftforge.network.NetworkHooks.openScreen(server,new SimpleMenuProvider((id,inventory,owner)->new WarpGateMenu(id,inventory,this,destinations),net.minecraft.network.chat.Component.translatable("block."+Tinkerer.ID+".warp_gate")),buffer->{buffer.writeBlockPos(worldPosition);buffer.writeBoolean(destinations);});}
    public void tick(){
        var current=new java.util.HashSet<java.util.UUID>();for(var player:level.getEntitiesOfClass(Player.class,new AABB(worldPosition).move(0,1,0))){if(player.isShiftKeyDown()&&onGate(player)){current.add(player.getUUID());if(prompted.add(player.getUUID())&&player.containerMenu==player.inventoryMenu)open(player,true);}}prompted.retainAll(current);
    }
    public boolean teleport(ServerPlayer player,int index){
        if(!usable(player)||!onGate(player)||index<0||index>=10||level.getGameTime()==usedTick)return false;
        var pearl=items.getStackInSlot(index);if(!pearl.is(Tinkerer.SKY_PEARL.get())||!SkyPearlItem.bound(pearl)||!level.dimension().location().equals(SkyPearlItem.dimension(pearl)))return false;
        if(!teleportTo(player,SkyPearlItem.position(pearl)))return false;usedTick=level.getGameTime();return true;
    }
    public static boolean teleportTo(ServerPlayer player,BlockPos pos){
        var level=player.serverLevel();if(!enabled()||!player.isAlive()||player.isSpectator()||level.isOutsideBuildHeight(pos)||level.isOutsideBuildHeight(pos.above(3))||!level.getWorldBorder().isWithinBounds(pos))return false;
        level.getChunkAt(pos);if(!(level.getBlockEntity(pos) instanceof WarpGateEntity target)){player.displayClientMessage(net.minecraft.network.chat.Component.translatable("ttmisc.noDest"),true);return false;}if(target.locked()){player.displayClientMessage(net.minecraft.network.chat.Component.translatable("ttmisc.noTeleport"),true);return false;}
        var landing=new net.minecraft.world.phys.Vec3(pos.getX()+.5,pos.getY()+1.6,pos.getZ()+.5);if(!level.noCollision(player,player.getBoundingBox().move(landing.subtract(player.position()))))return false;
        level.playSound(null,player.blockPosition(),tc4revival.content.TCSounds.WAND.get(),net.minecraft.sounds.SoundSource.PLAYERS,1,1);thaumcraft.api.fx.LegacyEffects.sparkle(level,player.position(),0x3333ee,1.5f,20,.5f);
        player.stopRiding();player.closeContainer();player.teleportTo(landing.x,landing.y,landing.z);player.fallDistance=0;
        thaumcraft.api.fx.LegacyEffects.sparkle(level,landing,0x3333ee,1.5f,20,.5f);
        level.playSound(null,pos,tc4revival.content.TCSounds.WAND.get(),net.minecraft.sounds.SoundSource.PLAYERS,1,.1f);return true;
    }
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putBoolean("Locked",locked);tag.put("Inventory",items.serializeNBT());}
    @Override public void load(CompoundTag tag){super.load(tag);locked=tag.getBoolean("Locked");var inventory=tag.getCompound("Inventory").copy();inventory.putInt("Size",10);items.deserializeNBT(inventory);}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
