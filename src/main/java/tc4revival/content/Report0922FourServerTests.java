package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public class Report0922FourServerTests {
    @GameTest(template="wand_test_empty")
    public static void horizonsLegacyCreaturesAndAnimatedGolemsHaveTheirOwnAspects(GameTestHelper test) throws Exception {
        if(!net.minecraftforge.fml.ModList.get().isLoaded("thaumichorizons")){test.succeed();return;}
        var resource=test.getLevel().getServer().getResourceManager().getResource(net.minecraft.resources.ResourceLocation.parse("thaumichorizons:entity_aspects/legacy.json")).orElseThrow();
        try(var reader=resource.openAsReader()){
            for(var rule:tc4revival.aspect.entity.EntityAspectJson.decode(com.google.gson.JsonParser.parseReader(reader))){
                var type=net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(net.minecraft.resources.ResourceLocation.parse(rule.entity()));
                test.assertTrue(type!=null,"Migrated entity exists: "+rule.entity());var entity=type.create(test.getLevel());
                if(rule.golemMaterial()!=null)thaumcraft.api.golems.GolemApi.configure((GolemEntity)entity,net.minecraft.resources.ResourceLocation.parse(rule.golemMaterial()),null,null);
                var found=tc4revival.aspect.entity.EntityAspects.resolve(entity).orElseThrow().aspects();
                for(var q:rule.aspects())test.assertTrue(found.amount(TCAspects.catalog().require(q.aspect()))==q.amount(),"Entity aspect "+rule.entity()+" / "+q.aspect());
                entity.discard();
            }
        }
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void restoredAddonAspectDefinitionsResolveInTheLoadedGame(GameTestHelper test) throws Exception {
        var manager=test.getLevel().getServer().getResourceManager();int checked=0;
        // The old partial files are intentionally empty after explicit-table materialization.
        for(String id:java.util.List.of("thaumic_tinkerer:explicit","automagy:explicit","thaumichorizons:explicit")){
            var key=net.minecraft.resources.ResourceLocation.parse(id);if(!net.minecraftforge.fml.ModList.get().isLoaded(key.getNamespace()))continue;
            var resource=manager.getResource(key.withPath("item_aspects/"+key.getPath()+".json")).orElseThrow();
            java.util.List<tc4revival.aspect.item.ItemAspectRule> rules;
            try(var reader=resource.openAsReader()){rules=tc4revival.aspect.item.ItemAspectJson.decode(com.google.gson.JsonParser.parseReader(reader));}
            test.assertTrue(!rules.isEmpty(),"Loaded addon has its materialized explicit table: "+id);
            for(var rule:rules)for(String item:rule.items()){
                var registered=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse(item));
                test.assertTrue(registered!=null&&registered!=Items.AIR,"Restored definition targets a real item: "+item);
                var found=tc4revival.aspect.item.ItemStackAspects.resolve(new ItemStack(registered)).orElseThrow();
                for(var quantity:rule.aspects()){
                    String tag=quantity.aspect();if(item.equals("thaumic_tinkerer:nether_shard")&&tag.equals("ignis")&&net.minecraftforge.fml.ModList.get().isLoaded("forbidden_magic"))tag="forbidden_magic:infernus";
                    test.assertTrue(found.amount(TCAspects.catalog().require(tag))>=quantity.amount(),"Missing migrated aspect "+item+" / "+tag);
                }
                checked++;
            }
        }
        test.assertTrue(checked>0,"Run with at least one affected addon");test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void generatedAddonDataIsLimitedButExplicitDataStaysComplete(GameTestHelper test) throws Exception {
        var files=tc4revival.aspect.sync.ServerAspectData.snapshot().directory("item_aspects");int checked=0;
        var limits=tc4revival.aspect.recipe.InferenceLimits.server();
        for(var entry:files.entrySet()){
            for(var rule:tc4revival.aspect.item.ItemAspectJson.decode(entry.getValue())){
                if(entry.getKey().equals(tc4revival.aspect.editor.AdvancedAspectSources.RESOURCE)){
                    test.assertTrue(rule.generated(),"Runtime inferred data retains provenance: "+rule.items());
                    test.assertTrue(rule.aspects().size()<=limits.types()&&rule.aspects().stream().allMatch(q->q.amount()<=limits.maximum()),
                            "Runtime generated data obeys the loaded limits: "+rule.items());checked++;
                }else test.assertTrue(!rule.generated(),"Bundled materialized data remains explicit: "+entry.getKey());
            }
        }
        test.assertTrue(checked>0,"Run with advanced inference enabled and actual inferred items");
        if(net.minecraftforge.fml.ModList.get().isLoaded("automagy")){
            var item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse("automagy:enchanted_tome"));
            test.assertTrue(tc4revival.aspect.item.ItemStackAspects.resolve(new ItemStack(item)).orElseThrow().size()==8,"Explicit enchanted tome keeps eight types");
        }
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void emptyTotemNodesPreserveTheirColumnAfterReload(GameTestHelper test){
        var level=test.getLevel();var bottom=test.absolutePos(new BlockPos(3,2,3));
        level.setBlockAndUpdate(bottom,TCArcane.OBSIDIAN_TOTEM.get().defaultBlockState());
        for(int y=1;y<=3;y++){
            var pos=bottom.above(y);level.setBlockAndUpdate(pos,TCArcane.OBSIDIAN_NODE_TOTEM.get().defaultBlockState());
            var node=(AuraNodeBlockEntity)level.getBlockEntity(pos);var saved=node.saveWithFullMetadata();
            level.removeBlockEntity(pos);
            var restored=(AuraNodeBlockEntity)net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos,level.getBlockState(pos),saved);
            level.setBlockEntity(restored);for(int tick=0;tick<1200;tick++)restored.tick();
            test.assertTrue(level.getBlockState(pos).is(TCArcane.OBSIDIAN_TOTEM.get()),"An exhausted node must retain its physical totem at height "+y);
            test.assertTrue(level.getBlockEntity(pos)==null,"Exhausted carrier no longer ticks as a node");
        }
        for(int y=0;y<=3;y++)test.assertTrue(level.getBlockState(bottom.above(y)).is(TCArcane.OBSIDIAN_TOTEM.get()),"All four column blocks survive");
        var isolated=bottom.east(4);level.setBlockAndUpdate(isolated,TCArcane.AURA_NODE.get().defaultBlockState());
        var node=(AuraNodeBlockEntity)level.getBlockEntity(isolated);for(int tick=0;tick<1200;tick++)node.tick();
        test.assertTrue(level.getBlockState(isolated).isAir(),"Uncontained exhausted nodes still disappear");test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void jarLabelKeepsSelectedZeroAmountAspectThroughSerialization(GameTestHelper test){
        var label=TCItems.JAR_LABEL.get();var stack=new ItemStack(label);var aspect=TCAspects.catalog().require("praecantatio");
        label.setStoredAspects(stack,new AspectAmounts().add(aspect,0));
        var copy=ItemStack.of(stack.save(new net.minecraft.nbt.CompoundTag()));
        test.assertTrue(label.storedAspects(copy).contains(aspect)&&label.storedAspects(copy).size()==1,"Zero quantity denotes a visible selection, not an empty label");test.succeed();
    }
}
