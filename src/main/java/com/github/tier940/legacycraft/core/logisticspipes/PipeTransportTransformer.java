package com.github.tier940.legacycraft.core.logisticspipes;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import net.minecraft.launchwrapper.IClassTransformer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

// LP 0.10.4.28+ removed three checks from canPipeConnect_internal that LP 0.10.4.27 had:
// 1. EnderIO conduit check (enderIOProxy.isItemConduit) — field removed in LP 0.10.4.49+
// 2. RF Power Supplier (hasRFPowerSupplierUpgrade + powerProxy.isEnergyReceiver)
// 3. IC2 EU Power Supplier (getIC2PowerLevel > 0 + IC2Proxy.isEnergySink)
// Without them LP pipes refuse to connect to BC Lasers, IC2 machines, and EnderIO conduits.
// EnderIO check is emitted only if SimpleServiceLocator still declares the enderIOProxy field.
// MJ (BuildCraft Joules) was never checked here in any LP version — no regression.
//
// EnumFacing.getOpposite() has SRG name func_176734_d in obfuscated (production) jars and
// MCP name getOpposite in dev (rfg.deobf) environments. The method name is detected at
// transform time by scanning the existing LP bytecode so injected calls match the environment.
public class PipeTransportTransformer implements IClassTransformer {

    private static final Logger LOGGER = LogManager.getLogger("LCCore/PipeTransportTransformer");

    private static final String TARGET_CLASS = "logisticspipes.transport.PipeTransportLogistics";
    private static final String TARGET_METHOD = "canPipeConnect_internal";
    private static final String TARGET_DESC = "(Lnet/minecraft/tileentity/TileEntity;Lnet/minecraft/util/EnumFacing;)Z";

    private static final String SERVICE_LOCATOR = "logisticspipes/proxy/SimpleServiceLocator";
    private static final String SERVICE_LOCATOR_RES = "logisticspipes/proxy/SimpleServiceLocator.class";
    private static final String INV_UTIL_FIELD = "inventoryUtilFactory";
    private static final String INV_UTIL_FIELD_DESC = "Llogisticspipes/utils/InventoryUtilFactory;";

    private static final String PIPE_TRANSPORT = "logisticspipes/transport/PipeTransportLogistics";
    private static final String CORE_PIPE = "logisticspipes/pipes/basic/CoreUnroutedPipe";
    private static final String UPGRADE_MGR = "logisticspipes/interfaces/IPipeUpgradeManager";
    private static final String ENUM_FACING = "net/minecraft/util/EnumFacing";
    private static final String TILE_ENTITY = "net/minecraft/tileentity/TileEntity";

    // SRG name used in obfuscated (production) jars; dev jars use "getOpposite"
    private static final String ENUM_FACING_OPPOSITE_SRG = "func_176734_d";
    private static final String ENUM_FACING_OPPOSITE_DESC = "()L" + ENUM_FACING + ";";

    private static final String ENDER_IO_PROXY_FIELD = "enderIOProxy";
    private static final String ENDER_IO_PROXY_IFACE = "logisticspipes/proxy/interfaces/IEnderIOProxy";
    private static final String ENDER_IO_PROXY_FIELD_DESC = "Llogisticspipes/proxy/interfaces/IEnderIOProxy;";

    // Used for RF capability check — bypasses canReceive() so the pipe stub is always visible
    // to any Forge Energy receiver; actual power delivery in requestRFPower() still gates on
    // isEnergyReceiver (which checks canReceive()), so no power is wasted to full batteries.
    private static final String CAPABILITY_ENERGY_CLASS = "net/minecraftforge/energy/CapabilityEnergy";
    private static final String CAPABILITY_ENERGY_FIELD = "ENERGY";
    private static final String CAPABILITY_ENERGY_FIELD_DESC = "Lnet/minecraftforge/common/capabilities/Capability;";
    private static final String HAS_CAPABILITY_DESC = "(Lnet/minecraftforge/common/capabilities/Capability;Lnet/minecraft/util/EnumFacing;)Z";

    private static final String IC2_PROXY_IFACE = "logisticspipes/proxy/interfaces/IIC2Proxy";
    private static final String IC2_PROXY_FIELD_DESC = "Llogisticspipes/proxy/interfaces/IIC2Proxy;";

    private Boolean cachedHasEnderIO = null;

