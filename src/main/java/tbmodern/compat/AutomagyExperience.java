package tbmodern.compat;
import net.minecraft.world.level.block.entity.BlockEntity;
/** Loaded only when Automagy is installed. Its public API atomically drains and syncs the jar. */
public class AutomagyExperience {
 public static int extract(BlockEntity tile,int maximum){
  return tile instanceof automagy.content.ExperienceJarBlockEntity jar?jar.extractExperience(maximum,false):0;
 }
}
