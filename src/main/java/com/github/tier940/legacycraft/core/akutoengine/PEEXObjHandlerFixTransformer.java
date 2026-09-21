package com.github.tier940.legacycraft.core.akutoengine;

import net.minecraft.launchwrapper.IClassTransformer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/**
 * Fixes ProjectE's ObjHandler after PEEX's TransformerObjHandler corrupts it.
 *
 * <p>
 * PEEX's TransformerObjHandler uses ClassWriter(COMPUTE_MAXS) which drops
 * the InnerClasses attribute. This transformer re-transforms with
 * COMPUTE_FRAMES and preserves the original InnerClasses entries.
 */
public class PEEXObjHandlerFixTransformer implements IClassTransformer, Opcodes {

    private static final Logger LOGGER = LogManager.getLogger("LCCore/PEEXObjHandlerFixTransformer");
    private static final String TARGET_CLASS = "moze_intel/projecte/gameObjs/ObjHandler";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (!TARGET_CLASS.equals(transformedName) || bytes == null) {
            return bytes;
        }

        try {
            ClassNode classNode = new ClassNode();
            ClassReader classReader = new ClassReader(bytes);
            classReader.accept(classNode, ClassReader.SKIP_FRAMES);

            InnerClassNode[] innerClasses = extractInnerClasses(classNode);

            if (innerClasses == null || innerClasses.length == 0) {
                LOGGER.info(
                        "PEEXObjHandlerFixTransformer: no InnerClasses entries in ObjHandler, will still recompute frames");
            } else {
                LOGGER.info(
                        "PEEXObjHandlerFixTransformer: found {} InnerClasses entries in ObjHandler, preserving and recomputing frames",
                        innerClasses.length);
            }

            ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);
            classNode.accept(classWriter);

            LOGGER.info("PEEXObjHandlerFixTransformer: fixed ObjHandler InnerClasses (World$2 fix)");
            return classWriter.toByteArray();
        } catch (Exception e) {
            LOGGER.error("PEEXObjHandlerFixTransformer: Failed to fix ObjHandler", e);
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
