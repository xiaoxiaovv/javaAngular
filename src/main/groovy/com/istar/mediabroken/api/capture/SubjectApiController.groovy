package com.istar.mediabroken.api.capture

import com.istar.mediabroken.api.CheckExpiry
import com.istar.mediabroken.api.CheckPrivilege
import com.istar.mediabroken.api.CurrentUser
import com.istar.mediabroken.api.CurrentUserId
import com.istar.mediabroken.entity.LoginUser
import com.istar.mediabroken.entity.account.Account
import com.istar.mediabroken.entity.account.Privilege
import com.istar.mediabroken.entity.capture.KeywordsScopeEnum
import com.istar.mediabroken.service.account.AccountService
import com.istar.mediabroken.service.capture.SubjectService
import com.istar.mediabroken.service.system.OperationLogService
import com.istar.mediabroken.utils.ExprUtils
import com.istar.mediabroken.utils.StringUtils
import groovy.util.logging.Slf4j
import org.apache.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

import java.text.SimpleDateFormat

import static com.istar.mediabroken.api.ApiResult.apiResult
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR
import static org.springframework.web.bind.annotation.RequestMethod.DELETE
import static org.springframework.web.bind.annotation.RequestMethod.GET
import static org.springframework.web.bind.annotation.RequestMethod.POST
import static org.springframework.web.bind.annotation.RequestMethod.PUT

/**
 * Author: Luda
 * Time: 2017/8/1
 */
@RestController
@Slf4j
@RequestMapping(value = "/api/capture/")
class SubjectApiController {
    @Autowired
    SubjectService subjectService
    @Autowired
    OperationLogService operationLogService
    @Autowired
    AccountService accountService
    @Value('${subject.orcount}')
    int orCount
    @Value('${subject.keywordscount}')
    int keywordsCount
    /**
     * ????????????
     * @param userId
     * @param subjectName
     * @param monitorName
     * @param keywordsScope
     * @param titleKeywords
     * @param keyWords
     * @param areaWords
     * @param excludeWords
     * @param originalKeywords ???????????????keyWords?????????string????????????json?????????originalKeywords???true??????????????????json???str??????????????????originalKeywords???false?????????????????????
     * @return
     */
    @RequestMapping(value = "subject", method = POST)
    public Map addUserSubject(
            @CurrentUserId Long userId,
            @RequestParam(value = "subjectName", required = true) String subjectName,
            @RequestParam(value = "keywordsScope", required = false, defaultValue = "") int keywordsScope,
            @RequestParam(value = "titleKeywords", required = false, defaultValue = "") String titleKeywords,
            @RequestParam(value = "keyWords", required = false, defaultValue = "") String keyWords,
            @RequestParam(value = "areaWords", required = false, defaultValue = "") String areaWords,
            @RequestParam(value = "excludeWords", required = false, defaultValue = "") String excludeWords,
            @RequestParam(value = "originalKeywords", required = false, defaultValue = "false") boolean originalKeywords
    ) {
        subjectName = URLDecoder.decode(subjectName,"utf-8")
        keyWords = URLDecoder.decode(keyWords,"utf-8")
        titleKeywords = URLDecoder.decode(titleKeywords,"utf-8")
        excludeWords = URLDecoder.decode(excludeWords,"utf-8")
        //??????????????????????????????
        if (subjectName.equals("") || subjectName.equals("")) {
            return apiResult(SC_INTERNAL_SERVER_ERROR, '?????????????????????');
        }else if (subjectName.length() > 20){
            return apiResult(SC_INTERNAL_SERVER_ERROR, '????????????????????????20?????????');
        }
        if (keywordsScope != KeywordsScopeEnum.contentScope.key && keywordsScope != KeywordsScopeEnum.titleScope.key ){
            return apiResult(SC_INTERNAL_SERVER_ERROR, '??????????????????????????????');
        }
        //?????????????????????3???
        if (keywordsScope == KeywordsScopeEnum.contentScope.key) {
            if (!"".equals(keyWords)) {
                if (originalKeywords){
                    if (StringUtils.existSpecialCode(keyWords)){
                        return apiResult(SC_INTERNAL_SERVER_ERROR, "?????????????????????????????????");
                    }
                    Map jsonRes = ExprUtils.strExpr2JsonExpr(keyWords)
                    if (jsonRes.msg){
                        return apiResult(SC_INTERNAL_SERVER_ERROR, jsonRes.msg);
                    }
                    keyWords = jsonRes.jsonExpr
                }else {
                    try {
                        if (StringUtils.existSpecialCode(ExprUtils.jsonExpr2StrExpr(keyWords).strExpr)){
                            return apiResult(SC_INTERNAL_SERVER_ERROR, "?????????????????????????????????");
                        }
                    }catch (Exception e){
                        return apiResult(SC_INTERNAL_SERVER_ERROR, "????????????????????????");
                    }
                }
                def verificationResult = subjectService.isKeywordsVerificated(keyWords)
                if (verificationResult.status != HttpStatus.SC_OK) {
                    return verificationResult
                }
            } else {
                return apiResult([status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: '???????????????????????????']);
            }
        } else if (keywordsScope == KeywordsScopeEnum.titleScope.key) {
            if ("".equals(titleKeywords)) {
                return apiResult([status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: '???????????????????????????']);
            }else {
                titleKeywords = StringUtils.replaceMultipleSpace(titleKeywords)
                if (titleKeywords.split(" ").size() > keywordsCount){
                    return apiResult([status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "?????????????????????" + keywordsCount + "???"])
                }
            }
        }
        def result = subjectService.addUserSubject(userId, subjectName.trim(), keywordsScope, titleKeywords.trim(), keyWords.trim(), areaWords.trim(), excludeWords.trim())
        return result
    }

