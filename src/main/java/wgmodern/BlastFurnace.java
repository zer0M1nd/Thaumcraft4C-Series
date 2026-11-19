package wgmodern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import tc4revival.aspect.*;
import tc4revival.content.*;

/** One 3x3x3 multiblock; the bottom centre owns its queue and every port resolves that owner. */
public class BlastFurnace extends BaseEntityBlock {
    public static final IntegerProperty PART=IntegerProperty.create("part",0,26),GLOW=IntegerProperty.create("glow",0,2);
    private static final Set<BlockPos> DISMANTLING=new HashSet<>();
    public BlastFurnace(){super(Properties.copy(Blocks.NETHER_BRICKS).strength(4,6).isValidSpawn((state,level,pos,type)->false).noOcclusion().lightLevel(s->s.getValue(PART)==22?15:(Set.of(10,12,14,16).contains(s.getValue(PART))?13:0)));registerDefaultState(stateDefinition.any().setValue(PART,4).setValue(GLOW,0));}
    @Override public String getDescriptionId(){return "tile.WG_StoneDevice.blastFurnace";}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(PART,GLOW);}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Tile(p,s);}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,Content.BLAST_TILE.get(),Tile::tick);}
    public static BlockPos offset(int part){return new BlockPos(part%3-1,part/9,(part%9)/3-1);}
    public static Direction port(int part){return switch(part){case 1,10->Direction.NORTH;case 3,12->Direction.WEST;case 5,14->Direction.EAST;case 7,16->Direction.SOUTH;default->Direction.UP;};}
    public static BlockState original(int part){if(part==22)return Blocks.LAVA.defaultBlockState();if(part==4||Set.of(9,11,15,17).contains(part))return Blocks.OBSIDIAN.defaultBlockState();if(part>=9&&part<18)return Blocks.SOUL_SAND.defaultBlockState();if(part<18)return Blocks.NETHER_BRICKS.defaultBlockState();int x=part%3-1,z=(part%9)/3-1;return Blocks.NETHER_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,x==-1?Direction.EAST:x==1?Direction.WEST:z==-1?Direction.SOUTH:Direction.NORTH);}
    public static boolean valid(Level l,BlockPos centre){for(int part=0;part<27;part++){var p=centre.offset(offset(part));if(!l.hasChunkAt(p))return false;var s=l.getBlockState(p);if(part>=18&&part!=22){if(!s.is(Blocks.NETHER_BRICK_STAIRS)||s.getValue(StairBlock.HALF)!=Half.BOTTOM)return false;var f=s.getValue(StairBlock.FACING);int x=part%3-1,z=(part%9)/3-1;if(!((x==-1&&f==Direction.EAST)||(x==1&&f==Direction.WEST)||(z==-1&&f==Direction.SOUTH)||(z==1&&f==Direction.NORTH)))return false;}else if(!s.is(original(part).getBlock())||(part==22&&!s.getFluidState().isSource()))return false;}return true;}
    public static void form(ServerLevel l,BlockPos centre,Direction facing){for(int part=0;part<27;part++){var p=centre.offset(offset(part));l.setBlock(p,Content.BLAST.get().defaultBlockState().setValue(PART,part),3);var tile=(Tile)l.getBlockEntity(p);tile.owner=centre;tile.facing=facing;tile.setChanged();}}
    public static InteractionResult activate(UseOnContext c){if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.PASS;if(!tc4revival.research.KnowledgeSavedData.get(p.server).snapshot(p.getUUID()).map(k->k.researchMarkers().contains("witchinggadgets:INFERNALBLASTFURNACE")).orElse(false))return InteractionResult.PASS;var level=p.serverLevel();for(int x=-1;x<=1;x++)for(int y=0;y<=2;y++)for(int z=-1;z<=1;z++){var centre=c.getClickedPos().offset(x,y-2,z);if(!valid(level,centre))continue;for(int i=0;i<27;i++)if(!level.mayInteract(p,centre.offset(offset(i)))||!p.mayUseItemAt(centre.offset(offset(i)),Direction.UP,c.getItemInHand()))return InteractionResult.FAIL;var cost=Map.of(TCAspects.catalog().require("ignis"),50,TCAspects.catalog().require("terra"),50,TCAspects.catalog().require("perditio"),50);if(!(c.getItemInHand().getItem() instanceof CastingWandItem wand)||!wand.consumeForPlayer(c.getItemInHand(),p,cost,true,true,true))return InteractionResult.FAIL;form(level,centre,p.getDirection().getOpposite());thaumcraft.api.ThaumcraftApiHelper.addStickyWarpToPlayer(p,3);return InteractionResult.SUCCESS;}return InteractionResult.PASS;}
    public static void init(){thaumcraft.api.wands.WandTriggerRegistry.register(new ResourceLocation("witchinggadgets","blast_furnace"),s->s.is(Blocks.NETHER_BRICKS)||s.is(Blocks.NETHER_BRICK_STAIRS)||s.is(Blocks.OBSIDIAN)||s.is(Blocks.SOUL_SAND),BlastFurnace::activate);}
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){int part=s.getValue(PART);if(part==22)return Shapes.empty();if(part<18)return Shapes.block();int n=part-18;return Shapes.or(Block.box(0,0,0,16,8,16),Block.box(n%3==0?8:0,8,n<3?8:0,(n+1)%3==0?8:16,16,n>5?8:16));}
    @Override public List<ItemStack> getDrops(BlockState s,LootParams.Builder p){int part=s.getValue(PART);return part==22?List.of():List.of(new ItemStack(original(part).getBlock()));}
    @Override public void onRemove(BlockState old,Level l,BlockPos p,BlockState next,boolean moving){if(!old.is(next.getBlock())&&!l.isClientSide&&l.getBlockEntity(p) instanceof Tile tile){var owner=tile.owner;if(owner!=null&&DISMANTLING.add(owner)){try{for(int i=0;i<27;i++){var q=owner.offset(offset(i));if(q.equals(p)||!l.hasChunkAt(q)||!l.getBlockState(q).is(this))continue;l.setBlock(q,original(i),3);}}finally{DISMANTLING.remove(owner);}}}super.onRemove(old,l,p,next,moving);}
    @Override public void entityInside(BlockState s,Level l,BlockPos p,Entity e){if(l.isClientSide||s.getValue(PART)!=22)return;if(e instanceof ItemEntity item&&l.getBlockEntity(p) instanceof Tile tile){var master=tile.master();if(master==null)return;if(BlastRecipe.find(l,item.getItem()).isPresent()){master.offer(item.getItem());item.discard();}else {item.discard();((ServerLevel)l).sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA,p.getX()+.5,p.getY()+.5,p.getZ()+.5,3,.2,.1,.2,0);l.playSound(null,p,net.minecraft.sounds.SoundEvents.LAVA_EXTINGUISH,net.minecraft.sounds.SoundSource.BLOCKS,.5f,2.6f+(l.random.nextFloat()-l.random.nextFloat())*.8f);}}else if(e instanceof LivingEntity&&!e.fireImmune()){e.hurt(l.damageSources().lava(),3);e.setSecondsOnFire(10);}}
    public static class Tile extends BlockEntity implements thaumcraft.api.aspects.IEssentiaTransport {
        public BlockPos owner;public Direction facing=Direction.NORTH;public final List<ItemStack> inputs=new ArrayList<>();public int speedup,remaining,duration;public boolean special;
        public Tile(BlockPos p,BlockState s){super(Content.BLAST_TILE.get(),p,s);if(s.getValue(PART)==4)owner=p;}
        public Tile master(){return owner!=null&&level!=null&&level.hasChunkAt(owner)&&level.getBlockEntity(owner) instanceof Tile t&&t.getBlockState().getValue(PART)==4?t:null;}
        public void offer(ItemStack stack){var copy=stack.copy();for(var input:inputs)if(ItemStack.isSameItemSameTags(input,copy)&&input.getCount()+copy.getCount()<=input.getMaxStackSize()){input.grow(copy.getCount());setChanged();return;}inputs.add(copy);setChanged();}
        public int bellows(){int count=0;for(var f:Direction.Plane.HORIZONTAL){var p=worldPosition.relative(f,2);if(!level.hasChunkAt(p))continue;var s=level.getBlockState(p);if(s.getBlock() instanceof BellowsBlock&&s.getValue(BellowsBlock.FACING)==f.getOpposite()&&!level.hasNeighborSignal(p))count++;}return count;}
        public int time(){return Math.max(1,duration/(speedup>0?2:1)-bellows()*40);}
        public void output(ItemStack item){if(item.isEmpty())return;var target=worldPosition.relative(facing,2).above();if(level.hasChunkAt(target)){var be=level.getBlockEntity(target);if(be!=null){var cap=be.getCapability(ForgeCapabilities.ITEM_HANDLER,facing.getOpposite()).resolve();if(cap.isPresent())item=ItemHandlerHelper.insertItemStacked(cap.get(),item,false);}}if(!item.isEmpty()){var e=new ItemEntity(level,worldPosition.getX()+.5+facing.getStepX()*1.66,worldPosition.getY()+1.4,worldPosition.getZ()+.5+facing.getStepZ()*1.66,item.copy());e.setDeltaMovement(facing.getStepX()*.075,.025,facing.getStepZ()*.075);level.addFreshEntity(e);}}
        public static void tick(Level l,BlockPos p,BlockState s,Tile t){int part=s.getValue(PART);if(!Set.of(1,3,4,5,7,10,12,14,16).contains(part))return;var master=t.master();if(master==null)return;if((part==1&&l.getGameTime()%5==0)||part==3||part==5||part==7){var next=p.relative(t.facing);var f=port(part);if(l.hasChunkAt(next)&&l.getBlockEntity(next) instanceof EssentiaTransport tr&&tr.connectable(t.facing.getOpposite())&&tr.canOutput(f.getOpposite())&&tr.suction(f.getOpposite())<t.suction(f)&&tr.takeEssentia(TCAspects.catalog().require("ignis"),1,f.getOpposite())==1){master.speedup+=600;master.setChanged();}}if(Set.of(10,12,14,16).contains(part)&&master.speedup<=0){master.speedup=thaumcraft.api.visnet.VisNetHandler.drainVis(l,p,thaumcraft.api.aspects.Aspect.FIRE,5);if(master.speedup>0)master.setChanged();}if(part!=4)return;boolean cooking=false;if(t.remaining>0){t.remaining--;cooking=true;if(t.speedup>0)t.speedup--;t.remaining=Math.min(t.remaining,t.time());t.setChanged();}if(t.remaining<=0&&!t.inputs.isEmpty()){var input=t.inputs.get(0);var recipe=BlastRecipe.find(l,input);if(recipe.isEmpty()){/* Reload may remove a running recipe. Return its input instead of dereferencing null. */t.output(input);t.inputs.remove(0);cooking=false;}else if(cooking){var r=recipe.get();t.output(r.output());var bonus=r.bonus();int n=0,b=t.bellows();if(!bonus.isEmpty()){if(b==0){if(l.random.nextInt(4)==0)n++;}else for(int i=0;i<b;i++)if(l.random.nextFloat()<.44f)n++;bonus.setCount(n);t.output(bonus);}input.shrink(r.count);if(input.isEmpty())t.inputs.remove(0);t.special=false;cooking=false;}else {var r=recipe.get();t.duration=r.time;t.remaining=t.time();t.special=r.special;cooking=true;}t.setChanged();}int glow=cooking?(t.special?2:1):0;if(s.getValue(GLOW)!=glow)for(int i=0;i<27;i++){var q=p.offset(offset(i));if(l.hasChunkAt(q)&&l.getBlockState(q).is(Content.BLAST.get()))l.setBlock(q,l.getBlockState(q).setValue(GLOW,glow),2);}}
        @Override public boolean connectable(Direction f){return port(getBlockState().getValue(PART))==f&&getBlockState().getValue(PART)<9;}
        @Override public boolean canInput(Direction f){return Set.of(1,3,4,5,7).contains(getBlockState().getValue(PART));}
        @Override public boolean canOutput(Direction f){return false;}@Override public Aspect suctionType(Direction f){return TCAspects.catalog().require("ignis");}
        @Override public int suction(Direction f){var m=master();return m!=null&&m.speedup<40?128:0;}
        @Override public int minimumSuction(){return 0;}@Override public Aspect essentiaType(Direction f){return null;}@Override public int essentiaAmount(Direction f){return 0;}
        @Override public int addEssentia(Aspect a,int n,Direction f){return 0;}@Override public int takeEssentia(Aspect a,int n,Direction f){return 0;}
        @Override protected void saveAdditional(CompoundTag t){super.saveAdditional(t);if(owner!=null)t.putLong("owner",owner.asLong());t.putString("facing",facing.getName());t.putInt("speedup",speedup);t.putInt("remaining",remaining);t.putInt("duration",duration);t.putBoolean("special",special);var list=new ListTag();inputs.forEach(s->list.add(s.save(new CompoundTag())));t.put("inputs",list);}
        @Override public void load(CompoundTag t){super.load(t);owner=t.contains("owner")?BlockPos.of(t.getLong("owner")):null;facing=Direction.byName(t.getString("facing"));if(facing==null||!facing.getAxis().isHorizontal())facing=Direction.NORTH;speedup=Math.max(0,t.getInt("speedup"));remaining=Math.max(0,t.getInt("remaining"));duration=Math.max(0,t.getInt("duration"));special=t.getBoolean("special");inputs.clear();for(var element:t.getList("inputs",Tag.TAG_COMPOUND)){var s=ItemStack.of((CompoundTag)element);if(!s.isEmpty())inputs.add(s);}}
    }
}
