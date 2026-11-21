package emtmodern.focus;

import ic2.api.item.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import tc4revival.aspect.*;
import tc4revival.content.*;
import java.util.*;

/** Original vis-to-EU pulse, honoring IC2's per-item transfer limits. */
public class ChargeFocus extends thaumcraft.api.wands.ItemFocusBasic {
    @Override public int color(ItemStack focus){return 0xffff00;}
    @Override public String sortingPrefix(){return "WANDCHARGING";}
    @Override public boolean isFoil(ItemStack stack){return true;}
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack stack,int rank){return List.of(FocusUpgrade.POTENCY,FocusUpgrade.FRUGAL);}
    @Override public Map<Aspect,Integer> costs(ItemStack stack){var values=new LinkedHashMap<Aspect,Integer>();thaumcraft.api.aspects.AspectSelection.select(TCAspects.catalog(),thaumcraft.api.aspects.AspectSelection.FOCUS_COST).forEach(aspect->values.put(aspect,10));return values;}
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack,Level world,Player player,InteractionHand hand,BlockHitResult hit){
        if(world.isClientSide)return InteractionResultHolder.success(stack);
        if(stack.getItem() instanceof CastingWandItem wand&&(player.getAbilities().instabuild||wand.consumeForPlayer(stack,player,costs(stack),false,true,true))){
            // Modern potency uses the installed focus and wielder attribute.
            int left=(int)(emtmodern.energy.EmtFocusConfig.CHARGE_OUTPUT.get()*Math.pow(1.1,wand.focusPotency(stack,player)));
            for(var armor:player.getInventory().armor){if(left<=0)break;if(armor.getItem() instanceof IElectricItem)left=(int)(left-ElectricItem.manager.charge(armor,left,4,false,false));}
            for(var item:player.getInventory().items){if(left<=0)break;if(item.getItem() instanceof IElectricItem)left=(int)(left-ElectricItem.manager.charge(item,left,4,false,false));}
        }
        return InteractionResultHolder.success(stack);
    }
}
