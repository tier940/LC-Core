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
 * Rewrites {@code PipeTransportLogistics.moveSolids()} to hoist repeated
 * {@code MainProxy.getGlobalTick()} and {@code MainProxy.isServer()} calls
 * out of the per-item loop.
 */
public class MoveSolidsTransformer implements IClassTransformer {

    private static final Logger LOGGER = LogManager.getLogger("LCCore/MoveSolidsTransformer");

    private static final String TARGET_CLASS = "logisticspipes.transport.PipeTransportLogistics";
    private static final String TARGET_METHOD = "moveSolids";
    private static final String TARGET_DESC = "()V";

    private static final String MAIN_PROXY = "logisticspipes/proxy/MainProxy";
    private static final String GET_GLOBAL_TICK = "getGlobalTick";
    private static final String GET_GLOBAL_TICK_DESC = "()I";
    private static final String IS_SERVER = "isServer";
    private static final String IS_SERVER_DESC = "(Lnet/minecraft/world/IBlockAccess;)Z";

    private static final String LP_ITEM_LIST = "logisticspipes/transport/LPItemList";
    private static final String LP_TRAVELING_ITEM = "logisticspipes/transport/LPTravelingItem";
    private static final String LP_TILE = "logisticspipes/pipes/basic/LogisticsTileGenericPipe";
    private static final String CONTAINER_FIELD = "container";
    private static final String CONTAINER_DESC = "L" + LP_TILE + ";";
    private static final String ITEMS_FIELD = "items";
    private static final String ITEMS_DESC = "L" + LP_ITEM_LIST + ";";
    private static final String GET_WORLD = "getWorld";
    private static final String GET_WORLD_DESC = "()Lnet/minecraft/world/World;";

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
                    return new MoveSolidsRewriter(mv);
                }
                return mv;
            }
        };
        reader.accept(cv, 0);
        LOGGER.info("Patched PipeTransportLogistics.moveSolids — hoisted getGlobalTick/isServer out of item loop");
        return writer.toByteArray();
    }

    private static class MoveSolidsRewriter extends MethodVisitor {

        // local var slots (0=this, 1=iterator, 2=item in original)
        // We add: 3=globalTick(int), 4=isServer(boolean)
        private static final int LOCAL_GLOBAL_TICK = 3;
        private static final int LOCAL_IS_SERVER = 4;

        private boolean preambleEmitted = false;
        private int getGlobalTickCount = 0;

        MoveSolidsRewriter(MethodVisitor mv) {
            super(Opcodes.ASM5, mv);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String mName, String desc,
                                    boolean itf) {
            if (!preambleEmitted && LP_ITEM_LIST.equals(owner) && "flush".equals(mName)) {
                // Emit the original flush call first
                super.visitMethodInsn(opcode, owner, mName, desc, itf);

                // Then hoist: int globalTick = MainProxy.getGlobalTick();
                super.visitMethodInsn(Opcodes.INVOKESTATIC, MAIN_PROXY, GET_GLOBAL_TICK,
                        GET_GLOBAL_TICK_DESC, false);
                super.visitVarInsn(Opcodes.ISTORE, LOCAL_GLOBAL_TICK);

                // boolean isServer = MainProxy.isServer(this.container.getWorld());
                super.visitVarInsn(Opcodes.ALOAD, 0);
                super.visitFieldInsn(Opcodes.GETFIELD,
                        "logisticspipes/transport/PipeTransportLogistics",
                        CONTAINER_FIELD, CONTAINER_DESC);
                super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, LP_TILE, GET_WORLD,
                        GET_WORLD_DESC, false);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, MAIN_PROXY, IS_SERVER,
                        IS_SERVER_DESC, false);
                super.visitVarInsn(Opcodes.ISTORE, LOCAL_IS_SERVER);

                preambleEmitted = true;
                return;
            }

            // Replace getGlobalTick() calls with local var load
            if (Opcodes.INVOKESTATIC == opcode && MAIN_PROXY.equals(owner) && GET_GLOBAL_TICK.equals(mName) &&
                    GET_GLOBAL_TICK_DESC.equals(desc)) {
                getGlobalTickCount++;
                super.visitVarInsn(Opcodes.ILOAD, LOCAL_GLOBAL_TICK);
                return;
            }

            // Replace isServer(IBlockAccess) call with local var load
            // The original pattern is: this.container.getWorld() -> MainProxy.isServer()
            // We need to pop the IBlockAccess arg that was already pushed, then load local
            if (Opcodes.INVOKESTATIC == opcode && MAIN_PROXY.equals(owner) && IS_SERVER.equals(mName) &&
                    IS_SERVER_DESC.equals(desc)) {
                // Pop the IBlockAccess argument that's already on the stack
                super.visitInsn(Opcodes.POP);
                super.visitVarInsn(Opcodes.ILOAD, LOCAL_IS_SERVER);
                return;
            }

            // For the container.getWorld() call that precedes isServer, we need to let it
            // stay on the stack so the POP above consumes it. But we should also eliminate
            // the container getfield. Problem: we can't easily detect the push sequence.
            // The POP above handles it - getWorld() still runs but result is discarded.
            // This is acceptable since getWorld() is a simple field getter.

            super.visitMethodInsn(opcode, owner, mName, desc, itf);
        }
    }
}
