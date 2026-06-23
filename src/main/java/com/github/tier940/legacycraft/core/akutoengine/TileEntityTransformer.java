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
 * ASM transformer that patches {@code akuto2.akutoengine.tiles.engines.TileEntityAkutoEngineBase}.
 *
 * <p>
 * flyingperson23's BuildCraft dropped the {@code TileEntity} parameter from
 * {@code getReceiverToPower}. Every {@code INVOKEVIRTUAL} call to the old 2-arg signature is
 * rewritten to {@code SWAP+POP} (discarding the {@code TileEntity} operand) and then invoke the
 * 1-arg version. Affected call sites: {@code getPowerToExtractMJ} and {@code sendPower}.
 */
public class TileEntityTransformer implements IClassTransformer {

    private static final Logger LOGGER = LogManager.getLogger("LCCore/TileEntityTransformer");

    private static final int ASM_API = Opcodes.ASM5;

    private static final String TARGET_CLASS = "akuto2.akutoengine.tiles.engines.TileEntityAkutoEngineBase";
    // Slash-form internal name of the BCR 8.x base class that owns the 1-arg getReceiverToPower.
    private static final String ENGINE_BASE = "buildcraft/lib/engine/TileEngineBase_BC8";
    // Unobfuscated method name — AkutoEngine ships its own source so no SRG mapping needed.
    private static final String METHOD = "getReceiverToPower";
    // Old 2-arg signature: getReceiverToPower(TileEntity neighbor, EnumFacing side) — present in
    // the AkutoEngine sources compiled against an older BCR API that still passed the neighbour TE.
    private static final String OLD_DESC = "(Lnet/minecraft/tileentity/TileEntity;" +
            "Lnet/minecraft/util/EnumFacing;)Lbuildcraft/api/mj/IMjReceiver;";
    // New 1-arg signature: getReceiverToPower(EnumFacing side) — BCR 8.0.11 dropped the neighbour
    // TileEntity parameter because the receiver lookup is now done internally.
    private static final String NEW_DESC = "(Lnet/minecraft/util/EnumFacing;)Lbuildcraft/api/mj/IMjReceiver;";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        // Fast-path: skip every class except the one AkutoEngine target. bytes==null guard is
        // defensive against the LaunchWrapper contract (null means class not found).
        if (!TARGET_CLASS.equals(transformedName) || bytes == null) return bytes;

        ClassReader reader = new ClassReader(bytes);

        // COMPUTE_MAXS recomputes max_stack / max_locals. SWAP+POP does not change stack depth,
        // so this is not strictly needed; included as a standard ClassWriter defensive measure.
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        // Wrap the writer in a ClassVisitor so we can intercept only visitMethod.
        // ASM5 matches the bytecode version AkutoEngine targets (Java 8 / class file 52).
        ClassVisitor cv = new ClassVisitor(ASM_API, writer) {

            @Override
            public MethodVisitor visitMethod(
                                             int access,
                                             String mName,
                                             String desc,
                                             String signature,
                                             String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, mName, desc, signature, exceptions);
                // Wrap every method visitor so visitMethodInsn can intercept call sites
                // regardless of which method in the class contains them.
                return new MethodVisitor(ASM_API, mv) {

                    @Override
                    public void visitMethodInsn(
                                                int opcode,
                                                String owner,
                                                String iName,
                                                String iDesc,
                                                boolean itf) {
                        // Match only the exact old virtual call; descriptor check prevents false
                        // positives if the class ever has an overload with the same name.
                        if (opcode == Opcodes.INVOKEVIRTUAL && METHOD.equals(iName) && OLD_DESC.equals(iDesc)) {
                            // Stack before INVOKEVIRTUAL: [..., this, TileEntity, EnumFacing]
                            // SWAP → [..., this, EnumFacing, TileEntity]
                            // POP → [..., this, EnumFacing] (TileEntity discarded)
                            super.visitInsn(Opcodes.SWAP);
                            super.visitInsn(Opcodes.POP);
                            // Owner changes to ENGINE_BASE because getReceiverToPower is defined
                            // on TileEngineBase_BC8, not on TileEntityAkutoEngineBase itself.
                            super.visitMethodInsn(opcode, ENGINE_BASE, iName, NEW_DESC, itf);
                            LOGGER.info("Patched getReceiverToPower call in {} (dropped TileEntity arg)", mName);
                        } else {
                            // Pass all other method calls through unchanged.
                            super.visitMethodInsn(opcode, owner, iName, iDesc, itf);
                        }
                    }
                };
            }
        };
        // 0 = no optional flags; we don't skip debug info. COMPUTE_MAXS recomputes max_stack /
        // max_locals only — it does NOT regenerate StackMapTable (that requires COMPUTE_FRAMES).
        // SWAP+POP preserves stack depth, so even max_stack recomputation is unnecessary here;
        // COMPUTE_MAXS is included as a ClassWriter convention rather than a strict requirement.
        reader.accept(cv, 0);
        return writer.toByteArray();
    }
}
