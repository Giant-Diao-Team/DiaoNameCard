package org.diao.diaoNameCard.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.diao.diaoNameCard.Main;
import org.diao.diaoNameCard.model.NameCard;
import yslelf.cloudpick.bukkit.api.PacketSender;
import yslelf.cloudpick.bukkit.api.event.CustomPacketEvent;

import java.util.Comparator;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CloudPick API 数据包监听器
 * 负责接收客户端发来的请求并返回名片数据
 */
public class PacketListener implements Listener {

    private final Main plugin;
    private final String GET_CARD_IDENTIFIER = "dnc_name_card";
    private final String GET_CARD_LIST_IDENTIFIER = "dnc_name_card_list";
    private final String SEND_CARD_IDENTIFIER = "dnc_card";
    private final String SEND_CARD_LIST_IDENTIFIER = "dnc_card_list";

    public PacketListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMessageReceive(CustomPacketEvent event) {
        String identifier = event.getIdentifier();

        // 请求当前佩戴的名片信息
        if (GET_CARD_IDENTIFIER.equals(identifier)) {
            handleGetEquippedCard(event);
        }
        // 请求拥有的所有名片列表
        else if (GET_CARD_LIST_IDENTIFIER.equals(identifier)) {
            handleGetCardList(event);
        }
    }

    /**
     * 处理获取玩家当前佩戴名片的请求
     * @param event CustomPacketEvent
     */
    private void handleGetEquippedCard(CustomPacketEvent event) {
        String targetPlayerName = event.getData().toArray(new String[0])[0];
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        Player requester = event.getPlayer();

        if (targetPlayer == null) return;

        plugin.getPlayerDataManager().getEquippedCard(targetPlayer.getUniqueId()).thenAccept(card -> {
            if (card != null) {
                // 通过API将名片信息发回给请求的客户端
                PacketSender.sendCustomData(
                        requester,
                        SEND_CARD_IDENTIFIER,
                        card.getTexturePath(),
                        card.getDisplayName(),
                        card.getDescription()
                );
            }
        });
    }

    /**
     * 处理获取玩家所有名片列表的请求
     * @param event CustomPacketEvent
     */
    private void handleGetCardList(CustomPacketEvent event) {
        String targetPlayerName = event.getData().toArray(new String[0])[0];
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        Player requester = event.getPlayer();

        if (targetPlayer == null) return;

        // 异步获取玩家拥有的名片
        plugin.getPlayerDataManager().getPlayerOwnedCards(targetPlayer.getUniqueId()).thenAccept(ownedCards -> {
            // 获取全部名片（已排序）
            Collection<NameCard> allCards = plugin.getCardManager().getAllCardsSorted();

            // 创建已拥有名片的ID集合用于快速查找
            Set<String> ownedCardIds = ownedCards.stream()
                    .map(NameCard::getId)
                    .collect(Collectors.toSet());

            // 先发送玩家拥有的名片
            for (NameCard card : ownedCards) {
                PacketSender.sendCustomData(
                        requester,
                        SEND_CARD_LIST_IDENTIFIER,
                        card.getId(),
                        card.getTexturePath(),
                        card.getDisplayName(),
                        card.getDescription(),
                        "true"  // 玩家拥有此名片
                );
            }

            // 再发送玩家没有的名片
            for (NameCard card : allCards) {
                if (!ownedCardIds.contains(card.getId())) {
                    PacketSender.sendCustomData(
                            requester,
                            SEND_CARD_LIST_IDENTIFIER,
                            card.getId(),
                            card.getTexturePath(),
                            card.getDisplayName(),
                            card.getDescription(),
                            "false"  // 玩家没有此名片
                    );
                }
            }
        });
    }
}