package org.diao.diaoNameCard.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.diao.diaoNameCard.Main;
import org.diao.diaoNameCard.model.NameCard;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DncCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public DncCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "========== " + ChatColor.AQUA + "大貂名片 帮助" + ChatColor.GOLD + " ==========");

            boolean hasAnyCommand = false;
            if (sender.hasPermission("diaonamecard.player.set")) {
                sender.sendMessage(ChatColor.YELLOW + "/dnc set <名片ID>" + ChatColor.GRAY + " - 佩戴你拥有的名片");
                hasAnyCommand = true;
            }
            if (sender.hasPermission("diaonamecard.admin.reload")) {
                sender.sendMessage(ChatColor.YELLOW + "/dnc reload" + ChatColor.GRAY + " - 重载插件配置文件");
                hasAnyCommand = true;
            }
            if (sender.hasPermission("diaonamecard.admin.add")) {
                sender.sendMessage(ChatColor.YELLOW + "/dnc add <玩家> <名片ID>" + ChatColor.GRAY + " - 给予玩家名片");
                hasAnyCommand = true;
            }
            if (sender.hasPermission("diaonamecard.admin.remove")) {
                sender.sendMessage(ChatColor.YELLOW + "/dnc remove <玩家> <名片ID>" + ChatColor.GRAY + " - 移除玩家名片");
                hasAnyCommand = true;
            }

            if (!hasAnyCommand) {
                sender.sendMessage(ChatColor.RED + "你当前没有任何可用命令。");
            }

            sender.sendMessage(ChatColor.GOLD + "===================================");
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "reload":
                handleReload(sender);
                break;
            case "add":
                handleAdd(sender, args);
                break;
            case "remove":
                handleRemove(sender, args);
                break;
            case "set":
                handleSet(sender, args);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "未知子命令，请输入 /dnc 查看帮助。");
                break;
        }
        return true;
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("diaonamecard.admin.reload")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令。");
            return;
        }
        plugin.getCardManager().loadCards();
        sender.sendMessage(ChatColor.GREEN + "大貂名片插件配置已重载。");
    }

    private void handleAdd(CommandSender sender, String[] args) {
        if (!sender.hasPermission("diaonamecard.admin.add")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令。");
            return;
        }
        if (args.length != 3) {
            sender.sendMessage(ChatColor.RED + "用法: /dnc add <玩家名> <名片ID>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        String cardId = args[2];

        if (target == null) {
            sender.sendMessage(ChatColor.RED + "玩家 " + args[1] + " 不在线。");
            return;
        }
        if (plugin.getCardManager().getCard(cardId) == null) {
            sender.sendMessage(ChatColor.RED + "名片ID " + cardId + " 不存在。");
            return;
        }

        plugin.getPlayerDataManager().givePlayerCard(target.getUniqueId(), cardId);
        sender.sendMessage(ChatColor.GREEN + "已成功给予玩家 " + target.getName() + " 名片: " + cardId);
        target.sendMessage(ChatColor.GREEN + "你获得了一张新的名片: " + cardId);
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("diaonamecard.admin.remove")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令。");
            return;
        }
        if (args.length != 3) {
            sender.sendMessage(ChatColor.RED + "用法: /dnc remove <玩家名> <名片ID>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        String cardId = args[2];

        if (target == null) {
            sender.sendMessage(ChatColor.RED + "玩家 " + args[1] + " 不在线。");
            return;
        }

        plugin.getPlayerDataManager().removePlayerCard(target.getUniqueId(), cardId);
        sender.sendMessage(ChatColor.GREEN + "已成功移除玩家 " + target.getName() + " 的名片: " + cardId);
        target.sendMessage(ChatColor.RED + "你的名片 " + cardId + " 已被移除。");
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "该命令只能由玩家执行。");
            return;
        }
        if (!sender.hasPermission("diaonamecard.player.set")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令。");
            return;
        }
        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "用法: /dnc set <名片ID>");
            return;
        }

        Player player = (Player) sender;
        String cardId = args[1];

        if (plugin.getCardManager().getCard(cardId) == null) {
            player.sendMessage(ChatColor.RED + "名片ID " + cardId + " 不存在。");
            return;
        }

        plugin.getPlayerDataManager().playerHasCard(player.getUniqueId(), cardId).thenAccept(hasCard -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (hasCard) {
                    plugin.getPlayerDataManager().setEquippedCard(player.getUniqueId(), cardId);
                    player.sendMessage(ChatColor.GREEN + "你已成功佩戴名片: " + cardId);
                } else {
                    player.sendMessage(ChatColor.RED + "你尚未拥有该名片，无法佩戴。");
                }
            });
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>();
            if (sender.hasPermission("diaonamecard.admin.reload")) subCommands.add("reload");
            if (sender.hasPermission("diaonamecard.admin.add")) subCommands.add("add");
            if (sender.hasPermission("diaonamecard.admin.remove")) subCommands.add("remove");
            if (sender.hasPermission("diaonamecard.player.set")) subCommands.add("set");
            return subCommands.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("set") && sender.hasPermission("diaonamecard.player.set")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    List<String> ownedCardIds = new ArrayList<>();
                    plugin.getPlayerDataManager().getPlayerOwnedCards(player.getUniqueId())
                            .thenAccept(cards -> ownedCardIds.addAll(cards.stream().map(NameCard::getId).collect(Collectors.toList())))
                            .join();
                    return ownedCardIds.stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                }
            }
            if ((args[0].equalsIgnoreCase("add") && sender.hasPermission("diaonamecard.admin.add")) ||
                    (args[0].equalsIgnoreCase("remove") && sender.hasPermission("diaonamecard.admin.remove"))) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3) {
            if ((args[0].equalsIgnoreCase("add") && sender.hasPermission("diaonamecard.admin.add")) ||
                    (args[0].equalsIgnoreCase("remove") && sender.hasPermission("diaonamecard.admin.remove"))) {
                return plugin.getCardManager().getAllCards().stream().map(NameCard::getId)
                        .filter(id -> id.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}