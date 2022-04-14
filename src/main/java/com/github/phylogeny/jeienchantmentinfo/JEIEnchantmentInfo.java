package com.github.phylogeny.jeienchantmentinfo;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.ModIds;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.fml.ModList;
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
        String missingDescription = I18n.get(getLangKey("missing_description"));
        String conflictsTitle = "\n" + I18n.get(getLangKey("conflicts"));
        String maxLevelKey = getLangKey("max_level");
        String typeKey = getLangKey("type");
        String typeKeyPrefix = typeKey + ".";
        boolean escapePercents = ModList.get().getModContainerById(ModIds.MINECRAFT_ID).get().getModInfo().getVersion().getMinorVersion() == 15;
        ForgeRegistries.ENCHANTMENTS.getValues().forEach(enchantment ->
        {
            String enchantmentKey = enchantment.getDescriptionId();
            String descriptionKey = enchantmentKey.replace("." + ModIds.MINECRAFT_ID + ".", "." + MOD_ID + ".") + ".description";
            String description = I18n.get(descriptionKey);
            if (escapePercents)
                description = description.replace("%", "%%");

            if (descriptionKey.equals(description))
                description = missingDescription;

            description = ChatFormatting.BOLD + "" + ChatFormatting.UNDERLINE + I18n.get(enchantmentKey) + ChatFormatting.RESET + "\n" + description;
            EnchantmentCategory type = enchantment.category;
            List<String> types = new ArrayList<>();
            if (type.name().equals("ALL") || type.canEnchant(Items.COMPASS))
                types.add("vanishable");
            else if (type.canEnchant(Items.SHIELD))
                types.add("breakable");
            else
            {
                if (type.canEnchant(Items.ELYTRA))
                    types.add("wearable");
                else
                {
                    boolean helmet = type.canEnchant(Items.IRON_HELMET);
                    boolean chestplate = type.canEnchant(Items.IRON_CHESTPLATE);
                    boolean leggings = type.canEnchant(Items.IRON_LEGGINGS);
                    boolean boots = type.canEnchant(Items.IRON_BOOTS);
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
                if (type.canEnchant(Items.IRON_SHOVEL))
                    types.add("tool");
                else if (type.canEnchant(Items.IRON_AXE))
                    types.add("axe");

                if (type.canEnchant(Items.IRON_SWORD))
                    types.add("sword");

                if (type.canEnchant(Items.FISHING_ROD))
                    types.add("fishing_rod");

                if (type.canEnchant(Items.TRIDENT))
                    types.add("trident");

                if (type.canEnchant(Items.BOW))
                    types.add("bow");

                if (type.canEnchant(Items.CROSSBOW))
                    types.add("crossbow");
            }
            description += "\n" + I18n.get(typeKey, types.stream().map(suffix -> I18n.get(typeKeyPrefix + suffix)).collect(Collectors.joining(", ")));
            int maxLevel = enchantment.getMaxLevel();
            description += "\n" + I18n.get(maxLevelKey, maxLevel);
            StringBuilder conflictBuilder = new StringBuilder();
            ForgeRegistries.ENCHANTMENTS.getValues().stream()
                    .filter(enchantment2 -> enchantment != enchantment2 && !enchantment.isCompatibleWith(enchantment2))
                    .forEach(enchantment2 -> conflictBuilder.append("\n-").append(I18n.get(enchantment2.getDescriptionId())));
            String conflicts = conflictBuilder.toString();
            if (!conflicts.isEmpty())
                description += conflictsTitle + conflicts;

            List<ItemStack> books = IntStream.range(1, maxLevel + 1).mapToObj(i ->
            {
                ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
                EnchantedBookItem.addEnchantment(book, new EnchantmentInstance(enchantment, i));
                return book;
            }).collect(Collectors.toList());
            registration.addIngredientInfo(books, VanillaTypes.ITEM, new TextComponent(description));
        });
    }

    private String getLangKey(String name)
    {
        return String.join(".", "enchantment", MOD_ID, name);
    }
}