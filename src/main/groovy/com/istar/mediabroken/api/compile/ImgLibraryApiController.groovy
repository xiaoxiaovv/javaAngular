package com.istar.mediabroken.api.compile

import com.istar.mediabroken.api.CurrentUser
import com.istar.mediabroken.api.CurrentUserId
import com.istar.mediabroken.entity.LoginUser
import com.istar.mediabroken.service.account.AccountService
import com.istar.mediabroken.service.account.UserRoleService
import com.istar.mediabroken.service.compile.ImgLibraryService
import groovy.util.logging.Slf4j
import org.apache.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import static com.istar.mediabroken.api.ApiResult.apiResult
import static org.springframework.web.bind.annotation.RequestMethod.*

/**
 * 素材管理相关
 * Author: zc
 * Time: 20180704
 */
@RestController
@Slf4j
@RequestMapping(value = "/api/compile")
class ImgLibraryApiController {
    @Autowired
    ImgLibraryService imgLibraryService
    @Autowired
    AccountService accountService
    @Autowired
    UserRoleService userRoleService

    /**
     * 查询用户素材列表
     * @param userId
     * @param mina
     * @param imgGroupId
     * @param pageNo
     * @param pageSize
     * @return
     */
    @RequestMapping(value = "/imgLibrary/imgs", method = GET)
    public Map getUserImgs(
            @CurrentUserId long userId,
            @RequestParam(value = "mina", required = false, defaultValue = "false") boolean mina,
            @RequestParam(value = "imgGroupId", required = false, defaultValue = "0") String imgGroupId,
            @RequestParam(value = "pageNo", required = false, defaultValue = "1") int pageNo,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
            @RequestParam(value = "type", required = false, defaultValue = "img") String type
    ) {
        List result = imgLibraryService.getUserImgs(userId, mina, imgGroupId, pageNo, pageSize, type)
        return apiResult([status: HttpStatus.SC_OK, list: result])
    }

    /**
     * 获取视频转码状态
     * @param userId
     * @param ids
     * @return
     */
    @RequestMapping(value = "/imgLibrary/state", method = GET)
    public Map getUserMaterialState(
            @CurrentUserId long userId,
            @RequestParam(value = "ids", required = false) String ids
    ) {
        List result = imgLibraryService.getMaterialState(userId, ids)
        return apiResult([status: HttpStatus.SC_OK, list: result])
    }

    /**
     * 上传素材保存
     * @param userId
     * @param imgUrl
     * @param mina
     * @return
     */
    @RequestMapping(value = "imgLibrary/img", method = POST)
    public Map addUserImg(
            @CurrentUserId Long userId,
            @RequestParam(value = "imgUrl", required = true) String imgUrl,
            @RequestParam(value = "mina", required = false, defaultValue = "false") boolean mina
    ) {
        def result = imgLibraryService.addUserImg(userId, imgUrl, mina)
        if (result){
            return apiResult([status: HttpStatus.SC_OK, msg: "上传素材成功"])
        }else {
            return apiResult([status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "上传素材失败"])
        }
    }

    /**
     * 移除素材，并将服务器端保存的图片移除
     * @param userId
     * @param imgId
     * @return
     */
    @RequestMapping(value = "materialLibrary/material/{id}", method = DELETE)
    public Map removeUserImg(
            @CurrentUser LoginUser user,
            @PathVariable("id") String id
    ){
        boolean flag = checkUserRole(user)
        boolean result = false
        if(flag) {
            result = imgLibraryService.removeUserMaterial(user.getUserId(), id)
        } else {
            result = imgLibraryService.removeUserImg(user.getUserId(), id)
        }
        if (result) {
            return apiResult([status: HttpStatus.SC_OK, msg: "移除素材成功"])
        } else {
            return apiResult([status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: "移除素材失败"])
        }
    }

    private boolean checkUserRole(LoginUser user) {
        boolean check = false
        def role = accountService.getAccountRole(user.getUserId())
        def name = userRoleService.getRoleByAgentIdAndRoleName(user.getAgentId(), "全媒体机构用户")
        def nameMag = userRoleService.getRoleByAgentIdAndRoleName(user.getAgentId(), "全媒体机构管理员")
        String roleId = name.getId()
        String roleIdMag = nameMag.getId()
        if (org.apache.commons.lang3.StringUtils.equals(role.roleId, roleId) || org.apache.commons.lang3.StringUtils.equals(role.roleId, roleIdMag)) {
            check = true
        }
        return check
    }
}
