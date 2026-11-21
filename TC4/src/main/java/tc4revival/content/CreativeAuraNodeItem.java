package tc4revival.content;

import net.minecraft.world.item.BlockItem;

/** Plain creative nodes are random; explicit modern or legacy item data survives placement. */
public  class CreativeAuraNodeItem extends BlockItem {
    public CreativeAuraNodeItem() { super(TCArcane.AURA_NODE.get(),new Properties()); }
    static boolean restoreNode(net.minecraft.world.item.ItemStack stack,AuraNodeBlockEntity node) {
        var data=stack.getTagElement("BlockEntityTag");
        if (!hasNodeData(data)) data=stack.getTag();
        if (!hasNodeData(data)) return false;
        // Item placement carries the node's attributes, not a previous drainer or world timer.
        var placed=new net.minecraft.nbt.CompoundTag();
        for (String key:java.util.List.of("Aspects","AspectsBase","nodeVisBase","nodeId","type","modifier"))
            if(data.contains(key))placed.put(key,data.get(key).copy());
        if(!placed.contains("AspectsBase",net.minecraft.nbt.Tag.TAG_LIST) && !placed.contains("nodeVisBase",net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)
                && placed.contains("Aspects",net.minecraft.nbt.Tag.TAG_LIST))placed.put("AspectsBase",placed.get("Aspects").copy());
        node.load(placed);node.sync();return true;
    }
    private static boolean hasNodeData(net.minecraft.nbt.CompoundTag data) {
        return data!=null && (data.contains("Aspects",net.minecraft.nbt.Tag.TAG_LIST)
                || data.contains("AspectsBase",net.minecraft.nbt.Tag.TAG_LIST)
                || data.contains("type",net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)
                || data.contains("modifier",net.minecraft.nbt.Tag.TAG_ANY_NUMERIC));
    }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() { return tc4revival.client.CreativeAuraNodeRenderer.instance(); }
        });
    }
    @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack,net.minecraft.world.level.Level level,
            java.util.List<net.minecraft.network.chat.Component> lines,net.minecraft.world.item.TooltipFlag flags) {
        lines.add(net.minecraft.network.chat.Component.translatable("item.thaumcraft.aura_node.place").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
        lines.add(net.minecraft.network.chat.Component.translatable("item.thaumcraft.aura_node.creative").withStyle(net.minecraft.ChatFormatting.ITALIC));
        super.appendHoverText(stack,level,lines,flags);
    }
}
