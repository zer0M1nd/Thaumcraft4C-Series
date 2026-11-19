package thmodern.vat;

import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** Legacy creative-only acquisition aid; also supplies the handbook's infusion result icon. */
public final class InfusionIconItem extends Item {
    private final String augmentation;
    public InfusionIconItem(String augmentation){super(new Properties());this.augmentation=augmentation;}
    private InteractionResult apply(Player player,LivingEntity target){
        if(target==null||target!=thmodern.containment.ContainmentFocusItem.target(player,1.5))return InteractionResult.PASS;
        if(player.level().isClientSide)return InteractionResult.SUCCESS;
        for(var recipe:VatInfusionRecipes.all()){
            var output=recipe.output();if(!output.has("id")||!output.get("id").getAsString().equals(augmentation)||!recipe.target(target,false))continue;
            if(CreatureInfusions.apply(target,recipe,player.getUUID())){tc4revival.network.FocusEffectPacket.burst(player.level(),target.getEyePosition(),1);return InteractionResult.CONSUME;}
        }
        return InteractionResult.PASS;
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity target,InteractionHand hand){return apply(player,target);}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){return new InteractionResultHolder<>(apply(player,thmodern.containment.ContainmentFocusItem.target(player,1.5)),player.getItemInHand(hand));}
}
