package com.github.tier940.legacycraft.core.akutoengine;

import net.minecraft.launchwrapper.IClassTransformer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/**
 * Fixes the InnerClasses attribute on net/minecraft/world/World.
 *
 * <p>
 * Coremods use ClassWriter which may drop the InnerClasses attribute,
 * causing NoClassDefFoundError: World$2 at tick time.
 *
 * <p>
 * This transformer uses ASM Tree API to read the class, preserve InnerClasses
 * entries, and recompute frames.
 */
public class WorldInnerClassFixTransformer implements IClassTransformer, Opcodes {

    private static final Logger LOGGER = LogManager.getLogger("LCCore/WorldInnerClassFixTransformer");
    private static final String TARGET_CLASS = "net/minecraft/world/World";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        // Only target net/minecraft/world/World (exact match on SRG or MCP name)
        // Do NOT use substring matching — it corrupts unrelated classes like Railcraft.WorldPlugin
        if (!TARGET_CLASS.equals(transformedName) && !"amu".equals(transformedName)) {
            return bytes;
        }

        if (bytes == null) {
            return bytes;
        }

        try {
            LOGGER.info("WorldInnerClassFixTransformer: Processing {}", transformedName);

            // Use ASM Tree API for more robust manipulation
            ClassNode classNode = new ClassNode();
            ClassReader classReader = new ClassReader(bytes);
            classReader.accept(classNode, ClassReader.SKIP_FRAMES);

            // Extract existing InnerClasses entries for logging
            InnerClassNode[] innerClasses = extractInnerClasses(classNode);

            if (innerClasses == null || innerClasses.length == 0) {
                LOGGER.info("WorldInnerClassFixTransformer: no InnerClasses entries in {}, will still recompute frames",
                        transformedName);
            } else {
                LOGGER.info(
                        "WorldInnerClassFixTransformer: found {} InnerClasses entries in {}, preserving and recomputing frames",
                        innerClasses.length, transformedName);
                for (InnerClassNode icn : innerClasses) {
                    LOGGER.info("  InnerClass: name={}, outer={}, inner={}, access=0x{}",
                            icn.name, icn.outerName, icn.innerName, Integer.toHexString(icn.access));
                }
            }

            // Write back with COMPUTE_FRAMES and preserved InnerClasses
            ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);
            classNode.accept(classWriter);

            LOGGER.info("WorldInnerClassFixTransformer: fixed InnerClasses attribute on {}", transformedName);
            return classWriter.toByteArray();
        } catch (Exception e) {
            LOGGER.error("WorldInnerClassFixTransformer: Failed to fix InnerClasses on {}", transformedName, e);
            return bytes;
        }
    }

    private static InnerClassNode[] extractInnerClasses(ClassNode classNode) {
        if (classNode.innerClasses == null || classNode.innerClasses.isEmpty()) {
            return null;
        }
        return classNode.innerClasses.toArray(new InnerClassNode[0]);
    }
}
