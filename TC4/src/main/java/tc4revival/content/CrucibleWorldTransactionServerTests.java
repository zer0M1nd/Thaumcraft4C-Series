package tc4revival.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import tc4revival.research.KnowledgeSavedData;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class CrucibleWorldTransactionServerTests {
    @GameTest(template="wand_test_empty")
    public static void metalCrucibleTransactionsDebitAndPersistActualWorldOutputs(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));var box=new net.minecraft.world.phys.AABB(pos).inflate(3);
        String[] paths={"pure_iron","pure_gold","pure_copper","transmute_iron","transmute_gold","transmute_copper"};
        Item[] inputs={Items.IRON_ORE,Items.GOLD_ORE,Items.COPPER_ORE,Items.IRON_NUGGET,Items.GOLD_NUGGET,TCItems.nugget(NuggetType.COPPER)};
        UUID thrower=UUID.randomUUID();var data=KnowledgeSavedData.get(level.getServer());
        test.assertTrue(level.getServer().getPlayerList().getPlayer(thrower)==null,"Offline thrower fixture");
        int checked=0;
        try {
            for(int i=0;i<paths.length;i++) for(int water:new int[]{1,50,100}) {
                var recipe=(CrucibleRecipe)level.getRecipeManager().byKey(ResourceLocation.parse("thaumcraft:crucible/"+paths[i])).orElseThrow();
                data.update(thrower,bound->0,k->{k.rememberResearch(recipe.research());return null;});
                level.setBlock(pos,TCArcane.CRUCIBLE.get().defaultBlockState(),2);
                var tile=(CrucibleBlockEntity)level.getBlockEntity(pos);
                var saved=new CompoundTag();saved.putShort("Heat",(short)200);saved.putString("FluidName","water");saved.putInt("Amount",water);tile.load(saved);
                tile.contents().dissolve(recipe.costs());
                // Real world block-entity replacement before the transaction.
                var restored=new CrucibleBlockEntity(pos,tile.getBlockState());restored.load(tile.saveWithoutMetadata());level.setBlockEntity(restored);tile=restored;
                var input=new ItemEntity(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,new ItemStack(inputs[i]));input.setThrower(thrower);
                var before=new HashSet<>(level.getEntitiesOfClass(ItemEntity.class,box).stream().map(e->e.getUUID()).toList());
                tile.dissolve(input);
                var drops=level.getEntitiesOfClass(ItemEntity.class,box,e->!before.contains(e.getUUID()));
                try {
                    var expected=recipe.getResultItem(level.registryAccess());
                    test.assertTrue(input.isRemoved() && input.getItem().isEmpty(),"Catalyst debited before output: "+paths[i]);
                    test.assertTrue(tile.contents().water()==Math.max(0,water-50) && tile.contents().aspects().isEmpty(),"Exact actual water and aspects payment");
                    test.assertTrue(drops.size()==1 && drops.get(0) instanceof SpecialItemEntity && ItemStack.matches(drops.get(0).getItem(),expected),"Actual protected world output");
                    var after=tile.saveWithoutMetadata();tile.load(after);test.assertTrue(tile.saveWithoutMetadata().equals(after),"Post-transaction water/aspects survive reload");
                    tile.dissolve(input);tile.dissolve(drops.get(0));
                    test.assertTrue(level.getEntitiesOfClass(ItemEntity.class,box,e->!before.contains(e.getUUID())).size()==1 && tile.saveWithoutMetadata().equals(after),"Removed catalyst cannot replay; special result cannot redissolve");
                } finally {drops.forEach(ItemEntity::discard);input.discard();}
                level.removeBlockEntity(pos);level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);checked++;
            }
        } finally {level.removeBlockEntity(pos);level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);}
        tc4revival.TC4Revival.LOGGER.info("PASS metalCrucibleTransactionsDebitAndPersistActualWorldOutputs: {} actual transactions, offline thrower, low water, replacement and replay protection",checked);test.succeed();
    }
}
