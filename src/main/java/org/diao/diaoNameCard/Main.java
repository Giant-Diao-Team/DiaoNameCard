package org.diao.diaoNameCard;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.diao.diaoNameCard.command.DncCommand;
import org.diao.diaoNameCard.listener.PacketListener;
import org.diao.diaoNameCard.manager.CardManager;
import org.diao.diaoNameCard.manager.PlayerDataManager;
import org.diao.diaoNameCard.storage.Database;
import org.diao.diaoNameCard.storage.MySQL;
import org.diao.diaoNameCard.storage.SQLite;

/**
 * 大貂名片 (DiaoNameCard) 插件的主类
 * 负责插件的加载、卸载、初始化各个模块以及提供核心对象的访问。
 *
 * 作者: 止
 * 团队: 大貂team
 * GitHub: https://github.com/DiaoTeam/DiaoNameCard 感谢支持
 */
public final class Main extends JavaPlugin {

    // 成员变量，用于持有各个管理器的实例
    private CardManager cardManager;
    private PlayerDataManager playerDataManager;
    private Database database;

    /**
     * 当插件被启用时调用
     * 这是插件所有功能的入口点。
     */
    @Override
    public void onEnable() {
        // --- 狂拽酷炫的加载提示 (使用 Bukkit API 发送彩色消息) ---
        ConsoleCommandSender console = Bukkit.getConsoleSender();

        console.sendMessage(ChatColor.YELLOW + "************************************************************");
        console.sendMessage(ChatColor.YELLOW + "*");
        console.sendMessage(ChatColor.YELLOW + "*    " + ChatColor.AQUA + "██████╗   ██╗   █████╗   ██████╗    " + ChatColor.GOLD + "大貂名片");
        console.sendMessage(ChatColor.YELLOW + "*    " + ChatColor.AQUA + "██╔══██╗  ██║  ██╔══██╗ ██╔═══██╗   " + ChatColor.GRAY + "(DiaoNameCard)");
        console.sendMessage(ChatColor.YELLOW + "*    " + ChatColor.AQUA + "██║  ██║  ██║  ███████║ ██║   ██║");
        console.sendMessage(ChatColor.YELLOW + "*    " + ChatColor.AQUA + "██║  ██║  ██║  ██╔══██║ ██║   ██║   " + ChatColor.GREEN + "作者: 止");
        console.sendMessage(ChatColor.YELLOW + "*    " + ChatColor.AQUA + "██████╔╝  ██║  ██║  ██║ ╚██████╔╝   " + ChatColor.GREEN + "团队: 大貂team");
        console.sendMessage(ChatColor.YELLOW + "*    " + ChatColor.AQUA + "╚═════╝   ╚═╝  ╚═╝  ╚═╝  ╚═════╝ ");
        console.sendMessage(ChatColor.YELLOW + "*");
        console.sendMessage(ChatColor.YELLOW + "*      " + ChatColor.WHITE + "正在启动, 请稍候...");
        console.sendMessage(ChatColor.YELLOW + "*");
        console.sendMessage(ChatColor.YELLOW + "************************************************************");

        // --- 前置插件检测 ---
        if (getServer().getPluginManager().getPlugin("CloudPick") == null) {
            getLogger().severe("************************************************************");
            getLogger().severe("* [致命错误] 未找到核心前置插件: CloudPick !");
            getLogger().severe("* 大貂名片插件依赖 CloudPick 插件进行客户端通信。");
            getLogger().severe("* 请确保您已将 CloudPick-Bukkit.jar 放入 plugins 文件夹。");
            getLogger().severe("* 插件将自动禁用以防止服务器出错。");
            getLogger().severe("************************************************************");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("[大貂名片] 成功挂钩到前置插件 CloudPick, 插件将继续加载...");

        if (getServer().getPluginManager().getPlugin("DiaoCore") == null) {
            getLogger().severe("************************************************************");
            getLogger().severe("* [致命错误] 未找到核心前置插件: DiaoCore !");
            getLogger().severe("* 大貂名片插件依赖 DiaoCore 插件进行大貂化。");
            getLogger().severe("* 请确保您已将 DiaoCore 插件放入 plugins 文件夹。");
            getLogger().severe("* 没有大貂核心就像 貂 没有 蛋蛋 肯定起不了作用");
            getLogger().severe("* 插件将自动禁用以防止服务器出错。");
            getLogger().severe("************************************************************");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("[大貂名片] 成功挂钩到前置插件 CloudPick, 插件将继续加载...");

        // 1. 保存默认配置文件
        saveDefaultConfig();

        // 2. 初始化核心管理器
        this.cardManager = new CardManager(this);
        this.playerDataManager = new PlayerDataManager(this);

        // 3. 初始化数据库连接
        setupDatabase();

        // 4. 从配置文件加载所有名片信息到内存中
        this.cardManager.loadCards();

        // 5. 注册命令处理器和 Tab 补全器
        DncCommand dncCommand = new DncCommand(this);
        getCommand("dnc").setExecutor(dncCommand);
        getCommand("dnc").setTabCompleter(dncCommand);

        // 6. 注册事件监听器
        getServer().getPluginManager().registerEvents(new PacketListener(this), this);

        getLogger().info("[大貂名片] 插件已成功启用！尽情享受吧！");
    }

    /**
     * 当插件被禁用时调用
     */
    @Override
    public void onDisable() {
        if (database != null) {
            database.disconnect();
        }

        ConsoleCommandSender console = Bukkit.getConsoleSender();
        console.sendMessage(ChatColor.RED + "************************************************************");
        console.sendMessage(ChatColor.RED + "*");
        console.sendMessage(ChatColor.RED + "*      " + ChatColor.AQUA + "大貂名片 (DiaoNameCard)");
        console.sendMessage(ChatColor.RED + "*");
        console.sendMessage(ChatColor.RED + "*      " + ChatColor.WHITE + "插件已成功卸载。");
        console.sendMessage(ChatColor.RED + "*      " + ChatColor.GRAY + "感谢您的使用, 后会有期!");
        console.sendMessage(ChatColor.RED + "*");
        console.sendMessage(ChatColor.RED + "************************************************************");
    }

    private void setupDatabase() {
        String storageType = getConfig().getString("storage.type", "sqlite").toLowerCase();

        if (storageType.equals("mysql")) {
            this.database = new MySQL(this);
            getLogger().info("[大貂名片] 正在连接至 MySQL 数据库...");
        } else {
            this.database = new SQLite(this);
            getLogger().info("[大貂名片] 正在使用 SQLite 数据库...");
        }

        this.database.connect();
    }

    // --- Getter 方法 ---
    public CardManager getCardManager() {
        return cardManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public Database getDatabase() {
        return database;
    }
}