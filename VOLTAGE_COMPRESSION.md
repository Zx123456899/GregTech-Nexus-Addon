# Voltage Compression (voltage-compression branch)

This branch fixes a structural problem in the main branch: GTNA registers machines and recipes up
to `MAX`, but GregTech Modern (GTCEu) provides **no voltage components above UV**, so every machine
rated above UV could never be powered and every recipe above UV could never be run.

## 1. Global compression mapping

Every tier above UV (excluding UV itself) is compressed into the HV..UV window, preserving order:

| Original tier | Compressed tier |
|---|---|
| UHV (9)  | UV (8)  |
| UEV (10) | ZPM (7) |
| UIV (11) | LuV (6) |
| UXV (13) | IV (5)  |
| OpV (14) | EV (4)  |
| MAX (15) | HV (3)  |

Implemented in `src/main/java/com/raishxn/gtna/utils/VoltageCompression.java` (`compress(int)`).
Tiers at or below UV are unchanged. The mapping is a balance decision and can be adjusted in one
place.

## 2. What was compressed

**Machines (fixed-tier parts)** — the machine keeps its original item id, name and function stats,
but its **voltage tier** (definition `.tier()`) is compressed:

- Parallel control hatches (UHV/UEV/UIV/UXV/OpV) → UV/ZPM/LuV/IV/EV
- Thread hatches (UHV..MAX) → UV/ZPM/LuV/IV/EV/HV
- Overclock hatches (UHV..MAX) → UV/ZPM/LuV/IV/EV/HV
- Accelerate / Output-boost / Infinite-input / Output-boost item bus & fluid hatch (all tiers)
- Wireless energy & dynamo hatches (all tiers) — these are energy parts, so their constructor tier,
  capacity, texture and tooltip are compressed together (fully usable within HV..UV)

**Multiblock controllers** — controllers have no fixed voltage (power comes from the energy hatch),
so only internal tier gates were compressed:

- Industrial Slaughterhouse circuit 4 (Dragon): base tier UHV → UV
- Nexus ME Hypercore computation tier label: UEV → ZPM

**Recipes** — every recipe whose energy tier or ingredient tier exceeded UV was compressed:

- Advanced Integrated Ore Processor: UHV parts/EUt → UV
- Thread hatch UHV recipe: UEV circuits → ZPM circuits, UHV superconductor wire → NaquadahAlloy
- Thread hatch UV recipe: UHV circuits → UV circuits
- Antimatter fuel rods (Draconium/Cosmic/Infinity): UEV/UIV/UXV circuits → ZPM/LuV/IV circuits
- Dyson casings (control toroid, annihilate core, dimensional casings, spacetime field generator):
  UIV/UXV/UHV/UEV/OpV circuits → LuV/IV/UV/ZPM/EV circuits
- Artificial Star & Eye of Harmony: OpV circuits → EV circuits
- Nexus Capacitor UHV: UHV circuits → UV circuits
- ME cell component 256M: UHV circuits → UV circuits

## 3. ME series (incl. Nexus) → HV

The AE2/Nexus family machines with a fixed voltage tier are all rated **HV**:

- ME Mini / ME / ME Advanced / ME Ultimate Pattern Buffers (was LuV/ZPM/UV/UHV)
- ME Craft Pattern Hatch (was ZPM)
- ME Pattern Buffer Proxy (was UV)
- ME Storage Access Hatch / ME Big Storage Access Hatch / ME IO Port Hatch (was EV/IV/EV)
- Crafting CPU Interface was already HV — unchanged
- Their recipes were downgraded to HV parts and HV EUt (pattern buffers, upgrades, ME hatches)

**Nexus series**: Nexus Flux Matrix, Nexus Molecular Forge, Nexus ME Hypercore and ME Storage are
multiblocks with **no fixed voltage** (power comes from the energy hatch, which is now at most UV),
so their controllers are unchanged; their recipes are already ≤ UV. Nexus Capacitors are passive
energy-storage blocks (no voltage requirement), so their storage tier is intentionally kept.

## 4. Recipe ingredient audit

Every changed recipe was checked so that all ingredients are craftable within HV..UV:

- Circuit tags and GT components above UV were replaced by the compressed tier's equivalents
  (these have real GTCEu recipes).
- GTCEu material ingredients (e.g. Neutronium, Naquadria, Europium) are left as-is: they belong to
  GTCEu's own material progression, which GTNA does not override.
- No GTNA-owned ingredient was found that lacks a recipe after compression; per the branch rule,
  any ingredient that turns out to be unobtainable should be temporarily replaced by
  `GTMachines.HULL[<same tier>]` — no such case surfaced in this pass.

## 5. What was intentionally kept

- GTNABalance config maps keep their original tier rows (function scaling of parts is keyed by the
  original tier, so compressed machines keep their unique behaviour, e.g. thread counts).
- GameTests: unchanged — the overclock-divisor test still exercises the MAX-tier part with its
  original function tier.
- Machine item ids and lang names are unchanged (saves stay compatible; the tooltip/tier display
  reflects the compressed rating).
- Nexus capacitors: storage tier kept (see above).
