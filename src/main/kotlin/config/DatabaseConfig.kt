package org.example.comptable.config

import java.io.File
import java.util.Properties

data class DatabaseConfig(
    var url: String = "jdbc:mysql://localhost:3306/comptable",
    var user: String = "root",
    var password: String = "",
    var driverClassName: String = "com.mysql.cj.jdbc.Driver"
) {
    companion object {
        private const val CONFIG_FILE = "database.properties"

        fun load(): DatabaseConfig {
            val properties = Properties()
            val configFile = File(CONFIG_FILE)

            return if (configFile.exists()) {
                configFile.inputStream().use { properties.load(it) }
                DatabaseConfig(
                    url = properties.getProperty("db.url", "jdbc:mysql://localhost:3306/comptable"),
                    user = properties.getProperty("db.user", "root"),
                    password = properties.getProperty("db.password", ""),
                    driverClassName = properties.getProperty("db.driverClassName", "com.mysql.cj.jdbc.Driver")
                )
            } else {
                DatabaseConfig()
            }
        }

        fun save(config: DatabaseConfig) {
            val properties = Properties()
            properties.setProperty("db.url", config.url)
            properties.setProperty("db.user", config.user)
            properties.setProperty("db.password", config.password)
            properties.setProperty("db.driverClassName", config.driverClassName)

            File(CONFIG_FILE).outputStream().use { properties.store(it, "Database Configuration") }
        }
    }
}