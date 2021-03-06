package com.istar.mediabroken.api.account

import com.istar.mediabroken.api.CheckPrivilege
import com.istar.mediabroken.api.CurrentUser
import com.istar.mediabroken.api.CurrentUserId
import com.istar.mediabroken.api.NotCheckExpiry
import com.istar.mediabroken.entity.LoginUser
import com.istar.mediabroken.entity.account.Account
import com.istar.mediabroken.entity.account.AccountOpenTypeEnum
import com.istar.mediabroken.entity.account.LoginSourceEnum
import com.istar.mediabroken.entity.account.Privilege
import com.istar.mediabroken.entity.account.Role
import com.istar.mediabroken.entity.app.Agent
import com.istar.mediabroken.service.CaptureService
import com.istar.mediabroken.service.VerifyCodeService
import com.istar.mediabroken.service.account.AccountService
import com.istar.mediabroken.service.account.SimulateUserSecretKeyService
import com.istar.mediabroken.service.account.UserRoleService
import com.istar.mediabroken.service.app.AgentService
import com.istar.mediabroken.service.captcha.CaptchaService
import com.istar.mediabroken.service.captcha.LineCaptcha
import com.istar.mediabroken.service.system.UserBehaviorService
import com.istar.mediabroken.utils.DateUitl
import com.istar.mediabroken.utils.StringUtils
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.time.DateFormatUtils
import org.apache.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import static com.istar.mediabroken.api.ApiResult.apiResult
import static org.apache.http.HttpStatus.*
import static org.springframework.web.bind.annotation.RequestMethod.PUT

@Controller
@Slf4j
@RequestMapping(value = "/api/account")
public class AccountApiController {
    @Value("env")
    String env
    @Autowired
    AccountService accountService
    @Autowired
    CaptureService captureSrv
    @Autowired
    VerifyCodeService verifyCodeSrv
    @Autowired
    CaptchaService captchaSrv
    @Autowired
    AgentService agentService
    @Autowired
    SimulateUserSecretKeyService simulateUserSecretKeyService
    @Autowired
    private UserBehaviorService userBehaviorSrv
    @Autowired
    UserRoleService userRoleService


    int EXPIRY_INTERVAL = 10 * 60 * 1000
    int CAPTCHA_CODE_COUNT = 4
    int CODE_VALIDITY = 1 * 60 * 1000


    @RequestMapping(value = "/authority", method = RequestMethod.GET)
    @ResponseBody
    public Map getAccountAuthority(
            @CurrentUser LoginUser user
    ) {
        int img = 0;
        int video = 0;
        int tag = 0;
        Map map = new HashMap();
        def role = accountService.getAccountRole(user.userId)
        def name = userRoleService.getRoleByAgentIdAndRoleName(user.agentId, "?????????????????????")
        def nameMag = userRoleService.getRoleByAgentIdAndRoleName(user.agentId, "????????????????????????")
        String roleId = name.getId()
        String roleIdMag = nameMag.getId()
        if (role) {
            //?????????????????????????????????????????????????????????
            if (org.apache.commons.lang3.StringUtils.equals(role.roleId, roleId) || org.apache.commons.lang3.StringUtils.equals(role.roleId, roleIdMag)) {
                img = 1;
                video = 1;
                tag = 1;
                map.put("img", img)
                map.put("video", video)
                map.put("tag", tag)
                return apiResult(map)
            }
        }
        map.put("img", img)
        map.put("video", video)
        map.put("tag", tag)
        return apiResult(map)
    }
    /*
     *
     * ?????????????????????
     *
     * ???????????????
     *  <pre>
     *  {
     *      status: 200,   // 500, ??????????????????,??????????????????
     *      verifyCode: '???????????????'
     *  }
     *  </pre>
     */

    @RequestMapping(value = "/verifyCode", method = RequestMethod.GET)
    @ResponseBody
    public Map getVerificationCode(
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam(value = "mina", required = false, defaultValue = "false") boolean mina
    ) {
        if (!phoneNumber) {
            return apiResult(SC_INTERNAL_SERVER_ERROR, "?????????????????????")
        }
        if (StringUtils.isMobileNumber(phoneNumber)) {
            return apiResult(SC_INTERNAL_SERVER_ERROR, "????????????????????????")
        }
        if (!mina){
            def user = accountService.getUserByPhoneNumber(phoneNumber)
            if (user) {
                // todo very high ???????????????
                return apiResult(SC_INTERNAL_SERVER_ERROR, "????????????????????????")
            }
        }
        def verifyCodeObj = verifyCodeSrv.getVerifyCode(phoneNumber)
        if (verifyCodeObj) {
            def interval = System.currentTimeMillis() - verifyCodeObj.createTime.getTime()
            log.debug("interval: {}", interval)
            if (interval < CODE_VALIDITY) {
                // todo very high ???????????????
                return apiResult(SC_INTERNAL_SERVER_ERROR, "??????????????????,???????????????")
            }
        }

        verifyCodeSrv.sendVerifyCode(phoneNumber)
        return apiResult()
    }

