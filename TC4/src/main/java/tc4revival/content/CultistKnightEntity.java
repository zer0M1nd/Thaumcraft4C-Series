package tc4revival.content;

import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.util.RandomSource;

public  class CultistKnightEntity extends CultistEntity {
    public CultistKnightEntity(EntityType<? extends CultistKnightEntity> type,Level level) { super(type,level); }
    public static AttributeSupplier.Builder attributes() { return cultistAttributes().add(Attributes.MAX_HEALTH,36); }
    @Override protected void populateDefaultEquipmentSlots(RandomSource random,DifficultyInstance difficulty) {
        for (var armor:new CultistEquipment.Armor[]{CultistEquipment.Armor.PLATE_HELMET,CultistEquipment.Armor.PLATE_CHEST,CultistEquipment.Armor.PLATE_LEGS,CultistEquipment.Armor.BOOTS})
            setItemSlot(armor.slot.getSlot(),new ItemStack(TCEldritch.armor(armor)));
        if (random.nextFloat()<(level().getDifficulty()==Difficulty.HARD ? .05f : .01f)) {
            if (random.nextInt(5)==0) {
                setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(TCItems.voidTool(VoidEquipment.Tool.SWORD)));
                setItemSlot(EquipmentSlot.HEAD,new ItemStack(TCEldritch.armor(CultistEquipment.Armor.ROBE_HELMET)));
            } else {
                setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(TCItems.tool(ThaumiumToolType.SWORD)));
                if (random.nextBoolean()) setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);
            }
        } else setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));
    }
    @Override protected void populateDefaultEquipmentEnchantments(RandomSource random,DifficultyInstance difficulty) {
        float factor=difficulty.getSpecialMultiplier();
        if (!getMainHandItem().isEmpty() && random.nextFloat()<.25f*factor)
            setItemSlot(EquipmentSlot.MAINHAND,EnchantmentHelper.enchantItem(random,getMainHandItem(),(int)(5+factor*random.nextInt(18)),false));
    }
}
