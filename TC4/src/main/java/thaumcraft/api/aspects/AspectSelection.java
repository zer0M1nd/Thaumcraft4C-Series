package thaumcraft.api.aspects;

import java.util.*;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectCatalog;
import thaumcraft.api.util.ExtensionRegistry;

/** Semantic primal sets. Registry membership and per-player presentation are independent. */
public class AspectSelection {
    public static final String FOCUS_COST="thaumcraft:focus_cost", STRUCTURE_COST="thaumcraft:structure_cost",
        GATEWAY_COST="thaumcraft:gateway_cost", WAND_ASSEMBLY_COST="thaumcraft:wand_assembly_cost",
        INITIAL_KNOWLEDGE="thaumcraft:initial_knowledge", KNOWLEDGE_GRANT="thaumcraft:knowledge_grant",
        NODE_MANIPULATION="thaumcraft:node_manipulation", LEGACY_SIX="thaumcraft:legacy_six";
    private static final Set<String> SIX=Set.of("aer","terra","ignis","aqua","ordo","perditio");
    @FunctionalInterface public interface Rule { boolean include(String use, Aspect aspect); }
    private static final ExtensionRegistry<Rule> RULES=new ExtensionRegistry<>();
    private AspectSelection() {}
    public static void register(String id, Rule rule) { RULES.register(id,0,rule); }
    public static List<Aspect> select(AspectCatalog catalog, String use) {
        Objects.requireNonNull(use);
        return catalog.primals().stream().filter(a -> includes(use,a)).toList();
    }
    public static boolean includes(String use, Aspect aspect) {
        if (LEGACY_SIX.equals(use)) return SIX.contains(aspect.tag());
        for (var rule:RULES.entries()) if (!rule.value().include(use,aspect)) return false;
        return true;
    }
}
