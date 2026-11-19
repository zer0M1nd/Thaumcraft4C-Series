package automagy.content;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public final class RedcrystalItem extends BlockItem {
    public RedcrystalItem(Block block) { super(block,new Properties()); }
    @Override public net.minecraft.world.InteractionResult onItemUseFirst(net.minecraft.world.item.ItemStack stack,net.minecraft.world.item.context.UseOnContext context){
        if(!((RedcrystalBlock)getBlock()).kind.equals("merc")||!context.getLevel().hasChunkAt(context.getClickedPos())||!MirrorSignalLinks.mirror(context.getLevel().getBlockEntity(context.getClickedPos())))return net.minecraft.world.InteractionResult.PASS;
        var player=context.getPlayer();if(player==null||!player.mayUseItemAt(context.getClickedPos(),context.getClickedFace(),stack)||!context.getLevel().mayInteract(player,context.getClickedPos()))return net.minecraft.world.InteractionResult.FAIL;
        if(!context.getLevel().isClientSide){
            var bound=stack.copyWithCount(1);bound.getOrCreateTag().put("Mirror",MirrorSignalLinks.write(net.minecraft.core.GlobalPos.of(context.getLevel().dimension(),context.getClickedPos())));
            stack.shrink(1);if(!player.getInventory().add(bound))player.drop(bound,false);player.getInventory().setChanged();
        }return net.minecraft.world.InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
    @Override public boolean isFoil(net.minecraft.world.item.ItemStack stack){return stack.hasTag()&&MirrorSignalLinks.read(stack.getTag().getCompound("Mirror"))!=null;}
    @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack,net.minecraft.world.level.Level level,java.util.List<net.minecraft.network.chat.Component> lines,net.minecraft.world.item.TooltipFlag flags){
        var target=stack.hasTag()?MirrorSignalLinks.read(stack.getTag().getCompound("Mirror")):null;
        if(target!=null)lines.add(net.minecraft.network.chat.Component.translatable("tooltip.automagy.link",target.pos().getX(),target.pos().getY(),target.pos().getZ(),target.dimension().location().toString()));
    }
    @Override public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer==null) renderer=new automagy.client.RedcrystalItemRenderer(); return renderer;
            }
        });
    }
}
