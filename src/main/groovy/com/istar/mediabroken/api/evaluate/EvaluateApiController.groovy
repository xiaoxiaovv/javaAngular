package com.istar.mediabroken.api.evaluate

import com.istar.mediabroken.api.CurrentUser
import com.istar.mediabroken.entity.LoginUser
import com.istar.mediabroken.entity.account.Account
import com.istar.mediabroken.entity.capture.Site
import com.istar.mediabroken.entity.evaluate.ChannelSummary
import com.istar.mediabroken.entity.evaluate.EvaluateReport
import com.istar.mediabroken.entity.evaluate.ReportStatusEnum
import com.istar.mediabroken.entity.evaluate.TendencyTop
import com.istar.mediabroken.service.evaluate.EvaluateChannelService
import com.istar.mediabroken.service.evaluate.EvaluateNewsService
import com.istar.mediabroken.service.evaluate.EvaluateService
import com.istar.mediabroken.utils.NumUtils
import com.mongodb.BasicDBObject
import groovy.util.logging.Slf4j
import org.apache.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

import static com.istar.mediabroken.api.ApiResult.apiResult

/**
 * @author hanhui
 * @date 2018/6/20 10:07
 * @desc 测评相关
 * */
@RestController
@Slf4j
@RequestMapping(value = "/api/evaluate/")
class EvaluateApiController {

    @Autowired
    EvaluateService evaluateService
    @Autowired
    EvaluateChannelService evaluateChannelService
    @Autowired
    EvaluateNewsService evaluateNewsService

    /**
     * 获取测评概览基本信息
     * @param currentUser
     * @return
     */
    @RequestMapping(value = "/info", method = RequestMethod.GET)
    public Map getEvaluateInfo(
            @CurrentUser LoginUser currentUser
    ) {
        EvaluateReport evaluateReport = evaluateService.getValidEvaluateReportById(currentUser.userId)
        Map<String, Object> resultMap
        if (evaluateReport && evaluateReport.status == ReportStatusEnum.failed.getKey()) {
            ChannelSummary result = evaluateService.getEvaluateInfo(evaluateReport.id)
            result.articleCount = null
            result.multiple = null
            result.bsi = null
            result.tsi = null
            result.mii = null
            result.psi = null
            result.multipleRate = null
            result.bsiRate = null
            result.tsiRate = null
            result.miiRate = null
            result.psiRate = null
            return apiResult([status: HttpStatus.SC_OK, info: result.toMap()])
        } else if (evaluateReport) {
            ChannelSummary result = evaluateService.getEvaluateInfo(evaluateReport.id)
            if (result) {
                resultMap = getResultMap(result)
                return apiResult([status: HttpStatus.SC_OK, info: resultMap, isDemo: false])
            }
            return apiResult([status: HttpStatus.SC_OK, info: null])
        } else {
            EvaluateReport demoReport = evaluateService.getValidEvaluateReportById(0)
            ChannelSummary summaryResult = evaluateService.getEvaluateInfo(demoReport.id)
            resultMap = getResultMap(summaryResult)
            return apiResult([status: HttpStatus.SC_OK, info: resultMap, isDemo: true])
        }
    }

    private Map<String, Object> getResultMap(ChannelSummary result) {
        Map summaryMap = [:]
        summaryMap.put("articleCount", result.articleCount == 0 ? result.articleCount.toString() : result.articleCount)
        summaryMap.put("multiple", result.multiple == 0 ? result.multiple.toString() : result.multiple)
        summaryMap.put("bsi", result.bsi == 0 ? result.bsi.toString() : result.bsi)
        summaryMap.put("tsi", result.tsi == 0 ? result.tsi.toString() : result.tsi)
        summaryMap.put("mii", result.mii == 0 ? result.mii.toString() : result.mii)
        summaryMap.put("psi", result.psi == 0 ? result.psi.toString() : result.psi)
        Map resultMap = result.toMap()
        resultMap.putAll(summaryMap)
        resultMap
    }

