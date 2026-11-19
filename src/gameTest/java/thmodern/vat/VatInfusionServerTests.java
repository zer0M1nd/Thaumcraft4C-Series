package thmodern.vat;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraftforge.gametest.*;
import thmodern.Horizons;

@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class VatInfusionServerTests {
    private static VatInfusionRecipe recipe(String name){return VatInfusionRecipes.all().stream().filter(r->r.id.getPath().equals(name)).findFirst().orElseThrow();}
    @GameTest(template="empty")
    public static void potionIngredientChecksEffectAndStrength(GameTestHelper test){
        var requirement=recipe("quick_limbs").ingredients.get(0);var speed=PotionUtils.setPotion(new ItemStack(Items.POTION),Potions.STRONG_SWIFTNESS);var weak=PotionUtils.setPotion(new ItemStack(Items.POTION),Potions.SWIFTNESS);
        test.assertTrue(requirement.test(speed)&&!requirement.test(weak)&&!requirement.test(new ItemStack(Items.POTION)),"Quick limbs requires the strong speed potion, not any potion");test.succeed();
    }
    @GameTest(template="empty")
    public static void augmentationIsPersistentAndCannotBeAppliedTwice(GameTestHelper test){
        var cow=EntityType.COW.create(test.getLevel());double before=cow.getAttributeValue(Attributes.MOVEMENT_SPEED);var recipe=recipe("quick_limbs");
        test.assertTrue(CreatureInfusions.apply(cow,recipe,UUID.randomUUID()),"First infusion applies");
        test.assertTrue(Math.abs(cow.getAttributeValue(Attributes.MOVEMENT_SPEED)-before*1.3)<.00001,"Quick limbs adds thirty percent base movement speed");
        test.assertTrue(!CreatureInfusions.apply(cow,recipe,UUID.randomUUID()),"Duplicate infusion is refused");
        var tag=new CompoundTag();cow.save(tag);var restored=(net.minecraft.world.entity.LivingEntity)EntityType.create(tag,test.getLevel()).orElseThrow();CreatureInfusions.refresh(restored);
        test.assertTrue(Math.abs(restored.getAttributeValue(Attributes.MOVEMENT_SPEED)-before*1.3)<.00001&&restored.getPersistentData().getCompound(CreatureInfusions.DATA).getCompound("CloneCosts").getInt("motus")==16,"Reload neither doubles attributes nor drops cloning surcharge");test.succeed();
    }
    @GameTest(template="empty")
    public static void snapshotRetainsRecipeAndAlreadyConsumedComponents(GameTestHelper test){
        var tag=new CompoundTag();var recipe=recipe("awake_blood");tag.putString("Id",recipe.id.toString());tag.putString("Recipe",recipe.json().toString());tag.putUUID("Caster",UUID.randomUUID());tag.putInt("Component",2);tag.putInt("ItemTicks",4);tag.putInt("Instability",7);tag.putLong("DrawingFrom",new BlockPos(4,3,4).asLong());
        var process=new VatInfusionProcess();process.load(tag);var saved=process.save();test.assertTrue(process.active()&&saved.getInt("Component")==2&&saved.getInt("ItemTicks")==4&&saved.getInt("Instability")==7&&saved.getString("Recipe").equals(tag.getString("Recipe")),"Reload retains immutable output costs and consumed progress");
        tag.putString("Recipe","broken json");process.load(tag);test.assertTrue(!process.active(),"Malformed process cannot produce a free infusion");test.succeed();
    }
    @GameTest(template="empty")
    public static void merchantGolemAndUndeadCannotUseGenericInfusion(GameTestHelper test){
        var recipe=recipe("diamond_skin");test.assertTrue(!recipe.target(EntityType.VILLAGER.create(test.getLevel()),false)&&!recipe.target(EntityType.IRON_GOLEM.create(test.getLevel()),false)&&!recipe.target(EntityType.ZOMBIE.create(test.getLevel()),false)&&recipe.target(EntityType.COW.create(test.getLevel()),false),"Generic recipes honor excluded living categories");test.succeed();
    }
    @GameTest(template="empty")
    public static void completeVatCycleConsumesRealPedestalsAndAppliesOutput(GameTestHelper test){
        var level=test.getLevel();var center=test.absolutePos(new BlockPos(5,5,5));
        for(int part=0;part<36;part++)level.setBlock(center.offset(VatStructure.offset(part)),Horizons.VAT.get().defaultBlockState().setValue(VatBlock.PART,part),2);
        level.setBlock(center.above(),Horizons.VAT_MATRIX.get().defaultBlockState(),2);var vat=(VatEntity)level.getBlockEntity(center);
        var cow=EntityType.COW.create(level);cow.moveTo(center.above(3).getCenter());level.addFreshEntity(cow);test.assertTrue(vat.capture(cow),"A real creature enters the vat");
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"VatInfusionTest"));player.setPos(center.above(2).getCenter());thaumcraft.api.ThaumcraftApiHelper.completeResearch(player,"infusionVat");
        var recipe=recipe("quick_limbs");var pedestals=List.of(center.offset(-2,-1,0),center.offset(2,-1,0),center.offset(0,-1,2));
        for(int i=0;i<3;i++){level.setBlock(pedestals.get(i),tc4revival.content.TCArcane.ARCANE_PEDESTAL.get().defaultBlockState(),2);((tc4revival.content.ArcanePedestalBlockEntity)level.getBlockEntity(pedestals.get(i))).setItem(0,recipe.ingredients.get(i).getItems()[0].copy());}
        // Balanced skulls remove random instability events without mutating the server random generator.
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)if(x!=0||z!=0)level.setBlock(center.offset(x,2,z),net.minecraft.world.level.block.Blocks.SKELETON_SKULL.defaultBlockState(),2);
        test.assertTrue(vat.beginInfusion(player)&&vat.instability()<=0,"Research, subject and three matching pedestal items start a stabilized job");
        for(int i=0;i<30;i++)vat.tick();test.assertTrue(!CreatureInfusions.has(vat.subject(),"quick_limbs"),"No free output while essentia is missing");
        recipe.aspects.forEach((aspect,count)->vat.accept(aspect,count));for(int i=0;i<230;i++)vat.tick();
        test.assertTrue(vat.mode()==VatEntity.IDLE&&CreatureInfusions.has(vat.subject(),"quick_limbs")&&vat.essentia().stored().isEmpty(),"Finished job applies its output and clears the paid reservation");
        for(int i=0;i<3;i++)test.assertTrue(!recipe.ingredients.get(i).test(((tc4revival.content.ArcanePedestalBlockEntity)level.getBlockEntity(pedestals.get(i))).getItem(0)),"Actual pedestal ingredient was consumed");test.succeed();
    }
}
