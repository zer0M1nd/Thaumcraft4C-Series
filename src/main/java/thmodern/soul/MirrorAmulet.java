package thmodern.soul;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandlerModifiable;
import tc4revival.content.*;
import thmodern.Horizons;

@Mod.EventBusSubscriber(modid=Horizons.ID)
public final class MirrorAmulet {
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void death(LivingDeathEvent event){if(event.getEntity() instanceof ServerPlayer player)rescue(player);}
    public static boolean rescue(ServerPlayer player){
        var inventory=top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).resolve().orElse(null);if(inventory==null)return false;
        for(var handler:inventory.getCurios().values()){
            var stacks=handler.getStacks();for(int slot=0;slot<stacks.getSlots();slot++)if(stacks.getStackInSlot(slot).is(Horizons.MIRROR_AMULET.get()))return rescue(player,stacks,slot);
        }return false;
    }
    static boolean rescue(ServerPlayer player,IItemHandlerModifiable worn,int index){
        var amulet=worn.getStackInSlot(index);if(!amulet.is(Horizons.MIRROR_AMULET.get()))return false;
        var destination=ItemMirrorItem.destination(amulet);if(destination==null)return false;var level=player.server.getLevel(destination.dimension());if(level==null||level.isOutsideBuildHeight(destination.pos())||!level.getWorldBorder().isWithinBounds(destination.pos()))return false;
        level.getChunkAt(destination.pos());if(!(level.getBlockEntity(destination.pos()) instanceof ItemMirrorBlockEntity mirror)){
            // Clear only the broken binding; custom names and other addon data remain intact.
            for(String key:java.util.List.of("linkDimension","linkX","linkY","linkZ","dimname"))amulet.getOrCreateTag().remove(key);worn.setStackInSlot(index,amulet);player.displayClientMessage(net.minecraft.network.chat.Component.translatable("tc.handmirrorerror"),false);return false;
        }
        boolean moved=false;player.closeContainer();
        for(int slot=0;slot<player.getInventory().getContainerSize();slot++){var item=player.getInventory().getItem(slot);if(!item.isEmpty()&&mirror.spawnItem(item)){player.getInventory().setItem(slot,ItemStack.EMPTY);moved=true;}}
        var accessories=top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).resolve().orElse(null);
        if(accessories!=null)for(var handler:accessories.getCurios().values())for(var stacks:java.util.List.of(handler.getStacks(),handler.getCosmeticStacks()))for(int slot=0;slot<stacks.getSlots();slot++){
            var item=stacks.getStackInSlot(slot);if(item==amulet||item.isEmpty())continue;if(mirror.spawnItem(item)){stacks.setStackInSlot(slot,ItemStack.EMPTY);moved=true;}
        }
        if(!moved)return false;
        var pearl=new ItemStack(TCItems.PRIMORDIAL_PEARL.get());var drop=TCEntities.PERMANENT_ITEM.get().create(player.serverLevel());
        if(drop!=null){drop.setItem(pearl);drop.setPos(player.position());drop.setDefaultPickUpDelay();}
        worn.setStackInSlot(index,drop!=null&&player.serverLevel().addFreshEntity(drop)?ItemStack.EMPTY:pearl);
        mirror.transferEvent();player.getInventory().setChanged();player.inventoryMenu.broadcastChanges();player.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,player.getX(),player.getY()+1,player.getZ(),30,.3,.6,.3,.2);
        player.level().playSound(null,player.blockPosition(),TCSounds.CRAFT_FAIL.get(),net.minecraft.sounds.SoundSource.PLAYERS,1,1);return true;
    }
    private MirrorAmulet(){}
}
