package automagy.content;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
public final class ThaumostaticSpireBlockEntity extends InventoryBlockEntity {
    private BlockPos controller;private int power,ticks;private boolean sky;
    private long refuelingUntil,nextSparkPacket;
    public ThaumostaticSpireBlockEntity(BlockPos pos,BlockState state){super(Content.THAUMOSTATIC_SPIRE_TILE.get(),pos,state,0);}
    public void controller(BlockPos pos){controller=pos.immutable();sync();}
    public boolean powered(){return power>0;}
    public boolean refueling(){return powered()&&level!=null&&level.getGameTime()<refuelingUntil;}
    public void sparks(){
        if(!(level instanceof net.minecraft.server.level.ServerLevel server)||level.getGameTime()<nextSparkPacket)return;
        nextSparkPacket=level.getGameTime()+50;refuelingUntil=level.getGameTime()+160;sync();
        tc4revival.network.FocusEffectPacket.lightning(level,worldPosition.getCenter().add(0,1.55,0),worldPosition.getCenter().add(0,3.55,0));
    }
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(){return new net.minecraft.world.phys.AABB(worldPosition).inflate(3);}
    @Override public CompoundTag getUpdateTag(){var tag=super.getUpdateTag();tag.putLong("RefuelingUntil",refuelingUntil);return tag;}
    public static void tick(Level level,BlockPos pos,BlockState state,ThaumostaticSpireBlockEntity tile){
        if(tile.ticks++%60==0)tile.sky=level.canSeeSky(pos.above(2));
        boolean powered=tile.power>0;if(tile.power>0)tile.power--;
        if(tile.power==0&&tile.sky){var aer=thaumcraft.api.aspects.Aspect.AIR;tile.power=thaumcraft.api.visnet.VisNetHandler.drainVis(level,pos.above(),aer,10);
            if(tile.power==0&&tile.controller!=null&&level.hasChunkAt(tile.controller)&&level.getBlockEntity(tile.controller) instanceof ThaumostaticControllerBlockEntity owner&&owner.active())tile.power=thaumcraft.api.visnet.VisNetHandler.drainVis(level,tile.controller,aer,10);
        }
        tile.setChanged();if(powered!=(tile.power>0))tile.sync();
    }
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putInt("Power",power);if(controller!=null)tag.putLong("Controller",controller.asLong());}
    @Override public void load(CompoundTag tag){super.load(tag);power=Math.max(0,Math.min(10,tag.getInt("Power")));controller=tag.contains("Controller")?BlockPos.of(tag.getLong("Controller")):null;ticks=0;refuelingUntil=tag.getLong("RefuelingUntil");nextSparkPacket=0;}
}
