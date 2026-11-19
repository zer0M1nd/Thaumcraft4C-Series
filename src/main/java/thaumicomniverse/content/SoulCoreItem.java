package thaumicomniverse.content;

import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import thaumcraft.api.wands.AutomationCore;

public class SoulCoreItem extends Item implements AutomationCore {
    public SoulCoreItem() { super(new Properties().stacksTo(1).rarity(Rarity.RARE)); }
    @Override public boolean enabled(ItemStack stack) { return thaumicomniverse.RevivalFeatures.Group.SOUL.enabled(); }
    @Override public UUID owner(ItemStack stack) {
        return stack.hasTag() && stack.getTag().hasUUID("Owner") ? stack.getTag().getUUID("Owner") : null;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if(!enabled(stack))return InteractionResultHolder.pass(stack);
        if(player instanceof net.minecraftforge.common.util.FakePlayer)return InteractionResultHolder.fail(stack);
        if(!level.isClientSide) {
            stack.getOrCreateTag().putUUID("Owner",player.getUUID());
            stack.getOrCreateTag().putString("OwnerName",player.getGameProfile().getName());
            player.displayClientMessage(Component.translatable("thaumicomniverse.soul_core.bound",player.getName()),true);
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flags) {
        lines.add(owner(stack)==null?Component.translatable("thaumicomniverse.soul_core.bind"):
                Component.translatable("thaumicomniverse.soul_core.bound",stack.getTag().getString("OwnerName")));
        lines.add(Component.translatable("thaumicomniverse.soul_core.automation"));
    }
}
