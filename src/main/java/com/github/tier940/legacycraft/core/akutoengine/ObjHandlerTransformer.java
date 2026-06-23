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
 * ASM transformer that patches {@code akuto2.akutoengine.ObjHandler.registerRecipes()}.
 *
 * <p>
 * flyingperson23's BuildCraft dropped {@code EnumGateModifier} from the {@code GateVariant}
 * constructor. The old 3-arg {@code INVOKESPECIAL} is rewritten to {@code POP} the modifier
 * argument off the stack and then invoke the 2-arg constructor instead.
 */
public class ObjHandlerTransformer implements IClassTransformer {

    private static final Logger LOGGER = LogManager.getLogger("LCCore/ObjHandlerTransformer");

    private static final int ASM_API = Opcodes.ASM5;

    // transformedName is dot-separated; ASM internal owner names are slash-separated.
    private static final String TARGET_CLASS = "akuto2.akutoengine.ObjHandler";
    // GATE_VARIANT uses the slash-separated internal name required by ASM visitMethodInsn.
    private static final String GATE_VARIANT = "buildcraft/silicon/gate/GateVariant";
    private static final String INIT_METHOD = "<init>";
    // BCR 8.x removed EnumGateModifier from GateVariant.<init>; OLD_DESC is the pre-BCR-8 signature
    // and NEW_DESC is the replacement. The transformer rewrites every call site in registerRecipes.
    private static final String OLD_DESC = "(Lbuildcraft/silicon/gate/EnumGateLogic;" +
            "Lbuildcraft/silicon/gate/EnumGateMaterial;" +
            "Lbuildcraft/silicon/gate/EnumGateModifier;)V";
    private static final String NEW_DESC = "(Lbuildcraft/silicon/gate/EnumGateLogic;" +
            "Lbuildcraft/silicon/gate/EnumGateMaterial;)V";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        // bytes can be null when the class was already transformed or not found on disk.
        if (!TARGET_CLASS.equals(transformedName) || bytes == null) return bytes;

        ClassReader reader = new ClassReader(bytes);
        // COMPUTE_MAXS recomputes max_stack / max_locals. POP reduces stack depth by one, so
        // max_stack may decrease; COMPUTE_MAXS ensures the value stays correct. It does NOT
        // regenerate StackMapTable (that requires COMPUTE_FRAMES).
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor cv = new ClassVisitor(ASM_API, writer) {

            @Override
            public MethodVisitor visitMethod(
                                             int access, String mName, String desc,
                                             String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, mName, desc, signature, exceptions);
                // Only registerRecipes contains GateVariant.<init> call sites; skip all other methods.
                if (!"registerRecipes".equals(mName)) return mv;
                return new MethodVisitor(ASM_API, mv) {

                    @Override
                    public void visitMethodInsn(
                                                int opcode, String owner, String iName,
                                                String iDesc, boolean itf) {
                        if (opcode == Opcodes.INVOKESPECIAL && GATE_VARIANT.equals(owner) &&
                                INIT_METHOD.equals(iName) &&
                                OLD_DESC.equals(iDesc)) {
                            // Stack before: [..., this, EnumGateLogic, EnumGateMaterial, EnumGateModifier]
                            // POP → [..., this, EnumGateLogic, EnumGateMaterial] (modifier discarded)
                            super.visitInsn(Opcodes.POP);
                            super.visitMethodInsn(opcode, owner, iName, NEW_DESC, itf);
                            LOGGER.info(
                                    "Patched GateVariant.<init> call in ObjHandler.registerRecipes (dropped EnumGateModifier)");
                        } else {
                            // Pass all other method calls through unchanged.
                            super.visitMethodInsn(opcode, owner, iName, iDesc, itf);
                        }
                    }
                };
            }
        };
        // Flag 0: no SKIP_DEBUG / EXPAND_FRAMES. COMPUTE_MAXS handles max_stack / max_locals
        // recomputation; StackMapTable frames are left as-is (COMPUTE_FRAMES is not needed here).
        reader.accept(cv, 0);
        return writer.toByteArray();
    }
}
