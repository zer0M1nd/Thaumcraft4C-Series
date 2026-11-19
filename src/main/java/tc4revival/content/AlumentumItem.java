package tc4revival.content;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public  class AlumentumItem extends Item implements StoredAspectItem {
    public AlumentumItem() { super(new Properties()); }
    @Override public int getBurnTime(ItemStack stack,net.minecraft.world.item.crafting.RecipeType<?> type) { return 6400; }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        level.playSound(player,player,net.minecraft.sounds.SoundEvents.ARROW_SHOOT,net.minecraft.sounds.SoundSource.PLAYERS,.3f,.4f/(level.random.nextFloat()*.4f+.8f));
        if (!level.isClientSide) level.addFreshEntity(new AlumentumEntity(level,player));
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    public static void setup(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(()->net.minecraft.world.level.block.DispenserBlock.registerBehavior(TCItems.ALUMENTUM.get(),new net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior() {
            @Override protected net.minecraft.world.entity.projectile.Projectile getProjectile(Level level,net.minecraft.core.Position position,ItemStack stack) {
                var projectile=new AlumentumEntity(TCEntities.ALUMENTUM.get(),level); projectile.setPos(position.x(),position.y(),position.z()); return projectile;
            }
            @Override protected void playSound(net.minecraft.core.BlockSource source) { source.getLevel().levelEvent(1009,source.getPos(),0); }
        }));
    }
}
