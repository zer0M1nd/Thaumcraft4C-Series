package tc4revival.content;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;

public  class NodeJarItem extends BlockItem implements StoredAspectItem {
    public NodeJarItem() { super(TCArcane.NODE_JAR.get(),new Properties().stacksTo(1)); }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() { return tc4revival.client.NodeJarRenderer.itemRenderer(); }
        });
    }
    @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack,net.minecraft.world.level.Level level,
                                         java.util.List<net.minecraft.network.chat.Component> lines,net.minecraft.world.item.TooltipFlag flags) {
        var data=NodeJarData.read(stack.hasTag() ? stack.getTag() : new net.minecraft.nbt.CompoundTag());
        var description=net.minecraft.network.chat.Component.translatable("nodetype."+data.type()+".name").withStyle(net.minecraft.ChatFormatting.BLUE);
        if (data.modifier()!=null) description.append(", ").append(net.minecraft.network.chat.Component.translatable("nodemod."+data.modifier()+".name"));
        lines.add(description);
        if(tc4revival.client.AspectTooltips.enabled())return;
        var known=tc4revival.client.ClientKnowledge.snapshot(); var aspects=data.aspects();
        aspects.aspects().stream().sorted(java.util.Comparator.comparing(tc4revival.aspect.Aspect::tag)).forEach(aspect->lines.add(
                known.map(knowledge->knowledge.knows(aspect)).orElse(false) ? net.minecraft.network.chat.Component.literal(thaumcraft.api.aspects.AspectViews.display(aspect).displayName()+" x "+aspects.amount(aspect))
                        : net.minecraft.network.chat.Component.translatable("tc.aspect.unknown")));
    }
    @Override public InteractionResult place(BlockPlaceContext context) {
        var result=super.place(context);
        if (result.consumesAction() && context.getPlayer()!=null && context.getPlayer().getAbilities().instabuild) context.getItemInHand().shrink(1);
        return result;
    }
}
