package automagy.logic;
import java.util.function.BooleanSupplier;
/** Heat is independent of the selected recipe; progress belongs to a stable recipe/carrier pair. */
public final class WorkbenchCycle {
 public record State(int progress,int heat,int idleCoolingTicks){}
 private int progress,heat,idleCoolingTicks;
 public State state(){return new State(progress,heat,idleCoolingTicks);}
 public void restore(State state){progress=Math.max(0,state.progress);heat=Math.max(-700,Math.min(12000,state.heat));idleCoolingTicks=Math.floorMod(state.idleCoolingTicks,5);}
 public void resetProgress(){progress=0;}
 public void passive(boolean hasGolem){if(heat>0)heat--;else if(heat<0&&!hasGolem&&++idleCoolingTicks>=5){heat++;idleCoolingTicks=0;}}
 /** Successful cooling consumes one fabrico within range eight; eight is not the essentia cost. */
 public boolean work(WorkbenchUpgrades upgrades,int speed,int strength,boolean stationary,BooleanSupplier cool){
  if(heat>upgrades.heatThreshold()&&cool.getAsBoolean())heat=-700;
  if(progress>=upgrades.craftTime(speed,strength,heat)){progress=0;heat=Math.min(12000,heat+10);return true;}
  if(stationary){progress++;heat=Math.min(12000,heat+5);}return false;
 }
 public boolean slowsGolem(WorkbenchUpgrades upgrades){return heat>upgrades.heatThreshold()+5;}
}
