package tc4revival.content;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class ShockLifecycleServerTests {
    @GameTest(template="wand_test_empty")
    public static void shockAcquisitionMatchesLegacy(GameTestHelper test) {
        var level=test.getLevel();var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"ShockRecipe"));var grid=new net.minecraft.world.inventory.TransientCraftingContainer(player.inventoryMenu,3,3);
        for(int slot=0;slot<9;slot++)grid.setItem(slot,new ItemStack(slot==4?Items.POTATO:slot%2==0?TCItems.shard(ShardType.AIR):Items.QUARTZ));
        var recipe=level.getRecipeManager().getAllRecipesFor(TCArcane.RECIPE_TYPE.get()).stream().filter(r->r.getId().getPath().endsWith("focus_shock")).findFirst().orElseThrow();var output=recipe.assemble(grid,level.registryAccess());
        test.assertTrue(recipe.matches(grid,level)&&output.is(TCItems.SHOCK_FOCUS.get())&&output.getCount()==1,"Four air shards, four quartz and potato give one shock focus");var aspects=tc4revival.aspect.TCAspects.catalog();test.assertTrue(recipe.visCost(grid).equals(Map.of(aspects.require("aer"),10,aspects.require("ordo"),10,aspects.require("perditio"),10)),"Legacy three ten-vis acquisition costs");test.assertTrue(recipe.unlocked(grid,k->k.equals("FOCUSSHOCK"))&&!recipe.unlocked(grid,k->false),"Research gate");
        for(int slot=0;slot<9;slot++){var held=grid.getItem(slot);grid.setItem(slot,ItemStack.EMPTY);test.assertTrue(!recipe.matches(grid,level),"All nine ingredients required");grid.setItem(slot,held);}
        tc4revival.TC4Revival.LOGGER.info("PASS shockAcquisitionMatchesLegacy: actual loaded recipe, research, costs and nine missing-slot cases");test.succeed();
    }
    @GameTest(template="wand_test_empty",timeoutTicks=100)
    public static void shockOrbDeflectionLifetimeAndReloadPreserveLegacy(GameTestHelper test) {
        var level=test.getLevel();var base=test.absolutePos(new BlockPos(3,18,3));var center=Vec3.atCenterOf(base);int cx=base.getX()>>4,cz=base.getZ()>>4;boolean forced=level.setChunkForced(cx,cz,true);var owner=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"ShockLife"));owner.moveTo(center.add(0,8,0));owner.setYRot(90);owner.setXRot(20);
        test.runAfterDelay(20,()->{var targets=new ArrayList<Entity>();try {
            var source=new ShockOrbEntity(level,owner,8,9);source.setPos(center);source.setDeltaMovement(1,2,3);test.assertTrue(!source.hurt(level.damageSources().generic(),1)&&source.getDeltaMovement().equals(new Vec3(1,2,3)),"Unattributed damage cannot deflect");test.assertTrue(source.hurt(level.damageSources().playerAttack(owner),1)&&source.getDeltaMovement().distanceTo(owner.getLookAngle().scale(.9))<.000001,"Attributed hit sets speed .9 and attacker look");
            var saved=new CompoundTag();source.saveWithoutId(saved);var loaded=new ShockOrbEntity(TCEntities.SHOCK_ORB.get(),level);loaded.load(saved);loaded.setPos(center);
            var near=EntityType.COW.create(level);near.moveTo(center.add(2,0,0));level.addFreshEntity(near);targets.add(near);var far=EntityType.COW.create(level);far.moveTo(center.add(6,0,0));level.addFreshEntity(far);targets.add(far);
            test.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,new AABB(base).inflate(8)).containsAll(targets),"Both world targets queryable");loaded.onHit(new BlockHitResult(center,Direction.UP,base.below(),false));test.assertTrue(near.getHealth()==near.getMaxHealth()-9&&far.getHealth()==far.getMaxHealth()-9,"Reload preserves upgraded damage and range");
            source.setDeltaMovement(Vec3.ZERO);source.tickCount=500;source.tick();test.assertTrue(!source.isRemoved(),"Orb survives age 500");source.tickCount=501;source.tick();test.assertTrue(source.isRemoved(),"Orb expires at 501");
            tc4revival.TC4Revival.LOGGER.info("PASS shockOrbDeflectionLifetimeAndReloadPreserveLegacy: attributed deflection, 500/501 boundary and actual default damage/range after NBT load");test.succeed();
        }finally{targets.forEach(Entity::discard);if(forced)level.setChunkForced(cx,cz,false);}});
    }
}
