package me.myogoo.ae2fct.integration;

import appeng.crafting.RecipeAccess;
import me.myogoo.ae2fct.Ae2fct;
import me.myogoo.ae2fct.config.FluidCraftingConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/** Read-only recipe-viewer entries from this client's common config, not server policy. */
public record BlacklistRecipe(Identifier id, int displayIndex, List<List<ItemStack>> inputs,
        List<ItemStack> outputs, int width, boolean shapeless) {
    public static List<BlacklistRecipe> load() {
        Level level = Minecraft.getInstance().level;
        return level == null ? List.of() : load(level);
    }

    public static List<BlacklistRecipe> load(Level level) {
        List<BlacklistRecipe> recipes = new ArrayList<>();
        var context = SlotDisplayContext.fromLevel(level);
        // AE2 already synchronizes crafting recipes; vanilla client RecipeAccess does not contain them in 26.
        for (var holder : RecipeAccess.byType(level, RecipeType.CRAFTING)) {
            if (!FluidCraftingConfig.isVirtualFluidRecipeBlacklisted(holder.id())) {
                continue;
            }
            var displays = holder.value().display();
            for (int index = 0; index < displays.size(); index++) {
                var display = displays.get(index);
                List<SlotDisplay> ingredients;
                int width;
                boolean shapeless;
                if (display instanceof ShapedCraftingRecipeDisplay shaped) {
                    if (shaped.width() > 3 || shaped.height() > 3 || shaped.width() < 1) {
                        continue;
                    }
                    ingredients = shaped.ingredients();
                    width = shaped.width();
                    shapeless = false;
                } else if (display instanceof ShapelessCraftingRecipeDisplay unordered) {
                    ingredients = unordered.ingredients();
                    width = 3;
                    shapeless = true;
                } else {
                    continue;
                }
                var outputs = display.result().resolveForStacks(context);
                if (ingredients.size() > 9 || outputs.isEmpty()) {
                    continue;
                }
                recipes.add(new BlacklistRecipe(holder.id().identifier(), index,
                        ingredients.stream().map(ingredient -> ingredient.resolveForStacks(context)).toList(),
                        outputs, width, shapeless));
            }
        }
        return List.copyOf(recipes);
    }

    public List<ItemStack> inputAt(int gridSlot) {
        int column = gridSlot % 3;
        int index = gridSlot / 3 * width + column;
        return column < width && index < inputs.size() ? inputs.get(index) : List.of();
    }

    public Identifier displayId() {
        return Ae2fct.makeId("virtual_fluid_blacklist/" + id.getNamespace() + "/" + id.getPath() + "/" + displayIndex);
    }
}
