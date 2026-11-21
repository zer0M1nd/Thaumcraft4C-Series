package thmodern.vat;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** Handbook result icon and creative-only equivalent of the legacy self-infusion cheat. */
public final class SelfInfusionIconItem extends Item {
    private final String key;
    public SelfInfusionIconItem(String key){super(new Properties());this.key=key;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        if(!player.getAbilities().instabuild)return InteractionResultHolder.pass(player.getItemInHand(hand));
        if(!level.isClientSide){var types=SelfInfusions.read(player.getPersistentData().getCompound(SelfInfusions.DATA));types.add(key);SelfInfusions.replace(player,types);}
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
}
