package thmodern.vat;

import com.google.gson.JsonParser;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;

@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class CreatureTransformationServerTests {
    private static VatInfusionRecipe recipe(String output){
        return new VatInfusionRecipe(ResourceLocation.parse("thaumichorizons:test_conversion"),JsonParser.parseString("""
            {"research":"cowInfusion","instability":2,"target":{"kind":"entity","id":"minecraft:cow"},
             "ingredients":[{"item":"minecraft:brown_mushroom"}],"aspects":{"herba":8,"bestia":4},
             "output":{"kind":"entity","id":"%s"}}
            """.formatted(output)).getAsJsonObject());
    }
    @GameTest(template="empty")
    public static void conversionPreservesIdentityEquipmentAndAugmentations(GameTestHelper test){
        var cow=EntityType.COW.create(test.getLevel());cow.setCustomName(Component.literal("Retained cow"));cow.setAge(-1200);
        cow.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND,3));
        var speed=VatInfusionRecipes.all().stream().filter(r->r.id.getPath().equals("quick_limbs")).findFirst().orElseThrow();
        CreatureInfusions.apply(cow,speed,null);cow.setHealth(7);
        var converted=CreatureTransformation.prepare(cow,recipe("minecraft:mooshroom"));
        test.assertTrue(converted instanceof MushroomCow&&converted.getUUID().equals(cow.getUUID()),"Species changes without creating a new identity");
        test.assertTrue(converted.getCustomName().equals(cow.getCustomName())&&converted.getMainHandItem().getCount()==3&&converted.isBaby(),"Name, equipment and age survive");
        test.assertTrue(converted.getHealth()==7&&CreatureInfusions.has(converted,"quick_limbs"),"Health and augmentation survive");
        test.assertTrue(Math.abs(converted.getAttributeValue(Attributes.MOVEMENT_SPEED)-.26)<.00001,"Compatible speed modifier is preserved once on mooshroom base speed");
        var costs=converted.getPersistentData().getCompound(CreatureInfusions.DATA).getCompound("CloneCosts");
        test.assertTrue(costs.getInt("herba")==8&&costs.getInt("motus")==16,"Transformation adds to existing clone costs");
        var saved=new CompoundTag();converted.save(saved);var restored=(Mob)EntityType.create(saved,test.getLevel()).orElseThrow();
        test.assertTrue(restored.getType()==EntityType.MOOSHROOM&&restored.getUUID().equals(cow.getUUID()),"Converted type and identity persist in jar/save payload");
        test.assertTrue(test.getLevel().getEntity(cow.getUUID())==null,"Neither detached source nor replacement was spawned");test.succeed();
    }
    @GameTest(template="empty")
    public static void invalidConversionDoesNotMutateSource(GameTestHelper test){
        var cow=EntityType.COW.create(test.getLevel());var before=new CompoundTag();cow.save(before);
        for(var key:new String[]{"thaumichorizons:missing_species","minecraft:item","minecraft:cow"})
            test.assertTrue(CreatureTransformation.prepare(cow,recipe(key))==null,"Unknown, nonliving and same-species outputs are rejected: "+key);
        var after=new CompoundTag();cow.save(after);test.assertTrue(before.equals(after),"Rejected conversion does not modify the source creature");test.succeed();
    }
}
