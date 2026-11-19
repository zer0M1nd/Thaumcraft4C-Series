package thaumicomniverse.compat.create;

import java.util.*;
import java.lang.reflect.*;
import com.simibubi.create.content.contraptions.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import thaumicomniverse.compat.nodemap.NodeMapConfig;

/** A client render handoff, never a server/entity lifetime change. */
public final class ContraptionRenderHandoff {
    private static final Map<Integer,Pending> PENDING=new LinkedHashMap<>();
    private static boolean releasing;
    private static ClientLevel world;
    private static final class Pending {
        final AbstractContraptionEntity entity;
        final StructureTransform transform;
        final Map<BlockPos,Block> blocks=new LinkedHashMap<>();
        final Map<BlockPos,Mesh> sections=new LinkedHashMap<>();
        final long created=System.nanoTime();
        net.minecraft.world.phys.Vec3 pose;
        Entity.RemovalReason reason;
        boolean requested;
        Pending(AbstractContraptionEntity entity,StructureTransform transform){this.entity=entity;this.transform=transform;}
        boolean placed(ClientLevel level,BlockPos pos,Block expected){
            if(!level.hasChunkAt(pos))return false;
            var actual=level.getBlockState(pos);
            if(actual.is(expected))return true;
            // Create's piston contraption maps its moving head/pole onto the
            // stationary piston base. customBlockPlacement changes the base's
            // state there instead of placing that temporary head/pole block.
            return entity.getContraption() instanceof com.simibubi.create.content.contraptions.piston.PistonContraption
                &&(expected instanceof com.simibubi.create.content.contraptions.piston.MechanicalPistonHeadBlock
                   ||expected instanceof com.simibubi.create.content.contraptions.piston.PistonExtensionPoleBlock)
                &&actual.getBlock() instanceof com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock;
        }
    }
    private interface Mesh { boolean uploaded() throws ReflectiveOperationException; }
    public static boolean enabled(){return NodeMapConfig.SPEC.isLoaded()&&NodeMapConfig.SMOOTH_DISASSEMBLY.get();}
    public static void capture(ContraptionDisassemblyPacket packet){
        var level=Minecraft.getInstance().level;
        if(!enabled()||level==null||PENDING.size()>=64)return;
        if(world!=level){PENDING.clear();world=level;}
        var data=(thaumicomniverse.mixin.CreateDisassemblyPacketAccess)packet;
        if(!(level.getEntity(data.omniverse$entity()) instanceof AbstractContraptionEntity entity)||entity.getContraption()==null)return;
        var pending=new Pending(entity,data.omniverse$transform());
        for(var info:entity.getContraption().getBlocks().values()){
            var pos=data.omniverse$transform().apply(info.pos());
            if(level.isOutsideBuildHeight(pos))return;
            pending.blocks.put(pos,info.state().getBlock());
        }
        if(!pending.blocks.isEmpty())PENDING.put(entity.getId(),pending);
    }
    public static boolean defer(ClientLevel level,int id,Entity.RemovalReason reason){
        if(releasing||!enabled()||world!=level)return false;
        var pending=PENDING.get(id);
        if(pending==null||level.getEntity(id)!=pending.entity)return false;
        pending.reason=reason;
        pending.entity.setContraptionMotion(net.minecraft.world.phys.Vec3.ZERO);
        // The last interpolated movement packet may still be short of the final
        // placement anchor. Keep the retained model at the server's placement.
        pending.pose=pending.entity.position().add(net.minecraft.world.phys.Vec3.atLowerCornerOf(pending.transform.offset).subtract(pending.entity.getAnchorVec()));
        pose(pending);
        return true;
    }
    public static boolean frozen(AbstractContraptionEntity entity){
        var pending=PENDING.get(entity.getId());return pending!=null&&pending.entity==entity&&pending.reason!=null;
    }
    private static void pose(Pending pending){
        if(pending.entity instanceof ControlledContraptionEntity controlled){
            var access=(thaumicomniverse.mixin.CreateControlledPositionAccess)controlled;
            if(pending.transform.rotationAxis!=null)controlled.setRotationAxis(pending.transform.rotationAxis);
            access.omniverse$angle(pending.transform.angle);
            access.omniverse$previousAngle(pending.transform.angle);
            access.omniverse$angleDelta(0);
        }
        pending.entity.setPos(pending.pose);
        pending.entity.xo=pending.entity.xOld=pending.pose.x;
        pending.entity.yo=pending.entity.yOld=pending.pose.y;
        pending.entity.zo=pending.entity.zOld=pending.pose.z;
    }
    public static void beforeFrame(net.minecraftforge.event.TickEvent.RenderTickEvent event){
        if(event.phase==net.minecraftforge.event.TickEvent.Phase.START)
            for(var p:PENDING.values())if(p.reason!=null)pose(p);
    }
    public static void frame(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS)return;
        var mc=Minecraft.getInstance();
        if(mc.level!=world){PENDING.clear();world=mc.level;return;}
        for(var it=PENDING.values().iterator();it.hasNext();){
            var p=it.next();boolean finish=!enabled()||mc.level.getEntity(p.entity.getId())!=p.entity;
            // Several assembly/removal packets can be processed in one frame
            // after a client pause. A controller's newer assembly supersedes
            // its old visual even if the intermediate blocks were never drawn.
            if(!finish&&p.entity instanceof ControlledContraptionEntity){
                var controller=((thaumicomniverse.mixin.CreateControlledPositionAccess)p.entity).omniverse$controllerPos();
                if(controller!=null)for(var other:mc.level.entitiesForRendering()){
                    if(other==p.entity||!(other instanceof ControlledContraptionEntity))continue;
                    var newer=PENDING.get(other.getId());
                    if((newer==null||newer.created>p.created)&&controller.equals(((thaumicomniverse.mixin.CreateControlledPositionAccess)other).omniverse$controllerPos())){finish=true;break;}
                }
            }
            // Abort orphaned or out-of-view transitions. This is a failure bound,
            // not the success criterion: normal handoff waits for mesh uploads.
            finish|=System.nanoTime()-p.created>5_000_000_000L
                ||p.entity.distanceToSqr(mc.gameRenderer.getMainCamera().getPosition())>Math.pow(mc.options.getEffectiveRenderDistance()*16+32,2);
            if(!finish&&p.reason!=null)try{
                boolean allPlaced=p.blocks.entrySet().stream().allMatch(e->p.placed(mc.level,e.getKey(),e.getValue()));
                if(p.requested&&!allPlaced)finish=true; // reassembled or replaced before upload
                else if(allPlaced){
                    if(!p.requested){
                        var positions=new LinkedHashSet<BlockPos>();
                        for(var pos:p.blocks.keySet())positions.add(SectionPos.of(pos).origin());
                        for(var origin:positions){var mesh=requestMesh(origin);if(mesh==null){finish=true;break;}p.sections.put(origin,mesh);}
                        p.requested=true;
                    }else{
                        finish=true;for(var mesh:p.sections.values())if(!mesh.uploaded()){finish=false;break;}
                    }
                }
            }catch(ReflectiveOperationException|RuntimeException unavailable){finish=true;}
            if(finish){
                it.remove();
                if(p.reason!=null&&mc.level.getEntity(p.entity.getId())==p.entity){
                    releasing=true;try{mc.level.removeEntity(p.entity.getId(),p.reason);}finally{releasing=false;}
                }
            }
        }
    }
    private static Mesh requestMesh(BlockPos origin)throws ReflectiveOperationException{
        var mc=Minecraft.getInstance();int x=origin.getX()>>4,y=origin.getY()>>4,z=origin.getZ()>>4;
        if(net.minecraftforge.fml.ModList.get().isLoaded("embeddium")){
            Class<?> renderer=Class.forName("me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer");
            Object instance=renderer.getMethod("instanceNullable").invoke(null);if(instance==null)return null;
            var field=renderer.getDeclaredField("renderSectionManager");field.setAccessible(true);var manager=field.get(instance);if(manager==null)return null;
            var find=manager.getClass().getDeclaredMethod("getRenderSection",int.class,int.class,int.class);find.setAccessible(true);
            var section=find.invoke(manager,x,y,z);if(section==null)return null;
            var built=section.getClass().getMethod("getLastBuiltFrame");int before=(int)built.invoke(section);
            var pending=section.getClass().getMethod("getPendingUpdate");var disposed=section.getClass().getMethod("isDisposed");
            renderer.getMethod("scheduleRebuildForChunk",int.class,int.class,int.class,boolean.class).invoke(instance,x,y,z,true);
            return ()->(boolean)disposed.invoke(section)||((int)built.invoke(section)!=before&&pending.invoke(section)==null);
        }
        var field=ObfuscationReflectionHelper.findField(LevelRenderer.class,"f_109469_");
        var area=(ViewArea)field.get(mc.levelRenderer);if(area==null)return null;
        for(var section:area.chunks)if(section.getOrigin().equals(origin)){
            var before=section.getCompiledChunk();section.setDirty(true);
            return ()->!section.getOrigin().equals(origin)||(!section.isDirty()&&section.getCompiledChunk()!=before
                &&section.getCompiledChunk()!=ChunkRenderDispatcher.CompiledChunk.UNCOMPILED);
        }
        return null;
    }
    private ContraptionRenderHandoff(){}
    public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event){PENDING.clear();world=null;}
}
