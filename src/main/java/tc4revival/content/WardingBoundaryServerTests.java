package tc4revival.content;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class WardingBoundaryServerTests {
    @GameTest(template="wand_test_empty")
    public static void wardingForeignAndMissingBridgesStopOwnerSelection(GameTestHelper test) {
        var level=test.getLevel();var origin=test.absolutePos(new BlockPos(4,20,4));
        var owner=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"WardBridge"));
        var foreign=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"OtherBridge"));
        var positions=List.of(origin,origin.east(),origin.east(2));
        var before=new LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();positions.forEach(p->before.put(p,level.getBlockState(p)));
        var focus=TCItems.WARDING_FOCUS.get();var core=new ItemStack(focus);focus.applyUpgrade(core,FocusUpgrade.ARCHITECT,2);
        var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.GREATWOOD,WandParts.Cap.GOLD,false,true);WandFocusItem.attach(wand,core);
        wand.getOrCreateTag().putInt("areax",3);wand.getOrCreateTag().putInt("areay",0);wand.getOrCreateTag().putInt("areaz",0);
        try {
            for(int scene=0;scene<4;scene++) {
                for(var p:positions){level.setBlock(p,Blocks.STONE.defaultBlockState(),2);test.assertTrue(WardedBlockEntity.protect(owner,p),"Owner fixture protected");}
                if(scene==0){level.setBlock(origin.east(),Blocks.STONE.defaultBlockState(),2);test.assertTrue(WardedBlockEntity.protect(foreign,origin.east()),"Foreign bridge protected");}
                if(scene==1)level.setBlock(origin.east(),Blocks.AIR.defaultBlockState(),2);
                if(scene==2)level.setBlock(origin.east(),Blocks.STONE.defaultBlockState(),2);
                var selected=focus.blocks(wand,level,origin,Direction.UP,owner);
                test.assertTrue(selected.equals(scene==3?positions:List.of(origin)),"Foreign, air and ordinary blocks disconnect remote owned ward: "+scene);
                test.assertTrue(focus.blocks(wand,level,origin,Direction.UP,foreign).isEmpty(),"Foreign origin cannot be selected");
            }
            tc4revival.TC4Revival.LOGGER.info("PASS wardingForeignAndMissingBridgesStopOwnerSelection: foreign/air/ordinary bridge exclusion and owned connected control");test.succeed();
        } finally {before.forEach((p,state)->level.setBlock(p,state,2));}
    }

    @GameTest(template="wand_test_empty")
    public static void wardingMalformedNbtAndRealPacketCodecPreserveSafeState(GameTestHelper test) {
        var pos=test.absolutePos(new BlockPos(4,20,4));var proxy=TCArcane.WARDED.get().defaultBlockState();
        var owner=UUID.randomUUID();var original=Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.Z);
        var valid=new CompoundTag();valid.put("Original",NbtUtils.writeBlockState(original));valid.putUUID("Owner",owner);
        var sender=new WardedBlockEntity(pos,proxy);sender.load(valid);
        var buffer=new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            sender.getUpdatePacket().write(buffer);var packet=new ClientboundBlockEntityDataPacket(buffer);
            test.assertTrue(buffer.readableBytes()==0&&packet.getPos().equals(pos)&&packet.getType()==TCArcane.WARDED_ENTITY.get(),"Actual packet codec preserves position and BE type");
            var receiver=new WardedBlockEntity(pos,proxy);receiver.load(packet.getTag());
            test.assertTrue(receiver.original().equals(original)&&owner.equals(receiver.owner()),"Packet NBT restores original properties and owner");
            test.assertTrue(sender.getUpdateTag().equals(sender.saveWithoutMetadata()),"Chunk update tag contains complete persistent data");
        } finally {buffer.release();}
        var malformed=new ArrayList<CompoundTag>();malformed.add(new CompoundTag());
        for(var state:List.of(Blocks.AIR.defaultBlockState(),Blocks.CHEST.defaultBlockState(),proxy)) {var tag=valid.copy();tag.put("Original",NbtUtils.writeBlockState(state));malformed.add(tag);}
        var unknown=valid.copy();unknown.getCompound("Original").putString("Name","thaumcraft:missing_ward_original");malformed.add(unknown);
        var wrongType=valid.copy();wrongType.putString("Original","broken");malformed.add(wrongType);
        for(var tag:malformed){var ward=new WardedBlockEntity(pos,proxy);ward.load(tag);test.assertTrue(ward.original().is(Blocks.STONE),"Unsafe or unavailable original falls back to stone: "+tag);}
        for(int scene=0;scene<3;scene++) {var tag=valid.copy();if(scene==0)tag.remove("Owner");if(scene==1)tag.putString("Owner","broken");if(scene==2)tag.putIntArray("Owner",new int[]{1,2});var ward=new WardedBlockEntity(pos,proxy);ward.load(tag);test.assertTrue(ward.owner()==null&&ward.original().equals(original),"Malformed owner cannot grant ownership and preserves valid original");}
        tc4revival.TC4Revival.LOGGER.info("PASS wardingMalformedNbtAndRealPacketCodecPreserveSafeState: actual BE packet codec, six invalid originals, three invalid owners");test.succeed();
    }
}
