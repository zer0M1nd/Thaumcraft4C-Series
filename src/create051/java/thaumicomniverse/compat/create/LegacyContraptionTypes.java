package thaumicomniverse.compat.create;
import java.util.function.Supplier;
import com.simibubi.create.content.contraptions.*;
public final class LegacyContraptionTypes {
    public static Supplier<ContraptionType> register(String name,Supplier<? extends Contraption> factory){
        var type=ContraptionType.register("thaumicomniverse:"+name,factory);return ()->type;
    }
}
