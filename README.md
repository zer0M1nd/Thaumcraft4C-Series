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

The Latest column shows the newest published version. Three-part versions are stable releases and four-part versions are development builds.

### Base mod

| | Project | Based on | What it does | Requires | Latest |
| :-: | --- | --- | --- | --- | --- |
| 🟢 | [**Thaumcraft 4: Continuation**](TC4/) | Thaumcraft 4 by Azanor | Aspects, research, alchemy, infusion, wands and golems. The heart of the series. | TerraBlender, Curios API | <!--v:thaumcraft-->4.3 |

### Original addons

Addons created for this series.

| | Project | What it adds | Requires | Latest |
| :-: | --- | --- | --- | --- |
| 🟢 | [**Thaumic Omniverse**](ThaumicOmniverse/) | Equipment tuning, essentia routing, modern mod integrations and arcane airships. | Base mod. Optional: Create | <!--v:thaumicomniverse-->10.0 |

### Remade addons

Addons that recreate earlier mods.

| | Project | Based on | What it adds | Requires | Latest |
| :-: | --- | --- | --- | --- | --- |
| 🟢 | [**Thaumic Futures**](ThaumicFutures/) | Thaumcraft 5 and 6 by Azanor | Devices, tools and mechanics brought back from Thaumcraft 5 and 6. | Base mod | <!--v:thaumicfutures-->6.2 |
| 🟢 | [**Thaumic Tinkerer**](ThaumicTinkerer/) | Thaumic Tinkerer by Vazkii, pixlepix and nekosune | Specialized tools, enchantments and late-game Ichor equipment. | Base mod | <!--v:thaumic_tinkerer-->2.6 |
| 🟢 | [**Automagy**](Automagy/) | Automagy by Tuhljin and Draanor | Item sorting, redstone control and essentia automation. | Base mod | <!--v:automagy-->1.0 |
| 🟢 | [**Forbidden Magic**](ForbiddenMagic/) | Forbidden Magic by SpitefulFox | Deadly-sin research with forbidden wand parts, tools and enchantments. | Base mod. Optional: Blood Magic, Botania | <!--v:forbidden_magic-->1.0 |
| 🟢 | [**Thaumic Energistics**](ThaumicEnergistics/) | Thaumic Energistics by Nividica | Essentia storage and transport on AE2 ME networks. | Base mod, Applied Energistics 2 | <!--v:thaumicenergistics-->1.2 |
| 🟢 | [**Thaumic Horizons**](ThaumicHorizons/) | Thaumic Horizons by Kentington | Creature infusion, soul experiments and special lenses. | Base mod | <!--v:thaumichorizons-->1.2 |
| 🟢 | [**Thaumic Bases**](ThaumicBases/) | Thaumic Bases by Modbder | Magical crops, materials, tools and workshop devices. | Base mod | <!--v:thaumicbases-->1.4 |
| 🟡 | [**Electro-Magic Tools**](ElectroMagicTools/) | Electro-Magic Tools by fewizz and TheSilentium | Electrically powered thaumic tools, armor and machines. | Base mod, Industrial Craft 2 | <!--v:emt-->0.1.14 |
| 🟡 | [**Gadomancy**](Gadomancy/) | Gadomancy by makeo and HellFirePvP | More golems, node devices and infusion automation. | Base mod | <!--v:gadomancy-->0.1.16 |
| 🟡 | [**Nodal Mechanics**](NodalMechanics/) | Nodal Mechanics by Shukaro | Craft and grow your own aura nodes. | Base mod | <!--v:nodalmechanics-->0.1.7 |
| 🟡 | [**Tainted Magic**](TaintedMagic/) | Tainted Magic by John Yorke (tadashi_3) | Taint and warp research, shadowmetal gear and wand foci. | Base mod | <!--v:taintedmagic-->0.1.14 |
| 🟡 | [**Thaumic Infusion**](ThaumicInfusion/) | Thaumic Infusion by DrunkMafia | Infuse blocks with aspects to give them magical effects. | Base mod | <!--v:thaumicinfusion-->0.1.11 |
| 🟡 | [**Warp Theory**](WarpTheory/) | Warp Theory by Shukaro and thegreatunclean | More warp events and afflictions. | Base mod | <!--v:warptheory-->0.1.9 |
| 🟡 | [**Witching Gadgets**](WitchingGadgets/) | Witching Gadgets by BluSunrize | Cloaks, accessories, tools and magical machines. | Base mod | <!--v:witchinggadgets-->0.1.27 |
| 🔴 | [**Thaumic Concilium**](ThaumicConcilium/) | Thaumic Concilium by Ilya3000 and BreadX22 | Rift and incorporeal research, foci, bracelets and new creatures. | Base mod, Forbidden Magic, Thaumic Tinkerer | <!--v:thaumicconcilium-->0.1.8 |
| 🔴 | [**Thaumic Dyes**](ThaumicDyes/) | Thaumic Dyes by KryptonCaptain and Amerem | Dyeable thaumaturge armor, runic gear and masks. | Base mod | <!--v:thaumicdyes-->0.1.7 |
| 🔴 | [**Infusion Brewing**](InfusionBrewing/) | Infusion Brewing by mariri | Fluid potions, infusion-boosted effects and a potion focus. | Base mod | <!--v:infusionbrewing-->0.1.4 |
| 🔴 | [**Arcane Arteries**](ArcaneArteries/) | Arcane Arteries by jordsta95 and MaxIsH0t | Sacrificial tools, slates and runes that bridge to Blood Magic. | Base mod, Blood Magic | <!--v:arcanearteries-->0.1.4 |
| 🔴 | [**Arcane Engineering**](ArcaneEngineering/) | Arcane Engineering by Eris Izalith (cadaverous_queen) | Upgradable wands, thaumic drill heads and biodiesel boots. | Base mod, Immersive Engineering | <!--v:arcane_engineering-->0.1.4 |
| 🔴 | [**Advanced Thaumaturgy**](AdvancedThaumaturgy/) | Advanced Thaumaturgy by Lycaon; Advanced Thaumaturgy 2 by MonzterAssassin, Nekosune and ToFam | Mercurial wand upgrades, node modification and essentia engines. | Base mod | <!--v:advancedthaumaturgy-->0.1.4 |
| 🔴 | [**Forgotten Relics**](ForgottenRelics/) | Forgotten Relics by Integral (Aizistral) | Relics, accessories and spellbooks that blend in Botania. | Base mod, Botania | <!--v:forgottenrelics-->0.1.4 |
| 🔴 | [**Thaumic Exploration**](ThaumicExploration/) | Thaumic Exploration by Flaxbeard and gaeakat (nekosune) | Meditation vat, linked containers, wand parts and magic boots. | Base mod | <!--v:thaumicexploration-->0.1.5 |
| 🔴 | [**Technomancy**](Technomancy/) | Technomancy by Democretes, theflogat and Mordenkainen | Essentia and energy machines, node manufacturing and rituals. | Base mod. Optional: Botania, Blood Magic | <!--v:technomancy-->0.1.4 |
| 🔴 | [**The Heartwork**](Heartwork/) | The Heartwork by Mysterious Mountain Smith | Sabers, crops and dishes, and named blades. | Base mod. Optional: SlashBlade Resharped | <!--v:heartwork-->0.1.3 |

