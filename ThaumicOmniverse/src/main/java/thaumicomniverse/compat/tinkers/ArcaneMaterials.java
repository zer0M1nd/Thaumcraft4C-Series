package thaumicomniverse.compat.tinkers;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.*;
import slimeknights.tconstruct.library.modifiers.hook.behavior.*;
import slimeknights.tconstruct.library.modifiers.hook.build.*;
import slimeknights.tconstruct.library.modifiers.hook.combat.*;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.mining.*;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileLaunchModifierHook;
import net.minecraft.world.entity.projectile.*;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.context.*;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.*;
import tc4revival.aspect.*;
import tc4revival.aspect.item.ItemAspects;
import tc4revival.content.*;
import thaumicomniverse.RevivalFeatures;

/** Optional TConstruct integration. This class is loaded only after the dependency check. */
public final class ArcaneMaterials {
    public static final ModifierId DIVINE = new ModifierId("thaumicomniverse:divine_realm");
    public static final ModifierId APOCALYPSE = new ModifierId("thaumicomniverse:apocalypse");
    public static final ResourceLocation STORED_SLOTS = id("apocalypse_slots");
    public static final Set<String> THAUMIC_MODS = Set.of("thaumcraft", "automagy", "emt", "forbidden_magic", "gadomancy",
            "nodalmechanics", "taintedmagic", "thaumicbases", "thaumicenergistics", "thaumichorizons", "thaumicinfusion",
            "thaumicomniverse", "thaumic_tinkerer", "warptheory", "witchinggadgets", "thaumicfutures", "thaumicjourney",
            "thaumicexpedition", "thaumicconcilium");
    public static final Map<String,String> INGREDIENTS = Map.of("void", "thaumcraft:void_ingot",
            "alchemical_brass", "thaumicfutures:alchemical_brass_ingot", "thauminite", "thaumicbases:thauminite_ingot",
            "ichorium", "thaumic_tinkerer:ichorium");
    private static class FuturesSlots {
        static void setup(){thaumicfutures.api.tinkers.TinkersSlotViews.registerSlotViewModifier("thaumicomniverse:arcane",0,
            (tool,stored,base,current)->currentSlots(tool,current));}
    }
    private ArcaneMaterials() {}
    public static ResourceLocation id(String name) { return new ResourceLocation("thaumicomniverse",name); }
    public static boolean enabled() { return RevivalFeatures.Group.TINKERS.enabled(); }
    public static void register(IEventBus bus) {
        if(net.minecraftforge.fml.ModList.get().isLoaded("thaumicfutures"))
            bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(FuturesSlots::setup));
        var registry=ModifierDeferredRegister.create("thaumicomniverse");
        registry.register("undertow",Undertow::new); registry.register("void_touch",VoidTouch::new);
        registry.register("arcane_smith",ArcaneSmith::new); registry.register("imbued",Imbued::new);
        registry.register("divine_realm",DivineRealm::new); registry.register("apocalypse",Apocalypse::new);
        registry.register("aura_repair",AuraRepair::new); registry.register("runic_shield",RunicShield::new);
        registry.register(bus);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(BedrockMining.class);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(ArcaneProjectiles.class);
    }
    public static boolean hasMaterial(IToolContext tool) {
        return tool.getMaterials().getList().stream().anyMatch(m->m.getId().getNamespace().equals("thaumicomniverse")
                && INGREDIENTS.containsKey(m.getId().getPath()));
    }
    public static boolean flying(LivingEntity entity) {
        return entity.isFallFlying() || entity instanceof Player player && player.getAbilities().flying;
    }
    public static int visCost(int base, int level) {
        if(level<=0)return 0;
        return (int)Math.min(1_000_000_000L,(long)base << Math.min(30,level-1));
    }
    public static Map<Aspect,Integer> costs(int base,int level) {
        var costs=new LinkedHashMap<Aspect,Integer>();
        for(var aspect:TCAspects.catalog().primals())costs.put(aspect,visCost(base,level));
        return costs;
    }
    /** Match TC4's repair source order and atomic six-aspect transactions, including wand discounts. */
    public static boolean vis(Player player, Map<Aspect,Integer> costs, boolean commit) {
        return vis(player,costs,commit,true);
    }
    public static boolean vis(Player player, Map<Aspect,Integer> costs, boolean commit, boolean discounts) {
        if(player==null || commit && player.level().isClientSide)return false;
        for(var stack:WandInventory.wornBaubles(player)) {
            if(!(stack.getItem() instanceof WandInventory.VisAmulet amulet))continue;
            // The built-in amulet's consume API deliberately refuses client calls, even simulations.
            if(!commit && player.level().isClientSide && stack.getItem() instanceof VisAmuletItem item) {
                if(costs.entrySet().stream().allMatch(e->item.vis(stack,e.getKey())>=e.getValue()))return true;
            } else if(amulet.consumeVis(stack,player,costs,commit))return true;
        }
        var inventory=player.getInventory().items;
        for(int i=inventory.size()-1;i>=0;i--) {
            var stack=inventory.get(i);
            if(stack.getItem() instanceof CastingWandItem wand && thaumcraft.api.wands.WandAutomation.allowed(stack,player)
                    && (discounts?wand.consumeForPlayer(stack,player,costs,false,true,commit)
                        :wand.consume(stack,costs,a->1,false,commit,player.level())))return true;
        }
        return false;
    }
    private static boolean equipped(IToolStackView tool, EquipmentSlot slot) {
        return tool.getItem() instanceof ArmorItem armor ? armor.getEquipmentSlot()==slot : slot==EquipmentSlot.MAINHAND;
    }
    private static void attribute(String name,EquipmentSlot slot,Attribute attribute,double amount,BiConsumer<Attribute,AttributeModifier> consumer) {
        consumer.accept(attribute,new AttributeModifier(UUID.nameUUIDFromBytes(("thaumicomniverse:"+name+":"+slot).getBytes(StandardCharsets.UTF_8)),
                "thaumicomniverse."+name,amount,AttributeModifier.Operation.ADDITION));
    }
    public static final class Undertow extends Modifier implements InventoryTickModifierHook, AttributesModifierHook {
        @Override protected void registerHooks(ModuleHookMap.Builder b) { b.addHook(this,ModifierHooks.INVENTORY_TICK,ModifierHooks.ATTRIBUTES); }
        @Override public void onInventoryTick(IToolStackView tool,ModifierEntry entry,Level world,LivingEntity holder,int slot,boolean selected,boolean correctSlot,ItemStack stack) {
            if(enabled() && !world.isClientSide && holder.tickCount%100==0)ToolDamageUtil.repair(tool,entry.getLevel());
        }
        @Override public void addAttributes(IToolStackView tool,ModifierEntry entry,EquipmentSlot slot,BiConsumer<Attribute,AttributeModifier> consumer) {
            if(enabled() && equipped(tool,slot))attribute("undertow",slot,TCAttributes.WARP.get(),entry.getLevel(),consumer);
        }
    }
    public static final class VoidTouch extends Modifier implements MeleeHitModifierHook {
        @Override protected void registerHooks(ModuleHookMap.Builder b) { b.addHook(this,ModifierHooks.MELEE_HIT); }
        @Override public void afterMeleeHit(IToolStackView tool,ModifierEntry entry,ToolAttackContext context,float damage) {
            var target=context.getLivingTarget();var attacker=context.getAttacker();
            if(!enabled() || context.getLevel().isClientSide || target==null)return;
            if(target instanceof Player && attacker instanceof Player && (target.getServer()==null || !target.getServer().isPvpAllowed()))return;
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,60,entry.getLevel()-1),attacker);
        }
    }
    public static final class ArcaneSmith extends Modifier implements BreakSpeedModifierHook {
        @Override protected void registerHooks(ModuleHookMap.Builder b) { b.addHook(this,ModifierHooks.BREAK_SPEED); }
        @Override public void onBreakSpeed(IToolStackView tool,ModifierEntry entry,PlayerEvent.BreakSpeed event,Direction side,boolean effective,float multiplier) {
            var key=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(event.getState().getBlock());
            if(enabled() && key!=null && THAUMIC_MODS.contains(key.getNamespace()))event.setNewSpeed(event.getNewSpeed()*(1+entry.getLevel()));
        }
    }
    public static final class Imbued extends Modifier implements BreakSpeedModifierHook, BlockBreakModifierHook, MeleeDamageModifierHook, MeleeHitModifierHook, ProjectileLaunchModifierHook {
        @Override protected void registerHooks(ModuleHookMap.Builder b) { b.addHook(this,ModifierHooks.BREAK_SPEED,ModifierHooks.BLOCK_BREAK,ModifierHooks.MELEE_DAMAGE,ModifierHooks.MELEE_HIT,ModifierHooks.PROJECTILE_LAUNCH); }
        @Override public void onProjectileLaunch(IToolStackView tool,ModifierEntry entry,LivingEntity shooter,Projectile projectile,AbstractArrow arrow,ModDataNBT data,boolean primary) {
            if(enabled())projectile.getPersistentData().putInt(ArcaneProjectiles.IMBUED,entry.getLevel());
        }
        @Override public void onBreakSpeed(IToolStackView tool,ModifierEntry entry,PlayerEvent.BreakSpeed event,Direction side,boolean effective,float multiplier) {
            if(enabled() && vis(event.getEntity(),costs(1,entry.getLevel()),false,false))event.setNewSpeed(event.getNewSpeed()*(1+.3f*entry.getLevel()));
        }
        @Override public void afterBlockBreak(IToolStackView tool,ModifierEntry entry,ToolHarvestContext context) {
            if(enabled() && context.getPlayer()!=null && !context.getPlayer().isCreative())vis(context.getPlayer(),costs(1,entry.getLevel()),true,false);
        }
        @Override public float getMeleeDamage(IToolStackView tool,ModifierEntry entry,ToolAttackContext context,float base,float damage) {
            if(ArcaneProjectiles.tagged(context))return damage;
            return enabled() && vis(context.getPlayerAttacker(),costs(10,entry.getLevel()),false,false)?damage*(1+.3f*entry.getLevel()):damage;
        }
        @Override public void afterMeleeHit(IToolStackView tool,ModifierEntry entry,ToolAttackContext context,float damage) {
            if(enabled() && !ArcaneProjectiles.tagged(context))vis(context.getPlayerAttacker(),costs(10,entry.getLevel()),true,false);
        }
    }
    public static final class DivineRealm extends Modifier implements ToolDamageModifierHook, RemoveBlockModifierHook {
        @Override protected void registerHooks(ModuleHookMap.Builder b) { b.addHook(this,ModifierHooks.TOOL_DAMAGE,ModifierHooks.REMOVE_BLOCK); }
        @Override public int onDamageTool(IToolStackView tool,ModifierEntry entry,int amount,LivingEntity holder) { return enabled()?0:amount; }
        @Override public Boolean removeBlock(IToolStackView tool,ModifierEntry entry,ToolHarvestContext context) {
            if(!context.getState().is(net.minecraft.world.level.block.Blocks.BEDROCK))return null;
            if(!BedrockMining.allowed(tool,context.getWorld(),context.getPos()) || context.getPlayer()==null)return false;
            var player=context.getPlayer();var pos=context.getPos();var world=context.getWorld();
            if(player.isSpectator() || !player.mayBuild() || !world.mayInteract(player,pos) || !world.getWorldBorder().isWithinBounds(pos))return false;
            return world.removeBlock(pos,false);
        }
    }
    public static final class Apocalypse extends Modifier implements VolatileDataModifierHook, BreakSpeedModifierHook, MeleeDamageModifierHook, ProjectileLaunchModifierHook {
        @Override protected void registerHooks(ModuleHookMap.Builder b) { b.addHook(this,ModifierHooks.VOLATILE_DATA,ModifierHooks.BREAK_SPEED,ModifierHooks.MELEE_DAMAGE,ModifierHooks.PROJECTILE_LAUNCH); }
        @Override public void onProjectileLaunch(IToolStackView tool,ModifierEntry entry,LivingEntity shooter,Projectile projectile,AbstractArrow arrow,ModDataNBT data,boolean primary) {
            if(enabled())projectile.getPersistentData().putBoolean(ArcaneProjectiles.APOCALYPSE,true);
        }
        @Override public void addVolatileData(IToolContext tool,ModifierEntry entry,ToolDataNBT data) {
            int slots=enabled()?entry.getLevel():0;data.putInt(STORED_SLOTS,slots);data.addSlots(SlotType.UPGRADE,slots);
        }
        @Override public void onBreakSpeed(IToolStackView tool,ModifierEntry entry,PlayerEvent.BreakSpeed event,Direction side,boolean effective,float multiplier) {
            if(enabled() && flying(event.getEntity()))event.setNewSpeed(event.getNewSpeed()*1.4f*(event.getEntity().onGround()?1:5));
        }
        @Override public float getMeleeDamage(IToolStackView tool,ModifierEntry entry,ToolAttackContext context,float base,float damage) {
            if(ArcaneProjectiles.tagged(context))return damage;
            return enabled() && flying(context.getAttacker())?damage*1.4f:damage;
        }
    }
    private abstract static class MaterialUpgrade extends Modifier implements ValidateModifierHook {
        @Override protected void registerHooks(ModuleHookMap.Builder b) { b.addHook(this,ModifierHooks.VALIDATE); }
        @Override public Component validate(IToolStackView tool,ModifierEntry entry) {
            return hasMaterial(tool)?null:Component.translatable("modifier.thaumicomniverse.requires_material");
        }
    }
    public static final class AuraRepair extends MaterialUpgrade implements InventoryTickModifierHook {
        @Override protected void registerHooks(ModuleHookMap.Builder b) { super.registerHooks(b);b.addHook(this,ModifierHooks.INVENTORY_TICK); }
        @Override public void onInventoryTick(IToolStackView tool,ModifierEntry entry,Level world,LivingEntity holder,int slot,boolean selected,boolean correctSlot,ItemStack stack) {
            if(!enabled() || !hasMaterial(tool) || !(holder instanceof ServerPlayer player) || player.isCreative() || holder.tickCount%40!=0 || tool.getDamage()<=0)return;
            if(!(slot>=0 && slot<9) && !(tool.getItem() instanceof ArmorItem && correctSlot))return;
            int level=Math.min(2,entry.getLevel());
            var base=ItemAspects.baseSession().base(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(tool.getItem()).toString()).orElseGet(AspectAmounts::new);
            if(base.isEmpty()) {
                // Modular tools often have no fixed object tags. Use their actual thaumic materials, without enchantment scan bonuses.
                for(var material:tool.getMaterials().getList()) {
                    if(!material.getId().getNamespace().equals("thaumicomniverse"))continue;
                    var source=INGREDIENTS.get(material.getId().getPath());if(source==null)continue;
                    ItemAspects.baseSession().base(source).ifPresent(aspects->aspects.view().forEach(base::merge));
                }
            }
            if(!base.isEmpty() && vis(player,VisRepair.costs(TCAspects.catalog(),base,level).view(),true))ToolDamageUtil.repair(tool,level);
        }
    }
    public static final class RunicShield extends MaterialUpgrade implements AttributesModifierHook {
        @Override protected void registerHooks(ModuleHookMap.Builder b) { super.registerHooks(b);b.addHook(this,ModifierHooks.ATTRIBUTES); }
        @Override public Component validate(IToolStackView tool,ModifierEntry entry) {
            if(!(tool.getItem() instanceof ArmorItem))return Component.translatable("modifier.thaumicomniverse.requires_armor");
            return super.validate(tool,entry);
        }
        @Override public void addAttributes(IToolStackView tool,ModifierEntry entry,EquipmentSlot slot,BiConsumer<Attribute,AttributeModifier> consumer) {
            if(enabled() && hasMaterial(tool) && tool.getItem() instanceof ArmorItem && equipped(tool,slot))
                attribute("runic_shield",slot,TCAttributes.RUNIC_SHIELD.get(),Math.min(3,entry.getLevel())*2,consumer);
        }
    }
    /** Config changes must also affect saved tools whose derived slots were built in another profile. */
    public static IModDataView currentSlots(ToolStack tool,IModDataView data) {
        int level=tool.getModifierLevel(APOCALYPSE);if(level==0 || data==IModDataView.EMPTY)return data;
        int stored=data.getInt(STORED_SLOTS),current=enabled()?level:0;
        if(stored==current)return data;
        return new IModDataView() {
            @Override public <T>T get(ResourceLocation key,java.util.function.BiFunction<net.minecraft.nbt.CompoundTag,String,T> reader){return data.get(key,reader);}
            @Override public boolean contains(ResourceLocation key){return data.contains(key);}
            @Override public boolean contains(ResourceLocation key,int type){return data.contains(key,type);}
            @Override public int getSlots(SlotType type){return data.getSlots(type)+(type==SlotType.UPGRADE?current-stored:0);}
        };
    }
}
