package gadomancymodern.item;
public class AdditionalGolemCoreItem extends tc4revival.content.GolemComponentItem {
    public final net.minecraft.resources.ResourceLocation task;
    public AdditionalGolemCoreItem(net.minecraft.resources.ResourceLocation task){super(new Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON),Kind.CORE,100);this.task=task;}
    @Override public net.minecraft.network.chat.Component getName(net.minecraft.world.item.ItemStack stack){return net.minecraft.network.chat.Component.translatable("item.ItemGolemCore.name");}
    @Override public net.minecraft.world.InteractionResult interactLivingEntity(net.minecraft.world.item.ItemStack stack,net.minecraft.world.entity.player.Player player,net.minecraft.world.entity.LivingEntity target,net.minecraft.world.InteractionHand hand){return net.minecraft.world.InteractionResult.PASS;}
    @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack,net.minecraft.world.level.Level level,java.util.List<net.minecraft.network.chat.Component> tooltip,net.minecraft.world.item.TooltipFlag flag){tooltip.add(net.minecraft.network.chat.Component.translatable(task.equals(gadomancymodern.golem.GadGolems.BREAK)?"gadomancy.golem.breakcore":"gadomancy.golem.bodyguardcore"));}
}
