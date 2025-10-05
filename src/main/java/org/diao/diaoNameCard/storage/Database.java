package org.diao.diaoNameCard.storage;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 数据库操作接口
 * 定义了所有数据存储方式都需要实现的方法
 */
public interface Database {

    /**
     * 连接数据库并初始化表结构
     */
    void connect();

    /**
     * 关闭数据库连接
     */
    void disconnect();

    /**
     * 获取玩家拥有的所有名片ID
     * @param uuid 玩家的UUID
     * @return 一个包含名片ID列表的 CompletableFuture
     */
    CompletableFuture<List<String>> getPlayerCards(UUID uuid);

    /**
     * 为玩家添加一个名片
     * @param uuid 玩家的UUID
     * @param cardId 名片ID
     * @return 一个表示操作完成的 CompletableFuture
     */
    CompletableFuture<Void> addPlayerCard(UUID uuid, String cardId);

    /**
     * 移除玩家的一个名片
     * @param uuid 玩家的UUID
     * @param cardId 名片ID
     * @return 一个表示操作完成的 CompletableFuture
     */
    CompletableFuture<Void> removePlayerCard(UUID uuid, String cardId);

    /**
     * 获取玩家当前装备的名片ID
     * @param uuid 玩家的UUID
     * @return 一个包含名片ID的 CompletableFuture
     */
    CompletableFuture<String> getEquippedCard(UUID uuid);

    /**
     * 设置玩家当前装备的名片
     * @param uuid 玩家的UUID
     * @param cardId 名片ID
     * @return 一个表示操作完成的 CompletableFuture
     */
    CompletableFuture<Void> setEquippedCard(UUID uuid, String cardId);
}