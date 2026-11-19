package thmodern.vat;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;
@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class SheederServerTests {
    @GameTest(template="empty")
    public static void sheederShearingPersistsAndGrassRegrowsString(GameTestHelper test){
        var entity=Horizons.SHEEDER.get().create(test.getLevel());var shears=new ItemStack(Items.SHEARS);var pos=test.absolutePos(new BlockPos(3,3,3));
        var drops=entity.onSheared(null,shears,test.getLevel(),pos,100);
        test.assertTrue(drops.size()>=2&&drops.size()<=5&&drops.stream().allMatch(s->s.is(Items.STRING))&&entity.sheared(),"Shearing yields two to five string independent of fortune");
        test.assertTrue(entity.onSheared(null,shears,test.getLevel(),pos,0).isEmpty(),"No duplicate shearing before regrowth");
        var tag=new CompoundTag();entity.save(tag);var copy=Horizons.SHEEDER.get().create(test.getLevel());copy.load(tag);
        test.assertTrue(copy.sheared(),"Sheared fleece survives save/load");copy.ate();test.assertTrue(copy.isShearable(shears,test.getLevel(),pos),"Eating grass restores string fleece");
        test.assertTrue(copy.targetSelector.getAvailableGoals().isEmpty(),"Passive form has no hostile spider targets");test.succeed();
    }
    @GameTest(template="empty")
    public static void bothOriginsConvertAndWoolAllowsMixedColours(GameTestHelper test){
        for(var source:java.util.List.of(net.minecraft.world.entity.EntityType.SHEEP,net.minecraft.world.entity.EntityType.SPIDER)){
            var entity=(net.minecraft.world.entity.LivingEntity)source.create(test.getLevel());var name=source==net.minecraft.world.entity.EntityType.SHEEP?"sheep":"spider";
            var recipe=VatInfusionRecipes.all().stream().filter(r->r.id.getPath().equals("sheeder_from_"+name)).findFirst().orElseThrow();
            test.assertTrue(recipe.target(entity,false)&&CreatureTransformation.prepare(entity,recipe).getType()==Horizons.SHEEDER.get(),"Supported origin converts: "+name);
            if(name.equals("spider"))test.assertTrue(IngredientAssignment.match(recipe.ingredients,java.util.List.of(new ItemStack(tc4revival.content.TCItems.ZOMBIE_BRAIN.get()),new ItemStack(Items.RED_WOOL),new ItemStack(Items.BLUE_WOOL),new ItemStack(Items.BLACK_WOOL))).isPresent(),"Wool tag accepts mixed colours");
        }test.succeed();
    }
}
