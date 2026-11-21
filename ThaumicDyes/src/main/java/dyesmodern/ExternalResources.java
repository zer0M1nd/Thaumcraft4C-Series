package dyesmodern;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

/** Names only: original texture pixels are supplied by TC4's external resource loader. */
public final class ExternalResources {
    public static String armorTexture(String family,ItemStack stack,Entity wearer,EquipmentSlot slot,String layer) {
        String texture=switch(family) {
            case "CultistArcherDyed"->"cultist_archer_"+(layer==null?"overlay":"base");
            case "CultistKnightDyed"->"cultist_knight_"+(layer==null?"overlay":"base");
            case "CultistPraetorDyed"->"cultist_praetor_"+(layer==null?"overlay":"base");
            case "CultistRangerDyed"->"cultist_ranger_"+(layer==null?"overlay":"base");
            case "CultistRobesDyed"->layer==null?"robe_armor_overlay":"cultist_robe_base";
            case "ThaumiumFortressDyed"->"fortress_armor_"+(layer==null?"overlay":"base");
            case "ThaumiumRobeDyed"->"thaumium_robe_"+(layer==null?"overlay":"base");
            case "VoidRobeDyed"->layer==null?"robe_armor_overlay":"void_robe_base";
            case "CultistArcher"->"cultist_archer_armor";
            case "CultistPaladin"->"cultist_paladin_armor";
            case "CultistRanger"->"cultist_ranger_armor";
            case "InhabitedKnightArmor"->"zombie_plate_armor";
            case "IronFortressArmor"->"iron_fortress_armor2";
            case "SunKnight"->"sun_plate_armor";
            case "ThaumiumBoots"->"bootsthaumiumtrv";
            case "VoidBoots"->"bootsvoidtrv";
            case "ItemTXAncientMask"->"mask";
            case "ItemRunicBootsTraveller"->"runicbootstraveler";
            case "ItemRunicGoggles"->"runic_goggles";
            case "ItemRunicArmorPrimal","ItemRunicArmorPrimalBoots"->primalTexture(stack,wearer);
            case "ItemRunicArmor","ItemRunicArmorEnhanced"->null;
            default->throw new IllegalArgumentException("Unknown Thaumic Dyes armor family: "+family);
        };
        return texture==null?"thaumicdyes:textures/items/blank.png":"thaumicdyes:textures/models/"+texture+".png";
    }
    private static String primalTexture(ItemStack stack,Entity wearer) {
        String texture="guardian_robe_armor";
        if(wearer instanceof Player player) {
            if(player.getUUID().toString().equals("8c3dbe36-8161-4207-a2d6-e303dfe260ba"))return texture+"_warden1";
            if(has(player,"eldritch_eye"))texture+=has(player,"primordial_pearl")?"_warden":"1";
        }
        return texture+(DyesArmorItem.hardened(stack)?"":"_t");
    }
    private static boolean has(Player player,String id) {
        var item=ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("thaumcraft:"+id));
        if(item==null)return false;
        // The original hasItemStack scans the main inventory, excluding worn armor.
        return player.getInventory().items.stream().anyMatch(stack->stack.is(item));
    }
    private ExternalResources() {}
}
