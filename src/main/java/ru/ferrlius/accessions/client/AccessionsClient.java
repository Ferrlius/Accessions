package ru.ferrlius.accessions.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Client-only setup. The dedicated server is forbidden from touching this
 * class at all — every reference to it in common code MUST be guarded by
 * {@code FMLEnvironment.dist.isClient()} so the JVM never resolves it.
 *
 * Splitting client init into its own class is the canonical NeoForge pattern.
 * Even though Java lazy class loading would in theory keep an inline
 * {@code if (isClient) { ... new ClientScreen() ... }} safe, putting client
 * code in a dedicated class:
 *
 * - guarantees that no classloader edge case can leak a client-only symbol
 *   into the server bytecode stream,
 * - keeps client and common code visually obvious in code review,
 * - lets you add more client init later without re-auditing the dist guards
 *   on every line.
 */
public final class AccessionsClient {
    private AccessionsClient() {}

    public static void register(ModContainer modContainer) {
        modContainer.registerExtensionPoint(
                IConfigScreenFactory.class,
                (IConfigScreenFactory) (container, parentScreen) -> new AccessionsConfigScreen(parentScreen)
        );
    }
}
