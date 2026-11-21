package ttmodern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import tc4revival.content.*;
import tc4revival.aspect.*;

public final class DislocationFocus extends TinkererFocus {
    public DislocationFocus(){super("focus_dislocation","TTDL",0xffb200);}
    @Override public Map<Aspect,Integer> costs(ItemStack focus){return payment(null);}
    private static Map<Aspect,Integer> payment(BlockEntity entity){
        int factor=entity instanceof SpawnerBlockEntity?100:entity!=null?25:5;
        return Map.of(TCAspects.catalog().require("ordo"),factor*100,TCAspects.catalog().require("perditio"),factor*100,TCAspects.catalog().require("terra"),factor*(entity instanceof SpawnerBlockEntity?50:20));
    }
    public static boolean carrying(ItemStack focus){return focus.hasTag()&&focus.getTag().contains("CarriedBlock",Tag.TAG_COMPOUND);}
    public static BlockState carried(ItemStack focus){return carrying(focus)?NbtUtils.readBlockState(net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup(),focus.getTag().getCompound("CarriedBlock").getCompound("State")):Blocks.AIR.defaultBlockState();}
    @Override public String sortingKey(ItemStack focus){
        if(!carrying(focus))return super.sortingKey(focus);
        try{return super.sortingKey(focus)+java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(focus.getTag().getCompound("CarriedBlock").toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<net.minecraft.network.chat.Component> lines,TooltipFlag flag){super.appendHoverText(stack,level,lines,flag);if(carrying(stack))lines.add(carried(stack).getBlock().getName());}
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack,Level level,Player player,InteractionHand hand,BlockHitResult hit){
        if(level.isClientSide||hit==null||hit.getType()!=net.minecraft.world.phys.HitResult.Type.BLOCK)return InteractionResultHolder.pass(stack);
        var focus=attached(stack);boolean moved=carrying(focus)?place(stack,focus,player,hit):take(stack,focus,player,hit);
        if(moved){tc4revival.network.FocusEffectPacket.burst(level,hit.getLocation(),.2f);level.playSound(null,player.blockPosition(),TCSounds.WAND.get(),net.minecraft.sounds.SoundSource.PLAYERS,.5f,1);player.swing(hand);}
        return moved?InteractionResultHolder.success(stack):InteractionResultHolder.pass(stack);
    }
    private static boolean editable(Level level,BlockPos pos,Player player,Direction face,ItemStack stack){return !level.isOutsideBuildHeight(pos)&&level.hasChunkAt(pos)&&level.getWorldBorder().isWithinBounds(pos)&&level.mayInteract(player,pos)&&player.mayUseItemAt(pos,face,stack);}
    private static boolean allowed(Level level,BlockPos pos){
        var state=level.getBlockState(pos);String id=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        return !state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)&&!state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.BED_PART)&&!state.isAir()&&state.getBlock().asItem()!=Items.AIR&&state.getDestroySpeed(level,pos)>=0&&!state.is(Blocks.MOVING_PISTON)&&!state.is(Blocks.PISTON_HEAD)
            &&!state.is(PortableHoleFocusItem.BLACKLIST)&&!tc4revival.config.TCConfig.SERVER.portableHoleBlacklist.get().contains(id)&&!TinkererConfig.DISLOCATION_BLACKLIST.get().contains(id)
            &&!(level.getBlockEntity(pos) instanceof AuraNodeBlockEntity)&&!state.is(tc4revival.content.TCArcane.PORTABLE_HOLE.get());
    }
    private static BlockEntity reconstruct(BlockPos pos,BlockState state,CompoundTag payload){
        if(!payload.contains("Entity",Tag.TAG_COMPOUND))return null;
        var tag=payload.getCompound("Entity").copy();tag.putInt("x",pos.getX());tag.putInt("y",pos.getY());tag.putInt("z",pos.getZ());
        var entity=BlockEntity.loadStatic(pos,state,tag);if(entity==null||!entity.getType().isValid(state))throw new IllegalArgumentException("Invalid carried block entity");return entity;
    }
    private boolean take(ItemStack stack,ItemStack focus,Player player,BlockHitResult hit){
        var level=player.level();var pos=hit.getBlockPos();if(!editable(level,pos,player,hit.getDirection(),stack)||!allowed(level,pos))return false;
        var state=level.getBlockState(pos);var entity=level.getBlockEntity(pos);var wand=(CastingWandItem)stack.getItem();var cost=payment(entity);
        if(!wand.consumeForPlayer(stack,player,cost,false,false,false)||net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(level,net.minecraft.world.level.GameType.SURVIVAL,(net.minecraft.server.level.ServerPlayer)player,pos)<0)return false;
        if(!level.getBlockState(pos).equals(state)||level.getBlockEntity(pos)!=entity)return false;
        var payload=new CompoundTag();payload.put("State",NbtUtils.writeBlockState(state));if(entity!=null)payload.put("Entity",entity.saveWithFullMetadata());
        BlockEntity restore;try{restore=reconstruct(pos,state,payload);}catch(RuntimeException invalid){return false;}
        level.removeBlockEntity(pos);
        if(!level.setBlock(pos,Blocks.AIR.defaultBlockState(),2|16)){if(restore!=null)level.setBlockEntity(restore);return false;}
        if(!wand.consumeForPlayer(stack,player,cost,false,false,true)){level.setBlock(pos,state,2|16);if(restore!=null)level.setBlockEntity(restore);return false;}
        focus.getOrCreateTag().put("CarriedBlock",payload);attach(stack,focus);level.updateNeighborsAt(pos,state.getBlock());level.updateNeighbourForOutputSignal(pos,state.getBlock());return true;
    }
    private boolean place(ItemStack stack,ItemStack focus,Player player,BlockHitResult hit){
        var level=player.level();var pos=hit.getBlockPos().relative(hit.getDirection());if(!editable(level,pos,player,hit.getDirection(),stack))return false;
        var previous=level.getBlockState(pos);var state=carried(focus);
        if(state.isAir()||!previous.canBeReplaced()||level.getBlockEntity(pos)!=null||!state.canSurvive(level,pos)||!level.isUnobstructed(state,pos,net.minecraft.world.phys.shapes.CollisionContext.of(player)))return false;
        BlockEntity entity;try{entity=reconstruct(pos,state,focus.getTag().getCompound("CarriedBlock"));}catch(RuntimeException invalid){return false;}
        var snapshot=net.minecraftforge.common.util.BlockSnapshot.create(level.dimension(),level,pos);
        if(!level.setBlock(pos,state,2|16))return false;
        if(entity!=null){level.removeBlockEntity(pos);level.setBlockEntity(entity);entity.setChanged();}
        if(net.minecraftforge.event.ForgeEventFactory.onBlockPlace(player,snapshot,hit.getDirection())){
            level.removeBlockEntity(pos);level.setBlock(pos,previous,2|16);return false;
        }
        focus.getTag().remove("CarriedBlock");attach(stack,focus);level.sendBlockUpdated(pos,previous,state,3);level.updateNeighborsAt(pos,state.getBlock());level.updateNeighbourForOutputSignal(pos,state.getBlock());return true;
    }
}
