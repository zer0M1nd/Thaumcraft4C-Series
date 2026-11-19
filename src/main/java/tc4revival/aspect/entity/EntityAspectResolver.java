package tc4revival.aspect.entity;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectCatalog;

public  class EntityAspectResolver {
    public record Result(AspectAmounts aspects, String legacyIdentity) {
        public int scanHash() { return legacyIdentity.hashCode(); }
    }
    private final List<EntityAspectRule> rules;
    private final AspectCatalog catalog;
    private final EntityAspectContributions contributions;

    public EntityAspectResolver(List<EntityAspectRule> rules, AspectCatalog catalog) {
        this(rules,catalog,new EntityAspectContributions(List.of(),catalog));
    }
    public EntityAspectResolver(List<EntityAspectRule> rules, AspectCatalog catalog, EntityAspectContributions contributions) {
        this.contributions=contributions;
        this.rules = List.copyOf(rules);
        this.catalog = catalog;
        var identities = new HashSet<String>();
        for (var rule : rules) {
            if (!identities.add(rule.entity() + "/" + rule.variant()+"/"+rule.golemMaterial())) throw new IllegalArgumentException("Duplicate entity aspect rule");
            rule.amounts(catalog); // reject unknown aspects before publishing a reloaded table
        }
    }

    public int ruleCount() { return rules.size(); }
    public List<EntityAspectRule> rules() { return rules; }
    public EntityAspectResolver withAdditional(List<EntityAspectRule> additional){var all=new java.util.ArrayList<>(rules);all.addAll(additional);return new EntityAspectResolver(all,catalog,contributions);}

    /** EntityList identity has no scan hash qualifiers; players had no EntityList name. */
    public Optional<String> legacyName(EntityFacts facts) {
        if (facts.playerName() != null) return Optional.empty();
        EntityAspectRule selected = null;
        for (var candidate : rules) if (candidate.matches(facts)) selected = candidate;
        return Optional.ofNullable(selected).map(EntityAspectRule::legacyName);
    }

    public Optional<Result> resolve(EntityFacts facts) {
        if (facts.playerName() != null) {
            String identity = "player_" + facts.playerName();
            return Optional.of(contribute(facts,new Result(playerAspects(facts.playerName(), identity), identity)));
        }
        EntityAspectRule selected = null;
        for (var candidate : rules) {
            if (candidate.matches(facts)) selected = candidate; // legacy conditional registrations override earlier defaults
        }
        if (selected == null) return Optional.empty();
        String identity = selected.legacyName() + selected.qualifier();
        if (facts.baby()) identity += "CHILD";
        if (facts.zombieVillager()) identity += "VILLAGER";
        if (facts.swellingCreeper()) identity += "FLASHING";
        if (facts.powered()) identity += "POWERED";
        identity+=facts.golemMaterial();
        return Optional.of(contribute(facts,new Result(selected.amounts(catalog), identity)));
    }

    public Result contribute(EntityFacts facts,Result base) { return contributions.apply(facts,base); }
    private AspectAmounts playerAspects(String name, String identity) {
        var result = new AspectAmounts().add(catalog.require("humanus"), 4);
        String special = name.equalsIgnoreCase("azanor") ? "alienis"
                : name.equalsIgnoreCase("direwolf20") ? "bestia" : name.equalsIgnoreCase("pahimar") ? "permutatio" : null;
        if (special != null) return result.add(catalog.require(special), 20);
        var random = new Random(identity.hashCode());
        var choices = catalog.all();
        for (int draw = 0; draw < 3; draw++) result.add(choices.get(random.nextInt(choices.size())), 4);
        return result;
    }
}
