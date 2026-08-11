package com.gathers.app.core.domain.tag

/**
 * 标签 — 用于分类、筛选、虚拟文件夹
 */
data class Tag(
    val id: String,
    val name: String,
    val color: Long = 0xFF3482FF,
    val parentId: String? = null,
    val isAuto: Boolean = false,
)

/**
 * 标签系统 — 管理标签层次结构与查询
 */
class TagSystem(
    private val tags: Map<String, Tag> = emptyMap(),
) {
    fun getTag(id: String): Tag? = tags[id]

    fun allTags(): List<Tag> = tags.values.toList()

    fun childrenOf(parentId: String): List<Tag> =
        tags.values.filter { it.parentId == parentId }

    /** 通过文件名启发式匹配自动标签 */
    fun autoTag(fileName: String): List<String> = buildList {
        when {
            fileName.contains("Screenshot") || fileName.contains("screenshot") -> add("screenshot")
            fileName.contains("微信") || fileName.contains("WeChat") -> add("wechat")
            fileName.contains("QQ") || fileName.contains("qq") -> add("qq")
            fileName.contains("wx_camera") -> add("camera")
        }
    }
}