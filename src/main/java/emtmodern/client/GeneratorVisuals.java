package emtmodern.client;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
/** Legacy uses vanilla EntitySmokeFX, scale 3, with the consumed aspect's RGB tint. */
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
public class GeneratorVisuals {
    public static void smoke(BlockPos pos,int color){
        var particle=Minecraft.getInstance().particleEngine.createParticle(ParticleTypes.SMOKE,pos.getX()+.5,pos.getY()+1.1,pos.getZ()+.5,0,.1,0);
        if(particle!=null){particle.scale(3);particle.setColor((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f);}
    }
}
