# Changelog

Loader-specific release notes live in each module's `CHANGELOG.md`; the release workflow uses the selected module file.

## Unreleased (all supported versions)

### Fixes
- Do not offer virtual fluid for recipe transfer or missing-ingredient previews when the ingredient rejects the virtual bucket, including NBT/component-sensitive buckets.

## Unreleased (Minecraft 1.20.1)

### Fixes
- Fixed virtual XP buckets not crafting Mob Grinding Utils chicken feed used to obtain rotten eggs. Fluid tags, bucket amounts, and the recipe blacklist remain enforced.
- Require exactly 1000 mB for ordinary virtual-bucket ingredients, matching the newer versions.

## 19.1.2 (Minecraft 1.21.1)

### Fixes
- Fixed a crash with newer JEI versions when loading fluid recipe and usage lookup.

### Dependencies
- Updated JEI to `19.57.0.446`; requires JEI `19.55.0.432` or newer when installed.

## 19.1.1 (Minecraft 1.21.1)

### Features
- Added common QoL config.
    - `QoL`: allows fluid terminal interaction without the Terminal Fluid Interact Card.
- Added JEI/EMI virtual fluid recipe lookup, configurable from the Myotus terminal config tab.

### Fixes
- Fixed virtual fluid ghost ingredients being returned from AE2 pattern slots when closing a terminal.
- Enforced the virtual-fluid recipe blacklist for recipe transfer, manual crafting, and crafting pattern encoding while leaving real buckets valid.
- Fixed FastSuite recipe lookup paths bypassing the virtual-fluid recipe blacklist when matching cached recipes.

### Dependencies
- Updated Myotus to `19.1.1`.

## 26.0.0 (Minecraft 26.1.2)

### Compatibility
- Updated Myotus to stable `26.0.0` from Maven Central.
- Set the minimum compatible JEI version to `29.33.0.87`.

## 15.1.0 (Minecraft 1.20.1)

### Dependencies
- Updated Myotus to `15.1.0`.
