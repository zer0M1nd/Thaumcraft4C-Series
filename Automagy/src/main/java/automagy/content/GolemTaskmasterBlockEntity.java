package automagy.content;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class GolemTaskmasterBlockEntity extends BlockEntity implements InventariumMemoryUser,GolemLinkDevice {
    private BlockPos core;
    private UUID identity=UUID.randomUUID();
    private int cooldown;
    private boolean active;
    public boolean active(){return active;}
    private void active(boolean value){if(active!=value){active=value;setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}}
    public static void tick(net.minecraft.world.level.Level level,BlockPos pos,BlockState state,GolemTaskmasterBlockEntity master){
        if(level.isClientSide||master.cooldown-- >0)return;
        master.cooldown=19;var core=master.core();master.active(core!=null);if(core==null)return;
        var requests=core.requests().requests();if(requests.isEmpty())return;master.cooldown=0;
        var golems=level.getEntitiesOfClass(tc4revival.content.GolemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(5),g->GolemLinkerItem.linked(g,master,master));
        golems.sort(java.util.Comparator.comparing(g->g.getUUID().toString()));
        var sources=core.readableSources();int seen=0;
        for(var golem:golems){
            if(++seen>6)break;
            if(!golem.state().workEnabled()||!golem.carried().isEmpty()||golem.getDeltaMovement().horizontalDistanceSqr()>.0001||Math.abs(golem.getDeltaMovement().y)>.1||golem.configuration().core()!=tc4revival.content.GolemCoreType.FILL||!golem.configuration().addonData(InventariumDeliveryGoal.ID).isEmpty())continue;
            var custom=golem.configuration().customTask();if(custom!=null&&!custom.equals(InventariumDeliveryGoal.ID))continue;
            var destinationPos=golem.homeInventory();
            var destination=InventoryTransfer.at(level,destinationPos,golem.homeFacing());if(destination.isEmpty())continue;
            boolean assigned=false;
            for(var request:requests){
                if(!request.destination().dimension().equals(level.dimension()))continue;
                for(var source:sources.entrySet()){
                    if(InventariumContents.sameInventory(level,source.getKey(),new InventariumContents.Source(destinationPos,golem.homeFacing())))continue;
                    var inventory=InventariumContents.inventory(level,source.getKey());if(inventory.isEmpty())continue;
                    long available=InventoryTransfer.count(InventoryTransfer.snapshot(new InventoryPrefix(inventory.get(),source.getValue()),true),request.item().stack(),net.minecraft.world.item.ItemStack::isSameItemSameTags);
                    int limit=(int)Math.min(available,Math.min(golem.configuration().carryLimit(),request.item().stack().getMaxStackSize()));
                    limit=InventoryTransfer.room(destination.get(),request.item().stack(),limit);if(limit<=0)continue;
                    var claim=core.requests().claim(request.item(),request.destination(),limit);if(claim.isEmpty())continue;
                    assigned=InventariumDeliveryGoal.assign(golem,core,claim.get().id(),source.getKey().position(),source.getKey().side(),source.getValue());
                    if(!assigned)core.requests().fail(claim.get().id());else break;
                }
                if(assigned)break;
            }
        }
    }
    public GolemTaskmasterBlockEntity(BlockPos pos,BlockState state){super(Content.TASKMASTER_TILE.get(),pos,state);}
    @Override public BlockPos linkedCore(){return core;}
    @Override public void linkCore(BlockPos pos){core=pos==null?null:pos.immutable();setChanged();}
    @Override public UUID linkIdentity(){return identity;}
    @Override public void resetLinks(){identity=UUID.randomUUID();setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    public InventariumBlockEntity core(){return level!=null&&core!=null&&level.hasChunkAt(core)&&level.getBlockEntity(core) instanceof InventariumBlockEntity inventory&&inventory.hasMemoryUser(worldPosition)&&inventory.capacity().loaded()?inventory:null;}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putBoolean("Active",active);tag.putUUID("Identity",identity);if(core!=null)tag.put("Core",NbtUtils.writeBlockPos(core));}
    @Override public void load(CompoundTag tag){super.load(tag);active=tag.getBoolean("Active");identity=tag.hasUUID("Identity")?tag.getUUID("Identity"):UUID.randomUUID();core=tag.contains("Core",Tag.TAG_COMPOUND)?NbtUtils.readBlockPos(tag.getCompound("Core")):null;}
}
