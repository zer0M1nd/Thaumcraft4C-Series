package gadomancymodern.compressor;
import gadomancymodern.Gadomancy;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import tc4revival.aspect.*;
import tc4revival.content.*;
import thaumcraft.api.aspects.AspectList;

/** Original three-high compressor; adopts the GTNH remainder/capacity fixes. */
public class EssentiaCompressorEntity extends BlockEntity implements thaumcraft.api.aspects.IAspectContainer,thaumcraft.api.aspects.IEssentiaTransport {
    public UUID structure;public int index=-1,stage,ticks,consumeTick;public BlockPos pedestal;public boolean prevFound;public AspectAmounts contents=new AspectAmounts();
    public static java.util.function.Consumer<EssentiaCompressorEntity> CLIENT_EFFECTS=tile->{};
    public EssentiaCompressorEntity(BlockPos pos,BlockState state){super(Gadomancy.COMPRESSOR_BE.get(),pos,state);}
    public boolean formed(){return structure!=null;}
    public EssentiaCompressorEntity master(){if(!formed()||level==null)return null;if(index==0)return this;return level.getBlockEntity(worldPosition.below(index)) instanceof EssentiaCompressorEntity tile&&tile.index==0&&structure.equals(tile.structure)?tile:null;}
    public boolean valid(){var master=master();if(master==null)return false;for(int i=1;i<3;i++)if(!(level.getBlockEntity(master.worldPosition.above(i)) instanceof EssentiaCompressorEntity tile)||tile.index!=i||!structure.equals(tile.structure))return false;return true;}
    public static void tick(Level level,BlockPos pos,BlockState state,EssentiaCompressorEntity tile){tile.ticks++;if(!tile.formed())return;if(level.isClientSide){if(tile.index==0){CLIENT_EFFECTS.accept(tile);tile.attract();}return;}if(!tile.valid()){tile.breakStructure();return;}if(tile.index!=0)return;if(tile.ticks%10==0&&(tile.prevFound||tile.ticks%100==0))tile.prevFound=tile.drainNearby();if(tile.stage<8&&(tile.pedestal!=null||tile.ticks%40==0))tile.consumeElement();tile.attract();}
    public int storage(){return 200+stage*350;}
    public boolean drainNearby(){
        var sources=new LinkedHashSet<EssentiaSource>();
        for(var direction:Direction.values())sources.addAll(EssentiaSources.availableSources(this,direction,5,false));
        // No transfer occurs before the successful return. Reuse totals within this attempt
        // only; external reservoirs may change before the very next call, even in this tick.
        var totals=new IdentityHashMap<EssentiaSource,Long>();
        long stored=contents.total();
        for(var aspect:TCAspects.catalog().all()){
            if(contents.amount(aspect)>=storage())continue;
            for(var source:sources){
                if(source instanceof EssentiaCompressorEntity||!(source instanceof BlockEntity tile))continue;
                long total=totals.computeIfAbsent(source,key->{
                    if(key instanceof RevealedAspectContainer revealed)return revealed.revealedAspects().total();
                    long sum=0;for(var a:TCAspects.catalog().all())sum+=key.availableAmount(a);
                    return Math.min(Integer.MAX_VALUE,sum);
                });
                if(total>stored)continue;
                if(source.takeFromContainer(aspect,1)){
                    contents.add(aspect,1);changed();
                    tc4revival.network.EssentiaSourcePacket.send((net.minecraft.server.level.ServerLevel)level,worldPosition.above(),tile.getBlockPos(),aspect.color());
                    return true;
                }
            }
        }
        return false;
    }
    protected boolean validPedestal(BlockPos pos){return level.hasChunkAt(pos)&&level.getBlockState(pos).is(Gadomancy.ANCIENT_PEDESTAL.get())&&level.getBlockEntity(pos) instanceof AncientPedestalEntity base&&base.getItem(0).is(Gadomancy.DARKNESS_ELEMENT.get());}
    public void consumeElement(){if(stage>=8)return;if(pedestal!=null){if(!validPedestal(pedestal)){pedestal=null;consumeTick=0;changed();return;}if(++consumeTick>400){((AncientPedestalEntity)level.getBlockEntity(pedestal)).setItem(0,net.minecraft.world.item.ItemStack.EMPTY);pedestal=null;consumeTick=0;stage++;changed();}}else for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){var at=worldPosition.offset(x,0,z);if(validPedestal(at)){pedestal=at;consumeTick=0;changed();return;}}}
    public void attract(){if(stage<=0)return;var center=worldPosition.getCenter().add(0,1,0);var box=new AABB(worldPosition.getX()-.5,worldPosition.getY()-.5,worldPosition.getZ()-.5,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5).inflate(4);for(var entity:level.getEntities((net.minecraft.world.entity.Entity)null,box,e->e.isAlive())){var delta=center.subtract(entity.position()).scale(.125);double length=delta.length(),factor=1-length;if(length<1e-8||factor<=0)continue;factor*=factor;double mult=stage*.125;entity.setDeltaMovement(entity.getDeltaMovement().add(delta.x/length*factor*.08*mult,delta.y/length*factor*.11*mult,delta.z/length*factor*.08*mult));entity.hasImpulse=true;}}
    public void breakStructure(){if(!formed()||level==null||level.isClientSide)return;var base=worldPosition.below(index);var id=structure;var affected=new ArrayList<EssentiaCompressorEntity>();boolean taint=false;for(int i=0;i<3;i++)if(level.getBlockEntity(base.above(i)) instanceof EssentiaCompressorEntity tile&&id.equals(tile.structure)){affected.add(tile);taint|=!tile.contents.isEmpty();}for(var tile:affected){tile.structure=null;tile.index=-1;tile.contents=new AspectAmounts();tile.pedestal=null;tile.changed();}for(var tile:affected){tile.explode(taint);level.removeBlock(tile.worldPosition,false);}}
    protected void explode(boolean taint){var server=(net.minecraft.server.level.ServerLevel)level;var pos=worldPosition;level.explode(null,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,2,false,Level.ExplosionInteraction.NONE);for(int i=0;i<20;i++){var at=pos.offset(level.random.nextInt(4)-level.random.nextInt(4),level.random.nextInt(4)-level.random.nextInt(4),level.random.nextInt(4)-level.random.nextInt(4));if(!level.hasChunkAt(at))continue;if(level.isEmptyBlock(at)){var name=at.getY()<pos.getY()?"flux_goo":"flux_gas";var block=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(new net.minecraft.resources.ResourceLocation("thaumcraft",name));if(block!=null){var state=block.defaultBlockState();for(var property:state.getProperties())if(property.getName().equals("level")&&property instanceof net.minecraft.world.level.block.state.properties.IntegerProperty value&&value.getPossibleValues().contains(8))state=state.setValue(value,8);level.setBlockAndUpdate(at,state);}}if(taint&&tc4revival.config.TCConfig.SERVER.generates(tc4revival.config.TCConfig.WorldFeature.TAINT)&&level.random.nextInt(2)==0)tc4revival.content.WorldBiomeChanges.changeColumn(server,at,tc4revival.content.TCBiomes.TAINTED_LAND);}}
    protected boolean access(){return index==1&&master()!=null;}
    @Override public AspectList getAspects(){var master=master();return AspectList.from(access()?master.contents.copy():new AspectAmounts());}
    @Override public void setAspects(AspectList values){var master=master();if(!access())return;master.contents=new AspectAmounts();for(var aspect:values.unwrap().aspects())master.contents.add(aspect,Math.min(master.storage(),values.unwrap().amount(aspect)));master.changed();}
    @Override public boolean accepts(Aspect aspect){return aspect!=null&&access()&&master().contents.amount(aspect)<master().storage();}
    @Override public int capacity(Aspect aspect){return access()?master().storage():0;}
    @Override public boolean contains(Aspect aspect,int count){return access()&&count>=0&&master().contents.amount(aspect)>=count;}
    @Override public int availableAmount(Aspect aspect){return access()?master().contents.amount(aspect):0;}
    @Override public Object availabilityIdentity(){return master()==null?this:master();}
    @Override public boolean takeFromContainer(Aspect aspect,int count){if(!contains(aspect,count))return false;var master=master();master.contents.reduce(aspect,count);if(master.contents.amount(aspect)==0)master.contents.remove(aspect);master.changed();return true;}
    @Override public boolean connectable(Direction face){return false;}
    @Override public boolean canInput(Direction face){return access()&&face!=null&&face.getAxis().isHorizontal();}
    @Override public boolean canOutput(Direction face){return canInput(face);}
    @Override public int suction(Direction face){return canInput(face)?16:0;}@Override public Aspect suctionType(Direction face){return null;}@Override public int minimumSuction(){return 0;}
    @Override public Aspect essentiaType(Direction face){var list=getAspects().unwrap().aspects();return list.isEmpty()?null:list.get(level.random.nextInt(list.size()));}
    @Override public int essentiaAmount(Direction face){return (int)Math.min(Integer.MAX_VALUE,getAspects().unwrap().total());}
    @Override public int addEssentia(Aspect aspect,int amount,Direction face){return canInput(face)&&amount>0?amount-addToContainer(aspect,amount):0;}
    @Override public int takeEssentia(Aspect aspect,int amount,Direction face){int taken=canOutput(face)?Math.min(Math.max(0,amount),availableAmount(aspect)):0;return taken>0&&takeFromContainer(aspect,taken)?taken:0;}
    public void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);if(structure!=null)tag.putUUID("Structure",structure);tag.putInt("Index",index);tag.putInt("Stage",stage);AspectNbt.write(tag,contents);}
    @Override public void load(CompoundTag tag){super.load(tag);structure=tag.hasUUID("Structure")?tag.getUUID("Structure"):null;index=tag.getInt("Index");stage=Math.max(0,Math.min(8,tag.getInt("Stage")));contents=AspectNbt.read(tag,TCAspects.catalog()).amounts();pedestal=tag.contains("VisualPedestal")?BlockPos.of(tag.getLong("VisualPedestal")):null;consumeTick=0;}
    @Override public CompoundTag getUpdateTag(){var tag=saveWithoutMetadata();if(pedestal!=null)tag.putLong("VisualPedestal",pedestal.asLong());return tag;}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    @Override public AABB getRenderBoundingBox(){return new AABB(worldPosition.below(Math.max(0,index))).expandTowards(1,3,1);}
}
