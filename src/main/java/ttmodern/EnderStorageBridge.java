package ttmodern;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

/** Optional adapter to EnderStorage 1.20.1 public methods; no base-mod reflection or private access. */
public final class EnderStorageBridge {
    private static final org.slf4j.Logger LOGGER=com.mojang.logging.LogUtils.getLogger();
    private static boolean warned;
    public static boolean available(){return net.minecraftforge.fml.ModList.get().isLoaded("enderstorage");}
    private static Class<?> type(String name) throws ClassNotFoundException {return Class.forName("codechicken.enderstorage."+name);}
    private static void failure(Exception failure){if(!warned){warned=true;LOGGER.warn("Unable to access EnderStorage public integration methods",failure);}}
    public static boolean isChest(BlockEntity entity){if(entity==null||!available())return false;try{return type("tile.TileEnderChest").isInstance(entity);}catch(ReflectiveOperationException error){failure(error);return false;}}
    public static boolean bind(ItemStack focus,BlockEntity chest,ServerPlayer player){try{
        if(!isChest(chest))return false;var frequencyType=type("api.Frequency");Object frequency=chest.getClass().getMethod("getFrequency").invoke(chest);frequency=frequencyType.getMethod("copy").invoke(frequency);
        boolean anarchy=type("config.EnderStorageConfig").getField("anarchyMode").getBoolean(null);Object owner=frequencyType.getMethod("getOwner").invoke(frequency);if(anarchy&&!player.getUUID().equals(owner))frequencyType.getMethod("clearOwner").invoke(frequency);
        frequencyType.getMethod("writeToStack",ItemStack.class).invoke(frequency,focus);focus.getOrCreateTag().putBoolean("EnderStorage",true);return true;
    }catch(ReflectiveOperationException error){failure(error);return false;}}
    public static boolean open(ItemStack focus,ServerPlayer player){if(!available())return false;try{
        var frequencyType=type("api.Frequency");Object frequency=frequencyType.getMethod("readFromStack",ItemStack.class).invoke(null,focus);var managerType=type("manager.EnderStorageManager");Object manager=managerType.getMethod("instance",boolean.class).invoke(null,false);var storageType=type("storage.EnderItemStorage");Object kind=storageType.getField("TYPE").get(null);Object storage=managerType.getMethod("getStorage",frequencyType,type("manager.EnderStorageManager$StorageType")).invoke(manager,frequency,kind);
        var before=player.containerMenu;storageType.getMethod("openContainer",ServerPlayer.class,Component.class).invoke(storage,player,focus.getHoverName());return player.containerMenu!=before;
    }catch(ReflectiveOperationException error){failure(error);return false;}}
    public static void tooltip(ItemStack focus,java.util.List<Component> lines){if(!available())return;try{var frequencyType=type("api.Frequency");Object frequency=frequencyType.getMethod("readFromStack",ItemStack.class).invoke(null,focus);if((boolean)frequencyType.getMethod("hasOwner").invoke(frequency)){Object owner=frequencyType.getMethod("getOwnerName").invoke(frequency);if(owner instanceof Component text)lines.add(text);}lines.add((Component)frequencyType.getMethod("getTooltip").invoke(frequency));}catch(ReflectiveOperationException error){failure(error);}}
    private EnderStorageBridge(){}
}
