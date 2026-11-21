package automagy.content;
import automagy.logic.*;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.content.*;
public final class DimensionLureBlockEntity extends InventoryBlockEntity {
 public long lastAmbient=Long.MIN_VALUE;
 public double midpoint(){var host=controller();return host==null?worldPosition.getY()+.5:(worldPosition.getY()+1+host.getBlockPos().getY())/2.0;}
 private final NetherBait bait=new NetherBait();private int ticks;
 public DimensionLureBlockEntity(BlockPos pos,BlockState state){super(Content.DIMENSION_LURE_TILE.get(),pos,state,0);}
 public boolean enabled(){return automagy.AutomagyConfig.DIMENSION_LURE.get() && getBlockState().getValue(DimensionLureBlock.ACTIVE);}
 private net.minecraft.world.level.block.entity.BlockEntity controller(){if(level==null)return null;for(int d=6;d<=10;d++){var p=worldPosition.above(d);if(level.isOutsideBuildHeight(p)||!level.hasChunkAt(p))continue;var tile=level.getBlockEntity(p);if(tile instanceof NethermindHost)return tile;}return null;}
 public boolean conjuring(){var tile=controller();return tile instanceof NethermindHost host&&host.conjuring();}
 /** A chunk boundary is a suspended check, not evidence of a broken ring. */
 private String ring(){for(var p:NethermindStructure.LURE_RING)if(!level.hasChunkAt(worldPosition.offset(p.x(),0,p.z())))return "unloaded";for(var p:NethermindStructure.LURE_RING)if(!level.getBlockState(worldPosition.offset(p.x(),0,p.z())).is(Content.RUNED_OBSIDIAN.get()))return "invalid";return "";}
 public boolean enable(){if(!automagy.AutomagyConfig.DIMENSION_LURE.get()||level==null||level.isClientSide||enabled()||controller()==null||!ring().isEmpty())return false;level.setBlockAndUpdate(worldPosition,getBlockState().setValue(DimensionLureBlock.ACTIVE,true));sync();level.playSound(null,worldPosition,TCSounds.BRAIN.get(),net.minecraft.sounds.SoundSource.BLOCKS,.8f,.2f+level.random.nextFloat()*.3f);if(level instanceof net.minecraft.server.level.ServerLevel server)server.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,500,.5,.5,.5,.01);level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.GHAST_SHOOT,net.minecraft.sounds.SoundSource.BLOCKS,1,.5f);for(var p:NethermindStructure.LURE_RING){var at=worldPosition.offset(p.x(),1,p.z());if(level.random.nextInt(3)>0&&level.isEmptyBlock(at))level.setBlockAndUpdate(at,net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState());}return true;}
 public void disable(boolean replace){if(level==null||level.isClientSide)return;var tile=controller();if(replace)level.setBlockAndUpdate(worldPosition,getBlockState().setValue(DimensionLureBlock.ACTIVE,false));bait.clearActive();sync();if(tile instanceof NethermindHost host)host.requireValidation();}
 public void select(NetherRuneCatalog.Definition rune){bait.select(rune.key(),rune.baitCost(),rune.baitUses());sync();}
 public void clearActive(){bait.clearActive();sync();}
 public boolean ready(){return bait.needed().isEmpty();}
 public boolean consumeBait(){boolean result=bait.consume();if(result)setChanged();return result;}
 public boolean acquire(){if(level==null||level.isClientSide||!enabled())return false;for(String key:bait.needed().keySet()){var aspect=tc4revival.aspect.TCAspects.catalog().find(key);if(aspect.isPresent()&&EssentiaSources.drain(this,aspect.get(),null,12,false).isPresent()){bait.received(key);sync();return true;}}return false;}
 public int stability(){if(level==null)return 0;int[] heights=new int[4];int index=0,nitor=0;for(int x:new int[]{-3,3})for(int z:new int[]{-3,3}){int height=0;var p=worldPosition.offset(x,0,z);while(height<4&&level.hasChunkAt(p.above(height))&&level.getBlockState(p.above(height)).is(TCArcane.OBSIDIAN_TOTEM.get()))height++;heights[index++]=height;if(height>0&&level.hasChunkAt(p.above(height))&&level.getBlockState(p.above(height)).is(TCArcane.NITOR.get()))nitor++;}var controller=controller();if(controller!=null)for(var dir:Direction.Plane.HORIZONTAL){var p=controller.getBlockPos().relative(dir,3).below(2);if(level.hasChunkAt(p)&&level.getBlockState(p).is(TCArcane.NITOR.get()))nitor++;}return NethermindStructure.stability(heights,nitor);}
 public static void tick(Level level,BlockPos pos,BlockState state,DimensionLureBlockEntity tile){if(level.isClientSide||!tile.enabled()||++tile.ticks%60!=0)return;String status=tile.ring();if(status.equals("unloaded"))return;if(!status.isEmpty()||tile.controller()==null)tile.disable(true);}
 @Override protected void saveAdditional(net.minecraft.nbt.CompoundTag tag){super.saveAdditional(tag);tag.put("Bait",NetherProgressData.bait(bait));}
 @Override public void load(net.minecraft.nbt.CompoundTag tag){super.load(tag);NetherProgressData.bait(bait,tag.getCompound("Bait"));}
}
