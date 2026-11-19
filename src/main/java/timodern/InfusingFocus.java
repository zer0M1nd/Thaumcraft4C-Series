package timodern;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerLevel;
import tc4revival.content.*;
import tc4revival.aspect.*;

public class InfusingFocus extends thaumcraft.api.wands.ItemFocusBasic {
    public int color(ItemStack focus){return 0;}
    public Map<Aspect,Integer> costs(ItemStack focus){return Map.of();}
    public List<FocusUpgrade> upgradesForRank(ItemStack focus,int rank){return List.of();}
    public String sortingPrefix(){return "BWI";}
    public String depthTexture(){return "thaumicinfusion:items/focus_infusion_depth";}
    public String ornamentTexture(){return "thaumicinfusion:item/focus_infusion_orn";}
    public InteractionResultHolder<ItemStack> cast(ItemStack wand,Level level,Player player,InteractionHand hand,BlockHitResult hit){
        if(hit==null||hit.getType()!=net.minecraft.world.phys.HitResult.Type.BLOCK){if(level.isClientSide)InfusionClient.open(hand);return InteractionResultHolder.sidedSuccess(wand,level.isClientSide);}
        if(!level.isClientSide)apply((ServerLevel)level,hit.getBlockPos(),player,wand.hasTag()?wand.getTag().getString("InfusionAspect"):"");
        return InteractionResultHolder.sidedSuccess(wand,level.isClientSide);
    }
    public static boolean apply(ServerLevel level,BlockPos pos,Player player,String aspect){
        if(!level.mayInteract(player,pos)||level.getBlockState(pos).isAir()||InfusionConfig.banned(level.getBlockState(pos).getBlock()))return false;
        var store=InfusionData.get(level);var entry=store.entries.get(pos.asLong());
        if(aspect.isEmpty()){
            if(entry==null)return false;
            if(!player.isCreative())for(String a:entry.aspects)refund(level,player.blockPosition(),a,InfusionEffects.get(a).cost());
            InfusionData.remove(level,pos);return true;
        }
        var definition=InfusionEffects.get(aspect);if(definition==null||!InfusionConfig.enabled(aspect)||TCAspects.catalog().find(aspect).isEmpty())return false;
        if(entry!=null&&entry.aspects.contains(aspect)){
            // Preserve the original acquisition rule, including its ordinary removal quirk:
            // removing one of several effects costs another fee; selecting the last removes it without a refund.
            if(entry.aspects.size()>1&&!player.isCreative()&&!drain(level,player.blockPosition(),aspect,definition.cost()))return false;
            entry.aspects.remove(aspect);
            if(entry.aspects.isEmpty())store.entries.remove(pos.asLong());
        }else{
            if(!player.isCreative()&&!drain(level,player.blockPosition(),aspect,definition.cost()))return false;
            if(entry==null){entry=new InfusionData.Entry();store.entries.put(pos.asLong(),entry);}
            var previous=new ArrayList<>(entry.aspects);entry.aspects.clear();entry.aspects.add(aspect);entry.aspects.addAll(previous);
        }
        InfusionData.changed(level,pos);level.playSound(null,pos,TCSounds.WAND.get(),net.minecraft.sounds.SoundSource.PLAYERS,.7f,.9f+level.random.nextFloat()*.1f);return true;
    }
    public static boolean drain(ServerLevel level,BlockPos center,String id,int amount){
        var aspect=TCAspects.catalog().find(id).orElse(null);if(aspect==null)return false;
        for(var p:BlockPos.betweenClosed(center.offset(-10,-10,-10),center.offset(9,9,9))){if(!level.hasChunkAt(p))continue;var vessel=thaumcraft.api.aspects.AspectSourceHelper.phialVessel(level.getBlockEntity(p),aspect);if(vessel!=null&&vessel.contains(aspect,amount)&&vessel.takeFromContainer(aspect,amount))return true;}return false;
    }
    public static int refund(ServerLevel level,BlockPos center,String id,int amount){
        var aspect=TCAspects.catalog().find(id).orElse(null);if(aspect==null)return amount;
        for(var p:BlockPos.betweenClosed(center.offset(-10,-10,-10),center.offset(9,9,9))){if(!level.hasChunkAt(p))continue;var vessel=thaumcraft.api.aspects.AspectSourceHelper.phialVessel(level.getBlockEntity(p),aspect);if(vessel!=null&&vessel.accepts(aspect)){amount=vessel.addToContainer(aspect,amount);if(amount==0)break;}}return amount;
    }
}
