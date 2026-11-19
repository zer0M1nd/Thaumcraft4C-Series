package conciliummodern;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.*;

public final class ConciliumSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS=DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,ThaumicConcilium.ID);
    public static final RegistryObject<SoundEvent> SAD=SOUNDS.register("sad",()->SoundEvent.createVariableRangeEvent(new ResourceLocation(ThaumicConcilium.ID,"sad")));
    private ConciliumSounds() {}
    public static final RegistryObject<SoundEvent> HUM=sound("hum"),BREATH=sound("breath"),WHISPERS=sound("whispers"),IRA=sound("ira");
    public static final RegistryObject<SoundEvent> BUBBLES=sound("bubbles");
    public static final RegistryObject<SoundEvent> MELTED=sound("melted");
    public static final RegistryObject<SoundEvent> STAB=sound("stab");
    public static final RegistryObject<SoundEvent> CHANT=sound("chant"),GROWL=sound("growl"),SPELL=sound("spell"),SHACKLES=sound("shackles"),SHACKLES_THROW=sound("shackles_throw");
    public static final RegistryObject<SoundEvent> CLACK=sound("clack"),MILL=sound("mill"),SHARD_DROP=sound("shard_drop");
    private static RegistryObject<SoundEvent> sound(String name){return SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ThaumicConcilium.ID,name)));}
    public static final RegistryObject<SoundEvent> THINK=SOUNDS.register("think",()->SoundEvent.createVariableRangeEvent(new ResourceLocation(ThaumicConcilium.ID,"think")));
    public static final RegistryObject<SoundEvent> THUMP=SOUNDS.register("thump",()->SoundEvent.createVariableRangeEvent(new ResourceLocation(ThaumicConcilium.ID,"thump")));
}
