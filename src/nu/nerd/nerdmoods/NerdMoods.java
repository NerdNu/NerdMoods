package nu.nerd.nerdmoods;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.WeatherType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

// ----------------------------------------------------------------------------
/**
 * Main plugin class.
 */
public class NerdMoods extends JavaPlugin {

    /**
     * Configuration wrapper instance.
     */

    protected static Configuration CONFIG;

    /**
     * Called when the plugin is enabled.
     * Loads the configuration and registers tab completer.
     */
    @Override
    public void onEnable() {
        saveDefaultConfig();
        CONFIG = new Configuration(this);
        CONFIG.reload();

        // Register tab completer for commands
        NerdMoodsTabCompleter tabCompleter = new NerdMoodsTabCompleter();
        Objects.requireNonNull(getCommand("ptime")).setTabCompleter(tabCompleter);
        Objects.requireNonNull(getCommand("prain")).setTabCompleter(tabCompleter);
    }

    // ------------------------------------------------------------------------
    /**
     * Called when the plugin is disabled.
     */
    @Override
    public void onDisable() {
        //No cleanup needed.
    }

    // ------------------------------------------------------------------------

    /**
     * Handles plugin commands.
     *
     * @param sender  the sender of the command.
     * @param command the command being executed.
     * @param label   the alias of the command used.
     * @param args    command arguments.
     * @return true if the command was handled, false to show usage.
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (command.getName().equalsIgnoreCase("nerdmoods")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("help")) {
                // Let Bukkit show usage help from plugin.yml.
                return false;
            } else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                // Permissions check here
                if (!sender.hasPermission("nerdmoods.admin")) {
                    sender.sendMessage(Component.text("You do not have permission to do that.").color(NamedTextColor.RED));
                    return true;
                }

                CONFIG.reload();
                sender.sendMessage(Component.text(getName() + " configuration reloaded.")
                        .color(NamedTextColor.AQUA));
                return true;
            }
            return false;
        } else if (command.getName().equalsIgnoreCase("prain")) {
            cmdPrain(sender, args);
            return true;
        } else if (command.getName().equalsIgnoreCase("ptime")) {
            cmdPtime(sender, args);
            return true;
        }

        return false;
    } // onCommand

    // --------------------------------------------------------------------------
    /**
     * Handles the /ptime command, allowing players to set their personal time
     * to a fixed value (e.g., day, night, or specific tick) or reset it to match world time.
     * <p>
     * Usage: /ptime [day|night|reset|&lt;time&gt;]
     *
     * @param sender the command sender.
     * @param args   the command arguments.
     */
    protected void cmdPtime(CommandSender sender, String[] args) {
        if (!CONFIG.ALLOW_PERSONAL_TIME) {
            sender.sendMessage(Component.text("That command is disabled.").color(NamedTextColor.RED));
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("You need to be in-game to use this command.").color(NamedTextColor.RED));
            return;
        }

        if (args.length != 1) {
            sender.sendMessage(Component.text("Usage: /ptime [day|night|reset|<time>]").color(NamedTextColor.RED));
            return;
        }

        long time;
        boolean reset = false;
        String input = args[0].toLowerCase();

        switch (input) {
            case "day" -> time = 6000L;
            case "night" -> time = 18000L;
            case "reset" -> {
                reset = true;
                time = 0; // unused when resetting
            }
            default -> {
                try {
                    time = Long.parseLong(input) % 24000L;
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Invalid time value. Use day, night, reset, or a number.").color(NamedTextColor.RED));
                    return;
                }
            }
        }

        if (reset) {
            player.resetPlayerTime();
            sender.sendMessage(Component.text("Your personal time has been reset to normal.").color(NamedTextColor.AQUA));
        } else {
            player.setPlayerTime(time, false); // fixed (frozen) personal time
            sender.sendMessage(Component.text("Your personal time has been set to " + input + ".").color(NamedTextColor.AQUA));
        }
    }
    // cmdPtime

    // --------------------------------------------------------------------------
    /**
     * Handles the /prain command, allowing players to change their personal (client-side)
     * weather to rain, clear, or reset it back to match the server's global weather.
     * <p>
     * This change is visual-only and does not affect the world’s actual weather state.
     * <p>
     * Usage: /prain [on|off|reset]
     *
     * @param sender the command sender (must be a player).
     * @param args   the command arguments; must be one of: on, off, reset.
     */
    protected void cmdPrain(CommandSender sender, String[] args) {
        if (!CONFIG.ALLOW_PERSONAL_WEATHER) {
            sender.sendMessage(Component.text("That command is disabled.").color(NamedTextColor.RED));
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("You need to be in-game to use this command.").color(NamedTextColor.RED));
            return;
        }

        if (args.length != 1) {
            sender.sendMessage(Component.text("Usage: /prain [on|off|reset]").color(NamedTextColor.RED));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "on" -> {
                player.setPlayerWeather(WeatherType.DOWNFALL);
                sender.sendMessage(Component.text("Rain enabled.").color(NamedTextColor.AQUA));
            }
            case "off" -> {
                player.setPlayerWeather(WeatherType.CLEAR);
                sender.sendMessage(Component.text("Rain disabled.").color(NamedTextColor.AQUA));
            }
            case "reset" -> {
                player.resetPlayerWeather();
                sender.sendMessage(Component.text("Normal weather resumed.").color(NamedTextColor.AQUA));
            }
            default -> sender.sendMessage(Component.text("Usage: /prain [on|off|reset]").color(NamedTextColor.RED));
        }
    }
// cmdPrain
} // class NerdMoods