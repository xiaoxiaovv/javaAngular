package com.istar.mediabroken.service.evaluate

import com.istar.mediabroken.entity.capture.Site
import com.istar.mediabroken.entity.evaluate.*
import com.istar.mediabroken.repo.account.AccountRepo
import com.istar.mediabroken.repo.admin.SettingRepo
import com.istar.mediabroken.repo.evaluate.*
import com.istar.mediabroken.utils.DateUitl
import com.istar.mediabroken.utils.NumUtils
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import java.text.DecimalFormat

/**
 * @author hanhui
 * @date 2018/6/20 10:07
 * @desc 测评相关
 * */
@Service
@Slf4j
class EvaluateService {
    @Autowired
    EvaluateRepo evaluateRepo
    @Autowired
    EvaluateReportRepo evaluateReportRepo
    @Autowired
    SiteInfoWeeklyRepo siteInfoWeeklyRepo
    @Autowired
    EvaluateChannelDetailRepo evaluateChannelDetailRepo
    @Autowired
    ChannelSummaryRepo channelSummaryRepo
    @Autowired
    SettingRepo settingRepo
    @Autowired
    AccountRepo accountRepo
    @Autowired
    EvaluateChannelRepo evaluateChannelRepo
    @Autowired
    EvaluateTeamRepo evaluateTeamRepo
    @Autowired
    SiteInfoDailyRepo siteInfoDailyRepo
    @Autowired
    EvaluateChannelDailyRepo evaluateChannelDailyRepo
    @Autowired
    TendencyTopRepo tendencyTopRepo
    @Autowired
    EvaluateContentService evaluateContentService
    @Autowired
    KeywordsWeeklyRepo keywordsWeeklyRepo
    /**
     * 获取测评概览数据
     * @param userId
     * @return
     */
    ChannelSummary getEvaluateInfo(String evaluateId) {
        ChannelSummary channelSummary = channelSummaryRepo.getEvaluateInfoByEvaId(evaluateId)
        return channelSummary
    }

    /**
     * 新增测评概览数据
     * @param userId
     * @desc 每次新建测评报告时调用
     */
    void addEvaluateInfo(EvaluateReport evaluateReport) {
        ChannelSummary channelSummary = new ChannelSummary()
        List channels = evaluateReport.channels
        channelSummary.evaluateId = evaluateReport.id
        channelSummary.channelsName = evaluateReport.channelsName
        channelSummary.evaluateName = evaluateReport.evaluateName
        channelSummary.channelCount = channels.size()
        channelSummary.startTime = evaluateReport.startTime
        channelSummary.endTime = evaluateReport.endTime
        channelSummaryRepo.addEvaluateInfo(channelSummary)
    }

    /**
     * 修改测评概览数据
     * @param evaluateId
     * @desc 每次权重修改及基础数据计算完成后调用
     */
    void updateEvaluateInfo(String evaluateId) {
        EvaluateReport evaluateReport = evaluateReportRepo.getEvaluateReportById(evaluateId)
        if (evaluateReport) {
            ChannelSummary channelSummary = channelSummaryRepo.getEvaluateInfoByEvaId(evaluateReport.id)
            def newsCount = getDistributeCount(evaluateReport.channels, evaluateReport.startTime, evaluateReport.endTime)
            getEvaluateIndex(channelSummary, evaluateReport)
            channelSummary.articleCount = newsCount
            channelSummaryRepo.updateEvaluateInfo(channelSummary)
        }
    }

    /**
     * 删除测评概览数据
     * @param evaluateId 测评报告Id
     * @desc 删除测评报告时调用
     */
    Integer removeEvaluateInfo(String evaluateId) {
        return channelSummaryRepo.removeEvaluateInfo(evaluateId)
    }

    //计算测评概览四力、综合指数及周增长率
    ChannelSummary getEvaluateIndex(ChannelSummary channelSummary, EvaluateReport evaluateReport) {
        DecimalFormat df = new DecimalFormat("######0.00");
        EvaluateChannelDetail evaluateChannelDetail = evaluateChannelDetailRepo.getEvaluateIndexById(evaluateReport.id)
        //channelSummary.channelCount
        long channelCount = evaluateChannelDetail.channelCount == 0 ? 1 : evaluateChannelDetail.channelCount
        channelSummary.multiple = Double.parseDouble(df.format(evaluateChannelDetail.multiple / channelCount))
        channelSummary.bsi = Double.parseDouble(df.format(evaluateChannelDetail.bsi / channelCount))
        channelSummary.tsi = Double.parseDouble(df.format(evaluateChannelDetail.tsi / channelCount))
        channelSummary.mii = Double.parseDouble(df.format(evaluateChannelDetail.mii / channelCount))
        channelSummary.psi = Double.parseDouble(df.format(evaluateChannelDetail.psi / channelCount))

        EvaluateReport beforeLastWeek = evaluateReportRepo.getReportByUserIdAndTime(evaluateReport.userId,
                DateUitl.getLastWeekMondayBegin(evaluateReport.startTime), DateUitl.getLastWeekSundayEnd(evaluateReport.endTime))
        if (beforeLastWeek) {
            ChannelSummary beforeLastWeekSummary = channelSummaryRepo.getEvaluateInfoByEvaId(beforeLastWeek.id)
            Double beforeLastWeekMultiple = beforeLastWeekSummary.multiple
            Double beforeLastWeekbsi = beforeLastWeekSummary.bsi
            Double beforeLastWeektsi = beforeLastWeekSummary.tsi
            Double beforeLastWeekmii = beforeLastWeekSummary.mii
            Double beforeLastWeekpsi = beforeLastWeekSummary.psi
            channelSummary.multipleRate = df.format((channelSummary.multiple - beforeLastWeekMultiple) / (beforeLastWeekMultiple == 0 ? 1 : beforeLastWeekMultiple) * 100) + "%"
            channelSummary.bsiRate = df.format((channelSummary.bsi - beforeLastWeekbsi) / (beforeLastWeekbsi == 0 ? 1 : beforeLastWeekbsi) * 100) + "%"
            channelSummary.tsiRate = df.format((channelSummary.tsi - beforeLastWeektsi) / (beforeLastWeektsi == 0 ? 1 : beforeLastWeektsi) * 100) + "%"
            channelSummary.miiRate = df.format((channelSummary.mii - beforeLastWeekmii) / (beforeLastWeekmii == 0 ? 1 : beforeLastWeekmii) * 100) + "%"
            channelSummary.psiRate = df.format((channelSummary.psi - beforeLastWeekpsi) / (beforeLastWeekpsi == 0 ? 1 : beforeLastWeekpsi) * 100) + "%"
        }
        return channelSummary
    }

