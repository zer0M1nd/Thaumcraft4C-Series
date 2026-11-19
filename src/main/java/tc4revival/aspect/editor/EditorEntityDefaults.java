package tc4revival.aspect.editor;

import java.util.*;
import java.util.function.Predicate;
import tc4revival.aspect.*;
import tc4revival.aspect.entity.*;
import tc4revival.aspect.sync.AspectDataSet;

/** Type-level previews from the server snapshot. Never instantiate arbitrary mod entities. */
public class EditorEntityDefaults {
    private final EntityAspectResolver resolver;
    private final AspectCatalog catalog;
    public EditorEntityDefaults(AspectDataSet files,AspectCatalog catalog,Predicate<String> exists){
        this.catalog=catalog;
        var rules=new ArrayList<EntityAspectRule>();
        files.directory("entity_aspects").values().forEach(json->rules.addAll(EntityAspectJson.decode(json)));
        var contributions=new ArrayList<EntityAspectContributions.Entry>();
        files.directory("entity_aspect_contributions").values().forEach(json->contributions.addAll(EntityAspectContributions.decode(json.getAsJsonObject(),exists)));
        resolver=new EntityAspectResolver(rules,catalog,new EntityAspectContributions(contributions,catalog));
    }
    public Optional<AspectAmounts> preview(String id){
        // The type editor has no player name or entity NBT. Show the invariant player base.
        if(id.equals("minecraft:player"))return Optional.of(new AspectAmounts().add(catalog.require("humanus"),4));
        var facts=new EntityFacts(id,null,false,false,false,false,true,0,"WOOD");
        var result=resolver.resolve(facts);
        if(result.isPresent())return result.map(EntityAspectResolver.Result::aspects);
        // Some addon types only register conditional variants. Preview the first
        // declared variant rather than manufacturing/spawning a live entity.
        return resolver.rules().stream().filter(r->r.entity().equals(id)).findFirst().map(r->r.amounts(catalog));
    }
    /** Synchronized observation definitions for read-only client research hints. */
    public List<EntityAspectRule> researchRules(){return resolver.rules();}
}
