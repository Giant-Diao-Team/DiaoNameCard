package org.diao.diaoNameCard.storage;

import org.diao.diaoNameCard.Main;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class SQLite implements Database {

    private final Main plugin;
    private Connection connection;
    private final String dbName = "player_data.db";

    public SQLite(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void connect() {
        File dbFile = new File(plugin.getDataFolder(), dbName);
        if (!dbFile.exists()) {
            try {
                dbFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("无法创建 SQLite 数据库文件: " + e.getMessage());
                return;
            }
        }

        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            plugin.getLogger().info("SQLite 数据库连接成功。");
            initializeTables();
        } catch (SQLException | ClassNotFoundException e) {
            plugin.getLogger().severe("无法连接到 SQLite 数据库: " + e.getMessage());
        }
    }

    private void initializeTables() {
        // 创建玩家数据表，存储装备的名片
        String playerMetaTable = "CREATE TABLE IF NOT EXISTS player_meta (" +
                "uuid VARCHAR(36) PRIMARY KEY NOT NULL," +
                "equipped_card_id VARCHAR(255)" +
                ");";

        // 创建玩家拥有的名片表
        String playerCardsTable = "CREATE TABLE IF NOT EXISTS player_cards (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "uuid VARCHAR(36) NOT NULL," +
                "card_id VARCHAR(255) NOT NULL," +
                "UNIQUE(uuid, card_id)" +
                ");";

        try (Statement statement = connection.createStatement()) {
            statement.execute(playerMetaTable);
            statement.execute(playerCardsTable);
        } catch (SQLException e) {
            plugin.getLogger().severe("无法创建数据库表: " + e.getMessage());
        }
    }

    @Override
    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("SQLite 数据库连接已关闭。");
            } catch (SQLException e) {
                plugin.getLogger().severe("关闭 SQLite 数据库连接时出错: " + e.getMessage());
            }
        }
    }

    @Override
    public CompletableFuture<List<String>> getPlayerCards(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> cards = new ArrayList<>();
            String sql = "SELECT card_id FROM player_cards WHERE uuid = ?;";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    cards.add(rs.getString("card_id"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return cards;
        });
    }

    @Override
    public CompletableFuture<Void> addPlayerCard(UUID uuid, String cardId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT OR IGNORE INTO player_cards (uuid, card_id) VALUES (?, ?);";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, cardId);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Void> removePlayerCard(UUID uuid, String cardId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM player_cards WHERE uuid = ? AND card_id = ?;";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, cardId);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<String> getEquippedCard(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT equipped_card_id FROM player_meta WHERE uuid = ?;";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return rs.getString("equipped_card_id");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null; // 如果没有记录或出错，返回 null
        });
    }

    @Override
    public CompletableFuture<Void> setEquippedCard(UUID uuid, String cardId) {
        return CompletableFuture.runAsync(() -> {
            // 使用 UPSERT 逻辑 (INSERT OR REPLACE)
            String sql = "INSERT OR REPLACE INTO player_meta (uuid, equipped_card_id) VALUES (?, ?);";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, cardId);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}