    //获取用户所设置站点的发稿总数
    Long getDistributeCount(List channels, Date startTime, Date endTime) {
        HashMap<Integer, List<String>> websiteMap = groupWebSitesByType(channels)
        Long newsCount = 0L
        websiteMap.each {
            Long distributeCount = siteInfoWeeklyRepo.getDistributeCount(it.key, it.value, startTime, endTime)
            newsCount = newsCount + distributeCount
        }
        return newsCount
    }

    public HashMap<Integer, List<String>> groupWebSitesByType(List channels) {
        Map<Integer, List<String>> websiteMap = new HashMap<>()
        def websites = channels.groupBy {
            it.siteType
        }
        websites.each {
            def siteDomain = []
            it.value.each { siteInfo ->
                if (siteInfo.siteDomain) {
                    siteDomain.add(siteInfo.siteDomain)
                } else {
                    siteDomain.add(siteInfo.siteName)
                }

            }
            websiteMap.put(it.key, siteDomain)
        }
        websiteMap
    }

    /**
     *
     * @param rankIndex publishCount:发文量   multiple:综合指数  psi:传播力  mii:影响力   bsi:引导力  tsi:公信力
     * @param userId
     * @return
     * @desc 根据排序类型 获取网站相应的指数排序
     */
    Map getWebsiteRank(String rankIndex, EvaluateReport evaluateReport, Integer pageNo, Integer pageSize) {
        def channelRankList = []
        def result = [:]
        Map totalMap
        if (("publishCount").equals(rankIndex)) {
            totalMap = getInfoRankAndPageByTypeAndRankIndex(Site.SITE_TYPE_WEBSITE, rankIndex, evaluateReport, channelRankList, pageNo, pageSize)
        } else {
            totalMap = getIndexRankAndPageByTypeAndRankIndex(Site.SITE_TYPE_WEBSITE, rankIndex, evaluateReport, channelRankList, pageNo, pageSize)
        }
        result.putAll(totalMap)
        result.put("list", channelRankList)
        return result
    }

    /**
     * @author hanhui
     * @date 2018/6/25 15:12
     * @param rankIndex publishCount:发文量  sumRead:累计阅读  sumLike:累计点赞  avgRead:平均阅读  avgLike:平均点赞
     * @desc 根据排序类型 获取微信公众号相应的指数排序
     * */
    Map getWechatRank(String rankIndex, EvaluateReport evaluateReport, Integer pageNo, Integer pageSize) {
        def channelRankList = []
        def result = [:]
        Map totalMap
        if (rankIndex in ["publishCount", "sumRead", "sumLike", "avgRead", "avgLike"]) {
            totalMap = getInfoRankAndPageByTypeAndRankIndex(Site.SITE_TYPE_WECHAT, rankIndex, evaluateReport, channelRankList, pageNo, pageSize)
        } else {
            totalMap = getIndexRankAndPageByTypeAndRankIndex(Site.SITE_TYPE_WECHAT, rankIndex, evaluateReport, channelRankList, pageNo, pageSize)
        }
        result.putAll(totalMap)
        result.put("list", channelRankList)
        return result
    }

    /**
     * @author hanhui
     * @date 2018/6/25 15:12
     * @param rankIndex publishCount:发文量  sumReprint:累计转载  sumLike:累计点赞  avgReprint:平均转载  avgLike:平均点赞
     * @desc 根据排序类型 获取微博相应的指数排序
     * */
    Map getWeiboRank(String rankIndex, EvaluateReport evaluateReport, Integer pageNo, Integer pageSize) {
        def channelRankList = []
        def result = [:]
        Map totalMap
        if (rankIndex in ["publishCount", "sumReprint", "sumLike", "avgReprint", "avgLike"]) {
            totalMap = getInfoRankAndPageByTypeAndRankIndex(Site.SITE_TYPE_WEIBO, rankIndex, evaluateReport,
                    channelRankList, pageNo, pageSize)
        } else {
            totalMap = getIndexRankAndPageByTypeAndRankIndex(Site.SITE_TYPE_WEIBO, rankIndex, evaluateReport,
                    channelRankList, pageNo, pageSize)
        }
        result.putAll(totalMap)
        result.put("list", channelRankList)
        return result
    }

