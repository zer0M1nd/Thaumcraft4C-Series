package conciliummodern;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class CrimsonAnnales extends Item {
    public CrimsonAnnales(){super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);if(!CrimsonContent.available())return InteractionResultHolder.pass(stack);
        if(player instanceof net.minecraft.server.level.ServerPlayer server&&!thaumcraft.api.ThaumcraftApiHelper.isResearchComplete(server,"thaumicconcilium:CRIMSONANNALES")){
            if(thaumcraft.api.ThaumcraftApiHelper.isResearchComplete(server,"CRIMSON")&&thaumcraft.api.ThaumcraftApiHelper.isResearchComplete(server,"thaumicconcilium:TCTAINTEDMAGIC")){
                thaumcraft.api.ThaumcraftApiHelper.completeResearch(server,"thaumicconcilium:CRIMSONANNALES");player.playSound(tc4revival.content.TCSounds.LEARN.get(),.75f,1);
            }else player.displayClientMessage(net.minecraft.network.chat.Component.translatable("tc.tooltip.book.2").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE),false);
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flag){
        lines.add(net.minecraft.network.chat.Component.translatable("tc.tooltip.book.0").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
        lines.add(net.minecraft.network.chat.Component.translatable("tc.tooltip.book.1").withStyle(net.minecraft.ChatFormatting.DARK_BLUE));
    }
}
