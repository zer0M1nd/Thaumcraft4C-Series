package automagy.content;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.AspectAmounts;
public final class ThaumostaticPylonBlockEntity extends InventoryBlockEntity implements tc4revival.content.RevealedAspectContainer,automagy.api.AspectReadingProvider {
    private BlockPos controller;
    public ThaumostaticPylonBlockEntity(BlockPos pos,BlockState state){super(Content.THAUMOSTATIC_PYLON_TILE.get(),pos,state,0);}
    public void controller(BlockPos pos){controller=pos==null?null:pos.immutable();sync();}
    public void requireValidation(){if(controller!=null&&level!=null&&level.hasChunkAt(controller)&&level.getBlockEntity(controller) instanceof ThaumostaticControllerBlockEntity tile)tile.requireValidation();}
    @Override public automagy.logic.AspectReading aspectReading(){return controller!=null&&level!=null&&level.hasChunkAt(controller)&&level.getBlockEntity(controller) instanceof ThaumostaticControllerBlockEntity tile&&tile.active()?tile.aspectReading():automagy.logic.AspectReading.MISSING;}
    @Override public AspectAmounts revealedAspects(){return controller!=null&&level!=null&&level.hasChunkAt(controller)&&level.getBlockEntity(controller) instanceof ThaumostaticControllerBlockEntity tile&&tile.active()?tile.revealedAspects():new AspectAmounts();}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);if(controller!=null)tag.putLong("Controller",controller.asLong());}
    @Override public void load(CompoundTag tag){super.load(tag);controller=tag.contains("Controller")?BlockPos.of(tag.getLong("Controller")):null;}
}
