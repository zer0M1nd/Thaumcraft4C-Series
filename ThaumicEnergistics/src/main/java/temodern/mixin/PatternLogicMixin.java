package temodern.mixin;

import appeng.parts.encoding.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import temodern.crafting.*;

@Mixin(value=PatternEncodingLogic.class,remap=false)
public abstract class PatternLogicMixin implements ArcaneEncodingLogic {
    @Shadow @Final private appeng.helpers.IPatternTerminalLogicHost host;
    @Unique private boolean teArcaneMode;
    public boolean teArcaneMode(){return teArcaneMode;}
    public void teArcaneMode(boolean value){teArcaneMode=value;((PatternEncodingLogic)(Object)this).saveChanges();}
    @Inject(method="setMode",at=@At("HEAD")) private void teClear(EncodingMode mode,CallbackInfo ci){teArcaneMode=false;}
    @Inject(method="writeToNBT",at=@At("TAIL")) private void teSave(CompoundTag tag,CallbackInfo ci){tag.putBoolean("thaumicenergisticsArcane",teArcaneMode);}
    @Inject(method="readFromNBT",at=@At("TAIL")) private void teRead(CompoundTag tag,CallbackInfo ci){teArcaneMode=tag.getBoolean("thaumicenergisticsArcane");}
    @Inject(method="loadEncodedPattern",at=@At("HEAD"),cancellable=true)
    private void teLoad(ItemStack stack,CallbackInfo ci){
        var decoded=new ArcanePattern.Decoder().decodePattern(stack,host.getLevel(),false);
        if(!(decoded instanceof ArcanePattern pattern))return;
        var logic=(PatternEncodingLogic)(Object)this;logic.setMode(EncodingMode.CRAFTING);teArcaneMode=true;
        logic.getEncodedInputInv().clear();logic.getEncodedOutputInv().clear();
        var grid=pattern.grid();for(int i=0;i<9;i++)if(!grid.getItem(i).isEmpty())logic.getEncodedInputInv().setStack(i,new appeng.api.stacks.GenericStack(appeng.api.stacks.AEItemKey.of(grid.getItem(i)),1));
        logic.saveChanges();ci.cancel();
    }
}
