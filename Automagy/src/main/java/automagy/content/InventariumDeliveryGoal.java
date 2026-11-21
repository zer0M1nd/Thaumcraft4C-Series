package automagy.content;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.golems.GolemApi;
import tc4revival.content.GolemEntity;

/** A physical courier: item transfers happen only after navigation reaches the loaded endpoint. */
public final class InventariumDeliveryGoal extends Goal {
    public static final ResourceLocation ID=ResourceLocation.parse("automagy:inventarium_delivery");
    private final GolemEntity golem;
    private int pathRetry,travelTicks;
    public InventariumDeliveryGoal(GolemEntity golem){this.golem=golem;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    public static void register(){GolemApi.registerTask(ID,(golem,work,targets)->work.addGoal(1,new InventariumDeliveryGoal(golem)));}
    /** The dispatcher has already reserved the request; call only on the server thread. */
    public static boolean assign(GolemEntity golem,InventariumBlockEntity core,long claim,BlockPos source,Direction side,int slots){
        if(golem.level().isClientSide||core.getLevel()!=golem.level()||!golem.carried().isEmpty()||!golem.configuration().addonData(ID).isEmpty())return false;
        var reservation=core.requests().claims().stream().filter(c->c.id()==claim).findFirst();if(reservation.isEmpty())return false;
        var job=reservation.get();if(!job.destination().dimension().equals(golem.level().dimension()))return false;
        var tag=new CompoundTag();tag.putString("Dimension",golem.level().dimension().location().toString());tag.putUUID("Book",core.requests().identity());tag.putLong("Claim",claim);
        tag.put("Core",NbtUtils.writeBlockPos(core.getBlockPos()));tag.put("Source",NbtUtils.writeBlockPos(source));
        tag.put("Requester",NbtUtils.writeBlockPos(job.destination().pos()));tag.put("Destination",NbtUtils.writeBlockPos(golem.homeInventory()));tag.putInt("DestinationSide",golem.homeFacing().get3DDataValue());
        tag.put("Item",job.item().stack().save(new CompoundTag()));tag.putInt("Side",side==null?-1:side.get3DDataValue());tag.putString("Stage","pickup");
        tag.putInt("Slots",Math.max(0,slots));
        if(!core.requests().bind(claim,golem.getUUID()))return false;
        GolemApi.data(golem,ID,tag);GolemApi.configure(golem,golem.configuration().customMaterial(),ID,golem.configuration().blockAppearance());return true;
    }
    @Override public boolean canUse(){return !golem.level().isClientSide&&golem.state().workEnabled()&&!golem.configuration().addonData(ID).isEmpty();}
    @Override public void start(){travelTicks=golem.configuration().addonData(ID).getInt("Wait");pathRetry=0;}
    @Override public boolean canContinueToUse(){return canUse();}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public void stop(){golem.getNavigation().stop();}
    private void save(CompoundTag tag){GolemApi.data(golem,ID,tag);}
    private void clear(){save(new CompoundTag());golem.getNavigation().stop();CourierLifecycle.completed(golem);}
    private void returning(CompoundTag job,InventariumBlockEntity core){
        if(core!=null)core.requests().fail(job.getLong("Claim"));job.putString("Stage","return");job.putInt("Wait",0);travelTicks=0;save(job);golem.getNavigation().stop();
    }
    @Override public void tick(){
        var level=golem.level();var job=golem.configuration().addonData(ID);if(job.isEmpty())return;
        BlockPos corePos=NbtUtils.readBlockPos(job.getCompound("Core"));
        if(!level.hasChunkAt(corePos))return;
        InventariumBlockEntity core=level.getBlockEntity(corePos) instanceof InventariumBlockEntity found&&job.hasUUID("Book")&&found.requests().identity().equals(job.getUUID("Book"))?found:null;
        if(core!=null&&!core.requests().owned(job.getLong("Claim"),golem.getUUID()))core=null;
        String stage=job.getString("Stage");ItemStack item=ItemStack.of(job.getCompound("Item"));
        if(item.isEmpty()){if(core!=null)core.requests().fail(job.getLong("Claim"));clear();return;}
        if(!stage.equals("return")&&(core==null||core.requests().deliveryLimit(job.getLong("Claim"))==0)){returning(job,core);stage="return";}
        if(stage.equals("return")&&golem.carried().isEmpty()){clear();return;}
        if(!golem.carried().isEmpty()&&!ItemStack.isSameItemSameTags(item,golem.carried())){if(core!=null)core.requests().fail(job.getLong("Claim"));clear();return;}
        BlockPos target=NbtUtils.readBlockPos(job.getCompound(stage.equals("deliver")?"Destination":"Source"));
        if(!level.hasChunkAt(target))return;
        if(golem.distanceToSqr(target.getCenter())>4){
            if(pathRetry--<=0){pathRetry=20;var path=golem.getNavigation().createPath(target,1);if(path!=null)golem.getNavigation().moveTo(path,golem.navigationSpeed());}
            int waited=++travelTicks;job.putInt("Wait",waited);if(waited%20==0)save(job);
            if(waited>1200){if(stage.equals("return")){golem.dropCargo();clear();}else returning(job,core);}return;
        }
        golem.getNavigation().stop();travelTicks=0;job.putInt("Wait",0);
        int side=job.getInt("Side");Direction face=stage.equals("deliver")?Direction.from3DDataValue(job.getInt("DestinationSide")):side<0?null:Direction.from3DDataValue(side);
        var inventory=stage.equals("deliver")?InventoryTransfer.at(level,target,face):InventariumContents.inventory(level,new InventariumContents.Source(target,face));var cargo=new GolemCargoInventory(golem);
        if(inventory.isEmpty()){if(stage.equals("return")){golem.dropCargo();clear();}else returning(job,core);return;}
        if(stage.equals("pickup")){
            if(InventariumContents.sameInventory(level,new InventariumContents.Source(target,face),new InventariumContents.Source(NbtUtils.readBlockPos(job.getCompound("Destination")),Direction.from3DDataValue(job.getInt("DestinationSide"))))){returning(job,core);return;}
            int allowed=core.readableSources().getOrDefault(new InventariumContents.Source(target,face),0);
            int moved=InventoryTransfer.move(new InventoryPrefix(inventory.get(),Math.min(job.getInt("Slots"),allowed)),cargo,item,core.requests().deliveryLimit(job.getLong("Claim")),ItemStack::isSameItemSameTags,stack->golem.spawnAtLocation(stack));
            if(moved==0){returning(job,core);return;}job.putString("Stage","deliver");save(job);
        }else if(stage.equals("deliver")){
            int moved;
            if(level.getBlockEntity(target) instanceof DeliveryMirrorBlockEntity mirror&&mirror.multi()){
                moved=mirror.deliverReserved(core,job.getLong("Claim"),golem.carried());
                if(moved>0)cargo.extractItem(0,moved,false);
            }else moved=InventoryTransfer.move(cargo,inventory.get(),item,core.requests().deliveryLimit(job.getLong("Claim")),ItemStack::isSameItemSameTags,stack->golem.spawnAtLocation(stack));
            core.requests().finish(job.getLong("Claim"),moved);returning(job,null);if(golem.carried().isEmpty())clear();
        }else{
            InventoryTransfer.move(cargo,inventory.get(),item,golem.carried().getCount(),ItemStack::isSameItemSameTags,stack->golem.spawnAtLocation(stack));
            if(!golem.carried().isEmpty())golem.dropCargo();clear();
        }
    }
}
