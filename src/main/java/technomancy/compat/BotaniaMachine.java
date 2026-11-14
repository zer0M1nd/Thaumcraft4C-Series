package technomancy.compat;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import technomancy.*;
import vazkii.botania.api.mana.ManaPool;
import vazkii.botania.api.mana.ManaReceiver;

/** Loaded only when Botania is installed. Mana bursts and spreaders use the real API. */
public class BotaniaMachine extends MachineEntity implements ManaReceiver {
    public BotaniaMachine(BlockPos p,BlockState s){super(p,s);}
    public static MachineEntity createBotania(BlockPos p,BlockState s){
        return ((MachineBlock)s.getBlock()).kind==MachineKind.MANA_FABRICATOR?new Fabricator(p,s):new BotaniaMachine(p,s);
    }
    public Level getManaReceiverLevel(){return getLevel();}
    public BlockPos getManaReceiverPos(){return getBlockPos();}
    public int getCurrentMana(){return mana;}
    public int getMaxMana(){return kind==MachineKind.BOTANICAL_PURIFIER?1000000:100000;}
    public boolean isFull(){return mana>=getMaxMana();}
    public void receiveMana(int amount){mana=(int)Math.max(0,Math.min((long)getMaxMana(),(long)mana+amount));changed();}
    public boolean canReceiveManaFromBursts(){return kind==MachineKind.HIPPIE_DYNAMO||kind==MachineKind.BOTANICAL_PURIFIER;}
    public static class Fabricator extends BotaniaMachine implements ManaPool {
        public Fabricator(BlockPos p,BlockState s){super(p,s);}
        public boolean isOutputtingPower(){return true;}
        public Optional<DyeColor> getColor(){return Optional.empty();}
        public void setColor(Optional<DyeColor> color){}
    }
}
