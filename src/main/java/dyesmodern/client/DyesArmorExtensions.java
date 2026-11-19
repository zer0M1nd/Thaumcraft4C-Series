package dyesmodern.client;

import dyesmodern.DyesArmorItem;
import net.minecraft.client.model.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public final class DyesArmorExtensions implements IClientItemExtensions {
    private final DyesArmorItem item;
    private DyesArmorModel inner,outer;
    private DyesArmorExtensions(DyesArmorItem item){this.item=item;}
    public static IClientItemExtensions create(DyesArmorItem item){return new DyesArmorExtensions(item);}
    @Override public Model getGenericArmorModel(LivingEntity wearer,ItemStack stack,EquipmentSlot slot,HumanoidModel<?> original) {
        String kind=switch(item.family) {
            case "CultistArcher","CultistArcherDyed"->"Archer";
            case "ThaumiumFortressDyed"->"Fortress";
            case "IronFortressArmor"->"IronFortress";
            case "CultistKnightDyed","InhabitedKnightArmor","SunKnight"->"Knight";
            case "CultistPaladin"->"Paladin";
            case "CultistPraetorDyed"->"Praetor";
            case "CultistRangerDyed"->"Ranger";
            case "CultistRanger"->"Ranger2";
            case "CultistRobesDyed","ThaumiumRobeDyed","VoidRobeDyed"->"Robes";
            case "ItemRunicArmorPrimal","ItemRunicArmorPrimalBoots"->"RobesSpecial";
            default->null;
        };
        if(kind==null)return original;
        boolean outside=slot==EquipmentSlot.CHEST||slot==EquipmentSlot.FEET;
        if(outside&&outer==null)outer=new DyesArmorModel(kind,true);
        if(!outside&&inner==null)inner=new DyesArmorModel(kind,false);
        var model=outside?outer:inner;model.prepare(wearer,stack,slot,original);return model;
    }
}
