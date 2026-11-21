package fmmodern;

import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import tc4revival.aspect.*;
import tc4revival.content.*;
import top.theillusivec4.curios.api.*;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class PainCollar extends Item implements ICurioItem,WandInventory.VisAmulet,thaumcraft.api.IRunicArmor {
    public PainCollar(){super(new Properties().stacksTo(1).rarity(Rarity.RARE));}
    @Override public int getRunicCharge(ItemStack stack){return 0;}
    @Override public boolean consumeVis(ItemStack stack,Player player,Map<Aspect,Integer> cost,boolean commit){
        if(player.level().isClientSide||fmmodern.ForbiddenConfig.value(ForbiddenConfig.NO_LUST))return false;
        var tag=stack.hasTag()?stack.getTag().copy():new net.minecraft.nbt.CompoundTag();boolean result=WandVis.consume(tag,cost,a->1,false,commit);if(result&&commit)stack.setTag(tag);return result;
    }
    public static void add(ItemStack stack,Aspect aspect,int wholeVis){WandVis.add(stack.getOrCreateTag(),aspect,wholeVis,25000,true,true);}
    @Override public void curioTick(SlotContext context,ItemStack stack){
        var wearer=context.entity();if(wearer.level().isClientSide||fmmodern.ForbiddenConfig.value(ForbiddenConfig.NO_LUST)||wearer.tickCount%5!=0)return;
        var held=wearer.getMainHandItem();
        if(held.getItem() instanceof CastingWandItem wand)for(var aspect:TCAspects.catalog().primals()){
            int amount=Math.min(5,Math.min(wand.capacity(held)-wand.vis(held,aspect),WandVis.stored(stack.getTag(),aspect)));
            if(amount>0){stack.getOrCreateTag().putInt(aspect.tag(),WandVis.stored(stack.getTag(),aspect)-amount);wand.addVis(held,aspect,amount,false,true);}
        }
        if(wearer instanceof Player player){var relay=VisRelayBlockEntity.nearby(player);if(relay!=null)for(var aspect:TCAspects.catalog().primals()){
            int wanted=Math.min(5,25000-WandVis.stored(stack.getTag(),aspect));if(wanted>0){int received=relay.consumeVis(aspect,wanted);if(received>0)WandVis.add(stack.getOrCreateTag(),aspect,received,25000,false,true);}
        }}
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player owner,LivingEntity entity,InteractionHand hand){
        if(!(entity instanceof Player target))return InteractionResult.PASS;
        if(owner.level().isClientSide)return InteractionResult.SUCCESS;
        var inventory=CuriosApi.getCuriosInventory(target).resolve();if(inventory.isEmpty())return InteractionResult.PASS;
        var necklaces=inventory.get().getCurios().get("necklace");if(necklaces==null)return InteractionResult.PASS;
        var slots=necklaces.getStacks();
        for(int i=0;i<slots.getSlots();i++)if(slots.getStackInSlot(i).isEmpty()&&slots.isItemValid(i,stack)){
            var copy=stack.copy();copy.setCount(1);copy.getOrCreateTag().putString("owner",owner.getGameProfile().getName());copy.getOrCreateTag().putUUID("ownerUUID",owner.getUUID());
            slots.setStackInSlot(i,copy);stack.shrink(1);
            target.displayClientMessage(Component.translatable("message.collar.placescollar",owner.getDisplayName()),false);owner.displayClientMessage(Component.translatable("message.collar.youplacecollar",target.getDisplayName()),false);return InteractionResult.SUCCESS;
        }
        owner.displayClientMessage(Component.translatable("message.collar.alreadywearing",target.getDisplayName()),false);return InteractionResult.FAIL;
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flags){
        lines.add(Component.translatable("item.capacity.text").append(" 250"));
        for(var aspect:TCAspects.catalog().primals()){int amount=WandVis.stored(stack.getTag(),aspect);if(amount>0)lines.add(Component.literal(thaumcraft.api.aspects.AspectViews.display(aspect).displayName()+" x "+java.math.BigDecimal.valueOf(amount,2).stripTrailingZeros().toPlainString()).withStyle(s->s.withColor(thaumcraft.api.aspects.AspectViews.display(aspect).color())));}
        if(stack.hasTag()&&stack.getTag().contains("owner"))lines.add(Component.translatable("tooltip.collar.owner").append(" "+stack.getTag().getString("owner")));
    }
}
