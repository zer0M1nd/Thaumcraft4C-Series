package tmmodern;
import net.minecraft.world.*;import net.minecraft.world.item.*;import net.minecraft.world.entity.*;import net.minecraft.world.entity.player.Player;import net.minecraft.world.level.Level;import net.minecraft.world.effect.*;
public class TMTools {
    public static class Pick extends PickaxeItem implements tc4revival.content.RepairableItem {public Pick(){super(SHADOW,1,-2.8f,new Properties().rarity(Rarity.UNCOMMON));}}
    public static class Axe extends AxeItem implements tc4revival.content.RepairableItem {public Axe(){super(SHADOW,5,-3,new Properties().rarity(Rarity.UNCOMMON));}}
    public static class Spade extends ShovelItem implements tc4revival.content.RepairableItem {public Spade(){super(SHADOW,1.5f,-3,new Properties().rarity(Rarity.UNCOMMON));}}
    public static class Hoe extends HoeItem implements tc4revival.content.RepairableItem {public Hoe(){super(SHADOW,-2,-1,new Properties().rarity(Rarity.UNCOMMON));}@Override public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext c){var l=c.getLevel();var p=c.getPlayer();var pos=c.getClickedPos();if(p==null||!l.mayInteract(p,pos))return InteractionResult.FAIL;var state=l.getBlockState(pos);boolean farm=state.is(net.minecraft.world.level.block.Blocks.FARMLAND);if(!farm&&!state.is(net.minecraft.world.level.block.Blocks.DIRT)&&!state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK))return InteractionResult.PASS;if(!l.isClientSide){l.setBlockAndUpdate(pos,(farm?net.minecraft.world.level.block.Blocks.DIRT:net.minecraft.world.level.block.Blocks.FARMLAND).defaultBlockState());c.getItemInHand().hurtAndBreak(1,p,e->e.broadcastBreakEvent(c.getHand()));}p.swing(c.getHand());return InteractionResult.SUCCESS;}}
    public static final Tier SHADOW=tier(3,2500,17,6,30),PRIMAL=tier(3,500,16,16.75f,30),HOLLOW=tier(1,200,4,-.5f,5);
    public static Tier tier(int level,int uses,float speed,float damage,int enchant){return new Tier(){public int getUses(){return uses;}public float getSpeed(){return speed;}public float getAttackDamageBonus(){return damage;}public int getLevel(){return level;}public int getEnchantmentValue(){return enchant;}public net.minecraft.world.item.crafting.Ingredient getRepairIngredient(){return net.minecraft.world.item.crafting.Ingredient.of(TaintedMagic.item("shadowmetal_ingot"));}};}
    public static class Blade extends SwordItem implements thaumcraft.api.IWarpingGear,tc4revival.content.RepairableItem {
        public final boolean primal;public Blade(boolean primal){super(primal?PRIMAL:SHADOW,3,-2.4f,new Item.Properties().rarity(primal?Rarity.EPIC:Rarity.UNCOMMON));this.primal=primal;}
        @Override public int getWarp(ItemStack s,Player p){return primal?5:0;}
        @Override public boolean hurtEnemy(ItemStack s,LivingEntity target,LivingEntity owner){if(primal){target.addEffect(new MobEffectInstance(MobEffects.WITHER,60,1));target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,120,1));}return super.hurtEnemy(s,target,owner);}
        @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand hand){if(primal){p.startUsingItem(hand);return InteractionResultHolder.consume(p.getItemInHand(hand));}return super.use(l,p,hand);}
        @Override public UseAnim getUseAnimation(ItemStack s){return UseAnim.NONE;}@Override public int getUseDuration(ItemStack s){return primal?72000:0;}
        @Override public void onUseTick(Level l,LivingEntity owner,ItemStack s,int remaining){if(!primal||l.isClientSide)return;for(var e:l.getEntities(owner,owner.getBoundingBox().inflate(15))){var vector=owner.position().add(0,owner.getBbHeight()/2,0).subtract(e.position().add(0,e.getBbHeight()/2,0));double distance=vector.length();if(distance<.00001)continue;double f=Math.max(0,1-distance/20);f*=f;var motion=vector.scale(f/distance);e.push(motion.x*.2,motion.y*.3,motion.z*.2);if(distance<2)e.hurt(owner.damageSources().magic(),3);e.hurtMarked=true;}}
        @Override public void inventoryTick(ItemStack s,Level l,Entity e,int slot,boolean held){if(primal&&!l.isClientSide&&e.tickCount%20==0&&s.isDamaged())s.setDamageValue(s.getDamageValue()-1);}
    }
    public static class HollowDagger extends SwordItem implements tc4revival.content.RepairableItem {
        public HollowDagger(){super(HOLLOW,3,-2.4f,new Properties());}
        @Override public boolean hurtEnemy(ItemStack s,LivingEntity target,LivingEntity owner){
            if(owner instanceof Player p&&!p.level().isClientSide){
                // The legacy ItemEssence damage-zero bottle is the modern empty_phial.
                // Looking up the removed "phial" ID resolves to AIR and matches empty slots.
                for(int i=0;i<p.getInventory().getContainerSize();i++){
                    var slot=p.getInventory().getItem(i);
                    if(!slot.isEmpty()&&slot.is(tc4revival.content.TCItems.EMPTY_PHIAL.get())){
                        slot.shrink(1);
                        var result=new ItemStack(TaintedMagic.item("crimson_blood"));
                        if(!p.getInventory().add(result))p.drop(result,false);
                    }
                }
            }
            return super.hurtEnemy(s,target,owner);
        }
    }
}
