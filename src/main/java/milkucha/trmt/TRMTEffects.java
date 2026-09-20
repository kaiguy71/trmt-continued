package milkucha.trmt;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Manages the registration of custom status effects.
 * NOTE: For 26.3, all effect definitions should ideally be moved to data components/JSON files.
 * This class remains as a placeholder for now, but its usage must be reviewed.
 */
public final class TRMTEffects {

    // Placeholder: If the effect is purely cosmetic or only used in recipes, it might not need 
    // programmatic registration if the recipe JSON handles the application.
    public static final StatusEffect LIGHTNESS = null; // Set to null as a placeholder for data-driven definition

    private TRMTEffects() {}

    /**
     * This method is kept empty/minimal because all effect definitions are expected 
     * to be handled by JSON components in the future.
     */
    public static void register() {
        // No programmatic registration needed here for 26.3 compatibility.
    }
}