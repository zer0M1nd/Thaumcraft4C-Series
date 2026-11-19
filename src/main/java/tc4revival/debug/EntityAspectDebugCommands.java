package tc4revival.debug;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.TCAspects;
import tc4revival.aspect.entity.EntityAspects;
import tc4revival.aspect.entity.EntityFacts;

 class EntityAspectDebugCommands {
    private EntityAspectDebugCommands() {}

    static ArgumentBuilder<CommandSourceStack, ?> tree() {
        return Commands.literal("entity_aspects")
                .then(Commands.literal("verify").executes(context -> {
                    var resolver = EntityAspects.resolver();
                    var powered = resolver.resolve(new EntityFacts("minecraft:creeper", null, false, false, true, true, false)).orElseThrow();
                    if (resolver.ruleCount() != 48 || powered.aspects().total() != 9
                            || !powered.legacyIdentity().equals("Creeperpowered1FLASHINGPOWERED")
                            || resolver.resolve(new EntityFacts("minecraft:warden", null, false, false, false, false, false)).isPresent()) {
                        context.getSource().sendFailure(Component.literal("Entity aspect data check failed"));
                        return 0;
                    }
                    context.getSource().sendSuccess(() -> Component.literal("Entity aspect checks passed: 48 rules; powered-creeper override; legacy identity; new-entity exclusion."), false);
                    return 1;
                }))
                .then(Commands.argument("targets", EntityArgument.entities()).executes(context -> {
                    var targets = EntityArgument.getEntities(context, "targets");
                    for (var entity : targets) {
                        var result = EntityAspects.resolve(entity);
                        String type = String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()));
                        String description = result.map(value -> "legacy=" + value.legacyIdentity() + "; hash=" + value.scanHash()
                                + "; " + value.aspects().view().entrySet().stream().map(entry -> entry.getKey().tag() + "=" + entry.getValue()).toList())
                                .orElse("no assigned aspects");
                        context.getSource().sendSuccess(() -> Component.literal(type + ": " + description), false);
                    }
                    return targets.size();
                }));
    }
}
