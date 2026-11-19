package tc4revival.content;

public  class JarLabelItem extends net.minecraft.world.item.Item implements StoredAspectItem {
    public JarLabelItem() { super(new Properties()); }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){return tc4revival.client.JarLabelRenderer.instance();}
        });
    }
    @Override public net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> use(net.minecraft.world.level.Level level,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if(!tc4revival.config.TCConfig.SERVER.revival.labelSelection.get())return net.minecraft.world.InteractionResultHolder.pass(stack);
        if(level.isClientSide)net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                ()->()->tc4revival.client.JarLabelScreen.open(hand));
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack,net.minecraft.world.level.Level level,java.util.List<net.minecraft.network.chat.Component> lines,net.minecraft.world.item.TooltipFlag flag) {
        if(tc4revival.config.TCConfig.SERVER.revival.labelSelection.get())lines.add(net.minecraft.network.chat.Component.translatable("thaumcraft.label.choose").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
    public static boolean mark(net.minecraft.server.level.ServerPlayer player,net.minecraft.world.InteractionHand hand,int slot,String tag,boolean all) {
        if(!tc4revival.config.TCConfig.SERVER.revival.labelSelection.get()||!player.isAlive()||player.isSpectator()
                ||hand==net.minecraft.world.InteractionHand.MAIN_HAND&&slot!=player.getInventory().selected)return false;
        var held=player.getItemInHand(hand);
        if(!(held.getItem() instanceof JarLabelItem label))return false;
        var aspect=tc4revival.aspect.TCAspects.catalog().find(tag).orElse(null);
        var knowledge=tc4revival.research.KnowledgeSavedData.get(player.server).snapshot(player.getUUID());
        if(aspect==null||!player.isCreative()&&!knowledge.map(k->k.knows(aspect)).orElse(false))return false;
        var marked=held.copyWithCount(1);label.setStoredAspects(marked,new tc4revival.aspect.AspectAmounts().add(aspect,0));
        if(net.minecraft.world.item.ItemStack.isSameItemSameTags(held,marked))return true;
        if(all||held.getCount()==1)held.setTag(marked.getTag().copy());
        else { if(!player.getInventory().add(marked))return false;held.shrink(1); }
        player.getInventory().setChanged();player.inventoryMenu.broadcastChanges();return true;
    }
}
