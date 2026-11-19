package tbmodern;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;

public final class KnowledgeFragment extends Item {
    private final String kind;
    public KnowledgeFragment(String kind) { super(new Properties()); this.kind = kind; }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer server) {
            if (kind.equals("mixed")) {
                for (Aspect aspect : Aspect.getPrimalAspects()) ThaumcraftApiHelper.addResearchPoints(server, aspect, (short)2);
            } else if (kind.equals("tainted")) {
                int awarded = 0;
                for (Aspect aspect : Aspect.getCompoundAspects()) {
                    if (aspect == Aspect.TAINT) {
                        ThaumcraftApiHelper.addResearchPoints(server, aspect, (short)8);
                        ThaumcraftApiHelper.addWarpToPlayer(server, 2, true); awarded++;
                    } else {
                        if (level.random.nextBoolean()) { ThaumcraftApiHelper.addResearchPoints(server, aspect, (short)1); awarded++; }
                        if (level.random.nextInt(24) == 0) break;
                    }
                }
                for (int i = 0; i < awarded; i++) if (level.random.nextFloat() <= .1F) {
                    if (level.random.nextFloat() <= .15F) ThaumcraftApiHelper.addWarpToPlayer(server, 1, true);
                    else ThaumcraftApiHelper.addStickyWarpToPlayer(server, 1);
                }
            } else {
                Aspect aspect = switch(kind) { case "air" -> Aspect.AIR; case "fire" -> Aspect.FIRE; case "water" -> Aspect.WATER;
                    case "earth" -> Aspect.EARTH; case "order" -> Aspect.ORDER; default -> Aspect.ENTROPY; };
                ThaumcraftApiHelper.addResearchPoints(server, aspect, (short)8);
            }
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
