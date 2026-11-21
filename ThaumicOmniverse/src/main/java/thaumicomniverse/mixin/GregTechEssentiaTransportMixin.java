package thaumicomniverse.mixin;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import tc4revival.aspect.Aspect;
import tc4revival.content.EssentiaTransport;
import thaumicomniverse.compat.gregtech.EssentiaHatch;

@Mixin(targets="com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity",remap=false)
public abstract class GregTechEssentiaTransportMixin implements EssentiaTransport {
    @Unique private EssentiaHatch omniverse$hatch(){var machine=((IMachineBlockEntity)(Object)this).getMetaMachine();return machine instanceof EssentiaHatch hatch?hatch:null;}
    public boolean connectable(Direction side){return omniverse$hatch()!=null;}
    public boolean canInput(Direction side){var hatch=omniverse$hatch();return hatch!=null&&hatch.canInput(side);}
    public boolean canOutput(Direction side){return false;}
    public Aspect suctionType(Direction side){var hatch=omniverse$hatch();return hatch==null?null:hatch.suctionType(side);}
    public int suction(Direction side){var hatch=omniverse$hatch();return hatch==null?0:hatch.suction(side);}
    public int minimumSuction(){return 0;}
    public Aspect essentiaType(Direction side){var hatch=omniverse$hatch();return hatch==null?null:hatch.aspect();}
    public int essentiaAmount(Direction side){var hatch=omniverse$hatch();return hatch==null?0:hatch.amount();}
    public int takeEssentia(Aspect aspect,int amount,Direction side){return 0;}
    public int addEssentia(Aspect aspect,int amount,Direction side){var hatch=omniverse$hatch();return hatch==null?0:hatch.addEssentia(aspect,amount,side);}
}
