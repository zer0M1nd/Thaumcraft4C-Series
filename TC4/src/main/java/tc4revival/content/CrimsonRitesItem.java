package tc4revival.content;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import tc4revival.research.*;
import tc4revival.network.*;

/** Reading the original cultist book grants CRIMSON once and never consumes the book. */
public  class CrimsonRitesItem extends Item {
    public CrimsonRitesItem() { super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON)); }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            var result=KnowledgeSavedData.get(serverPlayer.server).update(player.getUUID(),level.random::nextInt,
                    knowledge->ResearchCatalogs.progression().completeTrusted(knowledge,"CRIMSON",tc4revival.config.TCConfig.SERVER.disableWarp.get()));
            result.ifPresent(completion->{
                TCNetwork.send(serverPlayer,ResearchCompletionPacket.from(completion));
                level.playSound(null,player.getX(),player.getY(),player.getZ(),TCSounds.LEARN.get(),net.minecraft.sounds.SoundSource.PLAYERS,.75f,1);
            });
        }
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flags) {
        lines.add(Component.translatable("item.ItemEldritchObject.text.2").withStyle(ChatFormatting.DARK_PURPLE));
        lines.add(Component.translatable("item.ItemEldritchObject.text.3").withStyle(ChatFormatting.DARK_BLUE));
    }
}
