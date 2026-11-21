package tmmodern;
import net.minecraft.world.item.*;import net.minecraft.world.entity.*;import net.minecraft.world.entity.player.Player;import net.minecraft.world.level.Level;import tc4revival.aspect.Aspect;
public class TMArmor {
    public static com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute,net.minecraft.world.entity.ai.attributes.AttributeModifier> attributes(ArmorItem item,EquipmentSlot slot,com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute,net.minecraft.world.entity.ai.attributes.AttributeModifier> original){var result=com.google.common.collect.HashMultimap.create(original);if(slot==item.getEquipmentSlot()){var existing=original.get(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);java.util.UUID id=existing.isEmpty()?java.util.UUID.nameUUIDFromBytes(slot.getName().getBytes(java.nio.charset.StandardCharsets.UTF_8)):existing.iterator().next().getId();result.removeAll(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);result.put(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR,new net.minecraft.world.entity.ai.attributes.AttributeModifier(id,"Armor modifier",item.getDefense(),net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));}return result;}

    public static void repair(ItemStack s,Level l,Entity e){if(!l.isClientSide&&e.tickCount%20==0&&s.isDamaged())s.setDamageValue(s.getDamageValue()-1);}
    public static class Fortress extends tc4revival.content.FortressArmorItem implements thaumcraft.api.IWarpingGear,tc4revival.content.VisDiscountGear {
        public final boolean shadow;public Fortress(Type type,boolean shadow){super(type);this.shadow=shadow;}
        @Override public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute,net.minecraft.world.entity.ai.attributes.AttributeModifier> getAttributeModifiers(EquipmentSlot slot,ItemStack s){return attributes(this,slot,super.getAttributeModifiers(slot,s));}
        @Override public int getMaxDamage(ItemStack s){return (shadow?3000:300)*switch(getType()){case HELMET->11;case CHESTPLATE->16;case LEGGINGS->15;default->13;};}
        @Override public int getDefense(){return shadow?switch(getType()){case HELMET->4;case CHESTPLATE->10;case LEGGINGS->6;default->0;}:switch(getType()){case HELMET->4;case CHESTPLATE->8;case LEGGINGS->6;default->0;};}
        @Override public int getWarp(ItemStack s,Player p){return shadow?5:3;}@Override public int visDiscount(ItemStack s,Player p,Aspect a){return 5;}
        @Override public int getEnchantmentValue(){return 30;}@Override public void inventoryTick(ItemStack s,Level l,Entity e,int slot,boolean held){if(!shadow)repair(s,l,e);}
        @Override public String getArmorTexture(ItemStack s,Entity e,EquipmentSlot slot,String layer){return "taintedmagic:textures/models/model"+(shadow?"shadow":"void")+"fortressarmor.png";}
        @Override public boolean isValidRepairItem(ItemStack s,ItemStack material){return material.is(shadow?TaintedMagic.item("shadowmetal_ingot"):TMRuntime.item("thaumcraft:void_ingot"));}
    }
    public static class Goggles extends tc4revival.content.GogglesItem implements thaumcraft.api.IWarpingGear {
        public final boolean voidmetal;public Goggles(boolean value){voidmetal=value;}
        @Override public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute,net.minecraft.world.entity.ai.attributes.AttributeModifier> getAttributeModifiers(EquipmentSlot slot,ItemStack s){return attributes(this,slot,super.getAttributeModifiers(slot,s));}
        @Override public int getMaxDamage(ItemStack s){return 11*(voidmetal?300:500);}@Override public int getWarp(ItemStack s,Player p){return voidmetal?5:1;}@Override public int visDiscount(ItemStack s,Player p,Aspect a){return voidmetal?12:super.visDiscount(s,p,a);}@Override public int getDefense(){return 4;}
        @Override public String getArmorTexture(ItemStack s,Entity e,EquipmentSlot slot,String layer){return "taintedmagic:textures/models/model"+(voidmetal?"voidmetal":"warped")+"goggles.png";}
        @Override public void inventoryTick(ItemStack s,Level l,Entity e,int slot,boolean held){if(voidmetal)repair(s,l,e);}
    }
    public static class Boots extends ArmorItem implements thaumcraft.api.IWarpingGear,tc4revival.content.RunicGear,tc4revival.content.VisDiscountGear,tc4revival.content.RepairableItem {
        public Boots(){super(ArmorMaterials.DIAMOND,Type.BOOTS,new Properties().durability(3900).rarity(Rarity.EPIC));}@Override public int getWarp(ItemStack s,Player p){return 5;}@Override public int visDiscount(ItemStack s,Player p,Aspect a){return 5;}@Override public int getDefense(){return 4;}@Override public int getEnchantmentValue(){return 30;}
        @Override public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute,net.minecraft.world.entity.ai.attributes.AttributeModifier> getAttributeModifiers(EquipmentSlot slot,ItemStack s){return attributes(this,slot,super.getAttributeModifiers(slot,s));}
        @Override public String getArmorTexture(ItemStack s,Entity e,EquipmentSlot slot,String layer){return "taintedmagic:textures/models/modelvoidwalkerboots.png";}
        @Override public void inventoryTick(ItemStack s,Level l,Entity e,int slot,boolean held){repair(s,l,e);}
    }
}