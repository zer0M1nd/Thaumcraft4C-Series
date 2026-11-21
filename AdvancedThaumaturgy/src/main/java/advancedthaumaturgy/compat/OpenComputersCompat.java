package advancedthaumaturgy.compat;

import advancedthaumaturgy.*;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.machine.*;
import li.cil.oc.api.network.*;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;

/** Optional OpenComputers CE 1.20.1 adapter; no OC classes load without that mod. */
public class OpenComputersCompat {
    public static void register(){Driver.add(new MicrolithDriver());}
    public static class MicrolithDriver implements DriverBlock {
        @Override public boolean worksWith(Level level,BlockPos pos,Direction side){
            return level.getBlockEntity(pos) instanceof MachineEntity m && m.kind().microlith()
                && m.kind()!=MachineBlock.Kind.BASE && m.kind()!=MachineBlock.Kind.LIGHTNING && m.kind()!=MachineBlock.Kind.FROST && m.kind()!=MachineBlock.Kind.EXCAVATOR;
        }
        @Override public ManagedEnvironment createEnvironment(Level level,BlockPos pos,Direction side){
            return worksWith(level,pos,side)?new Microlith((MachineEntity)level.getBlockEntity(pos)):null;
        }
    }
    public static class Microlith extends AbstractManagedEnvironment {
        private final MachineEntity machine;
        public Microlith(MachineEntity machine){this.machine=machine;setNode(Network.newNode(this,Visibility.Network).withComponent("microlith").create());}
        @Callback public Object[] toggleActive(Context context,Arguments args){if(!machine.isRemoved())machine.setActive(!machine.active());return new Object[0];}
        @Callback public Object[] getActive(Context context,Arguments args){return new Object[]{!machine.isRemoved()&&machine.active()};}
        @Callback public Object[] setActive(Context context,Arguments args){if(!machine.isRemoved())machine.setActive(args.checkBoolean(0));return new Object[0];}
        @Callback public Object[] getMicrolithType(Context context,Arguments args){return new Object[]{switch(machine.kind()){case WATCHFUL->"watchfull";case HEALING->"healing";case FIRE->"fire";case FLUX->"fluxDissapator";default->"base";}};}
    }
}
