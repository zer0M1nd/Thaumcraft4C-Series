package tc4revival.content;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "thaumcraft")
public  class TravellerBootsItem extends ArmorItem implements RepairableItem, RunicGear {
    private static final Map<Player, Float> PREVIOUS_STEP = new WeakHashMap<>();
    public TravellerBootsItem() { super(SpecialArmorMaterial.INSTANCE, Type.BOOTS, new Properties().durability(350)); }
    @Override public int getMaxDamage(ItemStack stack) { return 350; }
    @Override public Rarity getRarity(ItemStack stack) { return Rarity.RARE; }
    @Override public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String layer) { return "thaumcraft:textures/models/bootstraveler.png"; }
    @Override public void onArmorTick(ItemStack stack, Level level, Player player) {
        if (!player.getAbilities().flying && player.zza > 0) {
            if (level.isClientSide && !player.isShiftKeyDown()) {
                PREVIOUS_STEP.putIfAbsent(player, player.maxUpStep()); player.setMaxUpStep(1);
            }
            if (player.onGround()) player.moveRelative(player.isInWater() ? .055f / 4 : .055f, new Vec3(0, 0, 1));
        }
        if (player.fallDistance > 0) player.fallDistance -= .25f;
    }
    /** Replaces the removed jumpMovementFactor field at the actual modern air-acceleration accessor. */
    public static float airSpeed(float vanilla, Player player) {
        var boots=player.getItemBySlot(EquipmentSlot.FEET);
        boolean hovering=HoverFlight.active(player);
        float current=boots.getItem() instanceof TravellerBootsItem && !player.getAbilities().flying && player.zza>0 && !player.onGround()
                ? hovering ? .03f : .05f : vanilla;
        return thaumcraft.api.equipment.BootsBehaviorProviders.airSpeed(
            new thaumcraft.api.equipment.BootsBehaviorProviders.MovementContext(player,boots,player.getAbilities().flying,hovering,player.zza>0,player.onGround(),vanilla),current);
    }
    @Override public boolean isValidRepairItem(ItemStack boots,ItemStack material){
        return switch(thaumcraft.api.equipment.BootsBehaviorProviders.repair(boots,material)){
            case ACCEPT -> true; case REJECT -> false; case PASS -> super.isValidRepairItem(boots,material);
        };
    }
    @SubscribeEvent public static void restoreStep(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player) || !player.level().isClientSide) return;
        if (player.isShiftKeyDown() || !(player.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof TravellerBootsItem)) {
            var step = PREVIOUS_STEP.remove(player); if (step != null) player.setMaxUpStep(step);
        }
    }
    @SubscribeEvent public static void jump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof Player player && player.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof TravellerBootsItem)
            player.setDeltaMovement(player.getDeltaMovement().add(0,(double).275f,0));
    }
}
