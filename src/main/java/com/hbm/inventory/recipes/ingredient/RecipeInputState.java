// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.ingredient;

import com.hbm.inventory.recipes.crafting.CraftingInputState;
import com.hbm.items.ModDataComponents;
import com.hbm.items.tool.ItemCraftingDegradation;
import com.hbm.registration.RegistryHandle;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;
import com.hbm.backport.recipe.DataComponentMatchers;
import net.minecraft.advancements.critereon.ItemCustomDataPredicate;
import net.minecraft.advancements.critereon.ItemDamagePredicate;
import net.minecraft.advancements.critereon.ItemSubPredicate;
import net.minecraft.advancements.critereon.NbtPredicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import com.hbm.backport.ItemStackTemplate;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import org.jspecify.annotations.Nullable;

public final class RecipeInputState {
    private RecipeInputState() {}

    /**
     * Components that hold what 1.7.10 NTM kept in NBT. Main matches every recipe input (crafting
     * grid via ShapedOreRecipe/OreDictionary.itemMatches, furnaces, machine AStack.matchesRecipe)
     * on item + metadata only and never looks at NBT; NBTStack only checks the tags it lists. So a
     * charged battery, a renamed or enchanted item, an irradiated ingot or a used fuel rod still
     * fits any recipe that names the item. These components are therefore never a reason to turn
     * a stack away; a recipe that wants one checks it through its ingredient (components /
     * predicates), which still applies. Components that replaced metadata (damage, block state,
     * potion, fluid content, bedrock ore, pellet enrichment, scrap material, decay heat, forge
     * count, ...) are not listed and keep matching exactly, like meta did.
     */
    private static volatile @Nullable Set<DataComponentType<?>> nbtLike;

    public static boolean ignoredByRecipes(DataComponentType<?> type) {
        Set<DataComponentType<?>> set = nbtLike;
        if (set == null) nbtLike = set = buildNbtLike();
        return set.contains(type);
    }

    /**
     * A tool or armor piece only had its damage as metadata in 1.7.10, so for an item with
     * durability every other component (names, enchantments, trims, fuel, abilities, data from
     * other mods, ...) is NBT-like and never turns the stack away.
     */
    public static boolean ignoredByRecipes(DataComponentType<?> type, ItemStack stack) {
        if (ignoredByRecipes(type)) return true;
        return type != DataComponents.DAMAGE
                && stack.getItem().components().has(DataComponents.MAX_DAMAGE);
    }

