// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import com.hbm.world.ore.OreGeneration;
import com.hbm.world.ore.OreGenerationProfile;
import com.hbm.world.ore.OreHeightProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.locale.Language;
import com.hbm.backport.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class OreGenerationPage extends RecipePage<OreGenerationProfile> {
    private static final int GRAPH_X = 26;
    private static final int GRAPH_Y = 48;
    private static final int GRAPH_WIDTH = 204;
    private static final int GRAPH_HEIGHT = 30;
    private static final int LINE = 11;
    private static final FormattedText ELLIPSIS = FormattedText.of("...");

    OreGenerationPage() {
        super(PageIds.page("ore_generation"), OreGenerationProfile.class);
    }

    @Override
    public Component title() {
        return text("title");
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(Items.IRON_PICKAXE);
    }

    @Override
    public int width() {
        return 240;
    }

    @Override
    public int height() {
        return 152;
    }

    @Override
    public List<OreGenerationProfile> rows() {
        List<OreGenerationProfile> profiles =
                Minecraft.getInstance().getSingleplayerServer() == null
                        ? OreGeneration.clientProfiles()
                        : OreGeneration.serverProfiles();
        return profiles.stream()
                .filter(
                        profile ->
                                profile.blocks().stream()
                                        .anyMatch(
                                                id ->
                                                        BuiltInRegistries.BLOCK
                                                                        .get(id)
                                                                        .asItem()
                                                                != Items.AIR))
                .toList();
    }

    @Override
    public ResourceLocation rowId(OreGenerationProfile row) {
        return row.id();
    }

    @Override
    public List<ItemStack> catalysts() {
        return List.of();
    }

    @Override
    public void layout(OreGenerationProfile row, PageLayout layout) {
        List<ItemStack> outputs =
                row.blocks().stream()
                        .map(BuiltInRegistries.BLOCK::get)
                        .map(block -> new ItemStack(block.asItem()))
                        .filter(stack -> !stack.isEmpty())
                        .toList();
        layout.output(4, 4).background().items(outputs);
    }

    // drawn by the page instead of JEI text widgets: EMI's JEI bridge drops those and the page
    // failed there; same look as JEI's (black, no shadow, 11px lines, "..." when cut off)
    private record Box(Component text, int x, int y, int width, int height) {}

    private static List<Box> boxes(OreGenerationProfile row) {
        List<Box> boxes = new ArrayList<>();
        boxes.add(new Box(text("dimension", name("dimension", row.dimension())), 26, 3, 208, 11));
        boxes.add(new Box(text("server_profile"), 26, 16, 208, 11));
        OreHeightProfile height = row.height();
        if (height.hasGraph()) {
            boxes.add(new Box(text("origin_probability"), 4, 31, 232, 11));
        } else {
            boxes.add(
                    new Box(
                            text(
                                    height.shape() == OreHeightProfile.Shape.UNKNOWN
                                            ? "no_graph"
                                            : "spatial"),
                            GRAPH_X,
                            GRAPH_Y,
                            GRAPH_WIDTH,
                            GRAPH_HEIGHT));
        }
        if (height.shape() != OreHeightProfile.Shape.UNKNOWN) {
            boxes.add(
                    new Box(
                            text(
                                    height.hasGraph() ? "origin_band" : "block_band",
                                    height.minY(),
                                    height.maxY()),
                            4,
                            82,
                            232,
                            11));
        }
        boxes.add(
                new Box(
                        row.attempts() >= 0
                                ? text("attempts", number(row.attempts()))
                                : text("attempts_unknown"),
                        4,
                        95,
                        232,
                        11));
        boxes.add(
                new Box(
                        row.veinSize() >= 0 ? text("size", row.veinSize()) : text("not_density"),
                        4,
                        108,
                        232,
                        11));
        MutableComponent details = Component.empty();
        for (Component condition : row.conditions()) {
            if (!details.getSiblings().isEmpty()) details.append("\n");
            details.append(condition);
        }
        details.append("\n").append(text("biomes", biomeNames(row)));
        boxes.add(new Box(details, 4, 124, 232, 23));
        return boxes;
    }

    private static List<FormattedText> wrapped(Box box) {
        return RecipePanel.font().getSplitter().splitLines(box.text(), box.width(), Style.EMPTY);
    }

    private static int fit(Box box) {
        return Math.max(1, (box.height() + 2) / LINE);
    }

    // the box's lines as JEI wraps them, the last one cut with "..." when the rest does not fit
    private static List<FormattedCharSequence> lines(Box box) {
        Font font = RecipePanel.font();
        List<FormattedText> wrapped = wrapped(box);
        int fit = fit(box);
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (int i = 0; i < Math.min(fit, wrapped.size()); i++) {
            FormattedText line = wrapped.get(i);
            if (i == fit - 1 && wrapped.size() > fit) {
                line =
                        FormattedText.composite(
                                font.substrByWidth(line, box.width() - font.width(ELLIPSIS)),
                                ELLIPSIS);
            }
            lines.add(Language.getInstance().getVisualOrder(line));
        }
        return lines;
    }

    /** biome lists over this long are grouped into "All Overworld except ..." and so on */
    private static final int GROUP_OVER = 20;

    private static final String[][] GROUPS = {
        {"overworld", "All Overworld"}, {"nether", "All Nether"}, {"end", "All End"}
    };

    private static Component biomeNames(OreGenerationProfile row) {
        List<ResourceLocation> rest = new ArrayList<>(row.biomes());
        MutableComponent names = Component.empty();
        var level = Minecraft.getInstance().level;
        if (rest.size() > GROUP_OVER && level != null) {
            var reg = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME);
            var tags = List.of(
                    net.minecraft.tags.BiomeTags.IS_OVERWORLD,
                    net.minecraft.tags.BiomeTags.IS_NETHER,
                    net.minecraft.tags.BiomeTags.IS_END);
            for (int g = 0; g < tags.size(); g++) {
                List<ResourceLocation> members = new ArrayList<>();
                reg.get(tags.get(g)).ifPresent(set -> set.forEach(
                        holder -> holder.unwrapKey().ifPresent(k -> members.add(k.location()))));
                if (members.isEmpty()) continue;
                List<ResourceLocation> missing = new ArrayList<>(members);
                missing.removeAll(rest);
                // group when the row covers most of the dimension, so the exceptions stay short
                if (missing.size() * 2 > members.size()) continue;
                rest.removeAll(members);
                if (!names.getSiblings().isEmpty()) names.append(", ");
                names.append(Component.translatableWithFallback(
                        "hbm.ore_generation.all_" + GROUPS[g][0], GROUPS[g][1]));
                if (!missing.isEmpty()) {
                    names.append(Component.translatableWithFallback("hbm.ore_generation.except", " except "));
                    for (int i = 0; i < missing.size(); i++) {
                        if (i > 0) names.append(", ");
                        names.append(name("biome", missing.get(i)));
                    }
                }
            }
        }
        for (ResourceLocation biome : rest) {
            if (!names.getSiblings().isEmpty()) names.append(", ");
            names.append(name("biome", biome));
        }
        return names;
    }

    private static Component name(String kind, ResourceLocation id) {
        if (id.getNamespace().equals("hbm") && id.getPath().startsWith("inline_" + kind + "/"))
            return text("unnamed_biome");
        if (kind.equals("dimension") && id.getNamespace().equals("minecraft")) {
            String key =
                    switch (id.getPath()) {
                        case "overworld" -> "flat_world_preset.minecraft.overworld";
                        case "the_nether" -> "advancements.nether.root.title";
                        case "the_end" -> "biome.minecraft.the_end";
                        default -> null;
                    };
            if (key != null) return Component.translatable(key);
        }
        return Component.translatableWithFallback(
                kind + "." + id.getNamespace() + "." + id.getPath().replace('/', '.'),
                id.toString());
    }

    @Override
    public void draw(OreGenerationProfile row, GuiGraphicsExtractor graphics) {
        Font font = RecipePanel.font();
        for (Box box : boxes(row)) {
            List<FormattedCharSequence> lines = lines(box);
            for (int i = 0; i < lines.size(); i++)
                graphics.text(font, lines.get(i), box.x(), box.y() + i * LINE, 0xFF000000, false);
        }
        OreHeightProfile height = row.height();
        if (!height.hasGraph()) return;
        int bottom = GRAPH_Y + GRAPH_HEIGHT;
        graphics.fill(GRAPH_X - 1, GRAPH_Y, GRAPH_X, bottom + 1, 0xFF555555);
        graphics.fill(GRAPH_X, bottom, GRAPH_X + GRAPH_WIDTH, bottom + 1, 0xFF555555);
        double max = height.peak();
        for (int x = 0; x < GRAPH_WIDTH; x++) {
            int y = heightAt(height, x);
            int pixels =
                    Math.max(1, (int) Math.round(height.probability(y) / max * (GRAPH_HEIGHT - 2)));
            graphics.fill(GRAPH_X + x, bottom - pixels, GRAPH_X + x + 1, bottom, 0xFF43874B);
        }
        graphics.text(font, "0", 15, bottom - font.lineHeight, 0xFF333333, false);
        Component peak = text("peak", number(max * 100));
        graphics.text(
                font,
                peak,
                GRAPH_X + GRAPH_WIDTH - font.width(peak),
                GRAPH_Y - font.lineHeight,
                0xFF333333,
                false);
    }

    @Override
    public void tooltip(
            OreGenerationProfile row, double mouseX, double mouseY, Consumer<Component> output) {
        for (Box box : boxes(row)) {
            if (mouseX < box.x()
                    || mouseX >= box.x() + box.width()
                    || mouseY < box.y()
                    || mouseY >= box.y() + box.height()) continue;
            // a cut-off box shows its whole text on hover, as JEI's text widget does
            if (wrapped(box).size() > fit(box)) {
                for (String line : box.text().getString().split("\n"))
                    output.accept(Component.literal(line));
                return;
            }
        }
        if (!row.height().hasGraph()
                || mouseX < GRAPH_X
                || mouseX >= GRAPH_X + GRAPH_WIDTH
                || mouseY < GRAPH_Y
                || mouseY > GRAPH_Y + GRAPH_HEIGHT) return;
        int y = heightAt(row.height(), (int) (mouseX - GRAPH_X));
        output.accept(text("point", y, number(row.height().probability(y) * 100)));
        output.accept(text("not_density"));
    }

    private static int heightAt(OreHeightProfile height, int x) {
        return (int)
                (height.minY()
                        + Math.round(
                                ((long) height.maxY() - height.minY())
                                        * (x / (double) (GRAPH_WIDTH - 1))));
    }

    private static String number(double value) {
        if (value == Math.rint(value)) return Long.toString((long) value);
        return String.format(Locale.ROOT, "%.4g", value);
    }

    private static MutableComponent text(String key, Object... args) {
        return Component.translatable("ore.hbm." + key, args);
    }
}