    /**
     * 测评概览-趋势统计top10
     * @param currentUser
     * @return
     */
    @RequestMapping(value = "/tendency", method = RequestMethod.GET)
    public Map findTendencyTop(
            @CurrentUser LoginUser currentUser,
            @RequestParam(value = "evaluateId", required = false, defaultValue = "") String evaluateId
    ) {
        if (evaluateId) {
            TendencyTop tendencyTop = evaluateService.findTendencyTop(evaluateId)
            return apiResult([status: HttpStatus.SC_OK, info: tendencyTop.toMap()])
        } else {
            EvaluateReport evaluateReport = evaluateService.getValidEvaluateReportById(currentUser.userId)
            if (evaluateReport) {
                if (evaluateReport.status == ReportStatusEnum.failed.getKey()) {
                    return apiResult([status: HttpStatus.SC_OK, msg: "测评创建失败，请检查渠道设置，如有问题请联系客服", info: null])
                } else if (evaluateReport.status == ReportStatusEnum.busy.getKey()) {
                    return apiResult([status: HttpStatus.SC_OK, msg: "测评数据正在计算中...", info: null])
                } else {
                    TendencyTop tendencyTop = evaluateService.findTendencyTop(evaluateReport.id)
                    if (tendencyTop) {
                        return apiResult([status: HttpStatus.SC_OK, info: tendencyTop.toMap()])
                    } else {
                        return apiResult([status: HttpStatus.SC_OK, msg: "测评数据正在计算中...", info: null])
                    }
                }
            } else {
                EvaluateReport demoReport = evaluateService.getValidEvaluateReportById(0)
                TendencyTop tendencyTop = evaluateService.findTendencyTop(demoReport.id)
                return apiResult([status: HttpStatus.SC_OK, info: tendencyTop.toMap()])
            }
        }
    }

    /**
     * @author hanhui
     * @date 2018/6/25 14:32
     * @desc 获取网站指数排行
     * */
    @RequestMapping(value = "/websiteRank", method = RequestMethod.GET)
    public Map getWebsiteRank(
            @CurrentUser LoginUser currentUser,
            @RequestParam(value = "rankIndex", required = true) String rankIndex,
            @RequestParam(value = "pageNo", required = true) int pageNo,
            @RequestParam(value = "pageSize", required = true) int pageSize
    ) {
        EvaluateReport evaluateReport = evaluateService.getValidEvaluateReportById(currentUser.userId)
        if (evaluateReport) {
            if (evaluateReport.status == ReportStatusEnum.failed.getKey()) {
                return apiResult([status: HttpStatus.SC_OK, msg: "测评创建失败，请检查渠道设置，如有问题请联系客服", list: []])
            } else if (evaluateReport.status == ReportStatusEnum.busy.getKey()) {
                return apiResult([status: HttpStatus.SC_OK, msg: "测评数据正在计算中...", list: []])
            } else {
                Map resultMap = evaluateService.getWebsiteRank(rankIndex, evaluateReport, pageNo, pageSize)
                def resultList = resultMap.get("list")
                if (resultList) {
                    return apiResult([status    : HttpStatus.SC_OK, list: resultList, totalPageNo: resultMap.get("totalPageNo"),
                                      totalCount: resultMap.get("totalCount")])
                } else if (evaluateReport.status in [2, 3]) {
                    for (BasicDBObject basicDBObject : evaluateReport.channels) {
                        if (basicDBObject.get("siteType") == Site.SiteTypeEnum.weSite.key) {
                            return apiResult([status: HttpStatus.SC_OK, msg: "该渠道暂无数据，请检查", list: []])
                        }
                    }
                    return apiResult([status: HttpStatus.SC_OK, msg: "未创建该类型渠道，请设置", list: []])
                } else {
                    return apiResult([status: HttpStatus.SC_OK, msg: "测评数据正在计算中...", list: []])
                }
            }
        } else {
            EvaluateReport demoReport = evaluateService.getValidEvaluateReportById(0)
            Map resultMap = evaluateService.getWebsiteRank(rankIndex, demoReport, pageNo, pageSize)
            def resultList = resultMap.get("list")
            return apiResult([status    : HttpStatus.SC_OK, list: resultList, totalPageNo: resultMap.get("totalPageNo"),
                              totalCount: resultMap.get("totalCount")])
        }
    }

