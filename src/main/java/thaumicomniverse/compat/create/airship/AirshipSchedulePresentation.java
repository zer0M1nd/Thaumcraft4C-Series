package thaumicomniverse.compat.create.airship;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;

/** Scoped to the held airship schedule menu; ordinary Create schedules retain their own vocabulary. */
public final class AirshipSchedulePresentation {
    private AirshipSchedulePresentation(){}
    public static boolean active(){var mc=Minecraft.getInstance();return mc!=null&&mc.player!=null&&mc.player.containerMenu instanceof com.simibubi.create.content.trains.schedule.ScheduleMenu&&mc.player.getMainHandItem().getItem() instanceof AirshipScheduleItem;}
    public static String key(String original){
        if(!active()||!original.startsWith("create."))return original;
        String changed="airship."+original;return I18n.exists(changed)?changed:original;
    }
    public static ItemStack icon(ItemStack original){
        if(active()&&original.is(com.simibubi.create.AllBlocks.TRACK_STATION.get().asItem()))return new ItemStack(AirshipContent.STATION.get());
        return original;
    }
}
