package wgmodern;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkHooks;
import tc4revival.aspect.*;
import tc4revival.content.*;

public class PrimordialGlove extends Item {
 public PrimordialGlove(){super(new Properties().stacksTo(1));}
 @Override public String getDescriptionId(){return "item.WG_PrimordialGlove";}
 public static List<ItemStack> gems(ItemStack glove){var out=new ArrayList<ItemStack>(Collections.nCopies(5,ItemStack.EMPTY));if(!glove.hasTag())return out;var rows=glove.getTag().getList("gems",Tag.TAG_COMPOUND);for(int i=0;i<rows.size();i++){var row=rows.getCompound(i);int slot=row.getByte("Slot")&255;if(slot<5){var s=ItemStack.of(row);if(s.getItem() instanceof InfusedGem){s.setCount(1);out.set(slot,s);}}}return out;}
 public static void gems(ItemStack glove,List<ItemStack> gems){if(!(glove.getItem() instanceof PrimordialGlove))return;var rows=new ListTag();for(int i=0;i<Math.min(5,gems.size());i++)if(!gems.get(i).isEmpty()){var row=gems.get(i).copyWithCount(1).save(new CompoundTag());row.putByte("Slot",(byte)i);rows.add(row);}glove.getOrCreateTag().put("gems",rows);}
 public static int selected(ItemStack glove){return glove.hasTag()?Math.floorMod(glove.getTag().getInt("selected"),5):0;}
 public static void select(ItemStack glove,int slot){if(glove.getItem() instanceof PrimordialGlove&&slot>=0&&slot<5)glove.getOrCreateTag().putInt("selected",slot);}
 public static AspectAmounts stored(ItemStack glove){return glove.hasTag()?AspectNbt.read(glove.getTag().getCompound("storedNode"),TCAspects.catalog()).amounts():new AspectAmounts();}
 public static AspectAmounts reduce(AspectAmounts source){var out=new AspectAmounts();for(var a:source.aspects())reduce(a,source.amount(a),out,new HashSet<>());return out;}
 private static void reduce(tc4revival.aspect.Aspect a,int n,AspectAmounts out,Set<String> path){if(n<=0||!path.add(a.tag()))return;if(a.isPrimal())out.merge(a,n);else {var children=new LinkedHashMap<String,Integer>();for(var child:a.components())children.merge(child,n,Integer::sum);children.forEach((key,amount)->TCAspects.catalog().find(key).ifPresent(c->reduce(c,amount,out,path)));}path.remove(a.tag());}
 public static AspectAmounts charge(AuraNodeBlockEntity node){var out=new AspectAmounts();var reduced=reduce(node.portableCharge());for(var a:reduced.aspects()){int n=PrimordialRules.nodeCharge(reduced.amount(a),node.nodeModifier()==null?-1:node.nodeModifier().ordinal());if(node.nodeType()==AuraNodeType.UNSTABLE)n+=node.getLevel().random.nextInt(5)-2;if(n>=1)out.add(a,n);}return out;}
 public static void contain(ItemStack glove,AuraNodeBlockEntity node){var tag=new CompoundTag();var charge=charge(node);AspectNbt.write(tag,charge);tag.putInt("type",node.nodeType().ordinal());tag.putInt("modifier",node.nodeModifier()==null?-1:node.nodeModifier().ordinal());glove.getOrCreateTag().put("storedNode",tag);glove.getOrCreateTag().putInt("visCapacity",(int)Math.min(Integer.MAX_VALUE,charge.total()));}
 @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context){var l=context.getLevel();var p=context.getPlayer();var pos=context.getClickedPos();if(p==null||!l.mayInteract(p,pos)||!p.mayUseItemAt(pos,context.getClickedFace(),stack))return InteractionResult.PASS;if(l.getBlockEntity(pos) instanceof AuraNodeBlockEntity node&&node.getClass()==AuraNodeBlockEntity.class){if(!l.isClientSide){contain(stack,node);l.removeBlock(pos,false);}return InteractionResult.sidedSuccess(l.isClientSide);}return InteractionResult.PASS;}
 public static void open(ServerPlayer p,boolean offhand){if(!(p.getItemInHand(offhand?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND).getItem() instanceof PrimordialGlove)||p.containerMenu!=p.inventoryMenu)return;NetworkHooks.openScreen(p,new SimpleMenuProvider((id,inv,x)->new PrimordialMenu(id,inv,offhand),Component.translatable("item.WG_PrimordialGlove")),b->b.writeBoolean(offhand));}
 @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand hand){var s=p.getItemInHand(hand);if(l.isClientSide)return InteractionResultHolder.success(s);var gems=gems(s);var gem=gems.get(selected(s));boolean performed=false;if(!gem.isEmpty()){int cut=InfusedGem.cut(gem),cost=cut==0?32:GemRules.charge(InfusedGem.aspect(gem),tc4revival.config.TCConfig.SERVER.allowMirrors.get());if(gem.getDamageValue()+cost<=gem.getMaxDamage()){int potency=EnchantmentHelper.getItemEnchantmentLevel(WGMagic.ENCHANTS.get("gemstone_potency").get(),gem);performed=InfusedGem.effect(p,InfusedGem.aspect(gem),cut,potency);if(performed&&!p.isCreative()){gem.setDamageValue(gem.getDamageValue()+cost);gems(s,gems);}}}if(p.isShiftKeyDown()&&!performed)open((ServerPlayer)p,hand==InteractionHand.OFF_HAND);return InteractionResultHolder.consume(s);}
 @Override public void inventoryTick(ItemStack s,Level l,Entity e,int slot,boolean selected){if(l.isClientSide||e.tickCount%10!=0||!s.hasTag()||!s.getTag().contains("storedNode"))return;var charge=stored(s);var gems=gems(s);if(e instanceof Player p&&p.containerMenu instanceof PrimordialMenu m&&m.glove==s){m.recharge(charge);return;}for(var gem:gems)if(gem.isDamaged())gem.setDamageValue(Math.max(0,gem.getDamageValue()-TCAspects.catalog().find(InfusedGem.aspect(gem)).map(charge::amount).orElse(0)));gems(s,gems);}
 @Override public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute,net.minecraft.world.entity.ai.attributes.AttributeModifier> getAttributeModifiers(EquipmentSlot slot,ItemStack s){var map=com.google.common.collect.HashMultimap.<net.minecraft.world.entity.ai.attributes.Attribute,net.minecraft.world.entity.ai.attributes.AttributeModifier>create();if(slot==EquipmentSlot.MAINHAND)map.put(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,new net.minecraft.world.entity.ai.attributes.AttributeModifier(BASE_ATTACK_DAMAGE_UUID,"Weapon modifier",8,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));return map;}
 @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> c){c.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){private PrimordialRenderer renderer;public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new PrimordialRenderer();return renderer;}});}
}
