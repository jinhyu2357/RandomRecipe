package org.example.jinhhyu.randomRecipe

import org.bukkit.plugin.java.JavaPlugin

class RandomRecipe : JavaPlugin() {

    override fun onEnable() {
        saveDefaultConfig()

        if (config.getBoolean("announce-on-enable", true)) {
            logger.info(config.getString("enable-message") ?: "RandomRecipe 플러그인이 활성화되었습니다.")
        }
    }

    override fun onDisable() {
        if (config.getBoolean("announce-on-disable", true)) {
            logger.info(config.getString("disable-message") ?: "RandomRecipe 플러그인이 비활성화되었습니다.")
        }
    }
}
