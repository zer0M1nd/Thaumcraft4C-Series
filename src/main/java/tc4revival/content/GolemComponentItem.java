package tc4revival.content;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Flattened core, elemental upgrade or decoration; blank cores cannot be installed. */
public  class GolemComponentItem extends Item {
    public enum Kind { CORE, UPGRADE, DECORATION }
    private final Kind kind;
    private final int value;
    public GolemComponentItem(Properties properties,Kind kind,int value) { super(properties); this.kind=kind; this.value=value; }
    public Kind kind() { return kind; }
    public int legacyValue() { return value; }
    @Override public net.minecraft.network.chat.Component getName(ItemStack stack) {
        return kind==Kind.DECORATION?net.minecraft.network.chat.Component.translatable("item.ItemGolemDecoration.name").append(": ").append(super.getName(stack)):super.getName(stack);
    }
    @Override public void appendHoverText(ItemStack stack,net.minecraft.world.level.Level level,java.util.List<net.minecraft.network.chat.Component> tooltip,net.minecraft.world.item.TooltipFlag flag) {
        if (kind==Kind.CORE) tooltip.add(net.minecraft.network.chat.Component.translatable("item.ItemGolemCore."+value+".name"));
        if (kind==Kind.UPGRADE) tooltip.add(net.minecraft.network.chat.Component.translatable("item.ItemGolemUpgrade."+value+".desc"));
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity target,InteractionHand hand) {
        if (!(target instanceof GolemEntity golem) || player.isShiftKeyDown()) return InteractionResult.PASS;
        // Predict without changing the synchronized client configuration.
        var config=golem.configuration(); var data=new net.minecraft.nbt.CompoundTag(); config.save(data);
        var candidate=GolemConfiguration.load(data);
        boolean accepted=switch (kind) {
            case CORE -> candidate.installCore(GolemCoreType.fromLegacy(value));
            case UPGRADE -> candidate.installUpgrade(value);
            case DECORATION -> value>=0 && value<8 && candidate.decorate("HGBFRVPM".charAt(value));
        };
        if (!accepted) return InteractionResult.PASS;
        if (!golem.level().isClientSide) {
            golem.state().configure(candidate); golem.refreshConfiguration(); stack.shrink(1);
            golem.playSound(kind==Kind.DECORATION?TCSounds.CAMERA_CLACK.get():TCSounds.GOLEM_UPGRADE.get(),kind==Kind.DECORATION?1:.5f,1);
            if (kind==Kind.CORE) golem.level().broadcastEntityEvent(golem,(byte)7);
        }
        player.swing(hand);
        return InteractionResult.sidedSuccess(golem.level().isClientSide);
    }
}