    private Map getInfoRankAndPageByTypeAndRankIndex(Integer siteType, String rankIndex, EvaluateReport evaluateReport,
                                                     List channelRankList, Integer pageNo, Integer pageSize) {
        HashMap<Integer, List<String>> websiteMap = groupWebSitesByType(evaluateReport.channels)
        List<EvaluateChannel> evaluateChannelList = evaluateChannelRepo.findByUserId(evaluateReport.userId, siteType)
        Map totalMap = ["totalCount": 0, "totalPageNo": 0]
        if (evaluateChannelList) {
            def channelInfo = [:]
            for (EvaluateChannel evaluateChannel : evaluateChannelList) {
                if (siteType == Site.SITE_TYPE_WEBSITE) {
                    channelInfo.put(evaluateChannel.siteDomain, evaluateChannel)
                } else {
                    channelInfo.put(evaluateChannel.siteName, evaluateChannel)
                }
            }
            List<SiteInfoWeekly> siteInfoWeeklyList = siteInfoWeeklyRepo.getIndexRankBySite(siteType,
                    evaluateReport.startTime, evaluateReport.endTime, websiteMap.get(siteType) ?: [], rankIndex, pageNo, pageSize)
            totalMap = getTotalPageNo(siteType, evaluateReport.startTime, evaluateReport.endTime, websiteMap.get(siteType), pageSize)
            for (SiteInfoWeekly siteInfo : siteInfoWeeklyList) {
                ChannelRank channelRank = new ChannelRank()
                EvaluateChannel info = channelInfo.get(siteInfo.siteDomain)
                channelRank.id = info ? info.id : ""
                channelRank.channelName = info ? info.siteName : siteInfo.siteDomain
                channelRank.siteType = info ? info.siteType : siteInfo.siteType
                switch (rankIndex) {
                    case "publishCount":
                        channelRank.index = siteInfo.publishCount
                        break
                    case "sumRead":
                        channelRank.index = siteInfo.sumRead
                        break
                    case "sumLike":
                        channelRank.index = siteInfo.sumLike
                        break
                    case "sumReprint":
                        channelRank.index = siteInfo.sumReprint
                        break
                    case "avgRead":
                        channelRank.index = NumUtils.doubleForMat(siteInfo.avgRead)
                        break
                    case "avgLike":
                        channelRank.index = NumUtils.doubleForMat(siteInfo.avgLike)
                        break
                    case "avgReprint":
                        channelRank.index = NumUtils.doubleForMat(siteInfo.avgReprint)
                        break
                }
                EvaluateTeam evaluateTeam = null
                if (info) {
                    evaluateTeam = evaluateTeamRepo.getEvaluateTeamById(evaluateReport.userId, info.evaluateTeamId)
                }
                channelRank.teamName = evaluateTeam ? evaluateTeam.teamName : "未分组"
                channelRank.channelStatus = siteInfo.status
                channelRank.lastPublishTime = siteInfo.recentPublishTime
                channelRankList.add(channelRank)
            }
        }
        totalMap
    }

    private Map getIndexRankAndPageByTypeAndRankIndex(Integer siteType, String rankIndex, EvaluateReport evaluateReport,
                                                      List channelRankList, Integer pageNo, Integer pageSize) {
        List<EvaluateChannelDetail> evaluateChannelDetailList = evaluateChannelDetailRepo
                .getIndexRankByEveId(siteType, evaluateReport.id, rankIndex, pageNo, pageSize)
        Map totalMap = ["totalCount": 0, "totalPageNo": 0]
        if (evaluateChannelDetailList) {
            totalMap = getTotalPageNo(siteType, evaluateReport.id, pageSize)
            for (EvaluateChannelDetail channelDetail : evaluateChannelDetailList) {
                ChannelRank channelRank = new ChannelRank()
                channelRank.id = channelDetail.channelId
                channelRank.channelName = channelDetail.siteName
                channelRank.siteType = channelDetail.siteType
                if ("multiple".equals(rankIndex)) {
                    channelRank.index = NumUtils.doubleForMat(channelDetail.multiple)
                } else if ("psi".equals(rankIndex)) {
                    channelRank.index = NumUtils.doubleForMat(channelDetail.psi)
                } else if ("mii".equals(rankIndex)) {
                    channelRank.index = NumUtils.doubleForMat(channelDetail.mii)
                } else if ("bsi".equals(rankIndex)) {
                    channelRank.index = NumUtils.doubleForMat(channelDetail.bsi)
                } else if ("tsi".equals(rankIndex)) {
                    channelRank.index = NumUtils.doubleForMat(channelDetail.tsi)
                }
                EvaluateChannel evaluateChannel = evaluateChannelRepo.findById(evaluateReport.userId, channelDetail.channelId)
                EvaluateTeam evaluateTeam = evaluateTeamRepo.getEvaluateTeamById(evaluateReport.userId, evaluateChannel.evaluateTeamId)
                channelRank.teamName = evaluateTeam ? evaluateTeam.teamName : "未分组"
                SiteInfoWeekly siteInfo
                if (channelDetail.siteType == Site.SITE_TYPE_WEBSITE) {
                    siteInfo = siteInfoWeeklyRepo.getSiteInfoBySite(channelDetail.siteDomain,
                            channelDetail.startTime, channelDetail.endTime)
                } else {
                    siteInfo = siteInfoWeeklyRepo.getSiteInfoBySite(channelDetail.siteName,
                            channelDetail.startTime, channelDetail.endTime)
                }
                channelRank.channelStatus = siteInfo.status
                channelRank.lastPublishTime = siteInfo.recentPublishTime
                channelRankList.add(channelRank)
            }
        }
        totalMap
    }

    private Map getTotalPageNo(int siteType, Date startTime, Date endTime, List<String> websites, int pageSize) {
        long totalCount = siteInfoWeeklyRepo.getTotalCount(siteType, startTime, endTime, websites ?: [])
        int totalPageNo = totalCount % pageSize > 0 ? totalCount / pageSize + 1 : totalCount / pageSize
        ["totalCount": totalCount, "totalPageNo": totalPageNo]
    }

