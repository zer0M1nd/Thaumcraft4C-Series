package thaumicomniverse.compat.create.airship;

import java.util.*;
import java.util.function.Function;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.phys.Vec3;

/** Client-only terrain surface, grouped into selectable voxels; hidden blocks never enter the mesh. */
final class AirshipTerrain {
    private record Face(int x,int y,int z,int size,Direction side,int color){}
    private static final class Voxel {
        int red,green,blue,count,faces;
        void add(int color,int mask){red+=(color>>16)&255;green+=(color>>8)&255;blue+=color&255;count++;faces|=mask;}
        int color(){return (red/count<<16)|(green/count<<8)|blue/count;}
    }
    private record Draw(double[][] vertices,int color,double depth){}
    private final BlockPos center;
    private final List<ChunkPos> queue=new ArrayList<>();
    private final Map<SectionPos,List<Face>> cache=new HashMap<>();
    private int cursor,section;
    private int precision=4;
    private boolean geometryDirty=true;
    private long projectedRevision=Long.MIN_VALUE;
    private final List<Draw> draws=new ArrayList<>();
    public long lastRenderNanos;
    void precision(int value){
        if(value!=0&&value!=1&&value!=2&&value!=4&&value!=8)throw new IllegalArgumentException("terrain precision");
        if(value==precision)return;precision=value;cursor=section=0;cache.clear();draws.clear();geometryDirty=true;
    }
    AirshipTerrain(BlockPos center){this.center=center;int cx=center.getX()>>4,cz=center.getZ()>>4;
        for(int x=-16;x<=16;x++)for(int z=-16;z<=16;z++)queue.add(new ChunkPos(cx+x,cz+z));
        queue.sort(Comparator.comparingInt(c->Math.abs(c.x-cx)+Math.abs(c.z-cz)));
    }
    void tick(){
        var level=Minecraft.getInstance().level;if(level==null||precision==0)return;
        long deadline=System.nanoTime()+3_000_000;
        for(int work=0;work<96&&System.nanoTime()<deadline;work++){
            var pos=queue.get(cursor%queue.size());var chunk=level.getChunkSource().getChunk(pos.x,pos.z,ChunkStatus.FULL,false);
            if(chunk==null){if(cache.keySet().removeIf(s->s.x()==pos.x&&s.z()==pos.z))geometryDirty=true;cursor++;section=0;continue;}
            int index=section++;if(section>=chunk.getSectionsCount()){section=0;cursor++;}
            var key=SectionPos.of(pos,chunk.getSectionYFromSectionIndex(index));var blocks=chunk.getSection(index);
            if(blocks.hasOnlyAir()){if(cache.remove(key)!=null)geometryDirty=true;continue;}
            var faces=new ArrayList<Face>();var at=new BlockPos.MutableBlockPos();var adjacent=new BlockPos.MutableBlockPos();
            var voxels=new LinkedHashMap<BlockPos,Voxel>();
            for(int y=0;y<16;y++)for(int z=0;z<16;z++)for(int x=0;x<16;x++){
                var state=blocks.getBlockState(x,y,z);if(state.isAir())continue;
                int wx=pos.getMinBlockX()+x,wy=key.minBlockY()+y,wz=pos.getMinBlockZ()+z;
                if(Math.abs(wx-center.getX())>256||Math.abs(wz-center.getZ())>256)continue;
                at.set(wx,wy,wz);int mask=0;
                for(var side:Direction.values()){
                    adjacent.setWithOffset(at,side);
                    var neighbour=level.getChunkSource().getChunk(adjacent.getX()>>4,adjacent.getZ()>>4,ChunkStatus.FULL,false);
                    if(neighbour!=null){var other=neighbour.getBlockState(adjacent);
                        if(other.isSolidRender(level,adjacent)||state.skipRendering(other,side))continue;
                    }
                    mask|=1<<side.ordinal();
                }
                if(mask==0)continue;
                int color=state.getMapColor(level,at).col;if(color==0)color=0x9B91AB;
                var voxel=new BlockPos(Math.floorDiv(wx,precision)*precision,Math.floorDiv(wy,precision)*precision,Math.floorDiv(wz,precision)*precision);
                voxels.computeIfAbsent(voxel,k->new Voxel()).add(color,mask);
            }
            voxels.forEach((p,v)->{for(var side:Direction.values())if((v.faces&(1<<side.ordinal()))!=0)
                faces.add(new Face(p.getX(),p.getY(),p.getZ(),precision,side,shade(v.color(),switch(side){case UP->1;case DOWN->.45;case NORTH,SOUTH->.7;default->.85;})));});
            if(!faces.equals(cache.get(key))){cache.put(key,faces);geometryDirty=true;}
        }
    }
    private static int shade(int c,double m){return ((int)(((c>>16)&255)*m)<<16)|((int)(((c>>8)&255)*m)<<8)|(int)((c&255)*m);}
    private static Vec3[] corners(Face f){
        double x=f.x,y=f.y,z=f.z,s=f.size;
        return switch(f.side){
            case UP->new Vec3[]{new Vec3(x,y+s,z),new Vec3(x,y+s,z+s),new Vec3(x+s,y+s,z+s),new Vec3(x+s,y+s,z)};
            case DOWN->new Vec3[]{new Vec3(x,y,z),new Vec3(x+s,y,z),new Vec3(x+s,y,z+s),new Vec3(x,y,z+s)};
            case NORTH->new Vec3[]{new Vec3(x,y,z),new Vec3(x,y+s,z),new Vec3(x+s,y+s,z),new Vec3(x+s,y,z)};
            case SOUTH->new Vec3[]{new Vec3(x,y,z+s),new Vec3(x+s,y,z+s),new Vec3(x+s,y+s,z+s),new Vec3(x,y+s,z+s)};
            case WEST->new Vec3[]{new Vec3(x,y,z),new Vec3(x,y,z+s),new Vec3(x,y+s,z+s),new Vec3(x,y+s,z)};
            case EAST->new Vec3[]{new Vec3(x+s,y,z),new Vec3(x+s,y+s,z),new Vec3(x+s,y+s,z+s),new Vec3(x+s,y,z+s)};
        };
    }
    void render(GuiGraphics g,Function<Vec3,double[]> project,long revision,int left,int top,int right,int bottom){
        long start=System.nanoTime();
        if(precision==0){lastRenderNanos=0;return;}
        if(geometryDirty||projectedRevision!=revision){
            draws.clear();double originDepth=project.apply(Vec3.ZERO)[2];
            for(var faces:cache.values())for(var face:faces){
                if(project.apply(Vec3.atLowerCornerOf(face.side.getNormal()))[2]<=originDepth)continue;
                var vertices=corners(face);double[][] points=new double[4][];double depth=0;
                for(int i=0;i<4;i++){points[i]=project.apply(vertices[i]);depth+=points[i][2];}
                if(Arrays.stream(points).allMatch(p->p[0]<left)||Arrays.stream(points).allMatch(p->p[0]>right)
                    ||Arrays.stream(points).allMatch(p->p[1]<top)||Arrays.stream(points).allMatch(p->p[1]>bottom))continue;
                draws.add(new Draw(points,face.color,depth/4));
            }
            draws.sort(Comparator.comparingDouble(Draw::depth));geometryDirty=false;projectedRevision=revision;
        }
        g.flush();RenderSystem.setShader(GameRenderer::getPositionColorShader);RenderSystem.disableDepthTest();RenderSystem.disableCull();
        var buffer=Tesselator.getInstance().getBuilder();buffer.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);var matrix=g.pose().last().pose();
        for(var draw:draws)for(var p:draw.vertices)buffer.vertex(matrix,(float)p[0],(float)p[1],0).color(0xFF000000|draw.color).endVertex();
        BufferUploader.drawWithShader(buffer.end());RenderSystem.enableCull();RenderSystem.enableDepthTest();lastRenderNanos=System.nanoTime()-start;
    }
}
