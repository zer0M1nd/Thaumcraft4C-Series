package thaumcraft.api.aspects;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;

/** Optional extraction attachments can use a smelter without sharing its fuel or input inventory. */
public interface EssentiaSmelter {
    AspectAmounts smeltedAspects();
    int extractSmelted(Aspect aspect,int amount,boolean simulate);
    default int alembicLimit(){return 5;}
    default int exportInterval(){return 40;}
}