    /**
     * ???????????????????????????????????????????????????
     * @param phone
     * @param username
     * @return
     */
    @RequestMapping(value = "/verifyCode/query", method = RequestMethod.GET)
    @ResponseBody
    Object getVerifyCode(
            @RequestParam String phone,
            @RequestParam(value = 'username', required = false, defaultValue = '') String username
    ) {
        def verifyCodeObj = username ? verifyCodeSrv.getVerifyCode(phone, username) : verifyCodeSrv.getVerifyCode(phone)

        if (verifyCodeObj) {
            return apiResult([
                    query     : verifyCodeObj.phoneNumber,
                    verifyCode: verifyCodeObj.verifyCode,
                    createTime: DateFormatUtils.format(verifyCodeObj.createTime, 'yyyy-MM-dd HH:mm:ss')
            ])
        } else {
            return apiResult(HttpStatus.SC_NOT_FOUND, '?????????????????????')
        }
    }

    /*
     *
     * ???????????????
     *
     * ???????????????
     *  <pre>
     *  {
     *      status: 200,
     *      sid: 'session id'
     *  }
     *  </pre>
     */

    @RequestMapping(value = "/registry", method = RequestMethod.POST)
    @ResponseBody
    public Map registry(
            HttpServletRequest request,
            @RequestParam("userName") String userName,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam("company") String company,
            @RequestParam("verifyCode") String verifyCode
    ) {
        /* if (env == ENV_ONLINE) {
             return apiResult(SC_INTERNAL_SERVER_ERROR, "???????????????????????????")
         }*/
        if (userName.length() <= 0) {
            return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "?????????????????????")
        }
        if (userName.length() > 30) {
            return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "???????????????")
        }
        if (!StringUtils.isLegitimate(userName)) {
            return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "?????????????????????????????????????????????")
        }
        // todo high ?????????????????????????????????????????????
        def user = accountService.getUserByUserName(userName)
        if (user) {
            // todo very high ???????????????
            return apiResult(SC_INTERNAL_SERVER_ERROR, "?????????????????????")
        }
        if (!password || !confirmPassword) {
            return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "??????????????????")
        }
        if (!StringUtils.securityPwd(password)) {
            return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "??????6-18?????????????????????????????????????????????")
        }
        if (password != confirmPassword) {
            return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "??????????????????????????????")
        }

        if (!phoneNumber) {
            return apiResult(SC_INTERNAL_SERVER_ERROR, "?????????????????????")
        }
        if (StringUtils.isMobileNumber(phoneNumber)) {
            return apiResult(SC_INTERNAL_SERVER_ERROR, "????????????????????????")
        }
        // todo very high ????????????????????????
        user = accountService.getUserByPhoneNumber(phoneNumber)
        if (user) {
            // todo very high ???????????????
            return apiResult(SC_INTERNAL_SERVER_ERROR, "????????????????????????")
        }
        //??????????????????
        if (company) {
            if (StringUtils.isCheckName(company)) {
                if (company.length() < 2 || company.length() > 30) {
                    return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "??????????????????????????????2-30???????????????????????????")
                }
            }
        }
        def verifyCodeObj = verifyCodeSrv.getVerifyCode(phoneNumber)
        if (verifyCodeObj) {
            if (verifyCodeObj.verifyCode != verifyCode) {
                return apiResult(SC_INTERNAL_SERVER_ERROR, "??????????????????")
            }
            def interval = System.currentTimeMillis() - verifyCodeObj.createTime.getTime()
            log.debug("interval: {}, EXPIRY_INTERVAL: {} ", interval, EXPIRY_INTERVAL)
            if (interval > EXPIRY_INTERVAL) {
                // todo very high ???????????????
                return apiResult(SC_INTERNAL_SERVER_ERROR, "??????????????????")
            }
        } else {
            return apiResult(SC_INTERNAL_SERVER_ERROR, "??????????????????")
        }

        // todo ????????????????????????????????????(??????????????????)
