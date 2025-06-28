package nu.nerd.nerdmoods;

import java.util.HashSet;
import java.util.Objects;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import org.jetbrains.annotations.NotNull;

// ----------------------------------------------------------------------------
/**
 * Main plugin class.
 */
public class NerdMoods extends JavaPlugin implements Listener {
    // ------------------------------------------------------------------------
    /**
     * @see JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {
        saveDefaultConfig();
        CONFIG = new Configuration(this);
        CONFIG.reload();

        protocolManager = ProtocolLibrary.getProtocolManager();
        protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Server.UPDATE_TIME) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (event.getPacketType() == PacketType.Play.Server.UPDATE_TIME &&
                    _ignoringTime.contains(event.getPlayer()) &&
                    event.getPacket().getLongs().read(1) >= 0) {
                    event.setCancelled(true);
                }
            }
        });

        // Register tab completer
        NerdMoodsTabCompleter tabCompleter = new NerdMoodsTabCompleter();
        Objects.requireNonNull(getCommand("ptime")).setTabCompleter(tabCompleter);
        Objects.requireNonNull(getCommand("prain")).setTabCompleter(tabCompleter);
    }

    // ------------------------------------------------------------------------
    /**
     * @see JavaPlugin#onDisable()
     */
    @Override
    public void onDisable() {
        protocolManager.removePacketListeners(this);
    }

    // ------------------------------------------------------------------------
    /**
     * @see JavaPlugin#onCommand(CommandSender,
     *      Command, String, String[])
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (command.getName().equalsIgnoreCase("nerdmoods")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("help")) {
                // Let Bukkit show usage help from plugin.yml.
                return false;
            } else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
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
     * Handle /ptime [day|night|<time>].
     *
     * @param sender the command sender.
     * @param args   command arguments.
     */
    protected void cmdPtime(CommandSender sender, String[] args) {
        if (!CONFIG.ALLOW_PERSONAL_TIME) {
            sender.sendMessage(Component.text("That command is disabled.").color(NamedTextColor.RED));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("You need to be in-game to use this command.");
            return;
        }

        World world = player.getWorld();

        boolean synchronise = false;
        boolean showUsage = false;
        long time = -1L;

        if (args.length == 0) {
            synchronise = true;
            time = world.getFullTime();
        } else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("day")) {
                time = 6000L;
            } else if (args[0].equalsIgnoreCase("night")) {
                time = 18000L;
            } else {
                try {
                    time = Long.parseLong(args[0]) % 24000;
                } catch (NumberFormatException e) {
                    showUsage = true;
                }
            }
        } else {
            showUsage = true;
        }

        if (showUsage || time < 0) {
            sender.sendMessage("Usage: /time [day|night|<number>]");
            return;
        }

        world.setTime(time);
        long worldTime = world.getTime();

        PacketContainer timePacket = protocolManager.createPacket(PacketType.Play.Server.UPDATE_TIME);
        timePacket.getLongs().write(0, worldTime);
        timePacket.getLongs().write(1, synchronise ? time : (time == 0 ? -1 : time));
        try {
            protocolManager.sendServerPacket(player, timePacket);
            if (synchronise) {
                _ignoringTime.remove(sender);
            } else {
                _ignoringTime.add(player);
            }
            sender.sendMessage(Component.text(synchronise ? "Normal time resumed." : "Time set to " + time + ".")
                    .color(NamedTextColor.AQUA));
        } catch (Exception e) {
            throw new RuntimeException("Cannot send packet " + timePacket, e);
        }
    } // cmdPtime

    // --------------------------------------------------------------------------
    /**
     * Handle /prain [on|off].
     *
     * @param sender the command sender.
     * @param args   command arguments.
     */

    @SuppressWarnings("DataFlowIssue")
    protected void cmdPrain(CommandSender sender, String[] args) {
        if (!CONFIG.ALLOW_PERSONAL_WEATHER) {
            sender.sendMessage(Component.text("That command is disabled.").color(NamedTextColor.RED));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("You need to be in-game to use this command.").color(NamedTextColor.RED));
            return;
        }

        boolean rain = false;
        boolean synchronise = false;
        boolean showUsage = false;

        if (args.length == 0) {
            synchronise = true;
            rain = player.getWorld().hasStorm();
        } else if (args.length == 1) {
            switch (args[0].toLowerCase()) {
                case "on" -> rain = true;
                case "off" -> rain = false;
                default -> showUsage = true;
            }
        } else {
            showUsage = true;
        }

        if (showUsage) {
            sender.sendMessage(Component.text("Usage: /prain [on|off]").color(NamedTextColor.RED));
            return;
        }

        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.GAME_STATE_CHANGE);
        packet.getGameStateIDs().write(0, rain ? 2 : 1);
        packet.getFloat().write(0, 0F);

        try {
            protocolManager.sendServerPacket(player, packet);
            sender.sendMessage(Component.text(
                            synchronise ? "Normal weather resumed." : (rain ? "Weather enabled." : "Weather disabled."))
                    .color(NamedTextColor.AQUA));
        } catch (Exception e) {
            throw new RuntimeException("Cannot send packet " + packet, e);
        }
    }
// cmdPrain

    // ------------------------------------------------------------------------
    /**
     * When the player quits, stop tracking their time ignore.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (CONFIG.ALLOW_PERSONAL_TIME) {
            _ignoringTime.remove(event.getPlayer());
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Configuration wrapper.
     */
    protected static Configuration CONFIG;

    /**
     * Used to access ProtocolLib functions.
     */
    protected ProtocolManager protocolManager;

    /**
     * Set of Players ignoring server-wide time and using their own time.
     */
    protected HashSet<Player> _ignoringTime = new HashSet<>();

} // class NerdMoods