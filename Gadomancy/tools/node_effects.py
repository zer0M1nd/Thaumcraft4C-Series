from pathlib import Path
p=Path(__file__).resolve().parents[1]/'src/main/java/gadomancymodern'
f=p/'client/NodeVisualEffects.java'
s=f.read_text().replace('protected static Object world;','protected static Object world;\n    protected static final java.util.List<Burst> BURSTS=new java.util.ArrayList<>();\n    public static class Burst{public final Vec3 at;public final float scale;public int age;public Burst(Vec3 at,float scale){this.at=at;this.scale=scale;}}')
s=s.replace('ABSORB.clear();','ABSORB.clear();BURSTS.clear();')
s=s.replace('case 2->tc4revival.client.ClientFocusEffects.blockSparkle(level,BlockPos.containing(packet.to),-9999,5);','''case 2->{if(BURSTS.size()<256)BURSTS.add(new Burst(packet.to,(1+level.random.nextInt(3))*(level.random.nextFloat()*.5f+.5f)*2));}
        case 3->{int[] palette=packet.type==1?new int[]{395606,75128,791894,65791,65932}:new int[]{11929351,15611778,15956768,16738082,16739632,0xff0000};var pos=BlockPos.containing(packet.to);for(int i=0;i<20;i++){int color=palette[level.random.nextInt(palette.length)];tc4revival.client.EldritchWisps.rune(level,pos.getX()+offset(),pos.getY()+offset(),pos.getZ()+offset(),(color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,30+level.random.nextInt(10),-.01f-(level.random.nextBoolean()?(level.random.nextBoolean()?.02f:.01f):0));}}
        case 4->{var pos=BlockPos.containing(packet.to);for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++)tc4revival.client.ClientFocusEffects.blockSparkle(level,pos.offset(x,y,z),-9999,10);}
        case 5->WispyLineEffects.add(packet.from,packet.to,40,packet.type);''')
s=s.replace('for(var item:ABSORB)', 'BURSTS.forEach(b->b.age++);BURSTS.removeIf(b->b.age>=31);for(var item:ABSORB)')
s=s.rsplit('}',1)[0]+'''    protected static float offset(){var r=Minecraft.getInstance().level.random;return r.nextFloat()*(r.nextBoolean()?1:-1)/2;}
    @SubscribeEvent public static void render(net.minecraftforge.client.event.RenderLevelStageEvent event){if(event.getStage()!=net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;reset();var mc=Minecraft.getInstance();var buffers=mc.renderBuffers().bufferSource();var type=tc4revival.client.TCRenderTypes.runes(net.minecraft.resources.ResourceLocation.parse("thaumcraft:textures/misc/nodes.png"));var out=buffers.getBuffer(type);for(var burst:BURSTS){var relative=burst.at.subtract(event.getCamera().getPosition());var pose=event.getPoseStack();pose.pushPose();pose.translate(relative.x,relative.y,relative.z);pose.mulPose(event.getCamera().rotation());tc4revival.client.ClientFocusEffects.quad(out,pose.last().pose(),burst.scale,(burst.age+1)/32f,1,burst.age/32f,31/32f,1,1,1,.75f,240);pose.popPose();}buffers.endBatch(type);}
}
'''
f.write_text(s)
f=p/'node/ExtendedNodeEntity.java';s=f.read_text().replace('if(thaumcraft.api.ThaumcraftApiHelper.isResearchComplete(player,"gadomancy:node_manipulator"))','if(snapshot.isPresent()&&tc4revival.research.ResearchCatalogs.catalog().find(key).isPresent()&&tc4revival.research.ResearchCatalogs.rules().prerequisites(snapshot.get(),key))');f.write_text(s)
