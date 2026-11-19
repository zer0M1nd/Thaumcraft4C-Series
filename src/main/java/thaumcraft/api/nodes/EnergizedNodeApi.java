package thaumcraft.api.nodes;

import java.util.UUID;
import tc4revival.content.EnergizedNodeBlockEntity;
import thaumcraft.api.aspects.AspectList;

/** Additive aura leases for energized nodes; device identities are stable instance UUIDs. */
public class EnergizedNodeApi {
    public static AspectList permanentAura(EnergizedNodeBlockEntity node) { return AspectList.from(node.aura()); }
    public static AspectList effectiveAura(EnergizedNodeBlockEntity node) { return AspectList.from(node.effectiveAura()); }
    public static int activeAmplifiers(EnergizedNodeBlockEntity node) { return node.activeAmplifierCount(); }
    public static boolean amplify(EnergizedNodeBlockEntity node, UUID device, AspectList bonus, int lifetimeTicks) {
        return node.amplify(device, bonus.unwrap(), lifetimeTicks);
    }
    public static boolean remove(EnergizedNodeBlockEntity node, UUID device) { return node.removeAmplifier(device); }
    public static void clear(EnergizedNodeBlockEntity node) { node.clearAmplifiers(); }
}
