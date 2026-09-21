# Changelog

## 15.1.0 (Minecraft 1.20.1)

### Fixes
- Fixed virtual creosote buckets not matching Immersive Engineering's fluid ingredients, including treated wood crafting. Blacklisted recipes still require real buckets.
- Fixed REI recipe/usage shortcuts for virtual fluids and added filled-bucket results when looking up AE2 fluid keys, respecting the recipe-viewer config.
- Fixed the virtual-fluid blacklist's EMI rendering error caused by a zero-duration animated arrow.
- Fixed returned virtual fluids remaining in the client offhand after closing a terminal, allowing duplication in creative mode.
- Fixed FastSuite/PolyEng bypassing the virtual fluid recipe blacklist during manual crafting, while keeping real filled buckets valid.
- Rechecked the blacklist before using cached crafting and pattern recipes.
- Fixed a Forge production startup crash caused by the missing Minecraft method mapping in the virtual fluid blacklist sync mixin.
- Fixed virtual fluid ghost ingredients being refunded from AE2 fake pattern slots when closing terminals.
- Blocked AE2FCT virtual fluid usage for recipes listed in the virtual fluid recipe blacklist, while keeping real filled buckets valid.

### Features
- Open the virtual-fluid blacklist category with the usage key on the Terminal Fluid Interact Card in JEI, EMI, or REI.
- Added common QoL config.
    - `QoL`: allows fluid terminal interaction without the Terminal Fluid Interact Card.
- Added JEI/EMI/REI virtual fluid recipe lookup, configurable from the Myotus terminal config tab.
- Added a read-only JEI/EMI/REI category for recipes listed in `recipe_transfer.virtualFluidRecipeBlacklist`.

### Versions
- Updated Myotus to `15.1.0`.
