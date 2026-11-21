package gadomancymodern.golem;
import gadomancymodern.Gadomancy;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.content.*;

@Mod.EventBusSubscriber(modid="gadomancy")
public class GolemEvents {
    @SubscribeEvent public static void interact(net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract event){
        if(!(event.getTarget() instanceof GolemEntity golem))return;var held=event.getItemStack();
        if(held.getItem() instanceof gadomancymodern.item.AdditionalGolemCoreItem core&&!golem.configuration().hasTask()){
            if(!golem.level().isClientSide&&GadGolems.install(golem,core.task)){if(!event.getEntity().getAbilities().instabuild)held.shrink(1);golem.playSound(TCSounds.GOLEM_UPGRADE.get(),.5f,1);}
            event.setCanceled(true);event.setCancellationResult(InteractionResult.sidedSuccess(golem.level().isClientSide));return;
        }
        if(GadGolems.bodyguard(golem)&&held.getItem() instanceof GolemBellItem){event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);return;}
        if((GadGolems.bodyguard(golem)||GadGolems.breaking(golem))&&!(held.getItem() instanceof GolemBellItem)&&!(held.getItem() instanceof GolemComponentItem)&&!(held.getItem() instanceof CastingWandItem)){
            if(!golem.level().isClientSide){if(GadGolems.bodyguard(golem)){var data=GadGolems.data(golem);data.putBoolean("Sitting",!data.getBoolean("Sitting"));GadGolems.data(golem,data);}else if(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)net.minecraftforge.network.NetworkHooks.openScreen(player,new net.minecraft.world.SimpleMenuProvider((id,inventory,viewer)->new GolemMenu(TCArcane.GOLEM_MENU.get(),id,inventory,golem),golem.getDisplayName()),buffer->{buffer.writeInt(golem.getId());buffer.writeNbt(golem.state().visualData());});}
            event.setCanceled(true);event.setCancellationResult(InteractionResult.sidedSuccess(golem.level().isClientSide));
        }
    }
    @SubscribeEvent public static void tick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event){if(event.getEntity() instanceof GolemEntity golem&&!golem.level().isClientSide){if(GadGolems.bodyguard(golem)){if(!golem.home().equals(golem.blockPosition()))golem.state().home(golem.blockPosition(),golem.homeFacing());golem.clearRestriction();}if(GolemShield.upgraded(golem))GolemShield.charge(golem);}}
    @SubscribeEvent public static void hurt(net.minecraftforge.event.entity.living.LivingHurtEvent event){if(event.getEntity() instanceof GolemEntity golem&&!golem.level().isClientSide&&event.getAmount()>0&&GolemShield.upgraded(golem))event.setAmount(GolemShield.absorb(golem,event.getAmount(),event.getSource()));}
    @SubscribeEvent public static void recover(net.minecraftforge.event.entity.player.AttackEntityEvent event){
        if(!(event.getTarget() instanceof GolemEntity golem)||!golem.isAlive()||!(event.getEntity().getMainHandItem().getItem() instanceof GolemBellItem)||!GadGolems.silverwood(golem)&&!GadGolems.breaking(golem)&&!GadGolems.bodyguard(golem))return;
        event.setCanceled(true);if(golem.level().isClientSide){golem.spawnAnim();return;}recover(golem,event.getEntity().isShiftKeyDown());
    }
    public static void recover(GolemEntity golem,boolean dismantle){
        var portable=GadGolems.portable(golem,dismantle);if(golem.spawnAtLocation(portable,.5f)==null)return;
        if(dismantle){if(golem.configuration().core()!=null)golem.spawnAtLocation(GadGolems.breaking(golem)?new ItemStack(Gadomancy.BREAK_CORE.get()):GadGolems.bodyguard(golem)?new ItemStack(Gadomancy.BODYGUARD_CORE.get()):new ItemStack(TCItems.golemCore(golem.configuration().core().ordinal())),.5f);for(byte upgrade:golem.configuration().upgrades())if(upgrade>=0&&golem.getRandom().nextBoolean())golem.spawnAtLocation(new ItemStack(TCItems.golemUpgrade(upgrade)),.5f);}
        golem.dropCargo();golem.playSound(TCSounds.ZAP.get(),.5f,1);golem.discard();
    }
    @SubscribeEvent public static void tooltip(net.minecraftforge.event.entity.player.ItemTooltipEvent event){var stack=event.getItemStack();if(!(stack.getItem() instanceof GolemPlacerItem))return;var tag=stack.getTag();if(tag!=null&&tag.contains("AddonTask")){String task=tag.getString("AddonTask");if(task.equals(GadGolems.BREAK.toString())||task.equals(GadGolems.BODYGUARD.toString())){var text=net.minecraft.network.chat.Component.translatable(task.equals(GadGolems.BREAK.toString())?"gadomancy.golem.breakcore":"gadomancy.golem.bodyguardcore").withStyle(net.minecraft.ChatFormatting.GOLD);if(tag.contains("core")&&event.getToolTip().size()>1)event.getToolTip().set(1,text);else event.getToolTip().add(text);}}if(GolemShield.upgraded(stack))event.getToolTip().add(net.minecraft.network.chat.Component.translatable("gadomancy.upgrades.runicshield").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));}
}
