package conciliummodern;

import com.emoniph.witchery.brewing.WitcheryBrewRegistry;
import com.emoniph.witchery.common.ExtendedPlayer;
import com.emoniph.witchery.entity.BrewEntity;
import com.emoniph.witchery.infusion.Infusion;
import com.emoniph.witchery.infusion.symbols.*;
import com.emoniph.witchery.init.*;
import java.lang.reflect.Method;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

/** Real DoomCat Witchery 0.1.3.32-port API. Loaded only after the optional host check. */
public final class DoomCatWitcheryHost implements WitcheryBridge.Host,ConciliumSpellIntegration.WitcheryHost {
    private static final Map<Integer,String> GENERIC=Map.ofEntries(
        Map.entry(11,"attuned_stone_charged"),Map.entry(21,"belladonna"),Map.entry(24,"bat_wool"),
        Map.entry(28,"foul_fume"),Map.entry(29,"diamond_vapour"),Map.entry(30,"oil_of_vitriol"),
        Map.entry(35,"reek_of_misfortune"),Map.entry(37,"tear_of_the_goddess"),
        Map.entry(114,"condensed_fear"),Map.entry(151,"pentacle"),Map.entry(157,"silver_dust"));
    private final Map<String,SymbolEffect> spells=new LinkedHashMap<>();
    private final Method addSpell;

    private DoomCatWitcheryHost(){
        // The port retained its real registry but made add private; invoke that one routine
        // so stroke lookup, spell IDs, display order and default strokes remain consistent.
        try{addSpell=EffectRegistry.class.getDeclaredMethod("add",SymbolEffect.class,StrokeSet[].class);addSpell.setAccessible(true);}
        catch(ReflectiveOperationException e){throw new IllegalStateException("Unsupported Witchery symbol registry",e);}
    }
    static void install(){WitcheryBridge.register(new DoomCatWitcheryHost());}
    @Override public String modId(){return "witchery";}
    @Override public ItemStack item(String legacyName,int metadata){
        String path=switch(legacyName){
            case "generic"->GENERIC.get(metadata);
            case "seeds_garlic"->"garlic";
            case "bramble"->metadata==0?"ender_bramble":metadata==1?"wild_bramble":null;
            default->metadata==0?legacyName:null;
        };
        if(path==null)return ItemStack.EMPTY;
        Item item=ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("witchery",path));
        return item==null||item==Items.AIR?ItemStack.EMPTY:new ItemStack(item);
    }
    @Override public ItemStack brew(List<ItemStack> ingredients,Integer color,boolean updateName){
        var result=new ItemStack(WitcheryItems.BREW.get());var tag=result.getOrCreateTag();var ordered=new ListTag();
        for(var ingredient:ingredients){if(ingredient.isEmpty())throw new IllegalArgumentException("Empty Witchery brew ingredient");ordered.add(ingredient.copyWithCount(1).save(new CompoundTag()));}
        tag.put("Items",ordered);if(color!=null)tag.putInt("Color",color);
        if(updateName){tag.putString("BrewName",WitcheryBrewRegistry.INSTANCE.getBrewName(tag));WitcheryBrewRegistry.INSTANCE.updateBrewInformation(tag);}
        return result;
    }
    @Override public ThrowableProjectile brewProjectile(ServerLevel level,LivingEntity thrower,ItemStack brew){return new BrewEntity(level,thrower,brew.copy(),false);}
    @Override public boolean vampire(Player player){return ExtendedPlayer.isVampire(player);}
    @Override public int werewolfLevel(Player player){return ExtendedPlayer.getWerewolfLevel(player);}
    @Override public MobEffect gasMask(){return WitcheryEffects.GAS_MASK.get();}
    @Override public MobEffect colorfulEffect(){return WitcheryEffects.COLORFUL.get();}
    @Override public ResourceLocation broomTexture(){return ResourceLocation.parse("witchery:textures/entity/broom.png");}
    @Override public BlockState stockade(){return WitcheryBlocks.STOCKADE_OAK.get().defaultBlockState();}
    @Override public EntityType<? extends Mob> witchHunter(){return WitcheryEntities.WITCH_HUNTER.get();}
    @Override public ConciliumSpellIntegration.WitcheryHost spells(){return this;}
    @Override public boolean registerSpell(ConciliumSpellIntegration.WitcherySpell definition,ConciliumSpellIntegration.WitcheryCast action){
        if(EffectRegistry.getEffect(definition.legacyId())!=null)throw new IllegalStateException("Witchery spell ID already occupied: "+definition.legacyId());
        var strokes=definition.strokes().stream().map(s->{byte[] b=new byte[s.strokes().size()];for(int i=0;i<b.length;i++)b[i]=s.strokes().get(i).byteValue();return new StrokeSet(s.level(),b);}).toArray(StrokeSet[]::new);
        for(var set:strokes)if(EffectRegistry.contains(set.strokes()))throw new IllegalStateException("Witchery stroke sequence already occupied: "+definition.key());
        var spell=new SymbolEffect(definition.legacyId(),definition.name(),definition.level(),definition.charging(),false,definition.key(),0){
            @Override public void perform(Level level,Player player,int power){if(!level.isClientSide&&player instanceof ServerPlayer serverPlayer)action.cast(serverPlayer,power);}
        };
        try{addSpell.invoke(null,spell,strokes);}catch(ReflectiveOperationException e){throw new IllegalStateException("Could not register real Witchery spell "+definition.key(),e);}
        spells.put(definition.key(),spell);return true;
    }
    @Override public int infusion(ServerPlayer player){return Infusion.getInfusionID(player);}
    @Override public boolean grantKnowledge(ServerPlayer player,List<String> keys){
        if(!spells.keySet().containsAll(keys))return false;
        keys.forEach(key->spells.get(key).acquireKnowledge(player));Infusion.syncPlayer(player);return true;
    }
    /** Called by the optional mixin after the real host commits the contract's effect. */
    public static boolean activateContract(Object contract,ItemStack stack,LivingEntity target,Player player){
        var original=stack.copy();boolean success=((com.emoniph.witchery.item.ContractItem)contract).activate(stack,target);
        if(success&&!player.level().isClientSide)com.ilya3point999k.thaumicconcilium.api.event.WitcheryImpDealEvent.fireImpDealEvent(player,original,target);
        return success;
    }
}
