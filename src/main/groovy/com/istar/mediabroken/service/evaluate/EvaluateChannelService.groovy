package com.istar.mediabroken.service.evaluate

import com.istar.mediabroken.entity.account.Account
import com.istar.mediabroken.entity.capture.Site
import com.istar.mediabroken.entity.evaluate.EvaluateChannel
import com.istar.mediabroken.entity.evaluate.EvaluateReport
import com.istar.mediabroken.repo.account.AccountRepo
import com.istar.mediabroken.repo.admin.SettingRepo
import com.istar.mediabroken.repo.evaluate.EvaluateChannelRepo
import com.istar.mediabroken.repo.evaluate.EvaluateRepo
import com.istar.mediabroken.repo.evaluate.EvaluateReportRepo
import com.istar.mediabroken.service.account.AccountService
import groovy.util.logging.Slf4j
import org.apache.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import static com.istar.mediabroken.api.ApiResult.apiResult

/**
 * @author zxj
 * @create 2018/6/20
 */
@Service
@Slf4j
class EvaluateChannelService {

    @Autowired
    EvaluateChannelRepo evaluateChannelRepo
    @Autowired
    EvaluateTeamService evaluateTeamService
    @Autowired
    EvaluateService evaluateService
    @Autowired
    EvaluateRepo evaluateRepo
    @Autowired
    AccountRepo accountRepo
    @Autowired
    SettingRepo settingRepo
    @Autowired
    AccountService accountService
    @Autowired
    EvaluateReportRepo evaluateReportRepo

    List findAllChannel(long userId) {
        return evaluateChannelRepo.getUserAllChannel(userId)
    }
    List<EvaluateChannel> findChannelByTypeAndName(long userId, int type, String name, String teamId) {
        return evaluateChannelRepo.findChannelByTypeAndName(userId, type, name, teamId)
    }

    List getEvaluateChannelAndTeam(long userId, int type, String name) {
        def list = []
        def teamsList = evaluateTeamService.getEvaluateTeams(userId)
        if (!teamsList) {
            def channelList = this.findChannelByTypeAndName(userId, type, name, '0')
            def map = [
                    id                 : '0',
                    userId             : userId,
                    teamName           : '?????????',
                    evaluateChannelList: channelList
            ]
            list.add(map)
            return list
        }
        //????????????
        def each = teamsList.each { team ->
            def channelList = this.findChannelByTypeAndName(userId, type, name, team.id)
            def map = [
                    id                 : team.id,
                    userId             : userId,
                    teamName           : team.teamName,
                    evaluateChannelList: channelList
            ]
            list.add(map)
        }
        //?????????
        def channelList = this.findChannelByTypeAndName(userId, type, name, '0')
        if (channelList) {
            def map = [
                    id                 : '0',
                    userId             : userId,
                    teamName           : '?????????',
                    evaluateChannelList: channelList
            ]
            list.add(map)
        }
        return list
    }

    EvaluateChannel findById(long userId, String channelId){
        return evaluateChannelRepo.findById(userId, channelId)
    }

    def setChannelMultipleWeight(long userId, String channelId, String mediaAttrName, double mediaAttrVal, int psiWeight, int miiWeight, int tsiWeight, int bsiWeight) {
        Map mediaAttr = [:]
        mediaAttr.put(mediaAttrName, mediaAttrVal)

        Map parameters = ["?????????": psiWeight,
                          "?????????": miiWeight,
                          "?????????": bsiWeight,
                          "?????????": tsiWeight]
        Map multipleWeight = [mediaAttr: mediaAttr, parameters: parameters]
        def weight = evaluateChannelRepo.setChannelMultipleWeight(userId, channelId, multipleWeight)
        if (weight) {
            //?????????
            this.modifyEvaluateChannelDetailAndDaily(userId, channelId)
        }
        return weight
    }

    def setChannelFourPowerWeight(long userId, String channelId, String fourPower, String classificationName, double classificationVal, int standardKey) {
        def res = true
        try {
            Map classification = [:]
            classification.put(classificationName, classificationVal)
            Map map = [classification: classification, standard: standardKey]
            //?????????
            if ("psi".equals(fourPower)) {
                evaluateChannelRepo.setChannelPsiWeight(userId, channelId, map)
            }
            //?????????
            if ("mii".equals(fourPower)) {
                evaluateChannelRepo.setChannelMiiWeight(userId, channelId, map)
            }
            //?????????
            if ("tsi".equals(fourPower)) {
                evaluateChannelRepo.setChannelTsiWeight(userId, channelId, map)
            }
            //?????????
            if ("bsi".equals(fourPower)) {
                evaluateChannelRepo.setChannelBsiWeight(userId, channelId, map)
            }
            this.modifyEvaluateChannelDetailAndDaily(userId, channelId)
        } catch (e) {
            res = false
        }
        return res
    }

