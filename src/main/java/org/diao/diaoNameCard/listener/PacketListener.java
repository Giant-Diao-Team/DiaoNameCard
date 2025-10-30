package org.diao.diaoNameCard.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.diao.diaoNameCard.Main;
import org.diao.diaoNameCard.model.NameCard;
import yslelf.cloudpick.bukkit.api.PacketSender;
import yslelf.cloudpick.bukkit.api.event.CustomPacketEvent;

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

    // 查询单张名片详情的标识符
    private final String GET_CARD_BY_ID_IDENTIFIER = "dnc_id_card";
    private final String SEND_CARD_BY_ID_IDENTIFIER = "dnc_id_card_i";

    public PacketListener(Main plugin) {
        this.plugin = plugin;
    }

/**
 * 处理自定义数据包事件的方法
 * 当接收到自定义数据包时，根据标识符执行相应的处理逻辑
 *
 * @param event 自定义数据包事件对象，包含数据包相关信息
 */
    @EventHandler
    public void onMessageReceive(CustomPacketEvent event) {
    // 从事件中获取标识符，用于判断具体的请求类型
        String identifier = event.getIdentifier();

    // 判断标识符是否为"获取当前名片"的请求
        if (GET_CARD_IDENTIFIER.equals(identifier)) {
            // 请求当前佩戴的名片信息，调用对应处理方法
            handleGetEquippedCard(event);
        } else if (GET_CARD_LIST_IDENTIFIER.equals(identifier)) {
            // 请求所有名片列表及玩家拥有状态 (这是您修改的版本)
            handleGetCardList(event);
        } else if (GET_CARD_BY_ID_IDENTIFIER.equals(identifier)) {
            // 新增：请求指定ID的名片详细信息
            handleGetCardById(event);
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
     * 处理获取玩家所有名片列表的请求 (您的实现版本)
     * @param event CustomPacketEvent
     */
    private void handleGetCardList(CustomPacketEvent event) {
        String targetPlayerName = event.getData().toArray(new String[0])[0];
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        Player requester = event.getPlayer();

        if (targetPlayer == null) return;

        plugin.getPlayerDataManager().getPlayerOwnedCards(targetPlayer.getUniqueId()).thenAccept(ownedCards -> {
            Collection<NameCard> allCards = plugin.getCardManager().getAllCardsSorted();
            Set<String> ownedCardIds = ownedCards.stream()
                    .map(NameCard::getId)
                    .collect(Collectors.toSet());

            for (NameCard card : ownedCards) {
                PacketSender.sendCustomData(
                        requester,
                        SEND_CARD_LIST_IDENTIFIER,
                        card.getId(),
                        card.getTexturePath(),
                        card.getDisplayName(),
                        card.getDescription(),
                        "true"
                );
            }

            for (NameCard card : allCards) {
                if (!ownedCardIds.contains(card.getId())) {
                    PacketSender.sendCustomData(
                            requester,
                            SEND_CARD_LIST_IDENTIFIER,
                            card.getId(),
                            card.getTexturePath(),
                            card.getDisplayName(),
                            card.getDescription(),
                            "false"
                    );
                }
            }
        });
    }

    /**
     * 处理根据ID获取单张名片详细信息的请求
     * @param event CustomPacketEvent
     */
    private void handleGetCardById(CustomPacketEvent event) {
        if (event.getData().isEmpty()) {
            return;
        }

        String requestedCardId = event.getData().toArray(new String[0])[0];
        Player requester = event.getPlayer();
        NameCard card = plugin.getCardManager().getCard(requestedCardId);

        if (card != null) {
            // 如果找到了名片，则发送其详细信息
            PacketSender.sendCustomData(
                    requester,
                    SEND_CARD_BY_ID_IDENTIFIER,
                    card.getTexturePath(),
                    card.getDisplayName(),
                    card.getDescription()
            );
        } else {
            // 如果根据ID没有找到名片，则发送 "null" 字符串作为响应
            PacketSender.sendCustomData(
                    requester,
                    SEND_CARD_BY_ID_IDENTIFIER,
                    "null"
            );
        }
    }
}