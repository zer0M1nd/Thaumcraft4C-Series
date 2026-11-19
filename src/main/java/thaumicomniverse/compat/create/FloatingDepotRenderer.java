package thaumicomniverse.compat.create;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.logistics.depot.*;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class FloatingDepotRenderer extends SafeBlockEntityRenderer<FloatingDepot.Tile> {
    public FloatingDepotRenderer(BlockEntityRendererProvider.Context context){}
    @Override protected void renderSafe(FloatingDepot.Tile tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        boolean flying=!tile.output()&&(tile.phase()==FloatingDepot.Tile.OUTBOUND||tile.phase()==FloatingDepot.Tile.RETURNING);
        boolean plate=flying||tile.output()&&tile.phase()==FloatingDepot.Tile.WAITING
                ||!tile.output()&&tile.phase()==FloatingDepot.Tile.HOME;
        var offset=flying?tile.plateOffset(partial):Vec3.ZERO;
        if(plate){
            pose.pushPose();pose.translate(offset.x,offset.y,offset.z);
            int atLight=flying?LevelRenderer.getLightColor(tile.getLevel(),BlockPos.containing(Vec3.atLowerCornerOf(tile.getBlockPos()).add(offset))):light;
            CachedBuffers.partial(ArcaneMachineRenderers.DEPOT_PLATE,tile.getBlockState()).light(atLight).renderInto(pose,buffers.getBuffer(RenderType.cutoutMipped()));
            if(flying&&!tile.cargo().isEmpty()){
                pose.translate(.5,.9375,.5);
                DepotRenderer.renderItem(tile.getLevel(),pose,buffers,atLight,overlay,tile.cargo(),0,new java.util.Random(0),
                        Vec3.atCenterOf(tile.getBlockPos()).add(offset),false);
            }
            pose.popPose();
        }
        DepotRenderer.renderItemsOf(tile,partial,pose,buffers,light,overlay,tile.getBehaviour(DepotBehaviour.TYPE));
    }
    @Override public int getViewDistance(){return 256;}
    public static void selection(net.minecraftforge.event.TickEvent.ClientTickEvent event){
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;
        var player=net.minecraft.client.Minecraft.getInstance().player;
        if(player==null||!(player.getMainHandItem().getItem() instanceof FloatingDepot.PlacementItem))return;
        var tag=player.getMainHandItem().getTag();
        if(tag==null||!tag.contains("InputDepot")||!tag.getString("InputDimension").equals(player.level().dimension().location().toString()))return;
        var pos=BlockPos.of(tag.getLong("InputDepot"));
        net.createmod.catnip.outliner.Outliner.getInstance().showAABB("thaumicomniverse:depot_input",new net.minecraft.world.phys.AABB(pos))
                .colored(0xa065dd).lineWidth(.0625f);
    }
}
