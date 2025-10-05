package org.diao.diaoNameCard.manager;

import org.diao.diaoNameCard.Main;
import org.diao.diaoNameCard.model.NameCard;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 玩家数据管理器
 * 负责处理所有与玩家名片数据相关的逻辑（增删改查）
 */
public class PlayerDataManager {

    private final Main plugin;

    public PlayerDataManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * 获取玩家拥有的所有名片对象
     * (已修复: 现在会自动包含玩家的默认名片)
     * @param uuid 玩家UUID
     * @return 名片对象列表的 CompletableFuture
     */
    public CompletableFuture<List<NameCard>> getPlayerOwnedCards(UUID uuid) {
        return plugin.getDatabase().getPlayerCards(uuid).thenApply(cardIds -> {
            // 获取默认名片的ID，以备后用
            String defaultCardId = plugin.getCardManager().getDefaultCardId();

            // 将数据库中存储的名片ID转换为 NameCard 对象列表
            List<NameCard> ownedCards = cardIds.stream()
                    .map(id -> plugin.getCardManager().getCard(id))
                    .filter(Objects::nonNull) // 过滤掉因配置删除而失效的名片
                    .collect(Collectors.toList());

            // 检查这个列表是否已经包含了默认名片
            boolean hasDefaultCard = ownedCards.stream()
                    .anyMatch(card -> card.getId().equalsIgnoreCase(defaultCardId));

            // 如果列表里没有默认名片 (并且服务器配置了默认名片)
            if (!hasDefaultCard && defaultCardId != null && !defaultCardId.isEmpty()) {
                NameCard defaultCard = plugin.getCardManager().getCard(defaultCardId);
                if (defaultCard != null) {
                    // 将默认名片对象手动添加到列表中
                    ownedCards.add(defaultCard);
                }
            }

            // 返回最终的、完整的名片列表
            return ownedCards;
        });
    }

    /**
     * 检查玩家是否拥有某个名片
     * @param uuid 玩家UUID
     * @param cardId 名片ID
     * @return 是否拥有的 CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> playerHasCard(UUID uuid, String cardId) {
        // 如果检查的是默认卡，直接返回 true
        if (cardId.equalsIgnoreCase(plugin.getCardManager().getDefaultCardId())) {
            return CompletableFuture.completedFuture(true);
        }
        return plugin.getDatabase().getPlayerCards(uuid).thenApply(cardIds -> cardIds.contains(cardId.toLowerCase()));
    }

    /**
     * 给予玩家一个名片
     * @param uuid 玩家UUID
     * @param cardId 名片ID
     */
    public void givePlayerCard(UUID uuid, String cardId) {
        plugin.getDatabase().addPlayerCard(uuid, cardId.toLowerCase());
    }

    /**
     * 移除玩家的一个名片
     * @param uuid 玩家UUID
     * @param cardId 名片ID
     */
    public void removePlayerCard(UUID uuid, String cardId) {
        // 如果移除的是当前佩戴的名片，则自动切换到默认名片
        getEquippedCard(uuid).thenAccept(equippedCard -> {
            if (equippedCard != null && equippedCard.getId().equalsIgnoreCase(cardId)) {
                setEquippedCard(uuid, plugin.getCardManager().getDefaultCardId());
            }
            plugin.getDatabase().removePlayerCard(uuid, cardId.toLowerCase());
        });
    }

    /**
     * 设置玩家佩戴的名片
     * @param uuid 玩家UUID
     * @param cardId 名片ID
     */
    public void setEquippedCard(UUID uuid, String cardId) {
        plugin.getDatabase().setEquippedCard(uuid, cardId.toLowerCase());
    }

    /**
     * 获取玩家当前佩戴的名片
     * @param uuid 玩家UUID
     * @return 名片对象的 CompletableFuture
     */
    public CompletableFuture<NameCard> getEquippedCard(UUID uuid) {
        return plugin.getDatabase().getEquippedCard(uuid).thenApply(cardId -> {
            if (cardId == null || cardId.isEmpty()) {
                // 如果没有设置，则返回默认名片
                return plugin.getCardManager().getCard(plugin.getCardManager().getDefaultCardId());
            }
            return plugin.getCardManager().getCard(cardId);
        });
    }
}