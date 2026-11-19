package thaumicomniverse;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.*;
import tc4revival.research.PlayerKnowledge;
import thaumicomniverse.research.*;
import static org.junit.jupiter.api.Assertions.*;

class UnifiedEnchantmentsTest {
    @Test void hundredsOfEnchantmentsUseOneResearchWithStableRecipePages() throws Exception {
        var path=Path.of("src/main/resources/data/thaumicomniverse/research_pages/universal_enchantments.json");
        for(int count:List.of(0,1,24,300)) {
            var entries=JsonParser.parseString(Files.readString(path)).getAsJsonObject().getAsJsonArray("entries");
            var ids=new ArrayList<ResourceLocation>();
            for(int i=0;i<count;i++)ids.add(ResourceLocation.parse("mod"+(i%9)+":enchantment_"+i));
            UniversalEnchantments.appendPages(entries,ids);
            assertEquals(1,entries.size());
            var row=entries.get(0).getAsJsonObject();
            assertEquals(UniversalEnchantments.RESEARCH,row.get("key").getAsString());
            var pages=row.getAsJsonArray("pages");assertEquals(count+1,pages.size());
            var recipes=new HashSet<String>();var components=new HashSet<String>();
            for(var p:pages){var page=p.getAsJsonObject();assertTrue(components.add(page.get("id").getAsString()));
                if(page.has("recipe"))recipes.add(page.getAsJsonObject("recipe").getAsJsonArray("keys").get(0).getAsString());}
            assertEquals(new HashSet<>(ids.stream().map(UniversalEnchantments::recipeId).map(Object::toString).toList()),recipes);
            var before=entries.deepCopy();Collections.reverse(ids);UniversalEnchantments.appendPages(entries,ids);
            assertEquals(before,entries,"Registry order/reload must not duplicate or move recipe components");
        }
        assertEquals(RevivalFeatures.Group.ENCHANTMENTS,RevivalFeatures.researchGroup(UniversalEnchantments.RESEARCH));
    }
    @Test void completedIndividualResearchMigratesOnceWithoutGrantingFreshPlayersAccess() {
        var aspects=new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
        var knowledge=new PlayerKnowledge(aspects);
        assertFalse(EnchantmentResearchMigration.migrate(knowledge));
        knowledge.rememberResearch("@"+UniversalEnchantments.PREFIX+"oldmod/seen_only");
        assertFalse(EnchantmentResearchMigration.migrate(knowledge),"A discovery clue is not completed research");
        knowledge.rememberResearch(UniversalEnchantments.PREFIX+"removedmod/completed");
        assertTrue(EnchantmentResearchMigration.migrate(knowledge));
        assertTrue(knowledge.researchMarkers().contains(UniversalEnchantments.RESEARCH));
        assertTrue(knowledge.researchMarkers().contains(UniversalEnchantments.PREFIX+"removedmod/completed"));
        assertFalse(EnchantmentResearchMigration.migrate(knowledge));
    }
}
