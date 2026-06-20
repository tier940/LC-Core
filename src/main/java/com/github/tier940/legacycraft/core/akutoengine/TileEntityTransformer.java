package com.github.tier940.legacycraft.core.akutoengine;

import net.minecraft.launchwrapper.IClassTransformer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

// Patches all INVOKEVIRTUAL calls to the old 2-arg getReceiverToPower(TileEntity, EnumFacing)
// in TileEntityAkutoEngineBase. flyingperson23's BC dropped the TileEntity arg, so each call
// is rewritten to SWAP+POP (discard TileEntity) then invoke the 1-arg version.
// Affected call sites: getPowerToExtractMJ (line 37) and sendPower (line 292).
public class TileEntityTransformer implements IClassTransformer {

    private static final Logger LOGGER = LogManager.getLogger("LCCore/TileEntityTransformer");

    private static final String TARGET_CLASS = "akuto2.akutoengine.tiles.engines.TileEntityAkutoEngineBase";
    private static final String ENGINE_BASE = "buildcraft/lib/engine/TileEngineBase_BC8";
    private static final String METHOD = "getReceiverToPower";
    private static final String OLD_DESC = "(Lnet/minecraft/tileentity/TileEntity;" +
            "Lnet/minecraft/util/EnumFacing;)Lbuildcraft/api/mj/IMjReceiver;";
    private static final String NEW_DESC = "(Lnet/minecraft/util/EnumFacing;)Lbuildcraft/api/mj/IMjReceiver;";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (!TARGET_CLASS.equals(transformedName) || bytes == null) return bytes;

        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, writer) {

            @Override
            public MethodVisitor visitMethod(
                                             int access,
                                             String mName,
                                             String desc,
                                             String signature,
                                             String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, mName, desc, signature, exceptions);
                return new MethodVisitor(Opcodes.ASM5, mv) {

                    @Override
                    public void visitMethodInsn(
                                                int opcode,
                                                String owner,
                                                String iName,
                                                String iDesc,
                                                boolean itf) {
                        if (opcode == Opcodes.INVOKEVIRTUAL && METHOD.equals(iName) && OLD_DESC.equals(iDesc)) {
                            // Stack before: ..., this, tileEntity, enumFacing
                            // SWAP: ..., this, enumFacing, tileEntity
                            super.visitInsn(Opcodes.SWAP);
                            // POP: ..., this, enumFacing
                            super.visitInsn(Opcodes.POP);
                            super.visitMethodInsn(opcode, ENGINE_BASE, iName, NEW_DESC, itf);
                            LOGGER.info("Patched getReceiverToPower call in {} (dropped TileEntity arg)", mName);
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
