package tc4revival.content;

import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.TCAspects;

/** Aspect-bearing seed and food. Additional legacy consumption effects are wired separately. */
public  class ManaBeanItem extends Item implements StoredAspectItem {
    private final java.util.Random initializationRandom=new java.util.Random();
    public ManaBeanItem() { super(new Properties().food(new FoodProperties.Builder().nutrition(1).saturationMod(.5f).meat().alwaysEat().build())); }
    public static ItemStack of(Aspect aspect) {
        var stack=new ItemStack(TCItems.MANA_BEAN.get());
        TCItems.MANA_BEAN.get().setStoredAspects(stack,new AspectAmounts().add(aspect,1)); return stack;
    }
    @Override public int getUseDuration(ItemStack stack) { return 10; }
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,net.minecraft.world.entity.LivingEntity living) {
        var stored=storedAspects(stack);
        var result=super.finishUsingItem(stack,level,living);
        if (!level.isClientSide && living instanceof net.minecraft.server.level.ServerPlayer player) {
            var effect=ManaBeanEffects.at(level.random.nextInt(ManaBeanEffects.SLOTS));
            if (effect!=null) {
                if (effect.isInstantenous()) effect.applyInstantenousEffect(player,player,player,2,3);
                else player.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect,160+level.random.nextInt(80),0));
            }
            if (level.random.nextFloat()<.25f && !stored.isEmpty()) {
                var aspect=stored.aspects().get(0);
                var reward=tc4revival.research.KnowledgeSavedData.get(player.getServer()).update(player.getUUID(),level.random::nextInt,knowledge->{
                    int before=knowledge.poolAmount(aspect);
                    knowledge.changePool(aspect,1);
                    return new tc4revival.research.ScanRules.Reward(aspect,knowledge.poolAmount(aspect)-before,knowledge.pool(aspect),false);
                });
                tc4revival.network.TCNetwork.send(player,tc4revival.network.ScanFeedbackPacket.from(new tc4revival.research.ScanRules.Result(
                        tc4revival.research.ScanRules.Status.SUCCESS,java.util.Optional.empty(),java.util.List.of(reward))));
            }
        }
        return result;
    }
    private void initialize(ItemStack stack) {
        if (!storedAspects(stack).isEmpty()) return;
        var aspects=TCAspects.catalog().all();
        setStoredAspects(stack,new AspectAmounts().add(aspects.get(initializationRandom.nextInt(aspects.size())),1));
    }
    @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected) {
        if (!level.isClientSide) initialize(stack);
    }
    @Override public void onCraftedBy(ItemStack stack,Level level,Player player) { if (!level.isClientSide) initialize(stack); }
    public int tint(ItemStack stack) {
        var stored=storedAspects(stack);
        if (!stored.isEmpty()) return stored.aspects().get(0).color();
        var aspects=TCAspects.catalog().all();
        return aspects.get((int)(System.currentTimeMillis()/500%aspects.size())).color();
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player=context.getPlayer(); var level=context.getLevel(); var clicked=context.getClickedPos(); var stack=context.getItemInHand();
        if (player==null || context.getClickedFace()!=Direction.DOWN || !level.mayInteract(player,clicked)
                || !player.mayUseItemAt(clicked,Direction.DOWN,stack)) return InteractionResult.PASS;
        var pos=clicked.below(); var state=TCArcane.MANA_POD.get().defaultBlockState();
        if (!LegacyBiomeProfiles.get(level.getBiome(clicked)).magical() || !state.canSurvive(level,pos)) return InteractionResult.PASS;
        // An occupied hanging position still counts as handled in the old item callback.
        if (!level.isEmptyBlock(pos)) return InteractionResult.sidedSuccess(level.isClientSide);
        if (!level.isClientSide && !level.isOutsideBuildHeight(pos) && level.setBlock(pos,state,2)) {
            var aspects=storedAspects(stack);
            if (!aspects.isEmpty() && level.getBlockEntity(pos) instanceof ManaPodBlockEntity pod) pod.setAspect(aspects.aspects().get(0));
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
