package com.github.phylogeny.jeienchantmentinfo;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import mezz.jei.api.constants.ModIds;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.Util;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class ModLanguageProvider extends LanguageProvider {
    private int commentNumber;
    private boolean forceAddDescription;
    private final Map<String, String> entries = new LinkedHashMap<>();
    private final PackOutput output;
    private static final String LOCALE = "en_us";
    public static final List<String> DESCRIPTION_SUFFIXES = Arrays.stream(DescriptionSuffix.values())
                                                                    .map(DescriptionSuffix::get)
                                                                    .toList();

    public enum DescriptionPrefix {
        VANILLA("enchantment." + ModIds.MINECRAFT_ID + "."),
        MOD("enchantment." + JEIEnchantmentInfo.MOD_ID + ".");

        final String value;

        DescriptionPrefix(String value) {
            this.value = value;
        }
    }

    public enum DescriptionSuffix {
        SHORT("desc"),
        LONG("description");

        final String value;

        DescriptionSuffix(String value) {
            this.value = value;
        }

        public String get() {
            return value;
        }
    }

    public ModLanguageProvider(PackOutput output) {
        super(output, JEIEnchantmentInfo.MOD_ID, LOCALE);
        this.output = output;
    }

    @SubscribeEvent
    public static void gatherData(final GatherDataEvent event) {
        DataGenerator dataGen = event.getGenerator();
        dataGen.addProvider(event.includeClient(), new ModLanguageProvider(dataGen.getPackOutput()));
    }

    public static String getDescriptionKey(Enchantment enchantment, String suffix) {
        String key = enchantment.getDescriptionId() + "." + suffix;
        if (key.startsWith(DescriptionPrefix.VANILLA.value))
            key = DescriptionPrefix.MOD.value + key.substring(DescriptionPrefix.VANILLA.value.length());

        return key;
    }

    private void addDescription(Enchantment enchantment, String description) {
        add(getDescriptionKey(enchantment, DescriptionSuffix.SHORT.value), description);
    }

    private void addComment(String comment) {
        add("_comment_" + ++commentNumber + "_", comment);
    }

    @SuppressWarnings("deprecation, UnstableApiUsage")
    static CompletableFuture<?> saveStable(CachedOutput pOutput, JsonElement pJson, Path pPath) {
        return CompletableFuture.runAsync(() -> {
            try {
                ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
                HashingOutputStream hashingoutputstream = new HashingOutputStream(Hashing.sha1(), bytearrayoutputstream);

                try (JsonWriter jsonwriter = new JsonWriter(new OutputStreamWriter(hashingoutputstream, StandardCharsets.UTF_8))) {
                    jsonwriter.setSerializeNulls(false);
                    jsonwriter.setIndent("    ");
                    GsonHelper.writeValue(jsonwriter, pJson, null);
                }

                pOutput.writeIfNeeded(pPath, bytearrayoutputstream.toByteArray(), hashingoutputstream.hash());
            } catch (IOException ioexception) {
                LOGGER.error("Failed to save file to {}", pPath, ioexception);
            }
        }, Util.backgroundExecutor());
    }

    public static String getTagLangKey(TagKey<?> tagKey) {
        String langKey =
                "tag." +
                tagKey.registry().location().toShortLanguageKey() +
                "." +
                tagKey.location().toLanguageKey();
        return langKey.replaceAll("[:/]", ".");
    }

    @Override
    public void add(TagKey<?> tagKey, String name) {
        add(getTagLangKey(tagKey), name);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        addTranslations();
        if (!entries.isEmpty()) {
            JsonObject json = new JsonObject();
            entries.forEach(json::addProperty);
            Path target = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(JEIEnchantmentInfo.MOD_ID).resolve("lang").resolve(LOCALE + ".json");
            return saveStable(cache, json, target);
        }
        return CompletableFuture.allOf();
    }

    @Override
    public void add(String key, String value) {
        if (forceAddDescription)
            key += "." + DescriptionSuffix.SHORT.value;

        super.add(key, value);
        entries.put(key, value);
    }

    @Override
    protected void addTranslations() {
        addComment("Enchantment Data");
        String prefix = "enchantment." + JEIEnchantmentInfo.MOD_ID + ".";
        add(prefix + "missing_desc", "--NO DESCRIPTION PROVIDED--\n(mod authors can add description lang file entries; refer to the ''JEI Enchantment Info'' mod readme to see how)");
        add(prefix + "conflicts", "§4Mutually exclusive with:§l");
        add(prefix + "max_level", "§1Max Level: %s");
        add(prefix + "type", "§2Applies to: §l%s");

        addComment("Item Tags");
        //Vanilla
        add(ItemTags.ARMOR_ENCHANTABLE, "Armor");
        add(ItemTags.BOW_ENCHANTABLE, "Bows");
        add(ItemTags.BREAKS_DECORATED_POTS, "Decorated Pot Breakers");
        add(ItemTags.CHEST_ARMOR_ENCHANTABLE, "Chestplates");
        add(ItemTags.CLUSTER_MAX_HARVESTABLES, "Cluster Harvesters");
        add(ItemTags.COMPLETES_FIND_TREE_TUTORIAL, "Tree Tutorial Criteria");
        add(ItemTags.CREEPER_DROP_MUSIC_DISCS, "Creeper-drop Music Discs");
        add(ItemTags.CROSSBOW_ENCHANTABLE, "Crossbows");
        add(ItemTags.DAMPENS_VIBRATIONS, "Vibration Dampeners");
        add(ItemTags.DIRT, "Dirts");
        add(ItemTags.DURABILITY_ENCHANTABLE, "Damageable Items");
        add(ItemTags.DYEABLE, "Dyeable Items");
        add(ItemTags.EQUIPPABLE_ENCHANTABLE, "Equippable Items");
        add(ItemTags.FIRE_ASPECT_ENCHANTABLE, "Fire Aspect Tools");
        add(ItemTags.FISHING_ENCHANTABLE, "Fishing Rods");
        add(ItemTags.FOOT_ARMOR_ENCHANTABLE, "Boots");
        add(ItemTags.FREEZE_IMMUNE_WEARABLES, "Freeze-immune Wearable Items");
        add(ItemTags.HEAD_ARMOR_ENCHANTABLE, "Helmets");
        add(ItemTags.IGNORED_BY_PIGLIN_BABIES, "Piglin Baby Unappealing Itenms");
        add(ItemTags.LEG_ARMOR_ENCHANTABLE, "Leggings");
        add(ItemTags.MACE_ENCHANTABLE, "Maces");
        add(ItemTags.MINING_ENCHANTABLE, "Mining Tools");
        add(ItemTags.MINING_LOOT_ENCHANTABLE, "Loot-mining Tools");
        add(ItemTags.NON_FLAMMABLE_WOOD, "Non-flammable Wood");
        add(ItemTags.PARROT_POISONOUS_FOOD, "Poisonous Parrot Food");
        add(ItemTags.PIGLIN_LOVED, "Piglin Tempt Items");
        add(ItemTags.SAND, "Sands");
        add(ItemTags.SHARP_WEAPON_ENCHANTABLE, "Sharp Weapons");
        add(ItemTags.SMELTS_TO_GLASS, "Glass Precursors");
        add(ItemTags.SOUL_FIRE_BASE_BLOCKS, "Soul Fire Base Blocks");
        add(ItemTags.SWORD_ENCHANTABLE, "Swords");
        add(ItemTags.TRIDENT_ENCHANTABLE, "Tridents");
        add(ItemTags.VANISHING_ENCHANTABLE, "Vanishing Items");
        forceAddDescription = true;
        add(ItemTags.VANISHING_ENCHANTABLE, "weapons, tools, wearable/damageable items");
        forceAddDescription = false;
        add(ItemTags.VILLAGER_PLANTABLE_SEEDS, "Villager-plantable Seeds");
        add(ItemTags.WEAPON_ENCHANTABLE, "Weapons");
        //Forge
        add(Tags.Items.BUCKETS_POWDER_SNOW, "Powdered Snow Buckets");
        add(Tags.Items.ENCHANTABLES, "Enchantable Items");
        add(Tags.Items.FOODS_EDIBLE_WHEN_PLACED, "Edible Placed Foods");
        add(Tags.Items.FOODS_FOOD_POISONING, "Poison Foods");
        add(Tags.Items.HIDDEN_FROM_RECIPE_VIEWERS, "Hidden Recipe Ingredients");
        add(Tags.Items.DYED_LIGHT_BLUE, "Light Blue Dyed Items");
        add(Tags.Items.DYED_LIGHT_GRAY, "Light Gray Dyed Items");
        add(ItemTags.MEAT, "Meats");
        add(Tags.Items.ORE_RATES_DENSE, "Dense Ores");
        add(Tags.Items.ORE_RATES_SINGULAR, "Singular Ores");
        add(Tags.Items.ORE_RATES_SPARSE, "Sparse Ores");
        add(Tags.Items.ORES_IN_GROUND_DEEPSLATE, "In-ground Deepslate Ores");
        add(Tags.Items.ORES_IN_GROUND_NETHERRACK, "In-ground Netherrack Ores");
        add(Tags.Items.ORES_IN_GROUND_STONE, "In-ground Stone Ores");
        add(Tags.Items.RAW_BLOCKS_COPPER, "Raw Copper Blocks");
        add(Tags.Items.RAW_BLOCKS_GOLD, "Raw Gold Blocks");
        add(Tags.Items.RAW_BLOCKS_IRON, "Raw Iron Blocks");
        add(Tags.Items.RAW_MATERIALS_COPPER, "Raw Copper Materials");
        add(Tags.Items.RAW_MATERIALS_GOLD, "Raw Gold Materials");
        add(Tags.Items.RAW_MATERIALS_IRON, "Raw Iron Materials");

        addComment("Enchantment Descriptions");
        addDescription(Enchantments.AQUA_AFFINITY, "Increases underwater mining speed.");
        addDescription(Enchantments.BANE_OF_ARTHROPODS, "Increases damage and applies Slowness IV to arthropod mobs (spiders, cave spiders, silverfish, endermites & bees).");
        addDescription(Enchantments.BLAST_PROTECTION, "Reduces explosion damage and knockback.");
        addDescription(Enchantments.BREACH, "Reduces the effectiveness of the target's armor.");
        addDescription(Enchantments.CHANNELING, "During thunderstorms, a thrown trident summons a lightning bolt on the target when hitting it.");
        addDescription(Enchantments.BINDING_CURSE, "Items can only be removed from armor slots via breaking or death.");
        addDescription(Enchantments.VANISHING_CURSE, "Item disappears on death.");
        addDescription(Enchantments.DENSITY, "Increases damage dealt per-block the attacker falls.");
        addDescription(Enchantments.DEPTH_STRIDER, "Increases underwater movement speed.");
        addDescription(Enchantments.EFFICIENCY, "Increases tool speed, as well as the chance for axes to disable shields.");
        addDescription(Enchantments.FEATHER_FALLING, "Reduces fall damage.");
        addDescription(Enchantments.FIRE_ASPECT, "Sets target on fire.");
        addDescription(Enchantments.FIRE_PROTECTION, "Reduces fire damage and burn time.");
        addDescription(Enchantments.FLAME, "Arrows set mobs/players on fire, and ignite TNT, campfires, and candles.");
        addDescription(Enchantments.FORTUNE, "Increases the amount of most block drops.");
        addDescription(Enchantments.FROST_WALKER, "Freezes water under foot to allow walking on water, and prevents damage from walking on magma blocks and campfires.");
        addDescription(Enchantments.IMPALING, "Increases damage against aquatic mobs.");
        addDescription(Enchantments.INFINITY, "Shooting consumes no regular arrows.");
        addDescription(Enchantments.KNOCKBACK, "Increases melee knockback.");
        addDescription(Enchantments.LOOTING, "Increases mob loot.");
        addDescription(Enchantments.LOYALTY, "Trident returns after being thrown.");
        addDescription(Enchantments.LUCK_OF_THE_SEA, "Increases rate of catching treasure, rather than junk.");
        addDescription(Enchantments.LURE, "Decreases waiting time between catches.");
        addDescription(Enchantments.MENDING, "Causes collected XP orbs to repair held items, rather than increasing experience.");
        addDescription(Enchantments.MULTISHOT, "Simultaneously shoots 3 arrows at the cost of one.");
        addDescription(Enchantments.PIERCING, "Arrows pass through shields and multiple entities.");
        addDescription(Enchantments.POWER, "Increases arrow damage.");
        addDescription(Enchantments.PROJECTILE_PROTECTION, "Reduces projectile damage.");
        addDescription(Enchantments.PROTECTION, "Reduces most forms of damage.");
        addDescription(Enchantments.PUNCH, "Increases arrow knockback.");
        addDescription(Enchantments.QUICK_CHARGE, "Decreases crossbow charging time.");
        addDescription(Enchantments.RESPIRATION, "Extends underwater breathing time.");
        addDescription(Enchantments.RIPTIDE, "Trident launches player with itself when thrown while in water or rain.");
        addDescription(Enchantments.SHARPNESS, "Increases melee damage.");
        addDescription(Enchantments.SILK_TOUCH, "Most mined blocks drop themselves.");
        addDescription(Enchantments.SMITE, "Increases damage to the undead.");
        addDescription(Enchantments.SOUL_SPEED, "Increases movement speed on soul sand and soul soil.");
        addDescription(Enchantments.SWEEPING_EDGE, "Increases sweeping attack damage.");
        addDescription(Enchantments.SWIFT_SNEAK, "Increases sneaking speed.");
        addDescription(Enchantments.THORNS, "Taking damage causes the attacker to also take damage, at the cost of reducing item durability.");
        addDescription(Enchantments.UNBREAKING, "Reduces durability damage.");
        addDescription(Enchantments.WIND_BURST, "Launches the attacker upward, allowing attacks to be chained together.");
    }
}
