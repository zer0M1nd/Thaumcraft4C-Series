package advancedthaumaturgy.compat;

import advancedthaumaturgy.MachineBlock;
import advancedthaumaturgy.MachineEntity;
import dan200.computercraft.api.ForgeComputerCraftAPI;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraftforge.common.util.LazyOptional;

/** AT2-30r exposed the Watchful Eye under the microlith peripheral type. */
public class ComputerCraftCompat {
    public static void register(){
        ForgeComputerCraftAPI.registerPeripheralProvider((level,pos,side)->{
            if(level.getBlockEntity(pos) instanceof MachineEntity machine && machine.kind()==MachineBlock.Kind.WATCHFUL){
                var optional=LazyOptional.<IPeripheral>of(()->new Microlith(machine));
                machine.addPeripheralInvalidation(optional::invalidate);
                return optional;
            }
            return LazyOptional.empty();
        });
    }
    public static class Microlith implements IPeripheral {
        private final MachineEntity machine;
        public Microlith(MachineEntity machine){this.machine=machine;}
        @Override public String getType(){return "microlith";}
        @Override public Object getTarget(){return machine;}
        @Override public boolean equals(IPeripheral other){return other instanceof Microlith m&&m.machine==machine;}
        @LuaFunction(mainThread=true) public void toggleActive(){if(!machine.isRemoved())machine.setActive(!machine.active());}
        @LuaFunction(mainThread=true) public boolean getActive(){return !machine.isRemoved()&&machine.active();}
        @LuaFunction(mainThread=true) public void setActive(boolean active){if(!machine.isRemoved())machine.setActive(active);}
        // Retain the spelling used in existing AT scripts.
        @LuaFunction public String getMicrolithType(){return "watchfull";}
    }
}
