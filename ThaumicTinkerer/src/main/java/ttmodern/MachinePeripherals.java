package ttmodern;

import java.util.*;
import dan200.computercraft.api.lua.*;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraft.resources.ResourceLocation;
import tc4revival.content.*;

/** Read-only machine access, except for the ear's existing pitch-change operation. */
public final class MachinePeripherals {
    private static final ResourceLocation PERIPHERAL_ID = ResourceLocation.fromNamespaceAndPath(Tinkerer.ID, "machine_peripheral");

    public static void setup() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addGenericListener(BlockEntity.class, MachinePeripherals::attach);
    }

    private static void attach(AttachCapabilitiesEvent<BlockEntity> event) {
        Machine machine = machineFor(event.getObject());
        if (machine == null) return;

        PeripheralProvider provider = new PeripheralProvider(machine);
        event.addCapability(PERIPHERAL_ID, provider);
        event.addListener(provider::invalidate);
    }

    private static Machine machineFor(BlockEntity entity) {
        if (entity instanceof TabletEntity tablet) return new Tablet(tablet);
        if (entity instanceof MagnetEntity magnet) return magnet.mob() ? new MobMagnet(magnet) : new Magnet(magnet);
        if (entity instanceof BrainJarBlockEntity jar) return new Brain(jar);
        if (entity instanceof DeconstructionTableBlockEntity table) return new Deconstructor(table);
        if (entity instanceof ArcaneEarBlockEntity ear) return new Ear(ear);
        if (entity instanceof ArcaneBoreBlockEntity bore) return new Bore(bore);
        if (entity instanceof EssentiaTransport && !(entity instanceof EssentiaSource) && !(entity instanceof EssentiaVessel))
            return new Transport(entity);
        if (entity instanceof RevealedAspectContainer) return new Container(entity);
        if (entity instanceof EssentiaTransport) return new Transport(entity);
        return null;
    }

    private static final class PeripheralProvider implements ICapabilityProvider {
        private final Machine machine;
        private LazyOptional<IPeripheral> handle = LazyOptional.empty();

        PeripheralProvider(Machine machine) {
            this.machine = machine;
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
            if (capability != ComputerIntegration.PERIPHERAL || machine.entity.isRemoved()
                    || machine.entity.getBlockState().is(ComputerIntegration.DISABLED)) return LazyOptional.empty();
            if (!handle.isPresent()) handle = LazyOptional.of(() -> machine);
            return handle.cast();
        }

        void invalidate() {
            handle.invalidate();
        }
    }

    public static LazyOptional<IPeripheral> lookup(BlockEntity entity) {
        return entity == null || entity.isRemoved() ? LazyOptional.empty() : entity.getCapability(ComputerIntegration.PERIPHERAL);
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
