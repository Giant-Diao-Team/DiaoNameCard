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

    private final String GET_CARD_BY_ID_IDENTIFIER = "dnc_id_card";
    private final String SEND_CARD_BY_ID_IDENTIFIER = "dnc_id_card_i";

    public PacketListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMessageReceive(CustomPacketEvent event) {
        String identifier = event.getIdentifier();

        if (GET_CARD_IDENTIFIER.equals(identifier)) {
            handleGetEquippedCard(event);
        } else if (GET_CARD_LIST_IDENTIFIER.equals(identifier)) {
            handleGetCardList(event);
        } else if (GET_CARD_BY_ID_IDENTIFIER.equals(identifier)) {
            handleGetCardById(event);
        }
    }

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
            plugin.getPlayerDataManager().playerHasCard(requester.getUniqueId(), requestedCardId).thenAccept(hasCard -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    // [已修改] 在参数最前面加入了名片ID
                    // 格式: 名片ID, 貼圖路徑, 展示名, 描述, 是否擁有
                    PacketSender.sendCustomData(
                            requester,
                            SEND_CARD_BY_ID_IDENTIFIER,
                            card.getId(),             // 1. 名片ID
                            card.getTexturePath(),    // 2. 贴图路径
                            card.getDisplayName(),    // 3. 展示名
                            card.getDescription(),    // 4. 描述
                            String.valueOf(hasCard)   // 5. 是否拥有
                    );
                });
            });
        } else {
            // 如果名片不存在，返回的 "null" 不需要附带ID
            PacketSender.sendCustomData(
                    requester,
                    SEND_CARD_BY_ID_IDENTIFIER,
                    "null"
            );
        }
    }
}