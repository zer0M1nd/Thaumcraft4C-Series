package thaumicomniverse.compat.create;

import com.simibubi.create.content.contraptions.behaviour.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.AllSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

/** Exchanges the real mounted depot slot, preserving its disassembly inventory. */
public class LegacyDepotInteraction extends MovingInteractionBehaviour {
    @Override public boolean handlePlayerInteraction(Player player,InteractionHand hand,BlockPos local,AbstractContraptionEntity entity) {
        if(hand!=InteractionHand.MAIN_HAND)return false;
        if(player.level().isClientSide)return true;
        var storage=LegacyMounted.of(entity.getContraption()).item(local);
        if(storage==null||storage.getSlots()!=1)return false;
        var held=player.getItemInHand(hand);
        if(!held.isEmpty()&&(!storage.isItemValid(0,held)||held.getCount()>storage.getSlotLimit(0)))return false;
        var previous=storage.getStackInSlot(0).copy();
        if(previous.isEmpty()&&held.isEmpty())return true;
        storage.setStackInSlot(0,held.copy());
        player.setItemInHand(hand,previous);
        AllSoundEvents.DEPOT_PLOP.playOnServer(player.level(),BlockPos.containing(entity.toGlobalVector(local.getCenter(),0)));
        return true;
    }
}
