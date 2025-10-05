package org.diao.diaoNameCard.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.diao.diaoNameCard.Main;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * MySQL 数据库操作实现类
 * 实现了 Database 接口，负责所有与 MySQL 数据库的交互。
 * 使用 HikariCP 连接池以提高性能和稳定性。
 */
public class MySQL implements Database {

    private final Main plugin;
    private HikariDataSource dataSource;

    public MySQL(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void connect() {
        try {
            // 使用 HikariCP 连接池
            HikariConfig config = new HikariConfig();

            // 从插件配置文件中读取数据库连接信息
            String host = plugin.getConfig().getString("storage.mysql.host", "localhost");
            int port = plugin.getConfig().getInt("storage.mysql.port", 3306);
            String database = plugin.getConfig().getString("storage.mysql.database", "minecraft");
            String username = plugin.getConfig().getString("storage.mysql.username", "user");
            String password = plugin.getConfig().getString("storage.mysql.password", "password");

            // 配置JDBC URL
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
            // 配置数据库用户名和密码
            config.setUsername(username);
            config.setPassword(password);

            // 推荐的 MySQL 连接池设置
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");
            config.addDataSourceProperty("autoReconnect", "true"); // 自动重连
            config.setConnectionTimeout(5000); // 5秒连接超时

            // 创建数据源
            dataSource = new HikariDataSource(config);
            plugin.getLogger().info("MySQL 数据库连接池已成功初始化。");

            // 异步初始化数据库表
            CompletableFuture.runAsync(this::initializeTables);

        } catch (Exception e) {
            plugin.getLogger().severe("无法初始化 MySQL 数据库连接池: " + e.getMessage());
        }
    }

    /**
     * 初始化数据库表结构
     * 如果表不存在，则创建它们。
     */
    private void initializeTables() {
        // 创建玩家元数据表，存储装备的名片ID
        // `uuid` 是主键，确保每个玩家只有一条记录。
        String playerMetaTable = "CREATE TABLE IF NOT EXISTS `player_meta` (" +
                "`uuid` VARCHAR(36) NOT NULL," +
                "`equipped_card_id` VARCHAR(255) NULL," +
                "PRIMARY KEY (`uuid`)" +
                ");";

        // 创建玩家拥有的名片列表
        // `uuid` 和 `card_id` 联合唯一索引，确保玩家不会重复拥有同一个名片。
        String playerCardsTable = "CREATE TABLE IF NOT EXISTS `player_cards` (" +
                "`id` INT NOT NULL AUTO_INCREMENT," +
                "`uuid` VARCHAR(36) NOT NULL," +
                "`card_id` VARCHAR(255) NOT NULL," +
                "PRIMARY KEY (`id`)," +
                "UNIQUE INDEX `uuid_card_id_unique` (`uuid`, `card_id`)" +
                ");";

        // try-with-resources 语句确保连接和声明在使用后自动关闭
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(playerMetaTable);
            statement.execute(playerCardsTable);
            plugin.getLogger().info("数据库表结构检查/创建完成。");
        } catch (SQLException e) {
            plugin.getLogger().severe("创建数据库表时出错: " + e.getMessage());
        }
    }

    @Override
    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("MySQL 数据库连接池已关闭。");
        }
    }

    @Override
    public CompletableFuture<List<String>> getPlayerCards(UUID uuid) {
        // CompletableFuture.supplyAsync 用于执行有返回值的异步任务
        return CompletableFuture.supplyAsync(() -> {
            List<String> cards = new ArrayList<>();
            String sql = "SELECT `card_id` FROM `player_cards` WHERE `uuid` = ?;";
            try (Connection connection = dataSource.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    cards.add(rs.getString("card_id"));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("获取玩家名片列表时出错: " + e.getMessage());
                e.printStackTrace();
            }
            return cards;
        });
    }

    @Override
    public CompletableFuture<Void> addPlayerCard(UUID uuid, String cardId) {
        // CompletableFuture.runAsync 用于执行没有返回值的异步任务
        return CompletableFuture.runAsync(() -> {
            // `INSERT IGNORE` 会在出现重复键（根据UNIQUE索引）时忽略插入，而不是报错
            String sql = "INSERT IGNORE INTO `player_cards` (`uuid`, `card_id`) VALUES (?, ?);";
            try (Connection connection = dataSource.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, cardId);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("为玩家添加名片时出错: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Void> removePlayerCard(UUID uuid, String cardId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM `player_cards` WHERE `uuid` = ? AND `card_id` = ?;";
            try (Connection connection = dataSource.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, cardId);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("移除玩家名片时出错: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<String> getEquippedCard(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT `equipped_card_id` FROM `player_meta` WHERE `uuid` = ?;";
            try (Connection connection = dataSource.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return rs.getString("equipped_card_id");
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("获取玩家佩戴名片时出错: " + e.getMessage());
                e.printStackTrace();
            }
            return null; // 如果没有记录或发生错误，返回 null
        });
    }

    @Override
    public CompletableFuture<Void> setEquippedCard(UUID uuid, String cardId) {
        return CompletableFuture.runAsync(() -> {
            // 使用 MySQL 的 `ON DUPLICATE KEY UPDATE` (UPSERT) 语法
            // 如果 `uuid` 已存在，则更新 `equipped_card_id` 字段；否则，插入新行。
            String sql = "INSERT INTO `player_meta` (`uuid`, `equipped_card_id`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `equipped_card_id` = VALUES(`equipped_card_id`);";
            try (Connection connection = dataSource.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, cardId);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("设置玩家佩戴名片时出错: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}