//        accountService.register(userName, password, phoneNumber)
        //??????????????????????????????
        Agent agent = agentService.getAgent(request)
        def agentId = agent.id
        accountService.accountRegister(userName, password, phoneNumber, "??????",
                accountService.getDefAppVersion().key, DateUitl.addDay(new Date(), 90), "", agentId, "0", "0", AccountOpenTypeEnum.register.key,true)
        // ???????????????
        verifyCodeSrv.removeVerifyCode(phoneNumber)

        // todo veryhigh ??????????????????
        def newUser = accountService.login(request, userName, password)
        return apiResult([sid: newUser.sid])
    }

    /*
     *
     * ??????
     *
     * ???????????????
     *  <pre>
     *  {
     *      status: 200,
     *      sid: 'session id'
     *  }
     *  </pre>
     */

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    @ResponseBody
    public Map login(
            HttpServletRequest request,
            @RequestParam("userName") String userName,
            @RequestParam("password") String password,
            @RequestParam(value = "key", required = true) String key,
            @RequestParam("verifyCode") String verifyCode,
            @RequestParam(value = "debug", required = false, defaultValue = '0') int debug
    ) {
        //?????????????????????
        if (org.apache.commons.lang3.StringUtils.isBlank(verifyCode) || verifyCode.trim().length() != CAPTCHA_CODE_COUNT) {
            return apiResult(SC_INTERNAL_SERVER_ERROR, "?????????????????????")
        }

        def captcha = captchaSrv.getCaptcha(key)

        if('online'.equals(env) || debug == 0) {        //???????????? ?????? debug????????????????????????????????????????????????
            if (!captcha ) {
                return apiResult(SC_INTERNAL_SERVER_ERROR, "??????????????????????????????")
            }

            if (!(verifyCode.toLowerCase().equals(captcha.verifyCode))) {
                //????????????????????????????????????????????????????????????????????????????????????????????????key??????????????????????????????
                captchaSrv.deleteCaptcha(key)
                return apiResult(SC_INTERNAL_SERVER_ERROR, [
                        msg: "?????????????????????",
                        key: UUID.randomUUID().toString()
                ])
            }
        }

        if(userName.startsWith(Account.ECLOUD_USERNAME_PREFIX)) {
            return apiResult(SC_NOT_FOUND, "??????????????????")
        }

        def user = accountService.login(request, userName, password)
        if (user) {
            if (user.valid) {
                if (user.expDate.getTime() < System.currentTimeMillis()) {
                    return apiResult(SC_FORBIDDEN, "???????????????????????????????????????????????????????????????")
                }else {
                    captchaSrv.deleteCaptcha(key)
                    return apiResult([
                            userId           : user.userId,
                            orgId            : user.orgId,
                            teamId           : user.teamId,
                            sid              : user.sid,
                            userName         : user.userName,
                            phoneNumberTips : user.phoneNumberTips,
                            nickName         : user.nickName,
                            avatar           : user.avatar,
                            isSitesInited    : user.isSitesInited,
                            isRecommandInited: user.isRecommandInited,
                            isCompileInited  : user.isCompileInited,
                            isPwdModified    : user.isPwdModified,
                            roleName         : user.roleName,
                            loginSource      : LoginSourceEnum.userLogin.key
                    ])
                }
            } else {
                return apiResult(SC_NOT_FOUND, "??????????????????")
            }

        } else {
            return apiResult(SC_NOT_FOUND, "??????????????????????????????")
        }
    }

    /*
     *
     * ??????
     *
     * ???????????????
     *  <pre>
     *  {
     *      status: 200,
     *      sid: 'session id'
     *  }
     *  </pre>
     */

    @RequestMapping(value = "/logout", method = RequestMethod.POST)
    @ResponseBody
    public Map logout(
            @CookieValue("sid") String sid,
            HttpServletResponse response,
            @RequestParam(value = "loginSource", required = false, defaultValue = "2") int loginSource
    ) {
        Cookie newCookie = new Cookie("sid", null)
        newCookie.setMaxAge(0) //???????????????
        newCookie.setPath('/')
        response.addCookie(newCookie); //?????????????????????????????????

        accountService.logout(sid, loginSource)
        return apiResult([:])
    }

    /*
     *
     * ??????
     *
     * ???????????????
     *  <pre>
     *  {
     *      status: 200,
     *      sid: 'session id'
     *  }
     *  </pre>
     */

    @RequestMapping(value = "/sidValidation", method = RequestMethod.POST)
    @ResponseBody
    public Map login(@CurrentUserId Long userId) {
        log.debug("{}", userId)
        return apiResult([userId: 1])
    }

    /*
     *
     * ????????????
     *
     * ???????????????
     *  <pre>
     *  {
     *      status: 200,
     *      msg: '????????????'
     *  }
     *  </pre>
     */

    @RequestMapping(value = "/password", method = RequestMethod.PUT)
    @ResponseBody
    public Map modifyPassword(
            @CurrentUserId Long userId,
            @RequestParam("oldPassword") String oldPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword) {
        if (!oldPassword || !newPassword || !confirmPassword) {
            return apiResult(status: 1, msg: "??????????????????")
        } else if (newPassword != confirmPassword) {
            return apiResult(status: 2, msg: "??????????????????????????????")
        }
        if (!StringUtils.securityPwd(newPassword)) {
            return apiResult(status: 5, msg: "??????6-18?????????????????????????????????????????????")
        }
        def isValid = accountService.validatePassword(userId, oldPassword)
        if (!isValid) {
            return apiResult(status: 3, msg: "?????????????????????")
        }

        def result = accountService.modifyPassword(userId, newPassword)
        return result
    }

    @RequestMapping(value = "/passwordStatus", method = RequestMethod.PUT)
    @ResponseBody
    public Map modifyPasswordStatus(
            @CurrentUserId Long userId
    ) {
        def result = accountService.modifyPasswordStatus(userId)
        return result
    }
    /*
    *
    * ??????????????????
    *
    * ???????????????
    *  <pre>
    *  {
    *      status: 200,
                userName: "?????????",
                gender: int, // 1: ???, 2: ???, ?????????0, ??????
                birthdayYear: 1970,   //?????????0
                birthdayMonth: 1,  //?????????0
                birthdayDate: 1,   //?????????0
                company: "????????????",  //??????????????????
                duty: 1 //1. ????????????, 2. ????????????   // ?????????1
    *
    *  }
    *  </pre>
    */

    @RequestMapping(value = "/user/basic", method = RequestMethod.GET)
    @ResponseBody
    public Map getBasicInfo(
            @CurrentUserId Long userId) {
        def result = accountService.getUserById(userId)
        return result
    }

    @RequestMapping(value = "/user/duty", method = RequestMethod.GET)
    public Map getDutyList(
            @CurrentUserId Long userId
    ) {
        def result = accountService.getDutyList()
        return result
    }

    /*
    *
    * ??????????????????
    *
    */

    @RequestMapping(value = "/user/basic", method = RequestMethod.PUT)
    @ResponseBody
    public Map modifyBasicInfo(
            @CurrentUserId Long userId,
            @RequestParam(value = "avatar", required = false, defaultValue = "") String avatar,
            @RequestParam(value = "nickName", required = false, defaultValue = "") String nickName,
            @RequestParam(value = "gender", required = false, defaultValue = "0") int gender,
            @RequestParam(value = "birthday", required = false, defaultValue = "") String birthday,
            @RequestParam(value = "company", required = false, defaultValue = "") String company,
            @RequestParam(value = "duty", required = false, defaultValue = "1") int duty,
            @RequestParam(value = "qqNumber", required = false, defaultValue = "") String qqNumber,
            @RequestParam(value = "weChatNumber", required = false, defaultValue = "") String weChatNumber,
            @RequestParam(value = "email", required = false, defaultValue = "") String email
    ) {
        if (birthday && birthday.length() > 10) {
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: '?????????????????????1990-01-01')
        }
        def result = accountService.modifyUser(userId, avatar, nickName, gender, birthday, company, duty, qqNumber, weChatNumber, email)
        return result
    }

    /*
    *
    * ??????????????????
    *
    * ???????????????
    *  <pre>
    *  {
    *      status: 200,
                userName: "?????????",
                nickName: "??????",
                phoneNumber: "?????????????????????"
    *
    *  }
    *  </pre>
    */

    @RequestMapping(value = "/user/account", method = RequestMethod.GET)
    @ResponseBody
    public Map getAccountInfo(
            @CurrentUserId Long userId) {

        def result = accountService.getUserById(userId)
        return result
    }

    /*
     *
     * ???????????????,?????????????????????
     *
     * ???????????????
     *  <pre>
     *  {
     *      status: 200,   // 500, ??????????????????,??????????????????
     *  }
     *  </pre>
     */

    @RequestMapping(value = "/forgotPassword/verifyCode", method = RequestMethod.GET)
    @ResponseBody
    public Map getVerifyCodeForRetrievePassword(
            @RequestParam("userName") String userName,
            @RequestParam("phoneNumber") String phoneNumber
    ) {
        if (userName.length() <= 0) {
            return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "?????????????????????")
        }
        if (userName.length() > 30) {
            return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "???????????????")
        }
        if (!StringUtils.isLegitimate(userName)) {
            return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "?????????????????????????????????????????????")
        }
        def user = accountService.getUserByUserName(userName)
        if (!user) {
            return apiResult(SC_INTERNAL_SERVER_ERROR, "???????????????")
        }
        if (!phoneNumber) {
            return apiResult(SC_INTERNAL_SERVER_ERROR, "?????????????????????")
        }
        if (StringUtils.isMobileNumber(phoneNumber)) {
            return apiResult(SC_INTERNAL_SERVER_ERROR, "????????????????????????")
        }
        if (user.phoneNumber != phoneNumber) {
            return apiResult(SC_INTERNAL_SERVER_ERROR, "??????????????????????????????")
        }

        def verifyCodeObj = verifyCodeSrv.getVerifyCode(phoneNumber, userName)
        if (verifyCodeObj) {
            def interval = System.currentTimeMillis() - verifyCodeObj.createTime.getTime()
            if (interval < CODE_VALIDITY) {
                return apiResult(SC_INTERNAL_SERVER_ERROR, "??????????????????,???????????????")
            }
        }

        def verifyCode = verifyCodeSrv.generateVerifyCode(phoneNumber, userName)
        verifyCodeSrv.sendVerifyCode(phoneNumber, verifyCode.verifyCode)
        return apiResult()
    }

    /*
     *
     * ????????????????????????,????????????????????????
     *
     * ???????????????
     *  <pre>
     *  {
     *      status: 200,   // 500, ??????????????????,??????????????????
     *  }
     *  </pre>
     */

    @RequestMapping(value = "/forgotPassword/validateVerifyCode", method = RequestMethod.GET)
    @ResponseBody
    public Map resetPasswordForRetrievePassword(
            @RequestParam("userName") String userName,
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam("verifyCode") String verifyCode) {
        if (userName.length() <= 0) {
            return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "?????????????????????")
        }
        if (userName.length() > 30) {
            return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "???????????????")
        }
        if (!StringUtils.isLegitimate(userName)) {
            return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "?????????????????????????????????????????????")
        }
        def user = accountService.getUserByUserName(userName)
        if (!user) {
            return apiResult(SC_INTERNAL_SERVER_ERROR, "???????????????")
        }
        if (!phoneNumber) {
            return apiResult(SC_INTERNAL_SERVER_ERROR, "?????????????????????")
        }
        if (StringUtils.isMobileNumber(phoneNumber)) {
            return apiResult(SC_INTERNAL_SERVER_ERROR, "????????????????????????")
        }
        if (user.phoneNumber != phoneNumber) {
            return apiResult(SC_INTERNAL_SERVER_ERROR, "??????????????????????????????")
        }
        def verifyCodeObj = verifyCodeSrv.getVerifyCode(phoneNumber, userName)


        if (verifyCodeObj) {
            if (verifyCodeObj.verifyCode != verifyCode) {
                return apiResult(SC_INTERNAL_SERVER_ERROR, "??????????????????")
            }

            def interval = System.currentTimeMillis() - verifyCodeObj.createTime.getTime()
            if (interval > EXPIRY_INTERVAL) {
                return apiResult(SC_INTERNAL_SERVER_ERROR, "??????????????????")
            }
        } else {
            return apiResult(SC_INTERNAL_SERVER_ERROR, "??????????????????")
        }

        return apiResult()
    }

    /*
     *
     * ????????????,?????????
     *
     * ???????????????
     *  <pre>
     *  {
     *      status: 200,   // 500, ??????????????????,??????????????????
     *  }
     *  </pre>
     */

    @RequestMapping(value = "/forgotPassword/resetPassword", method = RequestMethod.PUT)
    @ResponseBody
    public Map resetPasswordForRetrievePassword(
            HttpServletRequest request,
            @RequestParam("userName") String userName,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam("verifyCode") String verifyCode
    ) {
        if (userName.length() <= 0) {
            return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "?????????????????????")
        }
        if (userName.length() > 30) {
            return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "???????????????")
        }
        if (!StringUtils.isLegitimate(userName)) {
            return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "?????????????????????????????????????????????")
        }
        if (!password || !confirmPassword) {
            return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "??????????????????")
        }
        if (!StringUtils.securityPwd(password)) {
            return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "??????6-18?????????????????????????????????????????????")
        }
        if (password != confirmPassword) {
            return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "??????????????????????????????")
        }

        if (!phoneNumber) {
            return apiResult(SC_INTERNAL_SERVER_ERROR, "?????????????????????")
        }
        if (StringUtils.isMobileNumber(phoneNumber)) {
            return apiResult(SC_INTERNAL_SERVER_ERROR, "????????????????????????")
        }
        // ???????????????
        def verifyCodeObj = verifyCodeSrv.getVerifyCode(phoneNumber, userName)
        if (verifyCodeObj) {
            if (verifyCodeObj.verifyCode != verifyCode) {
                return apiResult(SC_INTERNAL_SERVER_ERROR, "??????????????????")
            }

            def interval = System.currentTimeMillis() - verifyCodeObj.createTime.getTime()
            if (interval > EXPIRY_INTERVAL) {
                return apiResult(SC_INTERNAL_SERVER_ERROR, "??????????????????")
            }
        } else {
            return apiResult(SC_INTERNAL_SERVER_ERROR, "??????????????????")
        }

        // ????????????
        def user = accountService.getUserByUserName(userName)
        accountService.modifyPassword(user.userId, password)

        // ???????????????
        verifyCodeSrv.removeVerifyCode(phoneNumber, userName)

        // todo veryhigh ??????????????????
        // ??????
        def newUser = accountService.login(request, userName, password)
        return apiResult([sid: newUser.sid])
    }

    @RequestMapping(value = "/apply", method = RequestMethod.POST)
    @ResponseBody
    public Map saveApplyInfo(
            @RequestParam(value = "name", required = false, defaultValue = "") String name,
            @RequestParam(value = "company", required = false, defaultValue = "") String company,
            @RequestParam(value = "position", required = false, defaultValue = "") String position,
            @RequestParam(value = "mobile", required = false, defaultValue = "") String mobile,
            @RequestParam(value = "email", required = false, defaultValue = "") String email,
            @RequestParam(value = "qq", required = false, defaultValue = "") String qq,
            @RequestParam(value = "message", required = false, defaultValue = "") String message
    ) {
        if (name.equals("") || company.equals("") || mobile.equals("")) {
            return apiResult(SC_INTERNAL_SERVER_ERROR, "??????????????????")
        }
        def result = accountService.saveApplyInfo(name, company, position,
                mobile, email, qq, message)
        return apiResult([result: result ? "success" : "fail"])
    }

    /**
     * ???????????????????????????
     * @param userId
     * @param classification
     * @param area
     * @param duty
     * @return
     */
    @RequestMapping(value = "/initialization", method = [RequestMethod.GET, RequestMethod.POST])
    @ResponseBody
    @NotCheckExpiry
    public Map initializeRecommandData(
            @CurrentUserId Long userId,
            @RequestParam(value = "classification", required = false) String classification,
            @RequestParam(value = "area", required = false, defaultValue = "") String area,
            @RequestParam(value = "duty", required = false, defaultValue = "1") int duty,
            @RequestParam(value = "subjectFlag", required = false, defaultValue = "0") int subjectFlag
    ) {
        String[] classifications = []
        if (classification) {
            classification = URLDecoder.decode(classification,"utf-8")
            classifications = classification.split(",")
        }
        def result = accountService.initializeRecommandData(userId, classifications, area, duty, subjectFlag)
        return result
    }

    /**
     * ???????????? ??????
     * @param userId
     * @return
     */
    @RequestMapping(value = "recommand/initialization", method = RequestMethod.POST)
    @ResponseBody
    @NotCheckExpiry
    public Map modifyRecommandInited(
            @CurrentUserId Long userId
    ) {
        def result = accountService.modifyRecommandInited(userId)
        return result
    }

    /**
     * ???????????? ??????
     * @param userId
     * @return
     */
    @RequestMapping(value = "compile/initialization", method = RequestMethod.POST)
    @ResponseBody
    @NotCheckExpiry
    public Map modifyCompileInited(
            @CurrentUserId Long userId
    ) {
        def result = accountService.modifyCompileInited(userId)
        return result
    }

    @CheckPrivilege(privileges = [Privilege.TEAM_MANAGE])
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    public Map getAgentAccountList(
            @CurrentUser LoginUser user
    ) {
        def result = accountService.getAccountList(user)
        return apiResult([list: result])
    }

    @CheckPrivilege(privileges = [Privilege.ACCOUNT_MANAGE])
    @RequestMapping(value = "/orgAccount", method = RequestMethod.POST)
    @ResponseBody
    public Map addOrgAccount(
            @CurrentUser LoginUser user,
            @RequestParam(value = "userName", required = true) String userName,
            @RequestParam(value = "password", required = true) String password,
            @RequestParam(value = "realName", required = false, defaultValue = "") String realName,
            @RequestParam(value = "phoneNumber", required = false, defaultValue = "") String phoneNumber,
            @RequestParam(value = "teamId", required = false, defaultValue = "0") String teamId
    ) {
        if (userName.length() <= 0) {
            return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "?????????????????????")
        }
        if (userName.length() > 20) {
            return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "???????????????")
        }
        if (!StringUtils.isLegitimate(userName)) {
            return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "?????????????????????????????????????????????")
        }
        def isAccountExist = accountService.accountIsExist(userName)
        if (isAccountExist) {
            return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "??????????????????");
        }
        if (!password) {
            return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "??????????????????")
        }
        if (!StringUtils.securityPwd(password)) {
            return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "??????6-18?????????????????????????????????????????????")
        }
        if (!phoneNumber) {
            return apiResult(SC_INTERNAL_SERVER_ERROR, "?????????????????????")
        }
        if (StringUtils.isMobileNumber(phoneNumber)) {
            return apiResult(SC_INTERNAL_SERVER_ERROR, "????????????????????????")
        }
        if (phoneNumber) {
            def isAccountExist2 = accountService.phoneNumberIsExist(phoneNumber)
            if (isAccountExist2) {
                return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "??????????????????????????????");
            }
        }
        def isOverstepOrgUserCount = accountService.isOverstepOrgUserCount(user.agentId, user.orgId)
        if (isOverstepOrgUserCount) {
            return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "???????????????????????????");
        }

        def loginUser = accountService.getUserInfoById(user.userId)
        def salesId = loginUser.salesId
        if (!salesId) {
            salesId = 0l
        }
        def productType = loginUser.getProductType()
        if (!productType){
            productType = accountService.getDefAppVersion().key
        }
        def addAccountResult = accountService.addOrgUser(loginUser.id,userName, password, phoneNumber, loginUser.getUserType(),
                productType, loginUser.getExpDate(), realName, loginUser.getAgentId(), loginUser.getOrgId(), teamId, salesId, loginUser.getCompany() ?: "")
        if (addAccountResult) {
            apiResult(HttpStatus.SC_OK, "????????????")
        } else {
            apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "????????????")
        }
    }

    @CheckPrivilege(privileges = [Privilege.ACCOUNT_MANAGE])
    @RequestMapping(value = "/orgAccount", method = RequestMethod.GET)
    @ResponseBody
    public Map getOrgAccount(
            @CurrentUser LoginUser user,
            @RequestParam(value = "userId", required = true) long userId
    ) {
        if (user.getUserId() == userId) {
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "????????????????????????")
        }
        def result = accountService.getOrgUserById(userId)
        if (result) {
            return apiResult(status: HttpStatus.SC_OK, msg: result)
        } else {
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "????????????????????????")
        }
    }

    @CheckPrivilege(privileges = [Privilege.ACCOUNT_MANAGE])
    @RequestMapping(value = "/orgAccount", method = RequestMethod.PUT)
    @ResponseBody
    public Map updateOrgAccount(
            @CurrentUser LoginUser user,
            @RequestParam(value = "userId", required = true) long userId,
            @RequestParam(value = "userName", required = true) String userName,
            @RequestParam(value = "password", required = true) String password,
            @RequestParam(value = "realName", required = false, defaultValue = "") String realName,
            @RequestParam(value = "phoneNumber", required = true) String phoneNumber,
            @RequestParam(value = "teamId", required = false, defaultValue = "0") String teamId
    ) {
        if (user.getUserId() == userId) {
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "????????????????????????")
        }
        if (userName.length() <= 0) {
            return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "?????????????????????")
        }
        if (userName.length() > 20) {
            return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "???????????????")
        }
        if (!StringUtils.isLegitimate(userName)) {
            return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "?????????????????????????????????????????????")
        }
        if (!password) {
            return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "??????????????????")
        }
        if (!StringUtils.securityPwd(password)) {
            return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "??????6-18?????????????????????????????????????????????")
        }
        if (!phoneNumber) {
            return apiResult(SC_INTERNAL_SERVER_ERROR, "?????????????????????")
        }
        if (StringUtils.isMobileNumber(phoneNumber)) {
            return apiResult(SC_INTERNAL_SERVER_ERROR, "????????????????????????")
        }
        if (phoneNumber) {
            def isAccountExist2 = accountService.phoneNumberIsExist(phoneNumber)
            if (isAccountExist2 != null && !isAccountExist2.get("userId").equals(userId)) {
                return apiResult(HttpStatus.SC_INTERNAL_SERVER_ERROR, "??????????????????????????????");
            }
        }
        def result = accountService.updateOrgUser(userId, userName, password, realName, phoneNumber, teamId)
        if (result) {
            return apiResult(status: HttpStatus.SC_OK, msg: "????????????")
        } else {
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "????????????")
        }
    }

    @CheckPrivilege(privileges = [Privilege.ACCOUNT_MANAGE])
    @RequestMapping(value = "/orgAccount/{userId}", method = RequestMethod.DELETE)
    @ResponseBody
    public Map deleteOrgAccount(
            @CurrentUser LoginUser user,
            @PathVariable long userId
    ) {
        if (user.getUserId() == userId) {
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "????????????????????????")
        }
        def result = accountService.deleteOrgUser(userId)
        if (result) {
            return apiResult(status: HttpStatus.SC_OK, msg: "????????????")
        } else {
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "????????????")
        }
    }

    @CheckPrivilege(privileges = [Privilege.ACCOUNT_MANAGE])
    @RequestMapping(value = "/orgUserTeam", method = RequestMethod.PUT)
    @ResponseBody
    public Map modifyOrgUserTeam(
            @CurrentUser LoginUser user,
            @RequestParam(value = "userIds", required = true) String userIds,
            @RequestParam(value = "teamId", required = true) String teamId
    ) {
        def result = accountService.modifyOrgUserTeam(userIds, teamId)
        if (result) {
            return apiResult(status: HttpStatus.SC_OK, msg: "????????????")
        } else {
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "????????????")
        }
    }

    @ResponseBody
    @RequestMapping(value = "/captcha", method = RequestMethod.GET)
    public Object captcha(
            @RequestParam(value = "w", required = false, defaultValue = "100") int width,
            @RequestParam(value = "h", required = false, defaultValue = "38") int height,
            @RequestParam(value = "k", required = true) String key,
            @RequestParam(value = "ua", required = true) String ua
    ) {

        //???????????????????????????????????????????????????????????????????????????
        LineCaptcha captcha = new LineCaptcha(width, height, CAPTCHA_CODE_COUNT, 20);
        String img = "data:image/png;base64," + captcha.getImageBase64()

        captchaSrv.createVerifyCode(key, captcha.code, ua)

        return apiResult([img: img])
    }

    @ResponseBody
    @RequestMapping(value = "/login/status", method = RequestMethod.GET)
    public Object loginStatus(
            HttpServletRequest request
    ) {

        boolean loginStatus = false

        def sid = request.getCookies()?.find { it.name == 'sid' }
        if(sid?.value) {
            def loginUser = accountService.getUserBySessionId(sid.value)
            if(loginUser?.enable) {
                def user = accountService.getUserInfoById(loginUser.userId)
                if(user.valid && user.expDate.getTime() > System.currentTimeMillis()) {
                    loginStatus = true
                }
            }
        }

        return loginStatus ? apiResult() : apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "??????????????????")
    }

    /**
     * ??????????????????
     * @param userId
     * @param phoneNumber
     * @param verifyCode
     * @return
     */
    @RequestMapping(value = "/phoneNumber", method = PUT)
    @ResponseBody
    Map modifyPhoneNumber(
            @CurrentUserId Long userId,
            @RequestParam(value = "phoneNumber", required = true) String phoneNumber,
            @RequestParam(value = "verifyCode", required = true) String verifyCode
    ) {
        try {
            def verifyCodeObj = verifyCodeSrv.getVerifyCode(phoneNumber)
            if (verifyCodeObj) {
                if (verifyCodeObj.verifyCode != verifyCode) {
                    return apiResult(SC_INTERNAL_SERVER_ERROR, "??????????????????")
                }
                def interval = System.currentTimeMillis() - verifyCodeObj.createTime.getTime()
                log.debug("interval: {}, EXPIRY_INTERVAL: {} ", interval, EXPIRY_INTERVAL)
                if (interval > EXPIRY_INTERVAL) {
                    // todo very high ???????????????
                    return apiResult(SC_INTERNAL_SERVER_ERROR, "??????????????????")
                }
            } else {
                return apiResult(SC_INTERNAL_SERVER_ERROR, "??????????????????")
            }
            return accountService.modifyPhoneNumber(userId, phoneNumber)
        } catch (Exception e) {
            return apiResult([status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: '????????????'])
        }
    }
}