    private static Set<DataComponentType<?>> buildNbtLike() {
        Set<DataComponentType<?>> set = Collections.newSetFromMap(new IdentityHashMap<>());
        // vanilla: all NBT in 1.7.10 (display name/lore, ench, RepairCost, color, BlockEntityTag...)
        set.add(DataComponents.CUSTOM_NAME);
        set.add(DataComponents.LORE);
        set.add(DataComponents.ENCHANTMENTS);
        set.add(DataComponents.STORED_ENCHANTMENTS);
        set.add(DataComponents.REPAIR_COST);
        set.add(DataComponents.CUSTOM_DATA);
        set.add(DataComponents.DYED_COLOR);
        set.add(DataComponents.UNBREAKABLE);
        set.add(DataComponents.HIDE_TOOLTIP);
        set.add(DataComponents.HIDE_ADDITIONAL_TOOLTIP);
        set.add(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
        set.add(DataComponents.ATTRIBUTE_MODIFIERS);
        set.add(DataComponents.CAN_PLACE_ON);
        set.add(DataComponents.CAN_BREAK);
        set.add(DataComponents.CONTAINER);
        set.add(DataComponents.BLOCK_ENTITY_DATA);
        set.add(DataComponents.LOCK);
        // NTM: stored in NBT by main (charge, contents, heat, depletion, links, mods, ...)
        addAll(
                set,
                ModDataComponents.BATTERY_CHARGE,
                ModDataComponents.MACHINE_BATTERY_STATE,
                ModDataComponents.CAPACITOR_STATE,
                ModDataComponents.REDD_CHARGE,
                ModDataComponents.MUFFLED,
                ModDataComponents.BLOCK_INVENTORY,
                ModDataComponents.STACKLOCK,
                ModDataComponents.MASS_STOCKPILE,
                ModDataComponents.LOCK_STATE,
                ModDataComponents.LOCKED_CONTENTS,
                ModDataComponents.PERSISTENT_DATA,
                ModDataComponents.DRILL_CONTENTS,
                ModDataComponents.REFINERY_CONTENTS,
                ModDataComponents.FLUID_TANK_CONTENTS,
                ModDataComponents.RBMK_FUEL,
                ModDataComponents.ZIRNOX_LIFE,
                ModDataComponents.PLATE_FUEL_LIFE,
                ModDataComponents.WATZ_YIELD,
                ModDataComponents.PELLET_DEPLETION,
                ModDataComponents.HOT_ITEM_HEAT,
                ModDataComponents.HOT_UNTIL,
                ModDataComponents.HAZ_RADIATION,
                ModDataComponents.NEUTRON_ACTIVATION,
                ModDataComponents.ARMOR_MODS,
                ModDataComponents.ARMOR_MOD_NV_ACTIVE,
                ModDataComponents.GUN_STATE,
                ModDataComponents.ARC_ELECTRODE_DURABILITY,
                ModDataComponents.PIPETTE_CAPACITY,
                ModDataComponents.TARGET_DESIGNATOR,
                ModDataComponents.DETONATOR_POS,
                ModDataComponents.ANCHOR_POS,
                ModDataComponents.REACTOR_POS,
                ModDataComponents.RBMK_TOOL_TARGET,
                ModDataComponents.MIRROR_TOOL_TARGET,
                ModDataComponents.ARTY_DESIGNATOR_TARGET,
                ModDataComponents.RADAR_LINKER_TARGET,
                ModDataComponents.WRENCH_TARGET,
                ModDataComponents.WIRE_TARGET,
                ModDataComponents.DRONE_LINK_TARGET,
                ModDataComponents.LINKER_TARGET,
                ModDataComponents.SETTINGS_TOOL_DATA,
                ModDataComponents.TOOL_PRESET_INDEX,
                ModDataComponents.RTTY_PAGER_CHANNEL,
                ModDataComponents.CONVEYOR_RUN,
                ModDataComponents.UNSTABLE_AT,
                ModDataComponents.UNSTABLE_TIMER,
                ModDataComponents.TANK_CONTENTS,
                ModDataComponents.TOOL_PRESETS,
                ModDataComponents.TOOLBOX_OPEN,
                ModDataComponents.DETONATOR_POS_LIST,
                ModDataComponents.REBAR_CONCRETE,
                ModDataComponents.REBAR_CORNER,
                ModDataComponents.CASING_PROGRESS,
                ModDataComponents.PNEUMATIC_BULK,
                ModDataComponents.SPIDERS,
                ModDataComponents.TURRET_WHITELIST,
                ModDataComponents.KIT_CONTENTS,
                ModDataComponents.KIT_COLOR_1,
                ModDataComponents.KIT_COLOR_2,
                ModDataComponents.B92_ANIMATION,
                ModDataComponents.AKIMBO_GHOST,
                ModDataComponents.POTATO_TIMER,
                ModDataComponents.NO9_LAMP,
                ModDataComponents.CONTAGION,
                ModDataComponents.TABLET_SEED,
                ModDataComponents.LORE_BOOK_KEY,
                ModDataComponents.LORE_BOOK_TITLE,
                ModDataComponents.LORE_BOOK_PAGES,
                ModDataComponents.LORE_BOOK_ARGS,
                ModDataComponents.LORE_BOOK_COVER);
        return set;
    }

    @SafeVarargs
    private static void addAll(
            Set<DataComponentType<?>> set,
            RegistryHandle<? extends DataComponentType<?>>... handles) {
        for (RegistryHandle<? extends DataComponentType<?>> handle : handles) set.add(handle.get());
    }

    public static boolean accepts(Ingredient ingredient, ItemStack stack) {
        return accepts(ingredient, stack, DataComponentMatchers.ANY);
    }

    public static boolean accepts(
            Ingredient ingredient, ItemStack stack, DataComponentMatchers additional) {
        if (only(stack)) return true;
        var custom = ingredient.getCustomIngredient();
        if (custom instanceof IngredientState state) return state.acceptsState(stack, additional);
        if (custom == null) return accepts(additional, stack);
        if (custom.getClass() != DataComponentIngredient.class) return accepts(additional, stack);
        for (ItemStack expected : CountIngredient.displayStacks(ingredient, 1)) {
            if (!stack.is(expected.getItem())) continue;
            boolean matched = true;
            for (var change : stack.getComponentsPatch().entrySet()) {
                if (ignoredByRecipes(change.getKey(), stack)) continue;
                if (!claims(additional, change.getKey(), stack)
                        && !Objects.equals(
                                stack.get(change.getKey()), expected.get(change.getKey()))) {
                    matched = false;
                    break;
                }
            }
            if (matched) return true;
        }
        return false;
    }

    public static boolean accepts(DataComponentMatchers matchers, ItemStack stack) {
        return accepts(matchers, DataComponentMatchers.ANY, stack);
    }

    public static boolean accepts(
            DataComponentMatchers matchers, DataComponentMatchers additional, ItemStack stack) {
        for (var change : stack.getComponentsPatch().entrySet()) {
            if (ignoredByRecipes(change.getKey(), stack)) continue;
            if (!claims(matchers, change.getKey(), stack)
                    && !claims(additional, change.getKey(), stack)) return false;
        }
        return true;
    }

    private static boolean claims(
            DataComponentMatchers matchers, DataComponentType<?> type, ItemStack stack) {
        // backport: 1.21.1 DataComponentPredicate keeps its expected components private;
        // asPatch() lists the same entries.
        for (var expected : matchers.exact().asPatch().entrySet()) {
            if (expected.getKey() == type) return true;
        }
        // backport: 26.x partial predicates are 1.21.1 ItemSubPredicates (DamagePredicate ->
        // ItemDamagePredicate, CustomDataPredicate -> ItemCustomDataPredicate); 1.21.1 has no
        // AnyValue predicate, so that branch is gone.
        for (ItemSubPredicate predicate : matchers.partial().values()) {
            boolean required = false;
            if (predicate instanceof ComponentStatePredicate state) {
                required = state.claims(type, stack);
            } else if (predicate instanceof ItemDamagePredicate) {
                required = type == DataComponents.DAMAGE;
            } else if (predicate instanceof ItemCustomDataPredicate(NbtPredicate value)
                    && type == DataComponents.CUSTOM_DATA) {
                required =
                        stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                                .tag
                                .equals(value.tag());
            }
            if (required) return true;
        }
        return false;
    }

    public static boolean only(ItemStack stack, DataComponentType<?>... required) {
        for (var change : stack.getComponentsPatch().entrySet()) {
            if (ignoredByRecipes(change.getKey(), stack)) continue;
            boolean allowed = false;
            for (DataComponentType<?> type : required) {
                if (change.getKey() == type) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) return false;
        }
        return true;
    }

    public static boolean preservesRemainder(ItemStack stack) {
        ItemStackTemplate template = ItemStackTemplate.remainderOf(stack);
        if (template == null) return false;
        ItemStack remaining = template.create();
        for (var change : stack.getComponentsPatch().entrySet()) {
            DataComponentType<?> type = change.getKey();
            if (type == DataComponents.DAMAGE
                    && stack.getItem() instanceof ItemCraftingDegradation
                    && remaining.is(stack.getItem())
                    && remaining.getDamageValue() == stack.getDamageValue() + 1) continue;
            if (!Objects.equals(stack.get(type), remaining.get(type))) return false;
        }
        return true;
    }

    public static boolean hasChangedInputs(CraftingInput input) {
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (!stack.isEmpty() && !stack.getComponentsPatch().isEmpty()) return true;
        }
        return false;
    }

    public static boolean crafting(
            CraftingRecipe recipe,
            CraftingInput input,
            @Nullable ItemStack output,
            boolean returnsRemainders) {
        if (recipe instanceof CraftingInputState state)
            return state.permitsInputStates(input, output, returnsRemainders);
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty() || only(stack)) continue;
            if (returnsRemainders && preservesRemainder(stack)) continue;
            boolean required = false;
            // backport: 26.x placementInfo().ingredients(); 1.21.1 keeps them in getIngredients()
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.test(stack) && accepts(ingredient, stack)) {
                    required = true;
                    break;
                }
            }
            if (!required) return false;
        }
        return true;
    }

    public static boolean preservesComponents(ItemStack source, ItemStack result) {
        if (result.isEmpty()) return false;
        for (var change : source.getComponentsPatch().entrySet()) {
            if (!Objects.equals(source.get(change.getKey()), result.get(change.getKey())))
                return false;
        }
        return true;
    }
}
