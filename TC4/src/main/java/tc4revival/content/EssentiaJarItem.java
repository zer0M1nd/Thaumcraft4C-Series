package tc4revival.content;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;

public  class EssentiaJarItem extends BlockItem implements StoredAspectItem {
    private final boolean portable;
    public EssentiaJarItem(boolean voidJar,boolean portable) { super(voidJar ? TCArcane.VOID_JAR.get() : TCArcane.ESSENTIA_JAR.get(),new Properties().stacksTo(portable ? 1 : 64)); this.portable=portable; }
    @Override public tc4revival.aspect.AspectAmounts scanAspects(net.minecraft.world.item.ItemStack stack) {
        return new tc4revival.aspect.AspectAmounts();
    }
    @Override public String getDescriptionId() { return portable ? getBlock()==TCArcane.VOID_JAR.get() ? "item.thaumcraft.filled_void_jar" : "item.thaumcraft.filled_essentia_jar" : super.getDescriptionId(); }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() { return tc4revival.client.EssentiaJarRenderer.itemRenderer(); }
        });
    }
    @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack,net.minecraft.world.level.Level level,java.util.List<net.minecraft.network.chat.Component> lines,net.minecraft.world.item.TooltipFlag flags) {
        if(tc4revival.client.AspectTooltips.enabled())return;
        if (!stack.hasTag()) return;
        tc4revival.aspect.TCAspects.catalog().find(stack.getTag().getString("AspectFilter")).ifPresent(filter->lines.add(
                (tc4revival.client.ClientKnowledge.snapshot().map(k->k.knows(filter)).orElse(false) ? net.minecraft.network.chat.Component.literal(thaumcraft.api.aspects.AspectViews.display(filter).displayName())
                        : net.minecraft.network.chat.Component.translatable("tc.aspect.unknown")).withStyle(net.minecraft.ChatFormatting.DARK_PURPLE)));
    }
    @Override public InteractionResult place(BlockPlaceContext context) {
        var result=super.place(context);
        if (portable && result.consumesAction() && context.getPlayer()!=null && context.getPlayer().getAbilities().instabuild) context.getItemInHand().shrink(1);
        return result;
    }
    @Override public void registerBlocks(java.util.Map<net.minecraft.world.level.block.Block,net.minecraft.world.item.Item> map,net.minecraft.world.item.Item item) {
        // Filled forms place the same block but must not replace its ordinary asItem() identity.
        if (!portable) super.registerBlocks(map,item);
    }
}
