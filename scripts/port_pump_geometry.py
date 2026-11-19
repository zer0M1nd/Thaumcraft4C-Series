"""Translate the inspected author renderer's numeric mesh to modern vertex quads.
No reference build scripts are executed and no texture is copied into the mod.
"""
from pathlib import Path
import re
root=Path(__file__).resolve().parents[1]
old=(root.parent/'references/phase2/WitchingGadgets/original-source/src/main/java/witchinggadgets/client/render/TileRenderEssentiaPump.java').read_text()
sections=[]
texture=0
for fragment in re.split(r'tes\.startDrawing(?:Quads\(\)|\(4\));',old)[1:]:
    part=fragment.split('tes.draw();')[0]
    normal=re.search(r'tes.setNormal\(([^)]+)\)',part).group(1)
    points=re.findall(r'tes.addVertexWithUV\(([^)]+)\)',part)
    values=[]
    for point in points:
        # The verified source contains only numeric literals and simple division.
        entries=point.split(',')
        assert all(re.fullmatch(r'[\d\s./f-]+',e) for e in entries)
        values.append(','.join(str(float(eval(e.strip().removesuffix('f'),{'__builtins__':{}})))+'f' for e in entries))
    if len(values)==3:values.append(values[-1])
    assert len(values)==4
    sections.append('draw(p,b.getBuffer(RenderType.entityCutoutNoCull('+('ALCHEMY' if texture else 'METAL')+')),light,overlay,new float[]{'+normal+'},new float[][]{'+','.join('{'+v+'}' for v in values)+'});')
    if 'textures/blocks/alchemyblock.png' in fragment:texture=1
out='''package wgmodern;
import net.minecraft.client.renderer.*;import net.minecraft.client.renderer.blockentity.*;import net.minecraft.core.Direction;import net.minecraft.resources.ResourceLocation;import net.minecraft.world.item.*;import com.mojang.blaze3d.vertex.*;import com.mojang.math.Axis;
public class PumpRenderer implements BlockEntityRenderer<MirrorPump.Tile>{
 private static final ResourceLocation METAL=new ResourceLocation("thaumcraft","textures/models/alembic.png"),ALCHEMY=new ResourceLocation("thaumcraft","textures/blocks/alchemyblock.png");
 public PumpRenderer(BlockEntityRendererProvider.Context c){}
 @Override public void render(MirrorPump.Tile tile,float partial,PoseStack p,MultiBufferSource b,int light,int overlay){render(tile.facing(),p,b,light,overlay);}
 public static void render(Direction face,PoseStack p,MultiBufferSource b,int light,int overlay){p.pushPose();p.translate(.5,0,.5);p.mulPose(Axis.YP.rotationDegrees(switch(face){case SOUTH->180;case EAST->270;case WEST->90;default->0;}));p.translate(-.5,0,-.5);
'''+ '\n'.join(sections)+'''
 p.scale(.375f,.375f,.375f);p.translate(.625,-.0625,-.125);p.mulPose(Axis.ZP.rotationDegrees(45));p.mulPose(Axis.YP.rotationDegrees(-90));p.translate(0,0,-1.0625);bellows(p,b,light,overlay);p.translate(0,0,1.0625);p.mulPose(Axis.XP.rotationDegrees(-90));p.translate(0,.9375,.875);bellows(p,b,light,overlay);p.translate(0,-.9375,-.875);p.mulPose(Axis.XP.rotationDegrees(-90));p.translate(0,-1,1.8125);bellows(p,b,light,overlay);p.translate(0,1,-1.8125);p.mulPose(Axis.XP.rotationDegrees(-90));p.translate(0,-1.9375,-.125);bellows(p,b,light,overlay);p.popPose();
 }
 private static void bellows(PoseStack p,MultiBufferSource b,int l,int o){tc4revival.client.BellowsRenderer.itemRenderer().renderByItem(ItemStack.EMPTY,ItemDisplayContext.NONE,p,b,l,o);}
 private static void draw(PoseStack p,VertexConsumer b,int l,int o,float[] normal,float[][] vertices){var pose=p.last();var n=new org.joml.Vector3f(normal[0],normal[1],normal[2]).normalize();for(var v:vertices)b.vertex(pose.pose(),v[0],v[1],v[2]).color(255,255,255,255).uv(v[3],v[4]).overlayCoords(o).uv2(l).normal(pose.normal(),n.x,n.y,n.z).endVertex();}
}
'''
(root/'src/main/java/wgmodern/PumpRenderer.java').write_text(out)
print('Converted',len(sections),'inspected mesh polygons')
