package gadomancymodern.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import gadomancymodern.knowledge.KnowledgeBookEntity;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
public class KnowledgeBookRenderer implements BlockEntityRenderer<KnowledgeBookEntity> {
    private final BookModel model=new BookModel(BookModel.createBodyLayer().bakeRoot());
    private static final ResourceLocation TEXTURE=new ResourceLocation("minecraft","textures/entity/enchanting_table_book.png");
    public KnowledgeBookRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(KnowledgeBookEntity book,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){draw(book.ticks+partial,book.previousRotation+KnowledgeBookEntity.wrap(book.rotation-book.previousRotation)*partial,Mth.lerp(partial,book.previousPage,book.page),pose,buffers,light,overlay);}
    public void draw(float time,float rotation,float page,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();pose.translate(.5,.2+Math.sin(time*.1)*.03,.5);pose.mulPose(Axis.YP.rotation(-rotation));pose.mulPose(Axis.ZP.rotationDegrees(80));
        float left=Mth.clamp(Mth.frac(page+.25f)*1.6f-.3f,0,1),right=Mth.clamp(Mth.frac(page+.75f)*1.6f-.3f,0,1);
        // The original renderer deliberately fixes openness at .9, even without a nearby player.
        model.setupAnim(45,left,right,.9f);model.render(pose,buffers.getBuffer(RenderType.entitySolid(TEXTURE)),light,overlay,1,1,1,1);pose.popPose();
    }
    public static void effects(KnowledgeBookEntity book){
        var world=book.getLevel();var pos=book.getBlockPos();var random=world.random;
        if(book.ticks%16==0){int[] colors={11929351,15611778,15956768,16738082,16739632,0xff0000};for(int i=0;i<20;i++){int color=colors[random.nextInt(colors.length)];tc4revival.client.EldritchWisps.rune(world,pos.getX()+offset(random),pos.getY()-1+offset(random),pos.getZ()+offset(random),((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f,30+random.nextInt(10),-.01f-(random.nextBoolean()?random.nextBoolean()?.02f:.01f:0));}}
        if(book.ticks%32==0){int choice=random.nextInt(5);if(choice==0&&!book.turnedPages){world.playLocalSound(pos.getX()+.5,pos.getY()+.3,pos.getZ()+.5,tc4revival.content.TCSounds.PAGE.get(),net.minecraft.sounds.SoundSource.BLOCKS,.4f,1,false);book.turnedPages=true;}else if(choice<4){world.playLocalSound(pos.getX()+.5,pos.getY()+.3,pos.getZ()+.5,tc4revival.content.TCSounds.WRITE.get(),net.minecraft.sounds.SoundSource.BLOCKS,.2f,1,false);book.turnedPages=false;}}
    }
    private static float offset(net.minecraft.util.RandomSource random){return random.nextFloat()*(random.nextBoolean()?1:-1)/2;}
}