    private Map getTotalPageNo(Integer siteType, String evaluateId, int pageSize) {
        long totalCount = evaluateChannelDetailRepo.getTotalCount(siteType, evaluateId)
        int totalPageNo = totalCount % pageSize > 0 ? totalCount / pageSize + 1 : totalCount / pageSize
        ["totalCount": totalCount, "totalPageNo": totalPageNo]
    }

    /**
     * 趋势TOP10数据插入
     * @param userId
     * @desc 每周统计一次
     */
    void addIndexTop(String evaluateId) {
        EvaluateReport evaluateReport = evaluateReportRepo.getEvaluateReportById(evaluateId)
        TendencyTop tendency = tendencyTopRepo.findTendencyTop(evaluateId)
        TendencyTop tendencyTop = getTendencyTop(evaluateReport)
        if (tendency) {
            tendencyTopRepo.updateTendencyTop(tendencyTop)
        } else {
            tendencyTopRepo.addTendencyTop(tendencyTop)
        }
    }

    /**
     * 趋势TOP10数据更新
     * @param evaluateId
     * @desc 权重修改时需要调用
     */
    void updateIndexTop(String evaluateId) {
        EvaluateReport evaluateReport = evaluateReportRepo.getEvaluateReportById(evaluateId)
        TendencyTop tendencyTop = getTendencyTop(evaluateReport)
        tendencyTopRepo.updateTendencyTop(tendencyTop)
    }
    /**
     * 趋势TOP10数据查询
     */
    TendencyTop findTendencyTop(String evaluateId) {
        TendencyTop tendencyTop = tendencyTopRepo.findTendencyTop(evaluateId)
        return tendencyTop
    }


    private TendencyTop getTendencyTop(EvaluateReport evaluateReport) {
        TendencyTop tendencyTop = new TendencyTop()
        String evaId = evaluateReport.id
        Date startTime = evaluateReport.startTime
        LinkedHashMap publishCountMap = getIndexTop(evaId, "publishCount", startTime)
        LinkedHashMap multipleMap = getIndexTop(evaId, "multiple", startTime)
        LinkedHashMap psiMap = getIndexTop(evaId, "psi", startTime)
        LinkedHashMap miiMap = getIndexTop(evaId, "mii", startTime)
        LinkedHashMap tsiMap = getIndexTop(evaId, "tsi", startTime)
        LinkedHashMap bsiMap = getIndexTop(evaId, "bsi", startTime)
        tendencyTop.evaluateId = evaId
        tendencyTop.publishCount = publishCountMap
        tendencyTop.multiple = multipleMap
        tendencyTop.psi = psiMap
        tendencyTop.mii = miiMap
        tendencyTop.tsi = tsiMap
        tendencyTop.bsi = bsiMap
        return tendencyTop
    }

    /**
     * 获取渠道按照指定指标排名top10的详细信息
     * @param evaluateId
     * @param rankIndex
     * @return
     */
    Map getIndexTop(String evaluateId, String rankIndex, Date startTime) {
        List<EvaluateChannelDetail> detailList = evaluateChannelDailyRepo.getIndexTop(evaluateId, rankIndex)
        log.info(new Date().toString() + "获取渠道top10排名" + detailList.size())
        LinkedHashMap resultMap = [:]
        for (EvaluateChannelDetail detail : detailList) {
            List<EvaluateChannelDetail> details = evaluateChannelDailyRepo.getDetailByDomain(detail.siteType, evaluateId,
                    detail.siteDomain, detail.siteName)
            def channelDetail = []
            Map tempMap = getOneWeekDefaultData(startTime, detail.siteName)
            details.each {
                Map tendencyData = [:]
                tendencyData.channelName = it.siteName
                tendencyData.count = NumUtils.doublePoints(it[rankIndex] as Double)
                tendencyData.time = it.time
                tempMap.put(it.time, tendencyData)
            }
            tempMap.each {
                channelDetail.add(it.value)
            }
            String siteName = detail.siteName
            if (siteName.contains(".")) {
                siteName = siteName.replace(".", "_")
            }
            String siteType = null
            if (detail.siteType == Site.SiteTypeEnum.weSite.key) {
                siteType = Site.SiteTypeEnum.weSite.desc
            } else if (detail.siteType == Site.SiteTypeEnum.weChat.key) {
                siteType = Site.SiteTypeEnum.weChat.desc
            } else if (detail.siteType == Site.SiteTypeEnum.weiBo.key) {
                siteType = Site.SiteTypeEnum.weiBo.desc
            }
            resultMap.put(siteName + "_" + siteType, channelDetail)
        }
        return resultMap
    }

