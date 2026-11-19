package conciliummodern;

import java.util.Objects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** A modern Thaumic Dyes host supplies its real equipment; no unverified registry names are guessed. */
public final class ConciliumDyesIntegration {
    public interface Host {ItemStack equipment(String role,EquipmentSlot slot);}
    private static Host host;
    /** Called after item registration. Keep the optional module out of the linkage graph. */
    static void setup(){
        if(host!=null||!net.minecraftforge.fml.ModList.get().isLoaded("thaumicdyes"))return;
        try{
            var method=Class.forName("dyesmodern.DyesEquipment").getMethod("forConcilium",String.class,EquipmentSlot.class);
            register((role,slot)->{
                try{return (ItemStack)method.invoke(null,role,slot);}
                catch(ReflectiveOperationException exception){throw new IllegalStateException("Thaumic Dyes equipment bridge failed",exception);}
            });
        }catch(ReflectiveOperationException exception){
            throw new IllegalStateException("Installed Thaumic Dyes does not provide its equipment API",exception);
        }
    }
    public static void register(Host value){if(host!=null)throw new IllegalStateException("Thaumic Dyes host already registered");host=Objects.requireNonNull(value);}
    public static boolean available(){return host!=null&&net.minecraftforge.fml.ModList.get().isLoaded("thaumicdyes");}
    public static ItemStack equipment(String role,EquipmentSlot slot){if(!available())return ItemStack.EMPTY;var result=host.equipment(role,slot);return result==null?ItemStack.EMPTY:result.copy();}
    private ConciliumDyesIntegration(){}
}
