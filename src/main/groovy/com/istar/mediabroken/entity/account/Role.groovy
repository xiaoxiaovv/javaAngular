package com.istar.mediabroken.entity.account

/**
 * Author: Luda
 * Time: 2017/7/24
 */
class Role {

    public static final String ROLE_BJJ_ORG_ADMIN = '1'
    public static final String ROLE_BJJ_ORG_USER = '2'

    String id
    String name
    String description
    String updateUser
    String createUser
    String roleName
    String desc
    String agentId

    String subRoleName //相关的机构类用户

    Date updateTime
    Date createTime

    Map<String, Object> toMap() {
        return [
                _id        : id,
                name       : name,
                description: description,
                updateUser : updateUser,
                createUser : createUser,
                roleName   : roleName,//new
                desc       : desc,//new
                agentId    : agentId,//new
                subRoleName: subRoleName,
                updateTime : updateTime,
                createTime : createTime,
        ]
    }

    Role() {
        super
    }

    Role(Map map) {
        id = map._id
        name = map.name
        description = map.description
        updateUser = map.updateUser
        createUser = map.createUser
        roleName   = map.roleName
        desc       = map.desc
        agentId    = map.agentId
        subRoleName = map.subRoleName
        updateTime = map.updateTime
        createTime = map.createTime
    }

    @Override
    public String toString() {
        return "Role{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", updateUser='" + updateUser + '\'' +
                ", createUser='" + createUser + '\'' +
                ", roleName='" + roleName + '\'' +
                ", desc='" + desc + '\'' +
                ", agentId='" + agentId + '\'' +
                ", updateTime=" + updateTime +
                ", createTime=" + createTime +
                '}';
    }
}
