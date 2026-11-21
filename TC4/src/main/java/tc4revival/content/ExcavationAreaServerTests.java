package tc4revival.content;

import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class ExcavationAreaServerTests {
    @GameTest(template="wand_test_empty")
    public static void excavationAreaAndCancellationPreserveDebitOrder(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));var focus=TCItems.EXCAVATION_FOCUS.get();
        var player=new FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ExcavationArea"));player.setGameMode(GameType.SURVIVAL);
        player.moveTo(pos.getX()+.5,pos.getY()+2,pos.getZ()+.5,0,90);int cases=0;
        try {
            for(int rank=0;rank<=2;rank++)for(int earth:new int[]{0,14,15,30,45})for(int cancel=0;cancel<3;cancel++){
                for(var direction:Direction.values())level.setBlock(pos.relative(direction),direction==Direction.UP?Blocks.AIR.defaultBlockState():direction==Direction.NORTH?Blocks.GOLD_ORE.defaultBlockState():Blocks.STONE.defaultBlockState(),2);
                level.setBlock(pos,Blocks.STONE.defaultBlockState(),2);
                var core=new ItemStack(focus);if(rank>0)focus.applyUpgrade(core,FocusUpgrade.ENLARGE,2);if(rank>1)focus.applyUpgrade(core,FocusUpgrade.ENLARGE,4);
                var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.GREATWOOD,WandParts.Cap.GOLD,false,true);wand.getOrCreateTag().putInt("terra",earth);
                WandFocusItem.attach(wand,core);player.setItemInHand(InteractionHand.MAIN_HAND,wand);int mode=cancel;
                Consumer<BlockEvent.BreakEvent> reject=event->{if(event.getLevel()==level&&event.getPlayer()==player&&(mode==1&&event.getPos().equals(pos)||mode==2&&!event.getPos().equals(pos)))event.setCanceled(true);};
                MinecraftForge.EVENT_BUS.addListener(reject);
                try {
                    focus.cast(wand,level,player,InteractionHand.MAIN_HAND,null);
                    // Initial acquisition + six 0.25 increments for stone hardness 1.5 + completion.
                    for(int tick=0;tick<8;tick++)focus.usingTick(wand,player,100-tick);
                    boolean enough=earth>=15;int extra=enough&&mode==0?Math.min(rank,(earth-15)/15):0;
                    test.assertTrue(level.isEmptyBlock(pos)==(enough&&mode!=1),"Main target permission and initial funding");
                    int removed=0;for(var side:new Direction[]{Direction.DOWN,Direction.SOUTH,Direction.EAST,Direction.WEST})if(level.isEmptyBlock(pos.relative(side)))removed++;
                    test.assertTrue(removed==extra,"One same-state neighbor per affordable rank; protected neighbors never charged");
                    test.assertTrue(level.getBlockState(pos.north()).is(Blocks.GOLD_ORE),"Different neighbor block remains");
                    test.assertTrue(wand.getTag().getInt("terra")==earth-(enough?15:0)-extra*15,"Center debited before cancellation, neighbor debited only after success");cases++;
                } finally {MinecraftForge.EVENT_BUS.unregister(reject);focus.release(wand,level,player,0);}
            }
        } finally {
            level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(3)).forEach(ItemEntity::discard);
            level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);for(var side:Direction.values())level.setBlock(pos.relative(side),Blocks.AIR.defaultBlockState(),2);
        }
        tc4revival.TC4Revival.LOGGER.info("PASS excavationAreaAndCancellationPreserveDebitOrder: {} rank/funding/Forge-cancel combinations",cases);test.succeed();
    }
}
