package wgmodern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

/** The original creative-only sliding lock; puzzle completion is checked on the server. */
public class TileLock extends BaseEntityBlock {
 public static final String[] PRESETS=LockPuzzle.PRESETS;
 static final int[][] REMOVAL={{-1,-1,-1,1,1,1},{1,-1,-1,-1,1,1},{-1,1,-1,1,-1,1},{1,1,-1,-1,-1,1},{-1,0,-1,1,0,1},{1,0,-1,-1,0,1},{0,-1,-1,0,1,1},{-1,-1,0,1,1,0},{0,-1,1,0,1,-1},{-1,1,0,1,-1,0},{0,0,-1,-1,0,0},{0,0,1,1,0,0},{0,-1,0},{0,1,0}};
 public TileLock(){super(Properties.copy(Blocks.STONE).strength(-1,999).isValidSpawn((state,level,pos,type)->false).noOcclusion());}
 @Override public String getDescriptionId(){return "tile.WG_StoneDevice.magicTileLock";}
 @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
 @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Tile(p,s);}
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return l.isClientSide?null:createTickerHelper(type,Content.LOCK_TILE.get(),(world,pos,state,t)->t.tick());}
 @Override public InteractionResult use(BlockState s,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){if(p instanceof ServerPlayer server&&l.getBlockEntity(pos) instanceof Tile tile&&!tile.unlocked){tile.restart();NetworkHooks.openScreen(server,tile,pos);}return InteractionResult.sidedSuccess(l.isClientSide);}
 public static int[] preset(int n){return LockPuzzle.preset(n);}public static boolean move(int[] b,int p){return LockPuzzle.move(b,p);}public static boolean solved(int[] b){return LockPuzzle.solved(b);}
 public static class Tile extends BlockEntity implements MenuProvider {
  public int preset=-1,ticks=-1;public int[] board=TileLock.preset(0);public boolean unlocked;
  public Tile(BlockPos p,BlockState s){super(Content.LOCK_TILE.get(),p,s);}
  public void restart(){if(unlocked)return;if(preset<0)preset=level.random.nextInt(PRESETS.length);board=TileLock.preset(preset);changed();}
  public boolean move(int pos){if(unlocked||!TileLock.move(board,pos))return false;if(solved(board)){unlocked=true;ticks=0;}changed();return true;}
  public void changed(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
  public void tick(){if(!unlocked||ticks<0)return;if(ticks%5==0){int step=ticks/5;if(step<REMOVAL.length)for(int i=0;i<REMOVAL[step].length;i+=3){var p=worldPosition.offset(REMOVAL[step][i],REMOVAL[step][i+1],REMOVAL[step][i+2]);if(level.getBlockState(p).is(Content.LOCK.get())||level.getBlockState(p).is(Content.OBSIDIAN_PLATE.get())){level.removeBlock(p,false);level.playSound(null,p,SoundEvents.ENDERMAN_TELEPORT,SoundSource.BLOCKS,1,1);}}if(ticks>=80){level.removeBlock(worldPosition,false);return;}}ticks++;setChanged();}
  @Override protected void saveAdditional(CompoundTag t){super.saveAdditional(t);t.putInt("Preset",preset);t.putIntArray("Board",board);t.putBoolean("Unlocked",unlocked);t.putInt("Ticks",ticks);}
  @Override public void load(CompoundTag t){super.load(t);preset=t.getInt("Preset");board=t.getIntArray("Board");if(board.length!=9||Arrays.stream(board).distinct().count()!=9||Arrays.stream(board).anyMatch(x->x<0||x>8))board=TileLock.preset(preset);unlocked=t.getBoolean("Unlocked");ticks=t.getInt("Ticks");}
  @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}@Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
  @Override public Component getDisplayName(){return Component.translatable("tile.WG_StoneDevice.magicTileLock");}@Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return new Menu(id,inv,worldPosition);}
 }
 public static class Menu extends AbstractContainerMenu {
  public final Tile tile;public final BlockPos pos;public final ContainerData data;
  public Menu(int id,Inventory inv,BlockPos p){super(Content.LOCK_MENU.get(),id);pos=p;tile=inv.player.level().getBlockEntity(p) instanceof Tile t?t:null;data=tile!=null?new ContainerData(){public int get(int i){return i==9?(tile.unlocked?1:0):tile.board[i];}public void set(int i,int v){if(i==9)tile.unlocked=v!=0;else tile.board[i]=v;}public int getCount(){return 10;}}:new SimpleContainerData(10);addDataSlots(data);}
  @Override public boolean stillValid(Player p){return tile!=null&&!tile.isRemoved()&&p.level().getBlockEntity(pos)==tile&&p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos))<=64;}
  @Override public boolean clickMenuButton(Player p,int id){return !p.level().isClientSide&&stillValid(p)&&tile.move(id);}
  @Override public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}
 }
}
