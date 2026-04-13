package com.psycho.commands;

import com.psycho.Psycho;
import com.psycho.services.MlModelService;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class ModelCommand implements SubCommand {
    private final Psycho plugin;

    public ModelCommand(Psycho plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "model";
    }

    @Override
    public String getPermission() {
        return "psycho.command.model";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /psycho model <load|unload|reload>");
            return;
        }

        MlModelService.Result result = switch (args[1].toLowerCase()) {
            case "load" -> plugin.getMlModelService().load();
            case "unload" -> plugin.getMlModelService().unload();
            case "reload" -> plugin.getMlModelService().reload();
            default -> null;
        };

        if (result == null) {
            sender.sendMessage("§cUsage: /psycho model <load|unload|reload>");
            return;
        }

        sender.sendMessage(result.message());
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length != 2) {
            return completions;
        }

        String input = args[1].toLowerCase();
        if ("load".startsWith(input)) completions.add("load");
        if ("unload".startsWith(input)) completions.add("unload");
        if ("reload".startsWith(input)) completions.add("reload");
        return completions;
    }
}
