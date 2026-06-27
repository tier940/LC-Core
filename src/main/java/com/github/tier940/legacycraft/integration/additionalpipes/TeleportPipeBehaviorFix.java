package com.github.tier940.legacycraft.integration.additionalpipes;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.apache.logging.log4j.Logger;

import buildcraft.additionalpipes.APPipeDefintions;
import buildcraft.api.transport.pipe.PipeDefinition;

/**
 * Replaces the AP item and fluid teleport pipe behaviour constructors in-place so that existing
 * pipe items in world saves continue to work while using the fixed logic.
 *
 * <p>
 * The target fields ({@code logicConstructor} / {@code logicLoader}) are {@code public final}.
 * On Java 8 they are stripped of finality via {@code Field.class.getDeclaredField("modifiers")}
 * before the replacement. On Java 12+ that field is hidden; a warning is logged and the fix is
 * skipped rather than crashing.
 */
public final class TeleportPipeBehaviorFix {

    // logicConstructor / logicLoader are public final fields on PipeDefinition.
    // They cannot be written through a normal assignment — reflection is the only path.
    private static final String FIELD_LOGIC_CONSTRUCTOR = "logicConstructor";
    private static final String FIELD_LOGIC_LOADER = "logicLoader";
    // JDK-internal field hidden in Java 12+ (JEP 396); NoSuchFieldException on that version.
    private static final String FIELD_MODIFIERS = "modifiers";

    private TeleportPipeBehaviorFix() {}

    private static void stripFinal(Field target, Field modifiersField) throws IllegalAccessException {
        // setAccessible(true) lifts the visibility restriction (fields are public, so this is
        // technically redundant, but required before modifiersField.setInt succeeds on some JVMs).
        target.setAccessible(true);
        modifiersField.setInt(target, target.getModifiers() & ~Modifier.FINAL); // strip FINAL
    }

    private static void replaceBehavior(
                                        Field creatorField,
                                        Field loaderField,
                                        PipeDefinition def,
                                        PipeDefinition.IPipeCreator creator,
                                        PipeDefinition.IPipeLoader loader) throws IllegalAccessException {
        creatorField.set(def, creator);
        loaderField.set(def, loader);
    }

    // Called from LCCoreModule.preInit() (FMLPreInitializationEvent). AP completes its own
    // preInit before ours due to mod dependency ordering, so APPipeDefintions fields are
    // already populated (or null if AP skipped them) before we overwrite them here.
    public static void apply(Logger logger) {
        try {
            Field creatorField = PipeDefinition.class.getDeclaredField(FIELD_LOGIC_CONSTRUCTOR);
            Field loaderField = PipeDefinition.class.getDeclaredField(FIELD_LOGIC_LOADER);
            // Field.class.getDeclaredField("modifiers") is a JDK-internal trick to strip the
            // FINAL flag at runtime; this accessor is hidden in Java 12+ (JEP 396).
            Field modifiersField = Field.class.getDeclaredField(FIELD_MODIFIERS);
            modifiersField.setAccessible(true);

            stripFinal(creatorField, modifiersField);
            stripFinal(loaderField, modifiersField);

            // Both null-checks guard against AP loading but not registering its definitions
            // (e.g. if the mod is present but its preInit was skipped due to a dependency failure).
            if (APPipeDefintions.itemsTeleportPipeDef != null) {
                replaceBehavior(creatorField, loaderField, APPipeDefintions.itemsTeleportPipeDef,
                        PipeBehaviorTeleportItemsFixed::new, PipeBehaviorTeleportItemsFixed::new);
                logger.info("Fixed item teleport pipe behavior (null teleportSide)");
            }

            if (APPipeDefintions.liquidsTeleportPipeDef != null) {
                replaceBehavior(creatorField, loaderField, APPipeDefintions.liquidsTeleportPipeDef,
                        PipeBehaviorTeleportFluidsFixed::new, PipeBehaviorTeleportFluidsFixed::new);
                logger.info("Fixed fluid teleport pipe behavior (multi-sender fair-share)");
            }
        } catch (NoSuchFieldException e) {
            // NoSuchFieldException may mean Field.class.getDeclaredField("modifiers") is hidden
            // on Java 12+ (JEP 396), or that a PipeDefinition field name has changed. Either way
            // the fix is intentionally skipped rather than crashing the mod load.
            logger.warn(
                    "Cannot fix teleport pipe behaviors (Field.modifiers hidden on Java 12+): {}",
                    e.getMessage());
        } catch (Exception e) {
            // Covers SecurityException, IllegalAccessException, and any BCR internal error;
            // logged at ERROR because a non-critical catch here means pipes may still misbehave.
            logger.error("Failed to fix item/fluid teleport pipe behaviors", e);
        }
    }
}
