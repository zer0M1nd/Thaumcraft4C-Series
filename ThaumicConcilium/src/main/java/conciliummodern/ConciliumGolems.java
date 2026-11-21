package conciliummodern;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.*;
import tc4revival.content.*;

/** Stable task IDs use the core's native golem state, bell, markers and ghost inventory. */
@Mod.EventBusSubscriber(modid=ThaumicConcilium.ID)
public final class ConciliumGolems {
    public static final ResourceLocation ASSISTANT=id("assistant"),VALET=id("valet"),DATA=id("golem_commands");
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,ThaumicConcilium.ID);
    public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,ThaumicConcilium.ID);
    public static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,ThaumicConcilium.ID);
    public static final DeferredRegister<net.minecraft.sounds.SoundEvent> SOUNDS=DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,ThaumicConcilium.ID);
    public static final RegistryObject<net.minecraft.sounds.SoundEvent> GNAW=SOUNDS.register("gnaw",()->net.minecraft.sounds.SoundEvent.createVariableRangeEvent(id("gnaw")));
    public static final RegistryObject<Core> ASSISTANCE_CORE=ITEMS.register("assistance_core",()->new Core(ASSISTANT));
    public static final RegistryObject<Core> VALET_CORE=ITEMS.register("valet_core",()->new Core(VALET));
    public static final RegistryObject<EntityType<GolemBydlo>> BYDLO=ENTITIES.register("golem_bydlo",()->EntityType.Builder.of(GolemBydlo::new,MobCategory.CREATURE).sized(2,3.5f).clientTrackingRange(8).updateInterval(3).build(ThaumicConcilium.ID+":golem_bydlo"));
    public static final RegistryObject<EntityType<VengefulGolem>> VENGEFUL=ENTITIES.register("vengeful_golem",()->EntityType.Builder.of(VengefulGolem::new,MobCategory.MONSTER).sized(.6f,1.8f).clientTrackingRange(8).updateInterval(3).build(ThaumicConcilium.ID+":vengeful_golem"));
    public static final RegistryObject<Item> BYDLO_EGG=ITEMS.register("golem_bydlo_spawn_egg",()->new net.minecraftforge.common.ForgeSpawnEggItem(BYDLO,0x00ffff,0x555555,new Item.Properties()));
    public static final RegistryObject<Item> VENGEFUL_EGG=ITEMS.register("vengeful_golem_spawn_egg",()->new net.minecraftforge.common.ForgeSpawnEggItem(VENGEFUL,0x00ffff,0x00008b,new Item.Properties()));
    public static final RegistryObject<MenuType<GolemMenu>> MENU=MENUS.register("golem_commands",()->net.minecraftforge.common.extensions.IForgeMenuType.<GolemMenu>create(ConciliumGolemMenu::new));
    public static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath(ThaumicConcilium.ID,path);}
    public static boolean available(ResourceLocation task){return net.minecraftforge.fml.ModList.get().isLoaded("gadomancy")&&(!task.equals(ASSISTANT)||net.minecraftforge.fml.ModList.get().isLoaded("thaumichorizons"));}
    public static boolean ours(GolemEntity golem){var task=golem.configuration().customTask();return ASSISTANT.equals(task)||VALET.equals(task);}
    public static boolean owned(GolemEntity golem,Player player){return golem.ownerName().isEmpty()||golem.ownerName().equals(player.getGameProfile().getName());}
    public static CompoundTag data(GolemEntity golem){return golem.configuration().addonData(DATA);}
    public static void data(GolemEntity golem,CompoundTag data){thaumcraft.api.golems.GolemApi.data(golem,DATA,data);}
    public static void register(IEventBus bus){
        ITEMS.register(bus);ENTITIES.register(bus);MENUS.register(bus);SOUNDS.register(bus);ConciliumGolemSpawns.CODECS.register(bus);bus.addListener(ConciliumGolemSpawns::placements);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON,ConciliumGolemSpawns.SPEC,"thaumicconcilium-golems.toml");
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(ConciliumGolems::setup));
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event)->{event.put(BYDLO.get(),GolemBydlo.attributes().build());event.put(VENGEFUL.get(),VengefulGolem.attributes().build());});
    }
    private static void commonGoals(GolemEntity golem,net.minecraft.world.entity.ai.goal.GoalSelector work){work.addGoal(5,new GolemOpenDoorGoal(golem));work.addGoal(6,new GolemReturnHomeGoal(golem));work.addGoal(7,new LookAtPlayerGoal(golem,Player.class,6));work.addGoal(8,new RandomLookAroundGoal(golem));}
    private static void setup(){
        thaumcraft.api.golems.GolemApi.registerTask(ASSISTANT,(golem,work,targets)->{work.addGoal(0,new AssistantJarSupplyGoal(golem));work.addGoal(3,new AssistantCaptureGoal(golem));commonGoals(golem,work);});
        thaumcraft.api.golems.GolemApi.registerTask(VALET,(golem,work,targets)->{work.addGoal(3,new ValetCommandGoal(golem));commonGoals(golem,work);});
        thaumcraft.api.research.ResearchCategories.registerEnablement(ThaumicConcilium.ID+":ASSISTANCECORE",()->available(ASSISTANT));
        thaumcraft.api.research.ResearchCategories.registerEnablement(ThaumicConcilium.ID+":VALETCORE",()->available(VALET));
        thaumcraft.api.research.ResearchCategories.registerEnablement(ThaumicConcilium.ID+":BUFFGOLEM",()->net.minecraftforge.fml.ModList.get().isLoaded("thaumichorizons"));
    }
    public static void creative(CreativeModeTab.Output output){if(available(ASSISTANT))output.accept(ASSISTANCE_CORE.get());if(available(VALET))output.accept(VALET_CORE.get());if(net.minecraftforge.fml.ModList.get().isLoaded("thaumichorizons"))output.accept(BYDLO_EGG.get());if(net.minecraftforge.fml.ModList.get().isLoaded("automagy"))output.accept(VENGEFUL_EGG.get());}
    @SubscribeEvent(priority=EventPriority.HIGH) public static void interact(PlayerInteractEvent.EntityInteract event){
        if(!(event.getTarget() instanceof GolemEntity golem)||!owned(golem,event.getEntity())||event.isCanceled())return;
        var held=event.getItemStack();var player=event.getEntity();
        if(held.getItem() instanceof Core item && available(item.task)&&!golem.configuration().hasTask()){
            if(!event.getLevel().isClientSide){golem.configuration().installCore(item.task.equals(ASSISTANT)?GolemCoreType.EMPTY:GolemCoreType.USE);thaumcraft.api.golems.GolemApi.configure(golem,golem.configuration().customMaterial(),item.task,golem.configuration().blockAppearance());if(!player.getAbilities().instabuild)held.shrink(1);golem.playSound(TCSounds.GOLEM_UPGRADE.get(),.5f,1);golem.level().broadcastEntityEvent(golem,(byte)7);}
            event.setCanceled(true);event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));return;
        }
        if(!ours(golem)||player.isShiftKeyDown()||held.is(Items.NAME_TAG)||held.getItem() instanceof GolemBellItem||held.getItem() instanceof GolemComponentItem||held.getItem() instanceof CastingWandItem)return;
        if(player instanceof ServerPlayer server)net.minecraftforge.network.NetworkHooks.openScreen(server,new SimpleMenuProvider((id,inventory,viewer)->new ConciliumGolemMenu(id,inventory,golem),golem.getDisplayName()),buffer->{buffer.writeInt(golem.getId());buffer.writeNbt(golem.state().visualData());});
        event.setCanceled(true);event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
    }
    @SubscribeEvent public static void chat(net.minecraftforge.event.ServerChatEvent event){
        var player=event.getPlayer();for(var golem:player.level().getEntitiesOfClass(GolemEntity.class,player.getBoundingBox().inflate(32),g->VALET.equals(g.configuration().customTask())&&owned(g,player))){
            var data=data(golem);data.putString("Command",event.getRawText());data.putBoolean("Pending",true);data(golem,data);
        }
    }
    /** The generic bell would otherwise detach the compatibility USE/EMPTY core instead. */
    @SubscribeEvent(priority=EventPriority.HIGH) public static void dismantle(AttackEntityEvent event){
        if(!(event.getTarget() instanceof GolemEntity golem)||!ours(golem)||!(event.getEntity().getMainHandItem().getItem() instanceof GolemBellItem))return;
        if(!owned(golem,event.getEntity())){event.setCanceled(true);return;}if(!event.getEntity().isShiftKeyDown())return;
        event.setCanceled(true);if(golem.level().isClientSide||!owned(golem,event.getEntity())||!golem.isAlive())return;
        var config=golem.configuration();var portable=new ItemStack(TCItems.golem(config.material()));var tag=portable.getOrCreateTag();config.saveExtension(tag);tag.remove("AddonTask");tag.getCompound("AddonData").remove(DATA.toString());
        if(config.advanced())tag.putByte("advanced",(byte)1);tag.putBoolean("AxisAligned",config.axisAligned());
        if(golem.spawnAtLocation(portable,.5f)==null)return;
        golem.spawnAtLocation(new ItemStack(ASSISTANT.equals(config.customTask())?ASSISTANCE_CORE.get():VALET_CORE.get()),.5f);
        for(byte upgrade:config.upgrades())if(upgrade>=0&&golem.getRandom().nextBoolean())golem.spawnAtLocation(new ItemStack(TCItems.golemUpgrade(upgrade)),.5f);
        golem.dropCargo();golem.playSound(TCSounds.ZAP.get(),.5f,1);golem.discard();
    }
    public static final class Core extends Item {
        public final ResourceLocation task;Core(ResourceLocation task){super(new Properties().rarity(Rarity.UNCOMMON));this.task=task;}
        @Override public Component getName(ItemStack stack){return Component.translatable("item.ItemGolemCore.name");}
        @Override public void appendHoverText(ItemStack stack,net.minecraft.world.level.Level level,java.util.List<Component> lines,TooltipFlag flag){lines.add(Component.translatable(task.equals(ASSISTANT)?"item.TCItemGolemCore.0.name":"item.TCItemGolemCore.1.name"));}
    }
    private ConciliumGolems(){}
}
