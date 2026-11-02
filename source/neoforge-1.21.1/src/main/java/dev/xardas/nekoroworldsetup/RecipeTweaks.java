package dev.xardas.nekoroworldsetup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;

/**
 * Removes and adds recipes from recipes.json. Runs when the server starts and after every /reload, once
 * tags are ready: while recipes are being read the tags are not bound yet, so item tags would not match.
 */
public final class RecipeTweaks {
    private RecipeTweaks() { }

    static void apply(MinecraftServer server) {
        RecipeManager manager = server.getRecipeManager();
        HolderLookup.Provider registries = server.registryAccess();
        WsConfig.Recipes cfg = WsConfig.get().recipes;
        boolean adding = cfg.slabsToBlocks || cfg.stairsToBlocks || cfg.logsToSticks || cfg.logsToChests;
        if (!cfg.remove && !adding) return;

        List<RecipeHolder<?>> recipes = new ArrayList<>();
        int removed = 0;
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            if (cfg.remove && shouldRemove(cfg, holder, registries)) removed++;
            else recipes.add(holder);
        }
        List<RecipeHolder<?>> added = new ArrayList<>();
        if (cfg.slabsToBlocks || cfg.stairsToBlocks) addBlockRecipes(cfg, recipes, registries, added);
        if (cfg.logsToSticks) {
            added.add(shaped("logs_to_sticks", Map.of('#', Ingredient.of(ItemTags.LOGS)),
                    new ItemStack(Items.STICK, 16), "#", "#"));
        }
        if (cfg.logsToChests) {
            added.add(shaped("logs_to_chests", Map.of('#', Ingredient.of(ItemTags.LOGS)),
                    new ItemStack(Items.CHEST, 4), "###", "# #", "###"));
        }
        recipes.addAll(added);
        manager.replaceRecipes(recipes);
        NekoroWorldSetup.LOGGER.info("Recipes: removed {}, added {}", removed, added.size());
    }

    private static boolean shouldRemove(WsConfig.Recipes cfg, RecipeHolder<?> holder, HolderLookup.Provider registries) {
        if (cfg.removeIds.matches(holder.id(), tag -> false)) return true;
        if (cfg.removeOutputs.isEmpty()) return false;
        ItemStack result;
        try {
            result = holder.value().getResultItem(registries);
        } catch (RuntimeException e) {
            return false;
        }
        if (result.isEmpty()) return false;
        return cfg.removeOutputs.matches(BuiltInRegistries.ITEM.getKey(result.getItem()),
                tag -> result.is(TagKey.create(Registries.ITEM, tag)));
    }

    /** Slabs and stairs back into blocks, found from the normal crafting recipes, so modded blocks work too. */
    private static void addBlockRecipes(WsConfig.Recipes cfg, List<RecipeHolder<?>> recipes,
                                        HolderLookup.Provider registries, List<RecipeHolder<?>> out) {
        Set<Item> done = new HashSet<>();
        for (RecipeHolder<?> holder : recipes) {
            if (!(holder.value() instanceof ShapedRecipe recipe)) continue;
            ItemStack result = recipe.getResultItem(registries);
            if (!(result.getItem() instanceof BlockItem blockItem) || done.contains(result.getItem())) continue;
            Item source = singleItem(recipe.getIngredients());
            if (source == null || source == result.getItem()) continue;
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(result.getItem());
            String name = id.getNamespace() + "/" + id.getPath();

            if (cfg.slabsToBlocks && blockItem.getBlock() instanceof SlabBlock && result.getCount() == 6
                    && recipe.getWidth() == 3 && recipe.getHeight() == 1) {
                out.add(shaped("slab_to_block/" + name, Map.of('#', Ingredient.of(result.getItem())),
                        new ItemStack(source), "#", "#"));
                done.add(result.getItem());
            } else if (cfg.stairsToBlocks && blockItem.getBlock() instanceof StairBlock && result.getCount() == 4
                    && recipe.getWidth() == 3 && recipe.getHeight() == 3 && countFilled(recipe.getIngredients()) == 6) {
                out.add(shaped("stairs_to_block/" + name, Map.of('#', Ingredient.of(result.getItem())),
                        new ItemStack(source, 3), "##", "##"));
                done.add(result.getItem());
            }
        }
    }

    /** The one item used in every filled slot, or null if the slots use tags, several items or different ones. */
    private static Item singleItem(NonNullList<Ingredient> ingredients) {
        Item found = null;
        for (Ingredient ingredient : ingredients) {
            if (ingredient.isEmpty()) continue;
            ItemStack[] items = ingredient.getItems();
            if (items.length != 1) return null;
            Item item = items[0].getItem();
            if (found != null && found != item) return null;
            found = item;
        }
        return found;
    }

    private static int countFilled(NonNullList<Ingredient> ingredients) {
        int count = 0;
        for (Ingredient ingredient : ingredients) {
            if (!ingredient.isEmpty()) count++;
        }
        return count;
    }

    private static RecipeHolder<ShapedRecipe> shaped(String path, Map<Character, Ingredient> key, ItemStack result, String... pattern) {
        ShapedRecipe recipe = new ShapedRecipe("", CraftingBookCategory.BUILDING, ShapedRecipePattern.of(key, pattern), result);
        return new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath(NekoroWorldSetup.MOD_ID, path), recipe);
    }
}
