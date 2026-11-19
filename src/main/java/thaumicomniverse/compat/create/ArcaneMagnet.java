package thaumicomniverse.compat.create;

import com.simibubi.create.content.kinetics.base.*;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.behaviour.*;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.*;
import com.simibubi.create.foundation.gui.AllIcons;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import tc4revival.aspect.TCAspects;
import tc4revival.content.VisNetwork;

public class ArcaneMagnet extends DirectionalKineticBlock implements IBE<ArcaneMagnet.Tile> {
    public ArcaneMagnet(){super(Properties.copy(Blocks.IRON_BLOCK).strength(3).noOcclusion());}
    @Override public Class<Tile> getBlockEntityClass(){return Tile.class;}
    @Override public BlockEntityType<? extends Tile> getBlockEntityType(){return ArcaneCreateContent.MAGNET_TILE.get();}
    @Override public Direction.Axis getRotationAxis(BlockState state){return state.getValue(FACING).getAxis();}
    @Override public boolean hasShaftTowards(LevelReader level,BlockPos pos,BlockState state,Direction side){return side==state.getValue(FACING).getOpposite();}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!player.getItemInHand(hand).isEmpty()||hit.getDirection()!=state.getValue(FACING))return InteractionResult.PASS;
        if(!level.isClientSide&&level.getBlockEntity(pos) instanceof Tile tile){tile.easing=(tile.easing+1)%3;tile.notifyUpdate();player.displayClientMessage(Component.translatable("thaumicomniverse.flight.easing."+tile.easing),true);}
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    public static double range(float rpm,int air,int order){return 16+6*Math.max(0,Math.min(4,air))+6*Math.max(0,Math.min(4,order));}
    public static boolean inCone(Vec3 from,Vec3 target,Direction facing,double range){var delta=target.subtract(from);double along=delta.dot(Vec3.atLowerCornerOf(facing.getNormal()));return along>1.01&&delta.lengthSqr()<=range*range&&delta.lengthSqr()-along*along<=Math.pow(.5+along*.4,2);}
    public static boolean casing(BlockState state){return state.is(ArcaneCreateContent.MAGNETIC_CASING.get());}
    // Preserve saved option indices; only new blocks receive the new default.
    public enum Arrival implements INamedIconOptions {
        KEEP(AllIcons.I_MOVE_NEVER_PLACE),PLACE(AllIcons.I_MOVE_PLACE);
        final AllIcons icon;Arrival(AllIcons icon){this.icon=icon;}public AllIcons getIcon(){return icon;}public String getTranslationKey(){return "thaumicomniverse.flight.arrival."+name().toLowerCase(java.util.Locale.ROOT);}
    }
    public static class Tile extends KineticBlockEntity {
        private ScrollOptionBehaviour<Arrival> arrival;
        private int easing,air,order;
        private java.util.UUID active;
        public Tile(BlockPos pos,BlockState state){super(ArcaneCreateContent.MAGNET_TILE.get(),pos,state);}
        @Override public void addBehaviours(java.util.List<BlockEntityBehaviour> list){
            super.addBehaviours(list);
            arrival=new ScrollOptionBehaviour<>(Arrival.class,Component.translatable("thaumicomniverse.flight.arrival"),this,new com.simibubi.create.content.contraptions.DirectionalExtenderScrollOptionSlot((state,face)->face.getAxis()!=state.getValue(FACING).getAxis()));
            arrival.value=Arrival.PLACE.ordinal();list.add(arrival);
        }
        @Override protected Block getStressConfigKey(){return com.simibubi.create.AllBlocks.MECHANICAL_PISTON.get();}
        private record Target(ArcaneMobileEntity entity,BlockPos block,double distance){}
        @Override public void tick(){
            super.tick();
            if(!(level instanceof ServerLevel server)||!ArcaneContraptions.enabled()||getSpeed()==0)return;
            if(active!=null){
                var moving=server.getEntity(active);
                if(moving instanceof ArcaneMobileEntity entity&&worldPosition.equals(entity.controller())&&entity.travelling())return;
                active=null;
            }
            if(reconcileAdjacent(server)||level.hasNeighborSignal(worldPosition))return;
            air=VisNetwork.drain(level,worldPosition,TCAspects.catalog().require("aer"),4);
            order=VisNetwork.drain(level,worldPosition,TCAspects.catalog().require("ordo"),4);
            if(level.getGameTime()%10!=0)return;
            var facing=getBlockState().getValue(FACING);double reach=range(getSpeed(),air,order);var center=worldPosition.getCenter();
            Target nearest=null;
            for(var entity:level.getEntitiesOfClass(ArcaneMobileEntity.class,new AABB(worldPosition).inflate(reach))){
                if(entity.getContraption()==null||entity.getContraption().disassembled||!entity.canRequestMagnet(worldPosition,Math.abs(getSpeed())))continue;
                for(var info:entity.getContraption().getBlocks().values()){
                    if(!casing(info.state()))continue;
                    var global=entity.toGlobalVector(info.pos().getCenter(),1);
                    if(!inCone(center,global,facing,reach))continue;
                    double distance=center.distanceToSqr(global);
                    if(nearest==null||distance<nearest.distance())nearest=new Target(entity,info.pos(),distance);
                }
            }
            // Casing has no block entity. Skip palettes without it rather than scanning air blocks.
            int r=(int)Math.ceil(reach);var cursor=new BlockPos.MutableBlockPos();
            for(int cx=(worldPosition.getX()-r)>>4;cx<=(worldPosition.getX()+r)>>4;cx++)for(int cz=(worldPosition.getZ()-r)>>4;cz<=(worldPosition.getZ()+r)>>4;cz++){
                var chunk=server.getChunkSource().getChunkNow(cx,cz);if(chunk==null)continue;
                var sections=chunk.getSections();
                for(int s=0;s<sections.length;s++){
                    int bottom=chunk.getSectionYFromSectionIndex(s)<<4;
                    if(bottom>worldPosition.getY()+r||bottom+15<worldPosition.getY()-r||!sections[s].maybeHas(ArcaneMagnet::casing))continue;
                    for(int x=0;x<16;x++)for(int y=0;y<16;y++)for(int z=0;z<16;z++){
                        if(!casing(sections[s].getBlockState(x,y,z)))continue;
                        cursor.set((cx<<4)+x,bottom+y,(cz<<4)+z);
                        if(!inCone(center,cursor.getCenter(),facing,reach))continue;
                        double distance=cursor.distSqr(worldPosition);
                        if(nearest==null||distance<nearest.distance())nearest=new Target(null,cursor.immutable(),distance);
                    }
                }
            }
            if(nearest==null)return;
            if(nearest.entity()!=null){attract(nearest.entity(),nearest.block());return;}
            try {var entity=ArcaneMobileEntity.assemble(server,nearest.block(),false,worldPosition);if(entity!=null)attract(entity,BlockPos.ZERO);}
            catch(com.simibubi.create.content.contraptions.AssemblyException ignored){}
        }
        /** Move the contacted casing to the face, even when it is far from the structure anchor. */
        public boolean attract(ArcaneMobileEntity entity,BlockPos local){
            if(!ArcaneContraptions.enabled()||getSpeed()==0||level.hasNeighborSignal(worldPosition)||occupied(entity))return false;
            var info=entity.getContraption().getBlocks().get(local);if(info==null||!casing(info.state()))return false;
            var target=contact().getCenter();
            var destination=entity.position().add(target.subtract(entity.toGlobalVector(local.getCenter(),1)));
            if(entity.requestMagnet(worldPosition,destination,Math.abs(getSpeed()),easing,arrival.get()==Arrival.PLACE,local)){active=entity.getUUID();setChanged();return true;}
            return false;
        }
        public boolean attract(ArcaneMobileEntity entity){
            return entity.getContraption().getBlocks().values().stream().filter(i->casing(i.state()))
                .min(java.util.Comparator.comparingDouble(i->entity.toGlobalVector(i.pos().getCenter(),1).distanceToSqr(worldPosition.getCenter())))
                .map(i->attract(entity,i.pos())).orElse(false);
        }
        private BlockPos contact(){return worldPosition.relative(getBlockState().getValue(FACING));}
        private Target adjacent(ArcaneMobileEntity except){
            var at=contact();
            for(var entity:level.getEntitiesOfClass(ArcaneMobileEntity.class,new AABB(at).inflate(1))){
                if(entity==except||entity.getContraption()==null||entity.getContraption().disassembled)continue;
                for(var info:entity.getContraption().getBlocks().values())if(casing(info.state())&&entity.toGlobalVector(info.pos().getCenter(),1).distanceToSqr(at.getCenter())<.01)
                    return new Target(entity,info.pos(),0);
            }return null;
        }
        private boolean occupied(ArcaneMobileEntity except){return casing(level.getBlockState(contact()))||adjacent(except)!=null;}
        private boolean reconcileAdjacent(ServerLevel server){
            var held=adjacent(null);
            if(held!=null){
                if(!held.entity().travelling()&&arrival.get()==Arrival.PLACE)held.entity().disassemble();
                return true;
            }
            if(!casing(level.getBlockState(contact())))return false;
            if(arrival.get()==Arrival.KEEP)try{
                var entity=ArcaneMobileEntity.assemble(server,contact(),false,worldPosition);
                if(entity!=null)entity.requestMagnet(worldPosition,entity.position(),Math.abs(getSpeed()),easing,false,BlockPos.ZERO);
            }catch(com.simibubi.create.content.contraptions.AssemblyException ignored){}
            return true;
        }
        @Override protected void write(CompoundTag tag,boolean client){super.write(tag,client);tag.putInt("Easing",easing);if(active!=null)tag.putUUID("Moving",active);}
        @Override protected void read(CompoundTag tag,boolean client){super.read(tag,client);easing=Math.floorMod(tag.getInt("Easing"),3);active=tag.hasUUID("Moving")?tag.getUUID("Moving"):null;}
    }
}