    /**
     * ????????????
     * @param userId
     * @return
     */
    @RequestMapping(value = "subjects", method = GET)
    public Map getSubjects(
            @CurrentUserId Long userId
    ) {
        def result = subjectService.getUserSubjects(userId);
        return result;
    }

    /**
     * ????????????
     * @param userId
     * @param subjectId
     * @param originalKeywords ???????????????keyWords?????????string????????????json?????????originalKeywords???true??????????????????json???str??????????????????originalKeywords???false?????????????????????
     * @return
     */
    @RequestMapping(value = "subject/{subjectId}", method = GET)
    public Map getSubjectById(
            @CurrentUserId Long userId,
            @PathVariable("subjectId") String subjectId,
            @RequestParam(value = "originalKeywords", required = false, defaultValue = "false") boolean originalKeywords
    ) {
        def subject = subjectService.getUserSubjectById(userId, subjectId);
        if (!subject) {
            subject = []
        }else {
            if (originalKeywords){
                subject.keyWords = ExprUtils.jsonExpr2StrExpr(subject.keyWords).strExpr
            }
        }
        return apiResult([status: HttpStatus.SC_OK, msg: subject])
    }

    /**
     * ????????????
     * @param userId
     * @param subjectId
     * @return
     */
    @RequestMapping(value = "subject/{subjectId}", method = DELETE)
    public Map removeSubject(
            @CurrentUserId Long userId,
            @PathVariable("subjectId") String subjectId
    ) {
        subjectService.removeSubject(userId, subjectId)
        return apiResult();
    }

    /**
     * ????????????
     * @param userId
     * @param subjectName
     * @param monitorName
     * @param keywordsScope
     * @param titleKeywords
     * @param keyWords
     * @param areaWords
     * @param excludeWords
     * @param originalKeywords ???????????????keyWords?????????string????????????json?????????originalKeywords???true??????????????????json???str??????????????????originalKeywords???false?????????????????????
     * @return
     */
    @RequestMapping(value = "subject/{subjectId}", method = [PUT,POST])
    public Map modifySubject(
            @CurrentUserId Long userId,
            @PathVariable("subjectId") String subjectId,
            @RequestParam(value = "subjectName", required = true) String subjectName,
            @RequestParam(value = "keywordsScope", required = true) int keywordsScope,
            @RequestParam(value = "titleKeywords", required = false, defaultValue = "") String titleKeywords,
            @RequestParam(value = "keyWords", required = false, defaultValue = "") String keyWords,
            @RequestParam(value = "areaWords", required = false, defaultValue = "") String areaWords,
            @RequestParam(value = "excludeWords", required = false, defaultValue = "") String excludeWords,
            @RequestParam(value = "originalKeywords", required = false, defaultValue = "false") boolean originalKeywords
    ) {
        subjectName = URLDecoder.decode(subjectName,"utf-8")
        keyWords = URLDecoder.decode(keyWords,"utf-8")
        titleKeywords = URLDecoder.decode(titleKeywords,"utf-8")
        excludeWords = URLDecoder.decode(excludeWords,"utf-8")
        //??????????????????????????????
        if (subjectName.equals("") || subjectName.equals("")) {
            return apiResult(SC_INTERNAL_SERVER_ERROR, '?????????????????????');
        }else if (subjectName.length() > 20){
            return apiResult(SC_INTERNAL_SERVER_ERROR, '????????????????????????20?????????');
        }
        if (keywordsScope != KeywordsScopeEnum.contentScope.key && keywordsScope != KeywordsScopeEnum.titleScope.key ){
            return apiResult(SC_INTERNAL_SERVER_ERROR, '??????????????????????????????');
        }
        //???????????????
        String keyWordStr = ""
        if (keywordsScope == KeywordsScopeEnum.contentScope.key) {
            if (!"".equals(keyWords)) {
                if (originalKeywords){
                    if (StringUtils.existSpecialCode(keyWords)){
                        return apiResult(SC_INTERNAL_SERVER_ERROR, "?????????????????????????????????");
                    }
                    Map jsonRes = ExprUtils.strExpr2JsonExpr(keyWords)
                    if (jsonRes.msg){
                        return apiResult(SC_INTERNAL_SERVER_ERROR, jsonRes.msg);
                    }
                    keyWords = jsonRes.jsonExpr
                }else {
                    try {
                        if (StringUtils.existSpecialCode(ExprUtils.jsonExpr2StrExpr(keyWords).strExpr)){
                            return apiResult(SC_INTERNAL_SERVER_ERROR, "?????????????????????????????????");
                        }
                    }catch (Exception e){
                        return apiResult(SC_INTERNAL_SERVER_ERROR, "????????????????????????");
                    }
                }
                def verificationResult = subjectService.isKeywordsVerificated(keyWords)
                if (verificationResult.status != HttpStatus.SC_OK) {
                    return verificationResult
                }
            } else {
                return apiResult([status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: '???????????????????????????']);
            }
        } else if (keywordsScope == KeywordsScopeEnum.titleScope.key) {
            if ("".equals(titleKeywords)) {
                return apiResult([status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: '???????????????????????????']);
            }else {
                titleKeywords = StringUtils.replaceMultipleSpace(titleKeywords)
                if (titleKeywords.split(" ").size() > keywordsCount){
                    return apiResult([status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "?????????????????????" + keywordsCount + "???"])
                }
            }
        }
        def result = subjectService.modifyUserSubject(userId, subjectId, subjectName.trim(), keywordsScope, titleKeywords.trim(), keyWords.trim(), areaWords.trim(), excludeWords.trim())
        return result;
    }

