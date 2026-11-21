package thaumicomniverse.compat.create;

import java.util.*;
import com.simibubi.create.content.logistics.depot.*;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.behaviour.*;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** Two stationary endpoints share one travelling plate. Cargo has one owner at every phase. */
public class FloatingDepot extends DepotBlock {
    public static final int MAX_LINK_DISTANCE=8;
    public static boolean withinLinkRange(BlockPos input,BlockPos output){return input.distManhattan(output)<=MAX_LINK_DISTANCE;}
    public FloatingDepot(){super(Properties.copy(Blocks.OAK_PLANKS).strength(3).noOcclusion());}
    @Override public BlockEntityType<? extends DepotBlockEntity> getBlockEntityType(){return ArcaneCreateContent.DEPOT_TILE.get();}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(level.getBlockEntity(pos) instanceof Tile tile&&(tile.output()||tile.phase()!=Tile.HOME)){
            if(hit.getDirection()!=Direction.UP)return InteractionResult.PASS;
            if(!level.isClientSide){
                // Create's manual swap bypasses DepotBehaviour.onlyAccepts; these endpoints allow extraction only.
                var inventory=tile.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).resolve().orElse(null);
                boolean extracted=false;
                if(inventory!=null)for(int slot=0;slot<inventory.getSlots();slot++){
                    var stack=inventory.extractItem(slot,64,false);
                    if(!stack.isEmpty()){player.getInventory().placeItemBackInInventory(stack);extracted=true;}
                }
                if(extracted){
                    level.playSound(null,pos,net.minecraft.sounds.SoundEvents.ITEM_PICKUP,net.minecraft.sounds.SoundSource.PLAYERS,.2f,1);
                    tile.notifyUpdate();
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.use(state,level,pos,player,hand,hit);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!next.is(this)&&!level.isClientSide&&level.getBlockEntity(pos) instanceof Tile tile)tile.detach();
        super.onRemove(state,level,pos,next,moving);
    }
    public static class PlacementItem extends BlockItem {
        public PlacementItem(Block block){super(block,new Item.Properties());}
        @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context){
            var player=context.getPlayer();
            if(player==null||!player.isShiftKeyDown())return InteractionResult.PASS;
            if(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof Tile tile&&!tile.output()&&tile.peer==null){
                if(!context.getLevel().isClientSide){
                    var tag=stack.getOrCreateTag();tag.putLong("InputDepot",context.getClickedPos().asLong());
                    tag.putString("InputDimension",context.getLevel().dimension().location().toString());
                    player.displayClientMessage(Component.translatable("omniverse.depot.selected",context.getClickedPos().toShortString()),true);
                }
            }
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        @Override public InteractionResult place(BlockPlaceContext context){
            var tag=context.getItemInHand().getTag();
            if(tag!=null&&tag.contains("InputDepot")){
                boolean valid=tag.getString("InputDimension").equals(context.getLevel().dimension().location().toString())
                        &&withinLinkRange(BlockPos.of(tag.getLong("InputDepot")),context.getClickedPos());
                if(!valid){
                    if(context.getPlayer()!=null&&!context.getLevel().isClientSide)context.getPlayer().displayClientMessage(
                            Component.translatable("omniverse.depot.out_of_range",MAX_LINK_DISTANCE),true);
                    return InteractionResult.FAIL;
                }
            }
            return super.place(context);
        }
        @Override public boolean canAttackBlock(BlockState state,Level level,BlockPos pos,Player player){
            if(!player.isShiftKeyDown())return true;
            var tag=player.getMainHandItem().getTag();
            if(tag!=null&&!level.isClientSide){tag.remove("InputDepot");tag.remove("InputDimension");}
            return false;
        }
        @Override protected boolean updateCustomBlockEntityTag(BlockPos pos,Level level,Player player,ItemStack stack,BlockState state){
            boolean changed=super.updateCustomBlockEntityTag(pos,level,player,stack,state);
            var tag=stack.getTag();
            if(!level.isClientSide&&tag!=null&&tag.contains("InputDepot")&&tag.getString("InputDimension").equals(level.dimension().location().toString())
                    &&level.getBlockEntity(pos) instanceof Tile output){
                var inputPos=BlockPos.of(tag.getLong("InputDepot"));
                if(level.hasChunkAt(inputPos)&&level.getBlockEntity(inputPos) instanceof Tile input&&input.link(output)){
                    tag.remove("InputDepot");tag.remove("InputDimension");
                    if(player!=null)player.displayClientMessage(Component.translatable("omniverse.depot.linked"),true);
                }
            }
            return changed;
        }
        @Override public void appendHoverText(ItemStack stack,Level level,List<Component> tooltip,TooltipFlag flag){
            super.appendHoverText(stack,level,tooltip,flag);tooltip.add(Component.translatable("omniverse.depot.select_hint"));
            if(stack.hasTag()&&stack.getTag().contains("InputDepot"))tooltip.add(Component.translatable("omniverse.depot.selected",BlockPos.of(stack.getTag().getLong("InputDepot")).toShortString()));
        }
    }
    public static class Tile extends DepotBlockEntity {
        public static final int HOME=0,OUTBOUND=1,WAITING=2,RETURNING=3;
        private BlockPos peer;
        private boolean output;
        private int phase,flightTick,duration=20;
        private int flightStep=1;
        private double[] arcLengths;
        private BlockPos pathPeer;
        private ItemStack cargo=ItemStack.EMPTY;
        private ScrollValueBehaviour threshold;
        public Tile(BlockPos pos,BlockState state){super(ArcaneCreateContent.DEPOT_TILE.get(),pos,state);}
        @Override public void addBehaviours(List<BlockEntityBehaviour> list){
            super.addBehaviours(list);
            list.stream().filter(DepotBehaviour.class::isInstance).map(DepotBehaviour.class::cast).forEach(depot->{
                depot.enableMerging();depot.onlyAccepts(stack->!output&&phase==HOME&&ArcaneContraptions.enabled());
            });
            threshold=new ScrollValueBehaviour(Component.translatable("create.weighted_ejector.stack_size"),this,
                    new CenteredSideValueBoxTransform((s,d)->d.getAxis().isHorizontal()))
                    .between(0,64).withFormatter(n->n==0?"*":Integer.toString(n));
            threshold.onlyActiveWhen(()->!output);list.add(threshold);
        }
        private DepotBehaviour depot(){return getBehaviour(DepotBehaviour.TYPE);}
        public boolean output(){return output;}
        public int phase(){return phase;}
        public ItemStack cargo(){return cargo;}
        public BlockPos peer(){return peer;}
        public boolean link(Tile target){
            if(target==this||target.level!=level||!withinLinkRange(worldPosition,target.worldPosition)||peer!=null||target.peer!=null||output||target.output
                    ||phase!=HOME||target.phase!=HOME||!target.depot().isEmpty())return false;
            peer=target.worldPosition;target.peer=worldPosition;target.output=true;
            preparePath();
            target.duration=duration;notifyUpdate();target.notifyUpdate();return true;
        }
        private Tile partner(){
            return peer!=null&&level.hasChunkAt(peer)&&level.getBlockEntity(peer) instanceof Tile tile
                    &&worldPosition.equals(tile.peer)&&tile.output!=output?tile:null;
        }
        private Vec3 path(double t){
            var delta=Vec3.atLowerCornerOf(peer.subtract(worldPosition));
            return delta.scale(t).add(0,Math.sin(Math.PI*t)*Math.min(8,delta.length()*.2),0);
        }
        private void preparePath(){
            if(peer==null||peer.equals(pathPeer)&&arcLengths!=null)return;
            pathPeer=peer;arcLengths=new double[65];var previous=Vec3.ZERO;
            for(int i=1;i<arcLengths.length;i++){var point=path(i/64d);arcLengths[i]=arcLengths[i-1]+point.distanceTo(previous);previous=point;}
            double progress=duration>0?(double)flightTick/duration:0;
            duration=Math.max(1,(int)Math.ceil(arcLengths[64]*4)); // .25 blocks/tick = 5 m/s along the curve.
            flightTick=net.minecraft.util.Mth.clamp((int)Math.round(progress*duration),0,duration);
        }
        private void phase(int value,Tile target){
            phase=value;flightTick=0;notifyUpdate();
            if(target!=null){target.phase=value;target.flightTick=0;target.notifyUpdate();}
        }
        @Override public void tick(){
            super.tick();
            if(getHeldItem().isEmpty())depot().removeHeldItem();
            if(level==null||!ArcaneContraptions.enabled()||peer==null)return;
            if(!withinLinkRange(worldPosition,peer)){
                if(!level.isClientSide){var old=partner();reset();if(old!=null)old.reset();}
                return;
            }
            preparePath();
            // Never load remote chunks or let elapsed wall time teleport the cargo.
            if(!level.hasChunkAt(peer))return;
            var target=partner();
            if(level.isClientSide){
                if(!output&&(phase==OUTBOUND||phase==RETURNING)&&target!=null)flightTick=Math.min(duration,flightTick+flightStep);
                return;
            }
            if(target==null){reset();return;}
            if(output)return;
            switch(phase){
                case HOME->{
                    var held=getHeldItem();
                    if(!level.hasNeighborSignal(worldPosition)&&!held.isEmpty()&&target.depot().isEmpty()
                            &&(threshold.getValue()==0||held.getCount()>=Math.min(held.getMaxStackSize(),threshold.getValue()))){
                        var data=new CompoundTag();depot().write(data,false);
                        if(!data.getList("Incoming",Tag.TAG_COMPOUND).isEmpty()||!depot().isOutputEmpty())return;
                        cargo=held.copy();depot().removeHeldItem();phase(OUTBOUND,target);
                    }
                }
                case OUTBOUND,RETURNING->{
                    int beforeStep=flightStep;
                    flightStep=tc4revival.content.VisNetwork.drain(level,worldPosition,tc4revival.aspect.TCAspects.catalog().require("aer"),1)==1?3:1;
                    flightTick+=flightStep;
                    if(flightTick>=duration){
                        if(phase==OUTBOUND){
                            if(!target.depot().isEmpty())return;
                            target.depot().setCenteredHeldItem(new TransportedItemStack(cargo));
                            cargo=ItemStack.EMPTY;target.notifyUpdate();phase(WAITING,target);
                        }else phase(HOME,target);
                    }else if(level.getGameTime()%5==0||beforeStep!=flightStep)notifyUpdate();
                    setChanged();
                }
                case WAITING->{if(target.depot().isEmpty())phase(RETURNING,target);}
            }
        }
        private void reset(){
            if(!cargo.isEmpty()){
                if(getHeldItem().isEmpty())depot().setCenteredHeldItem(new TransportedItemStack(cargo));
                else Block.popResource(level,worldPosition,cargo);
            }
            peer=null;output=false;phase=HOME;flightTick=0;cargo=ItemStack.EMPTY;notifyUpdate();
        }
        public void detach(){
            var target=partner();
            if(!cargo.isEmpty())Block.popResource(level,worldPosition,cargo);
            cargo=ItemStack.EMPTY;if(target!=null)target.reset();peer=null;
        }
        public Vec3 plateOffset(float partialTick){
            if(peer==null||phase==HOME||phase==WAITING)return Vec3.ZERO;
            preparePath();
            double t=net.minecraft.util.Mth.clamp((flightTick+partialTick*flightStep)/duration,0,1);
            if(phase==RETURNING)t=1-t;
            double distance=t*arcLengths[64];int segment=1;
            while(segment<64&&arcLengths[segment]<distance)segment++;
            double along=(distance-arcLengths[segment-1])/Math.max(1e-9,arcLengths[segment]-arcLengths[segment-1]);
            return path((segment-1+along)/64);
        }
        @Override public AABB getRenderBoundingBox(){
            var box=new AABB(worldPosition);
            return !output&&peer!=null?box.minmax(new AABB(peer)).inflate(1,9,1):box;
        }
        @Override protected void write(CompoundTag tag,boolean client){
            super.write(tag,client);if(peer!=null)tag.putLong("PairedDepot",peer.asLong());
            tag.putBoolean("OutputDepot",output);tag.putInt("PlatePhase",phase);
            tag.putInt("FlightTick",flightTick);tag.putInt("FlightDuration",duration);tag.put("PlateCargo",cargo.save(new CompoundTag()));
            tag.putInt("FlightStep",flightStep);
        }
        @Override protected void read(CompoundTag tag,boolean client){
            super.read(tag,client);peer=tag.contains("PairedDepot")?BlockPos.of(tag.getLong("PairedDepot")):null;
            output=tag.getBoolean("OutputDepot");phase=net.minecraft.util.Mth.clamp(tag.getInt("PlatePhase"),0,3);
            duration=Math.max(1,tag.getInt("FlightDuration"));flightTick=net.minecraft.util.Mth.clamp(tag.getInt("FlightTick"),0,duration);
            flightStep=tag.getInt("FlightStep")==3?3:1;
            cargo=ItemStack.of(tag.getCompound("PlateCargo"));
        }
    }
}
