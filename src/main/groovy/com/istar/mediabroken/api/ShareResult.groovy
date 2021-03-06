package com.istar.mediabroken.api

/**
 * Author : YCSnail
 * Date   : 2017-06-26
 * Email  : liyancai1986@163.com
 */
class ShareResult<T> {

    public static final int success = 1
    public static final int failure = 0

    static Map shareSuccess() {
        return [
                status  : success,
                msg     : '同步成功！'
        ]
    }
    static Map shareSuccess(String msg) {
        return [
                status  : success,
                msg     : msg
        ]
    }
    static Map getUploadInfoFailure() {
        return [
                status  : failure,
                msg     : '获取上传信息失败！',
                code    : 0
        ]
    }
    static Map shareFailure() {
        return [
                status  : failure,
                msg     : '同步失败！',
                code    : 0
        ]
    }
    static Map shareFailure(String msg, int code) {
        return [
                status  : failure,
                msg     : msg,
                code    : code
        ]
    }
    static Map shareFailure(T errorEnum) {
        return [
                status  : failure,
                msg     : errorEnum.msg,
                code    : errorEnum.code
        ]
    }

}

enum WeiboErrorEnum {

    ERROR_REPEAT_CONTENT (20019, '提交相同的信息！'),
    ERROR_ACCOUNT_IS_LOCKED (20034, '微博账号被锁定！')

    private int code
    private String msg

    WeiboErrorEnum(int code, String msg) {
        this.code = code
        this.msg = msg
    }

    static String getErrorMsg(int code) {
        WeiboErrorEnum errorEnum = WeiboErrorEnum.find { it.code == code }
        return errorEnum ? errorEnum.msg : '同步失败！'
    }
}

enum WechatErrorEnum {

    ERROR_INVALID_MEDIA_ID(40007, '封面图片不存在！'),
    IMG_SIZE_OUT_OF_LIMIT(40009, '图片大小不能超过2M！'),
    INVALID_USERNAME(40132, '无效的微信号！'),
    REQUIRE_SUBSCRIBE(43004, '请先订阅公众号！'),
    CONTENT_SIZE_OUT_OF_LIMIT(45002, '文章内容信息超过限制！'),
    ERROR_DIGEST_IS_LONG(45004, '文章摘要信息超过限制！'),
    HAS_NO_MASS_SEND_QUOTA(45028, '公众号群发配额已不足！'),
    ERROR_CLIENT_MSG_ID_EXIST(45065, '文章不能重复发送！'),
    ERROR_MP_API_UNAUTHORIZED(48001, '公众号接口功能未授权！'),
    USER_NOT_AGREE_MASS_SEND_PROTOCOL(48003, '公众号还未开启群发权限！')

    private int code
    private String msg

    WechatErrorEnum(int code, String msg) {
        this.code = code
        this.msg = msg
    }

    static String getErrorMsg(int code) {
        WechatErrorEnum errorEnum = WechatErrorEnum.find { it.code == code }
        return errorEnum ? errorEnum.msg : '同步失败！'
    }
}

enum QqomErrorEnum {

    CLIENT_SECRET_IS_VALID(40017, '授权secret无效！'),
    API_REQUEST_FREQUENCY_MAX(46001, '接口调用频率超过限制！')

    private int code
    private String msg

    QqomErrorEnum(int code, String msg) {
        this.code = code
        this.msg = msg
    }

    static String getErrorMsg(int code) {
        QqomErrorEnum errorEnum = QqomErrorEnum.find { it.code == code }
        return errorEnum ? errorEnum.msg : '同步失败！'
    }
}