    @RequestMapping(value = "subject/{subjectId}/news", method = GET)
    public Map getUserSubjectNews(
            @CurrentUserId Long userId,
            @PathVariable("subjectId") String subjectId,
            @RequestParam(value = "siteType", required = false, defaultValue = "1") int siteType,//0?????? 1 web 2 ??????
            @RequestParam(value = "hot", required = false, defaultValue = "0") int hot,//0?????? 1 ??? 2 ??? 3 ???
            @RequestParam(value = "startTime", required = false, defaultValue = "") String startTime,
            @RequestParam(value = "endTime", required = false, defaultValue = "") String endTime,
            @RequestParam(value = "classification", required = false, defaultValue = "") String classification, //todo ????????????????????????
            @RequestParam(value = "orientation", required = false, defaultValue = "0") int orientation, //0????????? 1????????? 2????????? 3????????? 4????????????
            @RequestParam(value = "hasPic", required = false, defaultValue = "0") int hasPic, // 0 ??????,1 ?????????,2 ????????????
            @RequestParam(value = "order", required = false, defaultValue = "1") int order, //1 ???????????? 2 ????????????
            @RequestParam(value = "queryScope", required = false, defaultValue = "1") int queryScope, //???????????? 1 ?????? 2??????
            @RequestParam(value = "queryString", required = false, defaultValue = "") String queryString, //???????????????
            @RequestParam(value = "pageSize", required = false, defaultValue = "50") int pageSize,
            @RequestParam(value = "pageNo", required = false, defaultValue = "1") int pageNo,
            @RequestParam(value = "queryId", required = false, defaultValue = "") String queryId,
            @RequestParam(value = "withHighlight", required = false, defaultValue = "true") boolean withHighlight
    ) {
        //??????????????????????????????
        def sdf = new SimpleDateFormat('yyyy-MM-dd HH:mm:ss')
        def startDate
        def endDate
        try {
            if (startTime) {
                startDate = sdf.parse(startTime)
            }
            if (endTime){
                endDate = sdf.parse(endTime)
            }
        } catch (Exception e) {
            return apiResult(SC_INTERNAL_SERVER_ERROR, "?????????????????????")
        }

        if ((pageSize * pageNo) > 50000) {
            return apiResult(SC_INTERNAL_SERVER_ERROR, "??????????????????????????????")
        }
        if (pageNo == 1){
            Account userInfo = accountService.getUserInfoById(userId)
            operationLogService.addOperationLog(userInfo, "subject", siteType, hot, startDate, endDate, orientation, hasPic, order, queryString, pageSize)
        }
        def result = subjectService.getUserSubjectNewsByQueryId(userId, subjectId, siteType, hot, startDate, endDate, classification, orientation, hasPic, order, queryScope, queryString, pageSize, queryId, withHighlight)
        return result
    }

    @RequestMapping(value = "subject/latestNews", method = GET)
    public Map getUserSubjectLatestNews(
            @CurrentUserId Long userId
    ) {
        def result = subjectService.getUserSubjectLatestNews(userId)
        return result
    }

    @CheckExpiry
    @CheckPrivilege(privileges = [Privilege.SUBJECT_VIEW])
    @RequestMapping(value = "subject/{subjectId}/count", method = GET)
    @ResponseBody
    Object getSubjectNewsCount(
            @CurrentUser LoginUser user,
            @PathVariable String subjectId
    ) {
        def obj = subjectService.getSubjectNewsCountBySubjectId(user.userId, subjectId)
        return apiResult([msg : obj])
    }

    @CheckPrivilege(privileges = [Privilege.SUBJECT_VIEW])
    @RequestMapping(value = "subject/{subjectId}/count", method = DELETE)
    @ResponseBody
    Object resetSubjectResetTime(
            @CurrentUser LoginUser user,
            @PathVariable String subjectId
    ) {
        subjectService.resetSubjectCountInfo(user.userId, subjectId)
        return apiResult()
    }

}
