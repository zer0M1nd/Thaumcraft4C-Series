package tc4revival.content;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import tc4revival.aspect.Aspect;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** Loot-only rings: one percentage point for a single primal and no intrinsic shield. */
public  class ElementalRingItem extends Item implements ICurioItem, VisDiscountGear, RunicGear {
    private final ElementalRingType type;
    public ElementalRingItem(ElementalRingType type) {
        super(new Properties().stacksTo(1));
        this.type = type;
    }
    public ElementalRingType type() { return type; }
    @Override public int visDiscount(ItemStack stack, Player wearer, Aspect aspect) {
        return aspect != null && type.aspectTag().equals(aspect.tag()) ? 1 : 0;
    }
    @Override public boolean canEquip(SlotContext context, ItemStack stack) { return true; }
    @Override public boolean canUnequip(SlotContext context, ItemStack stack) { return true; }
    @Override public boolean canEquipFromUse(SlotContext context, ItemStack stack) { return false; }
    @Override public boolean isValidRepairItem(ItemStack stack, ItemStack material) { return false; }
    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {

    }
}
