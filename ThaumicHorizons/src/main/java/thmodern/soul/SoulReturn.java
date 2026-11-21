package thmodern.soul;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import thmodern.Horizons;
import thmodern.vat.*;

@Mod.EventBusSubscriber(modid=Horizons.ID)
public final class SoulReturn {
    public static final String BINDING="thaumichorizons:soul_beacon";
    @SubscribeEvent(priority=EventPriority.HIGH)
    public static void death(LivingDeathEvent event){if(event.getEntity() instanceof ServerPlayer player&&restore(player))event.setCanceled(true);}
    @SubscribeEvent public static void clonePlayer(PlayerEvent.Clone event){if(event.getOriginal().getPersistentData().contains(BINDING))event.getEntity().getPersistentData().put(BINDING,event.getOriginal().getPersistentData().getCompound(BINDING).copy());}
    public static boolean restore(ServerPlayer player){
        if(player.isSpectator())return false;var data=player.getPersistentData().getCompound(BINDING);
        var key=ResourceLocation.tryParse(data.getString("Dimension"));if(key==null||!data.contains("Position",net.minecraft.nbt.Tag.TAG_LONG))return false;
        var target=player.server.getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,key));if(target==null)return false;
        var pos=BlockPos.of(data.getLong("Position"));if(target.isOutsideBuildHeight(pos)||!target.getWorldBorder().isWithinBounds(pos))return false;
        // An explicitly bound resurrection destination may be outside loaded player chunks.
        target.getChunkAt(pos);
        if(!(target.getBlockEntity(pos) instanceof SoulBeaconEntity)||!(target.getBlockEntity(pos.below()) instanceof VatEntity vat)||!vat.readyForSoul())return false;
        var origin=player.serverLevel();var location=player.position();
        player.closeContainer();if(!vat.inhabitEffigy(player))return false;
        for(int slot=0;slot<player.getInventory().getContainerSize();slot++)if(drop(origin,location,player.getInventory().getItem(slot)))player.getInventory().setItem(slot,ItemStack.EMPTY);
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(inventory->inventory.getCurios().values().forEach(handler->{
            for(var stacks:java.util.List.of(handler.getStacks(),handler.getCosmeticStacks()))for(int slot=0;slot<stacks.getSlots();slot++)if(drop(origin,location,stacks.getStackInSlot(slot)))stacks.setStackInSlot(slot,ItemStack.EMPTY);
        }));
        player.getInventory().setChanged();player.inventoryMenu.broadcastChanges();
        player.removeAllEffects();SelfInfusions.refresh(player);player.setHealth(player.getMaxHealth());player.clearFire();player.fallDistance=0;player.setAirSupply(player.getMaxAirSupply());player.setDeltaMovement(Vec3.ZERO);
        origin.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,location.x,location.y+1,location.z,1,0,0,0,0);
        for(int i=0;i<25;i++){
            var spill=BlockPos.containing(location).offset(origin.random.nextInt(8)-origin.random.nextInt(8),origin.random.nextInt(8)-origin.random.nextInt(8),origin.random.nextInt(8)-origin.random.nextInt(8));
            if(!origin.isOutsideBuildHeight(spill)&&origin.hasChunkAt(spill)&&origin.getWorldBorder().isWithinBounds(spill)&&origin.isEmptyBlock(spill))origin.setBlock(spill,(spill.getY()<=location.y+1?tc4revival.content.TCArcane.FLUX_GOO.get():tc4revival.content.TCArcane.FLUX_GAS.get()).defaultBlockState().setValue(tc4revival.content.FiniteFluidBlock.QUANTA,1),3);
        }
        target.playSound(null,pos,net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,net.minecraft.sounds.SoundSource.PLAYERS,1,1);return true;
    }
    private static boolean drop(ServerLevel level,Vec3 pos,ItemStack stack){if(stack.isEmpty())return false;var item=new net.minecraft.world.entity.item.ItemEntity(level,pos.x,pos.y,pos.z,stack.copy());item.setDefaultPickUpDelay();return level.addFreshEntity(item);}
    private SoulReturn(){}
}
