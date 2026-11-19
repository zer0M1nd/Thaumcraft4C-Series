package ttmodern;

import java.lang.reflect.Method;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Uses only Botania's public interfaces, loaded lazily when the optional mod is present. */
public final class BotaniaDeflection {
    private record Api(Class<?> burst,Class<?> basic,Class<?> compositable,Method lens,Method fake,Method shooter,Method composite,Method properties){}
    private static Api api;private static boolean initialized,warned;
    private static Api api(){if(initialized)return api;initialized=true;if(!net.minecraftforge.fml.ModList.get().isLoaded("botania"))return null;try{
        var burst=Class.forName("vazkii.botania.api.internal.ManaBurst");var basic=Class.forName("vazkii.botania.api.mana.BasicLensItem");var compositable=Class.forName("vazkii.botania.api.mana.CompositableLensItem");api=new Api(burst,basic,compositable,burst.getMethod("getSourceLens"),burst.getMethod("isFake"),burst.getMethod("getShooterUUID"),basic.getMethod("getCompositeLens",ItemStack.class),compositable.getMethod("getProps",ItemStack.class));
    }catch(ReflectiveOperationException error){warn(error);}return api;}
    private static void warn(Exception error){if(!warned){warned=true;com.mojang.logging.LogUtils.getLogger().warn("Botania deflection integration could not read public lens API",error);}}
    public static boolean harmless(Projectile projectile,Player player){var api=api();if(api==null)return net.minecraft.resources.ResourceLocation.parse("botania:mana_burst").equals(net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(projectile.getType()));if(!api.burst().isInstance(projectile))return false;try{
        if((boolean)api.fake().invoke(projectile)||player.getUUID().equals(api.shooter().invoke(projectile)))return true;
        return !damaging((ItemStack)api.lens().invoke(projectile),api,0);
    }catch(ReflectiveOperationException error){warn(error);return true;}}
    private static boolean damaging(ItemStack lens,Api api,int depth) throws ReflectiveOperationException {
        if(lens.isEmpty()||depth>=16)return false;var item=lens.getItem();
        // Botania LensItem.PROP_DAMAGE: damage and explosive lenses, including addon implementations.
        if(api.compositable().isInstance(item)&&(((Number)api.properties().invoke(item,lens)).intValue()&16)!=0)return true;
        return api.basic().isInstance(item)&&damaging((ItemStack)api.composite().invoke(item,lens),api,depth+1);
    }
    private BotaniaDeflection(){}
}
