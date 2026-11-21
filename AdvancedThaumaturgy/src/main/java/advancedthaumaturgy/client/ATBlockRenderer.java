package advancedthaumaturgy.client;
import advancedthaumaturgy.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import tc4revival.client.*;
import tc4revival.content.*;
public class ATBlockRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    public ATBlockRenderer(BlockEntityRendererProvider.Context context){}
    private static ResourceLocation resource(String s){return AdvancedThaumaturgy.id(s);}
    @Override public void render(T entity,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){draw(entity.getBlockState().getBlock(),entity,pose,buffers,light,overlay);}
    private static void draw(Block block,BlockEntity entity,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(block==AdvancedThaumaturgy.WANDBENCH.get()){
            ArcaneWorkbenchRenderer.itemRenderer().renderByItem(ItemStack.EMPTY,ItemDisplayContext.NONE,pose,type->buffers.getBuffer(RenderType.entityCutoutNoCull(resource("textures/blocks/wandbench.png"))),light,overlay);return;
        }
        if(block==AdvancedThaumaturgy.CREATIVE_NODE.get()){
            var values=new tc4revival.aspect.AspectAmounts();tc4revival.aspect.TCAspects.catalog().primals().forEach(a->values.add(a,100));
            pose.pushPose();pose.translate(.5,.5,.5);NodeJarRenderer.itemNode(new NodeJarData(values,AuraNodeType.PURE,AuraNodeModifier.BRIGHT,""),pose,buffers);pose.popPose();return;
        }
        if(block==AdvancedThaumaturgy.ETHEREAL_JAR.get()){
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(block.defaultBlockState(),pose,buffers,light,overlay);return;
        }
        String model,texture;boolean micro=false;MachineBlock.Kind kind=null;
        if(block instanceof MachineBlock machine){kind=machine.kind;micro=kind.microlith();model=micro?"minilith":kind==MachineBlock.Kind.ENGINE?"thaum_engine":"thaum_sprinkler";texture=kind==MachineBlock.Kind.FERTILIZER?"advancedthaumaturgy:textures/models/thaum_sprinkler_tex.png":"minecraft:textures/block/obsidian.png";}
        else if(block==AdvancedThaumaturgy.NODE_MODIFIER.get()){model="thaum_node_modifier_2";texture="advancedthaumaturgy:textures/models/thaum_node_modifier_tex.png";}
        else return;
        var obj=OriginalObj.get(resource("models/"+model+".obj"));pose.pushPose();pose.translate(.5,0,.5);pose.scale(.5f,.5f,.5f);
        obj.draw(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.parse(texture))),light,overlay,0xffffff,g->!g.equals("Sphere"));
        if(micro&&kind!=MachineBlock.Kind.BASE&&kind!=MachineBlock.Kind.EXCAVATOR){
            int color=switch(kind){case FLUX->0x800080;case WATCHFUL->0x2255ff;case HEALING->0xff2f34;case FIRE->0xff5a01;case LIGHTNING->0xffff7e;case FROST->0xe1ffff;default->0xffffff;};
            if(entity instanceof MachineEntity m&&!m.active()&&kind!=MachineBlock.Kind.FLUX)color=(color>>1)&0x7f7f7f;
            pose.translate(0,Math.sin((Minecraft.getInstance().level==null?0:Minecraft.getInstance().level.getGameTime())/16d)*.05,0);
            obj.draw(pose,buffers.getBuffer(RenderType.entityTranslucent(ResourceLocation.parse("minecraft:textures/block/white_concrete.png"))),15728880,overlay,color,g->g.equals("Sphere"));
        }
        pose.popPose();
    }
    public static BlockEntityWithoutLevelRenderer items(){return Holder.RENDERER;}
    private static class Holder{static final BlockEntityWithoutLevelRenderer RENDERER=new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()){
        @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){if(stack.getItem() instanceof BlockItem item){
            draw(item.getBlock(),null,pose,buffers,light,overlay);
            if(item.getBlock()==AdvancedThaumaturgy.ETHEREAL_JAR.get()){
                var jar=new EtherealJarEntity(BlockPos.ZERO,item.getBlock().defaultBlockState());jar.readItem(stack);
                JarRenderer.contents(jar,pose,buffers,light);
            }
        }}
    };}
    public static class JarRenderer extends EssentiaJarRenderer {
        public JarRenderer(BlockEntityRendererProvider.Context context){super(context);}
        @Override public void render(EssentiaJarBlockEntity jar,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            contents(jar,pose,buffers,light);
        }
        static void contents(EssentiaJarBlockEntity jar,PoseStack pose,MultiBufferSource buffers,int light){
            // Keep the host label rendering and draw the 256-unit liquid at exact height.
            var view=new EssentiaJarBlockEntity(BlockPos.ZERO,jar.getBlockState()){
                @Override public int amount(){return 0;}
                @Override public tc4revival.aspect.Aspect aspect(){return jar.aspect();}
                @Override public tc4revival.aspect.Aspect filter(){return jar.filter();}
                @Override public net.minecraft.core.Direction facing(){return jar.facing();}
            };new EssentiaJarRenderer(null).render(view,0,pose,buffers,light,0);
            if(jar.aspect()==null||jar.amount()<=0)return;
            var sprite=Minecraft.getInstance().getTextureAtlas(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS).apply(ResourceLocation.parse("thaumcraft:block/animatedglow"));
            var out=TCRenderTypes.buffer(buffers,TCRenderTypes.ESSENTIA_LIQUID);
            int color=thaumcraft.api.client.ClientAspectViews.view(jar.aspect()).color();
            float low=.0725f,high=low+Math.min(256,jar.amount())/256f*.625f;
            float[][] vertices={{.25f,low,.25f},{.75f,low,.25f},{.75f,low,.75f},{.25f,low,.75f},{.25f,high,.25f},{.75f,high,.25f},{.75f,high,.75f},{.25f,high,.75f}};
            int[][] faces={{0,1,2,3},{7,6,5,4},{1,0,4,5},{3,2,6,7},{0,3,7,4},{2,1,5,6}};
            for(int face=0;face<faces.length;face++)for(int corner:faces[face]){
                var p=vertices[corner];float u=face<4?p[0]:p[2],v=face<2?p[2]:1-(p[1]-.01f);
                out.vertex(pose.last().pose(),p[0],p[1],p[2]).color(color>>16&255,color>>8&255,color&255,255).uv(sprite.getU(u*16),sprite.getV(v*16)).uv2(Math.max(200,light)).endVertex();
            }
        }
    }
}
