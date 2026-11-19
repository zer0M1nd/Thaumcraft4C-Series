package tc4revival.content;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.gametest.*;
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class PavingStoneServerTests {
    private static WardingPavingBlockEntity ward(GameTestHelper test) {
        var pos=test.absolutePos(new BlockPos(4,4,4));test.getLevel().setBlockAndUpdate(pos,TCArcane.WARDING_PAVING.get().defaultBlockState());return (WardingPavingBlockEntity)test.getLevel().getBlockEntity(pos);
    }
    @GameTest(template="wand_test_empty")
    public static void pavingTravelEffectsAndRecipes(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(4,4,4));var travel=TCArcane.TRAVEL_PAVING.get().defaultBlockState();level.setBlockAndUpdate(pos,travel);var sheep=EntityType.SHEEP.create(level);
        travel.getBlock().stepOn(level,pos,travel,sheep);test.assertTrue(sheep.getEffect(MobEffects.MOVEMENT_SPEED).getDuration()==40&&sheep.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier()==1&&sheep.getEffect(MobEffects.JUMP).getDuration()==40&&sheep.getEffect(MobEffects.JUMP).getAmplifier()==0,"Travel stone grants forty ticks of speed II and jump I to living entities");
        sheep.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,200,3));travel.getBlock().stepOn(level,pos,travel,sheep);test.assertTrue(sheep.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier()==3,"Stronger existing speed remains intact");
        test.assertTrue(travel.getLightEmission(level,pos)==9&&!travel.isValidSpawn(level,pos,EntityType.ZOMBIE)&&!TCArcane.WARDING_PAVING.get().defaultBlockState().isValidSpawn(level,pos,EntityType.ZOMBIE),"Travel light and both spawn exclusions match old paving");
        var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,0){public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int i){return ItemStack.EMPTY;}};
        for(boolean ward:new boolean[]{false,true}) {
            var grid=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,3);for(int slot:new int[]{0,2,3,5})grid.setItem(slot,new ItemStack(TCItems.ARCANE_STONE_BRICKS.get()));grid.setItem(1,new ItemStack(TCItems.shard(ward?ShardType.FIRE:ShardType.AIR)));grid.setItem(4,new ItemStack(TCItems.shard(ward?ShardType.ORDER:ShardType.EARTH)));
            String name=ward?"warding_paving_stone":"travel_paving_stone",research=ward?"PAVEWARD":"PAVETRAVEL";var recipe=level.getRecipeManager().getAllRecipesFor(TCArcane.RECIPE_TYPE.get()).stream().filter(r->r.getId().getPath().endsWith(name)).findFirst().orElseThrow();var output=recipe.assemble(grid,level.registryAccess());
            test.assertTrue(recipe.matches(grid,level)&&output.getCount()==4&&output.is(ward?TCItems.WARDING_PAVING.get():TCItems.TRAVEL_PAVING.get())&&recipe.visCost(grid).size()==2&&recipe.visCost(grid).values().stream().allMatch(v->v==10)&&recipe.unlocked(grid,k->k.equals(research))&&!recipe.unlocked(grid,k->false),"Actual four-output paving recipe and research gate");
        }
        pass(test,"travel-effects-recipes");
    }
    @GameTest(template="wand_test_empty")
    public static void pavingBarrierCollisionAndCleanup(GameTestHelper test) {
        var tile=ward(test);var level=test.getLevel();var pos=tile.getBlockPos();for(int i=0;i<100;i++)tile.tick();var barrier=level.getBlockState(pos.above());
        test.assertTrue(barrier.is(TCArcane.WARDING_BARRIER.get())&&level.getBlockState(pos.above(2)).is(TCArcane.WARDING_BARRIER.get()),"Periodic update fills two barrier cells");
        test.assertTrue(net.minecraft.world.level.levelgen.feature.TreeFeature.validTreePos(level,pos.above()),"Modern tree replacement preserves the old leaves-replaceable barrier");
        var sheep=EntityType.SHEEP.create(level);var player=net.minecraftforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"PavingTest"));var item=new net.minecraft.world.entity.item.ItemEntity(level,0,0,0,new ItemStack(Items.STONE));
        test.assertTrue(!barrier.getCollisionShape(level,pos.above(),CollisionContext.of(sheep)).isEmpty()&&barrier.getCollisionShape(level,pos.above(),CollisionContext.of(player)).isEmpty()&&barrier.getCollisionShape(level,pos.above(),CollisionContext.of(item)).isEmpty()&&barrier.getShape(level,pos.above()).isEmpty(),"Only non-player living entities collide; selection is empty");
        test.assertTrue(!barrier.isPathfindable(level,pos.above(),net.minecraft.world.level.pathfinder.PathComputationType.LAND),"Active ward obstructs mob navigation");
        sheep.setPos(pos.getX()-.6,pos.getY()+1,pos.getZ()+.5);sheep.move(MoverType.SELF,new Vec3(1,0,0));test.assertTrue(sheep.getX()<pos.getX(),"Actual entity motion collides with the barrier");
        level.setBlockAndUpdate(pos.east(),Blocks.REDSTONE_BLOCK.defaultBlockState());test.assertTrue(barrier.getCollisionShape(level,pos.above(),CollisionContext.of(sheep)).isEmpty()&&barrier.isPathfindable(level,pos.above(),net.minecraft.world.level.pathfinder.PathComputationType.LAND),"Powered ward permits collision and path passage");
        sheep.setPos(pos.getX()-.6,pos.getY()+1,pos.getZ()+.5);sheep.move(MoverType.SELF,new Vec3(1,0,0));test.assertTrue(sheep.getX()>pos.getX(),"Powered device permits actual passage");level.setBlockAndUpdate(pos.east(),Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());test.assertTrue(level.getBlockState(pos.above()).is(TCArcane.WARDING_BARRIER.get()),"Removing the ward leaves the old cleanup delay");
        for(int h=1;h<=2;h++){var fence=(WardingBarrierBlockEntity)level.getBlockEntity(pos.above(h));for(int i=0;i<100&&!fence.isRemoved();i++)fence.tick();test.assertTrue(level.isEmptyBlock(pos.above(h)),"Orphaned barrier removes itself within one hundred ticks");}
        pass(test,"barrier-collision-redstone-cleanup");
    }
    @GameTest(template="wand_test_empty")
    public static void pavingRepulsionAndLightException(GameTestHelper test) {
        var tile=ward(test);var level=test.getLevel();var pos=tile.getBlockPos();level.setBlockAndUpdate(pos.above(),TCArcane.LAMP_AIR.get().defaultBlockState());level.setBlockAndUpdate(pos.above(2),Blocks.STONE.defaultBlockState());for(int i=0;i<100;i++)tile.tick();test.assertTrue(level.getBlockState(pos.above()).is(TCArcane.LAMP_AIR.get())&&level.getBlockState(pos.above(2)).is(Blocks.STONE),"Warding does not overwrite lamp air or nonreplaceable blocks");
        var sheep=EntityType.SHEEP.create(level);sheep.setNoAi(true);sheep.setNoGravity(true);sheep.setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5);level.addFreshEntity(sheep);
        test.runAfterDelay(2,()->{
            sheep.setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5);sheep.setYRot(0);sheep.setOnGround(false);sheep.setDeltaMovement(Vec3.ZERO);for(int i=0;i<20;i++)tile.tick();test.assertTrue(sheep.getDeltaMovement().z<-.7&&sheep.getDeltaMovement().y<-.3,"Airborne non-player mobs receive backward and downward impulses every five ticks");
            sheep.setDeltaMovement(Vec3.ZERO);sheep.setOnGround(true);for(int i=0;i<20;i++)tile.tick();test.assertTrue(sheep.getDeltaMovement().equals(Vec3.ZERO),"Grounded mobs receive no impulse");
            sheep.setOnGround(false);level.setBlockAndUpdate(pos.east(),Blocks.REDSTONE_BLOCK.defaultBlockState());for(int i=0;i<20;i++)tile.tick();test.assertTrue(sheep.getDeltaMovement().equals(Vec3.ZERO),"Redstone also disables airborne impulses");sheep.discard();pass(test,"repulsion-light-exception");
        });
    }
    private static void pass(GameTestHelper test,String name) { com.mojang.logging.LogUtils.getLogger().info("Paving stone test passed: {}",name);test.succeed(); }
}
