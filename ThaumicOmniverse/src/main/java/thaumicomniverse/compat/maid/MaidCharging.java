package thaumicomniverse.compat.maid;

import java.util.*;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.memory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.items.*;
import tc4revival.content.*;

/** The receipt stores identity and location, never a copy of the deposited wand. */
public final class MaidCharging {
    private static final String STATIONS="thaumicomniverse:maid_stations",PENDING="thaumicomniverse:maid_charge_receipt",TOKEN="thaumicomniverse:maid_wand";
    private static final Set<String> DEVICES=Set.of("thaumcraft:arcane_workbench","thaumcraft:wand_pedestal","thaumicomniverse:wand_charger","emt:wand_charger");
    private MaidCharging() {}
    private static BlockPos station(ServerLevel level,BlockPos clicked) {
        if(!level.hasChunkAt(clicked))return null;
        var entity=level.getBlockEntity(clicked);
        if(entity instanceof WorkbenchChargerBlockEntity)return station(level,clicked.below());
        return DEVICES.contains(net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(level.getBlockState(clicked).getBlock()).toString())?clicked:null;
    }
    private static IItemHandler handler(ServerLevel level,BlockPos pos) {
        if(!level.hasChunkAt(pos) || station(level,pos)==null)return null;
        var entity=level.getBlockEntity(pos);return entity==null?null:entity.getCapability(ForgeCapabilities.ITEM_HANDLER,null).orElse(null);
    }
    @SubscribeEvent(priority=EventPriority.HIGH) public static void choose(PlayerInteractEvent.EntityInteract event) {
        if(!(event.getTarget() instanceof EntityMaid maid) || !(event.getItemStack().getItem() instanceof GolemBellItem) || !event.getEntity().isShiftKeyDown() || !maid.isOwnedBy(event.getEntity()))return;
        if(!event.getLevel().isClientSide) {
            var tag=event.getItemStack().getOrCreateTag();tag.putUUID("OmniverseMaid",maid.getUUID());tag.putString("OmniverseMaidDimension",maid.level().dimension().location().toString());
            event.getEntity().displayClientMessage(net.minecraft.network.chat.Component.translatable("thaumicomniverse.maid.choose_station"),true);
        }
        event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void bind(PlayerInteractEvent.RightClickBlock event) {
        var held=event.getItemStack();if(!(held.getItem() instanceof GolemBellItem) || !held.hasTag() || !held.getTag().hasUUID("OmniverseMaid"))return;
        if(event.getLevel().isClientSide){event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);return;}
        var level=(ServerLevel)event.getLevel();var pos=station(level,event.getPos());if(pos==null || !level.mayInteract(event.getEntity(),pos) || !event.getEntity().mayUseItemAt(pos,event.getFace(),held))return;
        if(!level.dimension().location().toString().equals(held.getTag().getString("OmniverseMaidDimension")))return;
        var entity=level.getEntity(held.getTag().getUUID("OmniverseMaid"));
        if(!(entity instanceof EntityMaid maid) || !maid.isOwnedBy(event.getEntity()) || maid.distanceToSqr(event.getEntity())>64*64)return;
        var stations=maid.getPersistentData().getList(STATIONS,Tag.TAG_COMPOUND).copy();
        String dimension=level.dimension().location().toString();
        boolean removing=event.getEntity().isShiftKeyDown();
        boolean alreadyBound=false;
        for(var entry:stations){var saved=(CompoundTag)entry;if(saved.getLong("pos")==pos.asLong() && saved.getString("dimension").equals(dimension))alreadyBound=true;}
        if(!removing && !alreadyBound && stations.size()>=8) {
            event.getEntity().displayClientMessage(net.minecraft.network.chat.Component.translatable("thaumicomniverse.maid.station_limit"),true);
            event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);return;
        }
        for(int i=stations.size()-1;i>=0;i--)if(stations.getCompound(i).getLong("pos")==pos.asLong() && stations.getCompound(i).getString("dimension").equals(dimension))stations.remove(i);
        if(!event.getEntity().isShiftKeyDown() && stations.size()<8){var saved=new CompoundTag();saved.putLong("pos",pos.asLong());saved.putString("dimension",dimension);stations.add(saved);}
        maid.getPersistentData().put(STATIONS,stations);
        event.getEntity().displayClientMessage(net.minecraft.network.chat.Component.translatable(event.getEntity().isShiftKeyDown()?"thaumicomniverse.maid.station_removed":"thaumicomniverse.maid.station_added"),true);
        event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);
    }
    public static boolean pending(EntityMaid maid) { return maid.getPersistentData().contains(PENDING,Tag.TAG_COMPOUND); }
    /** Reclaim before the slab serializes the maid, rather than after its inventory was saved. */
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void store(com.github.tartaricacid.touhoulittlemaid.api.event.InteractMaidEvent event) {
        if(event.getStack().getItem()!=com.github.tartaricacid.touhoulittlemaid.init.InitItems.SMART_SLAB_EMPTY.get()
                || !event.getMaid().isOwnedBy(event.getPlayer()))return;
        // Forge persistent data is server-owned. Do not let the client's slab handler predict
        // discard while the server may still refuse storage until the wand has been retrieved.
        if(event.getWorld().isClientSide || !reclaim(event.getMaid()))event.setCanceled(true);
    }
    @SubscribeEvent public static void dimension(net.minecraftforge.event.entity.EntityTravelToDimensionEvent event) {
        if(event.getEntity() instanceof EntityMaid maid && !maid.level().isClientSide && !reclaim(maid))event.setCanceled(true);
    }
    private static boolean ready(ItemStack stack,double fraction) {
        if(!(stack.getItem() instanceof CastingWandItem wand))return false;
        return tc4revival.aspect.TCAspects.catalog().primals().stream().allMatch(aspect->wand.vis(stack,aspect)>=wand.capacity(stack)*fraction);
    }
    private static boolean owns(ItemStack stack,CompoundTag receipt) { return stack.hasTag() && stack.getTag().hasUUID(TOKEN) && stack.getTag().getUUID(TOKEN).equals(receipt.getUUID("token")); }
    private static boolean room(EntityMaid maid,ItemStack wand) { return maid.getMainHandItem().isEmpty() || ItemHandlerHelper.insertItemStacked(maid.getAvailableInv(false),wand,true).isEmpty(); }
    private static void carry(EntityMaid maid,ItemStack wand) {
        wand.removeTagKey(TOKEN);
        if(maid.getMainHandItem().isEmpty())maid.setItemInHand(InteractionHand.MAIN_HAND,wand);
        else { var remainder=ItemHandlerHelper.insertItemStacked(maid.getAvailableInv(false),wand,false);if(!remainder.isEmpty())maid.spawnAtLocation(remainder); }
        maid.getPersistentData().remove(PENDING);
        maid.getPersistentData().putLong("thaumicomniverse:maid_next_charge",maid.level().getGameTime()+200);
    }
    /** Called before owner-follow teleport. Never loads a chunk or manufactures a replacement. */
    public static boolean reclaim(EntityMaid maid) {
        if(!pending(maid))return true;
        var receipt=maid.getPersistentData().getCompound(PENDING);
        var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,ResourceLocation.parse(receipt.getString("dimension")));
        var level=maid.getServer().getLevel(key);var pos=BlockPos.of(receipt.getLong("pos"));
        if(level==null || !level.hasChunkAt(pos))return false;
        if(maid.level()!=level || maid.distanceToSqr(pos.getCenter())>16) {
            if(maid.level()==level)maid.getBrain().setMemory(MemoryModuleType.WALK_TARGET,new WalkTarget(pos,.65f,1));return false;
        }
        var inventory=handler(level,pos);
        if(inventory!=null)for(int slot=0;slot<inventory.getSlots();slot++) {
            var wand=inventory.getStackInSlot(slot);if(!owns(wand,receipt))continue;
            if(!room(maid,wand))return false;
            var extracted=inventory.extractItem(slot,1,false);if(extracted.isEmpty())return false;
            carry(maid,extracted);return true;
        }
        for(var item:level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(5),entity->owns(entity.getItem(),receipt))) {
            if(!room(maid,item.getItem()))return false;
            var wand=item.getItem().copy();item.discard();carry(maid,wand);return true;
        }
        // Someone else removed it: release the reservation, but never duplicate it from NBT.
        maid.getPersistentData().remove(PENDING);return true;
    }
    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent event) {
        if(!(event.getEntity() instanceof EntityMaid maid) || !(maid.level() instanceof ServerLevel level) || maid.tickCount%10!=0)return;
        var data=maid.getPersistentData();
        if(pending(maid)) {
            var receipt=data.getCompound(PENDING);
            // A receipt belongs to one dimension; never inspect its coordinates in another world.
            if(!receipt.getString("dimension").equals(level.dimension().location().toString()))return;
            var pos=BlockPos.of(receipt.getLong("pos"));var inventory=handler(level,pos);
            boolean take=!thaumicomniverse.RevivalFeatures.Group.MAID.enabled() || !maid.getTask().getUid().equals(MaidWandTask.ID) || maid.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
                    || maid.getOwner()!=null && maid.distanceToSqr(maid.getOwner())>64 || level.getGameTime()-receipt.getLong("since")>=2400;
            // Depositing the wand disables StartAttacking's held-wand predicate, but nearby danger
            // must still interrupt charging before the attack memory has been populated.
            take|=com.github.tartaricacid.touhoulittlemaid.api.task.IRangedAttackTask.findFirstValidAttackTarget(maid).isPresent();
            boolean found=false;
            if(inventory!=null)for(int slot=0;slot<inventory.getSlots();slot++)if(owns(inventory.getStackInSlot(slot),receipt)) {
                found=true;take|=ready(inventory.getStackInSlot(slot),.9);
            }
            // A broken device or a wand removed by a player must not leave the maid waiting
            // for the two-minute charging timeout. Reclaim checks dropped items as well.
            take|=level.hasChunkAt(pos)&&!found;
            if(take)reclaim(maid);return;
        }
        if(!thaumicomniverse.RevivalFeatures.Group.MAID.enabled() || !maid.getTask().getUid().equals(MaidWandTask.ID) || maid.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET) || level.getGameTime()<data.getLong("thaumicomniverse:maid_next_charge"))return;
        if(!maid.canBrainMoving() || maid.getOwner()!=null && maid.distanceToSqr(maid.getOwner())>64)return;
        var wand=maid.getMainHandItem();if(!(wand.getItem() instanceof CastingWandItem item) || !item.rechargeable(wand) || ready(wand,.25))return;
        BlockPos selected=null;double distance=32*32;
        for(var entry:data.getList(STATIONS,Tag.TAG_COMPOUND)) {
            var saved=(CompoundTag)entry;if(!saved.getString("dimension").equals(level.dimension().location().toString()))continue;
            var pos=BlockPos.of(saved.getLong("pos"));var inventory=handler(level,pos);if(inventory==null)continue;
            double candidate=maid.distanceToSqr(pos.getCenter());if(candidate>=distance || !maid.isWithinRestriction(pos))continue;
            if(ItemHandlerHelper.insertItem(inventory,wand,true).isEmpty()){selected=pos;distance=candidate;}
        }
        if(selected==null)return;
        if(distance>9){maid.getBrain().setMemory(MemoryModuleType.WALK_TARGET,new WalkTarget(selected,.6f,1));return;}
        MaidWandCombat.stop(maid);var inventory=handler(level,selected);if(inventory==null)return;
        var token=UUID.randomUUID();var deposit=wand.copy();deposit.getOrCreateTag().putUUID(TOKEN,token);
        if(!ItemHandlerHelper.insertItem(inventory,deposit,true).isEmpty())return;
        var remainder=ItemHandlerHelper.insertItem(inventory,deposit,false);if(!remainder.isEmpty())return;
        var receipt=new CompoundTag();receipt.putLong("pos",selected.asLong());receipt.putString("dimension",level.dimension().location().toString());receipt.putUUID("token",token);receipt.putLong("since",level.getGameTime());
        data.put(PENDING,receipt);maid.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
    }
}
