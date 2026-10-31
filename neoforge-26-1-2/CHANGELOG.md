# Changelog

## 26.0.1 (Minecraft 26.1.2)

### Fixes
- Restore virtual-fluid recipe and usage lookup with JEI's modern ingredient commands, including filled-bucket recipes and fuel uses.
- Preserve keyboard-only fluid lookup, JEI ingredient selection and visibility checks, and the original lookup when no replacement focus is available.
- Do not offer virtual fluid for recipe transfer or missing-ingredient previews when the ingredient rejects the virtual bucket, including component-sensitive buckets.

### Dependencies
- Require JEI `29.46.0.108` or newer, the first 26.1.2 release with the modern ingredient command input flow.

## 26.0.0 (Minecraft 26.1.2)

- Hide Extended Terminal crafting output when a blacklisted recipe uses virtual fluid, while keeping real-bucket recipes visible.

- Open the virtual-fluid blacklist category with the usage key on the Terminal Fluid Interact Card in JEI or REI.
- Fixed returned virtual fluids remaining in the client offhand after closing a terminal, allowing duplication in creative mode.
- Added English/Korean GuideME configuration pages and a read-only virtual-fluid blacklist category in JEI/REI, using the local common config and original filled-bucket ingredients.
- Enforced the virtual-fluid recipe blacklist for manual crafting and crafting pattern encoding, including cached recipes, and synchronized the server policy to terminal recipe previews.
- Added optional FastSuite cached recipe support for virtual-fluid bucket matching and blacklist enforcement.
- Allowed unlisted shapeless recipes, including tagged bucket ingredients, to match virtual fluids using their contained fluid.
- Virtual fluids need a full bucket (1,000 mB) to satisfy bucket ingredients; 999 mB does not count as a bucket.

- Ported fluid crafting, virtual fluid items, and terminal configuration to Minecraft 26.1.2 and Java 25.
- Updated to NeoForge 26.1.2.100, requiring at least 26.1.2.99.
- Updated JEI integration to 29.33.0.87 and declared it as the minimum compatible version.
- Added REI keyboard fluid lookup from recipe entries and inventory slots, plus optional ExtendedTerminal JEI fluid-aware crafting transfer; EMI remains unsupported.
- Kept AE2 fluid-key recipe lookup keyboard-only, preserving left-click bucket filling.
- Fixed virtual-fluid component equality so separately created stacks with the same fluid, amount, and capacity share the same AE2 item key.
- Fixed invisible virtual fluids in inventory slots and on the cursor by reusing AE2's fluid sprite renderer in the item model.
- Fixed closing a pattern encoding terminal returning ghost virtual fluids to ME storage and clearing the pattern grid.

### Dependencies
- Updated Myotus to stable `26.0.0` from Maven Central.
