package com.istar.mediabroken.service.account

import com.istar.mediabroken.entity.account.AccountVsRole
import com.istar.mediabroken.entity.account.Role
import com.istar.mediabroken.entity.account.UserRole
import com.istar.mediabroken.repo.account.AccountVsRoleRepo
import com.istar.mediabroken.repo.account.RoleRepo
import org.apache.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import static com.istar.mediabroken.api.ApiResult.apiResult

@Service
class UserRoleService {
    @Autowired
    RoleRepo roleRepo
    @Autowired
    AccountVsRoleRepo accountVsRoleRepo

    Map addUserRole(long userId, String roleId, String updateUser, String createUser, Date updateTime, Date createTime) {
        def userRole = new UserRole(
                _id: UUID.randomUUID().toString(),
                userId: userId,
                roleId: roleId,
                updateUser: updateUser,
                createUser: createUser,
                updateTime: updateTime,
                createTime: createTime
        )
        boolean result = roleRepo.addUserRole(userRole)
        if (result) {
            return apiResult(status: HttpStatus.SC_OK, msg: '添加角色信息成功')
        } else {
            return apiResult(status: HttpStatus.SC_INTERNAL_SERVER_ERROR, msg: '添加角色信息失败')
        }
    }

    Role getRoleByAgentIdAndRoleName(String agentId, String roleName) {
        return roleRepo.getRoleByAgentIdAndRoleName(agentId, roleName)
    }

    String getRoleNameById(Long userId) {
        AccountVsRole accountVsRole = accountVsRoleRepo.getRoleIdByUserId(userId)
        //如果accountVsRole为空则为历史帐号 默认为 企业用户
        Role role = roleRepo.getRoleByRoleId(accountVsRole?accountVsRole.roleId:"3")
        return role.roleName
    }

    Role getRoleById(Long userId) {
        AccountVsRole accountVsRole = accountVsRoleRepo.getRoleIdByUserId(userId)
        //如果accountVsRole为空则为历史帐号 默认为 机构用户
        Role role = roleRepo.getRoleByRoleId(accountVsRole?accountVsRole.roleId:"3")
        return role
    }
}
