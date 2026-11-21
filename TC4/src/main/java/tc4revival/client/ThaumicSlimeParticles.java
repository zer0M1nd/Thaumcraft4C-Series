package tc4revival.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import tc4revival.content.ThaumicSlimeEntity;

public  class ThaumicSlimeParticles {
    private ThaumicSlimeParticles() {}
    public static void sploosh(net.minecraft.world.entity.Entity entity,int count) {
        if (!(entity.level() instanceof ClientLevel level)) return;
        for (int i=0;i<count;i++) {
            float angle=level.random.nextFloat()*(float)Math.PI*2, radius=level.random.nextFloat()*.5f+.5f;
            Minecraft.getInstance().particleEngine.add(new Fragment(level,entity.getX()+Math.sin(angle)*radius,
                    entity.getY()+level.random.nextFloat()*entity.getBbHeight(),entity.getZ()+Math.cos(angle)*radius,true));
        }
    }
    public static void spawn(net.minecraft.world.entity.Entity entity,int size,int count) {
        if (!(entity.level() instanceof ClientLevel level)) return;
        for (int i=0;i<count;i++) {
            float angle=level.random.nextFloat()*(float)Math.PI*2, radius=(level.random.nextFloat()*.5f+.5f)*size*.5f;
            Minecraft.getInstance().particleEngine.add(new Fragment(level,entity.getX()+Math.sin(angle)*radius,
                    (entity.getBoundingBox().minY+entity.getBoundingBox().maxY)/2,entity.getZ()+Math.cos(angle)*radius));
        }
    }
    public static void tentacleArise(net.minecraft.world.entity.Entity entity) {
        if (!(entity.level() instanceof ClientLevel level)) return;
        var below=entity.blockPosition().below(); var state=level.getBlockState(below); float height=entity.getBbHeight();
        for (int index=0;index<2*height;index++) {
            float angle=level.random.nextFloat()*(float)Math.PI*height,radius=height*.25f*(level.random.nextFloat()*.5f+.5f);
            Minecraft.getInstance().particleEngine.add(new Fragment(level,entity.getX()+Math.sin(angle)*radius,entity.getY(),entity.getZ()+Math.cos(angle)*radius,2));
            if (!state.isAir()) {
                angle=level.random.nextFloat()*(float)Math.PI*height; radius=height*.25f*(level.random.nextFloat()*.5f+.5f);
                var debris=new net.minecraft.client.particle.TerrainParticle(level,entity.getX()+Math.sin(angle)*radius,entity.getY(),entity.getZ()+Math.cos(angle)*radius,0,0,0,state,below);
                Minecraft.getInstance().particleEngine.add(debris);
            }
        }
    }
    private static  class Fragment extends TextureSheetParticle {
        private final float u,v;
        private final float initialAlpha;
        Fragment(ClientLevel level,double x,double y,double z) {
            this(level,x,y,z,false);
        }
        Fragment(ClientLevel level,double x,double y,double z,boolean spore) {
            this(level,x,y,z,spore ? 1 : 0);
        }
        Fragment(ClientLevel level,double x,double y,double z,int kind) {
            super(level,x,y,z,0,0,0);
            setSprite(Minecraft.getInstance().getItemRenderer().getItemModelShaper().getItemModel(new ItemStack(Items.SLIME_BALL)).getParticleIcon());
            u=random.nextFloat()*3; v=random.nextFloat()*3;
            rCol=.7f; gCol=0; bCol=1; alpha=.4f; gravity=1; quadSize*=.5f;
            if (kind==1) { boolean bright=level.random.nextBoolean(); rCol=bright ? .6f : .3f; bCol=.3f; alpha=bright ? .4f : .6f; }
            if (kind==2) { rCol=.4f; bCol=.4f; alpha=.5f; }
            initialAlpha=alpha;
            lifetime=(int)(66/(level.random.nextFloat()*.9f+.1f));
        }
        @Override public ParticleRenderType getRenderType() { return ParticleRenderType.TERRAIN_SHEET; }
        @Override protected float getU0() { return sprite.getU(u*4); }
        @Override protected float getU1() { return sprite.getU((u+1)*4); }
        @Override protected float getV0() { return sprite.getV(v*4); }
        @Override protected float getV1() { return sprite.getV((v+1)*4); }
        @Override public float getQuadSize(float partial) { return quadSize*(1-age/(float)lifetime); }
        @Override public void render(VertexConsumer buffer,Camera camera,float partial) { alpha=initialAlpha*(1-age/(float)lifetime); super.render(buffer,camera,partial); }
    }
}
