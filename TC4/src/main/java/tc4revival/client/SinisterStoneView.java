package tc4revival.client;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import tc4revival.content.SinisterNodeMemory;
public  class SinisterStoneView {
    private static final SinisterNodeMemory NODES=new SinisterNodeMemory();
    private static boolean active;
    private SinisterStoneView() {}
    public static void observe(Level level,BlockPos pos) { NODES.observe(level.dimension().location(),pos,System.currentTimeMillis()); }
    public static void update(Level level,Entity entity) { active=NODES.active(level.dimension().location(),entity.getEyePosition(),entity.getLookAngle(),System.currentTimeMillis()); }
    public static float model(net.minecraft.world.item.ItemStack stack) { return tc4revival.content.SinisterStoneItem.legacyMetadata(stack)==1||active?1:0; }
}
