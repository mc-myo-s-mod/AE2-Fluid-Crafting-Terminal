package me.myogoo.ae2fct.mixin;

import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;
import java.nio.file.Files;

public final class Plugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains(".jei.et.")) {
            if (!isLoaded("extendedterminal") || isLoaded("emi") || !isLoaded("jei")) {
                return false;
            }
            if (targetClassName.contains(".extendedcrafting.")) {
                return isLoaded("extendedcrafting");
            }
            if (targetClassName.contains(".avaritiaRe.") || targetClassName.contains(".avaritiaNeo.")) {
                var avaritia = LoadingModList.get().getModFileById("avaritia");
                if (avaritia == null) {
                    return false;
                }
                // Both implementations use the same mod id. Inspect the JAR without loading optional classes.
                String recipeClass = targetClassName.contains(".avaritiaNeo.")
                        ? "net/byAqua3/avaritia/recipe/RecipeExtremeCrafting.class"
                        : "committee/nova/mods/avaritia/common/crafting/recipe/ITierCraftingRecipe.class";
                return Files.exists(avaritia.getFile().findResource(recipeClass));
            }
            return true;
        }
        if (mixinClassName.contains(".ie.")) {
            return isLoaded("immersiveengineering");
        }
        if (mixinClassName.contains(".fastsuite.")) {
            return isLoaded("fastsuite");
        }
        if (mixinClassName.contains(".et.")) {
            return isLoaded("extendedterminal");
        }
        if (mixinClassName.contains(".emi.")) {
            return isLoaded("emi");
        }
        if (mixinClassName.endsWith(".jei.UseCraftingRecipeTransferMixin")) {
            return isLoaded("jei");
        }
        if (mixinClassName.contains(".jei.")) {
            return !isLoaded("emi") && isLoaded("jei");
        }
        if (mixinClassName.contains(".rei.")) {
            return isLoaded("roughlyenoughitems");
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return List.of();
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private static boolean isLoaded(String modId) {
        return LoadingModList.get().getMods().stream().map(ModInfo::getModId).anyMatch(modId::equals);
    }
}
