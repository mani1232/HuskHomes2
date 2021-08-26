package me.william278.huskhomes2.migrators;

import me.william278.huskhomes2.HuskHomes;
import org.bukkit.Bukkit;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

public class UpgradeDatabase {

    private static final HuskHomes plugin = HuskHomes.getInstance();

    //todo this stuff doesn't work. TEST BEFORE LAUNCH

    // Upgrade the database system if the config file version is not high enough
    public static void upgradeDatabase() {
        plugin.reloadConfig();
        if (plugin.getConfig().getInt("config_file_version", 1) <= 6) {
            plugin.getLogger().info("Detected that the database might need updating. Running database upgrade...");
            try (PreparedStatement tableUpdateStatement = HuskHomes.getConnection().prepareStatement(
                    "ALTER TABLE " + HuskHomes.getSettings().getPlayerDataTable()
                            + " ADD `is_ignoring_requests` boolean NOT NULL DEFAULT 0, "
                            + "ADD `offline_location_id` integer NULL DEFAULT NULL, "
                            + "ADD FOREIGN KEY (`offline_location_id`) REFERENCES " + HuskHomes.getSettings().getLocationsDataTable() + " (`location_id`) ON DELETE SET NULL ON UPDATE NO ACTION;")) {
                tableUpdateStatement.executeUpdate();
                plugin.getLogger().info("Database update complete!");
            } catch (SQLException e) {
                plugin.getLogger().info("Skipped performing the database upgrade: " + e.getCause() + ". This might be because another server on your HuskHomes network already carried out the upgrade - in which case you can safely ignore this warning.");
                e.printStackTrace();
            } finally {
                // Update the config file version
                plugin.getConfig().set("config_file_version", 7);
                plugin.saveConfig();
            }
        }
        if (plugin.getConfig().getInt("config_file_version", 1) <= 7) {
            plugin.getLogger().info("Detected that the database might need updating. Running database upgrade...");
            try (PreparedStatement tableUpdateStatement = HuskHomes.getConnection().prepareStatement(
                    "ALTER TABLE " + HuskHomes.getSettings().getHomesDataTable()
                            + " ADD `creation_time` timestamp NOT NULL DEFAULT 0;")) {
                tableUpdateStatement.execute();
                try (PreparedStatement timeSettingStatement = HuskHomes.getConnection().prepareStatement(
                        "UPDATE " + HuskHomes.getSettings().getHomesDataTable() + " SET `creation_time`=CURRENT_TIMESTAMP;")) {
                    timeSettingStatement.executeUpdate();
                }
                plugin.getLogger().info("Database update complete!");
            } catch (SQLException e) {
                plugin.getLogger().info("Skipped performing the database upgrade: " + e.getCause() + ". This might be because another server on your HuskHomes network already carried out the upgrade - in which case you can safely ignore this warning.");
                e.printStackTrace();
            } finally {
                try (PreparedStatement tableUpdateStatement = HuskHomes.getConnection().prepareStatement(
                        "ALTER TABLE " + HuskHomes.getSettings().getWarpsDataTable()
                                + " ADD `creation_time` timestamp NOT NULL DEFAULT 0;")) {
                    tableUpdateStatement.execute();
                    try (PreparedStatement timeSettingStatement = HuskHomes.getConnection().prepareStatement(
                            "UPDATE " + HuskHomes.getSettings().getWarpsDataTable() + " SET `creation_time`=CURRENT_TIMESTAMP;")) {
                        timeSettingStatement.executeUpdate();
                    }
                    plugin.getLogger().info("Database update complete!");
                } catch (SQLException e) {
                    plugin.getLogger().info("Skipped performing the database upgrade: " + e.getCause() + ". This might be because another server on your HuskHomes network already carried out the upgrade - in which case you can safely ignore this warning.");
                    e.printStackTrace();
                } finally {
                    // Update the config file version
                    plugin.getConfig().set("config_file_version", 8);
                    plugin.saveConfig();
                }
            }

        }
    }

}