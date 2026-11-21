package ttmodern;

import java.util.*;
import dan200.computercraft.api.lua.*;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;
import tc4revival.content.*;

/** Read-only machine access, except for the ear's existing pitch-change operation. */
public final class MachinePeripherals {
    private static final Map<BlockEntity,LazyOptional<IPeripheral>> handles=new IdentityHashMap<>();
    public static void setup(){net.minecraftforge.common.MinecraftForge.EVENT_BUS.addGenericListener(BlockEntity.class,MachinePeripherals::attach);}
    private static void attach(net.minecraftforge.event.AttachCapabilitiesEvent<BlockEntity> event){event.addListener(()->{LazyOptional<IPeripheral> handle; synchronized(handles){handle=handles.remove(event.getObject());}if(handle!=null)handle.invalidate();});}
    public static LazyOptional<IPeripheral> lookup(BlockEntity entity){
        if(entity==null||entity.isRemoved())return LazyOptional.empty();
        synchronized(handles){var cached=handles.get(entity);if(cached!=null)return cached;}
        IPeripheral peripheral=entity instanceof TabletEntity tablet?new Tablet(tablet):entity instanceof MagnetEntity magnet?(magnet.mob()?new MobMagnet(magnet):new Magnet(magnet)):entity instanceof BrainJarBlockEntity jar?new Brain(jar):entity instanceof DeconstructionTableBlockEntity table?new Deconstructor(table):entity instanceof ArcaneEarBlockEntity ear?new Ear(ear):entity instanceof ArcaneBoreBlockEntity bore?new Bore(bore):entity instanceof EssentiaTransport&&!(entity instanceof EssentiaSource)&&!(entity instanceof EssentiaVessel)?new Transport(entity):entity instanceof RevealedAspectContainer?new Container(entity):entity instanceof EssentiaTransport?new Transport(entity):null;
        if(peripheral==null)return LazyOptional.empty();var result=LazyOptional.of(()->peripheral);synchronized(handles){handles.put(entity,result);}return result;
    }
    public abstract static class Machine implements IPeripheral {
        protected final BlockEntity entity;private final String type;
        Machine(BlockEntity entity,String type){this.entity=entity;this.type=type;}
        protected void check() throws LuaException {if(entity.isRemoved()||entity.getLevel()==null||!entity.getLevel().hasChunkAt(entity.getBlockPos())||entity.getLevel().getBlockEntity(entity.getBlockPos())!=entity)throw new LuaException("Machine is no longer available");}
        @Override public String getType(){return type;}
        @Override public Object getTarget(){return entity;}
        @Override public boolean equals(IPeripheral other){return other instanceof Machine machine&&machine.entity==entity&&machine.type.equals(type);}
    }
    public static final class Container extends Machine {
        Container(BlockEntity entity){super(entity,"tt_aspectContainer");}
        @LuaFunction(mainThread=true) public Map<Integer,String> getAspects() throws LuaException {check();var result=new LinkedHashMap<Integer,String>();if(entity instanceof EssentiaJarBlockEntity jar&&jar.filter()!=null){result.put(1,jar.filter().tag());return result;}var aspects=((RevealedAspectContainer)entity).revealedAspects();if(aspects!=null)for(var tag:aspects.aspects().stream().map(tc4revival.aspect.Aspect::tag).sorted().toList())result.put(result.size()+1,tag);return result;}
        @LuaFunction(mainThread=true) public int getAspectCount(String tag) throws LuaException {check();var values=((RevealedAspectContainer)entity).revealedAspects();return values==null?0:values.view().entrySet().stream().filter(entry->entry.getKey().tag().equals(tag)).mapToInt(Map.Entry::getValue).sum();}
    }
    public static final class Transport extends Machine {
        Transport(BlockEntity entity){super(entity,"tt_aspectTransport");}
        private EssentiaTransport pipe() throws LuaException {check();return (EssentiaTransport)entity;}
        private Direction face(int side) throws LuaException {if(side<0||side>5)throw new LuaException("Side must be in 0..5");return Direction.from3DDataValue(side);}
        @LuaFunction(mainThread=true) public boolean isConnectable(int side) throws LuaException {return pipe().connectable(face(side));}
        @LuaFunction(mainThread=true) public boolean canInputFrom(int side) throws LuaException {return pipe().canInput(face(side));}
        @LuaFunction(mainThread=true) public boolean canOutputTo(int side) throws LuaException {return pipe().canOutput(face(side));}
        @LuaFunction(mainThread=true) public String getSuctionType(int side) throws LuaException {var aspect=pipe().suctionType(face(side));return aspect==null?null:aspect.tag();}
        @LuaFunction(mainThread=true) public int getSuctionAmount(int side) throws LuaException {return pipe().suction(face(side));}
        @LuaFunction(mainThread=true) public String getEssentiaType(int side) throws LuaException {var aspect=pipe().essentiaType(face(side));return aspect==null?null:aspect.tag();}
        @LuaFunction(mainThread=true) public int getEssentiaAmount(int side) throws LuaException {return pipe().essentiaAmount(face(side));}
        @LuaFunction(mainThread=true) public int getMinimumSuction() throws LuaException {return pipe().minimumSuction();}
    }
    public static final class Tablet extends Machine {
        Tablet(TabletEntity entity){super(entity,"tt_animationTablet");}
        private TabletEntity tablet() throws LuaException {check();return (TabletEntity)entity;}
        @LuaFunction(mainThread=true) public boolean getRedstone() throws LuaException {return tablet().redstone();}
        @LuaFunction(mainThread=true) public void setRedstone(boolean value) throws LuaException {var tablet=tablet();tablet.configure(tablet.leftClick(),value);}
        @LuaFunction(mainThread=true) public boolean getLeftClick() throws LuaException {return tablet().leftClick();}
        @LuaFunction(mainThread=true) public void setLeftClick(boolean value) throws LuaException {var tablet=tablet();tablet.configure(value,tablet.redstone());}
        @LuaFunction(mainThread=true) public int getRotation() throws LuaException {return tablet().getBlockState().getValue(TabletBlock.FACING).get3DDataValue()-2;}
        @LuaFunction(mainThread=true) public void setRotation(int value) throws LuaException {if(value<0||value>3)throw new LuaException("Rotation must be in 0..3");tablet().rotation(Direction.from3DDataValue(value+2));}
        @LuaFunction(mainThread=true) public boolean hasItem() throws LuaException {return !tablet().items.getStackInSlot(0).isEmpty();}
        @LuaFunction(mainThread=true) public boolean trigger() throws LuaException {return tablet().trigger();}
    }
    public static class Magnet extends Machine {
        Magnet(MagnetEntity entity){super(entity,"tt_magnet");}
        @LuaFunction(mainThread=true) public boolean isPulling() throws LuaException {check();return ((MagnetEntity)entity).pulling();}
        @LuaFunction(mainThread=true) public void setPulling(boolean pulling) throws LuaException {check();((MagnetEntity)entity).pulling(pulling);}
        @LuaFunction(mainThread=true) public int getSignal() throws LuaException {check();return ((MagnetEntity)entity).signal();}
    }
    public static final class MobMagnet extends Magnet {
        MobMagnet(MagnetEntity entity){super(entity);}
        @LuaFunction(mainThread=true) public boolean getAdultSearch() throws LuaException {check();return ((MagnetEntity)entity).adult();}
        @LuaFunction(mainThread=true) public void setAdultSearch(boolean adult) throws LuaException {check();((MagnetEntity)entity).adult(adult);}
    }
    public static final class Brain extends Machine {
        Brain(BrainJarBlockEntity entity){super(entity,"tt_braininajar");}
        @LuaFunction(mainThread=true) public int getXP() throws LuaException {check();return ((BrainJarBlockEntity)entity).experience();}
    }
    public static final class Deconstructor extends Machine {
        Deconstructor(DeconstructionTableBlockEntity entity){super(entity,"tt_deconstructor");}
        @LuaFunction(mainThread=true) public boolean hasItem() throws LuaException {check();return !((DeconstructionTableBlockEntity)entity).getItem(0).isEmpty();}
        @LuaFunction(mainThread=true) public boolean hasAspect() throws LuaException {check();return ((DeconstructionTableBlockEntity)entity).pending()!=null;}
        @LuaFunction(mainThread=true) public String getAspect() throws LuaException {check();var aspect=((DeconstructionTableBlockEntity)entity).pending();return aspect==null?null:aspect.tag();}
    }
    public static final class Ear extends Machine {
        Ear(ArcaneEarBlockEntity entity){super(entity,"tt_arcaneear");}
        @LuaFunction(mainThread=true) public int getNote() throws LuaException {check();return ((ArcaneEarBlockEntity)entity).note();}
        @LuaFunction(mainThread=true) public void setNote(int note) throws LuaException {check();if(note<0||note>24)throw new LuaException("Note must be in 0..24");var ear=(ArcaneEarBlockEntity)entity;int steps=(note-ear.note()+25)%25;for(int i=0;i<steps;i++)ear.changePitch();}
    }
    public static final class Bore extends Machine {
        Bore(ArcaneBoreBlockEntity entity){super(entity,"tt_arcanebore");}
        private ArcaneBoreEquipment equipment() throws LuaException {check();var bore=(ArcaneBoreBlockEntity)entity;return ArcaneBoreEquipment.inspect(bore.focus(),bore.pick());}
        @LuaFunction(mainThread=true) public boolean hasPickaxe() throws LuaException {return equipment().pickPresent();}
        @LuaFunction(mainThread=true) public boolean hasFocus() throws LuaException {return equipment().focusPresent();}
        @LuaFunction(mainThread=true) public boolean isPickaxeBroken() throws LuaException {check();var pick=((ArcaneBoreBlockEntity)entity).pick();return pick.isDamageableItem()&&pick.getDamageValue()+1>=pick.getMaxDamage();}
        @LuaFunction(mainThread=true) public boolean isWorking() throws LuaException {var equipment=equipment();var bore=(ArcaneBoreBlockEntity)entity;var base=bore.base();return base!=null&&equipment.canDig(bore.pick())&&(bore.getLevel().hasNeighborSignal(bore.getBlockPos())||bore.getLevel().hasNeighborSignal(base.pos()));}
        @LuaFunction(mainThread=true) public int getRadius() throws LuaException {return equipment().width();}
        @LuaFunction(mainThread=true) public int getSpeed() throws LuaException {return equipment().speed();}
        @LuaFunction(mainThread=true) public boolean hasNativeClusters() throws LuaException {return equipment().dowsing();}
        @LuaFunction(mainThread=true) public int getFortune() throws LuaException {return equipment().fortune();}
        @LuaFunction(mainThread=true) public boolean hasSilkTouch() throws LuaException {return equipment().silk();}
    }
}
