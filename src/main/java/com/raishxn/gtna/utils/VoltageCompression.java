package com.raishxn.gtna.utils;

import com.gregtechceu.gtceu.api.GTValues;

/**
 * GTCEu officially stops at UV (8): no energy components exist above it, so GTNA machines and
 * recipes registered above UV can never be powered or crafted. This branch compresses every tier
 * above UV (excluding UV itself) into the HV..UV window, preserving order:
 *
 * <pre>
 * UHV(9)  -&gt; UV(8)
 * UEV(10) -&gt; ZPM(7)
 * UIV(11) -&gt; LuV(6)
 * UXV(13) -&gt; IV(5)
 * OpV(14) -&gt; EV(4)
 * MAX(15) -&gt; HV(3)
 * </pre>
 *
 * Tiers at or below UV are returned unchanged. The mapping is a deliberate balance decision for
 * the {@code voltage-compression} branch (see VOLTAGE_COMPRESSION.md) and can be adjusted there.
 */
public final class VoltageCompression {

    private VoltageCompression() {}

    /** Compress a GT voltage tier into the HV..UV window (identity for tiers &le; UV). */
    public static int compress(int tier) {
        if (tier <= GTValues.UV) {
            return tier;
        }
        return switch (tier) {
            case GTValues.UHV -> GTValues.UV;
            case GTValues.UEV -> GTValues.ZPM;
            case GTValues.UIV -> GTValues.LuV;
            case GTValues.UXV -> GTValues.IV;
            case GTValues.OpV -> GTValues.EV;
            case GTValues.MAX -> GTValues.HV;
            // Safety: any other tier above UV falls back to the top of the supported window.
            default -> GTValues.UV;
        };
    }
}