    def intiChannelWeight(long userId, String channelId, String fourPower) {
        def res = true
        try {
            Map map = [:]
            //??????
            if ("multiple".equals(fourPower)) {
                evaluateChannelRepo.setChannelMultipleWeight(userId, channelId, map)
            }
            //?????????
            if ("psi".equals(fourPower)) {
                evaluateChannelRepo.setChannelPsiWeight(userId, channelId, map)
            }
            //?????????
            if ("mii".equals(fourPower)) {
                evaluateChannelRepo.setChannelMiiWeight(userId, channelId, map)
            }
            //?????????
            if ("tsi".equals(fourPower)) {
                evaluateChannelRepo.setChannelTsiWeight(userId, channelId, map)
            }
            //?????????
            if ("bsi".equals(fourPower)) {
                evaluateChannelRepo.setChannelBsiWeight(userId, channelId, map)
            }
            this.modifyEvaluateChannelDetailAndDaily(userId, channelId)
        } catch (e) {
            res = false
        }
        return res
    }

    //??????????????????????????????????????????
    def modifyEvaluateChannelDetailAndDaily(long userId, String channelId) {
        //???????????????
        def evaluateReport = evaluateService.getEvaluateReportByUserId(userId)
        //????????????
        def channel = this.findById(userId, channelId)
        //??????????????????????????????
        Map power = evaluateService.getWeeklyFourPower(channelId, userId, channel.siteType,channel.siteName, channel.siteDomain, evaluateReport.startTime, evaluateReport.endTime)
        //???????????????
        Map rate = evaluateService.getFourPowerRate(userId, evaluateReport.id, channel.id, evaluateReport.startTime, evaluateReport.endTime)
        def detail = evaluateChannelRepo.getEvaluateChannelDetail(userId, evaluateReport.id, channel.id, evaluateReport.startTime, evaluateReport.endTime)
        //?????????????????????EvaluateChannelDetail
        evaluateChannelRepo.updateEvaluateChannelDetail(detail.id, userId,
                power.psi, power.mii, power.tsi, power.bsi, power.multiple,
                rate.psiRate ?: 0.00, rate.miiRate ?: 0.00, rate.tsiRate ?: 0.00, rate.bsiRate ?: 0.00, rate.multipleRate ?: 0.00)
        //?????????dailyFourPower???????????????????????????
        List channelList = evaluateService.getDailyEvaluateChannel(channel.siteType,channel.siteName, channel.siteDomain, evaluateReport.startTime, evaluateReport.endTime)
        //??????EvaluateChannelDaily
        channelList.each { elem ->
            //????????????????????????????????????
            Map map = evaluateService.getChannelFourPower(channelId, userId, elem)
            //?????????????????????
            def siteInfo = evaluateService.getSiteInfoDailyEvaluateChannel(channel.siteType,channel.siteName, channel.siteDomain, elem.time, elem.time)
            def daily = evaluateChannelRepo.getEvaluateChannelDetailList(userId, evaluateReport.id, channelId, elem.time, elem.time).get(0)
            if (siteInfo){
                evaluateChannelRepo.updateEvaluateChannelDaily(daily.id, userId,
                        map.psi ?: 0.00, map.mii ?: 0.00, map.tsi ?: 0.00, map.bsi ?: 0.00, map.multiple ?: 0.00,
                        siteInfo.publishCount ?: 0l, siteInfo.sumReprint ?: 0l, siteInfo.avgReprint ?: 0.00, siteInfo.sumRead ?: 0l, siteInfo.avgRead ?: 0.00, siteInfo.sumLike ?: 0l, siteInfo.avgLike ?: 0.00
                )
            }

            if (!siteInfo){
                evaluateChannelRepo.updateEvaluateChannelDaily(daily.id, userId,
                        map.psi ?: 0.00, map.mii ?: 0.00, map.tsi ?: 0.00, map.bsi ?: 0.00, map.multiple ?: 0.00,
                        0l,0l, 0.00, 0l, 0.00, 0l, 0.00
                )
            }
        }
        //????????????????????????
        evaluateService.updateEvaluateInfo(evaluateReport.id)
        evaluateService.updateIndexTop(evaluateReport.id)
    }


