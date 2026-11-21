package tc4revival.content;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import tc4revival.aspect.Aspect;

public  class CultistArmorItem extends ArmorItem implements RepairableItem,RunicGear,VisDiscountGear,WarpingGear {
    private final CultistEquipment.Armor definition;
    public CultistArmorItem(CultistEquipment.Armor armor) {
        super(armor.style==CultistEquipment.Style.LEADER ? CultistEquipment.FortressMaterial.INSTANCE : ArmorMaterials.IRON,
                armor.slot,new Properties().rarity(armor.style==CultistEquipment.Style.LEADER ? Rarity.RARE : Rarity.UNCOMMON)); definition=armor;
    }
    public CultistEquipment.Armor definition() { return definition; }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new tc4revival.client.CultistArmorExtensions(()->definition.style));
    }
    @Override public int visDiscount(ItemStack stack,Player player,Aspect aspect) { return definition.style==CultistEquipment.Style.ROBE || definition.style==CultistEquipment.Style.BOOTS ? 1 : 0; }
    @Override public int warp(ItemStack stack,Player player) { return visDiscount(stack,player,null); }
    @Override public boolean isValidRepairItem(ItemStack stack,ItemStack ingredient) { return ingredient.is(Items.IRON_INGOT) || super.isValidRepairItem(stack,ingredient); }
    @Override public String getArmorTexture(ItemStack stack,Entity entity,EquipmentSlot slot,String layer) {
        String name=switch(definition.style) {
            case ROBE->"cultist_robe_armor"; case LEADER->"cultist_leader_armor"; case BOOTS->"cultistboots";
            case PLATE->entity.getType().builtInRegistryHolder().key().location().getPath().equals("inhabited_zombie") ? "zombie_plate_armor" : "cultist_plate_armor";
        };
        return "thaumcraft:textures/models/"+name+".png";
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flags) {

    }
}
