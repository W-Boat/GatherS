package com.gathers.app.data

import java.util.concurrent.TimeUnit

/** 智能规则触发结果 */
enum class RuleAction {
    /** 打标签（归类） */
    TAG,

    /** 锁定保护（高价值） */
    PROTECT,

    /** 建议清理（低价值） */
    SUGGEST_CLEAN,
}

/** 一条可配置的智能规则 */
data class SmartRule(
    val id: String,
    val name: String,
    val sourceApp: String? = null,      // null = 任意应用
    val keyword: String? = null,        // 文件名/路径关键词（null = 任意）
    val minAgeDays: Int = 0,            // 截图存在天数下限
    val action: RuleAction,
    val enabled: Boolean = true,
    val description: String = "",
)

/** 内置智能规则集 */
object BuiltinRules {
    val all: List<SmartRule> = listOf(
        SmartRule(
            id = "protect_finance",
            name = "高价值财务凭证保护",
            sourceApp = null,
            keyword = "订单|支付|账单|余额|收款|转账|付款码|收款码|发票|证件",
            action = RuleAction.PROTECT,
            enabled = true,
            description = "包含订单/支付/凭证关键词的截图自动锁定，清理时跳过",
        ),
        SmartRule(
            id = "tag_temp_voucher",
            name = "临时凭证归类",
            sourceApp = null,
            keyword = "取餐码|验证码|车票|取件码",
            action = RuleAction.TAG,
            enabled = true,
            description = "取餐码/验证码等临时凭证归类为【临时凭证】，24 小时后提示清理",
        ),
        SmartRule(
            id = "clean_error",
            name = "系统错误截图清理",
            sourceApp = null,
            keyword = "错误|报错|崩溃|异常|空白",
            action = RuleAction.SUGGEST_CLEAN,
            enabled = true,
            description = "报错/空白截图判定为低价值，推荐清理",
        ),
        SmartRule(
            id = "clean_old",
            name = "超期截图软清理",
            sourceApp = null,
            keyword = null,
            minAgeDays = 30,
            action = RuleAction.SUGGEST_CLEAN,
            enabled = false,
            description = "超过 30 天且未收藏的截图建议清理",
        ),
        SmartRule(
            id = "tag_wechat",
            name = "微信聊天截图归类",
            sourceApp = "微信",
            keyword = null,
            action = RuleAction.TAG,
            enabled = true,
            description = "来源为微信的截图归类为【聊天记录】",
        ),
    )
}

/**
 * 规则引擎：将截图与智能规则匹配，产出建议（保护/清理/归类）。
 */
object RuleEngine {

    /** 应用于单张截图，返回命中的规则动作集合 */
    fun apply(screenshot: Screenshot, rules: List<SmartRule> = BuiltinRules.all): List<RuleAction> {
        if (screenshot.isProtected) return listOf(RuleAction.PROTECT)
        val haystack = "${screenshot.displayName} ${screenshot.relativePath} ${screenshot.summary} ${screenshot.contentTags.joinToString()}".lowercase()
        val ageDays = (System.currentTimeMillis() - screenshot.takenAt) / TimeUnit.DAYS.toMillis(1)

        val hits = mutableListOf<RuleAction>()
        rules.filter { it.enabled }.forEach { rule ->
            val appOk = rule.sourceApp == null || screenshot.sourceApp == rule.sourceApp
            val kwOk = rule.keyword == null || rule.keyword.split("|").any { haystack.contains(it.lowercase()) }
            val ageOk = screenshot.takenAt > 0 && ageDays >= rule.minAgeDays
            if (appOk && kwOk && ageOk) {
                if (!hits.contains(rule.action)) hits.add(rule.action)
            }
        }
        // 用户手动收藏/锁定永远视为保护
        if (screenshot.isFavorite) hits.add(RuleAction.PROTECT)
        if (screenshot.isLowValue) hits.add(RuleAction.SUGGEST_CLEAN)
        return hits
    }

    /** 生成清理建议列表（低价值优先） */
    fun suggestCleanup(
        screenshots: List<Screenshot>,
        rules: List<SmartRule> = BuiltinRules.all,
    ): List<Screenshot> =
        screenshots
            .filter { !it.inTrash && !it.isProtected && !it.isFavorite }
            .filter { apply(it, rules).contains(RuleAction.SUGGEST_CLEAN) }
            .sortedBy { it.takenAt }
}
