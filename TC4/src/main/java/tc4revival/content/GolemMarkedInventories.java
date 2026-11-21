package tc4revival.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Unique marked inventories; native handlers already include both halves of a double chest. */
public class GolemMarkedInventories {
    private GolemMarkedInventories() {}
    public static List<BlockPos> positions(GolemWorkHost host,boolean adjacent) {
        if (!(host.mob().level() instanceof ServerLevel level)) return List.of();
        var result = new LinkedHashSet<BlockPos>();
        for (var marker : host.markers().inDimension(level.dimension().location())) {
            var pos = marker.pos();
            if (GolemInventoryViews.exists(level,pos) && (!adjacent || GolemWorkRange.reaches(host,pos,4))) result.add(pos);
        }
        return List.copyOf(result);
    }
    public static List<BlockPos> withGoods(GolemWorkHost host,ItemStack stack,byte color) {
        if (!(host.mob().level() instanceof ServerLevel level)) return List.of();
        var result = new ArrayList<BlockPos>();
        for (var pos : positions(host,false)) for (var side : host.markers().faces(pos,level.dimension().location(),color)) {
            if (!GolemItemTransfer.extract(GolemInventoryViews.single(level,pos,side),stack,stack.getCount(),host.configuration(),false).isEmpty()) {
                result.add(pos); break;
            }
        }
        return result;
    }
    public static List<BlockPos> withRoom(GolemWorkHost host,ItemStack stack,byte color) {
        if (!(host.mob().level() instanceof ServerLevel level)) return List.of();
        var result = new ArrayList<BlockPos>();
        for (var pos : positions(host,false)) for (var side : host.markers().faces(pos,level.dimension().location(),color)) {
            if (!ItemStack.matches(GolemItemTransfer.insert(level,pos,side,stack,false),stack)) { result.add(pos); break; }
        }
        return result;
    }
}
