package thaumicomniverse.compat.create;

import java.util.*;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.*;
import com.simibubi.create.content.kinetics.belt.behaviour.*;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.TCAspects;
import tc4revival.content.*;
import thaumcraft.api.aspects.IEssentiaStorage;

public class EssentiaSpout extends Block implements IBE<EssentiaSpout.Tile>,IWrenchable {
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty LIT=net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT;
    public EssentiaSpout(){super(Properties.copy(Blocks.OAK_PLANKS).strength(2.5f).noOcclusion().lightLevel(s->s.getValue(LIT)?9:0));registerDefaultState(defaultBlockState().setValue(LIT,false));}
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block,BlockState> builder){builder.add(LIT);}
    @Override public Class<Tile> getBlockEntityClass(){return Tile.class;}
    @Override public BlockEntityType<? extends Tile> getBlockEntityType(){return ArcaneCreateContent.SPOUT_TILE.get();}
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return com.simibubi.create.AllShapes.SPOUT;}
    @Override public boolean hasAnalogOutputSignal(BlockState state){return true;}
    @Override public int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos){return level.getBlockEntity(pos) instanceof Tile tile?(tile.amount==0?0:1+14*tile.amount/64):0;}
    public static class Tile extends SmartBlockEntity implements IEssentiaStorage,EssentiaTransport,RevealedAspectContainer {
        private Aspect aspect;private int amount;public int processingTicks=-1;
        private com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour outputFilter;
        public Tile(BlockPos pos,BlockState state){super(ArcaneCreateContent.SPOUT_TILE.get(),pos,state);}
        @Override public void addBehaviours(List<BlockEntityBehaviour> list){
            list.add(new BeltProcessingBehaviour(this).whenItemEnters(this::enter).whileItemHeld(this::process));
            outputFilter=new com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour(this,new CenteredSideValueBoxTransform((state,side)->side.getAxis().isHorizontal()));
            list.add(outputFilter);
        }
        private static final String PROGRESS="OmniCrucibleSpouting";
        private record Operation(ArcaneProcessingRecipe recipe,Aspect aspect,int cost,int index,int stages,boolean crucible){}
        private Operation operation(ArcaneProcessingRecipe recipe,ItemStack stack,boolean crucible){
            var costs=recipe.aspects();int index=0;
            if(crucible&&stack.hasTag()){
                var progress=stack.getTag().getCompound(PROGRESS);
                if(progress.getString("Recipe").equals(recipe.getId().toString())&&progress.getString("Sequence").equals(sequence(stack)))index=progress.getInt("Index");
            }
            int stages=costs.view().values().stream().mapToInt(n->(n+63)/64).sum();
            if(index<0||index>=stages)return null;
            int offset=index;Aspect selected=null;int count=0;
            for(var a:costs.alphabetical()){
                int segments=(costs.amount(a)+63)/64;
                if(offset<segments){selected=a;count=Math.min(64,costs.amount(a)-64*offset);break;}
                offset-=segments;
            }
            if(selected==null||aspect!=null&&!aspect.equals(selected)||!EssentiaFilter.test(outputFilter.getFilter(),selected,level,recipe.selectionOutput(level.registryAccess())))return null;
            return new Operation(recipe,selected,count,index,stages,crucible);
        }
        private static String sequence(ItemStack stack){return stack.hasTag()?stack.getTag().getCompound("SequencedAssembly").toString():"{}";}
        private Operation recipe(ItemStack stack){
            if(!ArcaneContraptions.enabled())return null;
            boolean intermediate=stack.hasTag()&&stack.getTag().contains("SequencedAssembly");
            if(!intermediate){
                var direct=level.getRecipeManager().getAllRecipesFor(ArcaneAssembly.ESSENTIA.type.get()).stream()
                    .filter(r->!r.getIngredients().isEmpty()&&r.getIngredients().get(0).test(stack))
                    .sorted(Comparator.comparing(r->r.getId().toString())).map(r->operation(r,stack,false)).filter(Objects::nonNull).findFirst();
                if(direct.isPresent())return direct.get();
            }
            var ordinary=SequencedAssemblyRecipe.getRecipes(level,stack,ArcaneAssembly.ESSENTIA.type.get(),ArcaneProcessingRecipe.class)
                .sorted(Comparator.comparing(r->r.getId().toString())).map(r->operation(r,stack,false)).filter(Objects::nonNull).findFirst();
            if(ordinary.isPresent())return ordinary.get();
            return SequencedAssemblyRecipe.getRecipes(level,stack,ArcaneAssembly.CRUCIBLE.type.get(),ArcaneProcessingRecipe.class)
                .sorted(Comparator.comparing(r->r.getId().toString())).map(r->operation(r,stack,true)).filter(Objects::nonNull).findFirst().orElse(null);
        }
        private BeltProcessingBehaviour.ProcessingResult enter(TransportedItemStack input,TransportedItemStackHandlerBehaviour handler){return recipe(input.stack)==null?BeltProcessingBehaviour.ProcessingResult.PASS:BeltProcessingBehaviour.ProcessingResult.HOLD;}
        private BeltProcessingBehaviour.ProcessingResult process(TransportedItemStack input,TransportedItemStackHandlerBehaviour handler){
            var operation=recipe(input.stack);if(operation==null)return BeltProcessingBehaviour.ProcessingResult.PASS;
            if(processingTicks!=-1&&processingTicks!=5)return BeltProcessingBehaviour.ProcessingResult.HOLD;
            if(amount<operation.cost)return BeltProcessingBehaviour.ProcessingResult.HOLD;
            if(processingTicks==-1){processingTicks=20;notifyUpdate();return BeltProcessingBehaviour.ProcessingResult.HOLD;}
            ItemStack result;
            if(operation.crucible&&operation.index+1<operation.stages){
                result=input.stack.copyWithCount(1);var tag=new CompoundTag();tag.putString("Recipe",operation.recipe.getId().toString());tag.putString("Sequence",sequence(input.stack));tag.putInt("Index",operation.index+1);result.getOrCreateTag().put(PROGRESS,tag);
            }else {
                var results=operation.recipe.rollResults();if(results.isEmpty())return BeltProcessingBehaviour.ProcessingResult.PASS;
                result=results.get(0).copy();if(result.hasTag())result.getTag().remove(PROGRESS);
            }
            var output=input.copy();output.stack=result;output.clearFanProcessingData();
            input.stack.shrink(1);var remaining=input.stack.isEmpty()?null:input.copy();
            extract(operation.aspect,operation.cost,false);
            handler.handleProcessingOnItem(input,TransportedItemStackHandlerBehaviour.TransportedResult.convertToAndLeaveHeld(List.of(output),remaining));
            notifyUpdate();return BeltProcessingBehaviour.ProcessingResult.HOLD;
        }
        @Override public void tick(){
            super.tick();if(level==null)return;
            if(processingTicks>=0)processingTicks--;
            if(processingTicks<0&&amount==0&&aspect!=null){aspect=null;if(!level.isClientSide)notifyUpdate();}
            if(level.isClientSide){
                if(processingTicks>=0&&processingTicks<=12&&aspect!=null)CreateArcaneClient.spoutParticle(this);return;
            }
            if(!ArcaneContraptions.enabled()||level.getGameTime()%5!=0||amount>=64)return;
            for(var side:Direction.values()){
                if(side==Direction.DOWN||!level.hasChunkAt(worldPosition.relative(side)))continue;
                if(!(level.getBlockEntity(worldPosition.relative(side)) instanceof EssentiaTransport source)||!source.canOutput(side.getOpposite())||source.suction(side.getOpposite())>=64||source.minimumSuction()>64)continue;
                var a=source.essentiaType(side.getOpposite());if(a==null||insert(a,1,true)==0)continue;
                int taken=source.takeEssentia(a,1,side.getOpposite());if(taken>0){insert(a,taken,false);break;}
            }
        }
        public Aspect visualAspect(){return aspect;}
        public int amount(){return amount;}
        @Override public void notifyUpdate(){
            if(level!=null&&!level.isClientSide&&getBlockState().getValue(LIT)!=(amount>0))level.setBlock(worldPosition,getBlockState().setValue(LIT,amount>0),3);
            super.notifyUpdate();
        }
        @Override public AspectAmounts contents(){return aspect==null?new AspectAmounts():new AspectAmounts().add(aspect,amount);}
        @Override public AspectAmounts revealedAspects(){return contents();}
        @Override public int capacity(Aspect a){return aspect==null||aspect.equals(a)?64:0;}
        @Override public int insert(Aspect a,int n,boolean simulate){if(!ArcaneContraptions.enabled()||n<=0||a==null)return 0;int accepted=Math.min(n,Math.max(0,capacity(a)-amount));if(accepted>0&&!simulate){aspect=a;amount+=accepted;notifyUpdate();}return accepted;}
        @Override public int extract(Aspect a,int n,boolean simulate){int taken=a!=null&&a.equals(aspect)?Math.min(Math.max(0,n),amount):0;if(taken>0&&!simulate){amount-=taken;if(amount==0&&processingTicks<0)aspect=null;notifyUpdate();}return taken;}
        @Override public boolean connectable(Direction side){return side!=Direction.DOWN;}
        @Override public boolean canInput(Direction side){return connectable(side);}
        @Override public boolean canOutput(Direction side){return false;}
        @Override public Aspect suctionType(Direction side){return aspect;}
        @Override public int suction(Direction side){return ArcaneContraptions.enabled()&&amount<64?64:0;}
        @Override public int minimumSuction(){return 0;}
        @Override public Aspect essentiaType(Direction side){return aspect;}
        @Override public int essentiaAmount(Direction side){return amount;}
        @Override public int takeEssentia(Aspect a,int n,Direction side){return 0;}
        @Override public int addEssentia(Aspect a,int n,Direction side){return canInput(side)?insert(a,n,false):0;}
        @Override protected void write(CompoundTag tag,boolean client){super.write(tag,client);if(aspect!=null)tag.putString("Aspect",aspect.tag());tag.putInt("Amount",amount);tag.putInt("ProcessingTicks",processingTicks);}
        @Override protected void read(CompoundTag tag,boolean client){super.read(tag,client);aspect=TCAspects.catalog().find(tag.getString("Aspect")).orElse(null);amount=aspect==null?0:Math.max(0,Math.min(64,tag.getInt("Amount")));processingTicks=tag.contains("ProcessingTicks")?Math.max(-1,Math.min(20,tag.getInt("ProcessingTicks"))):-1;}
    }
}
