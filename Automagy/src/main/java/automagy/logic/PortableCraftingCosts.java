package automagy.logic;

/** Both payments round independently, exactly as two public wand API calls do. */
public final class PortableCraftingCosts {
 public static int centivis(int recipeVis,int focusCentivis,float craftingModifier,float focusModifier){
  return Math.addExact(discount(Math.multiplyExact(recipeVis,100),craftingModifier),discount(focusCentivis,focusModifier));
 }
 private static int discount(int raw,float modifier){
  if(raw<0||!Float.isFinite(modifier)||modifier<.1f)throw new IllegalArgumentException("Invalid vis cost");
  float amount=raw*modifier;
  if(!Float.isFinite(amount)||amount>=2147483648f)throw new IllegalArgumentException("Vis cost overflow");
  return (int)amount;
 }
 private PortableCraftingCosts(){}
}
