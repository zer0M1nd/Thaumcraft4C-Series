package tc4revival.content;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;
import tc4revival.network.ElementalProspectingPacket;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class ElementalProspectingServerTests {
    @GameTest(template="wand_test_empty")
    public static void elementalProspectingSnapshotAndUse(GameTestHelper test) {
        var level=test.getLevel();var center=test.absolutePos(new BlockPos(3,3,3));
        var states=new LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        var positions=List.of(center,center.east(2),center.east(4),center.offset(8,8,8),center.offset(9,0,0),center.west(2),center.west(4));
        for(var pos:positions)states.put(pos,level.getBlockState(pos));
        try {
            level.setBlock(center,Blocks.STONE.defaultBlockState(),18);level.setBlock(center.east(2),Blocks.STONE.defaultBlockState(),18);
            level.setBlock(center.east(4),Blocks.IRON_ORE.defaultBlockState(),18);
            level.setBlock(center.offset(8,8,8),Blocks.GOLD_ORE.defaultBlockState(),18);level.setBlock(center.offset(9,0,0),Blocks.DIAMOND_ORE.defaultBlockState(),18);
            level.setBlock(center.west(2),Blocks.WATER.defaultBlockState(),18);level.setBlock(center.west(4),Blocks.LAVA.defaultBlockState(),18);
            var snapshot=ElementalProspecting.scan(level,center);
            test.assertTrue(snapshot.stream().anyMatch(mark->mark.x()==4&&mark.y()==0&&mark.z()==0&&mark.value()>0),"Ore behind solid stone remains detectable");
            test.assertTrue(snapshot.stream().anyMatch(mark->mark.x()==8&&mark.y()==8&&mark.z()==8),"Snapshot includes all corners of the old seventeen-cube");
            test.assertTrue(snapshot.stream().noneMatch(mark->Math.abs(mark.x())>8||Math.abs(mark.y())>8||Math.abs(mark.z())>8),"No sample outside radius-eight cube");
            test.assertTrue(snapshot.stream().anyMatch(mark->mark.x()==-2&&mark.value()==-5) && snapshot.stream().anyMatch(mark->mark.x()==-4&&mark.value()==-10),"Water and lava keep old sentinel values");
            test.assertTrue(snapshot.stream().noneMatch(mark->mark.x()==0&&mark.y()==0&&mark.z()==0),"Ordinary stone is absent");
            int expected=(int)tc4revival.research.ScanTargetResolver.block(level,center.east(4)).orElseThrow().aspects().total();
            test.assertTrue(snapshot.stream().filter(mark->mark.x()==4&&mark.y()==0&&mark.z()==0).findFirst().orElseThrow().value()==expected,"Prospecting uses the actual block aspect definition");
            var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ProspectTest"));player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            var pick=new ItemStack(TCItems.ELEMENTAL_PICKAXE.get());player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,pick);
            var context=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(center),Direction.UP,center,false));
            var result=pick.getItem().useOn(context);
            test.assertTrue(result==net.minecraft.world.InteractionResult.PASS && pick.getDamageValue()==5,"Real use callback preserves five durability cost and PASS result");
            test.assertTrue(level.getBlockState(center).is(Blocks.STONE),"Prospecting does not mine its target");
        } finally { states.forEach((pos,state)->level.setBlock(pos,state,18)); }
        passed(test,"snapshot-and-use");
    }
    @GameTest(template="wand_test_empty")
    public static void elementalProspectingPacketBounds(GameTestHelper test) {
        var packet=new ElementalProspectingPacket(test.getLevel().dimension().location(),new BlockPos(-15,-61,20),List.of(new ElementalProspecting.Mark(-8,8,0,-10),new ElementalProspecting.Mark(0,0,0,0),new ElementalProspecting.Mark(8,-8,8,20)));
        var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            packet.encode(buffer);test.assertTrue(ElementalProspectingPacket.decode(buffer).equals(packet),"Packet preserves signed offsets, liquid sentinels and ore values");
            buffer.clear();buffer.writeResourceLocation(packet.dimension());buffer.writeBlockPos(packet.center());buffer.writeVarInt(4914);
            boolean rejected=false;try{ElementalProspectingPacket.decode(buffer);}catch(IllegalArgumentException expected){rejected=true;}
            test.assertTrue(rejected,"Oversized volume rejected before allocating marks");
        } finally { buffer.release(); }
        passed(test,"packet-bounds");
    }
    private static void passed(GameTestHelper test,String name) { tc4revival.TC4Revival.LOGGER.info("Elemental prospecting PASS: {}",name);test.succeed(); }
}
