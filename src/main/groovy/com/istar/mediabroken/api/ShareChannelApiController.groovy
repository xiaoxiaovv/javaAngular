package com.istar.mediabroken.api

import com.alibaba.fastjson.JSONObject
import com.istar.mediabroken.entity.LoginUser
import com.istar.mediabroken.entity.account.Privilege
import com.istar.mediabroken.service.ShareChannelService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

import javax.servlet.http.HttpServletRequest

import static com.istar.mediabroken.api.ApiResult.apiResult

/**
 * Author : YCSnail
 * Date   : 2017-03-30
 * Email  : liyancai1986@163.com
 */
@RestController
@Slf4j
@RequestMapping(value = '/api/share')
class ShareChannelApiController {

    @Autowired
    ShareChannelService channelSrv

    /*
    *
    * 获取所有已绑定的分享渠道
    *
    * 服务器返回
    *  <pre>
    *  {
    *      list: [
                    id          : id,
                    channelType : $int,       // 1: 新浪微博  2: 微信  3: 头条号
                    name        : '账号名称',
                    uid         : '绑定账号的uid',
                    userId      : '本系统用户ID'
    *      ]
    *  }
    *  </pre>
    */

    @CheckPrivilege(privileges = [Privilege.SHARE_CHANNEL])
    @RequestMapping(value = "/channels", method = RequestMethod.GET)
    Object channelList(
            @CurrentUserId Long userId
    ) {
        def list = channelSrv.getShareChannels(userId)

        return apiResult([
                list: list
        ])
    }

    /*
    * 取消新浪微博账号授权
    */

    @CheckPrivilege(privileges = [Privilege.SHARE_CHANNEL])
    @RequestMapping(value = "/channel/{channelId}", method = RequestMethod.DELETE)
    Object delChannel(
            HttpServletRequest request,
            @CurrentUserId Long userId,
            @PathVariable Long channelId
    ) {
        return apiResult([result: channelSrv.delShareChannel(request, userId, channelId)])
    }

    /**
     * 多渠道批量同步
     * @param userId
     * @param content
     * @param channelIds
     * @param wechatSyncType 0-发送到素材 1-直接群发
     * @return{
     detail       : [
     status  : 0,        //1-成功  0-失败
     msg     : '描述',
     code    : $int      //状态码
     ]
     }
     */
    @CheckPrivilege(privileges = [Privilege.MATERIAL_SHARE])
    @RequestMapping(value = "/material/{materialId}", method = RequestMethod.POST)
    @CheckExpiry
    Object syncMaterial(
            HttpServletRequest request,
            @CurrentUser LoginUser user,
            @PathVariable String materialId,
            @RequestParam String channelIds,
            @RequestParam String timeStamp,
            @RequestParam int wechatSyncType
    ) {
        def ids = []
        channelIds?.split(',')?.each {
            ids << Long.valueOf(it)
        }
        def result = channelSrv.shareMaterialAndAddHistory(request, user.userId, user.agentId, user.orgId, user.teamId, materialId, ids, timeStamp, wechatSyncType)
        return result
    }

    @CheckPrivilege(privileges = [Privilege.TEAM_MANAGE])
    @RequestMapping(value = "/channels", method = RequestMethod.POST)
    Object getChannelList(
            @CurrentUserId Long userId,
            @RequestBody String data
    ) {
        JSONObject jsonObj = JSONObject.parse(data)
        def userIds = jsonObj.get("userIds").toString().split(",")
        def channels = [:]
        userIds.each { it ->
            def list = channelSrv.getShareChannels(Long.parseLong(it))
            channels.put(""+it, list)
        }
        return apiResult([list: channels])
    }

}
