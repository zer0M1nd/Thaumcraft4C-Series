package dyesmodern;

import com.google.common.collect.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import tc4revival.aspect.Aspect;
import tc4revival.content.*;

/** Original 1.10.5 families share registration and data without losing their per-family behavior. */
public class DyesArmorItem extends ArmorItem implements RunicGear,VisDiscountGear,WarpingGear,RevealingGear {
    public final String family,legacyKey;
    public DyesArmorItem(String family,String legacyKey,ArmorMaterial material,Type type) {
        super(material,type,new Properties().stacksTo(1));this.family=family;this.legacyKey=legacyKey;
    }
    public boolean dyeable(){return family.endsWith("Dyed");}
    public boolean runic(){return family.startsWith("ItemRunic");}
    public boolean primal(){return family.startsWith("ItemRunicArmorPrimal");}
    public int upgradeSlots(){return primal()?3:family.equals("ItemRunicArmorEnhanced")?2:runic()?1:0;}
    public boolean traveller(){return family.equals("ThaumiumBoots")||family.equals("VoidBoots")||family.equals("ItemRunicBootsTraveller")||family.equals("ItemRunicArmorPrimalBoots");}
    public boolean specialArmor(){return runic()||Set.of("IronFortressArmor","ThaumiumFortressDyed","ThaumiumRobeDyed","VoidRobeDyed").contains(family);}
    public boolean hasCustomColor(ItemStack stack){return dyeable();}
    public int getColor(ItemStack stack){
        // Modern damageable stacks always carry Damage, whereas legacy durability was metadata.
        if(!stack.hasTag()||stack.getTag().getAllKeys().stream().allMatch("Damage"::equals))return 0xFFFFFF;
        var display=stack.getTagElement("display");return display!=null&&display.contains("color")?display.getInt("color"):6961280;
    }
    public static int upgrade(ItemStack stack,int slot){return stack.hasTag()?stack.getTag().getByte(slot==0?"upgrade":"upgrade"+(slot+1)):0;}
    public int countUpgrade(ItemStack stack,int value){int count=0;for(int i=0;i<upgradeSlots();i++)if(upgrade(stack,i)==value)count++;return count;}
    public static boolean hardened(ItemStack stack){return stack.getItem() instanceof DyesArmorItem item&&item.countUpgrade(stack,5)>0;}
    public static int mask(ItemStack stack){return stack.hasTag()&&stack.getTag().contains("mask")?stack.getTag().getInt("mask"):-1;}
    public static boolean goggles(ItemStack stack){return stack.hasTag()&&stack.getTag().contains("goggles");}
    public boolean fortress(){return family.equals("IronFortressArmor")||family.equals("ThaumiumFortressDyed");}
    @Override public int intrinsicRunicCharge(ItemStack stack){return runic()?new int[]{8,24,16,8}[DyesMaterial.legacySlot(getType())]*(2+countUpgrade(stack,2))/2:0;}
    @Override public int visDiscount(ItemStack stack,Player player,Aspect aspect){return switch(family){
        case "CultistRobesDyed","ThaumiumBoots","ItemRunicArmorEnhanced"->2;
        case "CultistRanger"->getType()==Type.HELMET?0:1;
        case "CultistRangerDyed"->getType()==Type.HELMET?0:2;
        case "ThaumiumRobeDyed"->getType()==Type.HELMET?5:3;
        case "VoidRobeDyed","VoidBoots","ItemTXAncientMask","ItemRunicGoggles","ItemRunicArmorPrimal","ItemRunicArmorPrimalBoots"->5;
        default->0;};}
    @Override public int warp(ItemStack stack,Player player){return switch(family){
        case "CultistRobesDyed","CultistRanger","InhabitedKnightArmor","ItemTXAncientMask"->1;
        case "VoidRobeDyed","VoidBoots"->2;
        case "ItemRunicArmorPrimal","ItemRunicArmorPrimalBoots"->3;
        case "IronFortressArmor"->mask(stack)==2?1:0;
        default->0;};}
    @Override public boolean showNodes(ItemStack stack,LivingEntity wearer){return switch(family){
        case "ThaumiumRobeDyed","VoidRobeDyed"->getType()==Type.HELMET;
        case "ItemRunicGoggles","ItemRunicArmorPrimal","ItemRunicArmorPrimalBoots"->true;
        case "IronFortressArmor","ThaumiumFortressDyed"->goggles(stack);
        case "CultistRangerDyed"->mask(stack)==0;
        default->false;};}
    @Override public boolean showIngamePopups(ItemStack stack,LivingEntity wearer){return showNodes(stack,wearer);}
    @Override public boolean isValidRepairItem(ItemStack stack,ItemStack ingredient){
        if(runic())return false;
        if(family.equals("ItemTXAncientMask")||family.startsWith("Void"))return ingredient.is(TCItems.material(MaterialType.VOID));
        if(family.startsWith("Thaumium"))return ingredient.is(TCItems.material(MaterialType.THAUMIUM));
        return ingredient.is(Items.IRON_INGOT);
    }
    @Override public Rarity getRarity(ItemStack stack){if(primal()||family.startsWith("Void"))return Rarity.EPIC;return runic()||fortress()||family.startsWith("Thaumium")||Set.of("CultistPaladin","CultistPraetorDyed","ItemTXAncientMask").contains(family)?Rarity.RARE:Rarity.UNCOMMON;}
    @Override public boolean isBookEnchantable(ItemStack stack,ItemStack book){return !runic()||(primal()&&hardened(stack));}
    @Override public boolean isEnchantable(ItemStack stack){return (!runic()||primal()&&hardened(stack))&&!stack.isEnchanted();}
    @Override public int getMaxDamage(ItemStack stack){return runic()?0:super.getMaxDamage(stack);}
    @Override public void inventoryTick(ItemStack stack,Level level,Entity holder,int slot,boolean selected){
        if(level.isClientSide)return;
        if(primal()&&!hardened(stack)&&stack.hasTag())stack.getTag().remove("Enchantments");
        if(family.startsWith("Void")&&holder instanceof LivingEntity&&holder.tickCount%20==0&&stack.isDamaged())stack.setDamageValue(stack.getDamageValue()-1);
    }
    @Override public void onArmorTick(ItemStack stack,Level level,Player player){
        if(traveller())DyesMovement.tick(this,player);
        if(!level.isClientSide&&family.startsWith("Void")&&player.tickCount%20==0&&stack.isDamaged())stack.setDamageValue(stack.getDamageValue()-1);
    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context){
        var level=context.getLevel();var pos=context.getClickedPos();var state=level.getBlockState(pos);
        if(dyeable()&&state.is(Blocks.WATER_CAULDRON)){
            if(!level.isClientSide){((DyedArmorItem)this).clearColor(stack);LayeredCauldronBlock.lowerFillLevel(state,level,pos);level.updateNeighbourForOutputSignal(pos,state.getBlock());}
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
    @Override public InteractionResult useOn(UseOnContext context){
        var stack=context.getItemInHand();
        // Let chests and infusion pedestals handle the click before cycling upgrades.
        if(upgradeSlots()>1&&validRotation(stack)){if(!context.getLevel().isClientSide)rotateUpgrades(stack);return InteractionResult.sidedSuccess(context.getLevel().isClientSide);}
        return super.useOn(context);
    }
    private boolean validRotation(ItemStack stack){for(int i=0;i<upgradeSlots();i++)if(upgrade(stack,i)<0)return false;return true;}
    public void rotateUpgrades(ItemStack stack){if(upgradeSlots()<2||!validRotation(stack))return;int first=upgrade(stack,0);for(int i=0;i<upgradeSlots()-1;i++)stack.getOrCreateTag().putByte(i==0?"upgrade":"upgrade"+(i+1),(byte)upgrade(stack,i+1));stack.getOrCreateTag().putByte("upgrade"+upgradeSlots(),(byte)first);}
    @Override public Multimap<Attribute,AttributeModifier> getAttributeModifiers(EquipmentSlot slot,ItemStack stack){
        var result=HashMultimap.<Attribute,AttributeModifier>create(super.getAttributeModifiers(slot,stack));
        if(slot!=getEquipmentSlot()||!runic())return result;
        if(countUpgrade(stack,7)>0)add(result,Attributes.KNOCKBACK_RESISTANCE,.2);
        if(countUpgrade(stack,8)>0)add(result,Attributes.MAX_HEALTH,10);
        if(getType()==Type.BOOTS&&countUpgrade(stack,9)>0)add(result,Attributes.MOVEMENT_SPEED,.03);
        if(countUpgrade(stack,10)>0)add(result,Attributes.ATTACK_DAMAGE,2);
        if(!primal()&&countUpgrade(stack,5)>0)add(result,Attributes.ARMOR,upgradeSlots()==2?1:getDefense());
        return result;
    }
    private void add(Multimap<Attribute,AttributeModifier> result,Attribute attribute,double value){result.put(attribute,new AttributeModifier(UUID.nameUUIDFromBytes(("thaumicdyes:"+legacyKey+":"+attribute.getDescriptionId()).getBytes(StandardCharsets.UTF_8)),"Thaumic Dyes runic upgrade",value,AttributeModifier.Operation.ADDITION));}
    @Override public String getArmorTexture(ItemStack stack,Entity wearer,EquipmentSlot slot,String layer){return ExternalResources.armorTexture(family,stack,wearer,slot,layer);}
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){consumer.accept(dyesmodern.client.DyesArmorExtensions.create(this));}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flags){
        for(int i=0;i<upgradeSlots();i++){int upgrade=upgrade(stack,i);if(upgrade>0||upgradeSlots()>1)lines.add(Component.translatable("item.runic.upgrade."+upgrade).withStyle(upgrade>0?ChatFormatting.DARK_AQUA:ChatFormatting.DARK_GRAY));}
        if(goggles(stack))lines.add(Component.translatable("item.thaumcraft.goggles").withStyle(ChatFormatting.DARK_PURPLE));
        if(mask(stack)>=0&&fortress())lines.add(Component.translatable("item.HelmetFortress.mask."+mask(stack)).withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack,level,lines,flags);
    }
}
