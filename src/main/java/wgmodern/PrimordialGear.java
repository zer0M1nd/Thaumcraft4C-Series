package wgmodern;

import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.tags.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.network.chat.Component;
import tc4revival.content.*;

public class PrimordialGear {
 public static final List<String> MODES=List.of("aer","terra","ignis","aqua","ordo","perditio");
 public static int mode(ItemStack s){return s.hasTag()?Math.floorMod(s.getTag().getInt("currentMode"),6):0;}
 public static void mode(ItemStack s,int mode){if(s.getItem() instanceof Weapon||s.getItem() instanceof Armor)s.getOrCreateTag().putInt("currentMode",Math.floorMod(mode,6));}
 public static int color(ItemStack s){return tc4revival.aspect.TCAspects.catalog().require(MODES.get(mode(s))).color();}
 public static Component name(ItemStack s,Component base){var a=thaumcraft.api.aspects.Aspect.getAspect(MODES.get(mode(s)));return base.copy().append(Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY)).append(Component.literal(a.getName()).withStyle(style->style.withColor(color(s))));}
 public static void repair(ItemStack s,Entity e,int every){if(!e.level().isClientSide&&e.tickCount%every==0&&s.isDamaged())s.setDamageValue(Math.max(0,s.getDamageValue()-1));}
 public static class ToolMaterial implements Tier {public int getUses(){return 1500;}public float getSpeed(){return 8;}public float getAttackDamageBonus(){return 6;}public int getLevel(){return 4;}public int getEnchantmentValue(){return 25;}public Ingredient getRepairIngredient(){return Ingredient.of(TagKey.create(net.minecraft.core.registries.Registries.ITEM,new ResourceLocation("forge","ingots/void")));}}
 public static class Weapon extends DiggerItem implements thaumcraft.api.IRepairable {
  public final String kind;private static final ThreadLocal<Boolean> BREAKING=ThreadLocal.withInitial(()->false);
  public Weapon(String kind){super(kind.equals("sword")?4:kind.equals("axe")?3:2,kind.equals("sword")?-2.4f:kind.equals("axe")?-3f:-2.8f,new ToolMaterial(),kind.equals("axe")?BlockTags.MINEABLE_WITH_AXE:BlockTags.MINEABLE_WITH_PICKAXE,new Properties());this.kind=kind;}
  @Override public String getDescriptionId(){return "item.WG_Primordial"+(kind.equals("sword")?"Sword":kind.equals("hammer")?"Hammer":"Axe");}
  @Override public Component getName(ItemStack s){return name(s,super.getName(s));}
  @Override public boolean isValidRepairItem(ItemStack s,ItemStack ingredient){return ingredient.is(TCItems.material(MaterialType.VOID));}
  @Override public void inventoryTick(ItemStack s,Level l,Entity e,int slot,boolean equipped){repair(s,e,40);}
  @Override public UseAnim getUseAnimation(ItemStack s){return UseAnim.BLOCK;}@Override public int getUseDuration(ItemStack s){return 72000;}
  @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand hand){p.startUsingItem(hand);return InteractionResultHolder.consume(p.getItemInHand(hand));}
  @Override public boolean canPerformAction(ItemStack s,net.minecraftforge.common.ToolAction action){return kind.equals("sword")?action==net.minecraftforge.common.ToolActions.SWORD_DIG:kind.equals("axe")?action==net.minecraftforge.common.ToolActions.AXE_DIG:net.minecraftforge.common.ToolActions.DEFAULT_PICKAXE_ACTIONS.contains(action);}
  @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> c){c.accept(new tc4revival.client.LegacySwordPose());}
  @Override public boolean canApplyAtEnchantingTable(ItemStack s,net.minecraft.world.item.enchantment.Enchantment e){Item exemplar=kind.equals("sword")?Items.DIAMOND_SWORD:kind.equals("axe")?Items.DIAMOND_AXE:Items.DIAMOND_PICKAXE;return e.category.canEnchant(exemplar);}
  @Override public boolean hurtEnemy(ItemStack s,LivingEntity victim,LivingEntity attacker){s.hurtAndBreak(kind.equals("sword")?1:2,attacker,e->e.broadcastBreakEvent(EquipmentSlot.MAINHAND));return true;}
  @Override public float getDestroySpeed(ItemStack s,BlockState state){if(kind.equals("sword"))return state.is(Blocks.COBWEB)?15:state.is(BlockTags.SWORD_EFFICIENT)?1.5f:1;return kind.equals("axe")&&(state.is(BlockTags.LEAVES)||state.getBlock() instanceof VineBlock)?6:super.getDestroySpeed(s,state);}
  public boolean areaMaterial(BlockState s){return kind.equals("axe")?s.is(BlockTags.MINEABLE_WITH_AXE)||s.is(BlockTags.LEAVES)||s.getBlock() instanceof VineBlock||s.getBlock() instanceof BushBlock||s.is(Blocks.CACTUS):s.is(BlockTags.MINEABLE_WITH_PICKAXE)||s.is(BlockTags.MINEABLE_WITH_SHOVEL)||s.getBlock() instanceof AbstractGlassBlock||s.is(BlockTags.ICE)||s.is(Blocks.SNOW);}
  @Override public boolean mineBlock(ItemStack s,Level l,BlockState state,BlockPos pos,LivingEntity user){return BREAKING.get()||super.mineBlock(s,l,state,pos,user);}
  @Override public boolean onBlockStartBreak(ItemStack stack,BlockPos origin,Player player){
   if(kind.equals("sword")||BREAKING.get()||!(player instanceof ServerPlayer p)||kind.equals("hammer")&&p.isShiftKeyDown())return false;
   var hit=p.level().clip(new ClipContext(p.getEyePosition(),p.getEyePosition().add(p.getLookAngle().scale(p.getBlockReach())),ClipContext.Block.OUTLINE,ClipContext.Fluid.ANY,p));
   if(hit.getType()==HitResult.Type.BLOCK)breakPlane(stack,origin,p,hit.getDirection().getAxis());
   return false;
  }
  /** Original tools directly harvest the plane without consuming durability; modern destruction keeps protection hooks. */
  public void breakPlane(ItemStack stack,BlockPos origin,ServerPlayer p,Direction.Axis axis){
   if(kind.equals("sword")||BREAKING.get()||kind.equals("hammer")&&p.isShiftKeyDown())return;
   BREAKING.set(true);
   try{for(int a=-1;a<=1;a++)for(int b=-1;b<=1;b++){
    var pos=origin.offset(axis==Direction.Axis.X?0:a,axis==Direction.Axis.Y?0:axis==Direction.Axis.X?a:b,axis==Direction.Axis.Z?0:b);
    if(!p.level().hasChunkAt(pos))continue;
    var state=p.level().getBlockState(pos);
    if(state.isAir()||state.getDestroySpeed(p.level(),pos)<0||!areaMaterial(state)||!state.canHarvestBlock(p.level(),pos,p))continue;
    p.gameMode.destroyBlock(pos);
   }}finally{BREAKING.set(false);}
  }
 }
 public static class Material implements ArmorMaterial {public int getDurabilityForType(ArmorItem.Type t){return (t==ArmorItem.Type.HELMET?11:t==ArmorItem.Type.CHESTPLATE?16:t==ArmorItem.Type.LEGGINGS?15:13)*40;}public int getDefenseForType(ArmorItem.Type t){return t==ArmorItem.Type.HELMET||t==ArmorItem.Type.BOOTS?3:t==ArmorItem.Type.CHESTPLATE?7:6;}public int getEnchantmentValue(){return 30;}public SoundEvent getEquipSound(){return SoundEvents.ARMOR_EQUIP_IRON;}public Ingredient getRepairIngredient(){return Ingredient.of(TCItems.material(MaterialType.VOID));}public String getName(){return "witchinggadgets:primordial";}public float getToughness(){return 0;}public float getKnockbackResistance(){return 0;}}
 public static class Armor extends ArmorItem implements RepairableItem,RunicGear,RevealingGear,WarpEvents.MaskProtection {
  public Armor(Type type){super(new Material(),type,new Properties().rarity(Rarity.RARE));}
  public int legacySlot(){return switch(getType()){case HELMET->0;case CHESTPLATE->1;case LEGGINGS->2;case BOOTS->3;};}
  @Override public String getDescriptionId(){return "item.WG_Primordial"+switch(getType()){case HELMET->"Helm";case CHESTPLATE->"Chest";case LEGGINGS->"Legs";case BOOTS->"Boots";};}
  @Override public Component getName(ItemStack s){return name(s,super.getName(s));}
  @Override public boolean protectsFromWarp(ItemStack s){return mask(s)==0;}
  public static int mask(ItemStack s){return s.hasTag()&&s.getTag().contains("mask")?s.getTag().getInt("mask"):-1;}
  @Override public boolean showNodes(ItemStack s,LivingEntity wearer){return getType()==Type.HELMET&&s.hasTag()&&s.getTag().contains("goggles");}
  @Override public boolean showIngamePopups(ItemStack s,LivingEntity wearer){return showNodes(s,wearer);}
  @Override public boolean isValidRepairItem(ItemStack s,ItemStack ingredient){return ingredient.is(TCItems.material(MaterialType.VOID));}
  @Override public void inventoryTick(ItemStack s,Level l,Entity e,int slot,boolean selected){repair(s,e,40);}
  @Override public void onArmorTick(ItemStack s,Level l,Player p){repair(s,p,20);PrimordialEvents.armorTick(p,s);}
  @Override public String getArmorTexture(ItemStack s,Entity e,EquipmentSlot slot,String layer){return "witchinggadgets:textures/models/primordialarmor.png";}
  @Override public void appendHoverText(ItemStack s,Level l,List<Component> lines,TooltipFlag f){if(showNodes(s,null))lines.add(Component.translatable("item.thaumcraft.goggles").withStyle(ChatFormatting.DARK_PURPLE));if(mask(s)>=0)lines.add(Component.translatable("item.HelmetFortress.mask."+mask(s)).withStyle(ChatFormatting.GOLD));}
  @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> c){c.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){@Override public net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(LivingEntity e,ItemStack s,EquipmentSlot slot,net.minecraft.client.model.HumanoidModel<?> original){return PrimordialArmorModel.get(s,original);}});}
 }
}
