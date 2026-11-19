package tc4revival.content;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectNbt;
import tc4revival.aspect.TCAspects;
import tc4revival.research.KnowledgeSavedData;

public  class InfusionMatrixBlockEntity extends BlockEntity implements WandInteractable,RevealedAspectContainer,InfusionProcess.Host {
    public record Source(BlockPos position,int entityId,int color,boolean essentia,long expires) {}
    private final InfusionProcess process=new InfusionProcess();
    private InfusionRepeat pendingRepeat,lastRepeat;
    private ItemStack activationWand=ItemStack.EMPTY;
    private net.minecraft.world.InteractionHand activationHand=net.minecraft.world.InteractionHand.MAIN_HAND;
    private boolean committing;
    private thaumcraft.api.wands.MagicOperationRules.Context operation(InfusionOutput output,ItemStack result,Player player,boolean simulate){
        return new thaumcraft.api.wands.MagicOperationRules.Context(thaumcraft.api.wands.MagicOperationRules.Kind.INFUSION,
            output.recipeId(),player,level,worldPosition,activationHand,activationWand,result,output.research(),java.util.Map.of(),simulate);
    }
    public boolean repeat(ServerPlayer player) {
        return tc4revival.config.TCConfig.SERVER.revival.repeatInfusion.get()&&lastRepeat!=null&&lastRepeat.place(this,player);
    }
    public boolean canRepeat(ServerPlayer player) {
        return tc4revival.config.TCConfig.SERVER.revival.repeatInfusion.get()&&lastRepeat!=null&&lastRepeat.canPlace(this,player);
    }
    private InfusionLayout previewLayout;
    private long previewTick=Long.MIN_VALUE;
    private long matchTick=Long.MIN_VALUE;
    private ItemStack previewCenter=ItemStack.EMPTY;
    private List<ItemStack> previewComponents=List.of();
    private boolean previewOrdered;
    private Optional<InfusionPreview.Match> previewMatch=Optional.empty();
    private InfusionLayout layout=new InfusionLayout(List.of(),0);
    private boolean active,checkSurroundings=true,clientCrafting;
    private int ticks,clientInstability;
    private thaumcraft.api.crafting.InfusionDeviceModifiers.Stats deviceStats=thaumcraft.api.crafting.InfusionDeviceModifiers.Stats.NONE;
    private AspectAmounts clientAspects=new AspectAmounts();
    private final List<Source> sources=new ArrayList<>();
    public float startUp;
    public int craftCount;
    public InfusionMatrixBlockEntity(BlockPos pos,BlockState state) { this(TCArcane.INFUSION_MATRIX_ENTITY.get(),pos,state); }
    protected InfusionMatrixBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type,BlockPos pos,BlockState state){super(type,pos,state);}
    /** Overridable device bonuses; the saved process keeps its committed cost after reload. */
    protected InfusionRecipe.Plan adjustPlan(InfusionRecipe.Plan plan,InfusionLayout layout){
        deviceStats=thaumcraft.api.crafting.InfusionDeviceModifiers.evaluate(level,worldPosition,layout);
        var aspects=new AspectAmounts();var original=plan.aspects();
        for(var aspect:original.aspects())aspects.add(aspect,(int)Math.min(Integer.MAX_VALUE,Math.ceil(original.amount(aspect)*deviceStats.costMultiplier())));
        return new InfusionRecipe.Plan(plan.components(),aspects,plan.instability(),plan.xp(),plan.enchanting(),plan.output());
    }
    protected int adjustSymmetry(int symmetry){return Math.addExact(symmetry,deviceStats.instability());}
    protected int cycleDelay(int delay){return Math.max(1,(int)Math.ceil(delay*deviceStats.cycleMultiplier()));}
    public boolean active() { return active; }
    public record Preview(InfusionRisk risk,AspectAmounts required,int environment,List<InfusionPreview.Item> items) {
        public Preview(InfusionRisk risk,AspectAmounts required) { this(risk,required,risk==null?0:risk.environment(),List.of()); }
        public Preview { items=List.copyOf(items); }
    }
    public Optional<InfusionRisk> riskFor(ServerPlayer player) {
        return previewFor(player).map(Preview::risk);
    }
    public Optional<Preview> previewFor(ServerPlayer player) {
        if(level==null || level.isClientSide || player.level()!=level || !validLocation())return Optional.empty();
        // Shared between observers; scan only while requested, at most once each 20 ticks.
        if(previewLayout==null || level.getGameTime()-previewTick>=20) {
            previewLayout=InfusionLayout.scan(level,worldPosition);previewTick=level.getGameTime();
        }
        if(!(loadedEntity(worldPosition.below(2)) instanceof ArcanePedestalBlockEntity center))return Optional.empty();
        var components=previewLayout.pedestals().stream().map(this::loadedEntity)
                .filter(ArcanePedestalBlockEntity.class::isInstance).map(ArcanePedestalBlockEntity.class::cast)
                .map(p->p.getItem(0)).filter(s->!s.isEmpty()).map(ItemStack::copy).toList();
        if(crafting()) {
            var items=List.<InfusionPreview.Item>of();
            if(pendingRepeat!=null) {
                var original=level.getRecipeManager().byKey(pendingRepeat.recipe()).filter(InfusionRecipe.class::isInstance).map(InfusionRecipe.class::cast);
                if(original.isPresent())try {
                    var plan=original.get().plan(new InfusionRecipe.Input(pendingRepeat.center(),pendingRepeat.components(),key->true));
                    items=InfusionPreview.checklist(original.get(),plan,pendingRepeat.center(),center.getItem(0),components);
                }catch(IllegalArgumentException|ArithmeticException invalid) { /* The committed process remains authoritative after reload. */ }
            }
            int environment=adjustSymmetry(previewLayout.symmetry());
            return Optional.of(new Preview(new InfusionRisk(process.recipeInstability(),environment,process.instability()),process.required(),environment,items));
        }
        boolean ordered=tc4revival.config.TCConfig.SERVER.orderedInfusion.get();
        boolean same=ItemStack.matches(previewCenter,center.getItem(0)) && previewComponents.size()==components.size();
        for(int i=0;same && i<components.size();i++)same=ItemStack.matches(previewComponents.get(i),components.get(i));
        if(matchTick==Long.MIN_VALUE || level.getGameTime()-matchTick>=20 || !same || previewOrdered!=ordered) {
            previewMatch=InfusionPreview.find(level,center.getItem(0),components,ordered);
            previewCenter=center.getItem(0).copy();previewComponents=components;previewOrdered=ordered;matchTick=level.getGameTime();
        }
        // Device stability applies even to an empty altar, independently of a recipe's cost.
        deviceStats=thaumcraft.api.crafting.InfusionDeviceModifiers.evaluate(level,worldPosition,previewLayout);
        int environment=adjustSymmetry(previewLayout.symmetry());
        return Optional.of(previewMatch.map(match->{var plan=adjustPlan(match.plan(),previewLayout);
            return new Preview(InfusionRisk.preview(plan.instability(),environment),plan.aspects(),environment,match.items());
        }).orElseGet(()->new Preview(null,new AspectAmounts(),environment,List.of())));
    }
    public boolean crafting() { return level!=null && level.isClientSide ? clientCrafting : process.crafting(); }
    public int instability() { return level!=null && level.isClientSide ? clientInstability : process.instability(); }
    public void addInstability(int amount) {
        if (level==null || level.isClientSide || !process.crafting() || amount==0) return;
        process.addInstability(amount); changed();
    }
    public List<Source> sources() { return List.copyOf(sources); }
    @Override public AspectAmounts revealedAspects() { return level!=null && level.isClientSide ? clientAspects.copy() : process.remaining(); }
    public void activate() { active=true; changed(); }
    public void tick() {
        if (level==null) return;
        ticks++; sources.removeIf(source->source.expires()<=level.getGameTime());
        if (level.isClientSide) {
            tickAnimation();
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->tc4revival.client.InfusionIntakeEffects.emit(this));
            return;
        }
        if (checkSurroundings) { checkSurroundings=false; layout=InfusionLayout.scan(level,worldPosition); }
        if (ticks%(crafting() ? 20 : 100)==0 && !validLocation()) { active=false; changed(); return; }
        if (active && crafting() && ticks%Math.max(1,cycleDelay(process.delay()))==0) { process.cycle(level.random,this); setChanged(); }
    }
    private void tickAnimation() {
        if (crafting()) {
            if (craftCount==0 || craftCount%65==0) level.playLocalSound(worldPosition.getX(),worldPosition.getY(),worldPosition.getZ(),
                    craftCount==0 ? TCSounds.INFUSER_START.get() : TCSounds.INFUSER.get(),SoundSource.BLOCKS,.5f,1,false);
            craftCount++;
        } else if (craftCount>0) craftCount=Math.min(50,Math.max(0,craftCount-2));
        if (active && startUp!=1) { startUp+=Math.max(startUp/10,.001f); if (startUp>.999f) startUp=1; }
        if (!active && startUp>0) { startUp-=startUp/10; if (startUp<.001f) startUp=0; }
    }
    public boolean validLocation() {
        if (level==null || !(loadedEntity(worldPosition.below(2)) instanceof ArcanePedestalBlockEntity)) return false;
        var material=thaumcraft.api.crafting.InfusionAltarMaterials.forMatrix(getBlockState().getBlock());
        if(material==null||!level.getBlockState(worldPosition.below(2)).is(material.pedestal().get()))return false;
        for (int x:new int[]{-1,1}) for (int z:new int[]{-1,1}) {
            var pos=worldPosition.offset(x,-2,z);
            if (!level.hasChunkAt(pos) || !level.getBlockState(pos).is(material.pillar().get())
                    || !level.getBlockState(pos.above()).is(material.pillarTop().get())) return false;
        }
        return true;
    }
    private BlockEntity loadedEntity(BlockPos pos) { return level!=null && level.hasChunkAt(pos) ? level.getBlockEntity(pos) : null; }
    public void start(ServerPlayer player) {start(player,player.getMainHandItem(),net.minecraft.world.InteractionHand.MAIN_HAND);}
    public void start(ServerPlayer player,ItemStack wand,net.minecraft.world.InteractionHand hand) {
        if(committing)return;
        if(process.needsRecovery()){
            if(!process.retryRecovery()){active=false;pendingRepeat=null;player.displayClientMessage(Component.translatable("thaumcraft.infusion.recovery_failed"),false);changed();return;}
            if(process.crafting()){active=true;player.displayClientMessage(Component.translatable("thaumcraft.infusion.recovery_restored"),false);changed();return;}
        }
        if (crafting()) return;
        if (!validLocation()) { active=false; changed(); return; }
        layout=InfusionLayout.scan(level,worldPosition);
        if (!(loadedEntity(worldPosition.below(2)) instanceof ArcanePedestalBlockEntity center) || center.isEmpty()) return;
        var components=pedestals().stream().map(pedestal->pedestal.getItem(0)).filter(stack->!stack.isEmpty()).map(ItemStack::copy).toList();
        if (components.isEmpty()) return;
        var knowledge=KnowledgeSavedData.get(player.getServer()).snapshot(player.getUUID());
        var input=new InfusionRecipe.Input(center.getItem(0),components,key->knowledge.map(value->value.researchMarkers().contains(key)).orElse(false),tc4revival.config.TCConfig.SERVER.orderedInfusion.get());
        var recipe=InfusionRecipe.find(level,input); if (recipe.isEmpty()) return;
        var plan=adjustPlan(recipe.get().planForCraft(input,level),layout);
        var output=plan.output().freeze(recipe.get().getId().toString(),recipe.get().research(),center.getItem(0));
        if(plan.enchanting())try {output=output.asEnchantment(center.getItem(0));}
        catch(IllegalArgumentException invalid){return;}
        var proposed=output.apply(center.getItem(0));
        if(proposed.isEmpty() && !output.consumesCenter()
                || plan.enchanting() && !InfusionOutput.onlyEnchantmentIncreased(center.getItem(0),proposed))return;
        activationWand=wand.copy();activationHand=hand;
        if(!thaumcraft.api.wands.MagicOperationRules.allows(operation(output,proposed,player,true),true))return;
        plan=new InfusionRecipe.Plan(plan.components(),plan.aspects(),plan.instability(),plan.xp(),plan.enchanting(),output);
        pendingRepeat=InfusionRepeat.capture(recipe.get(),center.getItem(0),pedestals());
        process.start(plan,center.getItem(0),player.getGameProfile().getName(),adjustSymmetry(layout.symmetry()));
        level.playSound(null,worldPosition,TCSounds.CRAFT_START.get(),SoundSource.BLOCKS,.5f,1); changed();
    }
    @Override public boolean centerMatches(ItemStack expected) {
        if (!(loadedEntity(worldPosition.below(2)) instanceof ArcanePedestalBlockEntity center)) return false;
        return InfusionIngredient.centerMatches(center.getItem(0),expected);
    }
    @Override public InfusionProcess.Experience drainExperience() {
        var players=level.getEntitiesOfClass(Player.class,new AABB(worldPosition).inflate(10));
        for (var player:players) if (player.experienceLevel>0) {
            player.giveExperienceLevels(-1); player.hurt(level.damageSources().magic(),level.random.nextInt(2));
            addSource(worldPosition,player.getId(),0,false,15);
            level.playSound(null,player,SoundEvents.FIRE_EXTINGUISH,SoundSource.PLAYERS,1,2+level.random.nextFloat()*.4f);
            return InfusionProcess.Experience.DRAINED;
        }
        return players.isEmpty() ? InfusionProcess.Experience.NO_PLAYERS : InfusionProcess.Experience.NO_LEVELS;
    }
    @Override public boolean drainEssentia(Aspect aspect) {
        var source=EssentiaSources.drain(this,aspect,null,12,false);
        source.ifPresent(pos->addSource(pos,0,aspect.color(),true,15)); return source.isPresent();
    }
    @Override public List<ArcanePedestalBlockEntity> pedestals() {
        return layout.pedestals().stream().map(this::loadedEntity).filter(ArcanePedestalBlockEntity.class::isInstance).map(ArcanePedestalBlockEntity.class::cast).toList();
    }
    @Override public void ingredientStarted(BlockPos position) { addSource(position,0,0,false,60); }
    private void addSource(BlockPos pos,int entity,int color,boolean essentia,int duration) {
        sources.removeIf(source->source.position().equals(pos) && source.entityId()==entity && source.color()==color && source.essentia()==essentia);
        sources.add(new Source(pos.immutable(),entity,color,essentia,level.getGameTime()+duration)); changed();
    }
    @Override public void instabilityEvent(int event) { InfusionInstability.apply((ServerLevel)level,worldPosition,layout.pedestals(),event); }
    @Override public void refreshSurroundings() { checkSurroundings=true; }
    @Override public void failed() { level.playSound(null,worldPosition,TCSounds.CRAFT_FAIL.get(),SoundSource.BLOCKS,1,.6f); }
    @Override public void finish(InfusionOutput output,String name) {
        if(committing)return;committing=true;
        try {finishOnce(output,name);} finally {committing=false;}
    }
    private void finishOnce(InfusionOutput output,String name) {
        if (!(loadedEntity(worldPosition.below(2)) instanceof ArcanePedestalBlockEntity center)) return;
        var result=output.apply(center.getItem(0)); if (result.isEmpty() && !output.consumesCenter()) { failed(); return; }
        var player=level instanceof ServerLevel server?server.getServer().getPlayerList().getPlayerByName(name):null;
        var activation=operation(output,result,player,false);
        if(!thaumcraft.api.wands.MagicOperationRules.allows(activation,true)
                || !thaumcraft.api.crafting.InfusionLifecycle.commit(new thaumcraft.api.crafting.InfusionLifecycle.Context(this,center,name,result,activation))){
            pendingRepeat=null;failed();setChanged();return;
        }
        lastRepeat=pendingRepeat;pendingRepeat=null;setChanged();
        for (var recipient:level.players()) if (recipient instanceof ServerPlayer server && recipient.getGameProfile().getName().equals(name)) {
            net.minecraftforge.event.ForgeEventFactory.firePlayerCraftingEvent(server,result,new SimpleContainer(0)); break;
        }
        level.blockEvent(center.getBlockPos(),center.getBlockState().getBlock(),12,0);
    }
    @Override public void changed() {
        setChanged(); if (level!=null && !level.isClientSide) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);
    }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putBoolean("Active",active); tag.put("Process",process.save());
        tag.put("ActivationWand",activationWand.save(new CompoundTag()));tag.putString("ActivationHand",activationHand.name());
        tag.putDouble("DeviceCycleMultiplier",deviceStats.cycleMultiplier());tag.putInt("DeviceInstability",deviceStats.instability());
        if(pendingRepeat!=null)tag.put("PendingRepeat",pendingRepeat.save());if(lastRepeat!=null)tag.put("LastRepeat",lastRepeat.save());
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); active=tag.getBoolean("Active");
        if(tag.contains("Process")){activationWand=ItemStack.of(tag.getCompound("ActivationWand"));activationHand="OFF_HAND".equals(tag.getString("ActivationHand"))?net.minecraft.world.InteractionHand.OFF_HAND:net.minecraft.world.InteractionHand.MAIN_HAND;}
        double multiplier=tag.contains("DeviceCycleMultiplier")?tag.getDouble("DeviceCycleMultiplier"):1;
        deviceStats=new thaumcraft.api.crafting.InfusionDeviceModifiers.Stats(1,Double.isFinite(multiplier)&&multiplier>0?multiplier:1,tag.getInt("DeviceInstability"));
        if (tag.contains("Process")) { process.load(tag.getCompound("Process")); checkSurroundings=true;
            pendingRepeat=InfusionRepeat.load(tag.getCompound("PendingRepeat"),worldPosition);lastRepeat=InfusionRepeat.load(tag.getCompound("LastRepeat"),worldPosition);
            if(process.needsRecovery()){active=false;pendingRepeat=null;}
        }
        else {
            clientCrafting=tag.getBoolean("Crafting"); clientInstability=tag.getInt("Instability"); clientAspects=AspectNbt.read(tag,TCAspects.catalog()).amounts();
            sources.clear(); for (var entry:tag.getList("Sources",net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                var row=(CompoundTag)entry; var pos=BlockPos.of(row.getLong("Position"));
                if (Math.abs(pos.getX()-(long)worldPosition.getX())>127 || Math.abs(pos.getY()-(long)worldPosition.getY())>127 || Math.abs(pos.getZ()-(long)worldPosition.getZ())>127) continue;
                sources.add(new Source(pos,row.getInt("Entity"),row.getInt("Color"),row.getBoolean("Essentia"),row.getLong("Expires")));
            }
        }
    }
    @Override public CompoundTag getUpdateTag() {
        var tag=new CompoundTag(); tag.putBoolean("Active",active); tag.putBoolean("Crafting",process.crafting()); tag.putInt("Instability",process.instability()); AspectNbt.write(tag,process.remaining());
        var list=new net.minecraft.nbt.ListTag(); for (var source:sources) {
            var row=new CompoundTag(); row.putLong("Position",source.position().asLong()); row.putInt("Entity",source.entityId()); row.putInt("Color",source.color());
            row.putBoolean("Essentia",source.essentia()); row.putLong("Expires",source.expires()); list.add(row);
        }
        tag.put("Sources",list); return tag;
    }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
    @Override public AABB getRenderBoundingBox() { return new AABB(worldPosition).inflate(1); }
    @Override public InteractionResult onWandUseFirst(ItemStack wand,UseOnContext context) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.PASS;
        if (!level.mayInteract(player,worldPosition) || !player.mayUseItemAt(worldPosition,context.getClickedFace(),wand)) return InteractionResult.FAIL;
        if (process.needsRecovery() || active && !crafting()) { start(player,wand,context.getHand()); return InteractionResult.SUCCESS; }
        if (!active && validLocation()) { activate(); return InteractionResult.SUCCESS; }
        return InteractionResult.PASS;
    }
    @Override public Optional<ItemStack> onWandUse(Level level,ItemStack wand,Player player) { return Optional.of(wand); }
    @Override public void onWandUseTick(ItemStack wand,Player player,int remaining) {}
    @Override public void onWandRelease(ItemStack wand,Level level,Player player,int remaining) {}
}
