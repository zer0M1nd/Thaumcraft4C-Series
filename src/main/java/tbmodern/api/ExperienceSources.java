package tbmodern.api;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.ModList;
public class ExperienceSources {
 public static int extract(BlockEntity tile,int maximum){
  if(tile==null||tile.isRemoved()||tile.getLevel()==null||tile.getLevel().isClientSide||maximum<=0)return 0;
  int result=tile instanceof ExperienceSource source?source.extractExperience(maximum)
   :ModList.get().isLoaded("automagy")?tbmodern.compat.AutomagyExperience.extract(tile,maximum):0;
  result=Math.min(maximum,Math.max(0,result));
  return result+ExperienceFluids.extract(tile,maximum-result);
 }
}
