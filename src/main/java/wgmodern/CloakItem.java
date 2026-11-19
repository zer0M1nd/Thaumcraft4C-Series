package wgmodern;

import java.util.*;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import top.theillusivec4.curios.api.*;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import tc4revival.content.CastingWandItem;

/** Traveller's Gear cloak and Baubles kama semantics, using independent Curios slots. */
public class CloakItem extends Item implements ICurioItem,DyeableLeatherItem {
    public final String kind;public final boolean kama;
    public CloakItem(String kind,boolean kama){super(new Properties().stacksTo(1).rarity(Rarity.RARE));this.kind=kind;this.kama=kama;}
    @Override public String getDescriptionId(){return (kama?"item.WG_Kama.":"item.WG_Cloak.")+kind;}
    @Override public int getColor(ItemStack stack){if(kind.equals("standard")||kind.equals("plain"))return hasCustomColor(stack)?DyeableLeatherItem.super.getColor(stack):2041173;if(kind.equals("spectral"))return thaumcraft.api.aspects.Aspect.DARKNESS.getColor();if(kind.equals("storage"))return thaumcraft.api.aspects.Aspect.VOID.getColor();return 0xffffff;}
    @Override public boolean canEquip(SlotContext c,ItemStack stack){return c.identifier().equals(kama?"belt":"cloak");}
    @Override public boolean canFitInsideContainerItems(){return !kind.equals("storage");}
    @Override public boolean canSync(SlotContext c,ItemStack stack){return true;}
    @Override public CompoundTag writeSyncData(SlotContext c,ItemStack stack){return stack.hasTag()?stack.getTag().copy():new CompoundTag();}
    @Override public void readSyncData(SlotContext c,CompoundTag tag,ItemStack stack){stack.setTag(tag.copy());}
    @Override public void onUnequip(SlotContext c,ItemStack next,ItemStack stack){if(stack.hasTag())stack.getTag().remove("isSpectral");}
    @Override public void onEquip(SlotContext c,ItemStack previous,ItemStack stack) {
        if(!kind.equals("spectral")||c.entity().level().isClientSide)return;
        var tag=stack.getOrCreateTag();
        boolean sameCloak=tag.hasUUID("wgCloakId")&&previous.is(this)&&previous.hasTag()
                &&previous.getTag().hasUUID("wgCloakId")&&tag.getUUID("wgCloakId").equals(previous.getTag().getUUID("wgCloakId"));
        // Curios also calls onEquip for NBT changes while worn. Preserve those,
        // but a newly equipped cloak always requires its activation payment.
        if(!sameCloak)tag.remove("isSpectral");
        if(!tag.hasUUID("wgCloakId"))tag.putUUID("wgCloakId",UUID.randomUUID());
    }
    @Override public void inventoryTick(ItemStack stack,Level level,Entity holder,int slot,boolean selected) {
        // Curios ticks equipped stacks through inventoryTick with slot -1.
        // Only an actual ordinary inventory slot means the cloak was taken off.
        if(slot>=0&&!level.isClientSide&&kind.equals("spectral")&&stack.hasTag())stack.getTag().remove("isSpectral");
    }
    public static List<SlotResult> equipped(LivingEntity e){return CuriosApi.getCuriosInventory(e).map(inv->inv.findCurios(s->s.getItem() instanceof CloakItem)).orElse(List.of());}
    public static boolean spectral(LivingEntity e){return equipped(e).stream().anyMatch(s->s.stack().hasTag()&&s.stack().getTag().getBoolean("isSpectral"));}
    public static boolean payAir(Player player){var air=tc4revival.aspect.TCAspects.catalog().require("aer");for(var stack:player.getInventory().items)if(stack.getItem() instanceof CastingWandItem wand&&wand.consume(stack,Map.of(air,1),a->1,true,true,player.level()))return true;return false;}
    public static NonNullList<ItemStack> contents(ItemStack cloak){var result=NonNullList.withSize(27,ItemStack.EMPTY);if(cloak.hasTag()){var root=new CompoundTag();root.put("Items",cloak.getTag().getList("InternalInventory",Tag.TAG_COMPOUND));ContainerHelper.loadAllItems(root,result);}return result;}
    public static void store(ItemStack cloak,NonNullList<ItemStack> items){var root=new CompoundTag();ContainerHelper.saveAllItems(root,items);cloak.getOrCreateTag().put("InternalInventory",root.getList("Items",Tag.TAG_COMPOUND));}
    public void activate(ServerPlayer p,SlotResult slot){var stack=slot.stack();switch(kind){case "storage"->{String name=slot.slotContext().identifier();int index=slot.slotContext().index();NetworkHooks.openScreen(p,new SimpleMenuProvider((id,inv,user)->new CloakMenu(id,inv,name,index),stack.getHoverName()),buf->{buf.writeUtf(name);buf.writeVarInt(index);});}case "raven"->stack.getOrCreateTag().putBoolean("noGlide",!stack.getOrCreateTag().getBoolean("noGlide"));case "spectral"->{if(payAir(p)){boolean active=!stack.getOrCreateTag().getBoolean("isSpectral");stack.getTag().putBoolean("isSpectral",active);if(active)for(var mob:p.level().getEntitiesOfClass(Mob.class,p.getBoundingBox().inflate(16)))if(mob.getTarget()==p&&!WGAccessories.ignoresSpectral(mob))mob.setTarget(null);}}default->{}}}
    @Override public void curioTick(SlotContext c,ItemStack stack){if(!(c.entity() instanceof Player p))return;var tag=stack.getTag();if(kind.equals("spectral")&&!p.level().isClientSide&&p.tickCount%100==0&&tag!=null&&tag.getBoolean("isSpectral")&&!payAir(p))tag.putBoolean("isSpectral",false);
        if(kind.equals("wolf")&&!p.level().isClientSide&&tag!=null&&tag.contains("wolfPotion")){int amp=tag.getInt("wolfPotion");for(var effect:List.of(MobEffects.DAMAGE_BOOST,MobEffects.MOVEMENT_SPEED,MobEffects.DAMAGE_RESISTANCE))p.addEffect(new MobEffectInstance(effect,60,amp));tag.remove("wolfPotion");}
        if(kind.equals("raven")&&!p.onGround()){var speed=p.getDeltaMovement();if(p.getAbilities().flying){if(p.zza>0)p.moveRelative(.05f,new net.minecraft.world.phys.Vec3(0,0,1));p.setDeltaMovement(p.getDeltaMovement().multiply(1,1.125,1));}else if(speed.y<0&&(tag==null||!tag.getBoolean("noGlide"))){double factor=p.isShiftKeyDown()?.1:.05,angle=Math.toRadians(p.getYHeadRot()+90);p.setDeltaMovement(speed.x+Math.cos(angle)*factor,speed.y*(p.isShiftKeyDown()?.75:.5),speed.z+Math.sin(angle)*factor);}p.fallDistance=0;}}
    @Override public void appendHoverText(ItemStack s,Level l,List<Component> lines,TooltipFlag f){if(s.hasTag()&&s.getTag().getBoolean("noGlide"))lines.add(Component.translatable("wg.desc.noGlide"));lines.add(Component.translatable(kama?"wg.desc.gearSlot.bauble.BELT":"wg.desc.gearSlot.tg.0"));}
}

