package tc4revival.content;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import tc4revival.aspect.TCAspects;
import tc4revival.research.KnowledgeSavedData;
import tc4revival.research.ResearchCatalogs;
import tc4revival.research.ResearchProgression;

public  class PrimalCharmItem extends Item implements StoredAspectItem {
    public PrimalCharmItem() { super(new Properties().stacksTo(1)); }
    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide) return;
        int roll = level.random.nextInt(20000);
        if (stack.hasTag()) stack.getTag().remove("blurb");
        if (roll < 20) {
            var names = new String[]{"aer", "terra", "ignis", "aqua", "ordo", "perditio"};
            var aspect = TCAspects.catalog().require(names[level.random.nextInt(6)]);
            level.addFreshEntity(new AspectOrbEntity(level, entity.getX(), entity.getY(), entity.getZ(), aspect, 1));
        } else if (roll == 42 && entity instanceof ServerPlayer player) {
            var store = KnowledgeSavedData.get(player.getServer());
            var known = store.snapshot(player.getUUID());
            if (known.isPresent() && (known.get().researchMarkers().contains("FOCUSPRIMAL") || known.get().researchMarkers().contains("@FOCUSPRIMAL"))) return;
            store.update(player.getUUID(), level.random::nextInt, knowledge -> new ResearchProgression(ResearchCatalogs.catalog(), ResearchCatalogs::enabled)
                    .completeTrusted(knowledge, "@FOCUSPRIMAL", tc4revival.config.TCConfig.SERVER.disableWarp.get()));
            player.sendSystemMessage(Component.translatable("tc.primalcharm.trigger").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
            tc4revival.network.TCNetwork.send(player, new tc4revival.network.CluePacket("FOCUSPRIMAL"));
        }
    }
    @Override public void appendHoverText(ItemStack stack, Level level, java.util.List<Component> lines, TooltipFlag flags) {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) return;
        // Recipe views recreate stacks while rendering. Object identity must not
        // change the tooltip between layout and drawing, or between frames.
        var random = new java.util.Random(player.getUUID().getLeastSignificantBits() ^ (player.tickCount / 120L));
        if (random.nextInt(200) < 25) lines.add(Component.translatable("tc.primalcharm." + random.nextInt(5)).withStyle(ChatFormatting.GOLD));
    }
}
