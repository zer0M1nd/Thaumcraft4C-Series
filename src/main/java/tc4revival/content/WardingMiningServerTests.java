package tc4revival.content;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.*;
import net.minecraft.network.protocol.*;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class WardingMiningServerTests {
    @GameTest(template="wand_test_empty")
    public static void wardingServerMiningActionsRejectSurvivalAndAllowCreative(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(4,20,4));var previous=level.getBlockState(pos);
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"WardMining"));
        var packets=new ArrayList<Packet<?>>();
        new ServerGamePacketListenerImpl(player.server,new Connection(PacketFlow.SERVERBOUND){@Override public void send(Packet<?> packet){packets.add(packet);}},player);
        player.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()-2,0,0);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_PICKAXE));
        int sequence=0;
        try {
            for(var mode:List.of(GameType.SURVIVAL,GameType.ADVENTURE,GameType.SPECTATOR,GameType.CREATIVE)) {
                player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
                level.setBlock(pos,Blocks.STONE.defaultBlockState(),2);test.assertTrue(WardedBlockEntity.protect(player,pos),"Protected fixture for "+mode);
                var ward=level.getBlockEntity(pos);player.gameMode.changeGameModeForPlayer(mode);
                player.gameMode.handleBlockBreakAction(pos,Action.START_DESTROY_BLOCK,Direction.NORTH,level.getMaxBuildHeight(),++sequence);
                for(int tick=0;tick<80;tick++)player.gameMode.tick();
                player.gameMode.handleBlockBreakAction(pos,Action.STOP_DESTROY_BLOCK,Direction.NORTH,level.getMaxBuildHeight(),++sequence);
                for(int tick=0;tick<10;tick++)player.gameMode.tick();
                test.assertTrue(mode==GameType.CREATIVE?level.getBlockState(pos).isAir():level.getBlockEntity(pos)==ward,"Actual start/stop mining honors mode: "+mode);
                player.gameMode.handleBlockBreakAction(pos,Action.ABORT_DESTROY_BLOCK,Direction.NORTH,level.getMaxBuildHeight(),++sequence);
            }
            player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);level.setBlock(pos,Blocks.STONE.defaultBlockState(),2);
            player.gameMode.handleBlockBreakAction(pos,Action.START_DESTROY_BLOCK,Direction.NORTH,level.getMaxBuildHeight(),++sequence);
            for(int tick=0;tick<80;tick++)player.gameMode.tick();
            player.gameMode.handleBlockBreakAction(pos,Action.STOP_DESTROY_BLOCK,Direction.NORTH,level.getMaxBuildHeight(),++sequence);
            test.assertTrue(level.getBlockState(pos).isAir(),"Same survival action path mines ordinary stone control");
            tc4revival.TC4Revival.LOGGER.info("PASS wardingServerMiningActionsRejectSurvivalAndAllowCreative: four game modes start/stop/abort actions, gamemode ticks and ordinary survival stone control");test.succeed();
        } finally {level.setBlock(pos,previous,2);level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(3)).forEach(net.minecraft.world.entity.Entity::discard);}
    }
}
