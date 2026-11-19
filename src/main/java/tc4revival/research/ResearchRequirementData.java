package tc4revival.research;

import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.item.*;
import tc4revival.aspect.entity.*;

/** Identical scan identities and prerequisite labels for the server, book and recipe viewer. */
public class ResearchRequirementData {
    public record Entry(ResearchRequirements.Requirement requirement, List<ItemStack> items, List<String> scanIdentities, String entityId, String owner) {
        public Entry(ResearchRequirements.Requirement requirement, List<ItemStack> items, List<String> scanIdentities, String entityId) {
            this(requirement, items, scanIdentities, entityId, "");
        }
        public Entry { items = List.copyOf(items); scanIdentities = List.copyOf(scanIdentities); }
        public Component prerequisite(int depth) {
            return depth > 1 ? Component.translatable("thaumcraft.guidance.indirect_prerequisite", depth, label())
                    : Component.translatable("thaumcraft.guidance.prerequisite", label());
        }
        public boolean complete(PlayerKnowledge knowledge) {
            if (knowledge == null) return false;
            boolean discovered = !owner.isEmpty() && (knowledge.researchMarkers().contains(owner) || knowledge.researchMarkers().contains("@" + owner));
            return switch (requirement.kind()) {
                case RESEARCH -> knowledge.researchMarkers().contains(ResearchRequirements.canonical(requirement.key()));
                case ITEM -> discovered || scanIdentities.stream().anyMatch(identity -> knowledge.scanned(PlayerKnowledge.ScanKind.OBJECT, identity));
                case ENTITY -> discovered || scanIdentities.stream().anyMatch(identity -> knowledge.scanned(PlayerKnowledge.ScanKind.ENTITY, identity));
                // Knowing an aspect is not the same as receiving its positive reward from a scan.
                // The clue marker is the authoritative result of an aspect-triggered discovery.
                case ASPECT -> discovered;
                case WARP -> knowledge.researchMarkers().contains(requirement.key()) || knowledge.researchMarkers().contains("@" + requirement.key());
                case SPECIAL -> requirement.key().startsWith("clue/")
                        ? knowledge.researchMarkers().contains(requirement.key().substring(5)) || knowledge.researchMarkers().contains("@" + requirement.key().substring(5))
                        : knowledge.researchMarkers().contains(requirement.key());
            };
        }
        public Component label() {
            Component name;
            String kind;
            switch (requirement.kind()) {
                case RESEARCH -> { name = Component.translatable("tc.research_name." + ResearchRequirements.canonical(requirement.key())); kind = "research_name"; }
                case ITEM -> {
                    kind = "scan_item";
                    if (!items.isEmpty()) name = items.get(0).getHoverName();
                    else {
                        String id = requirement.key().equals("minecraft:portal") ? "minecraft:nether_portal" : requirement.key();
                        var block = ResourceLocation.tryParse(id);
                        name = block != null && ForgeRegistries.BLOCKS.containsKey(block)
                                ? ForgeRegistries.BLOCKS.getValue(block).getName() : Component.literal(requirement.key());
                        kind = "scan_special";
                    }
                }
                case ENTITY -> {
                    var type = ResourceLocation.tryParse(entityId);
                    var entity = type == null ? null : ForgeRegistries.ENTITY_TYPES.getValue(type);
                    name = entity == null ? Component.literal(requirement.key()) : entity.getDescription(); kind = "scan_entity";
                }
                case ASPECT -> { name = Component.literal(tc4revival.aspect.TCAspects.catalog().require(requirement.key()).displayName()); kind = "scan_aspect"; }
                case WARP -> { return Component.translatable("thaumcraft.guidance.warp", ResearchRequirements.warpThreshold(requirement.key())); }
                case SPECIAL -> {
                    if (requirement.key().equals("CRIMSON")) return Component.translatable("thaumcraft.guidance.crimson");
                    if (requirement.key().equals("ENTEROUTER")) return Component.translatable("thaumcraft.guidance.outer");
                    String key = requirement.key().startsWith("clue/") ? requirement.key().substring(5) : requirement.key();
                    return Component.translatable("thaumcraft.guidance.discover", Component.translatable("tc.research_name." + key));
                }
                default -> throw new IllegalStateException();
            }
            var label = Component.translatable("thaumcraft.jei." + kind, name);
            if (requirement.alternative()) label.append(Component.translatable(requirement.kind() == ResearchRequirements.Kind.RESEARCH
                    ? "thaumcraft.jei.any_parent" : "thaumcraft.jei.any_clue"));
            return label;
        }
    }
    public static List<Entry> resolve(ResearchDefinition research, ItemAspectAccess access, BlockScanMapping blocks, List<EntityAspectRule> entities) {
        return ResearchRequirements.collect(research).stream().map(requirement -> {
            var items = requirement.item().map(ResearchRequirementData::items).orElse(List.of());
            var entity = entities.stream().filter(r -> r.legacyName().equals(requirement.key())).map(EntityAspectRule::entity).findFirst().orElse("");
            return new Entry(requirement, items, scans(requirement, items, access, blocks, entities), entity, research.key());
        }).toList();
    }
    public static List<Entry> server(ResearchDefinition research) {
        return resolve(research, ItemAspectAccess.SERVER, BlockScanTargets.mapping(), EntityAspects.resolver().rules());
    }
    private static List<String> scans(ResearchRequirements.Requirement requirement, List<ItemStack> items, ItemAspectAccess data, BlockScanMapping blocks, List<EntityAspectRule> entities) {
        var identities = new LinkedHashSet<String>();
        if (requirement.kind() == ResearchRequirements.Kind.ITEM) {
            items.forEach(stack -> identities.add(ScanIdentity.item(stack, data)));
            var trigger = requirement.item().orElseThrow();
            for (var rule : blocks.rules()) {
                var key = rule.target().legacyKey();
                if (key != null && ResearchItemMatch.matches(trigger, key, false, trigger.nbt().isEmpty(), false))
                    identities.add(Integer.toString(data.identity().scanHash(key)));
            }
        } else if (requirement.kind() == ResearchRequirements.Kind.ENTITY) {
            for (var rule : entities) if (rule.legacyName().equals(requirement.key())) {
                // Hash identities preserve vanilla age/state and legacy conditional qualifiers.
                for (int bits = 0; bits < 16; bits++) {
                    String identity = rule.legacyName() + rule.qualifier();
                    String[] flags = {"CHILD", "VILLAGER", "FLASHING", "POWERED"};
                    for (int i = 0; i < flags.length; i++) if ((bits & 1 << i) != 0) identity += flags[i];
                    identity += rule.golemMaterial() == null ? "" : rule.golemMaterial();
                    identities.add(Integer.toString(identity.hashCode()));
                }
            }
            if (requirement.key().equals("Thaumcraft.Wisp")) for (var aspect : tc4revival.aspect.TCAspects.catalog().all())
                identities.add(Integer.toString(("Thaumcraft.WispType" + aspect.tag()).hashCode()));
        }
        return List.copyOf(identities);
    }
    /** Expand wildcard display variants and material tags, so every matching scan item is indexed. */
    private static List<ItemStack> items(ResearchDefinition.Stack trigger) {
        var stacks = new LinkedHashMap<String, ItemStack>();
        if (trigger.ore().isPresent()) {
            var tag = net.minecraft.tags.ItemTags.create(ResourceLocation.parse(ResearchClues.materialTag(trigger.ore().get())));
            var tags = ForgeRegistries.ITEMS.tags();
            if (tags != null) for (var item : tags.getTag(tag)) {
                var stack = new ItemStack(item);
                if (!stack.isEmpty()) stacks.put(stack.save(new net.minecraft.nbt.CompoundTag()).toString(), stack);
            }
        }
        var displays = ResearchDisplayItems.bundled();
        int count = displays.find(trigger).map(entry -> Math.max(1, entry.variants().size())).orElse(1);
        for (int i = 0; i < count; i++) displays.resolve(trigger, i * 1000L).ifPresent(stack -> {
            stack.setCount(1);
            if (stack.isDamageableItem() && trigger.key().metadata() == 32767) stack.setDamageValue(0);
            stacks.putIfAbsent(stack.save(new net.minecraft.nbt.CompoundTag()).toString(), stack);
        });
        return List.copyOf(stacks.values());
    }
}
