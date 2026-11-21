package tc4revival.aspect.item;

import com.google.gson.*;
import java.util.*;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.*;
import static org.junit.jupiter.api.Assertions.*;

class NbtAspectRulesTest {
    @Test void absenceDefaultsDoNotCollapseVariantsOrOverwriteTheirCacheProjection(){
        var missing=new ItemNbtPredicate(JsonParser.parseString("{\"material\":{\"$exists\":false}}").getAsJsonObject());
        var iron=new CompoundTag();iron.putString("material","iron");
        assertTrue(missing.matches(null));assertTrue(missing.matches(missing.example()));assertFalse(missing.matches(iron));
        var exact=new ItemNbtPredicate(JsonParser.parseString("{\"material\":\"iron\"}").getAsJsonObject());
        var a=new CompoundTag();missing.project(iron,a);exact.project(iron,a);
        var b=new CompoundTag();exact.project(iron,b);missing.project(iron,b);assertEquals(a,b);assertEquals("iron",a.getString("material"));
        assertThrows(IllegalArgumentException.class,()->new ItemNbtPredicate(JsonParser.parseString("{\"x\":{\"$exists\":1}}").getAsJsonObject()));
    }
    private final AspectCatalog catalog = new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    private JsonElement table() { return JsonParser.parseString("""
        {"format":1,"entries":[
          {"items":["test:part"],"aspects":[{"aspect":"instrumentum","amount":1}]},
          {"items":["test:part"],"nbt":{"material":"wood"},"aspects":[{"aspect":"arbor","amount":2}]},
          {"items":["test:part"],"nbt":{"material":"wood"},"aspects":[{"aspect":"arbor","amount":5},{"aspect":"unavailable:kind","amount":7}]},
          {"items":["test:part"],"nbt":{"material":"iron"},"aspects":[{"aspect":"metallum","amount":3}]}]}
        """); }
    @Test void variantsMergeOnlyTheMatchingRowsAndRetainGenericBase() {
        var rules=ItemAspectJson.decodeKnown(table(),catalog,id->true);var resolver=new ItemAspectResolver(rules,catalog);
        var wood=new CompoundTag();wood.putString("material","wood");var iron=new CompoundTag();iron.putString("material","iron");
        var a=resolver.resolve("test:part",t->true,wood).orElseThrow();var b=resolver.resolve("test:part",t->true,iron).orElseThrow();
        assertEquals(5,a.amount(catalog.require("arbor")));assertEquals(0,a.amount(catalog.require("metallum")));
        assertEquals(3,b.amount(catalog.require("metallum")));assertEquals(0,b.amount(catalog.require("arbor")));
        assertEquals(1,a.amount(catalog.require("instrumentum")));assertEquals(1,resolver.resolve("test:part",t->true).orElseThrow().total());
        assertNotEquals(resolver.nbtIdentity("test:part",t->true,wood),resolver.nbtIdentity("test:part",t->true,iron));
        assertEquals(rules,ItemAspectJson.decode(ItemAspectJson.encode(rules)));
    }
    @Test void subsetCompoundsExactListsAndNumericValues() {
        var predicate=new ItemNbtPredicate(JsonParser.parseString("{\"data\":{\"tier\":2},\"materials\":[\"wood\",\"iron\"],\"enabled\":true}").getAsJsonObject());
        var tag=predicate.example();tag.getCompound("data").putByte("tier",(byte)2);tag.getCompound("data").putString("ignored","other");tag.putString("display","irrelevant");
        assertTrue(predicate.matches(tag));assertFalse(predicate.matches(null));
        tag.getList("materials",Tag.TAG_STRING).add(StringTag.valueOf("wood"));assertFalse(predicate.matches(tag));
        tag=predicate.example();tag.getCompound("data").putInt("tier",3);assertFalse(predicate.matches(tag));
    }
    @Test void cacheProjectionIgnoresUnrelatedNestedStateAndIsDefensive() {
        var source=JsonParser.parseString("{\"data\":{\"material\":\"wood\"}}").getAsJsonObject();var predicate=new ItemNbtPredicate(source);
        source.getAsJsonObject("data").addProperty("material","iron");var tag=predicate.example();
        var first=new CompoundTag();predicate.project(tag,first);tag.getCompound("data").putInt("charge",123);tag.putString("Name","changed");
        var next=new CompoundTag();predicate.project(tag,next);assertEquals(first,next);assertTrue(predicate.matches(tag));
        tag.getCompound("data").putString("material","iron");var third=new CompoundTag();predicate.project(tag,third);assertNotEquals(first,third);
        assertThrows(IllegalArgumentException.class,()->new ItemNbtPredicate(JsonParser.parseString("{\"bad\":null}").getAsJsonObject()));
    }
    @Test void bonusRowsUseTheSameNbtFilterButSumTheirMatches() {
        var resolver=new ItemAspectResolver(ItemAspectJson.decodeKnown(table(),catalog,id->true),catalog,true);var tag=new CompoundTag();tag.putString("material","wood");
        assertEquals(7,resolver.resolve("test:part",t->true,tag).orElseThrow().amount(catalog.require("arbor")));
    }
    @Test void numericListWitnessUsesOneNativeTypeAcrossDifferentSizes() {
        for(String json:List.of("{\"values\":[1,2147483648]}","{\"values\":[1,1.5]}")) {
            var predicate=new ItemNbtPredicate(JsonParser.parseString(json).getAsJsonObject());
            assertTrue(predicate.matches(predicate.example()));
        }
    }
    @Test void nativeMaterialAliasesUseOneBoundedSelectorWithoutMatchingOtherMaterials() {
        var predicate=new ItemNbtPredicate(JsonParser.parseString("{\"Material\":{\"$in\":[\"tconstruct:wood\",\"tconstruct:wood#warped\"]}}").getAsJsonObject());
        for(String value:List.of("tconstruct:wood","tconstruct:wood#warped")) {
            var tag=new CompoundTag();tag.putString("Material",value);assertTrue(predicate.matches(tag));
            var selected=new CompoundTag();predicate.project(tag,selected);assertEquals(value,selected.getString("Material"));
        }
        var iron=new CompoundTag();iron.putString("Material","tconstruct:iron");assertFalse(predicate.matches(iron));
        assertTrue(predicate.matches(predicate.example()));
        assertThrows(IllegalArgumentException.class,()->new ItemNbtPredicate(JsonParser.parseString("{\"Material\":{\"$in\":[]}}").getAsJsonObject()));
    }
    @Test void editorTagDoesNotOverrideMatchingNativeNbtButExactItemStillDoes() {
        var original=new ItemAspectResolver(ItemAspectJson.decodeKnown(table(),catalog,id->true).stream().filter(r->!r.nbt().isEmpty()).toList(),catalog);
        var tag=new CompoundTag();tag.putString("material","wood");
        var tagEdit=new tc4revival.aspect.editor.EditorDefinitions(Map.of("item_tag:test:parts",Map.of("terra",12)),1);
        var tagged=new tc4revival.aspect.editor.EditorItemResolver(original,tagEdit,catalog,(id,t)->true);
        assertTrue(tagged.editorOverride("test:part",t->true,tag).isEmpty());
        assertEquals(5,tagged.resolve("test:part",t->true,tag).orElseThrow().amount(catalog.require("arbor")));
        assertTrue(tagged.selectedRule("test:part",t->true,tag).isPresent());
        var exactEdit=new tc4revival.aspect.editor.EditorDefinitions(Map.of("item:test:part",Map.of("terra",7)),1);
        var exact=new tc4revival.aspect.editor.EditorItemResolver(original,exactEdit,catalog,(id,t)->true);
        assertEquals(7,exact.resolve("test:part",t->true,tag).orElseThrow().amount(catalog.require("terra")));
    }
}
