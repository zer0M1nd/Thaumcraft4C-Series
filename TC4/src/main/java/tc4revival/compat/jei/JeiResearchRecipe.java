package tc4revival.compat.jei;

import java.util.*;
import net.minecraft.network.chat.Component;
import tc4revival.client.ClientKnowledge;
import tc4revival.research.*;

public record JeiResearchRecipe(ResearchDefinition research, List<ResearchRequirementData.Entry> requirements, Set<String> revealed,
                                Map<String,Integer> depths) {
    public JeiResearchRecipe(ResearchDefinition research, List<ResearchRequirementData.Entry> requirements, Set<String> revealed) {
        this(research, requirements, revealed, Map.of());
    }
    public JeiResearchRecipe { requirements = List.copyOf(requirements); revealed = Set.copyOf(revealed); depths = Map.copyOf(depths); }
    public static JeiResearchRecipe create(ResearchDefinition research) {
        var rows = new ArrayList<>(tc4revival.client.ClientResearchGuidance.rows(research));
        var knowledge = ClientKnowledge.snapshot().orElse(null);
        var visible = new LinkedHashSet<String>();
        var depths = new LinkedHashMap<String,Integer>();
        var options=ResearchGuidance.options(research,knowledge,ResearchCatalogs.bundled(),tc4revival.client.ClientResearchGuidance::rows);
        for(var path:options.paths())if(rows.stream().noneMatch(row->row.requirement().equals(path.root().requirement())))rows.add(path.root());
        options.free().forEach(row->visible.add(ResearchGuidance.id(row)));
        if(ResearchGuidance.enabled())options.current(research.key(),knowledge).ifPresent(path->{
            int index=rows.indexOf(path.root());
            if(index>=0)rows.set(index,path.condition());
            else rows.add(path.condition());
            visible.add(ResearchGuidance.id(path.condition()));
            depths.put(ResearchGuidance.id(path.condition()), path.depth());
        });
        for (var row : rows) if (!ResearchGuidance.enabled() || row.complete(knowledge))
            visible.add(ResearchGuidance.id(row));
        return new JeiResearchRecipe(research, rows, visible, depths);
    }
    public boolean revealed(ResearchRequirementData.Entry row) { return revealed.contains(ResearchGuidance.id(row)); }
    public int depth(ResearchRequirementData.Entry row) { return depths.getOrDefault(ResearchGuidance.id(row), 1); }
    public Component label(ResearchRequirementData.Entry row) { return depth(row) > 1 ? row.prerequisite(depth(row)) : row.label(); }
    public String signature() {
        var key = new StringBuilder(research.toString());
        requirements.forEach(row -> { key.append(ResearchRequirements.id(row.requirement())).append(row.owner()); row.items().forEach(stack -> key.append(stack.save(new net.minecraft.nbt.CompoundTag()))); key.append(row.scanIdentities()); });
        key.append(new TreeSet<>(revealed));
        key.append(new TreeMap<>(depths));
        return key.toString();
    }
}
