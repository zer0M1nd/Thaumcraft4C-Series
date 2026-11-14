package technomancy;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.ItemHandlerHelper;
import thaumcraft.api.aspects.EssentiaStorageApi;
import thaumcraft.api.aspects.IEssentiaStorage;
import tc4revival.aspect.*;
import tc4revival.content.*;

/** Transfers debit the source first; a persistent escrow retains any refused remainder. */
public final class CoilLogic {
    public static void tick(MachineEntity m){
        var targetPos=m.getBlockPos().relative(m.facing());var target=MachineLogic.loaded(m,targetPos);if(target==null){signal(m,false);return;}
        if(m.kind==MachineKind.ITEM_COIL){
            var destination=target.getCapability(ForgeCapabilities.ITEM_HANDLER,m.facing().getOpposite()).resolve();if(destination.isEmpty()){signal(m,false);return;}
            boolean room=false;
            for(int slot=0;slot<destination.get().getSlots();slot++) {
                var item=destination.get().getStackInSlot(slot);
                if(item.isEmpty()||item.getCount()<Math.min(item.getMaxStackSize(),destination.get().getSlotLimit(slot))){room=true;break;}
            }
            signal(m,m.boost&&room);if(!m.runs())return;
            var escrow=ItemStack.of(m.state.getCompound("itemEscrow"));
            if(!escrow.isEmpty()){
                var remainder=ItemHandlerHelper.insertItemStacked(destination.get(),escrow,false);m.state.put("itemEscrow",remainder.save(new CompoundTag()));m.changed();
                if(!remainder.isEmpty())return;
            }
            var filter=ItemStack.of(m.state.getCompound("itemFilter"));
            for(var pos:m.links){
                if(pos.equals(targetPos)||pos.equals(m.getBlockPos()))continue;
                var source=MachineLogic.loaded(m,pos);if(source==null)continue;
                var inventory=source.getCapability(ForgeCapabilities.ITEM_HANDLER,linkFace(m,pos)).resolve();if(inventory.isEmpty())continue;
                for(int slot=0;slot<inventory.get().getSlots();slot++){
                    var offered=inventory.get().extractItem(slot,64,true);
                    if(offered.isEmpty()||!filter.isEmpty()&&!ItemStack.isSameItemSameTags(filter,offered))continue;
                    int accepted=offered.getCount()-ItemHandlerHelper.insertItemStacked(destination.get(),offered,true).getCount();
                    if(accepted<=0)continue;
                    var taken=inventory.get().extractItem(slot,accepted,false);if(taken.isEmpty())continue;
                    m.state.put("itemEscrow",taken.save(new CompoundTag()));m.changed();
                    var remaining=ItemHandlerHelper.insertItemStacked(destination.get(),taken,false);m.state.put("itemEscrow",remaining.save(new CompoundTag()));m.changed();
                    if(!remaining.isEmpty())return;break;
                }
            }
        }else{
            var destination=storage(m,targetPos,m.facing().getOpposite(),false);
            signal(m,m.boost&&tc4revival.aspect.TCAspects.catalog().all().stream().anyMatch(a->destination.insert(a,1,true)>0));
            if(!m.runs())return;
            var escrowAspect=MachineLogic.aspect(m.state.getString("escrowAspect"));int escrow=m.state.getInt("essentiaEscrow");
            if(escrow>0&&escrowAspect!=null){int moved=destination.insert(escrowAspect,escrow,false);m.state.putInt("essentiaEscrow",escrow-moved);m.changed();if(moved<escrow)return;}
            for(var pos:m.links){
                if(pos.equals(targetPos)||pos.equals(m.getBlockPos())||!m.server().hasChunkAt(pos))continue;
                var source=storage(m,pos,linkFace(m,pos),true);
                for(var a:source.contents().aspects()){
                    if(m.filter!=null&&!m.filter.equals(a)||destination.insert(a,1,true)<1)continue;
                    int taken=source.extract(a,1,false);if(taken<1)continue;
                    m.state.putString("escrowAspect",a.tag());m.state.putInt("essentiaEscrow",taken);m.changed();
                    int accepted=destination.insert(a,taken,false);m.state.putInt("essentiaEscrow",taken-accepted);m.changed();if(accepted>0)effect(m,pos,a);if(accepted<taken)return;
                }
            }
        }
    }
    public static Direction linkFace(MachineEntity m,BlockPos p){var faces=m.state.getCompound("linkFaces");return faces.contains(Long.toString(p.asLong()))?Direction.from3DDataValue(faces.getInt(Long.toString(p.asLong()))):Direction.UP;}
    private static IEssentiaStorage storage(MachineEntity m,BlockPos p,Direction side,boolean inventory){
        var entity=MachineLogic.loaded(m,p);
        if(entity instanceof EssentiaTubeBlockEntity tube&&tube.flow().kind()==EssentiaTubeFlow.Kind.BUFFER)return new IEssentiaStorage(){
            public AspectAmounts contents(){return tube.flow().contents();}
            public int totalCapacity(){return 8;}
            public int capacity(Aspect a){return tube.canInput(side)?contents().amount(a)+Math.max(0,8-(int)contents().total()):contents().amount(a);}
            public int insert(Aspect a,int n,boolean simulate){int accepted=tube.canInput(side)?Math.min(n,Math.max(0,8-(int)contents().total())):0;if(simulate)return accepted;int moved=0;while(moved<accepted&&tube.addEssentia(a,1,side)==1)moved++;return moved;}
            public int extract(Aspect a,int n,boolean simulate){return simulate?tube.canOutput(side)?Math.min(n,contents().amount(a)):0:tube.takeEssentia(a,n,side);}
        };
        // Linked sources are inventories in the original; the adjacent output is a pipe face.
        if(!inventory&&entity instanceof EssentiaTransport t&&!t.canInput(side))return EssentiaStorageApi.empty();
        return inventory?EssentiaStorageApi.atInventory(m.server(),p):EssentiaStorageApi.at(m.server(),p,side);
    }
    public static int supply(MachineEntity m,Aspect a,int n,Direction face){
        if(a==null||n<=0||face!=m.facing()||!m.runs()||m.filter!=null&&!m.filter.equals(a))return 0;
        int moved=0;
        for(var p:m.links){
            if(p.equals(m.getBlockPos())||p.equals(m.getBlockPos().relative(m.facing())))continue;
            var source=storage(m,p,linkFace(m,p),true);int taken=source.extract(a,n-moved,false);
            if(taken>0){moved+=taken;effect(m,p,a);}if(moved>=n)break;
        }return moved;
    }
    private static void effect(MachineEntity m,BlockPos from,Aspect a){
        if(!TechConfig.FANCY.get()||m.ticks%4!=0||from.distSqr(m.getBlockPos())>128*128)return;
        var start=net.minecraft.world.phys.Vec3.atCenterOf(from);var end=net.minecraft.world.phys.Vec3.atCenterOf(m.getBlockPos()).add(0,.4,0);
        int color=a.color();var particle=new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f),.6f);
        for(int i=0;i<=8;i++){var p=start.lerp(end,i/8d);m.server().sendParticles(particle,p.x,p.y,p.z,1,0,0,0,0);}
    }
    public static void signal(MachineEntity m,boolean value){
        if(m.state.getBoolean("coilSignal")==value)return;
        m.state.putBoolean("coilSignal",value);m.changed();
        m.getLevel().updateNeighborsAt(m.getBlockPos(),m.getBlockState().getBlock());
    }
    private CoilLogic(){}
}
