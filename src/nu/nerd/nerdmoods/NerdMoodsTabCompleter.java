package nu.nerd.nerdmoods;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class NerdMoodsTabCompleter implements TabCompleter {
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull Command command,
                                                @NotNull String alias,
                                                String @NotNull [] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String arg = args[0].toLowerCase();

            switch (command.getName().toLowerCase()) {
                case "ptime" -> {
                    if ("day".startsWith(arg)) completions.add("day");
                    if ("night".startsWith(arg)) completions.add("night");
                    if ("reset".startsWith(arg)) completions.add("reset");
                }
                case "prain" -> {
                    if ("on".startsWith(arg)) completions.add("on");
                    if ("off".startsWith(arg)) completions.add("off");
                    if ("reset".startsWith(arg)) completions.add("reset");
                }
            }
        }
        return completions;
    }
}