package com.wait.entity.type;

/**
 * 资源类型枚举
 * 用于 UV 统计、访问记录等功能
 * 
 * <p>
 * UV（Unique Visitor，独立访客）统计的是访问某个资源的独立访客数量。
 * 例如：帖子1被100个不同用户访问过，那么帖子1的UV就是100。
 * </p>
 * 
 * <p>
 * <b>注意：UV是对被访问的资源而言的，不是对访问者而言的。</b>
 * </p>
 */
public enum ResourceType {
    /**
     * 帖子
     * 用于统计帖子的独立访客数
     * 示例：帖子1被100个不同用户访问过，UV = 100
     */
    POST("post", "帖子"),

    /**
     * 页面（关于页面等静态页面）
     * 用于统计静态页面的独立访客数（关于我们、帮助、隐私政策等）
     * 示例：关于我们页面被50个不同用户访问过，UV = 50
     */
    PAGE("page", "页面"),

    /**
     * 用户首页
     * 用于统计用户主页的独立访客数（用于分析用户受欢迎程度）
     * 示例：用户123的主页被50个不同用户访问过，UV = 50
     */
    USER_PROFILE("user_profile", "用户首页"),

    /**
     * 分类页
     * 用于统计分类页面的独立访客数
     * 示例：技术分类页面被200个不同用户访问过，UV = 200
     */
    CATEGORY("category", "分类页"),

    /**
     * 搜索页
     * 用于统计搜索页面的独立访客数
     * 示例：搜索"Java"的页面被150个不同用户访问过，UV = 150
     */
    SEARCH("search", "搜索页");

    private final String code;
    private final String description;

    ResourceType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据代码获取枚举
     */
    public static ResourceType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ResourceType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}
