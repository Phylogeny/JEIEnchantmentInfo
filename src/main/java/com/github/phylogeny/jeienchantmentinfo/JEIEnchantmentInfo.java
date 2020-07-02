package com.github.phylogeny.jeienchantmentinfo;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.ModIds;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.resources.I18n;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
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
        String missingDescription = I18n.format(getLangKey("missing_description"));
        String conflictsTitle = "\n" + I18n.format(getLangKey("conflicts"));
        String maxLevelKey = getLangKey("max_level");
        String typeKey = getLangKey("type");
        String typeKeyPrefix = typeKey + ".";
        ForgeRegistries.ENCHANTMENTS.getValues().forEach(enchantment ->
        {
            String enchantmentKey = enchantment.getName();
            String descriptionKey = enchantmentKey.replace("." + ModIds.MINECRAFT_ID + ".", "." + MOD_ID + ".") + ".description";
            String description = I18n.format(descriptionKey).replace("Format error: ", "");
            if (descriptionKey.equals(description))
                description = missingDescription;

            description = TextFormatting.BOLD + "" + TextFormatting.UNDERLINE + I18n.format(enchantmentKey) + TextFormatting.RESET + "\n" + description;
            EnchantmentType type = enchantment.type;
            List<String> types = new ArrayList<>();
            if (type.name().equals("ALL") || type.canEnchantItem(Items.COMPASS))
                types.add("vanishable");
            else if (type.canEnchantItem(Items.SHIELD))
                types.add("breakable");
            else
            {
                if (type.canEnchantItem(Items.ELYTRA))
                    types.add("wearable");
                else
                {
                    boolean helmet = type.canEnchantItem(Items.IRON_HELMET);
                    boolean chestplate = type.canEnchantItem(Items.IRON_CHESTPLATE);
                    boolean leggings = type.canEnchantItem(Items.IRON_LEGGINGS);
                    boolean boots = type.canEnchantItem(Items.IRON_BOOTS);
                    if (helmet && chestplate && leggings && boots)
                        types.add("armor");
                    else
                    {
                        if (helmet)
                            types.add("armor.helmet");

                        if (chestplate)
                            types.add("armor.chestplate");

                        if (leggings)
                            types.add("armor.leggings");

                        if (boots)
                            types.add("armor.boots");
                    }
                }
                if (type.canEnchantItem(Items.IRON_SHOVEL))
                    types.add("tool");
                else if (type.canEnchantItem(Items.IRON_AXE))
                    types.add("axe");

                if (type.canEnchantItem(Items.IRON_SWORD))
                    types.add("sword");

                if (type.canEnchantItem(Items.FISHING_ROD))
                    types.add("fishing_rod");

                if (type.canEnchantItem(Items.TRIDENT))
                    types.add("trident");

                if (type.canEnchantItem(Items.BOW))
                    types.add("bow");

                if (type.canEnchantItem(Items.CROSSBOW))
                    types.add("crossbow");
            }
            description += "\n" + I18n.format(typeKey, types.stream().map(suffix -> I18n.format(typeKeyPrefix + suffix)).collect(Collectors.joining(", ")));
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

    private String getLangKey(String name)
    {
        return String.join(".", "enchantment", MOD_ID, name);
    }
}