    /**
     * @author hanhui
     * @date 2018/6/25 14:32
     * @desc 获取微信公众号指数排行
     * */
    @RequestMapping(value = "/wechatRank", method = RequestMethod.GET)
    public Map getWeChatRank(
            @CurrentUser LoginUser currentUser,
            @RequestParam(value = "rankIndex", required = true) String rankIndex,
            @RequestParam(value = "pageNo", required = true) int pageNo,
            @RequestParam(value = "pageSize", required = true) int pageSize
    ) {
        if ((rankIndex in ["sumRead", "sumLike", "avgRead", "avgLike"]) && ((Account.USERTYPE_TRIAL).equals(currentUser.userType))) {
            return apiResult([status: HttpStatus.SC_OK, msg: "升级版本以查看此内容，详情请联系客服或销售人员", list: []])
        }
        EvaluateReport evaluateReport = evaluateService.getValidEvaluateReportById(currentUser.userId)
        if (evaluateReport) {
            if (evaluateReport.status == ReportStatusEnum.failed.getKey()) {
                return apiResult([status: HttpStatus.SC_OK, msg: "测评创建失败，请检查渠道设置，如有问题请联系客服", list: []])
            } else if (evaluateReport.status == ReportStatusEnum.busy.getKey()) {
                return apiResult([status: HttpStatus.SC_OK, msg: "测评数据正在计算中...", list: []])
            } else {
                Map resultMap = evaluateService.getWechatRank(rankIndex, evaluateReport, pageNo, pageSize)
                def resultList = resultMap.get("list")
                if (resultList) {
                    return apiResult([status    : HttpStatus.SC_OK, list: resultList, totalPageNo: resultMap.get("totalPageNo"),
                                      totalCount: resultMap.get("totalCount")])
                } else if (evaluateReport.status in [2, 3]) {
                    for (BasicDBObject basicDBObject : evaluateReport.channels) {
                        if (basicDBObject.get("siteType") == Site.SiteTypeEnum.weChat.key) {
                            return apiResult([status: HttpStatus.SC_OK, msg: "该渠道暂无数据，请检查", list: []])
                        }
                    }
                    return apiResult([status: HttpStatus.SC_OK, msg: "未创建该类型渠道，请设置", list: []])
                } else {
                    return apiResult([status: HttpStatus.SC_OK, msg: "测评数据正在计算中...", list: []])
                }
            }
        } else {
            EvaluateReport demoReport = evaluateService.getValidEvaluateReportById(0)
            Map resultMap = evaluateService.getWechatRank(rankIndex, demoReport, pageNo, pageSize)
            def resultList = resultMap.get("list")
            return apiResult([status    : HttpStatus.SC_OK, list: resultList, totalPageNo: resultMap.get("totalPageNo"),
                              totalCount: resultMap.get("totalCount")])
        }
    }

