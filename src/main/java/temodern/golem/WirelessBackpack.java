package temodern.golem;

import java.util.List;
import java.util.Set;
import appeng.api.features.*;
import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.api.golems.GolemApi;
import tc4revival.content.*;
import temodern.TE;

@Mod.EventBusSubscriber(modid=TE.ID)
public final class WirelessBackpack extends Item implements IGridLinkableHandler {
    public static final ResourceLocation ID=ResourceLocation.fromNamespaceAndPath(TE.ID,"wireless_backpack");
    public WirelessBackpack(){super(new Properties().stacksTo(1));}
    @Override public boolean canLink(ItemStack stack){return stack.getItem()==this;}
    @Override public void link(ItemStack stack,GlobalPos pos){stack.getOrCreateTag().putString("dimension",pos.dimension().location().toString());stack.getOrCreateTag().putLong("accessPoint",pos.pos().asLong());}
    @Override public void unlink(ItemStack stack){stack.removeTagKey("dimension");stack.removeTagKey("accessPoint");}
    @Override public InteractionResult useOn(UseOnContext context){
        var player=context.getPlayer();
        if(player!=null && (!context.getLevel().mayInteract(player,context.getClickedPos())
                || !player.mayUseItemAt(context.getClickedPos(),context.getClickedFace(),context.getItemInHand())))return InteractionResult.FAIL;
        if(!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof IWirelessAccessPoint))return InteractionResult.PASS;
        if(!context.getLevel().isClientSide)link(context.getItemInHand(),GlobalPos.of(context.getLevel().dimension(),context.getClickedPos()));
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flags){
        lines.add(Component.translatable(stack.hasTag()&&stack.getTag().contains("accessPoint")?"gui.thaumicenergistics.linked":"gui.thaumicenergistics.unlinked"));
        lines.add(Component.translatable("gui.thaumicenergistics.backpack_help"));
    }
    public static CompoundTag data(GolemEntity golem){return golem.configuration().addonData(ID);}
    @SubscribeEvent public static void interact(PlayerInteractEvent.EntityInteract event){
        if(!(event.getTarget() instanceof GolemEntity golem))return;
        var player=event.getEntity();var held=player.getItemInHand(event.getHand());var data=data(golem);
        if(!golem.ownerName().isEmpty()&&!golem.ownerName().equalsIgnoreCase(player.getGameProfile().getName())&&!player.hasPermissions(2))return;
        if(held.getItem() instanceof WirelessBackpack){
            var config=golem.configuration();var core=config.core();
            if(!data.isEmpty()||config.customTask()!=null||core==null||!Set.of(GolemCoreType.GATHER,GolemCoreType.FILL,GolemCoreType.LIQUID,GolemCoreType.ESSENTIA).contains(core))return;
            if(!held.hasTag()||!held.getTag().contains("accessPoint"))return;
            event.setCancellationResult(InteractionResult.SUCCESS);event.setCanceled(true);
            if(player.level().isClientSide)return;
            GolemApi.data(golem,ID,held.copyWithCount(1).save(new CompoundTag()));
            GolemApi.configure(golem,config.customMaterial(),ID,config.blockAppearance());
            if(!player.getAbilities().instabuild)held.shrink(1);
        }else if(held.getItem() instanceof appeng.items.parts.FacadeItem&&!data.isEmpty()){
            event.setCancellationResult(InteractionResult.SUCCESS);event.setCanceled(true);
            if(player.level().isClientSide)return;
            var backpack=ItemStack.of(data);var old=ItemStack.of(backpack.getOrCreateTag().getCompound("facade"));
            backpack.getOrCreateTag().put("facade",held.copyWithCount(1).save(new CompoundTag()));
            if(!player.getAbilities().instabuild)held.shrink(1);
            GolemApi.data(golem,ID,backpack.save(new CompoundTag()));if(!old.isEmpty())player.getInventory().placeItemBackInInventory(old);
        }else if(held.isEmpty()&&player.isShiftKeyDown()&&!data.isEmpty()){
            event.setCancellationResult(InteractionResult.SUCCESS);event.setCanceled(true);
            if(player.level().isClientSide)return;
            var restored=ItemStack.of(data);var config=golem.configuration();
            GolemApi.data(golem,ID,new CompoundTag());GolemApi.configure(golem,config.customMaterial(),null,config.blockAppearance());
            player.getInventory().placeItemBackInInventory(restored);
        }
    }
    @SubscribeEvent public static void death(LivingDropsEvent event){
        if(event.getEntity() instanceof GolemEntity golem&&!golem.level().isClientSide){
            var stack=ItemStack.of(data(golem));if(stack.isEmpty())return;
            event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(golem.level(),golem.getX(),golem.getY(),golem.getZ(),stack));
            GolemApi.data(golem,ID,new CompoundTag());
        }
    }
    public static void register(){
        var item=TE.MATERIALS.get("golem_wireless_backpack").get();GridLinkables.register(item,(WirelessBackpack)item);
        GolemApi.registerTask(ID,WirelessWork::install);
    }
}