### Standalone tools

| | Project | What it does | Latest |
| :-: | --- | --- | --- |
| 🟢 | [**ThaumcraftUpdater**](ThaumcraftUpdater/) | Updates the installed series mods at launch. Not a Thaumcraft addon. | <!--v:thaumcraftupdater-->1.0 |
| 🟢 | [**ThaumcraftResourceAdapter**](ThaumcraftResourceAdapter/) | Converts resource packs for older Thaumcraft versions for use in 1.20.1. Client-side. | <!--v:thaumcraftresourceadapter-->1.1.1 |

## Building

Every project builds on its own with the Gradle wrapper in its folder. See the README of the project you want to build for the required tools, the dependency preparation step and the exact commands.

## Maintaining and extending

- This project is developed mainly by generative AI. We recommend maintaining it with an agent together with [MCModding](https://github.com/zer0M1nd/MCModding).
- You are welcome to build addons on top of this version. If an API your addon needs is missing, please ask for it in [Issues][issues]. We look forward to an ecosystem that needs no Mixins internally.
- Recommended: [Thaumifier](https://github.com/zer0M1nd/Thaumifier) helps an AI write text in the style of the Thaumonomicon.

## License

The series is released under the TC4Revival Noncommercial License 1.0, which is the PolyForm Noncommercial License 1.0.0 with additional conditions on resale and paid modpacks. See [LICENSE](LICENSE) and [NOTICE](NOTICE) here, and the license files and notices in each project folder. Third-party material keeps its own terms.

[site]: http://tc4c.zer0m1nd.me/en/index.html
[issues]: https://github.com/zer0M1nd/Thaumcraft4C-Series/issues
