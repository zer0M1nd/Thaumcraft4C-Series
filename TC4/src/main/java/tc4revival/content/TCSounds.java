package tc4revival.content;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import tc4revival.TC4Revival;

public  class TCSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, TC4Revival.MOD_ID);
    public static final RegistryObject<SoundEvent> WIND=sound("wind");
    public static final RegistryObject<SoundEvent> GOLEM_IRON_SHOOT=sound("golemironshoot");
    public static final RegistryObject<SoundEvent> GOLEM_UPGRADE=sound("upgrade");
    public static final RegistryObject<SoundEvent> SWING=sound("swing");
    public static final RegistryObject<SoundEvent> CAMERA_TICKS = SOUNDS.register("cameraticks", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.parse("thaumcraft:cameraticks")));
    public static final RegistryObject<SoundEvent> LEARN = SOUNDS.register("learn", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.parse("thaumcraft:learn")));
    public static final RegistryObject<SoundEvent> WRITE = SOUNDS.register("write", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.parse("thaumcraft:write")));
    public static final RegistryObject<SoundEvent> ERASE = SOUNDS.register("erase", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.parse("thaumcraft:erase")));
    public static final RegistryObject<SoundEvent> WHISPERS = SOUNDS.register("whispers", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.parse("thaumcraft:whispers")));
    public static final RegistryObject<SoundEvent> PECH_IDLE=sound("pech_idle");
    public static final RegistryObject<SoundEvent> PECH_TRADE=sound("pech_trade");
    public static final RegistryObject<SoundEvent> PECH_DICE=sound("pech_dice");
    public static final RegistryObject<SoundEvent> PECH_HIT=sound("pech_hit");
    public static final RegistryObject<SoundEvent> PECH_DEATH=sound("pech_death");
    public static final RegistryObject<SoundEvent> PECH_CHARGE=sound("pech_charge");
    private TCSounds() {}
    public static final RegistryObject<SoundEvent> CAMERA_CLACK = sound("cameraclack");
    public static final RegistryObject<SoundEvent> HH_ON = sound("hhon");
    public static final RegistryObject<SoundEvent> HH_OFF = sound("hhoff");
    public static final RegistryObject<SoundEvent> DOOR_FAIL = sound("doorfail");
    public static final RegistryObject<SoundEvent> KEY = sound("key");
    public static final RegistryObject<SoundEvent> URN_BREAK = sound("urnbreak");
    public static final RegistryObject<SoundEvent> ELDRITCH_IDLE = sound("egidle");
    public static final RegistryObject<SoundEvent> ELDRITCH_DEATH = sound("egdeath");
    public static final RegistryObject<SoundEvent> ELDRITCH_ATTACK = sound("egattack");
    public static final RegistryObject<SoundEvent> ELDRITCH_SCREECH = sound("egscreech");
    public static final RegistryObject<SoundEvent> CRAB_CLAW = sound("crabclaw");
    public static final RegistryObject<SoundEvent> CRAB_TALK = sound("crabtalk");
    public static final RegistryObject<SoundEvent> CRAB_DEATH = sound("crabdeath");
    public static final RegistryObject<SoundEvent> PAGE = sound("page");
    public static final RegistryObject<SoundEvent> WAND = sound("wand");
    public static final RegistryObject<SoundEvent> JAR = sound("jar");
    public static final RegistryObject<SoundEvent> INFUSER_START = sound("infuserstart");
    public static final RegistryObject<SoundEvent> INFUSER = sound("infuser");
    public static final RegistryObject<SoundEvent> ALEMBIC_KNOCK = sound("alembicknock");
    public static final RegistryObject<SoundEvent> SQUEEK = sound("squeek");
    public static final RegistryObject<SoundEvent> CREAK = sound("creak");
    public static final RegistryObject<SoundEvent> TOOL = sound("tool");
    public static final RegistryObject<SoundEvent> RUNIC_SHIELD_EFFECT = sound("runic_shield_effect");
    public static final RegistryObject<SoundEvent> COINS = sound("coins");
    public static final RegistryObject<SoundEvent> RUNIC_SHIELD_CHARGE = sound("runic_shield_charge");
    public static final RegistryObject<SoundEvent> SHOCK = sound("shock");
    public static final RegistryObject<SoundEvent> ICE = sound("ice");
    public static final RegistryObject<SoundEvent> FIRE_LOOP = sound("fireloop");
    public static final RegistryObject<SoundEvent> RUMBLE = sound("rumble");
    public static final RegistryObject<SoundEvent> WAND_FAIL = sound("wandfail");
    public static final RegistryObject<SoundEvent> ZAP = sound("zap");
    public static final RegistryObject<SoundEvent> JACOBS = sound("jacobs");
    public static final RegistryObject<SoundEvent> CRAFT_START = sound("craftstart");
    public static final RegistryObject<SoundEvent> CRAFT_FAIL = sound("craftfail");
    public static final RegistryObject<SoundEvent> CRYSTAL = sound("crystal");
    public static final RegistryObject<SoundEvent> EVIL_PORTAL = sound("evilportal");
    public static final RegistryObject<SoundEvent> PUMP = sound("pump");
    public static final RegistryObject<SoundEvent> GORE = sound("gore");
    public static final RegistryObject<SoundEvent> SWARM_ATTACK = sound("swarmattack");
    public static final RegistryObject<SoundEvent> SWARM = sound("swarm");
    public static final RegistryObject<SoundEvent> FLY = sound("fly");
    public static final RegistryObject<SoundEvent> ROOTS = sound("roots");
    public static final RegistryObject<SoundEvent> CHANT = sound("chant");
    public static final RegistryObject<SoundEvent> MONOLITH = sound("monolith");
    public static final RegistryObject<SoundEvent> TENTACLE = sound("tentacle");
    public static final RegistryObject<SoundEvent> BUBBLE = sound("bubble");
    public static final RegistryObject<SoundEvent> SPILL = sound("spill");
    public static final RegistryObject<SoundEvent> HEARTBEAT = sound("heartbeat");
    public static final RegistryObject<SoundEvent> BRAIN=sound("brain");
    public static final RegistryObject<SoundEvent> WISP_LIVE=sound("wisplive"), WISP_DEAD=sound("wispdead");
    private static RegistryObject<SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.parse("thaumcraft:" + name)));
    }
    public static void register(IEventBus bus) { SOUNDS.register(bus); }
}
