package temodern.golem;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.IPartHost;
import appeng.helpers.InterfaceLogicHost;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;
import thaumcraft.api.aspects.IEssentiaStorage;
import temodern.essentia.EssentiaKey;
import temodern.essentia.EssentiaKeyType;

/** Uses the interface's real finite buffer, including a part on the marked face. */
public record InterfaceEssentia(BlockEntity tile,Direction face) implements IEssentiaStorage {
    private InterfaceLogicHost host() {
        if(tile.isRemoved()||tile.getLevel()==null||!tile.getLevel().hasChunkAt(tile.getBlockPos())
                ||tile.getLevel().getBlockEntity(tile.getBlockPos())!=tile)return null;
        if(tile instanceof InterfaceLogicHost host)return host;
        return face!=null&&tile instanceof IPartHost parts&&parts.getPart(face) instanceof InterfaceLogicHost host?host:null;
    }
    public static IEssentiaStorage find(BlockEntity tile,Direction face) {
        var access=new InterfaceEssentia(tile,face);return access.host()==null?null:access;
    }
    @Override public AspectAmounts contents() {
        var result=new AspectAmounts();var host=host();if(host==null)return result;
        for(var entry:host.getStorage().getAvailableStacks())if(entry.getKey() instanceof EssentiaKey key&&key.aspect()!=null)
            result.merge(key.aspect(),(int)Math.min(Integer.MAX_VALUE,Math.max(0,entry.getLongValue())));
        return result;
    }
    @Override public int capacity(Aspect aspect) {
        return (int)Math.min(Integer.MAX_VALUE,(long)contents().amount(aspect)+insert(aspect,Integer.MAX_VALUE,true));
    }
    @Override public int totalCapacity() {
        var host=host();if(host==null)return 0;
        long slotCapacity=Math.max(0,Math.min(Integer.MAX_VALUE,host.getStorage().getCapacity(EssentiaKeyType.INSTANCE)));
        return (int)Math.min(Integer.MAX_VALUE,slotCapacity*host.getStorage().size());
    }
    @Override public int insert(Aspect aspect,int amount,boolean simulate) { return move(aspect,amount,simulate,true); }
    @Override public int extract(Aspect aspect,int amount,boolean simulate) { return move(aspect,amount,simulate,false); }
    private int move(Aspect aspect,int amount,boolean simulate,boolean insert) {
        if(amount<0)throw new IllegalArgumentException("Negative essentia");
        var host=host();if(host==null||amount==0)return 0;
        var storage=host.getStorage();var mode=simulate?Actionable.SIMULATE:Actionable.MODULATE;
        long moved=insert?storage.insert(EssentiaKey.of(aspect),amount,mode,IActionSource.empty())
                :storage.extract(EssentiaKey.of(aspect),amount,mode,IActionSource.empty());
        if(moved>0&&!simulate)host.saveChanges();
        return (int)moved;
    }
}
