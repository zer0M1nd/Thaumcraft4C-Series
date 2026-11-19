package fmmodern;

import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.wands.*;
import tc4revival.aspect.*;
import tc4revival.content.*;

public final class DarkWands {
    public static void creative(java.util.function.Consumer<ItemStack> output){
        for(String rod:new String[]{"tainted","infernal"})output.accept(thaumcraft.api.ThaumcraftApi.createWand("forbidden_magic:"+rod,"thaumium",false,false));
        for(String rod:new String[]{"neutronium","neutronium_staff"})output.accept(thaumcraft.api.ThaumcraftApi.createWand("forbidden_magic:"+rod,"forbidden_magic:orichalcum",false,true));
        output.accept(thaumcraft.api.ThaumcraftApi.createWand("forbidden_magic:profane","iron",false,true));
        if(!ForbiddenConfig.value(ForbiddenConfig.CROSS_WAND))return;
        if(IntegrationContent.bloodEnabled())for(String rod:new String[]{"blood","blood_staff"})output.accept(thaumcraft.api.ThaumcraftApi.createWand("forbidden_magic:"+rod,"forbidden_magic:alchemical",false,false));
        if(IntegrationContent.botanyEnabled()){
            output.accept(thaumcraft.api.ThaumcraftApi.createWand("forbidden_magic:livingwood","forbidden_magic:manasteel",false,false));
            for(String rod:new String[]{"dreamwood","dreamwood_staff"})output.accept(thaumcraft.api.ThaumcraftApi.createWand("forbidden_magic:"+rod,"forbidden_magic:elementium",false,false));
            output.accept(thaumcraft.api.ThaumcraftApi.createWand("wood","forbidden_magic:terrasteel",false,false));
        }
    }
    public static void setup(){
        for(String name:new String[]{"tainted","infernal","profane","neutronium","neutronium_staff"})WandRod.registerUpdate(ForbiddenMagic.ID+":"+name,DarkWands::tick);
        WandTriggerRegistry.register(new ResourceLocation(ForbiddenMagic.ID,"hellfire"),s->s.is(Blocks.OBSIDIAN)||s.is(Blocks.NETHERRACK),context->{
            if(!(context.getPlayer() instanceof ServerPlayer player)||context.getClickedFace()!=Direction.UP||!(context.getItemInHand().getItem() instanceof CastingWandItem wand)||!WandFocusItem.attached(context.getItemInHand()).isEmpty()||!ThaumcraftApiHelper.isResearchComplete(player,"forbidden_magic:HELLFIRE"))return InteractionResult.PASS;
            var pos=context.getClickedPos().above();var level=player.serverLevel();var stack=context.getItemInHand();
            if(!level.isEmptyBlock(pos)||!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,Direction.UP,stack))return InteractionResult.PASS;
            var fire=BaseFireBlock.getState(level,pos);if(!fire.canSurvive(level,pos))return InteractionResult.PASS;
            var cost=Map.of(TCAspects.catalog().require("ignis"),200);
            if(!wand.consumeForPlayer(stack,player,cost,false,false,false))return InteractionResult.PASS;
            var snapshot=net.minecraftforge.common.util.BlockSnapshot.create(level.dimension(),level,pos);
            if(net.minecraftforge.event.ForgeEventFactory.onBlockPlace(player,snapshot,Direction.UP))return InteractionResult.PASS;
            if(!wand.consumeForPlayer(stack,player,cost,false,false,true))return InteractionResult.PASS;
            level.setBlock(pos,fire,3);level.playSound(null,pos,net.minecraft.sounds.SoundEvents.FIRECHARGE_USE,net.minecraft.sounds.SoundSource.BLOCKS,1,1);return InteractionResult.SUCCESS;
        });
    }
    private static void tick(ItemStack stack,Player player){
        if(!(stack.getItem() instanceof CastingWandItem wand)||player.level().isClientSide)return;
        String rod=WandParts.rod(stack.getTag()).tag;int capacity=wand.capacity(stack);
        if(rod.endsWith(":neutronium")||rod.endsWith(":neutronium_staff")){for(var aspect:TCAspects.catalog().primals())wand.addVis(stack,aspect,capacity,false,true);return;}
        if(rod.endsWith(":infernal")){player.clearFire();player.removeEffect(MobEffects.WITHER);}
        if(rod.endsWith(":profane")){
            if(player.tickCount%20!=0||!(player instanceof ServerPlayer serverPlayer))return;
            var tag=stack.getOrCreateTag();
            if(!tag.contains("contract")){tag.putInt("contract",25000);ThaumcraftApiHelper.completeResearch(serverPlayer,"forbidden_magic:ROD_profane");}
            int remaining=Math.max(0,tag.getInt("contract"));
            for(var aspect:TCAspects.catalog().primals()){
                int restore=Math.min(remaining,Math.max(0,capacity-wand.vis(stack,aspect)));if(restore==0)continue;
                wand.addVis(stack,aspect,restore,false,true);remaining-=restore;
                if(player.getRandom().nextInt(2501)<restore)ThaumcraftApiHelper.addStickyWarpToPlayer(serverPlayer,1);
                if(remaining==0)break;
            }
            stack.getOrCreateTag().putInt("contract",remaining);
            if(remaining==0){stack.getOrCreateTag().putString("rod","forbidden_magic:profaned");ThaumcraftApiHelper.addStickyWarpToPlayer(serverPlayer,1);serverPlayer.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,player.getX(),player.getY()+1,player.getZ(),1,0,0,0,0);}return;
        }
        if(player.tickCount%100!=0)return;
        boolean tainted=rod.endsWith(":tainted")&&player.level().getBiome(player.blockPosition()).unwrapKey().map(k->k.location().toString().equals("thaumcraft:tainted_land")).orElse(false);
        for(var aspect:TCAspects.catalog().primals()){
            int limit=0;
            if(tainted)limit=capacity/10;
            else if(rod.endsWith(":infernal")){if(aspect.tag().equals("ignis"))limit=capacity/5;else if(player.level().dimension()==Level.NETHER)limit=capacity/10;}
            if(wand.vis(stack,aspect)<limit)wand.addVis(stack,aspect,1,true,true);
        }
    }
    @SubscribeEvent public void hurt(LivingHurtEvent event){
        if(!(event.getEntity() instanceof Player player))return;var stack=player.getMainHandItem();if(!(stack.getItem() instanceof CastingWandItem wand))return;
        String rod=WandParts.rod(stack.getTag()).tag;
        if(rod.equals("forbidden_magic:infernal")&&(event.getSource().is(DamageTypeTags.IS_FIRE)||event.getSource().is(DamageTypes.WITHER)))event.setCanceled(true);
        if(rod.equals("forbidden_magic:tainted")&&event.getSource().getMsgId().equals("taint")){var primals=TCAspects.catalog().primals();for(int i=0;i<3;i++)wand.addVis(stack,primals.get(player.getRandom().nextInt(primals.size())),1,true,true);}
    }
    @SubscribeEvent public void tooltip(ItemTooltipEvent event){var stack=event.getItemStack();if(!(stack.getItem() instanceof CastingWandItem))return;String rod=WandParts.rod(stack.getTag()).tag;if(rod.startsWith("forbidden_magic:profane"))event.getToolTip().add(Component.translatable("tooltip.forbidden_magic.contract",stack.hasTag()&&stack.getTag().contains("contract")?stack.getTag().getInt("contract"):25000));if(rod.startsWith("forbidden_magic:neutronium"))event.getToolTip().add(Component.translatable("tooltip.creativeonly"));}
}
