package org.diao.diaoNameCard.model;

/**
 * 名片数据模型类
 * 用于封装从配置文件加载的单个名片信息
 */
public class NameCard {
    private final String id; // 名片ID
    private final int layer; // 层级
    private final String texturePath; // 贴图路径
    private final String displayName; // 展示名
    private final String description; // 描述

/**
 * NameCard类的构造函数，用于创建名片对象
 * @param id 名片的唯一标识符
 * @param layer 名片的层级
 * @param texturePath 名片贴图的路径
 * @param displayName 名片的显示名称
 * @param description 名片的描述信息
 */
    public NameCard(String id, int layer, String texturePath, String displayName, String description) {
    // 初始化名片的ID属性
        this.id = id;
    // 初始化名片的层级属性
        this.layer = layer;
    // 初始化名片贴图的路径属性
        this.texturePath = texturePath;
    // 初始化名片的显示名称属性
        this.displayName = displayName;
    // 初始化名片的描述信息属性
        this.description = description;
    }

/**
 * 获取ID的方法
 * @return 返回对象的ID属性值
 */
    // Getter 方法
    public String getId() {
        return id; // 返回id属性的值
    }

/**
 * 获取当前对象的层级信息
 * @return 返回当前对象的层级值
 */
    public int getLayer() {
        return layer;    // 返回当前对象的layer属性值
    }

/**
 * 获取纹理路径的方法
 * @return 返回纹理文件的路径字符串
 */
    public String getTexturePath() {
        return texturePath; // 返回当前对象的纹理路径属性值
    }

/**
 * 获取显示名称的方法
 * @return 返回对象的显示名称字符串
 */
    public String getDisplayName() {
    // 返回displayName字段的值
        return displayName;
    }

/**
 * 获取描述信息的方法
 *
 * @return 返回当前对象的description属性值
 */
    public String getDescription() {
        return description;  // 返回description字段的值
    }
}