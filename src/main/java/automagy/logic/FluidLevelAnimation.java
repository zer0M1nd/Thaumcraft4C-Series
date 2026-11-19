package automagy.logic;

/** Client presentation state, independent of frame rate and never saved to gameplay NBT. */
public class FluidLevelAnimation {
 private double time=Double.NaN;
 private float height;
 public float sample(double tick,float target){
  target=Math.max(0,Math.min(1,target));
  if(Double.isNaN(time)||tick<time)height=target;
  else height+=(target-height)*(float)(1-Math.pow(.75,Math.min(20,tick-time)));
  time=tick;return height;
 }
}