    def addEvaluateChannel(long userId, int siteType, String siteName, String siteDomain, String evaluateTeamId) {
        //??????????????????
        def profile = accountService.getEvaluateChannelAccountProfile(userId)
        def meidaCount = profile.maxMediaSiteCount
        def wechatCount = profile.maxWechatSiteCount
        def weiboCount = profile.maxWeiboSiteCount
        def size = evaluateChannelRepo.findByUserId(userId, siteType).size()
        if (siteType == Site.SITE_TYPE_WEBSITE) {
            def var = meidaCount - size-1
            if (var < 0) {
                return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "?????????????????????????????????" + size + "???????????????????????????" + 0 + "????????????")
            }
            EvaluateChannel evaluateChannel = new EvaluateChannel(UUID.randomUUID().toString(), userId, siteName, siteDomain, siteType, evaluateTeamId, new Date(), new Date())
            def channel = evaluateChannelRepo.addEvaluateChannel(evaluateChannel)
            return apiResult([status: HttpStatus.SC_OK, channelId: channel, msg: "?????????" + (size + 1) + "???????????????????????????" + var + "????????????"])
        }
        if (siteType == Site.SITE_TYPE_WECHAT) {
            def var = wechatCount - size-1
            if (var < 0) {
                return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "????????????????????????????????????" + size + "??????????????????????????????" + 0 + "???????????????")
            }
            EvaluateChannel evaluateChannel = new EvaluateChannel(UUID.randomUUID().toString(), userId, siteName, siteDomain, siteType, evaluateTeamId, new Date(), new Date())
            def channel = evaluateChannelRepo.addEvaluateChannel(evaluateChannel)
            return apiResult([status: HttpStatus.SC_OK, channelId: channel, msg: "?????????" + (size + 1) + "??????????????????????????????" + var + "???????????????"])
        }

        if (siteType == Site.SITE_TYPE_WEIBO) {
            def var = weiboCount - size-1
            if (var < 0) {
                return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "?????????????????????????????????" + size + "???????????????????????????" + 0 + "????????????")
            }
            EvaluateChannel evaluateChannel = new EvaluateChannel(UUID.randomUUID().toString(), userId, siteName, siteDomain, siteType, evaluateTeamId, new Date(), new Date())
            def channel = evaluateChannelRepo.addEvaluateChannel(evaluateChannel)
            return apiResult([status: HttpStatus.SC_OK, channelId: channel, msg: "?????????" + (size + 1) + "???????????????????????????" + var + "????????????"])
        }

        return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "?????????????????????????????????")
    }

    def getEvaluateChannelByUserId(long userId, int siteType, String siteName, String siteDomain) {
        def channel = evaluateChannelRepo.findChannel(userId, siteType, siteName, siteDomain, "")
    }

    def getEvaluateChannelByUserIdAndTeam(long userId, int siteType, String siteName, String siteDomain, String teamId) {
        def channel = evaluateChannelRepo.findChannel(userId, siteType, siteName, siteDomain, teamId)
    }

    //?????????????????????????????????????????????????????????????????????
    def modifyStatFlag(long userId, def channelIds) {
        evaluateChannelRepo.modifyStatFlag(userId, channelIds)
    }

    def checkChannel(long userId, String channelId, String teamId) {
        def flag = false
        //??????????????????????????????
        try {
            EvaluateChannel id = evaluateChannelRepo.findById(userId, channelId)
            List<EvaluateReport> report = evaluateReportRepo.getEvaluateReportByUser(userId, [1, 2, 3])
            if (channelId) {
                report.each { elem ->
                    elem.channels.each { el ->
                        if (id.siteType == el.siteType && id.siteName.equals(el.siteName) && id.siteDomain.equals(el.siteDomain)) {
                            1 / 0
                        }
                    }
                }
            }

            if (teamId) {
                List<EvaluateChannel> teams = evaluateChannelRepo.findByTeamId(userId, teamId)
                report.each { elem ->
                    elem.channels.each { el ->
                        teams.each { team ->
                            if (team.siteType == el.siteType && team.siteName.equals(el.siteName) && team.siteDomain.equals(el.siteDomain)) {
                                1 / 0
                            }
                        }
                    }
                }
            }


        } catch (e) {
            flag = true
        }
        return flag
    }

    List getChannelList(int siteType, String siteName, String siteDomain) {
        return evaluateChannelRepo.getChannelListByTypeAndName(siteType, siteName, siteDomain)
    }

    def delChannelAndChannelStat(long userId, String channelId) {
        //?????????????????????
        def id = this.findById(userId, channelId)

        evaluateChannelRepo.delChannelById(userId, channelId)

        //?????????????????????evaluateChannel???
        def list = this.getChannelList(id.siteType, id.siteName, id.siteDomain)
        if (list == null || list.size() <= 0) {
            //evaluateChannel 0??????????????????evaluateChannel ???????????????channelForStat ????????????
            evaluateChannelRepo.delChannelForStatById(id.siteType, id.siteName, id.siteDomain, [1, 2])
        } else {
            //??????evaluateChannel???????????????????????? channelForStat????????????
            int official = 0
            int trial = 0
            list.each { channel ->
                Account account = accountRepo.getUserById(channel.userId)
                //??????
                if (account.userType.equals(Account.USERTYPE_TRIAL)) {
                    trial++
                }
                if (account.userType.equals(Account.USERTYPE_OFFICIAL)) {
                    official++
                }
            }

            if (trial <= 0) {
                //??????channelForStat???????????? ???2???
                evaluateChannelRepo.delChannelForStatById(id.siteType, id.siteName, id.siteDomain, [2])

            }
            if (official <= 0) {
                //??????channelForStat???????????? ???1???
                evaluateChannelRepo.delChannelForStatById(id.siteType, id.siteName, id.siteDomain, [1])
            }
        }

    }


    def modifyEvaluateChannel(long userId, String channelId, int siteType, String siteName, String siteDomain, String evaluateTeamId) {
        //?????????????????????
        def id = this.findById(userId, channelId)
        if (!id) {
            apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "?????????????????????")
        }
        if (siteType == Site.SITE_TYPE_WEBSITE) {
            evaluateChannelRepo.modifyChannel(userId, channelId, siteType, siteName, siteDomain, evaluateTeamId)
            return apiResult([status: HttpStatus.SC_OK, channelId: channelId, msg: "???????????????"])
        }
        if (siteType == Site.SITE_TYPE_WECHAT) {
            evaluateChannelRepo.modifyChannel(userId, channelId, siteType, siteName, siteDomain, evaluateTeamId)
            return apiResult([status: HttpStatus.SC_OK, channelId: channelId, msg: "???????????????"])
        }

        if (siteType == Site.SITE_TYPE_WEIBO) {
            evaluateChannelRepo.modifyChannel(userId, channelId, siteType, siteName, siteDomain, evaluateTeamId)
            return apiResult([status: HttpStatus.SC_OK, channelId: channelId, msg: "???????????????"])
        }

        //??????
        def list = this.getChannelList(id.siteType, id.siteName, id.siteDomain)

        if (list == null || list.size() <= 0) {
            //evaluateChannel 0??????????????????evaluateChannel ???????????????channelForStat ????????????
            evaluateChannelRepo.delChannelForStatById(id.siteType, id.siteName, id.siteDomain, [1, 2])
        } else {
            //??????evaluateChannel???????????????????????? channelForStat????????????
            int official = 0
            int trial = 0
            list.each { channel ->
                Account account = accountRepo.getUserById(channel.userId)
                //??????
                if (account.userType.equals(Account.USERTYPE_TRIAL)) {
                    trial++
                }
                if (account.userType.equals(Account.USERTYPE_OFFICIAL)) {
                    official++
                }
            }

            if (trial <= 0) {
                //??????channelForStat???????????? ???2???
                evaluateChannelRepo.delChannelForStatById(id.siteType, id.siteName, id.siteDomain, [2])

            }
            if (official <= 0) {
                //??????channelForStat???????????? ???1???
                evaluateChannelRepo.delChannelForStatById(id.siteType, id.siteName, id.siteDomain, [1])
            }
        }

    }


    def modifyChannelTeam(long userId, String teamId, List channelIds) {
        evaluateChannelRepo.modifyChannelTeamId(userId, teamId, channelIds)
    }

    def findForbiddenChannel(String siteDomain, int siteType, String siteName) {
        return evaluateChannelRepo.findForbiddenChannel(siteDomain, siteType, siteName)
    }
}
