package tc4revival.content;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class TaintBottleServerTests {
    @GameTest(template="wand_test_empty")
    public static void taintBottleImpactAndBiome(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(4,4,4));
        var cow=EntityType.COW.create(level);var zombie=EntityType.ZOMBIE.create(level);var outside=EntityType.COW.create(level);var tainted=TCEntities.TAINT_SPIDER.get().create(level);
        cow.setPos(pos.getX()+4.5,pos.getY(),pos.getZ()+4.5);zombie.setPos(pos.getX(),pos.getY(),pos.getZ());outside.setPos(pos.getX()+7,pos.getY(),pos.getZ());
        tainted.setPos(pos.getX()+1,pos.getY(),pos.getZ());level.addFreshEntity(tainted);level.addFreshEntity(cow);level.addFreshEntity(zombie);level.addFreshEntity(outside);
        var origins=new java.util.HashMap<BlockPos,net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome>>();
        for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++) {
            var p=pos.offset(x,0,z); origins.put(p,level.getBiome(p).unwrapKey().orElseThrow());
            level.setBlock(p.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
            level.setBlock(p,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
        }
        WorldBiomeChanges.changeColumns(level,origins.keySet(),net.minecraft.world.level.biome.Biomes.PLAINS);
        try {
            var bottle=new TaintBottleEntity(TCEntities.TAINT_BOTTLE.get(),level);bottle.setPos(pos.getX()+.2,pos.getY(),pos.getZ()+.2);bottle.seedImpact(921);level.random.setSeed(718);
            bottle.onHit(new BlockHitResult(bottle.position(),Direction.UP,pos.below(),false));
            test.assertTrue(bottle.isRemoved() && cow.hasEffect(TCEffects.FLUX_TAINT.get()) && cow.getEffect(TCEffects.FLUX_TAINT.get()).getDuration()==100,"Impact applies 100 ticks throughout the cube, including diagonal corners outside a sphere");
            test.assertTrue(!zombie.hasEffect(TCEffects.FLUX_TAINT.get()) && !outside.hasEffect(TCEffects.FLUX_TAINT.get()) && !tainted.hasEffect(TCEffects.FLUX_TAINT.get()),"Undead and out-of-range entities are excluded");
            test.assertTrue(origins.keySet().stream().anyMatch(p->level.getBiome(p).is(TCBiomes.TAINTED_LAND)) && origins.keySet().stream().anyMatch(p->level.getBlockState(p).is(TCArcane.TAINT_FIBRES.get())),"Impact changes biome and places supported fibres");
            cow.removeAllEffects();bottle.onHit(new EntityHitResult(cow));test.assertTrue(!cow.hasEffect(TCEffects.FLUX_TAINT.get()),"Removed projectile cannot repeat impact");
        } finally {cow.discard();zombie.discard();outside.discard();tainted.discard();origins.forEach((p,biome)->WorldBiomeChanges.changeColumn(level,p,biome));}
        pass(test,"impact-biome-immunity");
    }
    @GameTest(template="wand_test_empty")
    public static void taintBottleRecipeAndThrow(GameTestHelper test) {
        var level=test.getLevel();var item=TCItems.TAINT_BOTTLE.get();
        var costs=new AspectAmounts().add(TCAspects.catalog().require("vitium"),8).add(TCAspects.catalog().require("praecantatio"),8);
        var input=new CrucibleRecipe.Input(EssentiaPhialItem.filled(TCAspects.catalog().require("aer")),costs,key->key.equals("BOTTLETAINT"));
        var recipe=CrucibleRecipe.find(level,input).orElseThrow();
        test.assertTrue(recipe.assemble(input,level.registryAccess()).is(item),"Old catalyst matcher accepts any filled phial despite the displayed taint aspect");
        test.assertTrue(!recipe.catalystMatches(new ItemStack(TCItems.EMPTY_PHIAL.get())) && !recipe.matches(new CrucibleRecipe.Input(input.getItem(0),costs,key->false),level),"Empty phial and missing research are rejected");
        var stack=new ItemStack(item,8);test.assertTrue(stack.getMaxStackSize()==8 && ScribingToolsItem.accepts(stack) && ScribingToolsItem.ink(stack)==0,"Bottle retains old scribing marker but has no usable ink");
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"BottleTest"));
        var pos=test.absolutePos(new BlockPos(3,4,3));player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);player.setXRot(0);player.setYRot(0);player.getAbilities().instabuild=false;
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);item.use(level,player,net.minecraft.world.InteractionHand.MAIN_HAND);
        test.assertTrue(stack.getCount()==7,"Survival throw consumes one bottle");
        player.getAbilities().instabuild=true;item.use(level,player,net.minecraft.world.InteractionHand.MAIN_HAND);test.assertTrue(stack.getCount()==7,"Creative throw preserves stack");
        var thrown=level.getEntitiesOfClass(TaintBottleEntity.class,new AABB(pos).inflate(3));
        test.assertTrue(thrown.size()==2 && thrown.stream().allMatch(bottle->bottle.getOwner()==player && bottle.getDeltaMovement().y>0 && Math.abs(bottle.getDeltaMovement().length()-.5)<.04),"Both uses spawn owned upward-angled projectiles at old launch speed");
        thrown.forEach(Entity::discard);pass(test,"recipe-throw-scribing");
    }
    private static void pass(GameTestHelper test,String name){com.mojang.logging.LogUtils.getLogger().info("Taint bottle test passed: {}",name);test.succeed();}
}
