# Prismatic Arsenal (Minecraft Java 1.20.1, Fabric)

20 new ores plus Prismatic, each with full armor, tools, a sword, spear, mace and trident, a Tool Longbow that fires the weapon in your offhand, mace upgrades and stack-to-64 for every item without durability.

## Getting the .jar (no software needed)

1. Push this repository to GitHub.
2. Open the **Actions** tab. The **Build** workflow runs on every push (or click **Run workflow**).
3. When it finishes, open the run and download **prismatic-arsenal-1.20.1** under *Artifacts*.
4. Unzip it and put `prismatic-arsenal-1.0.0.jar` (not the `-sources` one) in your `.minecraft/mods` folder.

## Playing

Needs **Fabric Loader** for 1.20.1 and **Fabric API** for 1.20.1 in the same mods folder.

## Building locally

Install Java 17 and Gradle 8.8, then run `gradle build`. The jar ends up in `build/libs/`.

## Layout

- `src/main/java/com/prismaticarsenal/`: mod code
- `src/main/resources/`: `fabric.mod.json`, the mixin config, the mod icon and the item names (`lang/en_us.json`)
- `src/main/packed/`: one zip per ore set (textures, models, recipes, loot tables, ore generation) plus `extras.zip` for the vanilla mace upgrades, the Tool Longbow and mining tags. That's about 1,100 small files packed into 22 so the repo stays under GitHub's 100-file upload limit. The build unpacks them into the mod automatically, so don't unzip them in the repo.
- `.github/workflows/build.yml`: GitHub Actions build
