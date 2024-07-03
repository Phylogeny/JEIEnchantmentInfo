package com.github.phylogeny.jeienchantmentinfo;

import com.google.common.base.Suppliers;
import com.mojang.logging.LogUtils;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.ModIds;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.IntStream;

@JeiPlugin
@Mod(JEIEnchantmentInfo.MOD_ID)
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class JEIEnchantmentInfo implements IModPlugin {
    public static final String MOD_ID = "jeienchantmentinfo";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Supplier<Boolean> ESCAPE_PERCENTS = Suppliers.memoize(() ->
            ModList.get().getModContainerById(ModIds.MINECRAFT_ID).orElseThrow().getModInfo().getVersion().getMinorVersion() == 15);

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(MOD_ID, ModIds.JEI_ID);
    }

    private String getLangKey(String name) {
        return String.join(".", "enchantment", MOD_ID, name);
    }

    private static Optional<String> translate(String langKey, String translatedElement) {
        String translated = I18n.get(langKey);
        if (ESCAPE_PERCENTS.get())
            translated = translated.replace("%", "%%");

        if (!langKey.equals(translated)) {
            if (translated.startsWith("Format error: ")) {
                translated = translated.substring(14);
                String warning = String.format("""
                                A formatting error occurred while getting the description for '%s'.
                                The lang key '%s' returned the following errant description string:
                                [%s]""",
                        translatedElement, langKey, translated);
                if (translated.contains("%"))
                    warning += """
    
                                Since the description contains a '%' symbol, it's possible that it was not properly escaped.
                                Please ensure that all descriptions contain '%%', rather than '%'.""";

                LOGGER.warn(warning);
            }
            return Optional.of(translated);
        }
        return Optional.empty();
    }

    private static void addItemTagDescription(TagKey<Item> tagKey, StringBuilder info, String typeKey) {
        String tagLangKey = ModLanguageProvider.getTagLangKey(tagKey);
        if (tagKey.toString().equals(ItemTags.VANISHING_ENCHANTABLE.toString()))
            tagLangKey += "." + ModLanguageProvider.DescriptionSuffix.SHORT.value;

        String supportedItemsDescription = translate(tagLangKey, tagKey.toString()).orElseGet(() -> {
            String name = tagKey.location().getPath()
                    .replace("_", " ")
                    .replace("ore bearing", "ore-bearing");
            List<String> path = Arrays.asList(name.split("/"));
            Collections.reverse(path);
            name = String.join(" ", path);
            if (name.startsWith("non "))
                name = "non-" + name.substring(4);
            else if (name.endsWith(" in ground"))
                name = "in-ground " + name.substring(0, name.length() - 10);
            else if (name.endsWith(" enchantable"))
                name = name.substring(0, name.length() - 12);

            return name.replace("_", " ");
        });
        info.append("\n")
                .append(I18n.get(typeKey, supportedItemsDescription));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        String missingDescription = I18n.get(getLangKey("missing_desc"));
        String conflictsTitle = "\n" + I18n.get(getLangKey("conflicts"));
        String maxLevelKey = getLangKey("max_level");
        String typeKey = getLangKey("type");
        BuiltInRegistries.ENCHANTMENT.forEach(enchantment -> {
            String enchantmentKey = enchantment.getDescriptionId();
            StringBuilder info = new StringBuilder(ChatFormatting.BOLD + "" + ChatFormatting.UNDERLINE + I18n.get(enchantmentKey) + ChatFormatting.RESET + "\n");
            String description = missingDescription;
            for (String suffix : ModLanguageProvider.DESCRIPTION_SUFFIXES) {
                String descriptionKey = ModLanguageProvider.getDescriptionKey(enchantment, suffix);
                Optional<String> translated = translate(descriptionKey, enchantmentKey);
                if (translated.isPresent()) {
                    description = translated.get();
                    break;
                }
            }
            info.append(description);
            addItemTagDescription(enchantment.getSupportedItems(), info, typeKey);
            int maxLevel = enchantment.getMaxLevel();
            info.append("\n")
                       .append(I18n.get(maxLevelKey, maxLevel));
            StringBuilder conflictBuilder = new StringBuilder();
            BuiltInRegistries.ENCHANTMENT.stream()
                    .filter(enchantment2 -> enchantment != enchantment2 && !enchantment.isCompatibleWith(enchantment2))
                    .forEach(enchantment2 -> conflictBuilder.append("\n-").append(I18n.get(enchantment2.getDescriptionId())));
            String conflicts = conflictBuilder.toString();
            if (!conflicts.isEmpty())
                info.append(conflictsTitle)
                           .append(conflicts);

            List<ItemStack> books = IntStream.range(1, maxLevel + 1).mapToObj(i -> {
                ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
                book.enchant(enchantment, i);
                return book;
            }).toList();
            registration.addIngredientInfo(books, VanillaTypes.ITEM_STACK, Component.literal(info.toString()));
        });
//        StringBuilder info = new StringBuilder();
//        BuiltInRegistries.ITEM.getTagNames().forEach(tagKey -> addItemTagDescription(tagKey, info, typeKey));
//        registration.addIngredientInfo(new ItemStack(Items.TOTEM_OF_UNDYING), VanillaTypes.ITEM_STACK, Component.literal(info.toString()));
    }
}