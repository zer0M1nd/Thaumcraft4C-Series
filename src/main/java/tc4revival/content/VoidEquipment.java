package tc4revival.content;

import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

public  class VoidEquipment {
    private VoidEquipment() {}
    public enum Tool {
        PICKAXE("void_pickaxe", "voidpick", "ItemPickVoid", "III", " S ", " S "),
        AXE("void_axe", "voidaxe", "ItemAxeVoid", "II", "SI", "S "),
        SHOVEL("void_shovel", "voidshovel", "ItemShovelVoid", "I", "S", "S"),
        HOE("void_hoe", "voidhoe", "ItemHoeVoid", "II", "S ", "S "),
        SWORD("void_sword", "voidsword", "ItemSwordVoid", "I", "I", "S");
        public final String path, texture, legacyName;
        public final List<String> pattern;
        Tool(String path, String texture, String legacyName, String... pattern) {
            this.path=path; this.texture=texture; this.legacyName=legacyName; this.pattern=List.of(pattern);
        }
    }
    public enum Armor {
        HELMET(ArmorItem.Type.HELMET, "void_helmet", "voidhelm", "ItemHelmetVoid", 110, 3, "III", "I I"),
        CHESTPLATE(ArmorItem.Type.CHESTPLATE, "void_chestplate", "voidchest", "ItemChestplateVoid", 160, 7, "I I", "III", "III"),
        LEGGINGS(ArmorItem.Type.LEGGINGS, "void_leggings", "voidlegs", "ItemLeggingsVoid", 150, 6, "III", "I I", "I I"),
        BOOTS(ArmorItem.Type.BOOTS, "void_boots", "voidboots", "ItemBootsVoid", 130, 3, "I I", "I I");
        public final ArmorItem.Type type;
        public final String path, texture, legacyName;
        public final int durability, defense;
        public final List<String> pattern;
        Armor(ArmorItem.Type type, String path, String texture, String legacyName, int durability, int defense, String... pattern) {
            this.type=type; this.path=path; this.texture=texture; this.legacyName=legacyName;
            this.durability=durability; this.defense=defense; this.pattern=List.of(pattern);
        }
        public static Armor from(ArmorItem.Type type) { return valueOf(type.name()); }
    }
    public static void repair(ItemStack stack, Entity holder) {
        if (holder instanceof LivingEntity living && holder.tickCount % 20 == 0 && stack.isDamaged())
            stack.hurtAndBreak(-1, living, ignored -> {});
    }
    public static void weaken(Entity target, LivingEntity attacker, int duration) {
        if (target.level().isClientSide || !(target instanceof LivingEntity living)) return;
        if (target instanceof Player && attacker instanceof Player
                && (target.getServer() == null || !target.getServer().isPvpAllowed())) return;
        living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration));
    }
}
