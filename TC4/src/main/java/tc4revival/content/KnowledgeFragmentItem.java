package tc4revival.content;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import tc4revival.aspect.TCAspects;
import tc4revival.network.ScanFeedbackPacket;
import tc4revival.network.TCNetwork;
import tc4revival.research.KnowledgeFragments;
import tc4revival.research.KnowledgeSavedData;
import tc4revival.research.ScanRules;

public  class KnowledgeFragmentItem extends Item implements StoredAspectItem {
    public KnowledgeFragmentItem() { super(new Properties()); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            var rewards = KnowledgeSavedData.get(serverPlayer.getServer()).update(player.getUUID(), level.random::nextInt,
                    knowledge -> KnowledgeFragments.consume(knowledge, TCAspects.catalog(), level.random::nextInt));
            if (!player.getAbilities().instabuild) stack.shrink(1);
            TCNetwork.send(serverPlayer, ScanFeedbackPacket.from(new ScanRules.Result(ScanRules.Status.SUCCESS, java.util.Optional.empty(), rewards)));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