    private Map getOneWeekDefaultData(Date date, String channelName) {
        Map tempMap = [:]
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < 7; i++) {
            cal.setTime(date)
            cal.add(Calendar.DATE, i);
            Map tendencyData = [:]
            tendencyData.channelName = channelName
            tendencyData.count = "0.00"
            tendencyData.time = cal.getTime()
            tempMap.put(cal.getTime(), tendencyData)
        }
        return tempMap
    }

    long getDistributeCount(int siteType, List siteDomain, Date startTime, Date endTime) {
        return siteInfoWeeklyRepo.getDistributeCount(siteType, siteDomain, startTime, endTime)
    }

    EvaluateReport getEvaluateReportByUserId(long userId) {
        return evaluateReportRepo.getEvaluateReportByUserId(userId, [2, 3])
    }

    EvaluateReport getEvaluateReportByUserIdAndStatus(long userId) {
        return evaluateReportRepo.getEvaluateReportByUserId(userId, [1, 4])
    }

    EvaluateReport getValidEvaluateReportById(long userId) {
        return evaluateReportRepo.getValidEvaluateReportById(userId)
    }

    EvaluateReport getEvaluateReportById(String evaluateId) {
        return evaluateReportRepo.getEvaluateReportById(evaluateId)
    }

    //获取大数据处理后的数据
    EvaluateChannel getWeeklyEvaluateChannel(int siteType, String siteName, String siteDomain, Date startTime, Date endTime) {
        if (siteType == Site.SITE_TYPE_WEBSITE) {
            return evaluateRepo.getWeeklyEvaluateChannel(siteType, siteDomain, startTime, endTime)
        }
        if (siteType == Site.SITE_TYPE_WECHAT) {
            return evaluateRepo.getWeeklyEvaluateChannel(siteType, siteName, startTime, endTime)
        }
        if (siteType == Site.SITE_TYPE_WEIBO) {
            return evaluateRepo.getWeeklyEvaluateChannel(siteType, siteName, startTime, endTime)
        }
    }

    //一周四力计算
    Map getWeeklyFourPower(String channelId, long userId, int siteType, String siteName, String siteDomain, Date startTime, Date endTime) {
        def fourPower = this.getWeeklyEvaluateChannel(siteType, siteName, siteDomain, startTime, endTime)
        return this.getChannelFourPower(channelId, userId, fourPower)
    }

    //四力计算
    Map getChannelFourPower(String channelId, long userId, def fourPower) {
        //获取四力
        Map map
        def psi = 0.00
        def mii = 0.00
        def tsi = 0.00
        def bsi = 0.00
        def multiple = 0.00//综合指数
        if (!fourPower) {
            map = [psi     : psi,
                   mii     : mii,
                   tsi     : tsi,
                   bsi     : bsi,
                   multiple: multiple
            ]
            return map
        }
        def defBsi = fourPower.bsi//引导
        def defTsi = fourPower.tsi//公信
        def defMii = fourPower.mii//影响
        def defPsi = fourPower.psi//传播

        EvaluateChannel channel = evaluateChannelRepo.findById(userId, channelId)

        psi = this.getNewPsi(defPsi, channel)

        mii = this.getNewMii(defMii, channel)

        tsi = this.getNewTsi(defTsi, channel)

        bsi = this.getNewBsi(defBsi, channel)

        channel.setPsi(psi)
        channel.setMii(mii)
        channel.setTsi(tsi)
        channel.setBsi(bsi)
        multiple = this.getNewMultiple(channel)

        map = [psi     : psi,
               mii     : mii,
               tsi     : tsi,
               bsi     : bsi,
               multiple: multiple
        ]
        return map
    }

    //获取综合默认的媒体级别
    def getMultipleMediaAttr() {
        def set = settingRepo.getSystemSetting("evaluate", "synthesize.mediaAttr").content
    }

    //获取综合默认的四力权重
    def getMultipleFourPowerWight() {
        def set = settingRepo.getSystemSetting("evaluate", "synthesize.parameters").content
    }

    //获取默认四力标准值
    Map getFourPowerStandardValue() {
        def content = settingRepo.getSystemSetting("evaluate", "tendency.standard").content
    }
    //获取默认四力的分类值
    def getFourPowerClassification() {
        def content = settingRepo.getSystemSetting("evaluate", "tendency.classification").content
    }

    //获取综合指数
    def getMultiple(double wl, double psiWight, double miiWight, double tsiWight, double bsiWight, EvaluateChannel evaluateChannel) {
        def psi = evaluateChannel.psi
        def mii = evaluateChannel.mii
        def tsi = evaluateChannel.tsi
        def bsi = evaluateChannel.bsi
        return wl * (psiWight * psi + miiWight * mii + tsiWight * tsi + bsiWight * bsi) / 100
    }

    //加参四力(w1:内容系数,standardValue:标准值)
    def calculateFourPower(double wl, double standardValue, double fourPower) {
        return (wl * fourPower) / standardValue
    }

    //获取新的引导
    def getNewBsi(double bsi, EvaluateChannel evaluateChannel) {
        double newBis
        Map wight = evaluateChannel.bsiWeight
        if (wight && wight.size() > 0) {
            Map classification = wight.get("classification")
            def standard = wight.get("standard")
            def value
            classification.each { elem ->
                value = elem.getValue()
            }
            newBis = this.calculateFourPower(value, standard, bsi)
        } else {
            newBis = this.calculateFourPower(1, 1, bsi)
        }
        return newBis
    }
    //新的影响
    def getNewMii(double val, EvaluateChannel evaluateChannel) {
        double key
        Map wight = evaluateChannel.miiWeight
        if (wight && wight.size() > 0) {
            Map classification = wight.get("classification")
            def standard = wight.get("standard")
            def value
            classification.each { elem ->
                value = elem.getValue()
            }
            key = this.calculateFourPower(value, standard, val)
        } else {
            key = this.calculateFourPower(1, 1, val)
        }
        return key
    }
    //新的公信
    def getNewTsi(double val, EvaluateChannel evaluateChannel) {
        double key
        Map wight = evaluateChannel.tsiWeight
        if (wight && wight.size() > 0) {
            Map classification = wight.get("classification")
            def standard = wight.get("standard")
            def value
            classification.each { elem ->
                value = elem.getValue()
            }
            key = this.calculateFourPower(value, standard, val)
        } else {
            key = this.calculateFourPower(1, 1, val)
        }
        return key
    }
    //新的传播
    def getNewPsi(double val, EvaluateChannel evaluateChannel) {
        double key
        Map wight = evaluateChannel.psiWeight
        if (wight && wight.size() > 0) {
            Map classification = wight.get("classification")
            def standard = wight.get("standard")
            def value
            classification.each { elem ->
                value = elem.getValue()
            }
            key = this.calculateFourPower(value, standard, val)
        } else {
            key = this.calculateFourPower(1, 1, val)
        }
        return key
    }
    //综合
    def getNewMultiple(EvaluateChannel evaluateChannel) {
        double key
        Map wight = evaluateChannel.multipleWeight
        if (wight && wight.size() > 0) {
            Map attr = wight.get("mediaAttr")
            Map get = wight.get("parameters")
            def value
            attr.each { elem ->
                value = elem.getValue()
            }
            def psi = get.get("传播力")
            def mii = get.get("影响力")
            def bsi = get.get("引导力")
            def tsi = get.get("公信力")
            key = this.getMultiple(value, psi, mii, tsi, bsi, evaluateChannel)
        } else {
            Map get = this.getMultipleFourPowerWight()
            def psi = get.get("传播力")
            def mii = get.get("影响力")
            def bsi = get.get("引导力")
            def tsi = get.get("公信力")
            key = this.getMultiple(1, psi, mii, tsi, bsi, evaluateChannel)
        }
        return key
    }

    //获取四力和综合指数比率
    def getFourPowerRate(long userId, String evaluateId, String channelId, Date startTime, Date endTime) {
        def psiRate = 0.00
        def miiRate = 0.00
        def tsiRate = 0.00
        def bsiRate = 0.00
        def multipleRate = 0.00
        Map map
        //查询上上周的记录
        EvaluateChannelDetail detailAgo = evaluateChannelRepo.findEvaluateChannelDetail(userId, channelId, DateUitl.getLastWeekMondayBegin(endTime), DateUitl.getThisWeekMondayBegin(endTime))
        //查询上周的
        EvaluateChannelDetail detail = evaluateChannelRepo.getEvaluateChannelDetail(userId, evaluateId, channelId, startTime, endTime)
        if (!(detailAgo && detail)) {
            map = [
                    psiRate     : null,
                    miiRate     : null,
                    tsiRate     : null,
                    bsiRate     : null,
                    multipleRate: null
            ]
            return map
        }
        def psi = detail.psi
        def mii = detail.mii
        def tsi = detail.tsi
        def bsi = detail.bsi
        def multiple = detail.multiple

        def psiAgo = detailAgo.psi
        def miiAgo = detailAgo.mii
        def tsiAgo = detailAgo.tsi
        def bsiAgo = detailAgo.bsi
        def multipleAgo = detailAgo.multiple
        psiRate = NumUtils.getRate(psiAgo, psi)
        miiRate = NumUtils.getRate(miiAgo, mii)
        tsiRate = NumUtils.getRate(tsiAgo, tsi)
        bsiRate = NumUtils.getRate(bsiAgo, bsi)
        multipleRate = NumUtils.getRate(multipleAgo, multiple)

        map = [
                psiRate     : psiRate,
                miiRate     : miiRate,
                tsiRate     : tsiRate,
                bsiRate     : bsiRate,
                multipleRate: multipleRate
        ]
        return map
    }


    EvaluateChannelDetail getEvaluateChannelDetail(long userId, String evaluateId, String channelId, Date startTime, Date endTime) {
        return evaluateChannelRepo.getEvaluateChannelDetail(userId, evaluateId, channelId, startTime, endTime)
    }

    //获取EvaluateChannelDetail四力
    def getEvaluateChannelDetailFourPower(long userId, String evaluateId, String channelId, Date startTime, Date endTime) {
        def map
        def psi = 0.00
        def mii = 0.00
        def tsi = 0.00
        def bsi = 0.00
        def multiple = 0.00//综合指数
        def detail = evaluateChannelRepo.getEvaluateChannelDetail(userId, evaluateId, channelId, startTime, endTime)
        if (!detail) {
            map = [psi     : NumUtils.doubleForMat(psi),
                   mii     : NumUtils.doubleForMat(mii),
                   tsi     : NumUtils.doubleForMat(tsi),
                   bsi     : NumUtils.doubleForMat(bsi),
                   multiple: NumUtils.doubleForMat(multiple)
            ]
            return map
        }
        psi = detail.psi
        mii = detail.mii
        tsi = detail.tsi
        bsi = detail.bsi
        multiple = detail.multiple
        map = [psi     : NumUtils.doubleForMat(psi),
               mii     : NumUtils.doubleForMat(mii),
               tsi     : NumUtils.doubleForMat(tsi),
               bsi     : NumUtils.doubleForMat(bsi),
               multiple: NumUtils.doubleForMat(multiple)
        ]
        return map
    }

    List getEvaluateChannelDetailByTime(long userId, String evaluateId, String channelId, Date strTime, Date endTime) {
        return evaluateChannelRepo.getEvaluateChannelDetailList(userId, evaluateId, channelId, strTime, endTime)
    }

    Map getMultipleTrendByTime(String key, long userId, String evaluateId, String channelId, Date strTime, Date endTime) {
        List<EvaluateChannelDetail> time = this.getEvaluateChannelDetailByTime(userId, evaluateId, channelId, strTime, endTime)
        Map map = [:]
        def data = []
        def timeRange = []
        def mapTime = [:]
        for (int i = 0; i < 7; i++) {
            def day = DateUitl.addDay(strTime, i)
            mapTime.put(day, NumUtils.doubleForMat(0.00))
        }

        //综合指数
        if ("multiple".equals(key)) {
            time.each { elem ->
                mapTime.put(elem.time, NumUtils.doubleForMat(elem.multiple))
            }
        }
        //传播力
        if ("psi".equals(key)) {
            time.each { elem ->
                mapTime.put(elem.time, NumUtils.doubleForMat(elem.psi))
            }
        }
        //影响力
        if ("mii".equals(key)) {
            time.each { elem ->
                mapTime.put(elem.time, NumUtils.doubleForMat(elem.mii))
            }
        }
        //公信力
        if ("tsi".equals(key)) {
            time.each { elem ->
                mapTime.put(elem.time, NumUtils.doubleForMat(elem.tsi))
            }
        }
        //引到力
        if ("bsi".equals(key)) {
            time.each { elem ->
                mapTime.put(elem.time, NumUtils.doubleForMat(elem.bsi))
            }
        }
        //发文量
        if ("publishCount".equals(key)) {
            time.each { elem ->
                mapTime.put(elem.time, NumUtils.doubleForMat(elem.publishCount))
            }
        }
        //累计转载
        if ("sumReprint".equals(key)) {
            time.each { elem ->
                mapTime.put(elem.time, NumUtils.doubleForMat(elem.sumReprint))
            }
        }
        //平均转载
        if ("avgReprint".equals(key)) {
            time.each { elem ->
                mapTime.put(elem.time, NumUtils.doubleForMat(elem.avgReprint))
            }
        }
        //累计阅读
        if ("sumRead".equals(key)) {
            time.each { elem ->
                mapTime.put(elem.time, NumUtils.doubleForMat(elem.sumRead))
            }
        }
        //平均阅读
        if ("avgRead".equals(key)) {
            time.each { elem ->
                mapTime.put(elem.time, NumUtils.doubleForMat(elem.avgRead))
            }
        }
        //累计点赞
        if ("sumLike".equals(key)) {
            time.each { elem ->
                mapTime.put(elem.time, NumUtils.doubleForMat(elem.sumLike))
            }
        }
        //平均点赞
        if ("avgLike".equals(key)) {
            time.each { elem ->
                mapTime.put(elem.time, NumUtils.doubleForMat(elem.avgLike))
            }
        }

        data.addAll(mapTime.values())
        timeRange.addAll(mapTime.keySet())


        map.put("data", data)
        map.put("timeRange", timeRange)
        return map
    }

    def getWeeklyKeywords(long userId, String channelId, Date startTime, Date endTime) {
        def cloud = []
        EvaluateChannel channel = evaluateChannelRepo.findById(userId, channelId)
        if (channel.siteType == Site.SITE_TYPE_WEBSITE) {
            cloud = keywordsWeeklyRepo.getKeywordsWeeklyBySite([channel.siteDomain], [], [], startTime, endTime)
        }
        if (channel.siteType == Site.SITE_TYPE_WECHAT) {
            cloud = keywordsWeeklyRepo.getKeywordsWeeklyBySite([], [channel.siteName], [], startTime, endTime)
        }
        if (channel.siteType == Site.SITE_TYPE_WEIBO) {
            cloud = keywordsWeeklyRepo.getKeywordsWeeklyBySite([], [], [channel.siteName], startTime, endTime)
        }
        return cloud
    }

    //获取每天大数据处理的数据（多条）四力
    def getDailyEvaluateChannel(int siteType, String siteName, String siteDomain, Date startTime, Date endTime) {
        if (siteType == Site.SITE_TYPE_WEBSITE) {
            return evaluateRepo.getDailyEvaluateChannel(siteType, siteDomain, startTime, endTime)
        }
        if (siteType == Site.SITE_TYPE_WECHAT) {
            return evaluateRepo.getDailyEvaluateChannel(siteType, siteName, startTime, endTime)
        }
        if (siteType == Site.SITE_TYPE_WEIBO) {
            return evaluateRepo.getDailyEvaluateChannel(siteType, siteName, startTime, endTime)
        }
    }
    //获取大数据处理后的数据  转载数
    List getSiteInfoDailyList(int siteType, String siteName, String siteDomain, Date startTime, Date endTime) {
        if (siteType == Site.SITE_TYPE_WEBSITE) {
            return siteInfoDailyRepo.getSiteInfoDailyList(siteType, siteDomain, startTime, endTime)
        }
        if (siteType == Site.SITE_TYPE_WECHAT) {
            return siteInfoDailyRepo.getSiteInfoDailyList(siteType, siteName, startTime, endTime)
        }
        if (siteType == Site.SITE_TYPE_WEIBO) {
            return siteInfoDailyRepo.getSiteInfoDailyList(siteType, siteName, startTime, endTime)
        }
    }
    //获取大数据处理的数据(单条)转载数
    def getSiteInfoDailyEvaluateChannel(int siteType, String siteName, String siteDomain, Date startTime, Date endTime) {
        if (siteType == Site.SITE_TYPE_WEBSITE) {
            return siteInfoDailyRepo.getSiteInfoDailyEvaluateChannel(siteType, siteDomain, startTime, endTime)
        }
        if (siteType == Site.SITE_TYPE_WECHAT) {
            return siteInfoDailyRepo.getSiteInfoDailyEvaluateChannel(siteType, siteName, startTime, endTime)
        }
        if (siteType == Site.SITE_TYPE_WEIBO) {
            return siteInfoDailyRepo.getSiteInfoDailyEvaluateChannel(siteType, siteName, startTime, endTime)
        }
    }
    //定时任务计算每周的四力和综合指数
    void weeklyEvaluateChannelFourPower(long userId) {
        List<EvaluateChannel> channels = evaluateChannelRepo.findByUserId(userId, null)
        channels.each { elem ->
            def channelId = elem.id
            def evaluateReport = this.getEvaluateReportByUserIdAndStatus(userId)
            def channel = evaluateChannelRepo.findById(userId, channelId)
            //文章数
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
            long count = siteInfoWeeklyRepo.getDistributeCount(channel.siteType, [channel.siteDomain], evaluateReport.startTime, evaluateReport.endTime)
            //计算四力和综合指数
            Map power = this.getWeeklyFourPower(channelId, userId, channel.siteType, channel.siteName, channel.siteDomain, evaluateReport.startTime, evaluateReport.endTime)
            def che = evaluateChannelRepo.getEvaluateChannelDetail(userId, evaluateReport.id, channelId, evaluateReport.startTime, evaluateReport.endTime)
            if (che) {
                Map rate = this.getFourPowerRate(userId, evaluateReport.id, channel.id, evaluateReport.startTime, evaluateReport.endTime)
                evaluateChannelRepo.modifyEvaluateChannelDetail(che.id, che.userId,
                        power.psi, power.mii, power.tsi, power.bsi, power.multiple,
                        rate.psiRate ?: 0.00, rate.miiRate ?: 0.00, rate.tsiRate ?: 0.00, rate.bsiRate ?: 0.00, rate.multipleRate ?: 0.00
                )
                return
            }
            long siteCount = siteInfoWeeklyRepo.getTotalCount(channel.siteType, evaluateReport.startTime, evaluateReport.endTime, [siteDomain])
            if (siteCount > 0) {
                //添加到EvaluateChannelDetail(只有四力)
                String id = UUID.randomUUID().toString()
                evaluateChannelRepo.addEvaluateChannelDetail(id, userId, evaluateReport.id, channelId,
                        count, channel.siteDomain, channel.siteType, channel.siteName,
                        evaluateReport.evaluateName, evaluateReport.startTime, evaluateReport.endTime,
                        power.psi, power.mii, power.tsi, power.bsi, power.multiple,
                        0.00, 0.00, 0.00, 0.00, 0.00
                )
                //计算上升率
                Map rate = this.getFourPowerRate(userId, evaluateReport.id, channel.id, evaluateReport.startTime, evaluateReport.endTime)
                evaluateChannelRepo.updateEvaluateChannelDetailFourPowerRate(id, userId, rate.psiRate ?: 0.00, rate.miiRate ?: 0.00, rate.tsiRate ?: 0.00, rate.bsiRate ?: 0.00, rate.multipleRate ?: 0.00)
            }
        }
    }

    //计算每天的四力和综合指数
    void dailyEvaluateChannelFourPower(long userId) {
        List<EvaluateChannel> channels = evaluateChannelRepo.findByUserId(userId, null)
        channels.each { elem ->
            def evaluateReport = this.getEvaluateReportByUserIdAndStatus(elem.userId)
            //获取数据处理每天的四力
            List<EvaluateChannel> channelList = this.getDailyEvaluateChannel(elem.siteType, elem.siteName, elem.siteDomain, evaluateReport.startTime, evaluateReport.endTime)

            channelList.each { el ->
                //计算每天的四力和综合指数
                Map map = this.getChannelFourPower(elem.id, elem.userId, el)
                //获取数据处理的每天评论点赞
                def siteInfo = this.getSiteInfoDailyEvaluateChannel(elem.siteType, elem.siteName, elem.siteDomain, el.time, el.time)
                def daily = evaluateChannelRepo.getEvaluateChannelDaily(userId, evaluateReport.id, elem.id, siteInfo.time, siteInfo.time)
                if (siteInfo && daily) {
                    evaluateChannelRepo.updateEvaluateChannelDaily(daily._id, userId,
                            map.psi, map.mii, map.tsi, map.bsi, map.multiple,
                            siteInfo.publishCount, siteInfo.sumReprint, siteInfo.avgReprint, siteInfo.sumRead, siteInfo.avgRead, siteInfo.sumLike, siteInfo.avgLike)
                    return
                } else if (!siteInfo && daily) {
                    evaluateChannelRepo.updateEvaluateChannelDaily(daily._id, userId,
                            map.psi, map.mii, map.tsi, map.bsi, map.multiple,
                            0l, 0l, 0.00, 0l, 0.00, 0l, 0.00)
                    return
                }
                if (siteInfo) {
                    evaluateChannelRepo.addEvaluateChannelDaily(
                            elem.userId, evaluateReport.id, elem.id,
                            elem.siteDomain, elem.siteType, elem.siteName,
                            evaluateReport.evaluateName, evaluateReport.startTime, evaluateReport.endTime, el.time,
                            map.psi, map.mii, map.tsi, map.bsi, map.multiple,
                            siteInfo.publishCount, siteInfo.sumReprint, siteInfo.avgReprint, siteInfo.sumRead, siteInfo.avgRead, siteInfo.sumLike, siteInfo.avgLike
                    )
                }
                if (!siteInfo) {
                    evaluateChannelRepo.addEvaluateChannelDaily(
                            elem.userId, evaluateReport.id, elem.id,
                            elem.siteDomain, elem.siteType, elem.siteName,
                            evaluateReport.evaluateName, evaluateReport.startTime, evaluateReport.endTime, el.time,
                            map.psi, map.mii, map.tsi, map.bsi, map.multiple,
                            0l, 0l, 0.00, 0l, 0.00, 0l, 0.00
                    )
                }
            }
        }


    }


}
