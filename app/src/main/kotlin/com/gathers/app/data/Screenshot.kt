package com.gathers.app.data

import android.net.Uri

/**
 * 一条截图记录。
 *
 * @param id MediaStore 的 _ID（相册库中的唯一标识）
 * @param uri 指向图片文件的 content:// URI
 * @param displayName 文件名（如 Screenshot_2026-08-11-10-30-45.png）
 * @param relativePath 存储相对路径（如 Pictures/Screenshots）
 * @param sizeBytes 文件大小（字节）
 * @param width 图片宽度（px）
 * @param height 图片高度（px）
 * @param takenAt 截图时间（epoch millis，优先 DATE_TAKEN，回退 DATE_MODIFIED）
 * @param mimeType MIME 类型（image/png 等）
 * @param sourceApp 推断的来源应用（微信 / 系统截图 / 淘宝…）
 * @param contentTags 内容标签（财务 / 凭证 / 攻略 / 聊天 / 游戏 / 网页 / 错误…）
 * @param stateTags 状态标签（新截图 / 已读 / 过期候选…）
 * @param visualTags 视觉标签（纯文字 / 图文混排 / 二维码 / 未知）
 * @param summary 本地启发式生成的短摘要
 * @param blurScore 模糊度 0-100（越高越模糊），-1 表示未计算
 * @param isFavorite 用户收藏
 * @param isProtected 高价值锁定（清理时跳过）
 * @param inTrash 是否已进入回收站（副本 + 原图删除）
 * @param trashedAt 进入回收站时间
 * @param userNote 用户备注
 */
data class Screenshot(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val relativePath: String,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val takenAt: Long,
    val mimeType: String,
    val sourceApp: String = "",
    val contentTags: List<String> = emptyList(),
    val stateTags: List<String> = emptyList(),
    val visualTags: List<String> = emptyList(),
    val summary: String = "",
    val blurScore: Int = -1,
    val isFavorite: Boolean = false,
    val isProtected: Boolean = false,
    val inTrash: Boolean = false,
    val trashedAt: Long = 0L,
    val userNote: String = "",
) {
    val isLowValue: Boolean
        get() = contentTags.any { it in LOW_VALUE_TAGS } || blurScore >= 70

    companion object {
        /** 低价值内容标签：系统错误、空白页等建议清理 */
        val LOW_VALUE_TAGS = listOf("错误", "空白", "重复")
    }
}
