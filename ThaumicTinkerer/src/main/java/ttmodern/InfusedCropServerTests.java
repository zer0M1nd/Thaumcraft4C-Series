package ttmodern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class InfusedCropServerTests {
    @GameTest(template="wand_test_empty") public static void tinkererCropPreviewMatchesLoadedLootAndNetwork(GameTestHelper test){
        var recipes=CropDropPreview.collect(test.getLevel().getServer());var metal=recipes.stream().filter(recipe->recipe.aspect().equals("metallum")).findFirst().orElseThrow();test.assertTrue(metal.exact()&&metal.drops().size()==1&&metal.drops().get(0).stack().is(Items.IRON_INGOT)&&metal.drops().get(0).stack().getCount()==4&&metal.drops().get(0).chance()==1,"Preview reads exact count and probability from the actual loaded loot table");
        var table=com.google.gson.JsonParser.parseString("{\"pools\":[{\"rolls\":1,\"entries\":[{\"type\":\"minecraft:item\",\"name\":\"minecraft:diamond\",\"weight\":1},{\"type\":\"minecraft:empty\",\"weight\":3}]}]}").getAsJsonObject();var preview=CropDropPreview.parse("lucrum",table);test.assertTrue(preview.exact()&&preview.drops().get(0).chance()==.25,"Empty results remain in the weight denominator");table.getAsJsonArray("pools").get(0).getAsJsonObject().addProperty("rolls",2);test.assertTrue(!CropDropPreview.parse("lucrum",table).exact(),"Unsupported rules do not produce misleading probabilities");
        var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());try{CropDropPreview.encode(new CropDropPreview.Snapshot(recipes),buffer);var decoded=CropDropPreview.decode(buffer).recipes();test.assertTrue(decoded.size()==recipes.size(),"All crop types survive synchronization");for(int r=0;r<recipes.size();r++){var before=recipes.get(r);var after=decoded.get(r);test.assertTrue(before.aspect().equals(after.aspect())&&before.exact()==after.exact()&&before.drops().size()==after.drops().size(),"Preview metadata survives synchronization");for(int d=0;d<before.drops().size();d++)test.assertTrue(ItemStack.matches(before.drops().get(d).stack(),after.drops().get(d).stack())&&before.drops().get(d).chance()==after.drops().get(d).chance(),"Full drop NBT/count and probability survive synchronization");}}finally{buffer.release();}test.succeed();
    }
    @GameTest(template="wand_test_empty") public static void tinkererCropBreedingDropsAreReadOnlyAndSoilFertilizesOnce(GameTestHelper test) {
        var world=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));world.setBlock(pos.below(),Tinkerer.INFUSED_FARMLAND.get().defaultBlockState(),3);world.setBlock(pos,Tinkerer.INFUSED_CROP.get().defaultBlockState().setValue(InfusedCropBlock.AGE,7),3);
        var soil=(InfusedFarmlandEntity)world.getBlockEntity(pos.below());for(int i=0;i<20;i++)soil.fertilize("aqua");var crop=(InfusedCropEntity)world.getBlockEntity(pos);crop.initialize("terra",Map.of("ordo",5,"perditio",5));
        var before=soil.saveWithoutMetadata();var drops=Tinkerer.INFUSED_CROP.get().getDrops(world.getBlockState(pos),new LootParams.Builder(world).withParameter(LootContextParams.BLOCK_STATE,world.getBlockState(pos)).withParameter(LootContextParams.ORIGIN,pos.getCenter()).withParameter(LootContextParams.TOOL,ItemStack.EMPTY).withParameter(LootContextParams.BLOCK_ENTITY,crop));
        test.assertTrue(drops.stream().anyMatch(s->s.is(Tinkerer.INFUSED_SEEDS.get())&&InfusedSeedItem.aspect(s).equals("victus")),"Aqua residue combines with terra into Victus seeds");test.assertTrue(drops.stream().anyMatch(s->s.is(Tinkerer.INFUSED_GRAIN.get("terra").get())),"Mature primal crop drops its grain via loot table");test.assertTrue(before.equals(soil.saveWithoutMetadata()),"Loot preview never fertilizes soil");
        crop.fertilize(world);var after=soil.saveWithoutMetadata();crop.fertilize(world);test.assertTrue(after.equals(soil.saveWithoutMetadata()),"Actual harvest cannot fertilize twice through different hooks");test.assertTrue(soil.memory().values().stream().mapToInt(Integer::intValue).sum()==20,"Soil memory remains capped");
        world.removeBlock(pos,false);world.removeBlock(pos.below(),false);test.succeed();
    }
    @GameTest(template="wand_test_empty") public static void tinkererSeedInvalidDataAndOccupiedPlanting(GameTestHelper test) {
        var world=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));world.setBlock(pos,Blocks.FARMLAND.defaultBlockState(),3);world.setBlock(pos.above(),Blocks.CHEST.defaultBlockState(),3);
        var player=new net.minecraftforge.common.util.FakePlayer(world,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"TTSeed"));var seed=new ItemStack(Tinkerer.INFUSED_SEEDS.get());seed.getOrCreateTag().putString("Aspect","missing:unknown");var bad=new net.minecraft.nbt.CompoundTag();for(String primal:CropTraits.PRIMALS)bad.putInt(primal,Integer.MAX_VALUE);seed.getOrCreateTag().put("Traits",bad);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,seed);
        test.assertTrue(InfusedSeedItem.aspect(seed).equals("aer")&&InfusedSeedItem.traits(seed).values().stream().mapToInt(Integer::intValue).sum()==50,"Malformed genome is normalized without overflow");
        var result=seed.useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(pos.getCenter(),Direction.UP,pos,false)));
        test.assertTrue(!result.consumesAction()&&world.getBlockState(pos.above()).is(Blocks.CHEST)&&world.getBlockState(pos).is(Blocks.FARMLAND)&&seed.getCount()==1,"Planting never replaces an occupied block or changes its soil");world.removeBlock(pos.above(),false);world.removeBlock(pos,false);test.succeed();
    }
    @GameTest(template="wand_test_empty") public static void tinkererInfusedPotionDrinkAndTimedForcefield(GameTestHelper test) {
        var world=test.getLevel();var player=new net.minecraftforge.common.util.FakePlayer(world,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"TTPotion"));player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var potion=new ItemStack(Tinkerer.INFUSED_POTIONS.get("ignis").get());var remainder=potion.getItem().finishUsingItem(potion,world,player);test.assertTrue(remainder.is(Items.GLASS_BOTTLE)&&player.getEffect(ElementalEffects.EFFECTS.get("ignis").get()).getDuration()==3600,"Potion grants three minutes and returns exactly one bottle");
        var pos=test.absolutePos(new BlockPos(2,2,2));world.setBlock(pos,Tinkerer.FORCEFIELD.get().defaultBlockState(),3);test.assertTrue(!world.getBlockState(pos).getCollisionShape(world,pos).isEmpty(),"Temporary forcefield blocks motion");Tinkerer.FORCEFIELD.get().tick(world.getBlockState(pos),world,pos,world.random);test.assertTrue(world.isEmptyBlock(pos),"Forcefield expiration clears it");test.succeed();
    }
}
