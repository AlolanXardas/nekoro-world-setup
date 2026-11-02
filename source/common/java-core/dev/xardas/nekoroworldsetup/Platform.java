package dev.xardas.nekoroworldsetup;

import java.nio.file.Path;
import java.util.ServiceLoader;

/** The few things that differ between NeoForge and Fabric. Each loader project provides one implementation. */
public interface Platform {
    Platform INSTANCE = ServiceLoader.load(Platform.class, Platform.class.getClassLoader()).findFirst()
            .orElseThrow(() -> new IllegalStateException("No Platform for this mod loader"));

    Path configDir();
}
