package ttmodern;

import java.util.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import tc4revival.content.*;
import tc4revival.aspect.*;

public final class XpDrainFocus extends KamiFocus {
    public XpDrainFocus(){super("focus_xp_drain","TTKXP",0xffffff);}
    @Override public int color(ItemStack focus){return net.minecraft.util.Mth.hsvToRgb((System.currentTimeMillis()%9000)/9000f,1,1);}
    @Override public Map<Aspect,Integer> costs(ItemStack focus){return Map.of();}
    @Override public boolean costPerTick(ItemStack focus){return true;}
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus,int rank){return rank>=1&&rank<=5?List.of(FocusUpgrade.FRUGAL,FocusUpgrade.POTENCY):List.of();}
    static long experience(Player player){long level=Math.max(0,player.experienceLevel);long base=level<=16?level*level+6*level:level<=31?(5*level*level-81*level+720)/2:(9*level*level-325*level+4440)/2;return base+Math.round(player.experienceProgress*player.getXpNeededForNextLevel());}
    static int experienceCost(int frugal,int intended,int actual){return Math.max(1,Math.round(Math.max(1,15-frugal)*(actual/(float)intended)));}
    @Override public void usingTick(ItemStack stack,Player player,int remaining){
        if(player.level().isClientSide||!TinkererConfig.KAMI.get()||!player.isAlive()||!(stack.getItem() instanceof CastingWandItem wand))return;
        Aspect chosen=null;int lowest=Integer.MAX_VALUE,capacity=wand.capacity(stack);for(var aspect:TCAspects.catalog().primals()){int amount=wand.vis(stack,aspect);if(amount<capacity&&amount<lowest){chosen=aspect;lowest=amount;}}if(chosen==null)return;
        int intended=Math.round(500+100*wand.focusPotency(stack,player));int actual=Math.min(intended,capacity-lowest);int price=experienceCost(wand.focusUpgrade(stack,FocusUpgrade.FRUGAL),intended,actual);if(experience(player)<price)return;
        player.giveExperiencePoints(-price);wand.addVis(stack,chosen,actual,false,true);
    }
    @Override public void appendHoverText(ItemStack focus,net.minecraft.world.level.Level level,List<net.minecraft.network.chat.Component> lines,TooltipFlag flags){super.appendHoverText(focus,level,lines,flags);lines.add(net.minecraft.network.chat.Component.translatable("item.Focus.cost2"));lines.add(net.minecraft.network.chat.Component.translatable("ttmisc.experience").append(" x "+(15-upgradeLevel(focus,FocusUpgrade.FRUGAL.id))).withStyle(net.minecraft.ChatFormatting.GREEN));}
}
