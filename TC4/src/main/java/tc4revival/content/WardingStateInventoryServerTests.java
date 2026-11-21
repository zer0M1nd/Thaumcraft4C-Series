package tc4revival.content;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.item.BlockScanTargets;

/** Runtime input inventory for the pending legacy border-metadata conversion, not a parity assertion. */
@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class WardingStateInventoryServerTests {
    @GameTest(template="wand_test_empty",timeoutTicks=400)
    public static void exportWardingEligibleLegacyStates(GameTestHelper test) throws java.io.IOException {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(4,20,4));var rows=new JsonArray();var blocks=new HashSet<String>();int unmapped=0;
        for(var block:ForgeRegistries.BLOCKS)for(var state:block.getStateDefinition().getPossibleStates()) {
            if(state.hasBlockEntity()||state.isAir()||!state.isSolidRender(level,pos))continue;
            var target=BlockScanTargets.target(state);if(target.isEmpty()||target.get().legacyKey()==null){unmapped++;continue;}
            var key=target.get().legacyKey();String id=ForgeRegistries.BLOCKS.getKey(block).toString();blocks.add(id);
            var row=new JsonObject();row.addProperty("block",id);row.addProperty("class",block.getClass().getName());row.addProperty("legacy_picked_item",key.item());row.addProperty("legacy_picked_metadata",key.metadata());
            var properties=new JsonObject();BlockScanTargets.properties(state).entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->properties.addProperty(e.getKey(),e.getValue()));row.add("properties",properties);rows.add(row);
        }
        var root=new JsonObject();root.addProperty("note","Eligible state predicate plus known legacy scan identities; picked metadata is not storage metadata. Unmapped modern or missing states require classification.");root.addProperty("blocks",blocks.size());root.addProperty("states",rows.size());root.addProperty("unmapped_eligible_states",unmapped);root.add("entries",rows);
        var directory=Path.of(System.getProperty("tc4.validationOutput","validation-output"));
        Files.createDirectories(directory);
        var output=directory.resolve("warding-eligible-states.json");Files.writeString(output,new GsonBuilder().setPrettyPrinting().create().toJson(root));
        test.assertTrue(!rows.isEmpty(),"Loaded registry inventory is nonempty");
        tc4revival.TC4Revival.LOGGER.info("EXPORTED warding state inventory: {} blocks, {} known states, {} eligible unmapped states; this is inventory, not a parity PASS",blocks.size(),rows.size(),unmapped);test.succeed();
    }
}
