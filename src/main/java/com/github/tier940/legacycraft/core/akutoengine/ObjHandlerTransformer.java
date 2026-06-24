package com.github.tier940.legacycraft.core.akutoengine;

import net.minecraft.launchwrapper.IClassTransformer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Rewrites {@code GateVariant.<init>} calls in {@code ObjHandler.registerRecipes()} from the
 * pre-BCR-8 3-arg signature to the 2-arg signature, dropping the removed {@code EnumGateModifier}.
 */
public class ObjHandlerTransformer implements IClassTransformer {

    private static final Logger LOGGER = LogManager.getLogger("LCCore/ObjHandlerTransformer");

    private static final String TARGET_CLASS = "akuto2.akutoengine.ObjHandler";
    private static final String TARGET_METHOD = "registerRecipes";
    private static final String GATE_VARIANT = "buildcraft/silicon/gate/GateVariant";
    private static final String INIT_METHOD = "<init>";
    private static final String OLD_DESC = "(Lbuildcraft/silicon/gate/EnumGateLogic;" +
            "Lbuildcraft/silicon/gate/EnumGateMaterial;" +
            "Lbuildcraft/silicon/gate/EnumGateModifier;)V";
    private static final String NEW_DESC = "(Lbuildcraft/silicon/gate/EnumGateLogic;" +
            "Lbuildcraft/silicon/gate/EnumGateMaterial;)V";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (!TARGET_CLASS.equals(transformedName) || bytes == null) return bytes;

        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, writer) {

            @Override
            public MethodVisitor visitMethod(
                                             int access, String mName, String desc,
                                             String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, mName, desc, signature, exceptions);
                if (!TARGET_METHOD.equals(mName)) return mv;
                return new MethodVisitor(Opcodes.ASM5, mv) {

                    @Override
                    public void visitMethodInsn(
                                                int opcode, String owner, String iName,
                                                String iDesc, boolean itf) {
                        if (opcode == Opcodes.INVOKESPECIAL && GATE_VARIANT.equals(owner) &&
                                INIT_METHOD.equals(iName) &&
                                OLD_DESC.equals(iDesc)) {
                            // [..., this, EnumGateLogic, EnumGateMaterial, EnumGateModifier] → POP discards the
                            // modifier
                            super.visitInsn(Opcodes.POP);
                            super.visitMethodInsn(opcode, owner, iName, NEW_DESC, itf);
                            LOGGER.info(
                                    "Patched GateVariant.<init> call in ObjHandler.registerRecipes (dropped EnumGateModifier)");
                        } else {
                            super.visitMethodInsn(opcode, owner, iName, iDesc, itf);
                        }
                    }
                };
            }
        };
        reader.accept(cv, 0);
        return writer.toByteArray();
    }
}
