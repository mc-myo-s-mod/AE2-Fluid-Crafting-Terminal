<p align="center">
    <img width="200" src="readme/AE2FCT.png" alt="logo">  
</p>
<h1 align="center">AE2 Fluid Crafting Terminal</h1>
<p align="center">
    <a href="https://www.curseforge.com/minecraft/mc-mods/ae2-fluid-crafting-terminal" target="_blank">
      <img alt="CurseForge Downloads" src="https://img.shields.io/curseforge/dt/1476701?style=flat-square&logo=curseforge&color=f16436">
    </a>
    <a href="https://modrinth.com/mod/ae2-fluid-crafting-terminal" target="_blank">
      <img alt="Modrinth Downloads" src="https://img.shields.io/modrinth/dt/ae2-fluid-crafting-terminal?style=flat-square&logo=modrinth&color=00af5c">
    </a>
  <img alt="GitHub License" src="https://img.shields.io/github/license/mc-myo-s-mod/AE2FluidCraftingTerminal?style=flat-square">
</p>

## Feature
AE2 Fluid Crafting Terminal is an addon for Applied Energistics 2 that simplifies and enhances the fluid crafting experience. It adds fluid-aware support to existing AE2 terminals and handles fluids as naturally as items within your ME network.

## Repository Layout

This repository uses one multi-module layout for all supported Minecraft versions:

- `common`: shared source and resource overlay. Resource files in loader modules override common files with the same path.
- `forge-1-20-1`: Minecraft 1.20.1 Forge, Java 17.
- `neoforge-1-21-1`: Minecraft 1.21.1 NeoForge, Java 21.
- `neoforge-26-1-2`: Minecraft 26.1.2 NeoForge, Java 25, built through its own Gradle 9 wrapper.

Run the root wrapper with Java 21 for 1.20.1 and 1.21.1. The modules still compile with their own Java toolchains:

```bash
./gradlew :forge-1-20-1:build :neoforge-1-21-1:build --console=plain
```

Use either the root bridge or the native wrapper for 26.1.2. Do not run the root Gradle 8 wrapper on Java 25; the 26 module uses its own Gradle 9 wrapper and Java 25 toolchain:

```bash
./gradlew :neoforge-26-1-2:build --console=plain
./neoforge-26-1-2/gradlew --project-dir neoforge-26-1-2 build --console=plain
```

CI runs on pushes to `master`, pull requests targeting `master`, and manual dispatch. It builds all three Minecraft versions; AE2FCT GameTests are also required. New commits cancel superseded CI runs.

To publish, merge the changes into `master`, create a tag `v<minecraft_version>-<mod_version>` matching that module's `gradle.properties`, then publish its GitHub Release. Pushing a tag alone does not publish. The workflow checks that the tag belongs to `master`, builds only the selected module, attaches the JAR to the release, and publishes to CurseForge and Modrinth using `CURSEFORGE_TOKEN` and `MODRINTH_TOKEN`. Mod and Myotus `-SNAPSHOT` versions are rejected. Forge releases use the bundled `-all.jar` artifact, uploaded with the normal JAR filename.

Tags matching the current module properties and their Myotus requirements are:

| Target | Release tag | Myotus |
| --- | --- | --- |
| Forge 1.20.1 | `v1.20.1-15.1.0` | `15.1.0` |
| NeoForge 1.21.1 | `v1.21.1-19.1.0` | `19.1.1` |
| NeoForge 26.1.2 | `v26.1.2-26.0.0` | `26.0.0` |

The Myotus versions above are resolved from Maven Central by the module builds.
If re-releasing an existing 1.20.1 or 1.21.1 line, including `v1.21.1-19.1.0`, increment that module's `mod_version` and publish a new tag instead of reusing an already-published tag.

## License
code: LGPL 3.0
