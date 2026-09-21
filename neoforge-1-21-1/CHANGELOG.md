# Changelog

## 19.1.1 (Minecraft 1.21.1)

### Features
- Open the virtual-fluid blacklist category with the usage key on the Terminal Fluid Interact Card in JEI, EMI, or REI.
- Added common QoL config.
    - `QoL`: allows fluid terminal interaction without the Terminal Fluid Interact Card.
- Added JEI/EMI virtual fluid recipe lookup, configurable from the Myotus terminal config tab.

### Fixes
- Fixed returned virtual fluids remaining in the client offhand after closing a terminal, allowing duplication in creative mode.
- Fixed virtual fluid ghost ingredients being returned from AE2 pattern slots when closing a terminal.
- Enforced the virtual-fluid recipe blacklist for recipe transfer, manual crafting, and crafting pattern encoding while leaving real buckets valid.
- Fixed FastSuite recipe lookup paths bypassing the virtual-fluid recipe blacklist when matching cached recipes.

### Dependencies
- Updated Myotus to `19.1.1`.
