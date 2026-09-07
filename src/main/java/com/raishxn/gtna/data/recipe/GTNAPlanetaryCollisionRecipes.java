package com.raishxn.gtna.data.recipe;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.data.recipes.FinishedRecipe;

import com.raishxn.gtna.common.data.GTNAItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static com.raishxn.gtna.common.data.GTNARecipeType.COSMOS_SIMULATION_RECIPES;

/**
 * 「千行星碰撞维度」配方生成器。
 * 在 datagen 阶段枚举所有已注册的 GTM 材料：
 * - 产出每种带有 gemExquisite（精致宝石）的精致宝石，1200 个/种。
 * - 产出前三条恒星配方（stellar_atmosphere / stellar_metallogenesis /
 *   stellar_superheavy_synthesis）尚未使用的矿粉，1200 个/种。
 * 一个配方最多 120 个物品输出槽，若超出则自动拆分到后续配方。
 * 输入宽松（后期新手友好、耗时约 2 分钟之内）：不同档位反物质燃料棒 + UUMatter。
 */
public final class GTNAPlanetaryCollisionRecipes {

    private static final int OUTPUT_PER_RECIPE = 120;
    private static final int PER_MATERIAL_AMOUNT = 1200;

    /** 前三条恒星配方已经以矿粉(dust)形态产出的材料，本配方不再重复。 */
    private static final Set<Material> ALREADY_DUSTED = Set.of(
            GTMaterials.Carbon, GTMaterials.Silicon, GTMaterials.Iron, GTMaterials.Copper,
            GTMaterials.Nickel, GTMaterials.Aluminium, GTMaterials.Titanium, GTMaterials.Tungsten,
            GTMaterials.Silver, GTMaterials.Gold, GTMaterials.Lead, GTMaterials.Platinum,
            GTMaterials.Uranium238, GTMaterials.Naquadah, GTMaterials.NaquadahEnriched,
            GTMaterials.Naquadria, GTMaterials.Neutronium, GTMaterials.Duranium,
            GTMaterials.Tritanium, GTMaterials.Rhenium, GTMaterials.Osmium, GTMaterials.Iridium,
            GTMaterials.Europium, GTMaterials.Beryllium, GTMaterials.Hafnium, GTMaterials.Tantalum);

    private GTNAPlanetaryCollisionRecipes() {}

    public static void register(Consumer<FinishedRecipe> provider) {
        // 产出清单：优先精致宝石；宝石之后补上前三条配方未用过的矿粉。
        List<Material> materialOutputs = new ArrayList<>();
        for (Material material : GTCEuAPI.materialManager.getRegisteredMaterials()) {
            if (hasItem(TagPrefix.gemExquisite, material) || hasItem(TagPrefix.gem, material)) {
                materialOutputs.add(material); // 有宝石形态的材料
            } else if (hasItem(TagPrefix.dust, material) && !ALREADY_DUSTED.contains(material)) {
                materialOutputs.add(material); // 未覆盖的矿粉
            }
        }

        // 由于 120 槽上限，超出部分拆分到后续配方。
        List<List<Material>> chunks = new ArrayList<>();
        for (int i = 0; i < materialOutputs.size(); i += OUTPUT_PER_RECIPE) {
            chunks.add(materialOutputs.subList(i, Math.min(i + OUTPUT_PER_RECIPE, materialOutputs.size())));
        }

        for (int idx = 0; idx < chunks.size(); idx++) {
            List<Material> outputs = chunks.get(idx);
            if (outputs.isEmpty()) {
                continue;
            }
            String recipeId = idx == 0 ? "billion_planets_collision" : "billion_planets_collision_" + (idx + 1);
            buildRecipe(provider, recipeId, outputs);
        }
    }

    private static void buildRecipe(Consumer<FinishedRecipe> provider, String recipeId, List<Material> outputs) {
        var builder = COSMOS_SIMULATION_RECIPES.recipeBuilder(recipeId);
        // 输入：反物质燃料棒 + UUMatter，档位随配方规模递进。
        // 配方 1（含精致宝石）用高等级棒；后续分卷用更低消耗，保证新手后期也负担得起。
        if (outputs.size() >= OUTPUT_PER_RECIPE) {
            builder.inputItems(GTNAItems.COSMIC_NEUTRONIUM_ANTIMATTER_FUEL_ROD.get())
                    .inputFluids(GTMaterials.UUMatter.getFluid(8000));
        } else if (outputs.size() >= 80) {
            builder.inputItems(GTNAItems.NEUTRONIUM_ANTIMATTER_FUEL_ROD.get())
                    .inputFluids(GTMaterials.UUMatter.getFluid(4000));
        } else {
            builder.inputItems(GTNAItems.NEUTRONIUM_ANTIMATTER_FUEL_ROD.get())
                    .inputFluids(GTMaterials.UUMatter.getFluid(2000));
        }

        for (Material material : outputs) {
            builder.outputItems(getPrefixFor(material), material, PER_MATERIAL_AMOUNT);
        }
        // 耗时：机器 recipeModifier 会把真实耗时固定为 ~4800/2^overclock tick，
        // 在 overclock=1 即约 2 分钟，符合"两分钟之内"。
        builder.duration(1200).EUt(1).addData("tier", 8).save(provider);
    }

    private static boolean hasItem(TagPrefix prefix, Material material) {
        var stack = ChemicalHelper.get(prefix, material);
        return stack != null && !stack.isEmpty();
    }

    // 一个材料可能同时有粉尘与宝石；这里按"宝石优先，否则粉尘"输出。
    private static TagPrefix getPrefixFor(Material material) {
        if (hasItem(TagPrefix.gemExquisite, material)) {
            return TagPrefix.gemExquisite;
        }
        if (hasItem(TagPrefix.gem, material)) {
            return TagPrefix.gem;
        }
        return TagPrefix.dust;
    }
}