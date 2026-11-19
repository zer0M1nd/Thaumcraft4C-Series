package automagy;
import automagy.content.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
@net.minecraftforge.gametest.GameTestHolder("automagy")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public class ScribeRenderStateTest {
    @GameTest(template="empty",templateNamespace="automagy")
    public static void powerStateSynchronizesAndTableTransmitsLight(GameTestHelper h){
        var relative=new BlockPos(1,1,1);h.setBlock(relative,Content.SCRIBE.get());
        var pos=h.absolutePos(relative);var tile=(UnseenScribeBlockEntity)h.getLevel().getBlockEntity(pos);
        var state=tile.getBlockState();var tag=new CompoundTag();tag.putInt("PoweredTicks",20);tile.load(tag);
        h.assertTrue(tile.getUpdateTag().getInt("PoweredTicks")==20,"Power missing from initial client snapshot");
        h.assertTrue(tile.getUpdatePacket()!=null,"Power transitions have no update packet");
        h.assertTrue(state.getLightBlock(h.getLevel(),pos)==0&&state.propagatesSkylightDown(h.getLevel(),pos),"Scribe blocks skylight");
        h.assertTrue(state.getOcclusionShape(h.getLevel(),pos).isEmpty(),"Scribe masks neighboring geometry");
        for(int i=0;i<20;i++)UnseenScribeBlockEntity.tick(h.getLevel(),pos,state,tile);
        h.assertTrue(!tile.powered()&&tile.getUpdateTag().getInt("PoweredTicks")==0,"Power-off snapshot remains powered");
        h.succeed();
    }
}
