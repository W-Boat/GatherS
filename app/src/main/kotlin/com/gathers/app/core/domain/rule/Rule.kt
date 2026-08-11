package com.gathers.app.core.domain.rule

/**
 * 清理规则 — 定义自动清理/分类的条件与动作
 */
data class Rule(
    val id: String,
    val name: String,
    val condition: RuleCondition,
    val action: RuleAction,
    val isEnabled: Boolean = true,
)

sealed interface RuleCondition {
    /** 截图时间超过 N 天 */
    data class OlderThan(val days: Int) : RuleCondition
    /** 文件大小超过 N 字节 */
    data class LargerThan(val bytes: Long) : RuleCondition
    /** 来源应用匹配 */
    data class FromApp(val packageName: String) : RuleCondition
    /** 标签包含 */
    data class HasTag(val tagId: String) : RuleCondition
    /** 复合条件（全部满足） */
    data class AllOf(val conditions: List<RuleCondition>) : RuleCondition
    /** 复合条件（任一满足） */
    data class AnyOf(val conditions: List<RuleCondition>) : RuleCondition
}

sealed interface RuleAction {
    /** 移入回收站 */
    data object Trash : RuleAction
    /** 自动打标签 */
    data class ApplyTag(val tagId: String) : RuleAction
    /** 标记为保护 */
    data object Protect : RuleAction
}