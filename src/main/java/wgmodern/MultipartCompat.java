package wgmodern;

import java.util.*;
import codechicken.lib.data.*;
import codechicken.multipart.api.*;
import codechicken.multipart.api.part.*;
import codechicken.multipart.block.TileMultipart;
import codechicken.multipart.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.registries.*;
import tc4revival.aspect.*;
import tc4revival.content.*;

/** The four original FMP tube adapters, using CBMultipart's modern passthrough traits. */
public class MultipartCompat {
    public static final DeferredRegister<MultipartType<?>> TYPES=DeferredRegister.create(MultipartType.MULTIPART_TYPES,"witchinggadgets");
    public static final DeferredRegister<PartConverter> CONVERTERS=DeferredRegister.create(PartConverter.PART_CONVERTERS,"witchinggadgets");
    public static final Map<EssentiaTubeFlow.Kind,RegistryObject<MultipartType<Tube>>> PARTS=new EnumMap<>(EssentiaTubeFlow.Kind.class);
    static {for(var k:List.of(EssentiaTubeFlow.Kind.NORMAL,EssentiaTubeFlow.Kind.VALVE,EssentiaTubeFlow.Kind.FILTER,EssentiaTubeFlow.Kind.BUFFER))PARTS.put(k,TYPES.register("tube_"+k.name().toLowerCase(Locale.ROOT),()->new SimpleMultipartType<>(client->new Tube(k))));CONVERTERS.register("essentia_tubes",Converter::new);}
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus){TYPES.register(bus);CONVERTERS.register(bus);bus.addListener(MultipartCompat::traits);}
    public static void traits(RegisterMultipartTraitsEvent e){e.registerPassthroughInterface(EssentiaTransport.class);e.registerPassthroughInterface(WandInteractable.class);e.registerPassthroughInterface(RevealedAspectContainer.class);}
    public static Block block(EssentiaTubeFlow.Kind k){return switch(k){case VALVE->TCArcane.ESSENTIA_VALVE.get();case FILTER->TCArcane.ESSENTIA_FILTER.get();case BUFFER->TCArcane.ESSENTIA_BUFFER.get();default->TCArcane.ESSENTIA_TUBE.get();};}
    public static EssentiaTubeFlow.Kind kind(Block b){if(!(b instanceof EssentiaTubeBlock t))return null;return PARTS.containsKey(t.kind())?t.kind():null;}
    public static class Converter extends PartConverter {
        @Override public ConversionResult<Collection<MultiPart>> convert(LevelAccessor l,BlockPos p,BlockState s){var k=kind(s.getBlock());if(k==null)return emptyResultList();var part=new Tube(k);if(l.getBlockEntity(p) instanceof EssentiaTubeBlockEntity tube)part.flow.load(tube.flow().save(true),true);return ConversionResult.success(List.of(part));}
        @Override public ConversionResult<MultiPart> convert(MultipartPlaceContext c){var k=c.getItemInHand().getItem() instanceof BlockItem b?kind(b.getBlock()):null;return k==null?emptyResult():ConversionResult.success(new Tube(k));}
    }
    public static class Tube extends BaseMultipart implements TickablePart,NormalOcclusionPart,SlottedPart,EssentiaTransport,WandInteractable,RevealedAspectContainer,EssentiaTubeFlow.Host {
        public final EssentiaTubeFlow flow; private final RandomSource random=RandomSource.create();private View view;private float rotation;private int ventTicks,ventColor=0xaaaaaa;private boolean dirty;private final Boolean[] freeArms=new Boolean[6];
        public Tube(EssentiaTubeFlow.Kind k){flow=new EssentiaTubeFlow(k,this);}
        @Override public MultipartType<?> getType(){return PARTS.get(flow.kind()).get();}
        @Override public int getSlotMask(){return 1<<6;}
        @Override public VoxelShape getOcclusionShape(){return Block.box(6,6,6,10,10,10);}
        @Override public VoxelShape getShape(CollisionContext c){if(!hasTile())return getOcclusionShape();var shape=Shapes.empty();for(var a:EssentiaTubeBlock.parts(view()).values())shape=Shapes.or(shape,Shapes.create(a));return shape;}
        @Override public Iterable<ItemStack> getDrops(){return flow.kind()==EssentiaTubeFlow.Kind.FILTER&&flow.filter()!=null?List.of(new ItemStack(block(flow.kind())),new ItemStack(TCItems.JAR_LABEL.get())):List.of(new ItemStack(block(flow.kind())));}
        @Override public ItemStack getCloneStack(PartRayTraceResult hit){return new ItemStack(block(flow.kind()));}
        @Override public float getStrength(Player p,PartRayTraceResult hit){return block(flow.kind()).defaultBlockState().getDestroyProgress(p,level(),pos());}
        @Override public SoundType getPlacementSound(UseOnContext c){return SoundType.METAL;}
        @Override public void save(CompoundTag tag){tag.merge(flow.save(true));}
        @Override public void load(CompoundTag tag){flow.load(tag,true);}
        @Override public void writeDesc(MCDataOutput out){var tag=flow.save(false);tag.putInt("WGvent",ventTicks);tag.putInt("WGcolor",ventColor);out.writeCompoundNBT(tag);}
        @Override public void readDesc(MCDataInput in){var tag=in.readCompoundNBT();flow.load(tag,false);ventTicks=tag.getInt("WGvent");ventColor=tag.getInt("WGcolor");}
        @Override public void onPartChanged(MultiPart other){Arrays.fill(freeArms,null);if(hasTile())tile().markShapeChange();}
        @Override public void onMoved(){Arrays.fill(freeArms,null);view=null;}
        @Override public void onRemoved(){if(view!=null)view.setRemoved();}
        @Override public void onChunkUnload(){if(view!=null)view.setRemoved();}
        @Override public void tick(){if(level().isClientSide){if(ventTicks>0)ventTicks--;if(flow.kind()==EssentiaTubeFlow.Kind.VALVE)rotation=flow.allowFlow()?Math.max(0,rotation-20):Math.min(360,rotation+20);}else{flow.tick();if(ventTicks>0)ventTicks--;if(dirty){dirty=false;sendUpdate(this::writeDesc);tile().setChanged();tile().markShapeChange();}}}
        public View view(){if(view==null||!view.getBlockPos().equals(pos())||view.getLevel()!=level()||view.isRemoved()){view=new View(this);view.setLevel(level());}return view;}
        public BlockEntity adjacent(Direction side){if(side==null||!hasTile()||!level().hasChunkAt(pos().relative(side)))return null;return level().getBlockEntity(pos().relative(side));}
        public boolean armFree(Direction side){if(!hasTile())return true;int index=side.get3DDataValue();if(freeArms[index]!=null)return freeArms[index];var b=new net.minecraft.world.phys.AABB(.4375,.4375,.4375,.5625,.5625,.5625).expandTowards(side.getStepX()*.4375,side.getStepY()*.4375,side.getStepZ()*.4375);var probe=new Probe(Shapes.create(b));for(var part:tile().getPartList())if(part!=this&&(!part.occlusionTest(probe)||!probe.occlusionTest(part))){freeArms[index]=false;return false;}freeArms[index]=true;return true;}
        @Override public EssentiaTransport neighbor(Direction side){return armFree(side)&&adjacent(side) instanceof EssentiaTransport t&&t.connectable(side.getOpposite())?t:null;}
        @Override public RandomSource random(){return hasTile()?level().random:random;}
        @Override public boolean powered(){return hasTile()&&level().hasNeighborSignal(pos());}
        @Override public int bellows(){return hasTile()?BellowsBlockEntity.count(level(),pos(),Arrays.asList(Direction.values())):0;}
        @Override public void changed(){dirty=true;}
        public void sound(net.minecraft.sounds.SoundEvent s,float volume,float pitch){if(hasTile()&&!level().isClientSide)level().playSound(null,pos(),s,SoundSource.BLOCKS,volume,pitch);}
        @Override public void valveSound(){sound(TCSounds.SQUEEK.get(),.7f,.9f+random().nextFloat()*.2f);}
        @Override public void creak(){sound(TCSounds.CREAK.get(),1,1.3f+random().nextFloat()*.2f);}
        @Override public void vent(Aspect type){ventTicks=50;ventColor=type==null?0xaaaaaa:type.color();dirty=true;sound(net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,.1f,1+random().nextFloat()*.1f);}
        @Override public boolean connectable(Direction s){return s!=null&&armFree(s)&&flow.connectable(s);}
        @Override public boolean canInput(Direction s){return connectable(s)&&flow.canInput(s);}
        @Override public boolean canOutput(Direction s){return connectable(s)&&flow.canOutput(s);}
        @Override public Aspect suctionType(Direction s){return flow.suctionType(s);}
        @Override public int suction(Direction s){return flow.suction(s);}
        @Override public int minimumSuction(){return flow.minimumSuction();}
        @Override public Aspect essentiaType(Direction s){return flow.essentiaType(s);}
        @Override public int essentiaAmount(Direction s){return flow.essentiaAmount(s);}
        @Override public int takeEssentia(Aspect a,int n,Direction s){return s==null||connectable(s)?flow.takeEssentia(a,n,s):0;}
        @Override public int addEssentia(Aspect a,int n,Direction s){return s==null||connectable(s)?flow.addEssentia(a,n,s):0;}
        @Override public AspectAmounts revealedAspects(){return flow.kind()==EssentiaTubeFlow.Kind.FILTER?flow.filter()==null?new AspectAmounts():new AspectAmounts().add(flow.filter(),-1):flow.kind()==EssentiaTubeFlow.Kind.BUFFER?flow.contents():new AspectAmounts();}
        @Override public InteractionResult activate(Player p,PartRayTraceResult hit,ItemStack s,InteractionHand hand){if(!level().mayInteract(p,pos())||!p.mayUseItemAt(pos(),hit.getDirection(),s))return InteractionResult.FAIL;if(flow.kind()==EssentiaTubeFlow.Kind.VALVE){if(!level().isClientSide)flow.toggleValve();return InteractionResult.sidedSuccess(level().isClientSide);}if(flow.kind()==EssentiaTubeFlow.Kind.FILTER){if(p.isShiftKeyDown()&&flow.filter()!=null){if(!level().isClientSide){var label=new ItemStack(TCItems.JAR_LABEL.get());p.getInventory().placeItemBackInInventory(label);flow.filter(null);}return InteractionResult.sidedSuccess(level().isClientSide);}if(s.getItem() instanceof JarLabelItem&&flow.filter()==null){var aspect=((JarLabelItem)s.getItem()).storedAspects(s).aspects().stream().findFirst().orElse(null);if(aspect!=null){if(!level().isClientSide){flow.filter(aspect);if(!p.getAbilities().instabuild)s.shrink(1);}return InteractionResult.sidedSuccess(level().isClientSide);}}}return InteractionResult.PASS;}
        @Override public InteractionResult onWandUseFirst(ItemStack stack,UseOnContext c){var p=c.getPlayer();if(p==null||!level().mayInteract(p,pos())||!p.mayUseItemAt(pos(),c.getClickedFace(),stack))return InteractionResult.FAIL;int part=EssentiaTubeBlock.tracePart(view(),p);if(part<0)return InteractionResult.PASS;if(level().isClientSide)return InteractionResult.SUCCESS;if(part<6){var side=Direction.from3DDataValue(part);if(flow.kind()==EssentiaTubeFlow.Kind.BUFFER&&p.isShiftKeyDown())flow.cycleChoke(side);else{var adjacent=adjacent(side);EssentiaTubeFlow other=null;if(adjacent instanceof EssentiaTubeBlockEntity t)other=t.flow();else if(adjacent instanceof TileMultipart multi)for(var t:multi.getPartList())if(t instanceof Tube tube)other=tube.flow;if(other!=null&&(flow.kind()==EssentiaTubeFlow.Kind.BUFFER||other.kind()!=EssentiaTubeFlow.Kind.BUFFER)){if(!level().mayInteract(p,pos().relative(side))||!p.mayUseItemAt(pos().relative(side),side.getOpposite(),stack))return InteractionResult.FAIL;other.setOpen(side.getOpposite(),!flow.open(side));}flow.setOpen(side,!flow.open(side));}}else if(flow.kind()!=EssentiaTubeFlow.Kind.BUFFER){for(int i=flow.facing().get3DDataValue()+1;i<20;i++){var next=Direction.from3DDataValue(i%6);if(flow.kind()==EssentiaTubeFlow.Kind.VALVE?!(adjacent(next) instanceof EssentiaTransport):neighbor(next.getOpposite())!=null){flow.face(next);break;}}}sound(TCSounds.TOOL.get(),.5f,.9f+random().nextFloat()*.2f);return InteractionResult.CONSUME;}
        @Override public Optional<ItemStack> onWandUse(Level l,ItemStack s,Player p){return Optional.empty();}
        @Override public void onWandUseTick(ItemStack s,Player p,int remaining){}
        @Override public void onWandRelease(ItemStack s,Level l,Player p,int remaining){}
    }
    public static class Probe extends BaseMultipart implements NormalOcclusionPart {private final VoxelShape shape;public Probe(VoxelShape shape){this.shape=shape;}public MultipartType<?> getType(){return null;}public VoxelShape getOcclusionShape(){return shape;}}
    /** A read-only rendering adapter. No second block entity is placed or ticked in the world. */
    public static class View extends EssentiaTubeBlockEntity {
        private final Tube part;public View(Tube part){super(part.pos(),block(part.flow.kind()).defaultBlockState());this.part=part;}
        @Override public EssentiaTubeFlow flow(){return part.flow;}
        @Override public float rotation(){return part.rotation;}
        @Override public int clientVent(){return part.ventTicks;}
        @Override public int ventColor(){return part.ventColor;}
        @Override public BlockEntity adjacent(Direction side){var adjacent=part.adjacent(side);if(adjacent instanceof TileMultipart m)for(var p:m.getPartList())if(p instanceof Tube t)return t.view();return adjacent;}
        @Override public EssentiaTransport neighbor(Direction side){return part.neighbor(side);}
        @Override public boolean connectable(Direction side){return part.connectable(side);}
        @Override public boolean hasTransport(Direction side){return part.adjacent(side) instanceof EssentiaTransport;}
    }
}
