package com.github.phylogeny.jeienchantmentinfo;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.ModIds;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.resources.I18n;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

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
        ForgeRegistries.ENCHANTMENTS.getEntries().forEach(entry ->
        {
            ResourceLocation key = entry.getKey();
            String namespace = key.getNamespace().equals(ModIds.MINECRAFT_ID) ? MOD_ID : key.getNamespace();
            String translationKey = getEnchantmentKey(namespace, key.getPath()) + ".description";
            String description = I18n.format(translationKey).replace("Format error: ", "");
            if (translationKey.equals(description))
                description = missingDescription;

            Enchantment enchantment = entry.getValue();
            description = TextFormatting.BOLD + "" + TextFormatting.UNDERLINE + I18n.format(enchantment.getName()) + TextFormatting.RESET + "\n" + description;
            description += "\n" + I18n.format(typeKey, I18n.format(typeKeyPrefix + enchantment.type.name().toLowerCase()));
            int maxLevel = enchantment.getMaxLevel();
            description += "\n" + I18n.format(maxLevelKey, maxLevel);
            StringBuilder conflictBuilder = new StringBuilder();
            ForgeRegistries.ENCHANTMENTS.getEntries().forEach(entry2 ->
            {
                Enchantment enchantment2 = entry2.getValue();
                if (enchantment == enchantment2 || enchantment.isCompatibleWith(enchantment2))
                    return;

                conflictBuilder.append("\n-").append(I18n.format(enchantment2.getName()));
            });
            String conflicts = conflictBuilder.toString();
            if (!conflicts.isEmpty())
                description += conflictsTitle + conflicts;

            List<ItemStack> books = new ArrayList<>();
            for (int i = 0; i < maxLevel; i++)
            {
                ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
                EnchantedBookItem.addEnchantment(book, new EnchantmentData(enchantment, i + 1));
                books.add(book);
            }
            registration.addIngredientInfo(books, VanillaTypes.ITEM, description);
        });
    }

    private String getEnchantmentKey(String namespace, String name)
    {
        return String.join(".", "enchantment", namespace, name);
    }
}