    /**
     * @author hanhui
     * @date 2018/6/25 14:32
     * @desc 获取微博指数排行
     * */
    @RequestMapping(value = "/weiboRank", method = RequestMethod.GET)
    public Map getWeiboRank(
            @CurrentUser LoginUser currentUser,
            @RequestParam(value = "rankIndex", required = true) String rankIndex,
            @RequestParam(value = "pageNo", required = true) int pageNo,
            @RequestParam(value = "pageSize", required = true) int pageSize
    ) {
        if ((rankIndex in ["sumLike", "avgLike"]) && ((Account.USERTYPE_TRIAL).equals(currentUser.userType))) {
            return apiResult([status: HttpStatus.SC_OK, msg: "升级版本以查看此内容，详情请联系客服或销售人员", list: []])
        }
        EvaluateReport evaluateReport = evaluateService.getValidEvaluateReportById(currentUser.userId)
        if (evaluateReport) {
            if (evaluateReport.status == ReportStatusEnum.failed.getKey()) {
                return apiResult([status: HttpStatus.SC_OK, msg: "测评创建失败，请检查渠道设置，如有问题请联系客服", list: []])
            } else if (evaluateReport.status == ReportStatusEnum.busy.getKey()) {
                return apiResult([status: HttpStatus.SC_OK, msg: "测评数据正在计算中...", list: []])
            } else {
                Map resultMap = evaluateService.getWeiboRank(rankIndex, evaluateReport, pageNo, pageSize)
                def resultList = resultMap.get("list")
                if (resultList) {
                    return apiResult([status    : HttpStatus.SC_OK, list: resultList, totalPageNo: resultMap.get("totalPageNo"),
                                      totalCount: resultMap.get("totalCount")])
                } else if (evaluateReport.status in [2, 3]) {
                    for (BasicDBObject basicDBObject : evaluateReport.channels) {
                        if (basicDBObject.get("siteType") == Site.SiteTypeEnum.weiBo.key) {
                            return apiResult([status: HttpStatus.SC_OK, msg: "该渠道暂无数据，请检查", list: []])
                        }
                    }
                    return apiResult([status: HttpStatus.SC_OK, msg: "未创建该类型渠道，请设置", list: []])
                } else {
                    return apiResult([status: HttpStatus.SC_OK, msg: "测评数据正在计算中...", list: []])
                }
            }
        } else {
            EvaluateReport demoReport = evaluateService.getValidEvaluateReportById(0)
            Map resultMap = evaluateService.getWeiboRank(rankIndex, demoReport, pageNo, pageSize)
            def resultList = resultMap.get("list")
            return apiResult([status    : HttpStatus.SC_OK, list: resultList, totalPageNo: resultMap.get("totalPageNo"),
                              totalCount: resultMap.get("totalCount")])
        }
    }

