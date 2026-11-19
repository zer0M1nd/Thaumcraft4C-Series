package tc4revival.content;

import java.util.UUID;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class FrostContinuationServerTests {
    @GameTest(template="wand_test_empty")
    public static void frostAcquisitionMatchesLegacy(GameTestHelper test) {
        var level=test.getLevel();var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"FrostRecipe"));var grid=new net.minecraft.world.inventory.TransientCraftingContainer(player.inventoryMenu,3,3);
        for(int slot=0;slot<9;slot++)grid.setItem(slot,new ItemStack(slot==4?Items.DIAMOND:slot%2==0?TCItems.shard(ShardType.WATER):Items.QUARTZ));
        var recipe=level.getRecipeManager().getAllRecipesFor(TCArcane.RECIPE_TYPE.get()).stream().filter(r->r.getId().getPath().endsWith("focus_frost")).findFirst().orElseThrow();
        var output=recipe.assemble(grid,level.registryAccess());test.assertTrue(recipe.matches(grid,level)&&output.is(TCItems.FROST_FOCUS.get())&&output.getCount()==1,"Actual four water shards, four quartz and diamond");
        var aspects=tc4revival.aspect.TCAspects.catalog();test.assertTrue(recipe.visCost(grid).equals(java.util.Map.of(aspects.require("aqua"),10,aspects.require("ordo"),10,aspects.require("perditio"),10)),"Exact acquisition cost");test.assertTrue(recipe.unlocked(grid,k->k.equals("FOCUSFROST"))&&!recipe.unlocked(grid,k->false),"Exact research gate");
        for(int slot=0;slot<9;slot++){var held=grid.getItem(slot);grid.setItem(slot,ItemStack.EMPTY);test.assertTrue(!recipe.matches(grid,level),"Every ingredient required");grid.setItem(slot,held);}
        tc4revival.TC4Revival.LOGGER.info("PASS frostAcquisitionMatchesLegacy: loaded recipe, nine ingredients, research and vis");test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void frostOrientationUsesPostGravityMotionAndWrappedPreviousAngles(GameTestHelper test) {
        var level=test.getLevel();var owner=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"FrostAngles"));var pos=test.absolutePos(new BlockPos(3,15,3));owner.moveTo(Vec3.atCenterOf(pos));
        for(boolean fragile:new boolean[]{false,true})for(var velocity:new Vec3[]{new Vec3(0,0,1),new Vec3(.01,.2,-1),new Vec3(-.01,-.2,-1),new Vec3(1,0,0)})for(float previous:new float[]{0,179,-179}) {
            var shard=new FrostShardEntity(level,owner,1,1,0,fragile,false);shard.setPos(Vec3.atCenterOf(pos));shard.setDeltaMovement(velocity);shard.setYRot(previous);shard.setXRot(0);shard.yRotO=previous;shard.xRotO=0;shard.tick();
            var nativeProjectile=new net.minecraft.world.entity.projectile.Snowball(level,owner);
            nativeProjectile.setPos(Vec3.atCenterOf(pos));nativeProjectile.setDeltaMovement(velocity);
            nativeProjectile.setYRot(previous);nativeProjectile.setXRot(0);nativeProjectile.yRotO=previous;nativeProjectile.xRotO=0;nativeProjectile.tick();
            test.assertTrue(Math.abs(shard.getXRot()-nativeProjectile.getXRot())<.00001
                    &&Math.abs(shard.getYRot()-nativeProjectile.getYRot())<.00001
                    &&Math.abs(shard.yRotO-nativeProjectile.yRotO)<.00001,"Rotation matches the actual native projectile tick");
            shard.discard();nativeProjectile.discard();
        }
        tc4revival.TC4Revival.LOGGER.info("PASS frostOrientationUsesPostGravityMotionAndWrappedPreviousAngles: 24 gravity/heading/previous-angle cases");test.succeed();
    }
}
