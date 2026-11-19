package emtmodern.item;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public class FeatherWings extends ArmorItem implements WingEquipment,thaumcraft.api.IVisDiscountGear {
    public static final ArmorMaterial MATERIAL=new ArmorMaterial(){
        public int getDurabilityForType(Type type){return 120;}public int getDefenseForType(Type type){return 2;}
        public int getEnchantmentValue(){return 6;}public net.minecraft.sounds.SoundEvent getEquipSound(){return net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER;}
        public net.minecraft.world.item.crafting.Ingredient getRepairIngredient(){return net.minecraft.world.item.crafting.Ingredient.EMPTY;}
        public String getName(){return "emt:feather";}public float getToughness(){return 0;}public float getKnockbackResistance(){return 0;}
    };
    protected final WingFlight.Profile profile;
    public FeatherWings(String kind){super(kind.equals("feather")?MATERIAL:tc4revival.content.ThaumiumArmorMaterial.INSTANCE,Type.CHESTPLATE,new Properties().durability(kind.equals("feather")?120:250));profile=WingFlight.PROFILES.get(kind);}
    @Override public WingFlight.Profile wingProfile(){return profile;}
    @Override public void onArmorTick(ItemStack stack,Level level,Player player){WingFlight.tick(player,stack,profile);}
    @Override public int getVisDiscount(ItemStack stack,Player player,thaumcraft.api.aspects.Aspect aspect){return profile.discount();}
    @Override public String getArmorTexture(ItemStack stack,Entity entity,EquipmentSlot slot,String layer){return "emt:textures/models/"+profile.id()+"wing.png";}
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){consumer.accept(new emtmodern.client.WingArmorExtensions());}
    @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flags){if(profile.discount()>0)lines.add(net.minecraft.network.chat.Component.translatable("tooltip.EMT.visDiscount").append(": "+profile.discount()+"%"));}
    public static class Thaumium extends FeatherWings implements thaumcraft.api.IRepairable {public Thaumium(){super("thaumium");}}
}
