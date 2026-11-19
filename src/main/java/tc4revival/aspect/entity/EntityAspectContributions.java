package tc4revival.aspect.entity;

import com.google.gson.*;
import java.util.*;
import tc4revival.aspect.*;

/** Separate additive data leaves the legacy scan identity and base selector intact. */
public class EntityAspectContributions {
    public record Entry(EntityAspectRule selector,String operation) {}
    private final List<Entry> entries;
    private final AspectCatalog catalog;
    public EntityAspectContributions(List<Entry> entries,AspectCatalog catalog) {
        this.entries=List.copyOf(entries);this.catalog=catalog;
        for(var entry:entries) {
            if(!Set.of("add","merge","replace").contains(entry.operation()))throw new IllegalArgumentException("Unknown entity aspect operation");
            entry.selector().amounts(catalog);
        }
    }
    public EntityAspectResolver.Result apply(EntityFacts facts,EntityAspectResolver.Result base) {
        var amounts=base.aspects().copy();
        for(var entry:entries)if(entry.selector().matches(facts)) {
            var extra=entry.selector().amounts(catalog);
            switch(entry.operation()) {
                case "add" -> amounts.add(extra);
                case "merge" -> amounts.merge(extra);
                case "replace" -> amounts=extra;
            }
        }
        return new EntityAspectResolver.Result(amounts,base.legacyIdentity());
    }
    public static List<Entry> decode(JsonObject root,java.util.function.Predicate<String> entityExists) {
        if(root.get("format").getAsInt()!=1)throw new IllegalArgumentException("Unsupported contribution format");
        var result=new ArrayList<Entry>();
        for(var element:root.getAsJsonArray("entries")) {
            var row=element.getAsJsonObject();var entity=row.get("entity").getAsString();
            if(!entityExists.test(entity)) {
                if(row.has("optional")&&row.get("optional").getAsBoolean())continue;
                throw new IllegalArgumentException("Unknown contributed entity "+entity);
            }
            var quantities=new ArrayList<EntityAspectRule.Quantity>();
            row.getAsJsonObject("aspects").entrySet().forEach(e->quantities.add(new EntityAspectRule.Quantity(e.getKey(),e.getValue().getAsBigDecimal().intValueExact())));
            result.add(new Entry(new EntityAspectRule(entity,entity,
                row.has("variant")?EntityAspectRule.Variant.valueOf(row.get("variant").getAsString().toUpperCase(Locale.ROOT)):EntityAspectRule.Variant.ANY,
                "",quantities),row.get("operation").getAsString()));
        }
        return result;
    }
}
