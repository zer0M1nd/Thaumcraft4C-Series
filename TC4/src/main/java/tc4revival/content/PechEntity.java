package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.syncher.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;

/** TC4's neutral pack-carrying trader, with profession independent from later equipment changes. */
public  class PechEntity extends Monster implements RangedAttackMob {
    private static final EntityDataAccessor<Integer> PROFESSION=SynchedEntityData.defineId(PechEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ANGER=SynchedEntityData.defineId(PechEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> TAMED=SynchedEntityData.defineId(PechEntity.class,EntityDataSerializers.BOOLEAN);
    final SimpleContainer pack=new SimpleContainer(9);
    Player customer;
    public float mumble;
    private Goal combat,avoid;
    private boolean refreshCombat=true;
    public PechEntity(EntityType<? extends PechEntity> type,Level level) {
        super(type,level); setDropChance(EquipmentSlot.MAINHAND,.2f);
        if (getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation nav) nav.setCanOpenDoors(true);
        setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.WATER,8);
    }
    public static AttributeSupplier.Builder attributes() { return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,30).add(Attributes.ATTACK_DAMAGE,6).add(Attributes.MOVEMENT_SPEED,.5).add(Attributes.ARMOR,2); }
    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(PROFESSION,0); entityData.define(ANGER,0); entityData.define(TAMED,false); }
    public int profession() { return entityData.get(PROFESSION); }
    public void profession(int value) { entityData.set(PROFESSION,Math.max(0,Math.min(2,value))); }
    public boolean friendly() { return entityData.get(TAMED); }
    public void friendly(boolean value) { entityData.set(TAMED,value); refreshCombat=true; }
    public int anger() { return entityData.get(ANGER); }
    @Override protected Component getTypeName() { return Component.translatable("entity.thaumcraft.pech"+(profession()==0 ? "" : "."+profession())); }
    @Override protected float getStandingEyeHeight(Pose pose,EntityDimensions size) { return size.height*.66f; }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(1,new Goal() {
            { setFlags(java.util.EnumSet.of(Flag.MOVE,Flag.JUMP)); }
            public boolean canUse() { return customer!=null && friendly() && onGround() && !isInWater() && !hurtMarked; }
            public void start() { getNavigation().stop(); }
            public void stop() { customer=null; }
        });
        goalSelector.addGoal(3,new PechPickupGoal(this));
        goalSelector.addGoal(5,new OpenDoorGoal(this,true));
        goalSelector.addGoal(6,new MoveTowardsRestrictionGoal(this,.5));
        goalSelector.addGoal(6,new MoveThroughVillageGoal(this,1,false,4,()->true));
        goalSelector.addGoal(9,new RandomStrollGoal(this,.6));
        goalSelector.addGoal(9,new LookAtPlayerGoal(this,Player.class,3,1));
        goalSelector.addGoal(10,new LookAtPlayerGoal(this,Mob.class,8));
        goalSelector.addGoal(11,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
    }
    @Override public void setItemSlot(EquipmentSlot slot,ItemStack stack) { super.setItemSlot(slot,stack); if (slot==EquipmentSlot.MAINHAND) refreshCombat=true; }
    private void refreshCombat() {
        if (combat!=null) goalSelector.removeGoal(combat);
        if (avoid!=null) goalSelector.removeGoal(avoid);
        combat=getMainHandItem().is(Items.BOW) ? new RangedAttackGoal(this,.6,20,50,15)
                : getMainHandItem().is(TCItems.CASTING_WAND.get()) ? new RangedAttackGoal(this,.6,20,30,15) : new MeleeAttackGoal(this,.6,false);
        goalSelector.addGoal(2,combat);
        avoid=new AvoidEntityGoal<>(this,Player.class,8,.5,.6);
        if (!friendly()) goalSelector.addGoal(4,avoid);
        refreshCombat=false;
    }
    @Override protected void customServerAiStep() { if (refreshCombat) refreshCombat(); super.customServerAiStep(); if (tickCount%40==0) heal(1); }
    @Override public void tick() {
        mumble*=.75f;
        if (!level().isClientSide && anger()>0) {
            entityData.set(ANGER,anger()-1);
            if (getTarget()==null) { var target=level().getNearestPlayer(this,16); if (target!=null && !target.isCreative() && !target.isSpectator()) setTarget(target); }
        }
        if (level().isClientSide && (anger()>0 && random.nextInt(15)==0 || friendly() && random.nextInt(25)==0)) particles(anger()>0,1);
        super.tick();
    }
    private void angry(Player player) {
        if (anger()==0) { level().broadcastEntityEvent(this,(byte)19); playSound(TCSounds.PECH_CHARGE.get(),.4f,getVoicePitch()); }
        setTarget(player); entityData.set(ANGER,400+random.nextInt(400)); friendly(false);
    }
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount) {
        if (isInvulnerableTo(source)) return false;
        if (!level().isClientSide && source.getEntity() instanceof Player player) {
            for (var pech:level().getEntitiesOfClass(PechEntity.class,getBoundingBox().inflate(32,16,32))) pech.angry(player);
            angry(player);
        }
        return super.hurt(source,amount);
    }
    public static int fixedValue(ItemStack stack) {
        var custom = thaumcraft.api.entities.PechTradeValues.find(stack);
        if (custom.isPresent()) return custom.getAsInt();
        if (stack.is(TCItems.MANA_BEAN.get())) return 1;
        if (stack.is(Items.GOLD_INGOT) || stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE)) return 2;
        if (stack.is(Items.ENDER_PEARL)) return 3; if (stack.is(Items.DIAMOND)) return 4; if (stack.is(Items.EMERALD)) return 5; return 0;
    }
    public static int value(ItemStack stack) {
        if (stack.isEmpty()) return 0; int fixed=fixedValue(stack); if (fixed>0) return fixed;
        return tc4revival.aspect.item.ItemStackAspects.resolve(stack).map(a->Math.min(32,a.amount(tc4revival.aspect.TCAspects.catalog().require("lucrum")))).orElse(0);
    }
    public boolean canPickup(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!friendly() && fixedValue(stack)>0) return true;
        for (int i=0;i<9;i++) { var slot=pack.getItem(i); if (slot.isEmpty() || ItemStack.isSameItemSameTags(slot,stack) && slot.getCount()+stack.getCount()<=slot.getMaxStackSize()) return true; }
        return false;
    }
    public ItemStack pickup(ItemStack source) {
        var remaining=source.copy();
        if (!friendly() && value(remaining)>0) {
            if (random.nextInt(10)<value(remaining)) { friendly(true); level().broadcastEntityEvent(this,(byte)18); }
            remaining.shrink(1); return remaining;
        }
        for (int i=0;i<9 && !remaining.isEmpty();i++) {
            var slot=pack.getItem(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameTags(slot,remaining)) { int amount=Math.min(remaining.getCount(),slot.getMaxStackSize()-slot.getCount()); slot.grow(amount); remaining.shrink(amount); }
        }
        for (int i=0;i<9 && !remaining.isEmpty();i++) if (pack.getItem(i).isEmpty()) { pack.setItem(i,remaining.copy()); remaining=ItemStack.EMPTY; }
        pack.setChanged(); return remaining;
    }
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand) {
        if (player.isShiftKeyDown() || player.getItemInHand(hand).is(Items.NAME_TAG)) return super.mobInteract(player,hand);
        if (friendly()) {
            if (!level().isClientSide && customer==null && player instanceof net.minecraft.server.level.ServerPlayer server) {
                customer=player;
                net.minecraftforge.network.NetworkHooks.openScreen(server,new SimpleMenuProvider((id,inv,p)->new PechMenu(id,inv,this),getDisplayName()),buffer->buffer.writeInt(getId()));
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player,hand);
    }
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,net.minecraft.world.DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData group,CompoundTag tag) {
        var result=super.finalizeSpawn(level,difficulty,reason,group,tag); int roll=random.nextInt(20); Item item=switch(roll) {
            case 1->Items.STONE_SWORD; case 3->Items.STONE_AXE; case 5->Items.IRON_SWORD; case 6->Items.IRON_AXE; case 7->Items.FISHING_ROD; case 8->Items.STONE_PICKAXE; case 9->Items.IRON_PICKAXE;
            case 2,4,10,11,13->Items.BOW; default->Items.AIR;
        };
        var stack=new ItemStack(item);
        if (roll==0 || roll==12) {
            stack=TCItems.CASTING_WAND.get().create(WandParts.Rod.WOOD,WandParts.Cap.IRON,false,false);
            WandFocusItem.attach(stack,new ItemStack(TCItems.PECH_FOCUS.get()));
            for (var aspect:tc4revival.aspect.TCAspects.catalog().primals()) TCItems.CASTING_WAND.get().addVis(stack,aspect,
                    java.util.Set.of("terra","perditio","aqua").contains(aspect.tag()) ? 2+random.nextInt(6) : random.nextInt(4),true,true);
            profession(1); setDropChance(EquipmentSlot.MAINHAND,.1f);
        } else if (item==Items.BOW) profession(2);
        setItemSlot(EquipmentSlot.MAINHAND,stack);
        if (profession()!=1) populateDefaultEquipmentEnchantments(random,difficulty);
        setCanPickUpLoot(random.nextFloat()<.75f*difficulty.getSpecialMultiplier());
        return result;
    }
    @Override public void performRangedAttack(LivingEntity target,float distance) {
        if (profession()==1) {
            var blast=new PechBlastEntity(level(),this,1,0,random.nextFloat()<.1f);
            double x=target.getX()+target.getDeltaMovement().x-getX(),z=target.getZ()+target.getDeltaMovement().z-getZ();
            blast.shoot(x,target.getEyeY()-1.500000023841858-getY()+Math.sqrt(x*x+z*z)*.1,z,1.5f,4);
            level().addFreshEntity(blast); playSound(TCSounds.ICE.get(),.4f,1+random.nextFloat()*.1f);
        } else if (profession()==2) {
            var arrow=new net.minecraft.world.entity.projectile.Arrow(level(),this);
            double x=target.getX()-getX(),z=target.getZ()-getZ();
            arrow.shoot(x,target.getY(.333333333)-arrow.getY()+Math.sqrt(x*x+z*z)*.2,z,1.6f,14-level().getDifficulty().getId()*4);
            int power=net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.POWER_ARROWS,getMainHandItem());
            arrow.setBaseDamage(distance*2+random.nextGaussian()*.25+level().getDifficulty().getId()*.11+(power>0 ? power*.5+.5 : 0));
            arrow.setKnockback(net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.PUNCH_ARROWS,getMainHandItem()));
            level().addFreshEntity(arrow); playSound(net.minecraft.sounds.SoundEvents.ARROW_SHOOT,1,1/(random.nextFloat()*.4f+.8f));
        }
        swing(InteractionHand.MAIN_HAND);
    }
    @Override public boolean removeWhenFarAway(double distance) { int occupied=0; for (int i=0;i<9;i++) if (!pack.getItem(i).isEmpty()) occupied++; return occupied<5; }
    @Override public int getArmorValue() { return Math.min(20,super.getArmorValue()); }
    @Override public boolean canBeLeashed(Player player) { return false; }
    @Override protected void dropCustomDeathLoot(net.minecraft.world.damagesource.DamageSource source,int looting,boolean hit) { }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putInt("PechType",profession()); tag.putInt("Anger",anger()); tag.putBoolean("Tamed",friendly()); var loot=new ListTag(); for (int i=0;i<9;i++) loot.add(pack.getItem(i).save(new CompoundTag())); tag.put("Loot",loot); }
    @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); profession(tag.getInt("PechType")); entityData.set(ANGER,Math.max(0,tag.getInt("Anger"))); friendly(tag.getBoolean("Tamed")); var loot=tag.getList("Loot",Tag.TAG_COMPOUND); for (int i=0;i<9;i++) pack.setItem(i,ItemStack.of(loot.getCompound(i))); customer=null; }
    @Override protected SoundEvent getAmbientSound() { return TCSounds.PECH_IDLE.get(); }
    @Override protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) { return TCSounds.PECH_HIT.get(); }
    @Override protected SoundEvent getDeathSound() { return TCSounds.PECH_DEATH.get(); }
    @Override protected float getSoundVolume() { return .4f; }
    @Override public int getAmbientSoundInterval() { return 120; }
    @Override public void playAmbientSound() {
        if (random.nextInt(3)==0 && level().getEntitiesOfClass(PechEntity.class,getBoundingBox().inflate(4,2,4),p->p!=this).size()>0) {
            level().broadcastEntityEvent(this,(byte)17); playSound(TCSounds.PECH_TRADE.get(),.4f,getVoicePitch());
        } else { level().broadcastEntityEvent(this,(byte)16); super.playAmbientSound(); }
    }
    private void particles(boolean angry,int count) { for (int i=0;i<count;i++) level().addParticle(angry ? net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER : net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,getRandomX(1),getY()+.5+random.nextFloat()*getBbHeight(),getRandomZ(1),random.nextGaussian()*.02,random.nextGaussian()*.02,random.nextGaussian()*.02); }
    @Override public void handleEntityEvent(byte event) { switch(event) { case 16->mumble=(float)Math.PI; case 17->mumble=(float)Math.PI*2; case 18->particles(false,5); case 19->{ particles(true,5); mumble=(float)Math.PI*2; } default->super.handleEntityEvent(event); } }
}
