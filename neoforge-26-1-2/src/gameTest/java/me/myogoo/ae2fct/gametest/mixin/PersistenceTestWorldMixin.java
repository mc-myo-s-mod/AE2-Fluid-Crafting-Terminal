package me.myogoo.ae2fct.gametest.mixin;

import java.nio.file.Files;
import java.nio.file.Path;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.minecraft.gametest.framework.GameTestMainUtil", remap = false)
public abstract class PersistenceTestWorldMixin {
    // 26's GameTest launcher erases its world; the second JVM must read the first JVM's saved chunks.
    @Inject(method = "createOrResetDir", at = @At("HEAD"), cancellable = true)
    private static void ae2fct$keepSavedTestWorld(String universePath, CallbackInfo ci) {
        if ("read".equals(System.getProperty("ae2fct.persistenceSmoke"))) {
            if (!Files.isDirectory(Path.of(universePath).resolve("gametestworld"))) {
                throw new IllegalStateException("FCT persistence read requires the completed write world");
            }
            ci.cancel();
        }
    }
}
