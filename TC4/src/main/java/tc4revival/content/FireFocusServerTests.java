package tc4revival.content;

import java.util.UUID;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class FireFocusServerTests {
    @GameTest(template="wand_test_empty")
    public static void fireAcquisitionMatchesLegacy(GameTestHelper test) {
        var level=test.getLevel();var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"FireRecipe"));
        var grid=new net.minecraft.world.inventory.TransientCraftingContainer(player.inventoryMenu,3,3);
        for(int slot=0;slot<9;slot++)grid.setItem(slot,new ItemStack(slot==4?net.minecraft.world.item.Items.FIRE_CHARGE:slot%2==0?TCItems.shard(ShardType.FIRE):net.minecraft.world.item.Items.QUARTZ));
        var recipe=level.getRecipeManager().getAllRecipesFor(TCArcane.RECIPE_TYPE.get()).stream().filter(r->r.getId().getPath().endsWith("focus_fire")).findFirst().orElseThrow();
        var output=recipe.assemble(grid,level.registryAccess());test.assertTrue(recipe.matches(grid,level)&&output.is(TCItems.FIRE_FOCUS.get())&&output.getCount()==1,"Loaded four fire shards/four quartz/fire charge recipe");
        var aspects=tc4revival.aspect.TCAspects.catalog();test.assertTrue(recipe.visCost(grid).equals(java.util.Map.of(aspects.require("ignis"),20,aspects.require("perditio"),10)),"Exact legacy acquisition cost");
        test.assertTrue(recipe.unlocked(grid,key->key.equals("FOCUSFIRE"))&&!recipe.unlocked(grid,key->false),"Exact research gate");
        for(int slot=0;slot<9;slot++){var held=grid.getItem(slot);grid.setItem(slot,ItemStack.EMPTY);test.assertTrue(!recipe.matches(grid,level),"Every ingredient required");grid.setItem(slot,held);}
        tc4revival.TC4Revival.LOGGER.info("PASS fireAcquisitionMatchesLegacy: loaded recipe, research, vis and nine missing ingredients");test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void fireThreeModesSpawnAndPayLegacyCosts(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,8,3));
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"FireModes"));player.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
        var chunk=player.chunkPosition();boolean added=level.setChunkForced(chunk.x,chunk.z,true);
        var item=TCItems.FIRE_FOCUS.get();var wandItem=TCItems.CASTING_WAND.get();
        try {
            for(int mode=0;mode<3;mode++)for(int potency:new int[]{0,2}) {
                var focus=new ItemStack(item);
                if(potency==2){item.applyUpgrade(focus,FocusUpgrade.POTENCY,1);item.applyUpgrade(focus,FocusUpgrade.POTENCY,5);}
                item.applyUpgrade(focus,FocusUpgrade.ALCHEMISTS_FIRE,2);
                if(mode>0)item.applyUpgrade(focus,mode==1?FocusUpgrade.FIREBEAM:FocusUpgrade.FIREBALL,3);
                var wand=wandItem.create(WandParts.Rod.WOOD,WandParts.Cap.GOLD,false,true);WandFocusItem.attach(wand,focus);player.setItemInHand(InteractionHand.MAIN_HAND,wand);var before=wand.copy();
                WandCooldowns.clear(player);wand.use(level,player,InteractionHand.MAIN_HAND);
                if(mode<2)wandItem.onUseTick(level,player,wand,100);
                var spawned=level.getEntitiesOfClass(Entity.class,player.getBoundingBox().inflate(5),e->e instanceof EmberEntity||e instanceof ExplosiveOrbEntity);
                test.assertTrue(spawned.size()==(mode==2?1:2+potency),"Actual cast projectile count mode="+mode+" potency="+potency);
                for(var entity:spawned){
                    test.assertTrue(((net.minecraft.world.entity.projectile.Projectile)entity).getOwner()==player,"Projectile owns caster");
                    if(mode<2){var tag=new CompoundTag();entity.saveWithoutId(tag);test.assertTrue(entity instanceof EmberEntity&&tag.getInt("duration")== (mode==1?30:20)&&tag.getInt("firey")==1,"Ember duration and ignition level");float damage=mode==1?(2.5f+potency)*1.5f:2+potency;test.assertTrue(tag.getFloat("damage")==damage,"Ember potency damage");}
                    else test.assertTrue(entity instanceof ExplosiveOrbEntity,"Fireball mode creates orb");
                    entity.discard();
                }
                for(var aspect:tc4revival.aspect.TCAspects.catalog().primals())test.assertTrue(wandItem.vis(before,aspect)-wandItem.vis(wand,aspect)==switch(aspect.tag()){case "ignis"->mode==2?66:10;case "ordo"->mode==1?3:0;case "perditio"->mode==2?33:0;default->0;},"Installed mode pays its complete advertised cost");
                player.stopUsingItem();
            }
            var empty=wandItem.create(WandParts.Rod.WOOD,WandParts.Cap.GOLD,false,false);WandFocusItem.attach(empty,new ItemStack(item));player.setItemInHand(InteractionHand.MAIN_HAND,empty);player.startUsingItem(InteractionHand.MAIN_HAND);item.usingTick(empty,player,100);
            test.assertTrue(!player.isUsingItem()&&level.getEntitiesOfClass(EmberEntity.class,player.getBoundingBox().inflate(5)).isEmpty(),"Empty wand stops continuous cast without projectile");
            tc4revival.TC4Revival.LOGGER.info("PASS fireThreeModesSpawnAndPayLegacyCosts: six mode/potency casts and empty-wand boundary");test.succeed();
        } finally {player.stopUsingItem();WandCooldowns.clear(player);if(added)level.setChunkForced(chunk.x,chunk.z,false);}
    }

    @GameTest(template="wand_test_empty")
    public static void fireProjectilesDamageLifetimeAndPersistence(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,12,3));
        var owner=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"FireOwner"));owner.moveTo(pos.getX(),pos.getY(),pos.getZ(),90,0);
        for(int duration:new int[]{20,30}) {
            var ember=new EmberEntity(level,owner,0,4.5f,2,duration);var saved=new CompoundTag();ember.saveWithoutId(saved);var loaded=new EmberEntity(TCEntities.EMBER.get(),level);loaded.load(saved);
            test.assertTrue(loaded.duration()==duration,"Ember duration persists");loaded.setPos(owner.position());loaded.setDeltaMovement(1,0,0);loaded.tick();
            test.assertTrue(Math.abs(loaded.getX()-owner.getX()-(duration==20?.95:.975))<.00001,"Ember movement slows before vanilla projectile tick");
            loaded.tickCount=duration;loaded.tick();test.assertTrue(!loaded.isRemoved(),"Ember survives duration boundary");loaded.tickCount=duration+1;loaded.tick();test.assertTrue(loaded.isRemoved(),"Ember expires on next boundary callback");
            var cow=EntityType.COW.create(level);cow.moveTo(owner.position());ember.onHit(new EntityHitResult(cow));
            test.assertTrue(cow.getHealth()==cow.getMaxHealth()-4.5f&&cow.getRemainingFireTicks()==100&&ember.isRemoved(),"Saved ember damage, five-second ignition and discard");
            var immune=EntityType.BLAZE.create(level);var other=new EmberEntity(level,owner,0,4,2,duration);other.onHit(new EntityHitResult(immune));test.assertTrue(immune.getHealth()==immune.getMaxHealth()&&!immune.isOnFire()&&other.isRemoved(),"Fire-immune target takes no ember damage");
        }
        var ember=new EmberEntity(level,owner,0,1,0,20);var source=new net.minecraft.world.damagesource.DamageSource(level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(EmberEntity.DAMAGE_TYPE),ember,owner);
        test.assertTrue(source.is(DamageTypeTags.IS_FIRE)&&!source.is(DamageTypeTags.IS_PROJECTILE),"Embers preserve fire-only damage classification");
        test.assertTrue(!ember.hurt(level.damageSources().playerAttack(owner),1),"Ember cannot be deflected");ember.discard();
        for(float strength:new float[]{1,1.8f}) {
            var orb=new ExplosiveOrbEntity(level,owner,strength,true);var cow=EntityType.COW.create(level);cow.moveTo(owner.position());
            orb.onHit(new EntityHitResult(cow));test.assertTrue(Math.abs(cow.getHealth()-(cow.getMaxHealth()-strength*1.5f))<.0001&&orb.isRemoved(),"Orb direct hit scales strength before explosion");
        }
        var orb=new ExplosiveOrbEntity(level,owner,1.8f,true);test.assertTrue(!orb.hurt(level.damageSources().generic(),1),"Ownerless damage cannot deflect orb");test.assertTrue(orb.hurt(level.damageSources().playerAttack(owner),1)&&orb.getDeltaMovement().distanceTo(owner.getLookAngle().scale(.9))<.00001,"Deflection follows attacker look at speed .9");
        var tag=new CompoundTag();orb.saveWithoutId(tag);var loaded=new ExplosiveOrbEntity(TCEntities.EXPLOSIVE_ORB.get(),level);loaded.load(tag);var cow=EntityType.COW.create(level);loaded.onHit(new EntityHitResult(cow));test.assertTrue(Math.abs(cow.getHealth()-(cow.getMaxHealth()-2.7f))<.0001,"Upgraded strength survives NBT reload");
        orb.setPos(owner.position());orb.setDeltaMovement(Vec3.ZERO);orb.tickCount=500;orb.tick();test.assertTrue(!orb.isRemoved(),"Orb survives tick 500");orb.tickCount=501;orb.tick();test.assertTrue(orb.isRemoved(),"Orb expires after tick 500");
        tc4revival.TC4Revival.LOGGER.info("PASS fireProjectilesDamageLifetimeAndPersistence: ember hits, immunity, movement, lifetime, NBT; orb hits, deflection, lifetime and old reload behavior");test.succeed();
    }
}
