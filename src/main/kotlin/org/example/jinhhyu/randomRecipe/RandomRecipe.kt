package org.example.jinhhyu.randomRecipe

import org.bukkit.Keyed
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.inventory.CraftingInventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.concurrent.ThreadLocalRandom
import java.util.WeakHashMap

class RandomRecipe : JavaPlugin(), Listener {

    private enum class RandomizationMode {
        ALWAYS,
        FIRST_RUN
    }

    private lateinit var randomMaterials: List<Material>
    private val previewResults = WeakHashMap<CraftingInventory, ItemStack>()
    private lateinit var recipeDatabaseFile: File
    private lateinit var recipeDatabase: YamlConfiguration
    private var randomizationMode = RandomizationMode.ALWAYS

    override fun onEnable() {
        saveDefaultConfig()
        randomizationMode = loadRandomizationMode()
        randomMaterials = Material.values()
            .asSequence()
            .filter { it.isItem && it != Material.AIR }
            .toList()

        loadOrCreateRecipeDatabase()
        if (randomizationMode == RandomizationMode.FIRST_RUN) {
            initializeMissingRecipeMappings()
        }

        server.pluginManager.registerEvents(this, this)
    }

    @EventHandler
    fun onPrepareCraft(event: PrepareItemCraftEvent) {
        val recipe = event.recipe ?: run {
            previewResults.remove(event.inventory)
            return
        }
        if (!shouldApplyToMatrix(event.inventory.matrix.size)) {
            previewResults.remove(event.inventory)
            return
        }

        val resultToShow = createResultForRecipe(recipe, recipe.result.amount) ?: return
        event.inventory.result = resultToShow
        previewResults[event.inventory] = resultToShow.clone()
    }

    @EventHandler
    fun onCraftItem(event: CraftItemEvent) {
        val recipe = event.recipe
        if (!shouldApplyToMatrix(event.inventory.matrix.size)) {
            previewResults.remove(event.inventory)
            return
        }

        val baseAmount = recipe.result.amount.coerceAtLeast(1)
        val resultToCraft = previewResults[event.inventory]?.clone()
            ?: createResultForRecipe(recipe, baseAmount)?.also { previewResults[event.inventory] = it.clone() }
            ?: return

        event.currentItem = resultToCraft
        event.inventory.result = resultToCraft.clone()
    }

    private fun shouldApplyToMatrix(matrixSize: Int): Boolean {
        val isInventoryCrafting = matrixSize <= 4
        val applyToInventoryCrafting =
            config.getBoolean("apply-random-to-inventory-crafting", false)

        if (isInventoryCrafting && !applyToInventoryCrafting) {
            return false
        }
        return true
    }

    private fun createResultForRecipe(recipe: Recipe, baseAmount: Int): ItemStack? {
        return when (randomizationMode) {
            RandomizationMode.ALWAYS -> createRandomResult(baseAmount)
            RandomizationMode.FIRST_RUN -> createFixedResult(recipe, baseAmount)
        }
    }

    private fun createRandomResult(baseAmount: Int): ItemStack? {
        val randomMaterial = pickRandomMaterial() ?: return null
        val amount = baseAmount.coerceAtLeast(1).coerceAtMost(randomMaterial.maxStackSize)
        return ItemStack(randomMaterial, amount)
    }

    private fun createFixedResult(recipe: Recipe, baseAmount: Int): ItemStack? {
        val recipeKey = getRecipeKey(recipe) ?: return createRandomResult(baseAmount)
        val path = "recipes.${encodeRecipeKey(recipeKey)}"

        val mappedMaterial = recipeDatabase.getString(path)?.let { Material.matchMaterial(it) }
        val resultMaterial = mappedMaterial ?: pickRandomMaterial()?.also {
            recipeDatabase.set(path, it.name)
            saveRecipeDatabase()
        } ?: return null

        val amount = baseAmount.coerceAtLeast(1).coerceAtMost(resultMaterial.maxStackSize)
        return ItemStack(resultMaterial, amount)
    }

    private fun pickRandomMaterial(): Material? {
        if (randomMaterials.isEmpty()) {
            return null
        }
        val randomMaterial = randomMaterials[ThreadLocalRandom.current().nextInt(randomMaterials.size)]
        return randomMaterial
    }

    private fun getRecipeKey(recipe: Recipe): String? {
        val keyedRecipe = recipe as? Keyed ?: return null
        return keyedRecipe.key.toString()
    }

    private fun loadRandomizationMode(): RandomizationMode {
        val rawMode = config.getString("randomization-mode", RandomizationMode.ALWAYS.name)
            ?.trim()
            ?.uppercase()
            ?: RandomizationMode.ALWAYS.name

        return RandomizationMode.entries.firstOrNull { it.name == rawMode } ?: run {
            logger.warning("Invalid randomization-mode '$rawMode'. Using ALWAYS.")
            RandomizationMode.ALWAYS
        }
    }

    private fun loadOrCreateRecipeDatabase() {
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            logger.warning("Could not create plugin data folder: ${dataFolder.absolutePath}")
        }

        recipeDatabaseFile = File(dataFolder, "recipes.yml")
        if (!recipeDatabaseFile.exists()) {
            try {
                recipeDatabaseFile.createNewFile()
                logger.info("Created recipe database: ${recipeDatabaseFile.absolutePath}")
            } catch (exception: IOException) {
                throw IllegalStateException("Failed to create recipe database file.", exception)
            }
        }

        recipeDatabase = YamlConfiguration.loadConfiguration(recipeDatabaseFile)
        if (recipeDatabase.getConfigurationSection("recipes") == null) {
            recipeDatabase.createSection("recipes")
            saveRecipeDatabase()
        }
    }

    private fun initializeMissingRecipeMappings() {
        var createdMappings = 0
        val iterator = server.recipeIterator()
        while (iterator.hasNext()) {
            val recipe = iterator.next()
            val recipeKey = getRecipeKey(recipe) ?: continue
            val path = "recipes.${encodeRecipeKey(recipeKey)}"
            if (recipeDatabase.contains(path)) {
                continue
            }

            val randomMaterial = pickRandomMaterial() ?: continue
            recipeDatabase.set(path, randomMaterial.name)
            createdMappings++
        }

        if (createdMappings > 0) {
            saveRecipeDatabase()
            logger.info("Initialized $createdMappings random recipe mappings.")
        }
    }

    private fun saveRecipeDatabase() {
        try {
            recipeDatabase.save(recipeDatabaseFile)
        } catch (exception: IOException) {
            logger.severe("Failed to save recipe database: ${exception.message}")
        }
    }

    private fun encodeRecipeKey(recipeKey: String): String {
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(recipeKey.toByteArray(StandardCharsets.UTF_8))
    }
}
