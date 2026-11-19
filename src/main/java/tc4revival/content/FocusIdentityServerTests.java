package tc4revival.content;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import tc4revival.TC4Revival;

/** Reproduces integrated-server Entity.equals collisions without opening a client. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class FocusIdentityServerTests {
    private static FakePlayer player(GameTestHelper test,String name) {
        return new FakePlayer(test.getLevel(),new GameProfile(UUID.randomUUID(),name));
    }
    private static ItemStack equip(FakePlayer player,WandFocusItem focus) {
        var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.GREATWOOD,WandParts.Cap.GOLD,false,true);
        WandFocusItem.attach(wand,new ItemStack(focus));player.setItemInHand(InteractionHand.MAIN_HAND,wand);return wand;
    }
    @GameTest(template="wand_test_empty")
    public static void equalNetworkIdsDoNotStealFocusCooldownOrSuppressCombat(GameTestHelper test) {
        var level=test.getLevel();var player=player(test,"FocusServer");var twin=player(test,"FocusOther");twin.setId(player.getId());
        test.assertTrue(twin!=player && twin.equals(player),"Fixture must reproduce Entity network-ID equality");
        var pos=test.absolutePos(new BlockPos(3,4,1));player.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
        var cow=EntityType.COW.create(level);cow.moveTo(player.position().add(0,0,5));cow.setNoAi(true);level.addFreshEntity(cow);
        try {
            WandCooldowns.clear(twin);test.assertTrue(WandCooldowns.begin(twin,60000),"Other logical player owns a cooldown");
            for(var focus:new WandFocusItem[]{TCItems.FROST_FOCUS.get(),TCItems.PECH_FOCUS.get(),TCItems.PRIMAL_FOCUS.get(),TCItems.HELLBAT_FOCUS.get()}) {
                WandCooldowns.clear(player);var wand=equip(player,focus);
                var type=focus instanceof FrostFocusItem?FrostShardEntity.class:focus instanceof PechFocusItem?PechBlastEntity.class:focus instanceof PrimalFocusItem?PrimalOrbEntity.class:FireBatEntity.class;
                var region=player.getBoundingBox().inflate(8);
                var before=level.getEntitiesOfClass(Entity.class,region,entity->type.isInstance(entity));
                wand.use(level,player,InteractionHand.MAIN_HAND);
                var after=level.getEntitiesOfClass(Entity.class,region,entity->type.isInstance(entity));
                test.assertTrue(after.size()==before.size()+1,"Real wand use must spawn "+focus.getDescriptionId()+" despite equal-ID player's cooldown");
                after.forEach(Entity::discard);
                test.assertTrue(WandCooldowns.secondsRemaining(twin)>50,"Clearing/using one object cannot clear the other object's cooldown");
            }
            WandCooldowns.clear(player);var shock=equip(player,TCItems.SHOCK_FOCUS.get());shock.use(level,player,InteractionHand.MAIN_HAND);
            float health=cow.getHealth();TCItems.CASTING_WAND.get().onUseTick(level,player,shock,Integer.MAX_VALUE-1);
            test.assertTrue(cow.getHealth()<health,"Actual shock using callback damages pointed living target despite equal-ID cooldown");
        } finally { cow.discard();WandCooldowns.clear(player);WandCooldowns.clear(twin); }
        TC4Revival.LOGGER.info("PASS equalNetworkIdsDoNotStealFocusCooldownOrSuppressCombat");test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void animationBeforeUseDoesNotConsumeProjectileFocusCooldown(GameTestHelper test) {
        var level=test.getLevel();var caster=player(test,"FocusPacketOrder");
        var pos=test.absolutePos(new BlockPos(3,4,1));caster.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
        var target=EntityType.COW.create(level);target.moveTo(caster.position().add(0,0,5));target.setNoAi(true);
        test.assertTrue(level.addFreshEntity(target),"Target joins the test world");
        var spawned=new java.util.ArrayList<Entity>();
        try {
            for(var focus:new WandFocusItem[]{TCItems.FROST_FOCUS.get(),TCItems.PECH_FOCUS.get(),TCItems.PRIMAL_FOCUS.get(),TCItems.HELLBAT_FOCUS.get()}) {
                WandCooldowns.clear(caster);var wand=equip(caster,focus);var before=wand.copy();
                var item=TCItems.CASTING_WAND.get();
                // Minecraft predicts Item.use before sending its use packet; a
                // focus's player.swing can send the animation packet first.
                item.onEntitySwing(wand,caster);
                item.onBlockStartBreak(wand,pos,caster);
                test.assertTrue(WandCooldowns.secondsRemaining(caster)==0,"Animation and mining callbacks cannot reserve cast cooldown");
                var type=focus instanceof FrostFocusItem?FrostShardEntity.class:focus instanceof PechFocusItem?PechBlastEntity.class:focus instanceof PrimalFocusItem?PrimalOrbEntity.class:FireBatEntity.class;
                var region=caster.getBoundingBox().inflate(8);
                wand.use(level,caster,InteractionHand.MAIN_HAND);
                var shots=level.getEntitiesOfClass(Entity.class,region,entity->type.isInstance(entity));spawned.addAll(shots);
                test.assertTrue(shots.size()==1,"Animation-before-use must still spawn "+focus.getDescriptionId());
                test.assertTrue(tc4revival.aspect.TCAspects.catalog().primals().stream().anyMatch(aspect->item.vis(wand,aspect)<item.vis(before,aspect)),"Successful cast pays vis");
                var paid=wand.copy();
                item.onEntitySwing(wand,caster);
                item.onBlockStartBreak(wand,pos,caster);
                wand.use(level,caster,InteractionHand.MAIN_HAND);
                test.assertTrue(level.getEntitiesOfClass(Entity.class,region,entity->type.isInstance(entity)).size()==1,"Later animations cannot clear cooldown to duplicate a cast");
                test.assertTrue(ItemStack.matches(wand,paid),"Cooldown-rejected use cannot debit more vis");
                shots.forEach(Entity::discard);
            }
        } finally {target.discard();spawned.forEach(Entity::discard);WandCooldowns.clear(caster);}
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void equalNetworkIdReleaseDoesNotResetStationaryExcavation(GameTestHelper test) {
        var player=player(test,"MiningServer");var twin=player(test,"MiningOther");twin.setId(player.getId());
        var block=test.absolutePos(new BlockPos(3,3,3));var level=test.getLevel();level.setBlockAndUpdate(block,Blocks.GRASS_BLOCK.defaultBlockState());
        player.moveTo(block.getX()+.5,block.getY()+2,block.getZ()+.5,0,90);
        var focus=TCItems.EXCAVATION_FOCUS.get();var wand=equip(player,focus);var other=equip(twin,focus);
        try {
            for(int tick=0;tick<12 && !level.isEmptyBlock(block);tick++) {
                focus.release(other,level,twin,100); // Other-side completion/release must not remove this player's work.
                focus.usingTick(wand,player,100-tick);
            }
            test.assertTrue(level.isEmptyBlock(block),"Standing still must finish grass excavation while an equal-ID object resets its own progress");
        } finally { focus.release(wand,level,player,0);focus.release(other,level,twin,0); }
        TC4Revival.LOGGER.info("PASS equalNetworkIdReleaseDoesNotResetStationaryExcavation");test.succeed();
    }
}
