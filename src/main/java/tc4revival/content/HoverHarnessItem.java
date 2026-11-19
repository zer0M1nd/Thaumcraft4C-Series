package tc4revival.content;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.LegacyAspect;
import tc4revival.aspect.TCAspects;

public  class HoverHarnessItem extends ArmorItem implements RepairableItem, RunicGear, VisDiscountGear, LegacyHasteEnchantment.Harness {
    public HoverHarnessItem() { super(SpecialArmorMaterial.INSTANCE, Type.CHESTPLATE, new Properties().durability(400)); }
    @Override public int getMaxDamage(ItemStack stack) { return 400; }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            private tc4revival.client.HoverHarnessModel model;
            @Override public net.minecraft.client.model.Model getGenericArmorModel(net.minecraft.world.entity.LivingEntity entity, ItemStack stack, EquipmentSlot slot, net.minecraft.client.model.HumanoidModel<?> original) {
                if (model == null) model = new tc4revival.client.HoverHarnessModel(); model.prepare(entity, original); return model;
            }
        });
    }
    @Override public Rarity getRarity(ItemStack stack) { return Rarity.EPIC; }
    @Override public boolean allowHotbarRepair() { return false; }
    @Override public boolean isValidRepairItem(ItemStack stack, ItemStack material) { return material.is(Items.GOLD_INGOT) || super.isValidRepairItem(stack, material); }
    @Override public int visDiscount(ItemStack stack, Player player, Aspect aspect) { return TCAspects.get(LegacyAspect.AIR).equals(aspect) ? 5 : 2; }
    @Override public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String layer) { return "thaumcraft:textures/models/hoverharness.png"; }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer server) net.minecraftforge.network.NetworkHooks.openScreen(server,
                new SimpleMenuProvider((id, inventory, owner) -> new HoverHarnessMenu(id, inventory, hand), Component.translatable("container.hoverharness")),
                buffer -> buffer.writeEnum(hand));
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack, Level level, java.util.List<Component> lines, TooltipFlag flags) {
        if(tc4revival.client.AspectTooltips.enabled())return;
        var jar = HoverFuel.jar(stack);
        if (jar.getItem() instanceof EssentiaJarItem container) for (var aspect : container.storedAspects(jar).aspects())
            lines.add(tc4revival.client.ClientKnowledge.snapshot().map(k -> k.knows(aspect)).orElse(false)
                    ? Component.literal(thaumcraft.api.aspects.AspectViews.display(aspect).displayName() + " x " + container.storedAspects(jar).amount(aspect)) : Component.translatable("tc.aspect.unknown"));


    }
}
