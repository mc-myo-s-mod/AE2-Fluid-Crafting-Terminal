---
navigation:
  title: Configuration
  parent: index.md
  position: 002
categories:
  - ae2fct
---

# Configuration

AE2FCT allows blacklisting specific recipes so they require real fluid buckets instead of virtual fluids.

## Recipe Blacklist

File: `config/ae2fct-common.toml`

```toml
[recipe_transfer]
virtualFluidRecipeBlacklist = ["immersiveengineering:crafting/redstone_acid"]
```

- `virtualFluidRecipeBlacklist`: A list of crafting recipe IDs where virtual fluids cannot be used.
- Recipes added here will require actual filled buckets as in the original recipe.
- In multiplayer, the server's configuration determines crafting behavior.

## Blacklist Category

Blacklisted recipes can be viewed in the separate **Virtual Fluid Blacklist** recipe viewer category.
Press the recipe viewer usage key, usually `U`, on the Terminal Fluid Interact Card to open this category directly.
