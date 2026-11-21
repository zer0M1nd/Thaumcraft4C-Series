package tc4revival.content;

import com.google.gson.*;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.*;
import static org.junit.jupiter.api.Assertions.*;

class AddonWandDefinitionsTest {
    @Test void addonRodAndCapHaveIndependentJsonProperties() {
        var rod=WandParts.Rod.decode(JsonParser.parseString("""
            {"tag":"example:oak","capacity":120,"craftCost":5,"staff":true,"glowing":true,"regenerates":"aer","texture":"example:rod.png"}
            """).getAsJsonObject());
        assertTrue(rod.staff());assertTrue(rod.glowing());assertFalse(rod.runes());assertEquals(120,rod.capacity);
        var json=JsonParser.parseString("""
            {"tag":"example:cap","cost":0.8,"aspectCosts":{"aer":0.6},"texture":"example:cap.png"}
            """).getAsJsonObject();
        var cap=WandParts.Cap.decode(json);assertEquals(.6f,cap.cost("aer"));assertEquals(.8f,cap.cost("terra"));
        json.addProperty("cost",-1);assertThrows(IllegalArgumentException.class,()->WandParts.Cap.decode(json));
    }
    @Test void savedUpgradeKeyWinsOverObsoleteNumericAlias() {
        var tag=new CompoundTag();tag.putString("key","thaumcraft:dowsing");tag.putShort("id",(short)0);
        assertEquals(FocusUpgrade.DOWSING.id,FocusUpgrade.savedId(tag));
        tag.putString("key","missing:upgrade");assertEquals(-1,FocusUpgrade.savedId(tag));
        tag.remove("key");assertEquals(FocusUpgrade.POTENCY.id,FocusUpgrade.savedId(tag));
    }
    @Test void namespacedAspectsParticipateInTheGraphAndCyclesAreRejected() {
        var rows=new JsonArray();for(var value:tc4revival.data.AddonDefinitions.bundled("/data/thaumcraft/aspect_registry.json"))rows.add(value);
        rows.add(JsonParser.parseString("""
            {"tag":"example:insight","color":123456,"components":["aer","cognitio"],"chatColor":"f","blendFactor":1}
            """));
        var catalog=AspectDefinitions.decode(rows);
        assertEquals("example:insight",catalog.combine(catalog.require("aer"),catalog.require("cognitio")).orElseThrow().tag());
        rows.get(rows.size()-1).getAsJsonObject().getAsJsonArray("components").set(0,new JsonPrimitive("example:insight"));
        assertThrows(IllegalArgumentException.class,()->AspectDefinitions.decode(rows));
    }
}
