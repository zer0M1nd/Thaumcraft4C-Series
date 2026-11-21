package tc4revival.content;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The old metadata variants become separate registered items in 1.20.1. */
public record WandComponent(String path, String legacyItem, int metadata) {
    private static final List<WandComponent> ALL = create();
    public static List<WandComponent> all() { return ALL; }
    public static String rodPath(WandParts.Rod rod) {
        return (rod.staff() ? "staff_rod_" : "wand_rod_") + rod.tag.replace("_staff", "");
    }
    public static String capPath(WandParts.Cap cap) { return "wand_cap_" + cap.tag; }
    private static List<WandComponent> create() {
        var result = new ArrayList<WandComponent>();
        // Inert caps are ingredients, not usable casting caps.
        for (var cap : List.of(WandParts.Cap.IRON, WandParts.Cap.GOLD, WandParts.Cap.COPPER, WandParts.Cap.SILVER, WandParts.Cap.THAUMIUM))
            result.add(new WandComponent(capPath(cap), "WandCap", cap.metadata));
        result.add(new WandComponent("wand_cap_silver_inert", "WandCap", 5));
        result.add(new WandComponent("wand_cap_thaumium_inert", "WandCap", 6));
        result.add(new WandComponent(capPath(WandParts.Cap.VOID), "WandCap", 7));
        result.add(new WandComponent("wand_cap_void_inert", "WandCap", 8));
        java.util.Arrays.stream(WandParts.Rod.values()).filter(rod -> rod != WandParts.Rod.WOOD && !rod.tag.contains(":"))
                .sorted(Comparator.comparingInt(rod -> rod.metadata))
                .forEach(rod -> result.add(new WandComponent(rodPath(rod), "WandRod", rod.metadata)));
        return List.copyOf(result);
    }
}
