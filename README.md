<p align="center">
  <img src=".github/assets/logo-en.png" alt="Thaumcraft 4: Continuation" width="820">
</p>

# Thaumcraft 4: Continuation (TC4C) Series

**English** | [简体中文](README.zh-CN.md)

Thaumcraft 4 and the addons that grew up around it, remade for Minecraft 1.20.1 on Forge. This repository holds the public source of the whole series: the base mod, 25 addons and two standalone tools, each in its own top-level folder.

## Project statement

This project is a remake, for Minecraft 1.20.1, of the Minecraft mod Thaumcraft created by Azanor and of its addons. Its main gameplay framework is based on Thaumcraft 4 for Minecraft 1.7.10 and the ecosystem around it.

The author is not affiliated with Mojang AB, Azanor, Team CoFH, or the authors of the other original addons.

This project does not distribute any original asset files without authorization. To use it properly, the corresponding legacy mods must be placed in the game directory so that their assets can be read.

AI disclosure: this project is developed with generative AI. Its original content includes assets created with generative AI.

## Download and play

- **Mods and ready-to-play modpacks:** [tc4c.zer0m1nd.me][site]
- **Bug reports and suggestions:** [GitHub Issues][issues]
- **Community (Chinese):** QQ group 1125142992

## Getting started

- The series targets Minecraft 1.20.1 with Forge 47.x and Java 17. Each project's README states its exact minimum Forge version.
- The base mod requires [TerraBlender](https://www.curseforge.com/minecraft/mc-mods/terrablender) and [Curios API](https://www.curseforge.com/minecraft/mc-mods/curios). Every addon requires the base mod; anything further is listed in the tables below.
- Original textures, sounds and legacy language files are read from the `thaumcraft` folder of your game directory. On first launch the client checks that folder, tries to fetch the legacy mod files it still needs from their original download locations, verifies them and extracts only the required assets. If that fails, the same screen tells you which file to download and where to put it. Do not put legacy JARs in `mods`; they are only read as data. A dedicated server does not need them.
- The two standalone tools work without the base mod.

## Projects

Each entry links to its folder, which has its own README, license notices and build files.

Stability at a glance:

| | Meaning |
| :-: | --- |
| 🟢 | Played and tested by many players. Already offers a good gameplay experience. |
| 🟡 | Tested to some extent, with a small amount of real play. Bugs are possible, but it is playable overall. |
| 🔴 | Development or porting is initially complete, but it has not been widely tested. Not recommended for regular play yet. |

### Base mod

| | Project | Based on | What it does | Requires |
| :-: | --- | --- | --- | --- |
| 🟢 | [**Thaumcraft 4: Continuation**](TC4/) | Thaumcraft 4 by Azanor | The heart of the series. Scan the world to discover aspects, then put your research to work with wands, alchemy, infusion and golems. | TerraBlender, Curios API |

### Addons

| | Project | Based on | What it adds | Also requires |
| :-: | --- | --- | --- | --- |
| 🟢 | [**Thaumic Omniverse**](ThaumicOmniverse/) | Original to this series | Tune magical equipment, route essentia and use Create to build arcane airships that carry cargo along scheduled routes. Home of the series' integrations with modern mods. | Optional: Create |
| 🟢 | [**Thaumic Futures**](ThaumicFutures/) | Thaumcraft 5 and 6 by Azanor | Brings the grappling and building foci of Thaumcraft 5 and a selection of Thaumcraft 6 devices, tools and mechanics back to Thaumcraft 4, along with classic cross-mod recipes. | |
| 🟢 | [**Thaumic Tinkerer**](ThaumicTinkerer/) | Thaumic Tinkerer by Vazkii, pixlepix and nekosune | Craft specialized tools and enchantments, then progress through infusion to unlock Ichor equipment. | |
| 🟢 | [**Automagy**](Automagy/) | Automagy by Tuhljin and Draanor | Build magical sorting networks, control redstone signals and automate essentia handling with golems and enchanted devices. | |
| 🟢 | [**Forbidden Magic**](ForbiddenMagic/) | Forbidden Magic by SpitefulFox | Investigate the seven deadly sins and turn forbidden knowledge into wand parts, tools and enchantments. | Optional: Blood Magic, Botania |
| 🟢 | [**Thaumic Energistics**](ThaumicEnergistics/) | Thaumic Energistics by Nividica | Store and transport essentia through an Applied Energistics 2 ME network to supply alchemy and infusion. | Applied Energistics 2 |
| 🟢 | [**Thaumic Horizons**](ThaumicHorizons/) | Thaumic Horizons by Kentington | Infuse living creatures, experiment with souls and craft lenses that reveal new ways to see the world. | |
| 🟢 | [**Thaumic Bases**](ThaumicBases/) | Thaumic Bases by Modbder | Grow magical crops and put them to use alongside new materials, wands and workshop devices. | |
| 🟡 | [**Electro-Magic Tools**](ElectroMagicTools/) | Electro-Magic Tools by fewizz and TheSilentium | Combine electricity and vis to power tools, armor and machines that bridge industry and thaumaturgy. | Industrial Craft 2 |
| 🟡 | [**Gadomancy**](Gadomancy/) | Gadomancy by makeo and HellFirePvP | Build specialized golems and devices to manipulate aura nodes and automate infusion. | |
| 🟡 | [**Nodal Mechanics**](NodalMechanics/) | Nodal Mechanics by Shukaro | Catalyze aspects into an aura node and shape the supply of vis for your workshop. | |
| 🟡 | [**Tainted Magic**](TaintedMagic/) | Tainted Magic by John Yorke (tadashi_3) | Study taint and warp to forge shadowmetal gear and wield new wand foci. | |
| 🟡 | [**Thaumic Infusion**](ThaumicInfusion/) | Thaumic Infusion by DrunkMafia | Infuse ordinary blocks with aspects to give your buildings and contraptions magical properties. | |
| 🟡 | [**Warp Theory**](WarpTheory/) | Warp Theory by Shukaro and thegreatunclean | Gain warp from forbidden research and face new apparitions, afflictions and unpredictable events. | |
| 🟡 | [**Witching Gadgets**](WitchingGadgets/) | Witching Gadgets by BluSunrize | Craft magical cloaks, accessories and machines for travel, resource processing and work with aura nodes. | |
| 🔴 | [**Thaumic Concilium**](ThaumicConcilium/) | Thaumic Concilium by Ilya3000 and BreadX22 | Study rifts and incorporeal beings to craft foci, bracelets and devices for solo or cooperative play. | Forbidden Magic, Thaumic Tinkerer |
| 🔴 | [**Thaumic Dyes**](ThaumicDyes/) | Thaumic Dyes by KryptonCaptain and Amerem | Dye and wash thaumaturge armor, then upgrade it with runic protection and specialized masks. | |
| 🔴 | [**Infusion Brewing**](InfusionBrewing/) | Infusion Brewing by mariri | Brew potions into fluids, strengthen them through infusion and channel their effects through baubles and a wand focus. | |
| 🔴 | [**Arcane Arteries**](ArcaneArteries/) | Arcane Arteries by jordsta95 and MaxIsH0t | Craft sacrificial tools, slates and runes that connect thaumaturgy to Blood Magic's altar progression. | Blood Magic |
| 🔴 | [**Arcane Engineering**](ArcaneEngineering/) | Arcane Engineering by Eris Izalith (cadaverous_queen) | Upgrade treated-wood wands, fit thaumic drill heads and fuel biodiesel boots to take flight. | Immersive Engineering |
| 🔴 | [**Advanced Thaumaturgy**](AdvancedThaumaturgy/) | Advanced Thaumaturgy by Lycaon; Advanced Thaumaturgy 2 by MonzterAssassin, Nekosune and ToFam | Fit a mercurial wand with crystal upgrades, reshape aura nodes and turn essentia into power. | |
| 🔴 | [**Forgotten Relics**](ForgottenRelics/) | Forgotten Relics by Integral (Aizistral) | Uncover lost research and combine thaumaturgy with Botania to craft relics, weapons and spellbooks. | Botania |
| 🔴 | [**Thaumic Exploration**](ThaumicExploration/) | Thaumic Exploration by Flaxbeard and gaeakat (nekosune) | Meditate in an enchanted vat, link distant inventories and craft wands and boots with new abilities. | |
| 🔴 | [**Technomancy**](Technomancy/) | Technomancy by Democretes, theflogat and Mordenkainen | Combine essentia and energy to build machines that purify ores, manufacture nodes and fuel rituals. | Optional: Botania, Blood Magic |
| 🔴 | [**The Heartwork**](Heartwork/) | The Heartwork by Mysterious Mountain Smith | Forge sabers, grow crops for new dishes and use infusion to unlock named blades and their special moves. | Optional: SlashBlade Resharped |

### Standalone tools

| | Project | What it does |
| :-: | --- | --- |
| 🟢 | [**ThaumcraftUpdater**](ThaumcraftUpdater/) | Lets you choose which updates to apply to the installed series mods before Forge loads them. A launch-time tool, not a Thaumcraft addon. |
| 🟢 | [**ThaumcraftResourceAdapter**](ThaumcraftResourceAdapter/) | Converts local resource packs made for older Thaumcraft versions and addons into packs you can enable in Minecraft 1.20.1. Client-side. |

## Building

Every project builds on its own with the Gradle wrapper in its folder. See the README of the project you want to build for the required tools, the dependency preparation step and the exact commands.

## Maintaining and extending

- This project is developed mainly by generative AI. We recommend maintaining it with an agent together with [MCModding](https://github.com/zer0M1nd/MCModding).
- You are welcome to build addons on top of this version. If an API your addon needs is missing, please ask for it in [Issues][issues]. We look forward to an ecosystem that needs no Mixins internally.
- Recommended: [Thaumifier](https://github.com/zer0M1nd/Thaumifier) helps an AI write text in the style of the Thaumonomicon.

## License

The series is released under the TC4Revival Noncommercial License 1.0, which is the PolyForm Noncommercial License 1.0.0 with additional conditions on resale and paid modpacks. See [LICENSE](LICENSE) and [NOTICE](NOTICE) here, and the license files and notices in each project folder. Third-party material keeps its own terms.

[site]: http://tc4c.zer0m1nd.me
[issues]: https://github.com/zer0M1nd/Thaumcraft4C-Series/issues
