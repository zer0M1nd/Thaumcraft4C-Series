package tbmodern;

import java.util.HashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.content.*;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.nodes.NodeApi;

/** Permanent transformations use current base properties, never a cached effective snapshot. */
public final class NodeManipulator extends BlockEntity {
    private String mode = "", nodeIdentity = "", transition = "";
    private int work;
    private final HashMap<Aspect, Integer> previous = new HashMap<>();
    public NodeManipulator(BlockPos pos, BlockState state) { super(Content.MANIPULATOR.get(), pos, state); }
    public String mode() { return mode; }
    public void setMode(String mode) {
        if (!mode.isEmpty() && !Content.NODE_MODES.contains(mode)) throw new IllegalArgumentException("Unknown focus " + mode);
        this.mode = mode; reset(); sync();
    }
    private void reset() { work = 0; transition = ""; previous.clear(); }
    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    public static void tick(Level level, BlockPos pos, BlockState state, NodeManipulator device) {
        if ((level.getGameTime() + 7) % 2 != 0) return;
        if (device.mode.isEmpty() || !(level.getBlockEntity(pos.below()) instanceof AuraNodeBlockEntity node)) {
            if (device.work != 0 || !device.previous.isEmpty()) { device.reset(); device.sync(); }
            return;
        }
        if (!device.nodeIdentity.equals(node.nodeId())) {
            device.nodeIdentity = node.nodeId(); device.reset(); device.sync();
        }
        if((device.work>0 || device.mode.equals("efficiency") || device.mode.equals("speed")) && level.getGameTime()%4==1)
            WorkEffects.manipulator(level,pos,device.mode);
        if (device.mode.equals("efficiency")) {
            var charge = NodeApi.snapshot(node).charge();
            for (Aspect aspect : NodeApi.snapshot(node).capacity().getAspects()) {
                int current = charge.getAmount(aspect), last = device.previous.getOrDefault(aspect, current);
                if (current < last && level.random.nextBoolean()) NodeApi.insert(node, aspect, last - current, false);
                device.previous.put(aspect, NodeApi.snapshot(node).charge().getAmount(aspect));
            }
            return;
        }
        if ((level.getGameTime() + 7) % 20 != 0) return;
        if (device.mode.equals("speed")) {
            if (level.random.nextInt(5) == 1) for (Aspect aspect : NodeApi.getBase(node).getAspects()) NodeApi.insert(node, aspect, 1, false);
            return;
        }
        var type = node.permanentType();
        var modifier = node.permanentModifier();
        Transform target = transformation(device.mode, type, modifier);
        if (target == null) { if (device.work > 0) { device.reset(); device.sync(); } return; }
        String key = device.mode + ":" + type + ":" + modifier;
        if (!key.equals(device.transition)) { device.work = 0; device.transition = key; }
        device.work += 20;
        if (device.mode.equals("destruction") && device.work % 200 == 0) {
            var aspects = NodeApi.snapshot(node).charge().getAspects();
            if (aspects.length > 0) level.addFreshEntity(new AspectOrbEntity(level, pos.getX() + .5, pos.getY() - .5,
                pos.getZ() + .5, aspects[level.random.nextInt(aspects.length)].unwrap(), 1));
        }
        if (device.work >= target.ticks()) {
            if (target.destroyNode()) level.removeBlock(node.getBlockPos(), false);
            else NodeApi.edit(node, NodeApi.getBase(node), target.type(), target.modifier());
            device.reset();
        }
        device.sync();
    }
    public record Transform(AuraNodeType type, AuraNodeModifier modifier, int ticks, boolean destroyNode) {}
    public static Transform transformation(String mode, AuraNodeType type, AuraNodeModifier modifier) {
        return switch (mode) {
            case "brightness" -> modifier == null ? change(type, AuraNodeModifier.BRIGHT, 1200) : null;
            case "destruction" -> modifier == AuraNodeModifier.FADING ? new Transform(type, modifier, 7200, true)
                : change(type, modifier == null ? AuraNodeModifier.PALE : modifier == AuraNodeModifier.BRIGHT ? null : AuraNodeModifier.FADING,
                    modifier == null ? 120 : modifier == AuraNodeModifier.BRIGHT ? 60 : 180);
            case "hunger" -> type == AuraNodeType.NORMAL ? change(AuraNodeType.HUNGRY, modifier, 300) : null;
            case "instability" -> type == AuraNodeType.NORMAL ? change(AuraNodeType.UNSTABLE, modifier, 420) : null;
            case "purity" -> type == AuraNodeType.NORMAL ? change(AuraNodeType.PURE, modifier, 180)
                : type == AuraNodeType.TAINTED ? change(AuraNodeType.NORMAL, modifier, 1980) : null;
            case "sinister" -> type == AuraNodeType.NORMAL ? change(AuraNodeType.DARK, modifier, 360)
                : type == AuraNodeType.PURE ? change(AuraNodeType.NORMAL, modifier, 900) : null;
            case "taint" -> type == AuraNodeType.NORMAL ? change(AuraNodeType.TAINTED, modifier, 360) : null;
            case "stability" -> modifier == AuraNodeModifier.FADING ? change(type, AuraNodeModifier.PALE, 300)
                : modifier == AuraNodeModifier.PALE ? change(type, null, 600)
                : switch(type) {
                    case DARK -> change(AuraNodeType.NORMAL, modifier, 120);
                    case HUNGRY -> change(AuraNodeType.NORMAL, modifier, 30);
                    case UNSTABLE -> change(AuraNodeType.NORMAL, modifier, 210);
                    case TAINTED -> change(AuraNodeType.NORMAL, modifier, 1800);
                    default -> null;
                };
            default -> null;
        };
    }
    private static Transform change(AuraNodeType type, AuraNodeModifier modifier, int seconds) {
        return new Transform(type, modifier, seconds * 20, false);
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tag.putString("Mode", mode); tag.putString("Node", nodeIdentity);
        tag.putString("Transition", transition); tag.putInt("Work", work);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); mode = Content.NODE_MODES.contains(tag.getString("Mode")) ? tag.getString("Mode") : "";
        nodeIdentity = tag.getString("Node"); transition = tag.getString("Transition");
        work = Math.max(0, Math.min(40000, tag.getInt("Work"))); previous.clear();
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
