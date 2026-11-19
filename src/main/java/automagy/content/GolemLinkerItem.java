package automagy.content;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import thaumcraft.api.golems.GolemApi;
import tc4revival.content.GolemEntity;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=automagy.Automagy.ID)
public final class GolemLinkerItem extends Item {
    public static final ResourceLocation LINK=ResourceLocation.parse("automagy:device_link");
    public GolemLinkerItem(){super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));}
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void linkBeforeGolemMenu(net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract event){
        if(!(event.getItemStack().getItem() instanceof GolemLinkerItem linker)||!(event.getTarget() instanceof GolemEntity golem))return;
        // The golem menu consumes normal interaction before Item.interactLivingEntity runs.
        var result=linker.interactLivingEntity(event.getItemStack(),event.getEntity(),golem,event.getHand());
        if(result!=InteractionResult.PASS){event.setCanceled(true);event.setCancellationResult(result);}
    }
    @Override public boolean isFoil(ItemStack stack){return stack.hasTag()&&stack.getTag().hasUUID("Device");}
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context){return useOn(context);}
    @Override public InteractionResult useOn(UseOnContext context){
        Player player=context.getPlayer();var level=context.getLevel();BlockPos pos=context.getClickedPos();
        if(player==null||!player.mayBuild()||!level.mayInteract(player,pos))return InteractionResult.FAIL;
        if(!(level.getBlockEntity(pos) instanceof GolemLinkDevice device))return InteractionResult.PASS;
        if(!level.isClientSide){
            var tag=context.getItemInHand().getOrCreateTag();
            if(player.isShiftKeyDown()){device.resetLinks();tag.remove("Device");tag.remove("Position");tag.remove("Dimension");}
            else {tag.putUUID("Device",device.linkIdentity());tag.put("Position",NbtUtils.writeBlockPos(pos));tag.putString("Dimension",level.dimension().location().toString());}
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity entity,InteractionHand hand){
        if(!(entity instanceof GolemEntity golem)||player.isShiftKeyDown())return InteractionResult.PASS;
        if(!player.mayBuild()||!golem.ownerName().equals(player.getGameProfile().getName()))return InteractionResult.FAIL;
        var tag=stack.getTag();if(tag==null||!tag.hasUUID("Device")||!tag.getString("Dimension").equals(player.level().dimension().location().toString()))return InteractionResult.FAIL;
        BlockPos pos=NbtUtils.readBlockPos(tag.getCompound("Position"));
        if(!player.level().hasChunkAt(pos)||!(player.level().getBlockEntity(pos) instanceof GolemLinkDevice device)||!device.linkIdentity().equals(tag.getUUID("Device")))return InteractionResult.FAIL;
        if(!player.level().isClientSide){
            if(!golem.configuration().addonData(InventariumDeliveryGoal.ID).isEmpty())return InteractionResult.FAIL;
            var link=new CompoundTag();link.putUUID("Device",device.linkIdentity());link.put("Position",NbtUtils.writeBlockPos(pos));link.putString("Dimension",tag.getString("Dimension"));GolemApi.data(golem,LINK,link);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }
    public static boolean linked(GolemEntity golem,net.minecraft.world.level.block.entity.BlockEntity tile,GolemLinkDevice device){
        var tag=golem.configuration().addonData(LINK);return tag.hasUUID("Device")&&device.linkIdentity().equals(tag.getUUID("Device"))&&tag.getString("Dimension").equals(golem.level().dimension().location().toString())&&NbtUtils.readBlockPos(tag.getCompound("Position")).equals(tile.getBlockPos());
    }
}
