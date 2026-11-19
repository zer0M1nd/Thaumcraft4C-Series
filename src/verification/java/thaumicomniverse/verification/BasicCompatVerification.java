package thaumicomniverse.verification;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.*;
import static thaumicomniverse.verification.ServerVerification.*;

final class BasicCompatVerification {
    private static net.minecraft.resources.ResourceLocation armTypeId(com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType type) {
        try {
            try {
                var registry=(net.minecraft.core.Registry<com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType>)
                        Class.forName("com.simibubi.create.api.registry.CreateBuiltInRegistries").getField("ARM_INTERACTION_POINT_TYPE").get(null);
                return registry.getKey(type);
            } catch (ClassNotFoundException legacy) {
                return (net.minecraft.resources.ResourceLocation)com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType.class.getMethod("getId").invoke(type);
            }
        } catch (ReflectiveOperationException failure) { throw new AssertionError("Cannot inspect Create arm registry",failure); }
    }
    static void run(MinecraftServer server) {
        if (ModList.get().isLoaded("create")) check("Create arm and belt pedestal integration", () -> {
            var pos = new net.minecraft.core.BlockPos(35, 100, 35); var level = server.overworld();
            var block = ForgeRegistries.BLOCKS.getValues().stream().filter(value -> value instanceof tc4revival.content.ArcanePedestalBlock).findFirst().orElseThrow();
            level.setBlockAndUpdate(pos, block.defaultBlockState());
            var type = com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType.getPrimaryType(level, pos, block.defaultBlockState());
            require(type != null && armTypeId(type).equals(id("arcane_pedestal")), "Arm type not registered");
            require(thaumcraft.api.devices.DeviceExtensions.component(level.getBlockEntity(pos),thaumicomniverse.compat.create.CreateDevices.PEDESTAL).isPresent(), "Pedestal component absent");
            var bridge = thaumcraft.api.devices.DeviceExtensions.component(level.getBlockEntity(pos),thaumicomniverse.compat.create.CreateDevices.PEDESTAL).orElseThrow();
            require(bridge.getBehaviour(com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour.TYPE) != null, "Belt input missing");
            level.removeBlock(pos, false);
        });
        if (ModList.get().isLoaded("apotheosis")) check("Apotheosis all-stat bonus excludes TC attributes", () -> {
            var requestedAttributes = new java.util.ArrayList<net.minecraft.world.entity.ai.attributes.Attribute>();
            requestedAttributes.add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
            ForgeRegistries.ATTRIBUTES.getEntries().stream().filter(entry -> entry.getKey().location().getNamespace().equals("thaumcraft"))
                    .forEach(entry -> requestedAttributes.add(entry.getValue()));
            require(requestedAttributes.size() > 1, "No Thaumcraft attributes registered");
            class Bonus extends dev.shadowsoffire.apotheosis.adventure.socket.gem.bonus.special.AllStatsBonus {
                Bonus() { super(new dev.shadowsoffire.apotheosis.adventure.socket.gem.GemClass("verification",
                        java.util.Set.of(dev.shadowsoffire.apotheosis.adventure.loot.LootCategory.byId("helmet"))),
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL, java.util.Map.of(), requestedAttributes); }
                java.util.List<net.minecraft.world.entity.ai.attributes.Attribute> values() { return this.attributes; }
            }
            require(new Bonus().values().equals(java.util.List.of(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)), "All-stats includes Thaumcraft or removes vanilla attributes");
        });
        if (ModList.get().isLoaded("apotheosis")) check("Apotheosis gem assets names and attribute formatting", () -> {
            for(var name:java.util.List.of("arcane","warped")) {
                var stack=new ItemStack(ForgeRegistries.ITEMS.getValue(id("apotheosis:gem")));
                stack.getOrCreateTag().putString("gem","thaumicomniverse:"+name);
                String key=stack.getDescriptionId();
                require(key.equals("item.apotheosis.gem.thaumicomniverse:"+name),"Gem description key: "+key);
                for(var lang:java.util.List.of("zh_cn","en_us"))try(var in=thaumicomniverse.ThaumicOmniverse.class.getResourceAsStream("/assets/thaumicomniverse/lang/"+lang+".json")) {
                    var json=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                    require(json.has(key),"Missing gem translation "+key);
                }catch(java.io.IOException error){throw new AssertionError(error);}
                require(thaumicomniverse.ThaumicOmniverse.class.getResource("/assets/apotheosis/models/item/gems/"+name+".json")!=null,"Missing Apotheosis model location");
                require(thaumicomniverse.ThaumicOmniverse.class.getResource("/assets/thaumicomniverse/textures/item/gems/"+name+".png")!=null,"Missing gem texture");
            }
            for(var amount:new double[]{-.1,.1}) {
                var modifier=new net.minecraft.world.entity.ai.attributes.AttributeModifier(java.util.UUID.randomUUID(),"check",amount,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION);
                for(var entry:ForgeRegistries.ATTRIBUTES.getEntries()) {
                    var id=entry.getKey().location();if(!id.getNamespace().equals("thaumcraft"))continue;
                    var attr=entry.getValue();
                    var component=dev.shadowsoffire.attributeslib.api.IFormattableAttribute.toComponent(attr,modifier,net.minecraft.world.item.TooltipFlag.NORMAL);
                    if(id.getPath().equals("warp"))require(component.getStyle().getColor().equals(net.minecraft.network.chat.TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.DARK_PURPLE)),"Warp color");
                    if(id.getPath().endsWith("vis_cost")) {
                        require(component.getStyle().getColor().equals(net.minecraft.network.chat.TextColor.fromLegacyFormat(amount<0?net.minecraft.ChatFormatting.BLUE:net.minecraft.ChatFormatting.RED)),"Vis cost benefit color");
                        var value=dev.shadowsoffire.attributeslib.api.IFormattableAttribute.toValueComponent(attr,modifier.getOperation(),Math.abs(amount),net.minecraft.world.item.TooltipFlag.NORMAL);
                        require(value.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text && text.getKey().equals("attributeslib.value.percent") && text.getArgs()[0].equals("10"),"Vis cost percentage");
                    }
                }
            }
        });
        check("offline node recovery bounds and fading", () -> {
            var air = TCAspects.catalog().require("aer"); var fire = TCAspects.catalog().require("ignis");
            var tag = new CompoundTag(); tag.putByte("modifier", (byte) -1); tag.putLong("lastActive", 1);
            AspectNbt.write(tag, new AspectAmounts().add(air, 4).add(fire, 8));
            AspectNbt.write(tag, "AspectsBase", new AspectAmounts().add(air, 10).add(fire, 10));
            var result = thaumicomniverse.compat.nodemap.NodeRecoveryProjection.project(tag, 0, 135001, true);
            require(result.total() == 15 && result.capacity() == 20, "Recovery total");
            require(result.aspects().get(air).minimum() == 5 && result.aspects().get(air).maximum() == 7, "Random recovery bounds");
            require(thaumicomniverse.compat.nodemap.NodeRecoveryProjection.project(tag, 0, 135001, false).total() == 12, "Projected online recovery");
            tag.putByte("modifier", (byte) 2);
            require(thaumicomniverse.compat.nodemap.NodeRecoveryProjection.project(tag, 0, Long.MAX_VALUE, true).total() == 12, "Fading node recovered");
        });
        if (ModList.get().isLoaded("thaumicfutures") && ModList.get().isLoaded("tconstruct")) check("thaumium modifier original slot progression", () -> {
            require(thaumicfutures.compat.TinkersCompat.bonusSlots(2, 2) == 2, "Two-part tool");
            require(thaumicfutures.compat.TinkersCompat.bonusSlots(3, 3) == 2, "Three-part tool");
            require(thaumicfutures.compat.TinkersCompat.bonusSlots(4, 4) == 2, "Four-part tool");
            require(thaumicfutures.compat.TinkersCompat.bonusSlots(4, 0) == 0, "No thaumium parts");
        });
        if (ModList.get().isLoaded("thaumicfutures") && ModList.get().isLoaded("botania")) check("mana ink capability conservation", () -> {
            var stack = new ItemStack(ForgeRegistries.ITEMS.getValue(id("mana_inkwell")));
            var item = (thaumicfutures.compat.ManaInkwellItem) stack.getItem();
            var mana = stack.getCapability(vazkii.botania.api.BotaniaForgeCapabilities.MANA_ITEM).orElseThrow(() -> new AssertionError("Missing mana capability"));
            mana.addMana(101); require(item.getInk(stack) == 2, "Ink conversion");
            item.setInk(stack, 1); require(mana.getMana() == 51, "Fractional mana lost");
            mana.addMana(Integer.MAX_VALUE); require(mana.getMana() == 7500, "Overflow");
            require(!mana.canExportManaToItem(ItemStack.EMPTY) && mana.isNoExport(), "Export unexpectedly enabled");
        });
        if (ModList.get().isLoaded("thaumicfutures") && ModList.get().isLoaded("evilcraft")) check("blood scribing fluid capability conservation", () -> {
            var stack = new ItemStack(ForgeRegistries.ITEMS.getValue(id("veined_scribing_tools")));
            var item = (thaumicfutures.compat.VeinedScribingToolsItem) stack.getItem();
            var fluid = stack.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM).orElseThrow(() -> new AssertionError("Missing fluid capability"));
            require(fluid.fill(new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 100),
                    net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) == 0, "Water accepted as ink");
            var blood = ForgeRegistries.FLUIDS.getValue(id("evilcraft:blood"));
            fluid.fill(new net.minecraftforge.fluids.FluidStack(blood, 105), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            require(item.getInk(stack) == 10, "Blood conversion");
            item.setInk(stack, 9); require(fluid.getFluidInTank(0).getAmount() == 95, "Fractional blood lost");
        });
        if (ModList.get().isLoaded("allthemodium")) check("certificate includes all registered aspects", () -> {
            var json = com.google.gson.JsonParser.parseString("{\"components\":[],\"optional_components\":[{\"mod\":\"absent_mod\",\"component\":{}}]}").getAsJsonObject();
            var result = thaumicomniverse.recipe.CertificateRecipeSerializer.expand(json);
            require(result.getAsJsonArray("components").isEmpty() && !result.has("optional_components"), "Missing addon component retained");
            require(result.getAsJsonObject("aspects").size() == TCAspects.catalog().all().size(), "Missing registered aspect");
            for (var aspect : TCAspects.catalog().all()) require(result.getAsJsonObject("aspects").get(aspect.tag()).getAsInt()
                    == (aspect.isPrimal() ? 640 : 64), "Certificate cost: " + aspect.tag());
        });
    }
    static void dragonRecipe(MinecraftServer server) {
        var recipe = server.getRecipeManager().byKey(id("thaumicomniverse_verification:dragon_soul")).orElseThrow();
        var ingredients = recipe.getIngredients();
        require(ingredients.size() == 1 && ingredients.get(0).test(new ItemStack(ForgeRegistries.ITEMS.getValue(id("thaumaturge_certificate"))))
                && !ingredients.get(0).test(new ItemStack(ForgeRegistries.ITEMS.getValue(id("mysticalagradditions:dragon_scale")))), "KubeJS recipe replacement order");
    }
}
