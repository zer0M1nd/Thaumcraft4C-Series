package tc4revival.aspect.entity;

import java.util.List;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectCatalog;

public record EntityAspectRule(String entity, String legacyName, Variant variant, String qualifier, List<Quantity> aspects,String golemMaterial) {
    public EntityAspectRule(String entity,String legacyName,Variant variant,String qualifier,List<Quantity> aspects){this(entity,legacyName,variant,qualifier,aspects,null);}
    public enum Variant { ANY, POWERED, OAK_BOAT, PECH_FORAGER, PECH_MAGE, PECH_ARCHER }
    public record Quantity(String aspect, int amount) {}

    public EntityAspectRule {
        if (!entity.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) throw new IllegalArgumentException("Invalid entity ID " + entity);
        if (legacyName.isEmpty()) throw new IllegalArgumentException("Empty legacy entity identity");
        aspects = List.copyOf(aspects);
        if (aspects.isEmpty()) throw new IllegalArgumentException("Empty entity aspect rule");
        for (Quantity quantity : aspects) {
            if (quantity.amount() < 0 || quantity.amount() > 10000) throw new IllegalArgumentException("Invalid entity aspect quantity");
        }
    }

    public AspectAmounts amounts(AspectCatalog catalog) {
        var result = new AspectAmounts();
        aspects.forEach(quantity -> result.add(catalog.require(quantity.aspect()), quantity.amount()));
        return result;
    }

    public boolean matches(EntityFacts facts) {
        return entity.equals(facts.type()) && (golemMaterial==null||golemMaterial.equals(facts.golemMaterial())) && switch (variant) {
            case ANY -> true;
            case POWERED -> facts.powered();
            case OAK_BOAT -> facts.oakBoat();
            case PECH_FORAGER -> facts.pechType()==0;
            case PECH_MAGE -> facts.pechType()==1;
            case PECH_ARCHER -> facts.pechType()==2;
        };
    }
}