    private boolean detectEnderIOProxy() {
        if (cachedHasEnderIO != null) return cachedHasEnderIO;
        try (InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(SERVICE_LOCATOR_RES)) {
            if (is == null) {
                cachedHasEnderIO = false;
                return false;
            }
            ClassReader cr = new ClassReader(readFully(is));
            final boolean[] found = { false };
            cr.accept(
                    new ClassVisitor(Opcodes.ASM5) {

                        @Override
                        public FieldVisitor visitField(
                                                       int access,
                                                       String name,
                                                       String desc,
                                                       String sig,
                                                       Object value) {
                            if (ENDER_IO_PROXY_FIELD.equals(name)) found[0] = true;
                            return null;
                        }
                    },
                    ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            cachedHasEnderIO = found[0];
        } catch (Exception e) {
            cachedHasEnderIO = false;
        }
        return cachedHasEnderIO;
    }

    /**
     * Scans canPipeConnect_internal for an INVOKEVIRTUAL on EnumFacing that returns EnumFacing
     * with no parameters — that is getOpposite(). LP uses the SRG name in obfuscated jars and
     * the MCP name after rfg.deobf(). Using whatever name LP itself uses avoids NoSuchMethodError
     * in both environments.
     */
    private String detectEnumFacingOppositeMethod(byte[] classBytes) {
        final String[] result = { ENUM_FACING_OPPOSITE_SRG };
        new ClassReader(classBytes).accept(
                new ClassVisitor(Opcodes.ASM5) {

                    @Override
                    public MethodVisitor visitMethod(
                                                     int access,
                                                     String name,
                                                     String desc,
                                                     String sig,
                                                     String[] exceptions) {
                        if (!TARGET_METHOD.equals(name) || !TARGET_DESC.equals(desc)) return null;
                        return new MethodVisitor(Opcodes.ASM5) {

                            @Override
                            public void visitMethodInsn(
                                                        int opcode,
                                                        String owner,
                                                        String mName,
                                                        String mDesc,
                                                        boolean itf) {
                                if (opcode == Opcodes.INVOKEVIRTUAL && ENUM_FACING.equals(owner) &&
                                        ENUM_FACING_OPPOSITE_DESC.equals(mDesc)) {
                                    result[0] = mName;
                                }
                            }
                        };
                    }
                },
                ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
        return result[0];
    }

    private static byte[] readFully(InputStream is) throws Exception {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] tmp = new byte[4096];
        int n;
        while ((n = is.read(tmp)) != -1) buf.write(tmp, 0, n);
        return buf.toByteArray();
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (!TARGET_CLASS.equals(transformedName) || bytes == null) return bytes;

        final boolean emitEnderIO = detectEnderIOProxy();
        final String enumFacingOpposite = detectEnumFacingOppositeMethod(bytes);

        ClassReader reader = new ClassReader(bytes);
        // COMPUTE_FRAMES recalculates the entire StackMapTable, required when adding new
        // branch targets. COMPUTE_MAXS alone does not update frames and causes VerifyError.
        // getCommonSuperClass falls back gracefully if a type is unresolvable during early
        // LaunchWrapper class transformation.
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {

            @Override
            protected String getCommonSuperClass(String type1, String type2) {
                try {
                    return super.getCommonSuperClass(type1, type2);
                } catch (Exception e) {
                    return "java/lang/Object";
                }
            }
        };
        boolean[] patchApplied = { false };

        ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, writer) {

            @Override
            public MethodVisitor visitMethod(
                                             int access,
                                             String mName,
                                             String desc,
                                             String signature,
                                             String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, mName, desc, signature, exceptions);
                if (!TARGET_METHOD.equals(mName) || !TARGET_DESC.equals(desc)) return mv;

                return new MethodVisitor(Opcodes.ASM5, mv) {

                    private boolean patched = false;

                    @Override
                    public void visitFieldInsn(
                                               int opcode, String owner, String fName, String fDesc) {
                        if (!patched && opcode == Opcodes.GETSTATIC && SERVICE_LOCATOR.equals(owner) &&
                                INV_UTIL_FIELD.equals(fName) && INV_UTIL_FIELD_DESC.equals(fDesc)) {
                            patched = true;
                            patchApplied[0] = true;
                            emitRestoredChecks();
                        }
                        super.visitFieldInsn(opcode, owner, fName, fDesc);
                    }

                    private void emitRestoredChecks() {
                        Label skipRF = new Label();
                        Label returnTrue = new Label();
                        Label skipAll = new Label();

                        if (emitEnderIO) {
                            // 1. EnderIO: if (enderIOProxy.isItemConduit(tile, facing.opp())) return true
                            super.visitFieldInsn(
                                    Opcodes.GETSTATIC,
                                    SERVICE_LOCATOR,
                                    ENDER_IO_PROXY_FIELD,
                                    ENDER_IO_PROXY_FIELD_DESC);
                            super.visitVarInsn(Opcodes.ALOAD, 1);
                            super.visitVarInsn(Opcodes.ALOAD, 2);
                            super.visitMethodInsn(
                                    Opcodes.INVOKEVIRTUAL,
                                    ENUM_FACING,
                                    enumFacingOpposite,
                                    ENUM_FACING_OPPOSITE_DESC,
                                    false);
                            super.visitMethodInsn(
                                    Opcodes.INVOKEINTERFACE,
                                    ENDER_IO_PROXY_IFACE,
                                    "isItemConduit",
                                    "(L" + TILE_ENTITY + ";L" + ENUM_FACING + ";)Z",
                                    true);
                            super.visitJumpInsn(Opcodes.IFNE, returnTrue);
                        }

                        // 2. RF: if (!hasRFPowerSupplierUpgrade()) goto ic2
                        super.visitVarInsn(Opcodes.ALOAD, 0);
                        super.visitMethodInsn(
                                Opcodes.INVOKEVIRTUAL,
                                PIPE_TRANSPORT,
                                "getPipe",
                                "()L" + CORE_PIPE + ";",
                                false);
                        super.visitMethodInsn(
                                Opcodes.INVOKEVIRTUAL,
                                CORE_PIPE,
                                "getUpgradeManager",
                                "()L" + UPGRADE_MGR + ";",
                                false);
                        super.visitMethodInsn(
                                Opcodes.INVOKEINTERFACE,
                                UPGRADE_MGR,
                                "hasRFPowerSupplierUpgrade",
                                "()Z",
                                true);
                        super.visitJumpInsn(Opcodes.IFEQ, skipRF);
                        // if (tile.hasCapability(CapabilityEnergy.ENERGY, facing.opp())) return true
                        // Using hasCapability instead of isEnergyReceiver so the pipe stub is always
                        // visible to any Forge Energy block regardless of battery fullness.
                        // requestRFPower() independently gates actual delivery on isEnergyReceiver.
                        super.visitVarInsn(Opcodes.ALOAD, 1);
                        super.visitFieldInsn(
                                Opcodes.GETSTATIC,
                                CAPABILITY_ENERGY_CLASS,
                                CAPABILITY_ENERGY_FIELD,
                                CAPABILITY_ENERGY_FIELD_DESC);
                        super.visitVarInsn(Opcodes.ALOAD, 2);
                        super.visitMethodInsn(
                                Opcodes.INVOKEVIRTUAL,
                                ENUM_FACING,
                                enumFacingOpposite,
                                ENUM_FACING_OPPOSITE_DESC,
                                false);
                        super.visitMethodInsn(
                                Opcodes.INVOKEVIRTUAL,
                                TILE_ENTITY,
                                "hasCapability",
                                HAS_CAPABILITY_DESC,
                                false);
                        super.visitJumpInsn(Opcodes.IFNE, returnTrue);

                        // 3. IC2 EU: if (getIC2PowerLevel() <= 0) goto skip
                        super.visitLabel(skipRF);
                        super.visitVarInsn(Opcodes.ALOAD, 0);
                        super.visitMethodInsn(
                                Opcodes.INVOKEVIRTUAL,
                                PIPE_TRANSPORT,
                                "getPipe",
                                "()L" + CORE_PIPE + ";",
                                false);
                        super.visitMethodInsn(
                                Opcodes.INVOKEVIRTUAL,
                                CORE_PIPE,
                                "getUpgradeManager",
                                "()L" + UPGRADE_MGR + ";",
                                false);
                        super.visitMethodInsn(
                                Opcodes.INVOKEINTERFACE,
                                UPGRADE_MGR,
                                "getIC2PowerLevel",
                                "()I",
                                true);
                        super.visitJumpInsn(Opcodes.IFLE, skipAll);
                        // if (IC2Proxy.isEnergySink(tile)) return true
                        super.visitFieldInsn(
                                Opcodes.GETSTATIC,
                                SERVICE_LOCATOR,
                                "IC2Proxy",
                                IC2_PROXY_FIELD_DESC);
                        super.visitVarInsn(Opcodes.ALOAD, 1);
                        super.visitMethodInsn(
                                Opcodes.INVOKEINTERFACE,
                                IC2_PROXY_IFACE,
                                "isEnergySink",
                                "(L" + TILE_ENTITY + ";)Z",
                                true);
                        super.visitJumpInsn(Opcodes.IFEQ, skipAll);

                        super.visitLabel(returnTrue);
                        super.visitInsn(Opcodes.ICONST_1);
                        super.visitInsn(Opcodes.IRETURN);

                        super.visitLabel(skipAll);
                    }
                };
            }
        };

        reader.accept(cv, 0);
        if (patchApplied[0]) {
            LOGGER.info(
                    "Patched LP canPipeConnect_internal: restored RF/IC2{} energy checks (EnumFacing.{})",
                    emitEnderIO ? "/EnderIO" : "",
                    enumFacingOpposite);
        }
        return writer.toByteArray();
    }
}