    @RequestMapping(value = "channel/detail/{channelId}", method = RequestMethod.GET)
    public Map getEvaluateChannelDetail(
            @CurrentUser LoginUser currentUser,
            @PathVariable("channelId") String channelId
    ) {
        def evaluateReport = evaluateService.getEvaluateReportByUserId(currentUser.userId)
        if (!evaluateReport) {
            evaluateReport = evaluateService.getEvaluateReportByUserId(0)
            currentUser = new LoginUser()
            currentUser.setUserId(0)
        }
        def channel = evaluateChannelService.findById(currentUser.userId, channelId)
        def detail = evaluateService.getEvaluateChannelDetail(currentUser.userId, evaluateReport.id, channel.id, evaluateReport.startTime, evaluateReport.endTime)
        def count = 0L
        if (detail) {
            count = detail.contentCount
        }
        if (!detail || count == 0L) {
            String siteDomain = ""
            if (channel.siteType == Site.SITE_TYPE_WEBSITE) {
                siteDomain = channel.siteDomain
            }
            if (channel.siteType == Site.SITE_TYPE_WECHAT) {
                siteDomain = channel.siteName
            }
            if (channel.siteType == Site.SITE_TYPE_WEIBO) {
                siteDomain = channel.siteName
            }
            count = evaluateService.getDistributeCount(channel.siteType, [siteDomain], evaluateReport.startTime, evaluateReport.endTime)
        }
        Map power
        power = evaluateService.getEvaluateChannelDetailFourPower(currentUser.userId, evaluateReport.id, channel.id, evaluateReport.startTime, evaluateReport.endTime)

        //获取增长趋势
        Map rate = evaluateService.getFourPowerRate(currentUser.userId, evaluateReport.id, channel.id, evaluateReport.startTime, evaluateReport.endTime)
        if (!power) {
            def four = evaluateService.getWeeklyFourPower(channelId, currentUser.userId, channel.siteType, channel.siteName, channel.siteDomain, evaluateReport.startTime, evaluateReport.endTime)
            power = [psi     : NumUtils.doubleForMat(four.psi),
                     mii     : NumUtils.doubleForMat(four.mii),
                     tsi     : NumUtils.doubleForMat(four.tsi),
                     bsi     : NumUtils.doubleForMat(four.bsi),
                     multiple: NumUtils.doubleForMat(four.multiple)
            ]

        }

        Map map = [
                evaluateChannelId: channelId,
                evaluateName     : evaluateReport.evaluateName,//评测名称
                siteName         : channel.siteName,//渠道名称
                startTime        : evaluateReport.startTime,
                endTime          : evaluateReport.endTime,
                contentCount     : count,
                weight           : [psiWeight     : channel.psiWeight ?: [:],
                                    miiWeight     : channel.miiWeight ?: [:],
                                    tsiWeight     : channel.tsiWeight ?: [:],
                                    bsiWeight     : channel.bsiWeight ?: [:],
                                    multipleWeight: channel.multipleWeight ?: [:]
                ],
                psi              : power.psi,
                mii              : power.mii,
                tsi              : power.tsi,
                bsi              : power.bsi,
                multiple         : power.multiple,
                psiRate          : ((rate.psiRate) == 0.0 ? "0%" : rate.psiRate) ? NumUtils.doubleForMat(rate.psiRate * 100) + "%" : rate.psiRate,
                miiRate          : ((rate.miiRate) == 0.0 ? "0%" : rate.miiRate) ? NumUtils.doubleForMat(rate.miiRate * 100) + "%" : rate.miiRate,
                tsiRate          : ((rate.tsiRate) == 0.0 ? "0%" : rate.tsiRate) ? NumUtils.doubleForMat(rate.tsiRate * 100) + "%" : rate.tsiRate,
                bsiRate          : ((rate.bsiRate) == 0.0 ? "0%" : rate.bsiRate) ? NumUtils.doubleForMat(rate.bsiRate * 100) + "%" : rate.bsiRate,
                multipleRate     : ((rate.multipleRate) == 0.0 ? "0%" : rate.multipleRate) ? NumUtils.doubleForMat(rate.multipleRate * 100) + "%" : rate.bsiRate

        ]
        return apiResult([status: HttpStatus.SC_OK, info: map])

    }

