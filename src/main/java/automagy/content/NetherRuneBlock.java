package automagy.content;
import automagy.logic.NetherRuneCatalog;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
public final class NetherRuneBlock extends Block implements automagy.api.NetherRune {
 public static final BooleanProperty ACTIVE=BooleanProperty.create("active");
 private final NetherRuneCatalog.Definition definition;
 public NetherRuneBlock(NetherRuneCatalog.Definition definition){super(Properties.of().strength(2.5f,2000).sound(SoundType.STONE));this.definition=definition;registerDefaultState(stateDefinition.any().setValue(ACTIVE,false));}
 public NetherRuneCatalog.Definition definition(){return definition;}
 @Override public NetherRuneCatalog.Definition netherRune(BlockState state){return definition;}
 @Override public void setNetherRuneActive(Level level,BlockPos pos,BlockState state,boolean active){level.setBlockAndUpdate(pos,state.setValue(ACTIVE,active));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(ACTIVE);}
 @Override public boolean isFireSource(BlockState state,LevelReader level,BlockPos pos,Direction side){return true;}
}
