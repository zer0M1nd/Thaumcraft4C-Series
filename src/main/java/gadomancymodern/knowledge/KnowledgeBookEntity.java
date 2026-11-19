package gadomancymodern.knowledge;
import gadomancymodern.Gadomancy;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import tc4revival.aspect.*;
import tc4revival.content.*;
import tc4revival.research.*;
import java.util.*;
public class KnowledgeBookEntity extends BlockEntity implements RevealedAspectContainer {
    public static final Map<BlockState,Integer> KNOWLEDGE_VALUES=new HashMap<>();
    public static java.util.function.Consumer<KnowledgeBookEntity> CLIENT_EFFECTS=book->{};
    public int cognition,ticks,heartbeatAge,surroundingKnowledge;
    public boolean researching,turnedPages;
    public float rotation,previousRotation,targetRotation,open,page,previousPage,targetPage,pageVelocity;
    public ItemStack stored=ItemStack.EMPTY;
    public Map<String,Integer> work=new LinkedHashMap<>();
    private UUID heldId,finishId;
    public KnowledgeBookEntity(BlockPos pos,BlockState state){super(Gadomancy.KNOWLEDGE_BOOK_BE.get(),pos,state);}
    public static void tick(Level world,BlockPos pos,BlockState state,KnowledgeBookEntity book){book.tick();}
    public void tick(){
        animate();ticks++;heartbeatAge++;
        if(!level.isClientSide){
            if(heartbeatAge>8&&(!stored.isEmpty()||researching)){stop();heldId=null;changed();}
            var note=validNote(stored);
            if(researching&&note==null){stop();changed();}
            else if(!researching&&note!=null){work=KnowledgeWork.initial(ResearchCatalogs.catalog().require(note.key()).aspects());researching=true;changed();}
            if(researching){drainCognition();if(cognition>0){checkSurroundings();if(level.random.nextInt(KnowledgeWork.chance(surroundingKnowledge))==0)progress();}}
        }else if(researching&&cognition>0)CLIENT_EFFECTS.accept(this);
        if(stored.isEmpty())attract();
    }
    public void heartbeat(HeldResearchEntity entity){
        heartbeatAge=0;heldId=entity.getUUID();
        if(heldId.equals(finishId)){
            var note=ResearchNoteNbt.read(entity.getItem().getTag(),TCAspects.catalog()).note().orElse(null);
            if(note!=null){var completed=ResearchNotesItem.create(new ResearchNote(note.key(),note.color(),true,note.copies(),note.grid()));if(entity.getItem().hasTag()){completed.setTag(entity.getItem().getTag().copy());ResearchNoteNbt.write(completed.getOrCreateTag(),new ResearchNote(note.key(),note.color(),true,note.copies(),note.grid()));}entity.setItem(completed);}
            finishId=null;
        }
        var next=entity.getItem();if(!ItemStack.matches(stored,next)){stored=next.copy();changed();}
    }
    public static ResearchNote validNote(ItemStack stack){return validNote(stack,false);}
    private static ResearchNote validNote(ItemStack stack,boolean client){if(stack.isEmpty()||!(stack.getItem() instanceof ResearchNotesItem))return null;var read=ResearchNoteNbt.read(stack.getTag(),TCAspects.catalog());if(!read.problems().isEmpty())return null;return read.note().filter(note->!note.complete()&&(client?ResearchCatalogs.bundled():ResearchCatalogs.catalog()).find(note.key()).isPresent()).orElse(null);}
    public void drainCognition(){
        if(cognition>0){cognition--;if(cognition==0)changed();}
        if(cognition<=40&&ticks%32==0){var aspect=TCAspects.catalog().require("cognitio");for(var direction:Direction.values()){var source=EssentiaSources.drain(this,aspect,direction,4,false);if(source.isPresent()){cognition+=150;tc4revival.network.EssentiaSourcePacket.send((ServerLevel)level,worldPosition,source.get(),aspect.color());changed();break;}}}
    }
    public void checkSurroundings(){
        surroundingKnowledge=0;for(var mutable:BlockPos.betweenClosed(worldPosition.offset(-4,-3,-4),worldPosition.offset(4,3,4))){var pos=mutable.immutable();if(!level.hasChunkAt(pos))continue;var state=level.getBlockState(pos);var tile=level.getBlockEntity(pos);int value=state.is(Blocks.BOOKSHELF)?1:tile instanceof KnowledgeProvider provider?provider.providedKnowledge(level,pos):state.getBlock() instanceof KnowledgeProvider provider?provider.providedKnowledge(level,pos):KNOWLEDGE_VALUES.getOrDefault(state,0);surroundingKnowledge+=value;}
    }
    public void progress(){KnowledgeWork.progress(work,work.isEmpty()?0:level.random.nextInt(work.size()));if(work.isEmpty())finish();else changed();}
    private void finish(){finishId=heldId;stop();if(level instanceof ServerLevel server){tc4revival.network.FocusEffectPacket.blockSparkle(server,worldPosition,-9999,10);for(var direction:Direction.values())tc4revival.network.FocusEffectPacket.blockSparkle(server,worldPosition.relative(direction),-9999,10);}changed();}
    private void stop(){stored=ItemStack.EMPTY;researching=false;work.clear();}
    public void attract(){
        var area=new AABB(worldPosition.getX()-.5,worldPosition.getY()-.1,worldPosition.getZ()-.5,worldPosition.getX()+.5,worldPosition.getY()+.9,worldPosition.getZ()+.5).inflate(8);
        var center=new Vec3(worldPosition.getX()+.5,worldPosition.getY()+.9,worldPosition.getZ()+.5);
        // Attraction also predicts motion on a remote client, whose catalog is the server-synced display snapshot.
        var nearest=level.getEntitiesOfClass(ItemEntity.class,area,item->item.isAlive()&&!(item instanceof SpecialItemEntity)&&validNote(item.getItem(),level.isClientSide)!=null).stream().min(Comparator.comparingDouble(item->item.position().distanceToSqr(center))).orElse(null);if(nearest==null)return;
        if(!level.isClientSide&&nearest.position().distanceToSqr(center)<1){
            var captured=nearest.getItem().copyWithCount(1);var held=new HeldResearchEntity(level,worldPosition,captured);
            if(level.addFreshEntity(held)){var remaining=nearest.getItem().copy();remaining.shrink(1);nearest.setItem(remaining);if(remaining.isEmpty())nearest.discard();nearest.noPhysics=false;heldId=held.getUUID();stored=captured.copy();heartbeatAge=0;changed();}
        }else{
            nearest.noPhysics=true;var delta=Vec3.atCenterOf(worldPosition).subtract(nearest.position()).scale(1/15d);double distance=delta.length();if(distance>1e-9&&distance<1){double power=(1-distance)*(1-distance);nearest.setDeltaMovement(nearest.getDeltaMovement().add(delta.x/distance*power*.15,delta.y/distance*power*.25,delta.z/distance*power*.15));}
        }
    }
    private void animate(){
        previousRotation=rotation;previousPage=page;var player=level.getNearestPlayer(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,3,false);
        if(player!=null){targetRotation=(float)Math.atan2(player.getZ()-worldPosition.getZ()-.5,player.getX()-worldPosition.getX()-.5);open+=.1f;if(open<.5f||level.random.nextInt(40)==0){float prior=targetPage;do{targetPage+=level.random.nextInt(4)-level.random.nextInt(4);}while(prior==targetPage);}}
        else{targetRotation+=.02f;open-=.1f;}rotation=wrap(rotation);targetRotation=wrap(targetRotation);rotation+=wrap(targetRotation-rotation)*.4f;open=Mth.clamp(open,0,1);float step=Mth.clamp((targetPage-page)*.4f,-.2f,.2f);pageVelocity+=(step-pageVelocity)*.9f;page+=pageVelocity;
    }
    public static float wrap(float value){while(value>=Math.PI)value-=Math.PI*2;while(value< -Math.PI)value+=Math.PI*2;return value;}
    @Override public AspectAmounts revealedAspects(){var result=new AspectAmounts();work.forEach((aspect,amount)->TCAspects.catalog().find(aspect).ifPresent(a->result.add(a,amount)));return result;}
    public void changed(){setChanged();if(level!=null&&!level.isClientSide){level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);level.updateNeighbourForOutputSignal(worldPosition,getBlockState().getBlock());}}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putInt("cognitio",cognition);tag.putBoolean("researching",researching);if(!stored.isEmpty())tag.put("crystalStack",stored.save(new CompoundTag()));var aspects=new CompoundTag();work.forEach(aspects::putInt);tag.put("workAspects",aspects);if(heldId!=null)tag.putUUID("Held",heldId);if(finishId!=null)tag.putUUID("Finish",finishId);}
    @Override public void load(CompoundTag tag){super.load(tag);cognition=Math.max(0,tag.getInt("cognitio"));researching=tag.getBoolean("researching");stored=ItemStack.of(tag.getCompound("crystalStack"));work=new LinkedHashMap<>();var aspects=tag.getCompound("workAspects");aspects.getAllKeys().forEach(key->{int value=aspects.getInt(key);if(value>0)work.put(key,value);});heldId=tag.hasUUID("Held")?tag.getUUID("Held"):null;finishId=tag.hasUUID("Finish")?tag.getUUID("Finish"):null;}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
