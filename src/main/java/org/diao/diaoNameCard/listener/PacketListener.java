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
        // 从数据包中获取目标玩家的名称
        String targetPlayerName = event.getData().toArray(new String[0])[0];
        // 根据名称获取玩家对象
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        // 获取发送请求的玩家
        Player requester = event.getPlayer();

        if (targetPlayer == null) return;

        // 异步获取目标玩家拥有的所有名片（已修复，现在会包含默认名片）
        plugin.getPlayerDataManager().getPlayerOwnedCards(targetPlayer.getUniqueId()).thenAccept(cards -> {
            // 按名片配置中的层级从小到大排序
            cards.sort(Comparator.comparingInt(NameCard::getLayer));

            // 遍历排序后的名片列表，逐个发送给客户端
            for (NameCard card : cards) {
                // [已修正] 确保数据发送顺序为: 名片id, 贴图路径, 展示名, 描述
                PacketSender.sendCustomData(
                        requester,
                        SEND_CARD_LIST_IDENTIFIER,
                        card.getId(),           // 1. 名片id
                        card.getTexturePath(),  // 2. 贴图路径
                        card.getDisplayName(),  // 3. 展示名
                        card.getDescription()   // 4. 描述
                );
            }
        });
    }
}