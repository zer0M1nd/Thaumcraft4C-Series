package tbmodern;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraftforge.event.entity.item.ItemExpireEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.*;
import tc4revival.aspect.*;
import tc4revival.aspect.entity.EntityAspects;
import tc4revival.content.*;
import thaumcraft.api.ThaumcraftApiHelper;

@Mod.EventBusSubscriber(modid="thaumicbases")
public final class Enchantments {
    public static final DeferredRegister<Enchantment> REGISTER = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS,"thaumicbases");
    public static final Map<String,RegistryObject<Enchantment>> ENTRIES = new LinkedHashMap<>();
    static {
        for(String id:List.of("elder_knowledge","eldritch_bane","magic_touch","tainted","vaporising"))
            ENTRIES.put(id,REGISTER.register(id,()->new Kind(id)));
    }
    public static final class Kind extends Enchantment {
        private final String id;
        private Kind(String id) { super(id.equals("eldritch_bane") || id.equals("vaporising") ? Rarity.UNCOMMON : Rarity.COMMON,
            EnchantmentCategory.WEAPON,EquipmentSlot.values());this.id=id; }
        @Override public int getMaxLevel() { return id.equals("elder_knowledge") || id.equals("eldritch_bane") ? 5 : 3; }
        @Override public int getMinCost(int level) { return id.equals("elder_knowledge") || id.equals("eldritch_bane") ? 1+level*5 : super.getMinCost(level); }
        @Override public boolean canEnchant(ItemStack stack) { return id.equals("tainted") ? stack.getItem() instanceof SwordItem : super.canEnchant(stack); }
        @Override public boolean canApplyAtEnchantingTable(ItemStack stack) { return !id.equals("tainted") && super.canApplyAtEnchantingTable(stack); }
        @Override public boolean isDiscoverable() { return !id.equals("tainted"); }
        @Override public boolean isTradeable() { return !id.equals("tainted"); }
        @Override protected boolean checkCompatibility(Enchantment other) {
            if(other==this)return false;
            if(id.equals("eldritch_bane"))return !(other instanceof DamageEnchantment);
            return !(other instanceof Kind kind) || kind.id.equals("eldritch_bane");
        }
    }
    private static int level(ItemStack stack,String id) { return Math.min(255,EnchantmentHelper.getItemEnchantmentLevel(ENTRIES.get(id).get(),stack)); }
    @SubscribeEvent public static void hurt(LivingHurtEvent event) {
        if(!(event.getSource().getDirectEntity() instanceof ServerPlayer player))return;
        ItemStack weapon=player.getMainHandItem();var target=event.getEntity();
        int bane=level(weapon,"eldritch_bane"),taint=level(weapon,"tainted");
        if(bane>0 && (target instanceof EnderMan || target instanceof EldritchMob))event.setAmount(event.getAmount()+5*bane);
        if(taint>0 && !(target instanceof TaintedMob)) {
            event.setAmount(event.getAmount()+3*taint);
            if(player.getRandom().nextInt(Math.max(1,4-taint))==0)target.addEffect(new MobEffectInstance(TCEffects.FLUX_TAINT.get(),200,taint-1,true,true));
        }
    }
    @SubscribeEvent public static void death(LivingDeathEvent event) {
        if(!(event.getSource().getDirectEntity() instanceof ServerPlayer player))return;
        var target=event.getEntity();var level=player.serverLevel();var random=level.random;var weapon=player.getMainHandItem();
        var resolved=EntityAspects.resolve(target);if(resolved.isEmpty())return;
        var aspects=resolved.get().aspects();
        int elder=level(weapon,"elder_knowledge"),touch=level(weapon,"magic_touch"),vapor=level(weapon,"vaporising");
        if(elder>0 && random.nextInt(Math.max(1,7-elder))==0)for(var aspect:aspects.aspects()) {
            ThaumcraftApiHelper.addResearchPoints(player,thaumcraft.api.aspects.Aspect.wrap(aspect),(short)1);
            if(random.nextBoolean())break;
        }
        if(touch>0) {
            var primals=TCAspects.catalog().toPrimals(aspects,AspectCatalog.Reduction.SUM);
            for(int iteration=0;iteration<touch;iteration++) {
                for(var aspect:primals.aspects())if(primals.amount(aspect)>0 && random.nextBoolean())
                    level.addFreshEntity(new AspectOrbEntity(level,target.getX(),target.getY(),target.getZ(),aspect,1+random.nextInt(primals.amount(aspect))));
                if(random.nextBoolean())break;
            }
        }
        if(vapor>0 && random.nextInt(Math.max(1,5-vapor))==0)for(var aspect:aspects.aspects()) {
            if(aspects.amount(aspect)>0 && !random.nextBoolean()) {
                int count=Math.max(1,(1+random.nextInt(aspects.amount(aspect)))/2);
                while(count>0) { int batch=Math.min(64,count);target.spawnAtLocation(TCItems.ESSENCE_CRYSTAL.get().of(aspect,batch),target.getEyeHeight());count-=batch; }
            }
            if(random.nextInt(2+vapor)==0)break;
        }
    }
    @SubscribeEvent public static void expire(ItemExpireEvent event) {
        var entity=event.getEntity();if(entity.level().isClientSide)return;
        ItemStack stack=entity.getItem();
        if(!(stack.getItem() instanceof SwordItem) || level(stack,"tainted")>0)return;
        if(!(entity.level().getBlockEntity(entity.blockPosition()) instanceof AuraNodeBlockEntity node) || node.nodeType()!=AuraNodeType.TAINTED)return;
        Enchantment taint=ENTRIES.get("tainted").get();
        for(var existing:EnchantmentHelper.getEnchantments(stack).keySet())if(!existing.isCompatibleWith(taint))return;
        stack.enchant(taint,1+entity.level().random.nextInt(3));entity.setItem(stack);
        event.setExtraLife(1000);event.setCanceled(true);
    }
    private Enchantments() {}
}
