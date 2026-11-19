package tc4revival.client;

import java.lang.reflect.Method;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

/** Optional Patchouli access, without making the core mod depend on it. */
final class PatchouliLookupCompat {
    private record Access(Method book,Method contents,Method entry,Method locked,Method config,Method shift,Method time) {}
    private static boolean initialized;
    private static Access access;
    private static Access access(){
        if(!initialized){
            initialized=true;
            if(ModList.get().isLoaded("patchouli"))try{
                var util=Class.forName("vazkii.patchouli.common.util.ItemStackUtil");
                var book=Class.forName("vazkii.patchouli.common.book.Book");
                var contents=Class.forName("vazkii.patchouli.client.book.BookContents");
                var entry=Class.forName("vazkii.patchouli.client.book.BookEntry");
                var config=Class.forName("vazkii.patchouli.common.base.PatchouliConfig");
                var configApi=Class.forName("vazkii.patchouli.api.PatchouliConfigAccess");
                access=new Access(util.getMethod("getBookFromStack",ItemStack.class),book.getMethod("getContents"),
                    contents.getMethod("getEntryForStack",ItemStack.class),entry.getMethod("isLocked"),config.getMethod("get"),
                    configApi.getMethod("useShiftForQuickLookup"),configApi.getMethod("quickLookupTime"));
            }catch(ReflectiveOperationException|LinkageError error){
                tc4revival.TC4Revival.LOGGER.warn("Cannot read Patchouli quick lookup API",error);
            }
        }
        return access;
    }
    /** Match Patchouli's eligibility: an unlocked entry in a book carried in the hotbar. */
    static boolean hasLookup(ItemStack stack){
        var api=access();var player=Minecraft.getInstance().player;
        if(api==null||player==null)return false;
        try{
            for(int slot=0;slot<Inventory.getSelectionSize();slot++){
                var book=api.book().invoke(null,player.getInventory().getItem(slot));
                if(book==null)continue;
                var pair=(Pair<?,?>)api.entry().invoke(api.contents().invoke(book),stack);
                if(pair!=null&&!((Boolean)api.locked().invoke(pair.getFirst())))return true;
            }
        }catch(ReflectiveOperationException error){disable(error);}
        return false;
    }
    static boolean shift(){
        var api=access();if(api!=null)try{return (Boolean)api.shift().invoke(api.config().invoke(null));}
        catch(ReflectiveOperationException error){disable(error);}
        return false;
    }
    static float duration(){
        var api=access();if(api!=null)try{return Math.max(1,((Number)api.time().invoke(api.config().invoke(null))).floatValue());}
        catch(ReflectiveOperationException error){disable(error);}
        return 10;
    }
    private static void disable(ReflectiveOperationException error){
        access=null;tc4revival.TC4Revival.LOGGER.warn("Patchouli quick lookup compatibility disabled after API error",error);
    }
    private PatchouliLookupCompat(){}
}
