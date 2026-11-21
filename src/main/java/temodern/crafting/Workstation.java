package temodern.crafting;

import java.util.*;
import appeng.api.config.Actionable;
import appeng.api.crafting.*;
import appeng.api.implementations.blockentities.*;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.*;
import appeng.api.storage.StorageHelper;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.ItemHandlerHelper;
import tc4revival.aspect.Aspect;
import tc4revival.content.*;
import tc4revival.research.KnowledgeSavedData;
import temodern.item.KnowledgeCore;
import temodern.machine.NetworkMachine;
import temodern.essentia.EssentiaKey;

/** Shared physical inventory and server transactions for the terminal, assembler and encoder. */
public class Workstation implements ICraftingMachine, appeng.api.networking.crafting.ICraftingProvider {
    public final SimpleContainer inventory = new SimpleContainer(23);
    private final WorkHost host;
    private String pendingRecipe = "";
    private final AutomationPatterns automationPatterns = new AutomationPatterns();
    private final AutomationPatterns wandPatterns = new AutomationPatterns();
    private net.minecraftforge.common.util.FakePlayer caster;
    public Workstation(WorkHost host) { this.host=host; inventory.addListener(ignored -> {host.markWorkChanged(); if(host.kind().equals("arcane_assembler") && host.workLevel()!=null&&!host.workLevel().isClientSide)appeng.api.networking.crafting.ICraftingProvider.requestUpdate(host.node());}); }
    public boolean enabled() { return Set.of("arcane_terminal","arcane_assembler","distillation_encoder","knowledge_inscriber").contains(host.kind()); }
    public void save(CompoundTag tag) {
        var slots=NonNullList.withSize(23,ItemStack.EMPTY); for(int i=0;i<23;i++)slots.set(i,inventory.getItem(i));
        var data=new CompoundTag();ContainerHelper.saveAllItems(data,slots);data.putString("pendingRecipe",pendingRecipe);tag.put("workstation",data);
    }
    public void load(CompoundTag tag) {
        var data=tag.getCompound("workstation");var slots=NonNullList.withSize(23,ItemStack.EMPTY);ContainerHelper.loadAllItems(data,slots);
        for(int i=0;i<23;i++)inventory.setItem(i,slots.get(i));pendingRecipe=data.getString("pendingRecipe");
    }
    public CraftGrid grid() { var grid=new CraftGrid();for(int i=0;i<9;i++)grid.setItem(i,inventory.getItem(i).copy());return grid; }
    private Set<String> known(ServerPlayer player) {
        var core=inventory.getItem(10);
        if(host.kind().equals("arcane_assembler") && core.getItem() instanceof thaumcraft.api.wands.AutomationCore source && source.enabled(core)
                && host.workLevel() instanceof net.minecraft.server.level.ServerLevel level) return source.research(core,level);
        return player==null?java.util.Set.of():KnowledgeSavedData.get(player.getServer()).snapshot(player.getUUID()).map(k -> k.researchMarkers()).orElse(Set.of());
    }
    private record Plan(Recipe<net.minecraft.world.inventory.CraftingContainer> recipe, ItemStack result, Map<Aspect,Integer> cost) {}
    private Plan plan(CraftGrid grid, Set<String> known, boolean arcaneOnly) {
        var level=host.workLevel();
        if(!arcaneOnly) {
            var recipe=level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,level).orElse(null);
            if(recipe!=null)return new Plan(recipe,recipe.assemble(grid,level.registryAccess()),Map.of());
        }
        for(var recipe:level.getRecipeManager().getAllRecipesFor(TCArcane.RECIPE_TYPE.get())) {
            if(!pendingRecipe.isEmpty()&&!pendingRecipe.equals(recipe.getId().toString()))continue;
            if(recipe.matches(grid,level)&&(recipe.unlocked(grid,known::contains) || arcaneOnly && storedRecipe(recipe.getId().toString(),grid)))return new Plan(recipe,recipe.assemble(grid,level.registryAccess()),recipe.visCost(grid));
        }
        return null;
    }
    private boolean storedRecipe(String recipe,CraftGrid grid){
        for(var details:getAvailablePatterns()){
            var pattern=(ArcanePattern)details;
            if(!pattern.recipe().equals(recipe))continue;
            var decoded=ArcanePattern.recipe(host.workLevel(),recipe);
            if(decoded!=null&&ItemStack.matches(decoded.assemble(pattern.grid(),host.workLevel().registryAccess()),decoded.assemble(grid,host.workLevel().registryAccess())))return true;
        }
        return false;
    }
    public boolean inscribe(ServerPlayer player){
        if(!host.kind().equals("knowledge_inscriber"))return false;
        var sample=grid();
        for(var recipe:host.workLevel().getRecipeManager().getAllRecipesFor(TCArcane.RECIPE_TYPE.get())){
            if(recipe.matches(sample,host.workLevel())&&recipe.unlocked(sample,known(player)::contains)) {
                boolean changed=KnowledgeCore.toggle(inventory.getItem(10),ArcanePattern.encode(sample,recipe,host.workLevel()),host.workLevel());
                if(changed)inventory.setChanged();return changed;
            }
        }
        return false;
    }
    @Override public java.util.List<IPatternDetails> getAvailablePatterns(){
        if(host.workLevel()==null||!host.kind().equals("arcane_assembler"))return java.util.List.of();
        var result=new java.util.LinkedHashSet<IPatternDetails>();
        if(host.workLevel() instanceof net.minecraft.server.level.ServerLevel level){
            result.addAll(automationPatterns.resolve(inventory.getItem(10),inventory.getItem(11),level));
            var wand=inventory.getItem(9);
            if(wand.getItem() instanceof CastingWandItem item&&!item.isStaff(wand))
                result.addAll(wandPatterns.resolve(thaumcraft.api.wands.WandAutomation.core(wand),inventory.getItem(11),level));
        }
        var known=KnowledgeCore.decoded(inventory.getItem(10),host.workLevel());
        if(inventory.getItem(11).isEmpty())result.addAll(known);
        else {
            var selected=new ArcanePattern.Decoder().decodePattern(inventory.getItem(11),host.workLevel(),false);
            if(selected!=null&&known.contains(selected))result.add(selected);
        }
        return java.util.List.copyOf(result);
    }
    @Override public boolean pushPattern(IPatternDetails pattern,KeyCounter[] inputs){return pushPattern(pattern,inputs,Direction.UP);}
    @Override public boolean isBusy(){for(int i=0;i<9;i++)if(!inventory.getItem(i).isEmpty())return true;return !host.active();}
    private boolean outputFits(List<ItemStack> outputs) {
        var next=new SimpleContainer(6);for(int i=0;i<6;i++)next.setItem(i,inventory.getItem(12+i).copy());
        var wrapper=new InvWrapper(next);
        for(var output:outputs)if(!ItemHandlerHelper.insertItemStacked(wrapper,output.copy(),false).isEmpty())return false;
        return true;
    }
    private void storeOutput(List<ItemStack> outputs) {
        var next=new SimpleContainer(6);for(int i=0;i<6;i++)next.setItem(i,inventory.getItem(12+i).copy());
        var wrapper=new InvWrapper(next);
        for(var output:outputs)if(!ItemHandlerHelper.insertItemStacked(wrapper,output.copy(),false).isEmpty())throw new IllegalStateException("Output preflight changed");
        for(int i=0;i<6;i++)inventory.setItem(12+i,next.getItem(i));
    }
    public int upgrades(){return Math.min(5,host.getUpgrades().getInstalledUpgrades(appeng.core.definitions.AEItems.SPEED_CARD)
            +(inventory.getItem(22).is(appeng.core.definitions.AEItems.SPEED_CARD.asItem())?inventory.getItem(22).getCount():0));}
    public int workInterval(){return Math.max(1,20-4*upgrades());}
    public float modifier(Aspect aspect) {
        var stack=inventory.getItem(9);
        return stack.getItem() instanceof CastingWandItem wand&&automationCaster()!=null
                ?wand.consumptionModifier(stack,automationCaster(),aspect,true):1f;
    }
    private net.minecraftforge.common.util.FakePlayer automationCaster(){
        if(!(host.workLevel() instanceof net.minecraft.server.level.ServerLevel level))return null;
        if(caster==null||caster.level()!=level)caster=new net.minecraftforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"[ArcaneAssembler]"));
        caster.moveTo(host.workPosition().getCenter());
        caster.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,inventory.getItem(9));
        var slots=new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.entity.EquipmentSlot.CHEST,net.minecraft.world.entity.EquipmentSlot.LEGS,net.minecraft.world.entity.EquipmentSlot.FEET};
        for(int i=0;i<slots.length;i++)caster.setItemSlot(slots[i],inventory.getItem(18+i));
        caster.getAttribute(TCAttributes.VIS_COST.get()).setBaseValue(equipmentAttribute(TCAttributes.VIS_COST.get()));
        TCAttributes.ASPECT_COSTS.values().forEach(attribute->caster.getAttribute(attribute.get()).setBaseValue(equipmentAttribute(attribute.get())));
        return caster;
    }
    private double equipmentAttribute(net.minecraft.world.entity.ai.attributes.Attribute attribute){
        var value=new net.minecraft.world.entity.ai.attributes.AttributeInstance(attribute,ignored->{});
        var slots=new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.entity.EquipmentSlot.CHEST,net.minecraft.world.entity.EquipmentSlot.LEGS,net.minecraft.world.entity.EquipmentSlot.FEET};
        for(int i=0;i<4;i++)for(var modifier:inventory.getItem(18+i).getAttributeModifiers(slots[i]).get(attribute)){
            value.removeModifier(modifier.getId());value.addTransientModifier(modifier);
        }
        return value.getValue();
    }
    private double craftEnergy() {
        double warp=Math.max(0,equipmentAttribute(TCAttributes.WARP.get()));
        int speed=upgrades();return workInterval()*(1.5+.75*speed)*(1+.15*warp);
    }
    private boolean pay(Plan plan, ServerPlayer player, boolean commit) {
        if(host.kind().equals("arcane_assembler")){
            var stack=inventory.getItem(9);var worker=automationCaster();
            if(!(stack.getItem() instanceof CastingWandItem wand)||wand.isStaff(stack)||worker==null)return false;
            boolean paid=plan.cost.isEmpty()||wand.consumeForPlayer(stack,worker,plan.cost,true,true,commit);
            if(paid&&commit)inventory.setChanged();
            return paid;
        }
        if(plan.cost.isEmpty())return true;
        var wand=player==null?ItemStack.EMPTY:craftingWand(plan,player);
        if(player!=null&&wand.getItem() instanceof CastingWandItem item){
            boolean paid=item.consumeForPlayer(wand,player,plan.cost,true,true,commit);
            if(paid&&commit)player.getInventory().setChanged();
            return paid;
        }
        if(player!=null)return false;
        var costs=new LinkedHashMap<Aspect,Integer>();plan.cost.forEach((aspect,amount)->costs.put(aspect,(int)(tc4revival.content.ArcaneRecipe.hundredths(plan.cost,aspect)*(host.kind().equals("arcane_assembler")?modifier(aspect):1f))));
        return host.consumeNetworkVis(costs,!commit);
    }
    private ItemStack craftingWand(Plan plan,ServerPlayer player) {
        var candidates=new ArrayList<ItemStack>();candidates.add(inventory.getItem(9));
        if(host.inventoryWands())for(int i=0;i<player.getInventory().getContainerSize();i++)candidates.add(player.getInventory().getItem(i));
        for(var stack:candidates)if(stack.getItem() instanceof CastingWandItem item&&!item.isStaff(stack)
                &&item.consumeForPlayer(stack,player,plan.cost,true,true,false))return stack;
        return inventory.getItem(9);
    }
    /** A player-specific, non-persistent preview. Nothing is consumed by opening the terminal. */
    public record ManualPreview(ItemStack output, Map<Aspect,Integer> costs) {}
    public ManualPreview manualPreview(ServerPlayer player) {
        var plan=plan(grid(),known(player),false);
        if(plan==null)return new ManualPreview(ItemStack.EMPTY,Map.of());
        var costs=new LinkedHashMap<Aspect,Integer>();
        var wand=craftingWand(plan,player);
        plan.cost.forEach((aspect,amount)->costs.put(aspect,(int)(tc4revival.content.ArcaneRecipe.hundredths(plan.cost,aspect)*
                (wand.getItem() instanceof CastingWandItem item?item.consumptionModifier(wand,player,aspect,true):1f))));
        return new ManualPreview(host.active()&&pay(plan,player,false)?plan.result.copy():ItemStack.EMPTY,costs);
    }
    /** Recheck the recipe, research, destination and vis together on the server for every take. */
    public boolean takeManualResult(ServerPlayer player, ItemStack expected,
            java.util.function.Predicate<ItemStack> fits, java.util.function.Consumer<ItemStack> deliver) {
        if(!host.kind().equals("arcane_terminal")||!host.active())return false;
        var before=grid();var plan=plan(before,known(player),false);
        if(plan==null||plan.result.isEmpty()||!ItemStack.matches(expected,plan.result)
                ||!fits.test(plan.result.copy())||!pay(plan,player,false))return false;
        if(!pay(plan,player,true))return false;
        // ResultSlot/arcane workbench callbacks run before the grid debit. Addons may
        // restore a reusable ingredient here, so commit the callback's grid, not a stale copy.
        var crafting=grid();
        net.minecraftforge.event.ForgeEventFactory.firePlayerCraftingEvent(player,plan.result.copy(),crafting);
        plan.result.onCraftedBy(player.level(),player,plan.result.getCount());
        var remainders=CraftRemainders.forPlayer(plan.recipe,crafting,plan.result,player);
        for(int i=0;i<9;i++){
            var stack=crafting.getItem(i).copy();
            if(!stack.isEmpty()&&!CraftRemainders.catalyst(plan.result,crafting,i))stack.shrink(1);
            inventory.setItem(i,stack);
        }
        // Reserve the output's destination before returning buckets or other remainders to the player.
        deliver.accept(plan.result.copy());
        for(int i=0;i<9;i++){
            var rest=remainders.get(i).copy();
            if(rest.isEmpty()||CraftRemainders.catalyst(plan.result,crafting,i))continue;
            if(rest.isDamageableItem()&&rest.getDamageValue()>rest.getMaxDamage()){
                net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem(player,rest,null);continue;
            }
            var remaining=inventory.getItem(i);
            if(remaining.isEmpty())inventory.setItem(i,rest);
            else if(ItemStack.isSameItemSameTags(remaining,rest)&&remaining.getCount()+rest.getCount()<=remaining.getMaxStackSize())remaining.grow(rest.getCount());
            else if(!player.getInventory().add(rest))player.drop(rest,false);
        }
        replenish(before,plan,player);
        inventory.setChanged();
        if(!plan.recipe.isSpecial())player.awardRecipes(java.util.List.of(plan.recipe));
        return true;
    }
    /** Legacy terminals charge the inserted wand; the interface never bypasses wand crafting costs. */
    public boolean chargeWand(temodern.machine.VisSupply supply) {
        if(supply==null||!host.active())return false;
        var stack=inventory.getItem(9);
        if(!(stack.getItem() instanceof CastingWandItem wand)||!wand.rechargeable(stack))return false;
        boolean charged=false;
        for(var aspect:tc4revival.aspect.TCAspects.catalog().primals()){
            int wanted=wand.receiveVis(stack,aspect,Math.max(0,wand.capacity(stack)-wand.vis(stack,aspect)),true);
            if(wanted<=0)continue;
            int drained=supply.takeVis(aspect,wanted);
            if(drained>0){wand.receiveVis(stack,aspect,drained,false);charged=true;}
        }
        if(charged)inventory.setChanged();
        return charged;
    }
    public boolean craft(ServerPlayer player) {
        if(player!=null&&host.kind().equals("arcane_terminal")){
            var expected=manualPreview(player).output();
            return takeManualResult(player,expected,output->outputFits(java.util.List.of(output)),
                    output->storeOutput(java.util.List.of(output)));
        }
        if(!host.active() || !(host.kind().equals("arcane_terminal")||host.kind().equals("arcane_assembler")))return false;
        var grid=grid();var plan=plan(grid,known(player),player==null);if(plan==null||plan.result.isEmpty())return false;
        var outputs=new ArrayList<ItemStack>();outputs.add(plan.result.copy());
        var remainders=CraftRemainders.all(plan.recipe,grid,plan.result);
        // Manual crafting leaves catalysts in place. Automated jobs return them to the ME CPU.
        for(int i=0;i<9;i++) if(!remainders.get(i).isEmpty() && (player==null || !CraftRemainders.catalyst(plan.result,grid,i))) outputs.add(remainders.get(i));
        if(!outputFits(outputs)||!pay(plan,player,false))return false;
        var power=host.grid().getEnergyService();
        double ae=host.kind().equals("arcane_assembler")?craftEnergy():0;
        if(power.extractAEPower(ae,Actionable.SIMULATE,appeng.api.config.PowerMultiplier.CONFIG)+1e-6<ae)return false;
        if(!pay(plan,player,true))return false;
        power.extractAEPower(ae,Actionable.MODULATE,appeng.api.config.PowerMultiplier.CONFIG);
        for(int i=0;i<9;i++)if(!inventory.getItem(i).isEmpty() && (player==null || !CraftRemainders.catalyst(plan.result,grid,i)))inventory.removeItem(i,1);
        storeOutput(outputs);pendingRecipe="";host.markWorkChanged();
        if(player!=null&&host.kind().equals("arcane_terminal"))replenish(grid,plan,player);
        if(player!=null){net.minecraftforge.event.ForgeEventFactory.firePlayerCraftingEvent(player,plan.result.copy(),grid);plan.result.onCraftedBy(player.level(),player,plan.result.getCount());}
        return true;
    }
    private void replenish(CraftGrid before,Plan plan,ServerPlayer player){
        var network=host.grid();if(network==null)return;
        var storage=network.getStorageService().getInventory();var source=IActionSource.ofPlayer(player,host);
        for(int slot=0;slot<9;slot++){
            if(before.getItem(slot).isEmpty()||!inventory.getItem(slot).isEmpty())continue;
            var original=AEItemKey.of(before.getItem(slot));
            if(StorageHelper.poweredExtraction(network.getEnergyService(),storage,original,1,source)==1){inventory.setItem(slot,original.toStack());continue;}
            for(var entry:storage.getAvailableStacks()){
                if(entry.getLongValue()<1||!(entry.getKey() instanceof AEItemKey key))continue;
                var candidate=before.getItem(slot);before.setItem(slot,key.toStack());
                boolean valid=plan.recipe.matches(before,host.workLevel())&&ItemStack.matches(plan.result,plan.recipe.assemble(before,host.workLevel().registryAccess()));
                before.setItem(slot,candidate);
                if(valid&&StorageHelper.poweredExtraction(network.getEnergyService(),storage,key,1,source)==1){inventory.setItem(slot,key.toStack());before.setItem(slot,key.toStack());break;}
            }
        }
    }
    public boolean encode(ServerPlayer player, boolean distillation) {
        if(!distillation||!host.kind().equals("distillation_encoder"))return false;
        if(!inventory.getItem(11).is(appeng.core.definitions.AEItems.BLANK_PATTERN.asItem()))return false;
        var input=inventory.getItem(0);var aspects=AlchemyFurnaceBlockEntity.inputAspects(input);
        if(input.isEmpty()||aspects.isEmpty()||aspects.total()>50)return false;
        var outputs=aspects.view().entrySet().stream().filter(e->e.getValue()>0)
                .map(e->new GenericStack(EssentiaKey.of(e.getKey()),e.getValue())).toArray(GenericStack[]::new);
        if(outputs.length==0)return false;
        var pattern=PatternDetailsHelper.encodeProcessingPattern(new GenericStack[]{new GenericStack(AEItemKey.of(input),1)},outputs);
        if(!outputFits(List.of(pattern)))return false;
        inventory.removeItem(11,1);storeOutput(List.of(pattern));return true;
    }
    /** Refill the actual sample grid from ME, never create ghost items in a real slot. */
    public boolean refill(ServerPlayer player) {
        if(!host.active()||host.grid()==null)return false;
        var network=host.grid();boolean changed=false;
        for(int i=0;i<9;i++){
            var stack=inventory.getItem(i);if(stack.isEmpty()||stack.getCount()>=stack.getMaxStackSize())continue;
            long taken=StorageHelper.poweredExtraction(network.getEnergyService(),network.getStorageService().getInventory(),AEItemKey.of(stack),stack.getMaxStackSize()-stack.getCount(),IActionSource.ofPlayer(player,host));
            if(taken>0){stack.grow((int)taken);changed=true;}
        }
        if(changed)inventory.setChanged();return changed;
    }
    public void tick() {
        if(!enabled()||!host.kind().equals("arcane_assembler")||!host.active()||host.workLevel().getGameTime()%workInterval()!=0)return;
        getAvailablePatterns();
        if(automationPatterns.takeChanged()|wandPatterns.takeChanged())appeng.api.networking.crafting.ICraftingProvider.requestUpdate(host.node());
        craft(null);
        var grid=host.grid();if(grid==null)return;
        for(int i=12;i<18;i++){
            var stack=inventory.getItem(i);if(stack.isEmpty())continue;
            long inserted=StorageHelper.poweredInsert(grid.getEnergyService(),grid.getStorageService().getInventory(),AEItemKey.of(stack),stack.getCount(),host.action());
            if(inserted>0)inventory.removeItem(i,(int)inserted);
        }
    }
    @Override public PatternContainerGroup getCraftingMachineInfo() { return new PatternContainerGroup(AEItemKey.of(host.getMainMenuIcon()), Component.translatable("block.thaumicenergistics.arcane_assembler"), List.of()); }
    @Override public boolean acceptsPlans() { return host.kind().equals("arcane_assembler")&&host.active(); }
    @Override public boolean pushPattern(IPatternDetails details, KeyCounter[] inputs, Direction side) {
        if(!acceptsPlans()||!(details instanceof ArcanePattern pattern))return false;
        for(int i=0;i<9;i++)if(!inventory.getItem(i).isEmpty())return false;
        var proposed=pattern.suppliedGrid(inputs,host.workLevel());if(proposed==null)return false;var oldPending=pendingRecipe;pendingRecipe=pattern.recipe();
        var plan=plan(proposed,known(null),true);pendingRecipe=oldPending;
        if(plan==null || !getAvailablePatterns().contains(pattern))return false;
        for(int i=0;i<9;i++)inventory.setItem(i,proposed.getItem(i).copyWithCount(proposed.getItem(i).isEmpty()?0:1));
        for(var counter:inputs)counter.clear();
        pendingRecipe=pattern.recipe();host.markWorkChanged();return true;
    }
    public void dropContents() { if(host.kind().equals("knowledge_inscriber")||host.kind().equals("distillation_encoder"))for(int i=0;i<9;i++)inventory.setItem(i,ItemStack.EMPTY); Containers.dropContents(host.workLevel(),host.workPosition(),inventory);inventory.clearContent(); }
}
