package infusionbrewing;

import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import tc4revival.aspect.TCAspects;
import tc4revival.content.WandInventory;

public class PotionCurioItem extends Item implements ICurioItem {
    private final String element;
    public PotionCurioItem(String element){super(new Properties().stacksTo(1));this.element=element;}
    @Override public void curioTick(SlotContext context,ItemStack stack){
        if(context.cosmetic()||!(context.entity() instanceof Player player)||player.level().isClientSide)return;
        var optional=PotionData.read(stack);if(optional.isEmpty())return;var data=optional.get();if(BrewingRules.instant(data.legacyId()))return;
        var old=player.getEffect(data.effect());
        if(old!=null && old.getAmplifier()>=data.amplifier() && (old.isInfiniteDuration()||old.getDuration()>=300))return;
        var costs=Map.of(TCAspects.catalog().require(element),100,TCAspects.catalog().require("ordo"),50);
        if(!WandInventory.consume(player,costs))return;
        // The original discards a weaker effect before upgrading; modern addEffect alone
        // would retain it as a hidden effect and restore it after this dose expires.
        if(old!=null&&old.getAmplifier()<data.amplifier())player.removeEffect(data.effect());
        player.addEffect(new MobEffectInstance(data.effect(),BrewingRules.upkeepInterval(data.amplifier())+300,data.amplifier(),true,true));
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flags){PotionTooltips.append(stack,lines);}
}
