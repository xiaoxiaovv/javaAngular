package com.istar.mediabroken.entity.compile

class ImgLibrary {
    String id
    Long userId
    String imgGroupId
    String imgUrl
    boolean mina
    String videoSourceUrl
    String videoPublishUrl
    String videoPreviewUrl
    String cutFrameImgUrl
    String videoState
    String type
    Date updateTime
    Date createTime

    Map<String, Object> toMap() {
        return [
                _id       : id ?: UUID.randomUUID().toString(),
                userId    : userId,
                imgGroupId: imgGroupId ?: "0",
                imgUrl    : imgUrl,
                mina      : mina,
                videoSourceUrl :videoSourceUrl,
                videoPublishUrl: videoPublishUrl,
                videoPreviewUrl: videoPreviewUrl,
                cutFrameImgUrl: cutFrameImgUrl,
                videoState: videoState,
                type: type,
                updateTime: updateTime,
                createTime: createTime
        ]
    }

    ImgLibrary() {
        super
    }

    ImgLibrary(Map map) {
        id = map._id
        userId = map.userId
        imgGroupId = map.imgGroupId ?: "0"
        imgUrl = map.imgUrl
        mina = map.mina ?: false
        videoSourceUrl = map.videoSourceUrl
        videoPublishUrl = map.videoPublishUrl
        videoPreviewUrl = map.videoPreviewUrl
        cutFrameImgUrl = map.cutFrameImgUrl
        videoState = map.videoState
        type = map.type
        updateTime = map.updateTime
        createTime = map.createTime
    }

    @Override
    public String toString() {
        return "ImgLibrary{" +
                "id='" + id + '\'' +
                ", userId=" + userId +
                ", imgGroupId='" + imgGroupId + '\'' +
                ", imgUrl='" + imgUrl + '\'' +
                ", mina=" + mina +
                ", videoSourceUrl=" + videoSourceUrl +
                ", videoPublicUrl=" + videoPublishUrl +
                ", videoPreviewUrl=" + videoPreviewUrl +
                ", cutFrameImgUrl=" + cutFrameImgUrl +
                ", videoState=" + videoState +
                ", type=" + type +
                ", updateTime=" + updateTime +
                ", createTime=" + createTime +
                '}'
    }
}
