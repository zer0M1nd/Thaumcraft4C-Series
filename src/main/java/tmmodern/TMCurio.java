package tmmodern;
import net.minecraft.world.*;import net.minecraft.world.entity.player.Player;import net.minecraft.world.item.*;import net.minecraft.world.level.Level;import top.theillusivec4.curios.api.SlotContext;import net.minecraft.network.chat.Component;
public class TMCurio extends Item implements top.theillusivec4.curios.api.type.capability.ICurioItem,thaumcraft.api.IWarpingGear,tc4revival.content.RunicGear {
    public final boolean lumos;public TMCurio(boolean lumos){super(new Properties().stacksTo(1).rarity(Rarity.EPIC));this.lumos=lumos;}
    @Override public boolean canEquip(SlotContext context,ItemStack stack){return context.identifier().equals(lumos?"ring":"belt");}
    @Override public int getWarp(ItemStack s,Player p){return lumos?0:2;}@Override public int intrinsicRunicCharge(ItemStack s){return lumos?0:20;}
    public static boolean speed(ItemStack s){return !s.hasTag()||!s.getTag().contains("mode")||s.getTag().getBoolean("mode");}
    @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand hand){var s=p.getItemInHand(hand);if(!lumos&&p.isShiftKeyDown()){if(!l.isClientSide){s.getOrCreateTag().putBoolean("mode",!speed(s));p.displayClientMessage(Component.translatable("text.sash.speed."+(speed(s)?"on":"off")),true);}return InteractionResultHolder.success(s);}return super.use(l,p,hand);}
    @Override public void appendHoverText(ItemStack s,Level l,java.util.List<Component> lines,TooltipFlag f){if(!lumos)lines.add(Component.translatable("text.sash.speed."+(speed(s)?"on":"off")));}
}
