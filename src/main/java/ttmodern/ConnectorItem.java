package ttmodern;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.network.chat.Component;

public final class ConnectorItem extends Item {
    public ConnectorItem(){super(new Properties().stacksTo(1));}
    @Override public InteractionResult useOn(UseOnContext context){
        var level=context.getLevel();var player=context.getPlayer();var pos=context.getClickedPos();
        if(player==null || !level.mayInteract(player,pos))return InteractionResult.FAIL;
        if(level.isClientSide)return InteractionResult.SUCCESS;
        var tag=context.getItemInHand().getOrCreateTag();String message;
        if(level.getBlockEntity(pos) instanceof GolemConnectorEntity){tag.remove("Source");tag.remove("Dimension");tag.putLong("GolemSource",pos.asLong());tag.putString("GolemDimension",level.dimension().location().toString());player.displayClientMessage(Component.translatable("ttmisc.connector.set"),false);return InteractionResult.CONSUME;}
        if(!tag.contains("Source")) {
            if(level.getBlockEntity(pos) instanceof TransvectorLink){tag.remove("GolemSource");tag.remove("GolemDimension");tag.putLong("Source",pos.asLong());tag.putString("Dimension",level.dimension().location().toString());message="set";}
            else message="notinterf";
        }else{
            var source=BlockPos.of(tag.getLong("Source"));
            if(!tag.getString("Dimension").equals(level.dimension().location().toString()) || !level.hasChunkAt(source) || !(level.getBlockEntity(source) instanceof TransvectorLink)){
                tag.remove("Source");tag.remove("Dimension");message="notpresent";
            }else if(!level.mayInteract(player,source))return InteractionResult.FAIL;
            else if(level.getBlockEntity(source) instanceof InterfaceEntity && level.getBlockEntity(pos) instanceof InterfaceEntity)message="interffail";
            else {
                var entity=(TransvectorLink)level.getBlockEntity(source);
                if(entity.link(pos)){tag.remove("Source");tag.remove("Dimension");message="complete";}
                else message="toofar";
            }
        }
        player.displayClientMessage(Component.translatable("ttmisc.connector."+message),false);
        if(message.equals("set") || message.equals("complete"))level.playSound(null,pos,net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,net.minecraft.sounds.SoundSource.BLOCKS,.8f,1);
        return InteractionResult.CONSUME;
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack,net.minecraft.world.entity.player.Player player,net.minecraft.world.entity.LivingEntity target,net.minecraft.world.InteractionHand hand){
        if(!player.isShiftKeyDown()||!(target instanceof tc4revival.content.GolemEntity golem))return InteractionResult.PASS;if(player.level().isClientSide)return InteractionResult.SUCCESS;
        var tag=stack.getOrCreateTag();String message="notinterf";
        if(tag.contains("GolemSource",net.minecraft.nbt.Tag.TAG_LONG)){
            var source=BlockPos.of(tag.getLong("GolemSource"));var level=player.level();
            if(!tag.getString("GolemDimension").equals(level.dimension().location().toString())||!level.hasChunkAt(source)||!(level.getBlockEntity(source) instanceof GolemConnectorEntity connector)){message="notpresent";tag.remove("GolemSource");tag.remove("GolemDimension");}
            else if(connector.bind(player,golem)){message="complete";tag.remove("GolemSource");tag.remove("GolemDimension");}else message="toofar";
        }player.displayClientMessage(Component.translatable("ttmisc.golemconnector."+message),false);return InteractionResult.CONSUME;
    }
}
