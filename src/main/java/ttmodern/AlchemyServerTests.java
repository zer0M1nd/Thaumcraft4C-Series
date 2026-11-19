package ttmodern;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.content.*;

/** Exercises addon recipes against the main mod's actual transaction implementations. */
@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class AlchemyServerTests {
    private static ItemStack item(String id) {return new ItemStack(ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(id)));}
    @GameTest(template="wand_test_empty")
    public static void tinkererCrucibleAcquisitionConsumesExactCosts(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));var box=new net.minecraft.world.phys.AABB(pos).inflate(3);
        var thrower=UUID.randomUUID();var knowledge=tc4revival.research.KnowledgeSavedData.get(level.getServer());
        try {
            for(String name:List.of("gaseous_light","gaseous_shadow","bright_nitor")) {
                var recipe=(CrucibleRecipe)level.getRecipeManager().byKey(ResourceLocation.parse(Tinkerer.ID+":"+name)).orElseThrow();
                knowledge.update(thrower,bound->0,k->{k.rememberResearch(recipe.research());return null;});
                level.setBlock(pos,TCArcane.CRUCIBLE.get().defaultBlockState(),2);
                var tile=(CrucibleBlockEntity)level.getBlockEntity(pos);var saved=new CompoundTag();
                saved.putShort("Heat",(short)200);saved.putString("FluidName","water");saved.putInt("Amount",100);tile.load(saved);tile.contents().dissolve(recipe.costs());
                var input=new ItemEntity(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,item(name.equals("bright_nitor")?"thaumcraft:nitor":"thaumcraft:empty_phial"));input.setThrower(thrower);
                var before=new HashSet<>(level.getEntitiesOfClass(ItemEntity.class,box).stream().map(ItemEntity::getUUID).toList());
                tile.dissolve(input);
                var drops=level.getEntitiesOfClass(ItemEntity.class,box,e->!before.contains(e.getUUID()));
                try {
                    test.assertTrue(input.isRemoved() && input.getItem().isEmpty(),name+" catalyst consumed");
                    test.assertTrue(tile.contents().water()==50 && tile.contents().aspects().isEmpty(),name+" exact water/aspect payment");
                    test.assertTrue(drops.size()==1 && ItemStack.matches(drops.get(0).getItem(),recipe.getResultItem(level.registryAccess())),name+" real world output");
                    tile.dissolve(input);tile.dissolve(drops.get(0));
                    test.assertTrue(tile.contents().water()==50 && drops.get(0).isAlive(),"Transaction cannot replay or dissolve its own result");
                }finally{drops.forEach(ItemEntity::discard);input.discard();}
                level.removeBlock(pos,false);
            }
            test.succeed();
        }finally{level.removeBlock(pos,false);}
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererRestorerInfusionConsumesAndResumes(GameTestHelper test) {
        var level=test.getLevel();var recipe=(InfusionRecipe)level.getRecipeManager().byKey(ResourceLocation.parse(Tinkerer.ID+":restorer")).orElseThrow();
        var center=item("thaumcraft:thaumium_block");
        var components=List.of("minecraft:iron_ingot","minecraft:gold_ingot","minecraft:diamond","minecraft:cobblestone","minecraft:oak_planks","minecraft:leather","thaumcraft:enchanted_fabric","thaumcraft:thaumium_ingot").stream().map(AlchemyServerTests::item).toList();
        test.assertTrue(!recipe.matches(new InfusionRecipe.Input(center,components,key->false),level),"Research gate enforced");
        test.assertTrue(recipe.matches(new InfusionRecipe.Input(center,components,key->key.equals(Tinkerer.ID+":REPAIRER")),level),"Exact eight components match");
        var pedestals=new ArrayList<ArcanePedestalBlockEntity>();
        for(int i=0;i<components.size();i++) {
            var pos=test.absolutePos(new BlockPos(1+i%4,1,1+i/4));level.setBlock(pos,TCArcane.ARCANE_PEDESTAL.get().defaultBlockState(),2);
            var pedestal=(ArcanePedestalBlockEntity)level.getBlockEntity(pos);pedestal.setItem(0,components.get(i).copy());pedestals.add(pedestal);
        }
        var process=new InfusionProcess();var plan=recipe.plan(center);var supply=plan.aspects();
        process.start(plan,center,"addon-test",-100);
        class Host implements InfusionProcess.Host {
            int finished;ItemStack result=ItemStack.EMPTY;
            public boolean centerMatches(ItemStack expected){return ItemStack.matches(expected,center);}
            public InfusionProcess.Experience drainExperience(){throw new AssertionError("Unexpected XP cost");}
            public boolean drainEssentia(tc4revival.aspect.Aspect aspect){if(supply.amount(aspect)<1)return false;supply.remove(aspect,1);return true;}
            public List<ArcanePedestalBlockEntity> pedestals(){return pedestals;}
            public void ingredientStarted(BlockPos pos){}
            public void instabilityEvent(int event){throw new AssertionError("Stable fixture");}
            public void refreshSurroundings(){}
            public void failed(){throw new AssertionError("Unchanged center");}
            public void finish(InfusionOutput output,String player){finished++;test.assertTrue(pedestals.stream().allMatch(p->p.getItem(0).isEmpty()),"All ingredients consumed before publication");result=output.apply(center);}
            public void changed(){}
        }
        var host=new Host();var random=net.minecraft.util.RandomSource.create(9);
        try {
            for(int i=0;i<250 && process.crafting();i++){process.cycle(random,host);if(i==20)process.load(process.save());}
            test.assertTrue(!process.crafting() && host.finished==1 && host.result.is(Tinkerer.RESTORER.get().asItem()),"Resumed infusion completes once");
            test.assertTrue(supply.isEmpty(),"Exact recipe essentia consumed");test.succeed();
        }finally{pedestals.forEach(p->level.removeBlock(p.getBlockPos(),false));}
    }
}
