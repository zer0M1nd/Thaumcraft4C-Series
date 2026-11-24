# Thaumcraft 4: Continuation

[简体中文](README.zh-CN.md)

Scan the world to discover aspects, then put your research to work with wands, alchemy, infusion and golems.

A Chaos Matrix lets the Thaumatorium craft from its current catalyst and essentia. It stores up to 100 points across all aspects. With x matrices, intake runs at 0.5 + 0.5x times the base rate. Soul fire or a lit soul campfire supplies an independent 1.5× bonus. An attached Chaos Matrix takes precedence over Mnemonic Matrices.

Thaumium and Void Chests hold 54 and 81 stacks respectively. Placing two of the same kind together doubles their capacity. Vis from a nearby relay powers Repair-enchanted items inside either chest. Void tools also repair themselves inside a Void Chest. Both chest recipes unlock through secondary research.

Nugget transmutation uses one nugget to make three. The default cost is 3 Metallum. Set `extra_nugget_transmutation_cost = false` in the world’s server configuration to restore the cost of 2. The same setting applies to supported addon nuggets.

## Installation

Use Minecraft 1.20.1 with Forge 47.4.0 or later in the 47.x series.

Required mods include TerraBlender `[3.0.1.7,3.1)`, Curios API `[5.14.1,6)`.

Optional integrations include Just Enough Items `[15.20.0,16)`, Applied Energistics 2, JECharacters, AgriCraft and Patchouli. [Forge dependency declarations](src/main/resources/META-INF/mods.toml)

In JEI or an AE2 storage terminal, prefix an aspect condition with `!` or `！`. Separate it from item names or native filters with a space. For example, `@minecraft !ordo` and `!ordo @minecraft` both find Minecraft items containing Ordo. Multiple aspect conditions separated by spaces must all match.

`!1<=ordo<=5` finds items with one to five Ordo, `!ordo&types=1` finds items containing only Ordo, and `!(aer|ignis)&-terra&sum>=4` requires Aer or Ignis, excludes Terra and requires at least four aspect points in total. Comparisons accept `=`, `<`, `>`, `<=` and `>=`. A name alone requires at least one point. Exact names take priority, followed by Latin name prefixes and substrings of translated names or Chinese pinyin. Pinyin requires JECharacters.

Names are case-insensitive. Negation (`-`) binds first, then AND (`&`), then OR (`|`). Use parentheses to group conditions. An incomplete expression or an unknown aspect gives an empty list.

## Build

Install JDK 17 and Python 3.8 or later and point `JAVA_HOME` at the JDK. Run these commands from the repository root. The wrapper uses Gradle 8.8 and compiles against Forge 47.4.23.

Windows (PowerShell)

```powershell
py -3 dependencies/prepare.py
.\gradlew.bat assemble
```

Linux / macOS

```sh
python3 dependencies/prepare.py
./gradlew assemble
```

The installable JAR is `build/libs/[TC4Continuation]Thaumcraft-<version>.jar`. The `-sources.jar` contains source code.

The preparation script checks each local input against its SHA-256. To use files you already have, append `--from-dir <folder>`. [Pinned build inputs](dependencies/artifacts.json)

The full regression suite also reads the original Thaumcraft archive and the legacy reference catalogs.
