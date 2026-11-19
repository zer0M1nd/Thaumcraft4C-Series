package thaumicfutures.foci;

import java.util.List;
import java.util.Map;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import tc4revival.aspect.*;
import tc4revival.content.*;

public class GrappleFocusItem extends thaumcraft.api.wands.ItemFocusBasic {
    public static FocusUpgrade sticky() { return FocusUpgrade.require("thaumicfutures:sticky"); }
    @Override public int color(ItemStack focus) { return 0x1515ff; }
    @Override public String sortingPrefix() { return "FGR"; }
    @Override public int cooldownMillis(ItemStack focus) { return 250; }
    @Override public Map<Aspect,Integer> costs(ItemStack focus) { var a=TCAspects.catalog();return Map.of(a.require("aer"),1000,a.require("aqua"),1000); }
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus,int rank) {
        return rank==3 ? List.of(FocusUpgrade.FRUGAL,FocusUpgrade.POTENCY,sticky())
                : rank>=1 && rank<=5 ? List.of(FocusUpgrade.FRUGAL,FocusUpgrade.POTENCY) : List.of();
    }
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack,Level level,Player player,InteractionHand hand,BlockHitResult hit) {
        var focus=attached(stack);var wand=(CastingWandItem)stack.getItem();
        if (!level.isClientSide && wand.consumeForPlayer(stack,player,costs(focus),false,false,true)) {
            var grapple=new GrappleEntity(level,player,hand,wand.focusPotency(stack,player),upgradeLevel(focus,sticky().id)>0);
            grapple.replacePrevious(); level.addFreshEntity(grapple);
            level.playSound(null,grapple.blockPosition(),FutureFoci.ICE.get(),net.minecraft.sounds.SoundSource.PLAYERS,.3f,.8f+level.random.nextFloat()*.1f);
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
}
