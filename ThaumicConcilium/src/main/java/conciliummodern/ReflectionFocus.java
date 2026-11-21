package conciliummodern;

import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import tc4revival.aspect.*;
import tc4revival.content.*;
import tc4revival.research.KnowledgeSavedData;
import tc4revival.research.PlayerKnowledge.WarpKind;

/** Original experience/compound-pool sacrifice, committed only by the server. */
public final class ReflectionFocus extends WandFocusItem implements thaumcraft.api.IWarpingGear {
    private static FocusUpgrade oblivion() { return FocusUpgrade.require("thaumicconcilium:oblivion"); }
    private boolean forgetting(ItemStack focus) { return upgradeLevel(focus,oblivion().id)>0; }
    @Override public int getWarp(ItemStack stack,Player player) { return 4; }
    @Override public String sortingPrefix() { return "CONCILIUM_REFLECTION"; }
    @Override public String depthTexture() { return "thaumicconcilium:item/reflection_foci_depth"; }
    @Override public int color(ItemStack focus) { return 0; }
    @Override public Map<Aspect,Integer> costs(ItemStack focus) { return Map.of(); }
    @Override public boolean costPerTick(ItemStack focus) { return true; }
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus,int rank) {
        if(rank<1 || rank>5)return List.of();
        return rank==5?List.of(FocusUpgrade.FRUGAL,FocusUpgrade.POTENCY,oblivion()):List.of(FocusUpgrade.FRUGAL,FocusUpgrade.POTENCY);
    }
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack wand,Level level,Player player,InteractionHand hand,BlockHitResult hit) {
        player.startUsingItem(hand);return InteractionResultHolder.consume(wand);
    }
    // totalExperience is a lifetime counter and can disagree with spendable experience.
    private static long experience(Player player) {
        long level=Math.max(0,player.experienceLevel);
        long base=level<=16?level*level+6*level:level<=31?(5*level*level-81*level+720)/2:(9*level*level-325*level+4440)/2;
        return base+Math.round(player.experienceProgress*player.getXpNeededForNextLevel());
    }
    @Override public void usingTick(ItemStack stack,Player actor,int remaining) {
        if(!(actor instanceof ServerPlayer player) || !player.isAlive() || !(stack.getItem() instanceof CastingWandItem wand))return;
        // Knowledge borrowing is read-only; automated actors must not sacrifice a soul owner's pool.
        if(player instanceof net.minecraftforge.common.util.FakePlayer)return;
        if(remaining%20==0)player.level().playSound(null,player.blockPosition(),TCSounds.WHISPERS.get(),SoundSource.PLAYERS,.5f,.4f/(player.getRandom().nextFloat()*.4f+.8f));
        int potency=Math.max(0,(int)wand.focusPotency(stack,player));
        var data=KnowledgeSavedData.get(player.server);
        if(forgetting(attached(stack))) {
            if(remaining%5!=0)return;
            var compounds=TCAspects.catalog().compounds();
            if(compounds.isEmpty())return;
            var aspect=compounds.get(player.getRandom().nextInt(compounds.size()));
            var known=data.snapshot(player.getUUID()).orElse(null);
            if(known==null || known.poolAmount(aspect)<=0)return;
            data.update(player.getUUID(),player.getRandom()::nextInt,next->{
                next.changePool(aspect,-1);next.changeWarp(WarpKind.TEMPORARY,10+2*potency);return null;
            });
            player.sendSystemMessage(Component.translatable("TC.oblivion_response").append(" ")
                    .append(Component.translatable("tc.aspect.help."+aspect.tag())).withStyle(ChatFormatting.DARK_PURPLE));
        } else {
            int cost=Math.max(1,10-wand.focusUpgrade(stack,FocusUpgrade.FRUGAL));
            // Preserve the original distinct activation threshold and actual deduction.
            if(experience(player)<(long)cost+potency)return;
            data.update(player.getUUID(),player.getRandom()::nextInt,next->{next.changeWarp(WarpKind.TEMPORARY,1+potency);return null;});
            player.giveExperiencePoints(-cost);
        }
        // KnowledgeSync sends the changed pool and warp revision at the end of this tick.
    }
    @Override public void appendHoverText(ItemStack focus,Level level,List<Component> lines,TooltipFlag flags) {
        super.appendHoverText(focus,level,lines,flags);
        lines.add(Component.translatable("item.Focus.cost2"));
        if(forgetting(focus))lines.add(Component.translatable("TC.oblivion_cost").append(" x 1").withStyle(ChatFormatting.LIGHT_PURPLE));
        else {
            int cost=Math.max(1,10-upgradeLevel(focus,FocusUpgrade.FRUGAL.id));
            lines.add(Component.translatable("ttmisc.experience").append(" x "+cost).withStyle(ChatFormatting.GREEN));
            lines.add(Component.translatable("tooltip.thaumicconcilium.reflection_threshold",cost+upgradeLevel(focus,FocusUpgrade.POTENCY.id)).withStyle(ChatFormatting.GRAY));
        }
    }
}
