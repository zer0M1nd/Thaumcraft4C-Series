package temodern.test;

import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.content.*;
import tc4revival.research.KnowledgeSavedData;
import temodern.crafting.ArcanePattern;
import temodern.crafting.CraftGrid;
import temodern.machine.Machines;
import temodern.machine.NetworkMachine;

@GameTestHolder(temodern.TE.ID) @PrefixGameTestTemplate(false)
public class GroupFeatureTests {
    @GameTest(template="test_empty",timeoutTicks=180)
    public static void eitherSoulIdentityAuthorizesAssembler(GameTestHelper t){
        var pos=new BlockPos(2,2,2);
        t.setBlock(pos,Machines.ALL.get("arcane_assembler").get());
        t.setBlock(pos.west(),AEBlocks.CREATIVE_ENERGY_CELL.block());
        var machine=(NetworkMachine)t.getBlockEntity(pos);
        t.runAfterDelay(100,()->{
            var soul=ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("thaumicomniverse:soul_core"));
            t.assertTrue(soul instanceof thaumcraft.api.wands.AutomationCore,"This integration test requires Omniverse's real soul core");
            var outside=new ItemStack(soul);var inside=new ItemStack(soul);
            var outerOwner=java.util.UUID.randomUUID();var innerOwner=java.util.UUID.randomUUID();
            outside.getOrCreateTag().putUUID("Owner",outerOwner);inside.getOrCreateTag().putUUID("Owner",innerOwner);
            var knowledge=KnowledgeSavedData.get(t.getLevel().getServer());
            knowledge.ensure(outerOwner,n->0);knowledge.ensure(innerOwner,n->0);
            var wand=((CastingWandItem)TCItems.CASTING_WAND.get()).create(WandParts.Rod.SILVERWOOD,WandParts.Cap.THAUMIUM,false,true);
            wand.getOrCreateTag().put(thaumcraft.api.wands.WandAutomation.KEY,inside.save(new CompoundTag()));
            machine.workstation.inventory.setItem(9,wand);machine.workstation.inventory.setItem(10,outside);
            var grid=new CraftGrid();
            for(int i=0;i<3;i++)grid.setItem(i,new ItemStack(TCItems.nugget(NuggetType.QUICKSILVER)));
            grid.setItem(3,new ItemStack(TCItems.shard(ShardType.ENTROPY)));grid.setItem(4,AEItems.ANNIHILATION_CORE.stack());
            var recipe=ArcanePattern.recipe(t.getLevel(),"thaumicenergistics:diffusion_core");
            var encoded=ArcanePattern.encode(grid,recipe,t.getLevel());
            machine.workstation.inventory.setItem(11,encoded);
            t.assertTrue(machine.workstation.getAvailablePatterns().isEmpty(),"Unknown research is unavailable from either core");
            knowledge.update(innerOwner,n->0,k->k.rememberResearch("thaumicenergistics:CORES"));
            t.assertTrue(machine.workstation.getAvailablePatterns().size()==1,"Wand soul alone supplies knowledge");
            for(int i=0;i<9;i++)machine.workstation.inventory.setItem(i,grid.getItem(i).copy());
            t.assertTrue(machine.workstation.craft(null),"Wand soul actually authorizes a craft");
            knowledge.update(outerOwner,n->0,k->k.rememberResearch("thaumicenergistics:CORES"));
            inside.getOrCreateTag().putUUID("Owner",java.util.UUID.randomUUID());
            wand.getOrCreateTag().put(thaumcraft.api.wands.WandAutomation.KEY,inside.save(new CompoundTag()));
            t.assertTrue(machine.workstation.getAvailablePatterns().size()==1,"Separate soul authorizes even when the wand owner lacks the research");
            for(int i=0;i<9;i++)machine.workstation.inventory.setItem(i,grid.getItem(i).copy());
            t.assertTrue(machine.workstation.craft(null),"Separate soul actually authorizes a craft");
            machine.workstation.inventory.setItem(10,ItemStack.EMPTY);
            t.assertTrue(machine.workstation.getAvailablePatterns().isEmpty(),"Removing the only valid identity revokes patterns immediately");
            t.succeed();
        });
    }
    @GameTest(template="test_empty")
    public static void oldAssemblerVisIsArchivedAndCannotBeSpent(GameTestHelper t){
        var pos=new BlockPos(2,2,2);t.setBlock(pos,Machines.ALL.get("arcane_assembler").get());
        var machine=(NetworkMachine)t.getBlockEntity(pos);var old=machine.saveWithoutMetadata();
        var oldVis=new CompoundTag();oldVis.putInt("aer",12345);old.put("vis",oldVis);
        machine.load(old);var saved=machine.saveWithoutMetadata();
        t.assertTrue(!saved.contains("vis")&&saved.getCompound("legacyAssemblerVis").equals(oldVis),"Old buffer preserved as migration data, not an active buffer");
        t.assertTrue(machine.storedVis(tc4revival.aspect.TCAspects.catalog().require("aer"))==0,"No wand means zero spendable vis");
        machine.load(saved);
        t.assertTrue(machine.saveWithoutMetadata().getCompound("legacyAssemblerVis").equals(oldVis),"Migration data survives subsequent saves");t.succeed();
    }
}
