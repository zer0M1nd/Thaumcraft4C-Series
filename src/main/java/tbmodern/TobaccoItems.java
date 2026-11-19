package tbmodern;

import java.util.ArrayList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import tbmodern.api.Tobacco;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import tc4revival.content.TCEffects;

public final class TobaccoItems {
    public static final class Dose extends Item implements Tobacco {
        private final String kind;
        public Dose(String kind) { super(new Properties()); this.kind = kind; }
        @Override public void smoke(ServerPlayer player, ItemStack dose, ItemStack pipe, boolean silverwood) {
            var random = player.getRandom();
            switch(kind) {
                case "pile" -> { if(random.nextFloat() <= (silverwood ? .3F : .1F)) temporaryWarp(player,-1); }
                case "eldritch" -> { effect(player,TCEffects.DEATH_GAZE.get(),2000,0); if(!silverwood)temporaryWarp(player,3); }
                case "fighting" -> { effect(player,MobEffects.DAMAGE_BOOST,8000,1); if(silverwood && random.nextFloat() <= .1F)effect(player,MobEffects.DAMAGE_RESISTANCE,8000,0); }
                case "hunger" -> { player.getFoodData().eat(3,3); if(!silverwood && random.nextFloat() <= .4F)effect(player,MobEffects.CONFUSION,200,0); }
                case "knowledge" -> {
                    var aspects = new ArrayList<>(tc4revival.aspect.TCAspects.catalog().all());
                    for(int i=0;i<(silverwood?20:10);i++) {
                        Aspect aspect = Aspect.wrap(aspects.get(random.nextInt(aspects.size())));
                        ThaumcraftApiHelper.addResearchPoints(player,aspect,(short)(silverwood?2:1));
                        if(aspect == Aspect.TAINT && !silverwood)temporaryWarp(player,1);
                    }
                }
                case "mining" -> { effect(player,MobEffects.DIG_SPEED,8000,1); if(silverwood && random.nextFloat() <= .3F)effect(player,MobEffects.NIGHT_VISION,8000,0); }
                case "sanity" -> {
                    if(random.nextFloat() <= (silverwood?.6F:.4F))temporaryWarp(player,-1);
                    if(silverwood && random.nextFloat() <= .1F)ThaumcraftApiHelper.addStickyWarpToPlayer(player,-1);
                }
                case "tainted" -> {
                    if(silverwood) { player.broadcastBreakEvent(player.getUsedItemHand()); pipe.shrink(1); }
                    else { temporaryWarp(player,1+random.nextInt(3)); if(random.nextFloat() <= .4F)ThaumcraftApiHelper.addStickyWarpToPlayer(player,1); }
                }
                case "wispy" -> {
                    var aspects = new ArrayList<>(tc4revival.aspect.TCAspects.catalog().all());
                    if(silverwood)aspects.remove(Aspect.TAINT.unwrap());
                    var wisp = tc4revival.content.TCEntities.WISP.get().create(player.level());
                    if(wisp != null && !aspects.isEmpty()) {
                        wisp.setAspectTag(aspects.get(random.nextInt(aspects.size())).tag());
                        wisp.moveTo(player.getX(),player.getY(),player.getZ(),0,0);player.level().addFreshEntity(wisp);
                    }
                }
                default -> throw new IllegalStateException("Unknown tobacco: "+kind);
            }
        }
    }
    private static void effect(Player player,MobEffect effect,int ticks,int amplifier) { player.addEffect(new MobEffectInstance(effect,ticks,amplifier,true,true)); }
    private static void temporaryWarp(ServerPlayer player,int amount) { ThaumcraftApiHelper.addWarpToPlayer(player,amount,true); }
    public static final class Pipe extends Item {
        private final boolean silverwood;
        public Pipe(boolean silverwood) { super(new Properties().stacksTo(1)); this.silverwood=silverwood; }
        private ItemStack find(Player player) {
            for(int slot=0;slot<player.getInventory().getContainerSize();slot++) {
                var stack=player.getInventory().getItem(slot);
                if(!stack.isEmpty() && stack.getItem() instanceof Tobacco)return stack;
            }
            return ItemStack.EMPTY;
        }
        @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
            if(find(player).isEmpty())return InteractionResultHolder.fail(player.getItemInHand(hand));
            player.startUsingItem(hand);return InteractionResultHolder.consume(player.getItemInHand(hand));
        }
        @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }
        @Override public int getUseDuration(ItemStack stack) { return 64; }
        @Override public void onUseTick(Level level,LivingEntity entity,ItemStack stack,int remaining) {
            if(level.isClientSide && remaining<32) {
                var position=entity.getEyePosition().add(entity.getLookAngle().scale(.2));var motion=entity.getLookAngle().scale(.1);
                level.addParticle(silverwood?ParticleTypes.POOF:ParticleTypes.SMOKE,position.x,position.y,position.z,motion.x,motion.y,motion.z);
            }
        }
        @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity) {
            if(entity instanceof ServerPlayer player) {
                ItemStack dose=find(player);
                if(!dose.isEmpty() && dose.getItem() instanceof Tobacco tobacco) {
                    tobacco.smoke(player,dose,stack,silverwood);dose.shrink(1);
                    var position=entity.getEyePosition().add(entity.getLookAngle().scale(.2));
                    player.serverLevel().sendParticles(silverwood?ParticleTypes.POOF:ParticleTypes.SMOKE,position.x,position.y,position.z,100,.03,.03,.03,.03);
                }
            }
            return stack;
        }
    }
    public static final class Mortar extends Item {
        public Mortar() { super(new Properties().durability(256)); }
        @Override public boolean hasCraftingRemainingItem(ItemStack stack) { return stack.getDamageValue()<stack.getMaxDamage(); }
        @Override public ItemStack getCraftingRemainingItem(ItemStack stack) {
            if(!hasCraftingRemainingItem(stack))return ItemStack.EMPTY;
            var result=stack.copy();result.setCount(1);result.setDamageValue(stack.getDamageValue()+1);return result;
        }
    }
    private TobaccoItems() {}
}
