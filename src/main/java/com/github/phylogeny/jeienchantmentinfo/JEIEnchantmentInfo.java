package com.github.phylogeny.jeienchantmentinfo;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.ModIds;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.resources.I18n;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@JeiPlugin
@Mod(JEIEnchantmentInfo.MOD_ID)
public class JEIEnchantmentInfo implements IModPlugin
{
    public static final String MOD_ID = "jeienchantmentinfo";

    @Override
    public ResourceLocation getPluginUid()
    {
        return new ResourceLocation(MOD_ID, ModIds.JEI_ID);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration)
    {
        String missingDescription = I18n.format(getEnchantmentKey(MOD_ID, "missing_description"));
        String conflictsTitle = "\n" + I18n.format(getEnchantmentKey(MOD_ID, "conflicts"));
        String maxLevelKey = getEnchantmentKey(MOD_ID, "max_level");
        String typeKey = getEnchantmentKey(MOD_ID, "type");
        String typeKeyPrefix = getEnchantmentKey(MOD_ID, "type") + ".";
        ForgeRegistries.ENCHANTMENTS.getValues().forEach(enchantment ->
        {
            String enchantmentKey = enchantment.getName();
            String descriptionKey = enchantmentKey.replace("." + ModIds.MINECRAFT_ID + ".", "." + MOD_ID + ".") + ".description";
            String description = I18n.format(descriptionKey).replace("Format error: ", "");
            if (descriptionKey.equals(description))
                description = missingDescription;

            description = TextFormatting.BOLD + "" + TextFormatting.UNDERLINE + I18n.format(enchantmentKey) + TextFormatting.RESET + "\n" + description;
            description += "\n" + I18n.format(typeKey, I18n.format(typeKeyPrefix + enchantment.type.name().toLowerCase()));
            int maxLevel = enchantment.getMaxLevel();
            description += "\n" + I18n.format(maxLevelKey, maxLevel);
            StringBuilder conflictBuilder = new StringBuilder();
            ForgeRegistries.ENCHANTMENTS.getValues().stream()
                    .filter(enchantment2 -> enchantment != enchantment2 && !enchantment.isCompatibleWith(enchantment2))
                    .forEach(enchantment2 -> conflictBuilder.append("\n-").append(I18n.format(enchantment2.getName())));
            String conflicts = conflictBuilder.toString();
            if (!conflicts.isEmpty())
                description += conflictsTitle + conflicts;

            List<ItemStack> books = IntStream.range(1, maxLevel + 1).mapToObj(i ->
            {
                ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
                EnchantedBookItem.addEnchantment(book, new EnchantmentData(enchantment, i));
                return book;
            }).collect(Collectors.toList());
            registration.addIngredientInfo(books, VanillaTypes.ITEM, description);
        });
    }

    private String getEnchantmentKey(String namespace, String name)
    {
        return String.join(".", "enchantment", namespace, name);
    }
}