package thaumicomniverse.compat.gregtech;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockDisplayText;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import net.minecraft.network.chat.Component;
import java.util.List;
import thaumicomniverse.RevivalFeatures;

/** The normal GT recipe transaction owns fuel consumption, progress, persistence and EU output. */
public class EssentiaGenerator extends WorkableElectricMultiblockMachine {
    public EssentiaGenerator(IMachineBlockEntity holder){super(holder);}
    @Override public boolean isRecipeLogicAvailable(){return RevivalFeatures.Group.GREGTECH.enabled()&&super.isRecipeLogicAvailable();}
    @Override public boolean beforeWorking(GTRecipe recipe){return RevivalFeatures.Group.GREGTECH.enabled()&&getMaxVoltage()>=2048&&super.beforeWorking(recipe);}
    @Override public boolean regressWhenWaiting(){return false;}
    @Override public void addDisplayText(List<Component> text){
        var display=MultiblockDisplayText.builder(text,isFormed())
                .setWorkingStatus(recipeLogic.isWorkingEnabled(),recipeLogic.isActive())
                .addEnergyProductionLine(4096,4096);
        if(recipeLogic.isWaiting()){
            display.addWorkPausedLine(false);
            text.addAll(recipeLogic.getFancyTooltip());
        }else display.addWorkingStatusLine();
        display.addProgressLine(recipeLogic.getProgress(),recipeLogic.getMaxProgress(),recipeLogic.getProgressPercent());
        text.add(Component.translatable("thaumicomniverse.gt.generator",4096));
        for(var part:getParts())if(part instanceof EssentiaHatch hatch){
            text.add(Component.translatable("thaumicomniverse.gt.hatch_contents",hatch.amount(),hatch.capacity()));
        }
    }
}
