package thaumicomniverse.mixin;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu;
import thaumicomniverse.compat.ApotheosisEnchantments;

@Mixin(value=ApothEnchantmentMenu.class,remap=false)
public abstract class ApotheosisMenuMixin {
    @Unique private boolean omniverse$treasureShelf;
    @Redirect(method="*",at=@At(value="INVOKE",target="Ldev/shadowsoffire/apotheosis/ench/table/ApothEnchantmentMenu;gatherStats(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;I)Ldev/shadowsoffire/apotheosis/ench/table/ApothEnchantmentMenu$TableStats;"),remap=false)
    private ApothEnchantmentMenu.TableStats omniverse$readShelf(Level level,BlockPos pos,int value) {
        omniverse$treasureShelf=ApotheosisEnchantments.hasShelf(level,pos);
        return ApothEnchantmentMenu.gatherStats(level,pos,value);
    }
    @Group(name="tableEligibility",min=1,max=1)
    @Redirect(method="*",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/Item;isEnchantable(Lnet/minecraft/world/item/ItemStack;)Z"),require=0,remap=false)
    private boolean omniverse$named(Item item,ItemStack stack) { return eligible(item,stack); }
    @Group(name="tableEligibility",min=1,max=1)
    @Redirect(method="*",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/Item;m_8120_(Lnet/minecraft/world/item/ItemStack;)Z"),require=0,remap=false)
    private boolean omniverse$srg(Item item,ItemStack stack) { return eligible(item,stack); }
    @Unique private boolean eligible(Item item,ItemStack stack) {
        return item.isEnchantable(stack) || omniverse$treasureShelf && stack.isDamageableItem() || ApotheosisEnchantments.projectEligible(stack);
    }
    @Redirect(method={"getEnchantmentList","m_39471_"},at=@At(value="INVOKE",target="Ldev/shadowsoffire/apotheosis/ench/table/RealEnchantmentHelper;selectEnchantment(Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/item/ItemStack;IFFFZLjava/util/Set;)Ljava/util/List;"),remap=false)
    private List<EnchantmentInstance> omniverse$select(RandomSource random,ItemStack stack,int power,float quanta,float arcana,float rectification,boolean treasure,Set<Enchantment> blacklist) {
        return ApotheosisEnchantments.select(omniverse$treasureShelf,random,stack,power,quanta,arcana,rectification,treasure,blacklist);
    }
}
