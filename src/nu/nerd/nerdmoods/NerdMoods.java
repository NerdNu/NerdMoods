package nu.nerd.nerdmoods;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;

import org.bukkit.ChatColor;
import org.bukkit.Color;
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

// ----------------------------------------------------------------------------
/**
 * Main plugin class.
 */
public class NerdMoods extends JavaPlugin implements Listener {
    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {
        saveDefaultConfig();
        CONFIG = new Configuration(this);
        CONFIG.reload();

        _protocolManager = ProtocolLibrary.getProtocolManager();
        _protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Server.UPDATE_TIME) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (event.getPacketType() == PacketType.Play.Server.UPDATE_TIME &&
                    _ignoringTime.contains(event.getPlayer()) &&
                    event.getPacket().getLongs().read(1) >= 0) {
                    event.setCancelled(true);
                }
            }
        });
    }

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onDisable()
     */
    @Override
    public void onDisable() {
        _protocolManager.removePacketListeners(this);
    }

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onCommand(org.bukkit.command.CommandSender,
     *      org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("nerdmoods")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("help")) {
                // Let Bukkit show usage help from plugin.yml.
                return false;
            } else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                CONFIG.reload();
                sender.sendMessage(ChatColor.AQUA + getName() + " configuration reloaded.");
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
     * @param args command arguments.
     */
    protected void cmdPtime(CommandSender sender, String[] args) {
        if (!CONFIG.ALLOW_PERSONAL_TIME) {
            sender.sendMessage(ChatColor.RED + "That command is disabled.");
            return;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("You need to be in-game to use this command.");
            return;
        }

        Player player = (Player) sender;
        World world = player.getWorld();

        long time = world.getFullTime();
        boolean synchronise = false;
        boolean showUsage = false;

        if (args.length == 0) {
            synchronise = true;
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

        if (showUsage) {
            sender.sendMessage(ChatColor.RED + "Usage: /ptime [day|night|<time>]");
            return;
        }

        if (time < 0) {
            time += 24000;
        }

        // Use reflection to call CraftWorld.getHandle().getTime()
        // rather than adding an NMS dependency.
        boolean reflectionSucceeded = false;
        Long worldTime = 0L;
        try {
            Method getHandle = world.getClass().getMethod("getHandle");
            Object nmsWorld = getHandle.invoke(world);
            Method getTime = nmsWorld.getClass().getMethod("getTime");
            worldTime = (Long) getTime.invoke(nmsWorld);
            reflectionSucceeded = true;
        } catch (Exception ex) {
        }

        if (!reflectionSucceeded) {
            player.sendMessage(Color.RED + "/ptime is not working. Please report this to an admin.");
            return;
        }

        PacketContainer timePacket = _protocolManager.createPacket(PacketType.Play.Server.UPDATE_TIME);
        timePacket.getLongs().write(0, worldTime);
        timePacket.getLongs().write(1, synchronise ? time : (time == 0 ? -1 : -time));
        try {
            _protocolManager.sendServerPacket((Player) sender, timePacket);
            if (synchronise) {
                _ignoringTime.remove(sender);
            } else {
                _ignoringTime.add((Player) sender);
            }
            sender.sendMessage(ChatColor.AQUA + (synchronise ? "Normal time resumed." : "Time set to " + time + "."));
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Cannot send packet " + timePacket, e);
        }
    } // cmdPtime

    // --------------------------------------------------------------------------
    /**
     * Handle /prain [on|off].
     *
     * @param sender the command sender.
     * @param args command arguments.
     */
    protected void cmdPrain(CommandSender sender, String[] args) {
        if (!CONFIG.ALLOW_PERSONAL_WEATHER) {
            sender.sendMessage(ChatColor.RED + "That command is disabled.");
            return;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("You need to be in-game to use this command.");
            return;
        }

        Player player = (Player) sender;
        boolean rain = false;
        boolean synchronise = false;
        boolean showUsage = false;

        if (args.length == 0) {
            synchronise = true;
            rain = player.getLocation().getWorld().hasStorm();
        } else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("on")) {
                rain = true;
            } else if (args[0].equalsIgnoreCase("off")) {
                rain = false;
            } else {
                showUsage = true;
            }
        } else if (args.length > 1) {
            showUsage = true;
        }

        if (showUsage) {
            sender.sendMessage(ChatColor.RED + "Usage: /prain [on|off]");
            return;
        }

        PacketContainer weatherPacket = _protocolManager.createPacket(PacketType.Play.Server.GAME_STATE_CHANGE);
        weatherPacket.getIntegers().write(0, rain ? 2 : 1);
        weatherPacket.getFloat().write(0, 0F);
        try {
            _protocolManager.sendServerPacket(player, weatherPacket);
            if (synchronise) {
                sender.sendMessage(ChatColor.AQUA + "Normal weather resumed.");
            } else {
                sender.sendMessage(ChatColor.AQUA + (rain ? "Weather enabled." : "Weather disabled."));
            }
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Cannot send packet " + weatherPacket, e);
        }
    } // cmdPrain

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
    protected ProtocolManager _protocolManager;

    /**
     * Set of Players ignoring server-wide time and using their own time.
     */
    protected HashSet<Player> _ignoringTime = new HashSet<Player>();

} // class NerdMoods