package tc4revival.data;

import java.util.List;
import java.util.Set;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import tc4revival.content.TCEldritch;

public  class EldritchResources extends BlockStateProvider {
    public EldritchResources(PackOutput output,ExistingFileHelper helper) { super(output,"thaumcraft",helper); }
    @Override protected void registerStatesAndModels() {
        simpleBlockWithItem(TCEldritch.OBSIDIAN_TILE.get(),models().cubeAll("obsidian_tile",modLoc("block/obsidiantile")));
        for (String name:List.of("eldritch_altar","eldritch_obelisk","eldritch_obelisk_segment","eldritch_cap","eldritch_crystal","eldritch_crab_spawner")) {
            var block=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(modLoc(name));
            if(!name.equals("eldritch_crab_spawner"))simpleBlock(block,models().getBuilder(name).texture("particle",modLoc("block/obsidiantile")));
            var item=itemModels().getBuilder(name).parent(new net.minecraftforge.client.model.generators.ModelFile.UncheckedModelFile(mcLoc("builtin/entity")));
            boolean tall=name.equals("eldritch_obelisk") || name.equals("eldritch_obelisk_segment");
            item.transforms().transform(net.minecraft.world.item.ItemDisplayContext.GUI).rotation(30,225,0).scale(tall ? .16f : .625f).translation(0,0,0).end()
                    .transform(net.minecraft.world.item.ItemDisplayContext.GROUND).scale(tall ? .15f : .25f).translation(0,3,0).end()
                    .transform(net.minecraft.world.item.ItemDisplayContext.FIXED).scale(tall ? .16f : .5f).rotation(0,180,0).end()
                    .transform(net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND).rotation(0,225,0).scale(tall ? .2f : .5f).end()
                    .transform(net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND).rotation(75,45,0).scale(tall ? .15f : .375f).translation(0,2.5f,0).end()
                    .transform(net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(0,45,0).scale(tall ? .2f : .5f).end()
                    .transform(net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(75,45,0).scale(tall ? .15f : .375f).translation(0,2.5f,0).end().end();
        }
        simpleBlock(TCEldritch.PORTAL.get(),models().getBuilder("eldritch_portal").texture("particle",modLoc("block/obsidiantile")));
        simpleBlock(TCEldritch.DOOR_BARRIER.get(),models().getBuilder("eldritch_door_barrier").texture("particle",modLoc("block/deco_3")));
        var lock=models().orientable("eldritch_lock",modLoc("block/deco_3"),modLoc("block/deco_2"),modLoc("block/deco_3"));
        horizontalBlock(TCEldritch.LOCK.get(),lock); simpleBlockItem(TCEldritch.LOCK.get(),lock);
        itemModels().withExistingParent("obelisk_placer",mcLoc("item/generated")).texture("layer0",modLoc("item/ob_placer"));
        simpleBlockWithItem(TCEldritch.DOOR_FRAME.get(),models().cubeAll("eldritch_door_frame",modLoc("block/deco_3")));
        for (var block:List.of(TCEldritch.GLOWING_STONE.get(),TCEldritch.GLYPHED_STONE.get(),TCEldritch.CARVED_STONE.get())) {
            String name=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(block).getPath();
            String texture=switch(block.kind()) { case GLOWING->"es_i_1"; case GLYPHED->"es_i_2"; case CARVED->"deco_1"; };
            var isolated=models().withExistingParent(name+"_item",mcLoc("block/block")).texture("all",modLoc("block/"+texture)).texture("particle","#all");
            isolated.element().from(2,2,2).to(14,14,14).allFaces((side,face)->face.texture("#all")).end();
            itemModels().withExistingParent(name,modLoc("block/"+name+"_item"));
            getVariantBuilder(block).forAllStates(state->{
                int mask=0; for (var side:net.minecraft.core.Direction.values()) if (state.getValue(tc4revival.content.EldritchDecorBlock.property(side))) mask|=1<<side.ordinal();
                var model=models().getBuilder(name+"_"+mask).texture("all",modLoc("block/"+texture)).texture("particle","#all");
                model.element().from((mask&16)!=0 ? 0 : 2,(mask&1)!=0 ? 0 : 2,(mask&4)!=0 ? 0 : 2)
                        .to((mask&32)!=0 ? 16 : 14,(mask&2)!=0 ? 16 : 14,(mask&8)!=0 ? 16 : 14).allFaces((side,face)->face.texture("#all")).end();
                return net.minecraftforge.client.model.generators.ConfiguredModel.builder().modelFile(model).build();
            });
        }
        itemModels().withExistingParent("crimson_rites",mcLoc("item/generated")).texture("layer0",modLoc("item/crimson_rites"));
        itemModels().withExistingParent("crimson_sword",mcLoc("item/handheld")).texture("layer0",modLoc("item/crimson_blade"));
        for (var armor:tc4revival.content.CultistEquipment.Armor.values())
            itemModels().withExistingParent(armor.path,mcLoc("item/generated")).texture("layer0",modLoc("item/"+armor.texture));
        itemModels().withExistingParent("eldritch_eye",mcLoc("item/generated")).texture("layer0",modLoc("item/eldritch_object"));
        itemModels().withExistingParent("runed_tablet",mcLoc("item/generated")).texture("layer0",modLoc("item/eldritch_object_2"));
        simpleBlockWithItem(TCEldritch.TRAP.get(),models().cubeAll("eldritch_rune_trap",modLoc("block/es_5")));
        simpleBlock(TCEldritch.CRAB_SPAWNER.get(),models().cubeAll("eldritch_crab_spawner",modLoc("block/crust")));
        for (boolean crate:new boolean[]{false,true}) for (int rarity=0;rarity<3;rarity++) {
            String kind=crate ? "crate" : "urn",name="loot_"+kind+"_"+rarity;
            var model=models().getBuilder(name).texture("particle",modLoc("block/"+kind+"_side_"+rarity))
                    .texture("side",modLoc("block/"+kind+"_side_"+rarity)).texture("top",modLoc("block/"+kind+"_top"));
            int[][] boxes=crate ? new int[][]{{1,0,1,15,14,15}} : new int[][]{{3,0,3,13,1,13},{2,1,2,14,13,14},{4,13,4,12,16,12}};
            for (int[] box:boxes) model.element().from(box[0],box[1],box[2]).to(box[3],box[4],box[5])
                    .allFaces((side,face)->face.texture(side.getAxis()==net.minecraft.core.Direction.Axis.Y ? "#top" : "#side")).end();
            simpleBlock(TCEldritch.loot(crate,rarity),model);
            itemModels().withExistingParent(name,modLoc("block/"+name)).transforms()
                    .transform(net.minecraft.world.item.ItemDisplayContext.GUI).rotation(30,225,0).scale(.625f).end();
        }
        simpleBlockWithItem(TCEldritch.STONE.get(),models().cubeAll("eldritch_stone",modLoc("block/es_1")));
        simpleBlockWithItem(TCEldritch.NONSPAWNING_STONE.get(),models().cubeAll("eldritch_stone_nonspawning",modLoc("block/es_1")));
        simpleBlockWithItem(TCEldritch.ROCK.get(),models().cubeAll("eldritch_rock",modLoc("block/er_1")));
        simpleBlockWithItem(TCEldritch.CRUST.get(),models().cubeAll("eldritch_crust",modLoc("block/crust")));
        simpleBlockWithItem(TCEldritch.PEDESTAL_STONE.get(),models().cubeColumn("eldritch_pedestal_stone",modLoc("block/es_p"),modLoc("block/es_1")));
        stairsBlock(TCEldritch.STAIRS.get(),modLoc("block/es_1")); simpleBlockItem(TCEldritch.STAIRS.get(),models().getExistingFile(modLoc("block/eldritch_stairs")));
        slabBlock(TCEldritch.SLAB.get(),modLoc("block/eldritch_stone"),modLoc("block/es_1"));
        simpleBlockItem(TCEldritch.SLAB.get(),models().getExistingFile(modLoc("block/eldritch_slab")));
        // Register every alternate sprite for the position-dependent baked model.
        for (String prefix:List.of("es_","er_")) for (int variant=2;variant<=(prefix.equals("es_") ? 8 : 4);variant++)
            models().cubeAll("eldritch_texture_"+prefix+variant,modLoc("block/"+prefix+variant));
        simpleBlock(TCEldritch.FIELD.get(),models().getBuilder("eldritch_field").texture("particle",modLoc("block/es_1")));
        simpleBlock(TCEldritch.VOID.get(),models().getBuilder("eldritch_void").texture("particle",modLoc("block/es_1")));
    }
    public static LootTableProvider loot(PackOutput output) {
        return new LootTableProvider(output,Set.of(),List.of(new LootTableProvider.SubProviderEntry(Drops::new,LootContextParamSets.BLOCK)));
    }
    private static  class Drops extends BlockLootSubProvider {
        Drops() { super(Set.of(),FeatureFlags.REGISTRY.allFlags()); }
        @Override protected Iterable<Block> getKnownBlocks() {
            var blocks=new java.util.ArrayList<Block>(List.of(TCEldritch.ALTAR.get(),TCEldritch.OBELISK.get(),TCEldritch.OBELISK_SEGMENT.get(),TCEldritch.CAP.get(),TCEldritch.LOCK.get(),TCEldritch.CRYSTAL.get(),TCEldritch.OBSIDIAN_TILE.get(),TCEldritch.DOOR_FRAME.get(),TCEldritch.GLOWING_STONE.get(),TCEldritch.GLYPHED_STONE.get(),TCEldritch.CARVED_STONE.get(),TCEldritch.STONE.get(),TCEldritch.ROCK.get(),TCEldritch.NONSPAWNING_STONE.get(),TCEldritch.CRUST.get(),
                    TCEldritch.PEDESTAL_STONE.get(),TCEldritch.STAIRS.get(),TCEldritch.SLAB.get(),TCEldritch.TRAP.get(),TCEldritch.CRAB_SPAWNER.get()));
            for(boolean crate:new boolean[]{false,true})for(int rarity=0;rarity<3;rarity++)blocks.add(TCEldritch.loot(crate,rarity));
            return blocks;
        }
        @Override protected void generate() {
            for(boolean crate:new boolean[]{false,true})for(int rarity=0;rarity<3;rarity++)
                add(TCEldritch.loot(crate,rarity),net.minecraft.world.level.storage.loot.LootTable.lootTable().withPool(
                        net.minecraft.world.level.storage.loot.LootPool.lootPool().add(tc4revival.content.LegacyContainerLoot.entry(rarity))));
            for (Block block:getKnownBlocks()) {
                if(block instanceof tc4revival.content.EldritchLootBlock)continue;
                if (block instanceof tc4revival.content.EldritchMonumentBlock || block==TCEldritch.LOCK.get() || block==TCEldritch.TRAP.get() || block==TCEldritch.CRAB_SPAWNER.get() || block==TCEldritch.DOOR_FRAME.get() || block==TCEldritch.CARVED_STONE.get()) add(block,net.minecraft.world.level.storage.loot.LootTable.lootTable());
                else if (block==TCEldritch.CRYSTAL.get()) dropOther(block,tc4revival.content.TCItems.shard(tc4revival.content.ShardType.BALANCED));
                else if (block==TCEldritch.GLYPHED_STONE.get()) dropOther(block,tc4revival.content.TCItems.KNOWLEDGE_FRAGMENT.get());
                else if (block==TCEldritch.SLAB.get()) add(block,this::createSlabItemTable);
                else dropSelf(block);
            }
        }
    }
}
