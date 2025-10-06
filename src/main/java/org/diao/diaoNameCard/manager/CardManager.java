package org.diao.diaoNameCard.manager;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.diao.diaoNameCard.Main;
import org.diao.diaoNameCard.model.NameCard;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 名片配置管理器
 * 负责从 config.yml 加载和管理所有名片信息
 */
public class CardManager {

    private final Main plugin;
    private final Map<String, NameCard> nameCards = new HashMap<>();
    private String defaultCardId;

    public CardManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * 从配置文件加载所有名片信息
     */
    public void loadCards() {
        nameCards.clear(); // 清空旧数据
        plugin.reloadConfig(); // 重新加载配置文件

        ConfigurationSection cardsSection = plugin.getConfig().getConfigurationSection("namecards.cards");
        if (cardsSection == null) {
            plugin.getLogger().warning("配置文件中未找到 'namecards.cards' 部分，无法加载名片。");
            return;
        }

        for (String cardId : cardsSection.getKeys(false)) {
            int layer = cardsSection.getInt(cardId + ".layer");
            String texture = cardsSection.getString(cardId + ".texture", "");
            // 支持颜色代码
            String displayName = ChatColor.translateAlternateColorCodes('&', cardsSection.getString(cardId + ".display-name", "未命名名片"));
            String description = ChatColor.translateAlternateColorCodes('&', cardsSection.getString(cardId + ".description", ""));

            NameCard card = new NameCard(cardId, layer, texture, displayName, description);
            nameCards.put(cardId.toLowerCase(), card); // 使用小写ID作为键，避免大小写问题
        }

        this.defaultCardId = plugin.getConfig().getString("namecards.default-card-id", "");

        // 验证 default-card-id 的有效性，防止因配置错误导致功能失效
        if (this.defaultCardId == null || this.defaultCardId.isEmpty()) {
            // 如果服主根本没设置默认ID，给一个温和的警告
            plugin.getLogger().warning("配置文件中未设置 'default-card-id'，默认名片功能可能无法正常工作。");
        } else if (getCard(this.defaultCardId) == null) {
            // 如果设置的ID是无效的，给出严重的错误提示
            plugin.getLogger().severe("************************************************************");
            plugin.getLogger().severe("* [配置错误] 'default-card-id' 的值 '" + this.defaultCardId + "' 不是一个有效的名片 ID。");
            plugin.getLogger().severe("* 请检查 'namecards.cards' 列表中是否存在此 ID。");
            plugin.getLogger().severe("* 默认名片功能已被禁用，直到你填写正确的ID。");
            plugin.getLogger().severe("************************************************************");
            // 将 defaultCardId 设为 null，安全地禁用该功能，防止后续出错
            this.defaultCardId = null;
        }

        plugin.getLogger().info("成功加载了 " + nameCards.size() + " 个名片。");
    }

    /**
     * 根据ID获取名片
     * @param id 名片ID
     * @return NameCard 对象，如果不存在则返回 null
     */
    public NameCard getCard(String id) {
        if (id == null) return null;
        return nameCards.get(id.toLowerCase());
    }

    /**
     * 获取所有名片
     * @return 所有名片的集合
     */
    public Collection<NameCard> getAllCards() {
        return nameCards.values();
    }

    /**
     * 获取排序后的所有名片（按层级从小到大）
     * @return 排序后的名片集合
     */
    public Collection<NameCard> getAllCardsSorted() {
        return nameCards.values().stream()
                .sorted(Comparator.comparingInt(NameCard::getLayer))
                .collect(Collectors.toList());
    }

    /**
     * 获取默认名片的ID
     * @return 默认名片ID, 如果配置无效则返回 null
     */
    public String getDefaultCardId() {
        return defaultCardId;
    }
}