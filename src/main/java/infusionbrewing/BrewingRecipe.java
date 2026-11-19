package infusionbrewing;

import com.google.gson.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeSerializer;
import tc4revival.content.*;

public class BrewingRecipe extends InfusionRecipe {
    private final String mode;
    public BrewingRecipe(ResourceLocation id,JsonObject json){super(id,json);mode=json.get("mode").getAsString();if(!java.util.Set.of("duration","amplifier","splash","bauble","focus").contains(mode))throw new IllegalArgumentException("Unknown brewing mode");}
    @Override public boolean acceptsCenter(ItemStack central){
        if(!(central.is(Items.POTION)||central.is(Items.SPLASH_POTION)))return false;
        var data=PotionData.read(central);if(data.isEmpty())return false;var p=data.get();
        return switch(mode){
            case "duration"->!BrewingRules.instant(p.legacyId())&&BrewingRules.nextDuration(p.duration())>0;
            case "amplifier"->!BrewingRules.noAmplifier(p.legacyId())&&p.amplifier()<3;
            case "splash"->central.is(Items.POTION);
            case "bauble"->!BrewingRules.instant(p.legacyId())&&p.maxDuration();
            case "focus"->central.is(Items.SPLASH_POTION)&&p.maxDuration();default->false;
        };
    }
    @Override public Plan plan(ItemStack central){
        var base=super.plan(central);var optional=PotionData.read(central);if(optional.isEmpty()||!acceptsCenter(central))return base;
        var p=optional.get();var target=switch(mode){
            case "duration"->new PotionData(p.legacyId(),BrewingRules.nextDuration(p.duration()),p.amplifier());
            case "amplifier"->new PotionData(p.legacyId(),p.duration(),p.amplifier()+1);
            default->p;
        };
        ItemStack output;
        if(mode.equals("bauble")||mode.equals("focus"))output=base.output().apply(central);
        else if(mode.equals("splash")){output=new ItemStack(Items.SPLASH_POTION);if(central.hasTag())output.setTag(central.getTag().copy());}
        else {output=central.copy();output.setCount(1);}
        target.write(output);
        return new Plan(base.components(),base.aspects(),base.instability(),base.xp(),false,InfusionOutput.item(output));
    }
    @Override public ItemStack assemble(Input input,RegistryAccess access){return plan(input.getItem(0)).output().apply(input.getItem(0));}
    @Override public RecipeSerializer<?> getSerializer(){return InfusionBrewing.BREWING.get();}
    public static class Serializer implements RecipeSerializer<BrewingRecipe>{
        @Override public BrewingRecipe fromJson(ResourceLocation id,JsonObject json){return new BrewingRecipe(id,json);}
        @Override public BrewingRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf b){return fromJson(id,JsonParser.parseString(b.readUtf(1048576)).getAsJsonObject());}
        @Override public void toNetwork(FriendlyByteBuf b,BrewingRecipe recipe){b.writeUtf(recipe.displayDefinition().toString(),1048576);}
    }
}
