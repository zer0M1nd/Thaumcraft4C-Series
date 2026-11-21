package thaumicfutures.foci;

import java.util.List;
import java.util.Map;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import tc4revival.aspect.*;
import tc4revival.content.*;

public class ShardFocusItem extends thaumcraft.api.wands.ItemFocusBasic {
    public static FocusUpgrade persistent() { return FocusUpgrade.require("thaumicfutures:persistent"); }
    @Override public int color(ItemStack focus) { return 10037693; }
    @Override public String sortingPrefix() { return "FSH"; }
    @Override public int cooldownMillis(ItemStack focus) { return 300; }
    @Override public Map<Aspect,Integer> costs(ItemStack focus) {
        var a = TCAspects.catalog();
        return upgradeLevel(focus, persistent().id) > 0
                ? Map.of(a.require("ignis"),5,a.require("perditio"),5,a.require("aer"),5,a.require("aqua"),5)
                : Map.of(a.require("ignis"),5,a.require("perditio"),5,a.require("aer"),5);
    }
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus,int rank) {
        return rank == 5 ? List.of(FocusUpgrade.FRUGAL,FocusUpgrade.POTENCY,persistent())
                : rank >= 1 && rank <= 4 ? List.of(FocusUpgrade.FRUGAL,FocusUpgrade.POTENCY) : List.of();
    }
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack,Level level,Player player,InteractionHand hand,BlockHitResult hit) {
        var target = FocusTargeting.pointed(player,0,32,1.1,e -> e instanceof LivingEntity living && living.isAlive() && !living.isSpectator());
        if (!(target instanceof LivingEntity living)) return InteractionResultHolder.pass(stack);
        var focus = attached(stack); var wand = (CastingWandItem)stack.getItem();
        if (!level.isClientSide && wand.consumeForPlayer(stack,player,costs(focus),false,false,true)) {
            var shard = new HomingShardEntity(level,player,living,wand.focusPotency(stack,player),upgradeLevel(focus,persistent().id)>0);
            level.addFreshEntity(shard);
            level.playSound(null,shard.blockPosition(),FutureFoci.SCAN.get(),net.minecraft.sounds.SoundSource.PLAYERS,.3f,1.1f+level.random.nextFloat()*.1f);
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
}
