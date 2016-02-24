package nu.nerd.nerdmoods;

import org.bukkit.plugin.java.JavaPlugin;

// ----------------------------------------------------------------------------
/**
 * Configuration wrapper class.
 */
public class Configuration {
    public boolean ALLOW_PERSONAL_WEATHER;
    public boolean ALLOW_PERSONAL_TIME;

    // ------------------------------------------------------------------------
    /**
     * Constructor.
     *
     * @param plugin the owning plugin.
     */
    public Configuration(JavaPlugin plugin) {
        _plugin = plugin;
    }

    // ------------------------------------------------------------------------
    /**
     * Reload the configuration.
     */
    public void reload() {
        _plugin.reloadConfig();

        ALLOW_PERSONAL_WEATHER = _plugin.getConfig().getBoolean("allow-personal-weather");
        ALLOW_PERSONAL_TIME = _plugin.getConfig().getBoolean("allow-personal-time");
    }

    // ------------------------------------------------------------------------
    /**
     * The owning plugin.
     */
    protected JavaPlugin _plugin;
} // class Configuration