    /**
     * 设置综合指数参数
     * @param currentUser
     * @param channelId
     * @param mediaAttrName
     * @param mediaAttrVal
     * @param psiWeight
     * @param miiWeight
     * @param tsiWeight
     * @param bsiWeight
     * @return
     */
    @RequestMapping(value = "channel/detail/{channelId}", method = RequestMethod.POST)
    public Map setChannelMultipleWeight(
            @CurrentUser LoginUser currentUser,
            @PathVariable("channelId") String channelId,
            @RequestParam(value = "mediaAttrName", required = false, defaultValue = "无") String mediaAttrName,
            @RequestParam(value = "psiWeight", required = false, defaultValue = "10") int psiWeight,
            @RequestParam(value = "miiWeight", required = false, defaultValue = "10") int miiWeight,
            @RequestParam(value = "tsiWeight", required = false, defaultValue = "75") int tsiWeight,
            @RequestParam(value = "bsiWeight", required = false, defaultValue = "5") int bsiWeight
    ) {
        if (!(NumUtils.isIntegerRange(psiWeight) && NumUtils.isIntegerRange(miiWeight) && NumUtils.isIntegerRange(tsiWeight) && NumUtils.isIntegerRange(bsiWeight))) {
            return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, '输入内容不符合规则')
        }
        if ((psiWeight + miiWeight + tsiWeight + bsiWeight) != 100) {
            return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, '权重值总和必须等于100')
        }
        Map attr = evaluateService.getMultipleMediaAttr()
        def mediaAttrVal = attr.get(mediaAttrName)
        if (!mediaAttrVal) {
            mediaAttrVal = 1
        }
        def weight = evaluateChannelService.setChannelMultipleWeight(currentUser.userId, channelId, mediaAttrName, mediaAttrVal, psiWeight, miiWeight, tsiWeight, bsiWeight)
        if (weight) {
            return apiResult(HttpStatus.SC_OK, '保存成功')
        }
        return apiResult(HttpStatus.SC_OK, '保存失败')
    }

    /**
     * 设置四力的参数
     * @param currentUser
     * @param channelId
     * @param fourPower
     * @param classificationName
     * @param classificationVal
     * @param standard
     * @return
     */
    @RequestMapping(value = "channel/detail/fourPower/{channelId}", method = RequestMethod.POST)
    public Map setChannelFourPowerWeight(
            @CurrentUser LoginUser currentUser,
            @PathVariable("channelId") String channelId,
            @RequestParam(value = "fourPower", required = true) String fourPower,
            @RequestParam(value = "classificationName", required = false, defaultValue = "无") String classificationName,
            @RequestParam(value = "standard", required = false, defaultValue = "1") int standard
    ) {

        if (!NumUtils.isIntegerRange1000(standard)) {
            return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, '输入内容不符合规则')
        }
        Map classification = evaluateService.getFourPowerClassification()
        def classificationVal = classification.get(classificationName)
        if (!classificationVal) {
            classificationVal = 1.00
        }
        def weight = evaluateChannelService.setChannelFourPowerWeight(currentUser.userId, channelId, fourPower, classificationName, classificationVal, standard)
        if (weight) {
            return apiResult(HttpStatus.SC_OK, '保存成功')
        }
        return apiResult(HttpStatus.SC_OK, '保存失败')
    }

    /**
     * 初始化四力和综合参数
     * @param currentUser
     * @param channelId
     * @param fourPower
     * @return
     */
    @RequestMapping(value = "channel/detail/fourPower/{channelId}", method = RequestMethod.PUT)
    public Map initChannelWeight(
            @CurrentUser LoginUser currentUser,
            @PathVariable("channelId") String channelId,
            @RequestParam(value = "fourPower", required = true) String fourPower
    ) {

        def weight = evaluateChannelService.intiChannelWeight(currentUser.userId, channelId, fourPower)
        if (weight) {
            return apiResult(HttpStatus.SC_OK, '保存成功')
        }
        return apiResult(HttpStatus.SC_OK, '保存失败')
    }

    /**
     * 综合指数的媒体级别
     * @param currentUser
     * @return
     */
    @RequestMapping(value = "channel/detail/multiple/mediaAttr", method = RequestMethod.GET)
    public Map getMultipleMediaAttr(
            @CurrentUser LoginUser currentUser
    ) {

        def list = []
        Map attr = evaluateService.getMultipleMediaAttr()
        attr.each { key, val ->
            def map = [name: key, value: val]
            list.add(map)
        }
        return apiResult([status: HttpStatus.SC_OK, list: list])
    }

    /**
     * 获取用户综合指数的四力权重
     * @param currentUser
     * @return
     */
    @RequestMapping(value = "channel/detail/multiple/parameters/{channelId}", method = RequestMethod.GET)
    public Map getMultipleParameters(
            @PathVariable("channelId") String channelId,
            @CurrentUser LoginUser currentUser
    ) {
        def list = []
        def info = []
        def channel = evaluateChannelService.findById(currentUser.userId, channelId)
        if (channel.multipleWeight) {
            def weight = channel.multipleWeight.parameters
            weight.each { key, val ->
                def map = [name: key, value: val]
                list.add(map)
            }

            def attr = channel.multipleWeight.mediaAttr
            attr.each { key, val ->
                def map = [name: key, value: val]
                if (!"无".equals(key)) {
                    info.add(map)
                }

            }
        } else {
            Map weight = evaluateService.getMultipleFourPowerWight()
            weight.each { key, val ->
                def map = [name: key, value: val]
                list.add(map)
            }
        }


        return apiResult([status: HttpStatus.SC_OK, parameters: list, mediaAttr: info])
    }

    /**
     * 获取用户四力指数的标准
     * @param currentUser
     * @return
     */
    @RequestMapping(value = "channel/detail/fourPower/standard/{channelId}", method = RequestMethod.GET)
    public Map getFourPowerStandard(
            @PathVariable("channelId") String channelId,
            @RequestParam(value = "fourPower", required = true) String fourPower,
            @CurrentUser LoginUser currentUser
    ) {
        def list = []
        def info
        def standard = evaluateService.getFourPowerStandardValue()
        standard.each { key, val ->
            info = val
        }
        def channel = evaluateChannelService.findById(currentUser.userId, channelId)
        if ("psi".equals(fourPower) && channel.psiWeight) {
            def classification = channel.psiWeight.classification
            classification.each { key, val ->
                def map = [name: key, value: val]
                if (!"无".equals(key)) {
                    list.add(map)
                }
            }
            info = channel.psiWeight.standard

        }
        if ("mii".equals(fourPower) && channel.miiWeight) {
            def classification = channel.miiWeight.classification
            classification.each { key, val ->
                def map = [name: key, value: val]
                if (!"无".equals(key)) {
                    list.add(map)
                }
            }
            info = channel.miiWeight.standard

        }
        if ("tsi".equals(fourPower) && channel.tsiWeight) {
            def classification = channel.tsiWeight.classification
            classification.each { key, val ->
                def map = [name: key, value: val]
                if (!"无".equals(key)) {
                    list.add(map)
                }
            }
            info = channel.tsiWeight.standard

        }
        if ("bsi".equals(fourPower) && channel.bsiWeight) {
            def classification = channel.bsiWeight.classification
            classification.each { key, val ->
                def map = [name: key, value: val]
                if (!"无".equals(key)) {
                    list.add(map)
                }
            }
            info = channel.bsiWeight.standard

        }

        return apiResult([status: HttpStatus.SC_OK, classification: list, standard: info])
    }
    /**
     * 设置四力指数的分类
     * @param currentUser
     * @return
     */
    @RequestMapping(value = "channel/detail/fourPower/classification", method = RequestMethod.GET)
    public Map getFourPowerClassification(
            @CurrentUser LoginUser currentUser
    ) {
        def list = []
        def classification = evaluateService.getFourPowerClassification()
        classification.each { key, val ->
            def map = [name: key, value: val]
            list.add(map)
        }
        return apiResult([status: HttpStatus.SC_OK, list: list])
    }

    /**
     *  趋势统计
     * @param currentUser
     * @param channelId
     * @param key
     * @return
     */
    @RequestMapping(value = "channel/detail/{channelId}/trend", method = RequestMethod.GET)
    public Map getFourPowerTrend(
            @CurrentUser LoginUser currentUser,
            @PathVariable("channelId") String channelId,
            @RequestParam(value = "key", required = false, defaultValue = "multiple") String key
    ) {
        def evaluateReport = evaluateService.getEvaluateReportByUserId(currentUser.userId)
        def channel = evaluateChannelService.findById(currentUser.userId, channelId)
        if (!channel) {
            channel = evaluateChannelService.findById(0, channelId)
        }
        if (channel.siteType == Site.SITE_TYPE_WECHAT && currentUser.userType.equals(Account.USERTYPE_TRIAL) && (key in ["sumRead", "sumLike", "avgRead", "avgLike"])) {
            return apiResult([status: HttpStatus.SC_OK, msg: '升级版本以查看此内容，详情请联系客服或销售人员', list: []])
        }
        if (channel.siteType == Site.SITE_TYPE_WEIBO && currentUser.userType.equals(Account.USERTYPE_TRIAL) && (key in ["sumLike", "avgLike"])) {
            return apiResult([status: HttpStatus.SC_OK, msg: '升级版本以查看此内容，详情请联系客服或销售人员', list: []])
        }
        def map
        if (evaluateReport) {
            map = evaluateService.getMultipleTrendByTime(key, currentUser.userId, evaluateReport.id, channelId, evaluateReport.startTime, evaluateReport.endTime)
        } else {
            def demoReport = evaluateService.getEvaluateReportByUserId(0)
            map = evaluateService.getMultipleTrendByTime(key, 0, demoReport.id, channelId, demoReport.startTime, demoReport.endTime)
        }
        return apiResult([status: HttpStatus.SC_OK, list: map])
    }

    /**
     * 一周词云取30个
     * @param currentUser
     * @param channelId
     * @return
     */
    @RequestMapping(value = "channel/detail/{channelId}/wordCloud", method = RequestMethod.GET)
    public Map getEvaluateChannelWordCloud(
            @CurrentUser LoginUser currentUser,
            @PathVariable("channelId") String channelId
    ) {
        def evaluateReport = evaluateService.getEvaluateReportByUserId(currentUser.userId)
        def keywords
        if (!evaluateReport) {
            evaluateReport = evaluateService.getEvaluateReportByUserId(0)
            keywords = evaluateService.getWeeklyKeywords(0, channelId, evaluateReport.startTime, evaluateReport.endTime)
        } else {
            keywords = evaluateService.getWeeklyKeywords(currentUser.userId, channelId, evaluateReport.startTime, evaluateReport.endTime)
        }
        Map map = [list: keywords ?: []]
        return apiResult(map)
    }
    /**
     * 获取top 10
     * @param currentUser
     * @param channelId
     * @param key sumRead阅读    sumReprint转载  commentCount评论   sumLike点赞
     * @return
     */
    @RequestMapping(value = "channel/detail/{channelId}/topNews", method = RequestMethod.GET)
    public Map getEvaluateChannelTopNews(
            @CurrentUser LoginUser currentUser,
            @PathVariable("channelId") String channelId,
            @RequestParam(value = "key", required = false, defaultValue = "reprintCount") String key
    ) {
        def evaluateReport = evaluateService.getEvaluateReportByUserId(currentUser.userId)
        def channel
        channel = evaluateChannelService.findById(currentUser.userId, channelId)
        if (!channel) {
            channel = evaluateChannelService.findById(0, channelId)
        }
        if (channel.siteType == Site.SITE_TYPE_WECHAT && currentUser.userType.equals(Account.USERTYPE_TRIAL) && (key in ["sumRead", "sumLike", "avgRead", "avgLike"])) {
            return apiResult([status: HttpStatus.SC_OK, msg: '升级版本以查看此内容，详情请联系客服或销售人员', list: []])
        }
        if (channel.siteType == Site.SITE_TYPE_WEIBO && currentUser.userType.equals(Account.USERTYPE_TRIAL) && (key in ["sumLike", "avgLike"])) {
            return apiResult([status: HttpStatus.SC_OK, msg: '升级版本以查看此内容，详情请联系客服或销售人员', list: []])
        }
        def list
        if (evaluateReport) {
            list = evaluateNewsService.getEvaluateNewsList(currentUser.userId, channelId, evaluateReport.startTime, evaluateReport.endTime, key)
        } else {
            def demoReport = evaluateService.getEvaluateReportByUserId(0)
            list = evaluateNewsService.getEvaluateNewsList(0, channelId, demoReport.startTime, demoReport.endTime, key)
        }
        Map map = [list: list]
        return apiResult(map)
    }
}
