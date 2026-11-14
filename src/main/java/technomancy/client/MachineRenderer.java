package technomancy.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import technomancy.*;

/** Author model transforms, separate translucent contents, and per-machine animations. */
public final class MachineRenderer implements BlockEntityRenderer<MachineEntity> {
    private static BlockEntityWithoutLevelRenderer itemRenderer;
    public MachineRenderer(BlockEntityRendererProvider.Context context){}
    public static String geometry(MachineKind kind){
        if(kind.crystal())return "crystal";
        if(kind.catalyst())return "catalyst";
        if(ExistenceLogic.pylon(kind))return "existencepylon";
        return switch(kind){
            case QUANTUM_JAR,CREATIVE_JAR->"essentiacontainer";
            case ESSENTIA_DYNAMO->"essentiadynamo";case NODE_DYNAMO->"nodedynamo";
            case ECOLOGIC_TRANSMUTER->"biomemorpher";case NODE_FABRICATOR->"nodegenerator";
            case ORDERLY_LAMP->"fluxlamp";case ESSENTIA_COIL,ITEM_COIL->"teslacoil";
            case GALVANIZED_BELLOWS->"electricbellows";case ELDRITCH_CONSUMER->"eldritchconsumer";
            case ADVANCED_DECONSTRUCTION_TABLE->"advdecontable";case ESSENTIA_FUSOR->"essentiafusor";
            case BLOOD_DYNAMO->"blooddynamo";case BLOOD_FABRICATOR->"bloodfabricator";
            case HIPPIE_DYNAMO->"flowerdynamo";case MANA_FABRICATOR->"manafabricator";
            case EXISTENCE_FOUNTAIN->"existencefountain";case EXISTENCE_BURNER,DYNAMIC_EXISTENCE_BURNER->"existenceburner";
            default->null;
        };
    }
    private static String texture(String model,MachineKind k){return switch(model){
        case "advdecontable"->"reconstructor";case "teslacoil"->k==MachineKind.ITEM_COIL?"itemcoil":"essentiacoil";
        case "eldritchconsumer"->"eldcons";case "essentiafusor"->"fusor";case "crystal"->"blockcrystal";
        default->model;
    };}
    private static ResourceLocation texture(String name){return Technomancy.id("textures/models/"+name+".png");}
    private static void draw(ModelPart model,String texture,PoseStack pose,MultiBufferSource buffers,int light,int overlay,int color,float alpha){
        model.render(pose,buffers.getBuffer(RenderType.entityTranslucent(texture(texture))),light,overlay,(color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,alpha);
    }
    private static void draw(ModelPart model,String texture,PoseStack pose,MultiBufferSource buffers,int light,int overlay){draw(model,texture,pose,buffers,light,overlay,0xffffff,1);}
    private static void standard(PoseStack pose){pose.translate(.5,1.5,.5);pose.scale(-1,-1,1);}
    private static float yaw(Direction d){return switch(d){case SOUTH->180;case EAST->90;case WEST->270;default->0;};}
    @Override public void render(MachineEntity m,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){renderMachine(m,partial,pose,buffers,light,overlay);}
    private static void renderMachine(MachineEntity m,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(m.kind.catalyst()&&m.state.getString("ritual").startsWith("black_hole")){MachineEffects.sphere(m,pose,buffers,light);return;}
        String name=geometry(m.kind);if(name==null)return;
        ModelPart root=LegacyGeometry.get(name);root.getAllParts().forEach(ModelPart::resetPose);
        float time=m.getLevel()==null?0:m.getLevel().getGameTime()+partial;
        String tex=texture(name,m.kind);
        pose.pushPose();
        if(m.kind==MachineKind.GALVANIZED_BELLOWS){bellows(m,root,time,pose,buffers,light,overlay);}
        else if(m.kind==MachineKind.EXISTENCE_FOUNTAIN){
            pose.scale(-1,-1,1);pose.translate(.5,-.5,-.5);
            for(int i=0;i<31;i++)draw(root.getChild("fountain_"+i),"fountain",pose,buffers,light,overlay);
            int color=0x003300|Math.round(255f*ExistenceLogic.power(m)/ExistenceLogic.capacity(m.kind));
            for(int i=0;i<9;i++)draw(root.getChild("liquid_"+i),"existence",pose,buffers,light,overlay,color,.5f);
        }else if(ExistenceLogic.pylon(m.kind)){
            pose.translate(.5,0,.5);
            for(int i=0;i<4;i++)draw(root.getChild("pylon_"+i),"fountain",pose,buffers,light,overlay);
            int color=switch(m.kind){case EXISTENCE_PYLON_1->0x0000b3;case EXISTENCE_PYLON_2->0x008080;default->0x00b300;};
            draw(root.getChild("cube"),"white",pose,buffers,LightTexture.FULL_BRIGHT,overlay,color,1);
        }else if(m.kind==MachineKind.EXISTENCE_BURNER||m.kind==MachineKind.DYNAMIC_EXISTENCE_BURNER){
            pose.translate(.5,0,.5);
            for(int i=0;i<2;i++)draw(root.getChild("core_"+i),"anvil",pose,buffers,light,overlay);
            draw(root.getChild("burner"),m.kind==MachineKind.DYNAMIC_EXISTENCE_BURNER?"bottomrf":"anvil",pose,buffers,light,overlay);
            draw(root.getChild("cube"),"white",pose,buffers,LightTexture.FULL_BRIGHT,overlay,m.kind==MachineKind.DYNAMIC_EXISTENCE_BURNER?0x80b300:0x00b300,1);
        }else if(m.kind==MachineKind.ESSENTIA_FUSOR){draw(root,tex,pose,buffers,light,overlay);}
        else if(m.kind==MachineKind.ELDRITCH_CONSUMER){
            pose.scale(-1,-1,1);
            root.getChild("rotator").yRot=m.active?time*.15f:0;
            for(int side=0;side<4;side++)for(int i=0;i<5;i++)root.getChild("pieces_"+side+"_"+i).xRot=m.active?-.7f:0;
            draw(root,tex,pose,buffers,light,overlay);
        }else if(m.kind==MachineKind.HIPPIE_DYNAMO){
            pose.scale(-1,-1,1);pose.translate(-.5,.5,.5);dynamoFacing(pose,m.facing(),true);draw(root,tex,pose,buffers,light,overlay);
        }else{
            standard(pose);
            if(m.kind==MachineKind.ESSENTIA_DYNAMO||m.kind==MachineKind.BLOOD_DYNAMO){pose.mulPose(Axis.XP.rotationDegrees(180));dynamoFacing(pose,m.facing(),false);}
            if(m.kind==MachineKind.NODE_FABRICATOR){
                pose.mulPose(Axis.YP.rotationDegrees(-90+yaw(m.facing())));
                var core=root.getChild("CenterPiece1");core.visible=false;draw(root,tex,pose,buffers,light,overlay);core.visible=true;
                pose.mulPose(Axis.XP.rotationDegrees(m.active?time*(1+m.progress/5f):0));draw(core,tex,pose,buffers,light,overlay);
            }else if(m.kind.crystal()){
                int stage=crystalStage(m);draw(root.getChild(stage==0?"crystalBig":stage==1?"crystalMed":"crystalSma"),tex,pose,buffers,light,overlay,ritualColor(m.kind),.6f);
            }else if(m.kind.catalyst())draw(root,tex,pose,buffers,light,overlay,ritualColor(m.kind),1);
            else draw(root,tex,pose,buffers,light,overlay,m.kind==MachineKind.CREATIVE_JAR?0xff00ff:0xffffff,1);
        }
        pose.popPose();
        if(m.kind==MachineKind.NODE_FABRICATOR)MachineEffects.arcs(m,pose,buffers);
        if((m.kind==MachineKind.QUANTUM_JAR||m.kind==MachineKind.CREATIVE_JAR)&&m.aspect()!=null&&m.amount()>0)
            liquid(pose,buffers,light,.25f,.0625f,.75f,.0625f+.65f*m.amount()/m.capacity(),thaumcraft.api.client.ClientAspectViews.view(m.aspect()).color(),new ResourceLocation("thaumcraft","block/animatedglow"));
        if(m.kind==MachineKind.BLOOD_FABRICATOR&&m.fluid.getFluidAmount()>0){
            var extension=net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(m.fluid.getFluid().getFluid());
            liquid(pose,buffers,light,.26f,.24f,.76f,.24f+.55f*m.fluid.getFluidAmount()/m.fluid.getCapacity(),extension.getTintColor(m.fluid.getFluid()),extension.getStillTexture(m.fluid.getFluid()));
        }
        if(m.filter!=null&&(m.kind==MachineKind.QUANTUM_JAR||m.kind==MachineKind.CREATIVE_JAR))label(m,pose,buffers,light);
        if(m.kind==MachineKind.ADVANCED_DECONSTRUCTION_TABLE&&!m.inventory.getStackInSlot(0).isEmpty()){
            pose.pushPose();pose.translate(.5,1.1,.5);pose.mulPose(Axis.YP.rotationDegrees(time%360));pose.scale(.7f,.7f,.7f);
            Minecraft.getInstance().getItemRenderer().renderStatic(m.inventory.getStackInSlot(0),ItemDisplayContext.GROUND,light,overlay,pose,buffers,m.getLevel(),0);pose.popPose();
        }
        if(m.kind==MachineKind.ESSENTIA_FUSOR){
            for(var d:Direction.Plane.HORIZONTAL){var a=MachineLogic.sideAspect(m,d);if(a==null)continue;
                pose.pushPose();pose.translate(.5+d.getStepX()*.37,.77,.5+d.getStepZ()*.37);pose.mulPose(Axis.XP.rotationDegrees(90));
                aspect(pose,buffers,light,a,.12f);pose.popPose();
            }
            var out=MachineLogic.outputSide(m);var a=out==null?null:MachineLogic.sideAspect(m,out);if(a!=null){pose.pushPose();pose.translate(.5,.77,.5);pose.mulPose(Axis.XP.rotationDegrees(90));pose.mulPose(Axis.ZP.rotationDegrees(time%360));aspect(pose,buffers,light,a,.17f);pose.popPose();}
        }
    }
    private static int crystalStage(MachineEntity m){
        if(m.getLevel()==null)return 0;int stage=0;
        for(int i=1;i<=2;i++){if(m.getLevel().getBlockState(m.getBlockPos().below(i)).getBlock() instanceof MachineBlock b&&b.kind.crystal())stage++;else break;}return stage;
    }
    private static int ritualColor(MachineKind k){String id=k.id();return id.startsWith("earth")?0x00dd00:id.startsWith("fire")?0xdd0000:id.startsWith("water")?0x0000dd:id.startsWith("light")?0x111111:0xdddddd;}
    private static void dynamoFacing(PoseStack p,Direction d,boolean flower){
        switch(d){
            case DOWN->{if(flower)p.mulPose(Axis.XP.rotationDegrees(180));else{p.translate(0,-2,0);p.mulPose(Axis.YP.rotationDegrees(180));}}
            case UP->{if(flower){p.translate(0,-2,0);p.mulPose(Axis.YP.rotationDegrees(180));}else p.mulPose(Axis.XP.rotationDegrees(180));}
            case NORTH,SOUTH->{boolean flipped=(d==Direction.NORTH)!=flower;p.translate(0,-1,flipped?1:-1);if(flipped)p.mulPose(Axis.YP.rotationDegrees(180));p.mulPose(Axis.XP.rotationDegrees(90));}
            case WEST,EAST->{p.translate(d==Direction.WEST?1:-1,-1,0);p.mulPose(Axis.YP.rotationDegrees(d==Direction.WEST?270:90));p.mulPose(Axis.XP.rotationDegrees(90));}
        }
    }
    private static void bellows(MachineEntity m,ModelPart root,float time,PoseStack p,MultiBufferSource b,int light,int overlay){
        float inflation=m.active?(float)Math.sin(time/8)*.3f+.7f:1,scale=.125f+inflation*.875f;
        p.translate(.5,-.5,.5);p.mulPose(Axis.YP.rotationDegrees(switch(m.facing()){case NORTH->180;case WEST->270;case EAST->90;default->0;}));
        p.pushPose();p.translate(0,1,0);p.scale(.5f,(inflation+.1f)/2,.5f);root.getChild("Bag").setPos(0,.5f,0);draw(root.getChild("Bag"),"electricbellows",p,b,light,overlay);p.popPose();
        p.pushPose();p.translate(0,-scale/2+.5,0);draw(root.getChild("TopPlank"),"electricbellows",p,b,light,overlay);p.popPose();
        p.pushPose();p.translate(0,scale/2-.5,0);draw(root.getChild("BottomPlank"),"electricbellows",p,b,light,overlay);p.popPose();
        draw(root.getChild("MiddlePlank"),"electricbellows",p,b,light,overlay);draw(root.getChild("Nozzle"),"electricbellows",p,b,light,overlay);
    }
    private static void label(MachineEntity m,PoseStack pose,MultiBufferSource buffers,int light){
        pose.pushPose();pose.translate(.5,.4,.5);pose.mulPose(Axis.YP.rotationDegrees(-yaw(m.facing())));pose.translate(0,0,-.316);
        quad(pose,buffers,new ResourceLocation("thaumcraft","textures/models/label.png"),.25f,0xffffff,light);
        pose.translate(0,0,-.001);aspect(pose,buffers,light,m.filter,.168f);pose.popPose();
    }
    private static void aspect(PoseStack p,MultiBufferSource b,int l,tc4revival.aspect.Aspect a,float size){var view=thaumcraft.api.client.ClientAspectViews.view(a);quad(p,b,view.texture(),size,view.color(),l);}
    private static void quad(PoseStack p,MultiBufferSource b,ResourceLocation texture,float size,int color,int light){
        var out=b.getBuffer(RenderType.entityTranslucent(texture));
        vertex(out,p,-size,-size,0,0,1,color,light,0,0,-1);vertex(out,p,size,-size,0,1,1,color,light,0,0,-1);
        vertex(out,p,size,size,0,1,0,color,light,0,0,-1);vertex(out,p,-size,size,0,0,0,color,light,0,0,-1);
    }
    private static void liquid(PoseStack p,MultiBufferSource b,int light,float min,float low,float max,float high,int color,ResourceLocation texture){
        var sprite=Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
        var out=b.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
        float[][] points={{min,low,min},{max,low,min},{max,low,max},{min,low,max},{min,high,min},{max,high,min},{max,high,max},{min,high,max}};
        int[][] faces={{0,1,2,3},{7,6,5,4},{1,0,4,5},{3,2,6,7},{0,3,7,4},{2,1,5,6}};
        int[][] normals={{0,-1,0},{0,1,0},{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}};
        for(int f=0;f<6;f++)for(int i=0;i<4;i++){var v=points[faces[f][i]];var n=normals[f];vertex(out,p,v[0],v[1],v[2],i==0||i==3?sprite.getU0():sprite.getU1(),i<2?sprite.getV1():sprite.getV0(),color,light,n[0],n[1],n[2]);}
    }
    private static void vertex(VertexConsumer out,PoseStack p,float x,float y,float z,float u,float v,int color,int light,int nx,int ny,int nz){
        out.vertex(p.last().pose(),x,y,z).color(color>>16&255,color>>8&255,color&255,255).uv(u,v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.last().normal(),nx,ny,nz).endVertex();
    }
    public static BlockEntityWithoutLevelRenderer itemRenderer(){
        if(itemRenderer==null)itemRenderer=new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()){
            @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
                if(!(stack.getItem() instanceof BlockItem item)||!(item.getBlock() instanceof MachineBlock block))return;
                var m=new MachineEntity(BlockPos.ZERO,block.defaultBlockState());var tag=BlockItem.getBlockEntityData(stack);if(tag!=null)m.load(tag);
                pose.pushPose();if(block.kind==MachineKind.NODE_FABRICATOR){pose.translate(.34,.04,.34);pose.scale(.32f,.32f,.32f);}
                renderMachine(m,0,pose,buffers,light,overlay);pose.popPose();
            }
        };return itemRenderer;
    }
}
