package automagy.content;
import automagy.logic.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
public final class ThirstyTankBlockEntity extends InventoryBlockEntity {
 private int[] glyphs=new int[6];private TankGlyphs rules=TankGlyphs.from(glyphs);
 private long nextZap;
 public boolean rejectCapacityRemoval(net.minecraft.world.entity.player.Player player){if(level==null||level.isClientSide||level.getGameTime()<nextZap)return false;nextZap=level.getGameTime()+20;player.hurt(level.damageSources().magic(),1);level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_IMPACT,net.minecraft.sounds.SoundSource.BLOCKS,.15f,1.7f);return true;}
 private int cooldown=30,idle,lastDirection,ticks,rain;
 public final FluidTank tank=new FluidTank(16000){
  @Override protected void onContentsChanged(){sync();if(level!=null&&!level.isClientSide)level.updateNeighbourForOutputSignal(worldPosition,getBlockState().getBlock());}
  @Override public int fill(FluidStack stack,FluidAction action){if(stack.isEmpty()||!isEmpty()&&!getFluid().isFluidEqual(stack))return 0;int filled=super.fill(stack,action);return rules.voiding()?stack.getAmount():filled;}
  @Override public FluidStack drain(int amount,FluidAction action){return super.drain(rules.drainLimit(amount,getFluidAmount()),action);}
 };
 private LazyOptional<IFluidHandler> capability=LazyOptional.of(()->tank);
 public ThirstyTankBlockEntity(BlockPos p,BlockState s){super(Content.THIRSTY_TANK_TILE.get(),p,s,0);}
 public int glyph(Direction side){return glyphs[side.ordinal()];}
 public boolean install(Direction side,int glyph){if(glyph<1||glyph>9||glyph(side)!=0)return false;glyphs[side.ordinal()]=glyph;refresh();sync();return true;}
 public int remove(Direction side){int old=glyph(side);if(old==8&&!rules.canRemoveCapacity(tank.getFluidAmount()))return -1;glyphs[side.ordinal()]=0;refresh();sync();return old;}
 private void refresh(){rules=TankGlyphs.from(glyphs);tank.setCapacity(rules.capacity());}
 public int comparator(){return tank.isEmpty()?0:1+(int)(14L*tank.getFluidAmount()/tank.getCapacity());}
 private IFluidHandler neighbor(Direction dir){var pos=worldPosition.relative(dir);if(!level.hasChunkAt(pos))return null;var tile=level.getBlockEntity(pos);if(tile==null)return null;var handler=tile.getCapability(ForgeCapabilities.FLUID_HANDLER,dir.getOpposite()).orElse(null);return handler!=null?handler:automagy.compat.BotaniaApothecary.of(tile);}
 private void siphon(){
  var targets=new ArrayList<IFluidHandler>();for(var dir:Direction.values())if(glyph(dir)==3){var target=neighbor(dir);if(target!=null&&target!=tank&&!tank.isEmpty()&&target.fill(new FluidStack(tank.getFluid(),1),IFluidHandler.FluidAction.SIMULATE)>0)targets.add(target);}
  int share=rules.siphonShare(tank.getFluidAmount(),targets.size());if(share<=0)return;
  for(var target:targets){var offer=tank.drain(share,IFluidHandler.FluidAction.SIMULATE);int accepted=target.fill(offer,IFluidHandler.FluidAction.EXECUTE);if(accepted>0)tank.drain(Math.min(offer.getAmount(),accepted),IFluidHandler.FluidAction.EXECUTE);}
 }
 private boolean pilfer(Direction dir){var source=neighbor(dir);if(source==null||source==tank)return false;
  for(int i=0;i<source.getTanks();i++){var sample=source.getFluidInTank(i);if(sample.isEmpty())continue;var offer=source.drain(new FluidStack(sample,1000),IFluidHandler.FluidAction.SIMULATE);int accepted=tank.fill(offer,IFluidHandler.FluidAction.SIMULATE);if(accepted<=0)continue;var actual=source.drain(new FluidStack(offer,accepted),IFluidHandler.FluidAction.EXECUTE);if(actual.isEmpty())continue;tank.fill(actual,IFluidHandler.FluidAction.EXECUTE);return true;}return false;
 }
 private boolean milk(Direction direction){if(rules.voiding()?tank.getFluidAmount()>=tank.getCapacity():tank.getFluidAmount()+1000>tank.getCapacity())return false;
  var center=worldPosition.getCenter().add(direction.getStepX()*3.5,direction.getStepY()*3.5,direction.getStepZ()*3.5);var box=new net.minecraft.world.phys.AABB(center.x-2.5,center.y-2.5,center.z-2.5,center.x+2.5,center.y+2.5,center.z+2.5);
  for(var cow:level.getEntitiesOfClass(net.minecraft.world.entity.animal.Cow.class,box)){long now=level.getGameTime();var data=cow.getPersistentData();if(now-data.getLong("automagy:TimeMilked")<automagy.AutomagyConfig.COW_MILKING_COOLDOWN.get())continue;var fluid=cow instanceof net.minecraft.world.entity.animal.MushroomCow?SoupFluids.MUSHROOM.source.get():net.minecraftforge.common.ForgeMod.MILK.get();if(tank.fill(new FluidStack(fluid,1000),IFluidHandler.FluidAction.EXECUTE)>0){data.putLong("automagy:TimeMilked",now);cow.playSound(net.minecraft.sounds.SoundEvents.COW_HURT,.4f,1);return true;}}return false;
 }
 private boolean drink(Direction direction){var start=worldPosition.relative(direction);if(!level.hasChunkAt(start))return false;var fluid=level.getFluidState(start);if(fluid.isEmpty())return false;
  var wanted=new FluidStack(fluid.getType() instanceof net.minecraft.world.level.material.FlowingFluid flowing?flowing.getSource():fluid.getType(),1000);if(tank.fill(wanted,IFluidHandler.FluidAction.SIMULATE)<1000)return false;
  // Default legacy option preserves adjacent water sources, not only renewable pools.
  if(automagy.AutomagyConfig.TANK_PRESERVE_WATER.get()&&fluid.isSource()&&fluid.getType().isSame(Fluids.WATER)){if(rules.voiding()&&tank.getFluidAmount()>=tank.getCapacity())return false;tank.fill(new FluidStack(Fluids.WATER,1000),IFluidHandler.FluidAction.EXECUTE);return true;}
  var result=LiquidSourceSearch.find(start,p->{if(!level.hasChunkAt(p))return new LiquidSourceSearch.Cell(false,false);var f=level.getFluidState(p);boolean matches=!f.isEmpty()&&f.getType().isSame(fluid.getType());return new LiquidSourceSearch.Cell(matches,matches&&f.isSource());},p->{var list=new ArrayList<BlockPos>();list.add(p.relative(direction));for(var d:Direction.values())if(d!=direction&&d!=Direction.DOWN)list.add(p.relative(d));return list;},p->p.distSqr(worldPosition));
  if(result.isEmpty())return false;var source=result.get();var block=level.getBlockState(source).getBlock();IFluidHandler handler=block instanceof net.minecraftforge.fluids.IFluidBlock liquid?new net.minecraftforge.fluids.capability.wrappers.FluidBlockWrapper(liquid,level,source):block instanceof net.minecraft.world.level.block.BucketPickup pickup?new net.minecraftforge.fluids.capability.wrappers.BucketPickupHandlerWrapper(pickup,level,source):null;if(handler==null)return false;var offer=handler.drain(1000,IFluidHandler.FluidAction.SIMULATE);if(offer.isEmpty()||tank.fill(offer,IFluidHandler.FluidAction.SIMULATE)<offer.getAmount())return false;var actual=handler.drain(offer,IFluidHandler.FluidAction.EXECUTE);if(actual.isEmpty())return false;tank.fill(actual,IFluidHandler.FluidAction.EXECUTE);return true;
 }
 public static void tick(Level level,BlockPos pos,BlockState state,ThirstyTankBlockEntity tile){if(level.isClientSide)return;tile.ticks++;
  if(tile.ticks%5==0){if(tile.rules.consumption()>0)tile.tank.drain(tile.rules.consumption(),IFluidHandler.FluidAction.EXECUTE);tile.siphon();}
  if(tile.rules.controlled()&&!level.hasNeighborSignal(pos)){tile.cooldown=Math.max(0,tile.cooldown-1);return;}
  tile.idle=Math.min(601,tile.idle+1);if(--tile.cooldown<=0){for(int i=0;i<6;i++){tile.lastDirection=(tile.lastDirection+1)%6;var dir=Direction.from3DDataValue(tile.lastDirection);int glyph=tile.glyph(dir);if(tile.rules.milking()?glyph!=9:glyph==3)continue;boolean success=tile.rules.milking()?tile.milk(dir):glyph==4?tile.pilfer(dir):tile.drink(dir);if(success){tile.idle=0;break;}}
   tile.cooldown=tile.idle>600?80:tile.rules.drinkInterval();}
  if(automagy.AutomagyConfig.TANK_RAIN.get()&&!tile.rules.milking()&&level.isRainingAt(pos.above())){if(++tile.rain%5==0){tile.tank.fill(new FluidStack(Fluids.WATER,10),IFluidHandler.FluidAction.EXECUTE);if(tile.glyphs[level.random.nextInt(6)]==7)tile.rain+=3;}}
 }
 @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){return cap==ForgeCapabilities.FLUID_HANDLER&&!isRemoved()?capability.cast():super.getCapability(cap,side);}
 @Override public void invalidateCaps(){super.invalidateCaps();capability.invalidate();}
 @Override public void reviveCaps(){super.reviveCaps();capability=LazyOptional.of(()->tank);}
 @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putIntArray("Glyphs",glyphs);tag.put("Tank",tank.writeToNBT(new CompoundTag()));}
 @Override public void load(CompoundTag tag){super.load(tag);var saved=tag.getIntArray("Glyphs");glyphs=new int[6];for(int i=0;i<Math.min(6,saved.length);i++)glyphs[i]=saved[i]>=0&&saved[i]<=9?saved[i]:0;refresh();tank.readFromNBT(tag.getCompound("Tank"));}
}
