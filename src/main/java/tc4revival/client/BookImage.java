package tc4revival.client;

import java.awt.AlphaComposite;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.imageio.metadata.IIOMetadataNode;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/** Resource-pack GIFs, composited according to disposal metadata, played once under user control. */
public final class BookImage implements AutoCloseable {
    public record Frame(BufferedImage image,int millis) {}
    private final List<ResourceLocation> textures=new ArrayList<>();
    private final List<Integer> delays=new ArrayList<>();
    private int width,height,index;
    private long started,elapsed;
    private boolean playing;
    public BookImage(ResourceLocation resource) throws IOException {
        var mc=Minecraft.getInstance();
        try(var input=mc.getResourceManager().open(resource)) {
            for(var frame:decode(input)) {
                width=frame.image().getWidth();height=frame.image().getHeight();
                var image=new NativeImage(width,height,false);
                for(int y=0;y<height;y++)for(int x=0;x<width;x++) {
                    int argb=frame.image().getRGB(x,y);
                    image.setPixelRGBA(x,y,(argb&0xff00ff00)|((argb>>16)&255)|((argb&255)<<16));
                }
                textures.add(mc.getTextureManager().register("book_gif",new DynamicTexture(image)));
                delays.add(frame.millis());
            }
        }catch(IOException|RuntimeException e){close();throw e;}
    }
    public static List<Frame> decode(InputStream source) throws IOException {
        byte[] bytes=source.readNBytes(16*1024*1024+1);
        if(bytes.length>16*1024*1024)throw new IOException("Book GIF exceeds 16 MiB");
        try(var stream=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers=ImageIO.getImageReadersByFormatName("gif");
            var reader=readers.next();
            try {
                reader.setInput(stream);int count=reader.getNumImages(true);
                if(count<1||count>256)throw new IOException("Book GIF frame limit exceeded");
                var metadata=(IIOMetadataNode)reader.getStreamMetadata().getAsTree("javax_imageio_gif_stream_1.0");
                var logical=(IIOMetadataNode)metadata.getElementsByTagName("LogicalScreenDescriptor").item(0);
                var palette=(IIOMetadataNode)metadata.getElementsByTagName("GlobalColorTable").item(0);
                int background=0,backgroundIndex=-1;
                if(palette!=null) {
                    backgroundIndex=Integer.parseInt(palette.getAttribute("backgroundColorIndex"));
                    var colors=palette.getElementsByTagName("ColorTableEntry");
                    for(int c=0;c<colors.getLength();c++) {
                        var color=(IIOMetadataNode)colors.item(c);
                        if(Integer.parseInt(color.getAttribute("index"))==backgroundIndex)background=0xff000000
                                |Integer.parseInt(color.getAttribute("red"))<<16|Integer.parseInt(color.getAttribute("green"))<<8
                                |Integer.parseInt(color.getAttribute("blue"));
                    }
                }
                int w=Integer.parseInt(logical.getAttribute("logicalScreenWidth")),h=Integer.parseInt(logical.getAttribute("logicalScreenHeight"));
                if(w<1||h<1||(long)w*h*count>16_777_216)throw new IOException("Book GIF decoded pixel limit exceeded");
                var canvas=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);var result=new ArrayList<Frame>();
                for(int i=0;i<count;i++) {
                    var tree=(IIOMetadataNode)reader.getImageMetadata(i).getAsTree("javax_imageio_gif_image_1.0");
                    var descriptor=(IIOMetadataNode)tree.getElementsByTagName("ImageDescriptor").item(0);
                    var control=(IIOMetadataNode)tree.getElementsByTagName("GraphicControlExtension").item(0);
                    boolean transparent=control!=null&&control.getAttribute("transparentColorFlag").equalsIgnoreCase("TRUE");
                    int clear=transparent&&Integer.parseInt(control.getAttribute("transparentColorIndex"))==backgroundIndex?0:background;
                    if(i==0)fill(canvas,0,0,w,h,clear);
                    int x=Integer.parseInt(descriptor.getAttribute("imageLeftPosition")),y=Integer.parseInt(descriptor.getAttribute("imageTopPosition"));
                    var previous=copy(canvas);var frame=reader.read(i);var graphics=canvas.createGraphics();
                    graphics.drawImage(frame,x,y,null);graphics.dispose();
                    result.add(new Frame(copy(canvas),control==null?100:Math.max(20,Integer.parseInt(control.getAttribute("delayTime"))*10)));
                    String disposal=control==null?"none":control.getAttribute("disposalMethod");
                    if(disposal.equals("restoreToPrevious"))canvas=previous;
                    else if(disposal.equals("restoreToBackgroundColor")) {
                        fill(canvas,x,y,frame.getWidth(),frame.getHeight(),clear);
                    }
                }
                return List.copyOf(result);
            }finally{reader.dispose();}
        }
    }
    private static void fill(BufferedImage canvas,int x,int y,int w,int h,int color) {
        var graphics=canvas.createGraphics();graphics.setComposite(AlphaComposite.Src);
        graphics.setColor(new java.awt.Color(color,true));graphics.fillRect(x,y,w,h);graphics.dispose();
    }
    private static BufferedImage copy(BufferedImage source) {
        var result=new BufferedImage(source.getWidth(),source.getHeight(),BufferedImage.TYPE_INT_ARGB);
        var graphics=result.createGraphics();graphics.drawImage(source,0,0,null);graphics.dispose();return result;
    }
    public boolean playing(){return playing;}
    public int width(){return width;}
    public int height(){return height;}
    public int frame(){update();return index;}
    public void toggle(){update();if(playing){elapsed+=net.minecraft.Util.getMillis()-started;playing=false;}
        else if(elapsed<delays.stream().mapToInt(Integer::intValue).sum()){started=net.minecraft.Util.getMillis();playing=true;}}
    public void replay(){index=0;elapsed=0;started=net.minecraft.Util.getMillis();playing=true;}
    private void update(){if(!playing)return;long time=elapsed+net.minecraft.Util.getMillis()-started;index=0;
        while(index<delays.size()-1&&time>=delays.get(index)){time-=delays.get(index++);}
        if(index==delays.size()-1&&time>=delays.get(index)){playing=false;elapsed=delays.stream().mapToInt(Integer::intValue).sum();}}
    public void draw(GuiGraphics graphics,int x,int y,int w,int h){update();graphics.blit(textures.get(index),x,y,w,h,0,0,width,height,width,height);}
    @Override public void close(){var manager=Minecraft.getInstance().getTextureManager();textures.forEach(manager::release);textures.clear();}
}
