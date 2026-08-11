package com.gathers.app.data

import java.util.Calendar
import java.util.Locale

/**
 * 本地启发式分类打标器：
 * 基于文件名、目录路径（应用映射）与像素分析结果，
 * 生成 应用标签 / 内容标签 / 状态标签 / 视觉标签 与短摘要。
 */
object Tagger {

    /** 目录名 → 应用名映射 */
    private val appMap = listOf(
        "weixin" to "微信", "micromsg" to "微信", "wechat" to "微信",
        "qq" to "QQ", "tencent" to "QQ",
        "taobao" to "淘宝", "tmall" to "天猫", "alipay" to "支付宝", "ali" to "阿里系",
        "jd" to "京东", "pinduoduo" to "拼多多", "meituan" to "美团", "eleme" to "饿了么",
        "douyin" to "抖音", "kuaishou" to "快手", "bilibili" to "哔哩哔哩", "bilin" to "哔哩哔哩",
        "weibo" to "微博", "xiaohongshu" to "小红书", "zhihu" to "知乎", "tieba" to "贴吧",
        "wangzhe" to "王者荣耀", "game" to "游戏", "mi" to "小米", "xiaomi" to "小米",
        "huawei" to "华为", "oppo" to "OPPO", "vivo" to "vivo",
        "dingtalk" to "钉钉", "feishu" to "飞书", "wps" to "WPS",
        "dianping" to "大众点评", "gaode" to "高德", "baidu" to "百度",
        "screenshot" to "系统截图", "截屏" to "系统截图", "screen" to "系统截图",
        "camera" to "相机", "dcim" to "相机",
    )

    /** 关键词 → 内容标签 */
    private val contentRules = listOf(
        "订单" to "财务", "支付" to "财务", "账单" to "财务", "余额" to "财务",
        "金额" to "财务", "收款" to "财务", "转账" to "财务", "交易" to "财务",
        "付款码" to "凭证", "收款码" to "凭证", "二维码" to "凭证", "qr" to "凭证",
        "取餐码" to "凭证", "车票" to "凭证", "机票" to "凭证", "发票" to "凭证",
        "验证码" to "凭证", "证件" to "凭证", "身份证" to "凭证", "车票" to "凭证",
        "攻略" to "攻略", "教程" to "攻略", "地图" to "攻略", "路线" to "攻略",
        "聊天" to "聊天", "消息" to "聊天", "对话" to "聊天", "record" to "聊天",
        "战绩" to "游戏", "排位" to "游戏", "段位" to "游戏", "胜率" to "游戏",
        "网页" to "网页", "文章" to "网页", "新闻" to "网页",
        "错误" to "错误", "报错" to "错误", "崩溃" to "错误", "异常" to "错误",
        "空白" to "空白", "纯色" to "空白",
    )

    private val keywordMap = mapOf(
        "bank" to "银行", "招商" to "招商银行", "工商" to "工商银行", "建设" to "建设银行",
        "农行" to "农业银行", "支付" to "支付",
    )

    /** 目录路径 → 来源应用 */
    fun sourceAppFromPath(relativePath: String, displayName: String): String {
        val lower = relativePath.lowercase(Locale.ROOT)
        appMap.firstOrNull { (key, _) -> lower.contains(key) }?.let { return it.second }
        // 文件名中也可能携带应用名（如 WeiXin_xxx）
        val nameLower = displayName.lowercase(Locale.ROOT)
        appMap.firstOrNull { (key, _) -> nameLower.contains(key) }?.let { return it.second }
        return "其他"
    }

    /** 依据文件名/路径/EXIF 生成完整标签与摘要 */
    fun tag(
        displayName: String,
        relativePath: String,
        takenAt: Long,
        meta: ImageMeta? = null,
        action: String? = null,
    ): TagResult {
        val text = (displayName + " " + relativePath + " " + (meta?.imageDescription ?: "")).lowercase(Locale.ROOT)
        val sourceApp = sourceAppFromPath(relativePath, displayName)

        // 内容标签
        val contents = LinkedHashSet<String>()
        contentRules.forEach { (kw, tag) ->
            if (text.contains(kw)) contents.add(tag)
        }
        if (meta?.isMostlyBlank == true) contents.add("空白")
        if (action != null) contents.add("行为·$action")

        // 状态标签
        val states = LinkedHashSet<String>()
        val now = System.currentTimeMillis()
        val ageDays = (now - takenAt) / 86_400_000L
        when {
            ageDays <= 1 -> states.add("新截图")
            ageDays <= 7 -> states.add("本周")
            ageDays <= 30 -> states.add("本月")
            else -> states.add("过期候选")
        }
        if (contents.any { it in Screenshot.LOW_VALUE_TAGS }) states.add("建议清理")

        // 视觉标签
        val visuals = LinkedHashSet<String>()
        if (text.contains("二维码") || text.contains("qr")) visuals.add("二维码")
        if (meta?.isMostlyBlank == true) visuals.add("空白页")
        if (visuals.isEmpty()) visuals.add("图片")

        // 摘要（10-20 字）
        val summary = buildSummary(sourceApp, contents, takenAt, displayName)

        return TagResult(sourceApp, contents.toList(), states.toList(), visuals.toList(), summary)
    }

    private fun buildSummary(
        sourceApp: String,
        contents: Set<String>,
        takenAt: Long,
        displayName: String,
    ): String {
        val content = contents.firstOrNull { it != "行为·编辑" && it != "行为·裁剪" }
        val cal = Calendar.getInstance().apply { timeInMillis = takenAt }
        val monthDay = "%d月%d日".format(cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
        val namePart = displayName.substringBeforeLast('.').take(14)
        return when {
            content != null -> "${sourceApp}${content}截图 · $monthDay"
            else -> "${sourceApp}截图 · $namePart".take(20)
        }
    }

    /** 从文件名/摘要中提取金额关键词（用于报表的"账单类"汇总，非精确 OCR） */
    fun extractAmountHint(text: String): String? {
        val m = Regex("""(?:金额|余额|合计|共|¥|￥)\s*(\d+(?:[.,]\d{1,2})?)""").find(text)
        return m?.groupValues?.get(1)
    }
}

data class TagResult(
    val sourceApp: String,
    val contentTags: List<String>,
    val stateTags: List<String>,
    val visualTags: List<String>,
    val summary: String,
)
