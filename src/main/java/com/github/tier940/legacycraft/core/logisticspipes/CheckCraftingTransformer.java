package com.github.tier940.legacycraft.core.logisticspipes;

import net.minecraft.launchwrapper.IClassTransformer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Replaces the inline BitSet-walk + getDistanceTo loop at the top of
 * {@code RequestTreeNode.checkCrafting()} with a call to
 * {@link RequestTreeNodeHelper#buildValidSources}, which caches the result
 * per {@code IResource} instance across sibling nodes in the same request tree.
 *
 * <p>
 * Original bytecode pattern (offsets 0-88):
 * 
 * <pre>
 *   getRequestType → getRoutersInterestedIn → astore_1 (BitSet)
 *   new ArrayList → astore_2 (validSources)
 *   BitSet.nextSetBit loop building validSources
 * </pre>
 * 
 * Replaced with:
 * 
 * <pre>
 *   aload_0
 *   invokevirtual getRequestType
 *   invokestatic  RequestTreeNodeHelper.buildValidSources
 *   astore_2 (validSources)
 *   goto offset_91 (sort + rest of method)
 * </pre>
 */
public class CheckCraftingTransformer implements IClassTransformer {

    private static final Logger LOGGER = LogManager.getLogger("LCCore/CheckCraftingTransformer");

    private static final String TARGET_CLASS = "logisticspipes.request.RequestTreeNode";
    private static final String TARGET_METHOD = "checkCrafting";
    private static final String TARGET_DESC = "()Z";

    private static final String HELPER_CLASS = "com/github/tier940/legacycraft/core/logisticspipes/RequestTreeNodeHelper";
    private static final String HELPER_METHOD = "buildValidSources";
    private static final String HELPER_DESC = "(Llogisticspipes/request/resources/IResource;)Ljava/util/List;";

    private static final String REQUEST_TREE_NODE = "logisticspipes/request/RequestTreeNode";
    private static final String GET_REQUEST_TYPE = "getRequestType";
    private static final String GET_REQUEST_TYPE_DESC = "()Llogisticspipes/request/resources/IResource;";

    private static final String SERVER_ROUTER = "logisticspipes/routing/ServerRouter";
    private static final String GET_ROUTERS_INTERESTED = "getRoutersInterestedIn";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (!TARGET_CLASS.equals(transformedName) || bytes == null) return bytes;

        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, writer) {

            @Override
            public MethodVisitor visitMethod(int access, String mName, String desc,
                                             String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, mName, desc, signature, exceptions);
                if (TARGET_METHOD.equals(mName) && TARGET_DESC.equals(desc)) {
                    return new CheckCraftingRewriter(mv);
                }
                return mv;
            }
        };
        reader.accept(cv, 0);
        LOGGER.info("Patched RequestTreeNode.checkCrafting — cached validSources construction");
        return writer.toByteArray();
    }

    /**
     * Rewrites the validSources construction at the top of checkCrafting.
     * Strategy: intercept the first INVOKESTATIC getRoutersInterestedIn and replace the
     * entire preamble sequence up to the sort with our helper call.
     *
     * The original code:
     * offset 0: aload_0 → getRequestType → getRoutersInterestedIn → astore_1
     * offset 8: new ArrayList → astore_2
     * offset 16-88: BitSet.nextSetBit loop
     * offset 91: new workWeightedSorter...
     *
     * We intercept at getRoutersInterestedIn: at that point, this.getRequestType() result
     * is on the stack. We replace the INVOKESTATIC with our helper call, store into local 2,
     * and suppress all bytecode until we see the "new workWeightedSorter" (offset 91).
     */
    private static class CheckCraftingRewriter extends MethodVisitor {

        private boolean intercepted = false;
        private boolean suppressing = false;
        private boolean done = false;

        CheckCraftingRewriter(MethodVisitor mv) {
            super(Opcodes.ASM5, mv);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String mName, String desc,
                                    boolean itf) {
            if (!done && !intercepted && Opcodes.INVOKESTATIC == opcode && SERVER_ROUTER.equals(owner) &&
                    GET_ROUTERS_INTERESTED.equals(mName)) {
                // Stack has: IResource (from getRequestType)
                // Replace getRoutersInterestedIn with our helper
                super.visitMethodInsn(Opcodes.INVOKESTATIC, HELPER_CLASS, HELPER_METHOD,
                        HELPER_DESC, false);
                // Store result as validSources (local 2)
                super.visitVarInsn(Opcodes.ASTORE, 2);
                intercepted = true;
                suppressing = true;
                return;
            }

            if (suppressing) {
                // Suppress everything until we're past the loop.
                // We detect end of suppression in visitTypeInsn when we see
                // "new workWeightedSorter".
                return;
            }

            super.visitMethodInsn(opcode, owner, mName, desc, itf);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            if (suppressing) {
                if (opcode == Opcodes.NEW && "logisticspipes/request/RequestTree$workWeightedSorter".equals(type)) {
                    // End of suppression — the sort section begins
                    suppressing = false;
                    done = true;
                    super.visitTypeInsn(opcode, type);
                }
                // Otherwise suppress (new ArrayList inside the loop etc.)
                return;
            }
            super.visitTypeInsn(opcode, type);
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            if (suppressing) return;
            super.visitVarInsn(opcode, var);
        }

        @Override
        public void visitInsn(int opcode) {
            if (suppressing) return;
            super.visitInsn(opcode);
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            if (suppressing) return;
            super.visitIntInsn(opcode, operand);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String fname, String desc) {
            if (suppressing) return;
            super.visitFieldInsn(opcode, owner, fname, desc);
        }

        @Override
        public void visitJumpInsn(int opcode, org.objectweb.asm.Label label) {
            if (suppressing) return;
            super.visitJumpInsn(opcode, label);
        }

        @Override
        public void visitLabel(org.objectweb.asm.Label label) {
            if (suppressing) return;
            super.visitLabel(label);
        }

        @Override
        public void visitFrame(int type, int numLocal, Object[] local, int numStack,
                               Object[] stack) {
            if (suppressing) return;
            super.visitFrame(type, numLocal, local, numStack, stack);
        }

        @Override
        public void visitLdcInsn(Object value) {
            if (suppressing) return;
            super.visitLdcInsn(value);
        }

        @Override
        public void visitIincInsn(int var, int increment) {
            if (suppressing) return;
            super.visitIincInsn(var, increment);
        }

        @Override
        public void visitLineNumber(int line, org.objectweb.asm.Label start) {
            if (suppressing) return;
            super.visitLineNumber(line, start);
        }
    }
}
