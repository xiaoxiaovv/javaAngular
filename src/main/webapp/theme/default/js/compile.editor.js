/**
 * Author : YCSnail
 * Date : 2017-07-27
 * Email : liyancai1986@163.com
 */
var editorCtrl = function ($rootScope, $scope, $http, $state, $stateParams, $interval, $sce) {
    if($.cookie('isCompileInited') == 'false') {
        $scope.isBriefBed = true;
    }
    // init
    var _MATERIAL_AUTO_SAVE_KEY = encodeURIComponent(($.cookie('userName')+'__material__temp').toLocaleUpperCase());
    console.log(_MATERIAL_AUTO_SAVE_KEY)
    var _editorContainerId = 'editor';
    _defaultCover = '/theme/default/images/default-cover.jpg';
    $rootScope.minBar = true;
    $scope.navActive = 1;
    $scope.saveBtnStatus = 'normal';
    $scope.syncBtnStatus = 'normal';
    $scope.pushBtnStatus = 'normal';
    $scope.textImgBtnStatus = 'normal';
    $scope.showVideoDenger = false;
    $scope.tagIds = '';
    $scope.tagShow = false;
    $scope.LebelList = [];
    $scope.location = [];
    $scope.comitLebelList = [];
    $scope.reportCreateBtn = true;
    $scope.reportProgressBar = false;
    $scope.editorAreaStatus = true;
    $scope.reportResultMsg = false;
    $scope.templateShow = false;
    $scope.firstApply = true;
    $scope.secondApply = false;
    $scope.thirdApply = false;
    $scope.firstTemplate = true;
    $scope.secondTemplate = false;
    $scope.thirdTemplate = false;
    $scope.reportCreateCount = 0;
    $scope.wechatSyncType = 0;
    $scope.material = {picUrl: _defaultCover};
    $scope.progress = {value: 0, reportStatus: 1, message: ''};
    $scope.style = {};
    $scope.imgLib = {};
    $scope.sortableSource = ['recommendNewsListCont'];
    $scope.selectedNewsList = [];
    $scope.echartsNameList = ['来源分析', '传播走势图', '网站传播走势图', '媒体排行', '社交媒体传播走势图', '网民声量地域分布', '网民声量地域排行', '微博意见领袖'];
    $scope.echartsTypeList = ['sourceDistribution', 'spreadTrend', 'siteSpreadTrend', 'siteRank', 'bloggerSpreadTrend', 'bloggerDistribution', 'bloggerRank', 'weiboRank'];
    $scope.channelMap = {
        1 : {name_zh: '新浪微博', name_en: 'weibo'},
        2 : {name_zh: '微信公众号', name_en: 'wechat'},
        3 : {name_zh: '头条号', name_en: 'toutiao'},
        4 : {name_zh: '企鹅媒体平台', name_en: 'qqom'}
    };
    $scope.groupPageInfoMap = {};
    $scope.editorHeight = $("#"+_editorContainerId).height();
    $scope.detailModal = false;
    $scope.index = 0;
    $scope.fontFamily = [
        { name: '微软雅黑', val: '微软雅黑,Microsoft YaHei'},
        { name: '宋体', val: '宋体,SimSun'},
        { name: '楷体', val: '楷体,楷体_GB2312, SimKai'},
        { name: '黑体', val: '黑体, SimHei'},
        { name: '隶书', val: '隶书, SimLi'},
        { name: 'andale mono', val: 'andale mono'},
        { name: 'arial', val: 'arial, helvetica,sans-serif'},
        { name: 'arial black', val: 'arial black,avant garde'},
        { name: 'comic sans ms', val: 'comic sans ms'},
        { name: 'impact', val: 'impact,chicago'},
        { name: 'times new roman', val: 'times new roman'},
        { name: 'sans-serif',val:'sans-serif'}
    ]
    $scope.templateDefault = {
        bodyAlignment    : '左对齐',
        bodyImgAlignment : '左对齐',
        bodyFontFamily   : '微软雅黑,Microsoft YaHei'
    };
    $scope.customOne = {
        bodyAlignment    : '左对齐',
        bodyImgAlignment : '居中对齐',
        templateName     : '自定义1',
        bodyFontFamily   : '微软雅黑,Microsoft YaHei'
    };
    $scope.customTwo = {
        bodyAlignment    : '左对齐',
        bodyImgAlignment : '居中对齐',
        templateName     : '自定义2',
        bodyFontFamily   : '微软雅黑,Microsoft YaHei'
    };
    $scope.newsIds = [];
    $scope.typeUpload = 'img';
    $scope.$on('$stateChangeStart', function(event) {
        _.forEach($rootScope.shipinList, function(v, i) {
            if(v.videoPreviewUrl == '') {
                if($rootScope.uploadStatus = '上传中...') {
                    var answer = confirm("内容正在上传，离开本页可能中断上传，确认离开？");
                    if (!answer) {
                        event.preventDefault();
                    }
                }
            }
        })
    });
    UM.delEditor(_editorContainerId);
    var editor = UM.getEditor(_editorContainerId, {
        initialFrameWidth : '100%',
        initialFrameHeight: 450
    });
    editor.ready(function () {
        var newsId = $stateParams.newsId;
        var materialId = $stateParams.materialId;

        var getNewsOrMaterial = function () {
            if (newsId != undefined && newsId != '0') {
                getNews($scope, $http, newsId, editor);
            } else if (materialId != undefined && materialId != '0') {
                getMaterial($scope, $http, materialId, editor);
            }
        };
        //data
        var materialTemp = localStorage.getItem(_MATERIAL_AUTO_SAVE_KEY);
        if(materialTemp != null && materialTemp != undefined){
            bjj.dialog.confirm('你有未保存的草稿，是否继续编辑？', function () {
                $scope.material = JSON.parse(materialTemp); 
                $scope.$apply();
                if($scope.material.customTags){
                    $scope.material.customTags = JSON.parse($scope.material.customTags);

                    getCustomTags($scope, $http);
                } 
                editor.setContent($scope.material.content);
                startAutoSave($scope, $interval, editor);
            }, function () {
                getNewsOrMaterial();
                startAutoSave($scope, $interval, editor);
                localStorage.removeItem(_MATERIAL_AUTO_SAVE_KEY);
            });
        } else {
            getNewsOrMaterial();
            startAutoSave($scope, $interval, editor);
        }
        editor.focus();
        startListenBack2Top();
    });

    imgLibPageInfoInit($rootScope, $scope, $http);
    getDirectoryList($rootScope, $scope, $http);
    getStyleList($scope, $http);
    getImgLibList($rootScope, $scope, $http);
    getReport($scope, $http, $interval);
    getChannelList($scope, $http);
    getLebelList($scope, $http);
    $scope.$watch('tagLimit',function(newValue){
        if(newValue == true){
            $scope.tagShow = true;
            $scope.clearClick = true;
            getSectionList($scope, $http);
        }
    })
    $rootScope.uploadStatus = '';
    $rootScope.shipinList = [];
    //event
    $scope.localUploadImg = true;
    $scope.aliyunUploadImg = false;
    $scope.UploadNull = false;
    $scope.aliyunUploadSp = false;
    $scope.uploadType = function(type) {
        if(type == 2) {
            $scope.typeUpload = 'video';
            $scope.localUploadImg = false;
            $scope.aliyunUploadImg = false;
            $scope.$watch('videoLimit',function(newValue){
                if(newValue == true){
                    //有视频权限
                    $scope.aliyunUploadSp = true;
                    $scope.aliyunUploadImg = false;
                }else {
                    $scope.UploadNull = true;
                }
            })
        }else{
            if(type == 1) {
                $scope.typeUpload = 'img';
            }
            $scope.UploadNull = false;
            $scope.aliyunUploadSp = false;
            $scope.$watch('imgLimit',function(newValue){
                if(newValue == true){
                    $scope.aliyunUploadImg = true;
                    $scope.aliyunUploadSp = false;
                    $scope.localUploadImg = false;
                }else {
                    $scope.localUploadImg = true;
                }
            })
        }
        getImgLibList($rootScope, $scope, $http);
    }
    $scope.previewVideo = function() {
        $scope.previewVideoUrl = this.item.videoPublishUrl;
        $('#videoModal').modal('show');
    }
    //添加视频
    $scope.addVideo = function() {
        $scope.videoUrl = this.item.videoPublishUrl;
        editor.focus();
        editor.execCommand('inserthtml',"<p><br></p><video src='" + $scope.videoUrl + "' width='100%' height='300' type='video/mp4' controls='controls'></video><p><br></p>")

    }
    $scope.retryDecode = function() {
        var that = this;
        that.item.videoState = 2;
        that.item.videoStateText = '转码中...';
        bjj.http.ng.get($scope, $http, '/api/oss/repeatTransCode', {
            id : that.item.id,
            videoSourceUrl: that.item.videoSourceUrl
        }, function(res) {
            if(res.status == 200) {
                getImgLibList($rootScope, $scope,$http);
            }else if(res.status == 500){
                that.item.videoState = 4;
                that.item.videoStateText = '转码失败';
            }
        })
    }
    $scope.mouseenter = function() {
        if(this.item.videoState == 3) {
            this.item.actively = true;
        }
    }
    $scope.mouseleave = function() {
        if(this.item.videoState == 3) {
            this.item.actively = false;
        }
    }
    var videoTimer = setInterval(function() {
        var videoStateListIds = [];
        var videoStateList = _.filter($rootScope.shipinList,{'videoState':'2'});
        _.forEach(videoStateList, function(v,i) {
            videoStateListIds.push(v.id)
        })
        videoStateListIds = videoStateListIds.join(',');
        bjj.http.ng.get($scope, $http, '/api/compile/imgLibrary/state', {
            ids : videoStateListIds
        }, function(res) {
            _.forEach(res.list, function(v,i) {
                if(v.videoState == 3) {
                    _.forEach($rootScope.shipinList, function(y,x) {
                        if(y.id == v.id) {
                            getImgLibList($rootScope, $scope,$http)
                        }
                    })
                }
                if(v.videoState == 4) {
                    _.forEach($rootScope.shipinList, function(y,x) {
                        if(y.id == v.id) {
                            $rootScope.shipinList[i].videoState = 4;
                            $rootScope.shipinList[i].videoStateText = '转码失败';
                        }
                    })
                }
            })
            var videoStateListIds = [];
            var videoStateList = _.filter(res.list,{'videoState':'2'});
            _.forEach(videoStateList, function(v,i) {
                videoStateListIds.push(v.id)
            })
            videoStateListIds = videoStateListIds.join(',');
        })
    },5000);
    var initFlag = 0;
    $scope.toggleNav = function (param) {
        $scope.navActive = param.name;
        if($scope.navActive == 4) {
            $scope.UploadNull = false;
            $scope.typeUpload = 'img';
            $scope.uploadType();
            $scope.$watch('imgLimit',function(newValue){
                if(newValue == true){
                    $scope.aliyunUploadImg = true;
                    $scope.aliyunUploadSp = false;
                    $scope.localUploadImg = false;
                }else {
                    $scope.localUploadImg = true;
                }
            })
            $("ul.upload-btn li").eq(0).addClass('active').siblings().removeClass('active');
            $('ul.upload-btn li').click(function() {
                $(this).addClass('active').siblings().removeClass('active');
            });
            if(initFlag == 0) {
                //上传
                accessid = '';
                accesskey = '';
                host = '';
                policyBase64 = '';
                signature = '';
                callbackbody = '';
                filename = '';
                key = '';
                expire = 0;
                g_object_name = '';
                now = timestamp = Date.parse(new Date()) / 1000;

                function send_request() {
                    var xmlhttp = null;
                    if (window.XMLHttpRequest) {
                        xmlhttp = new XMLHttpRequest();
                    }
                    else if (window.ActiveXObject) {
                        xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
                    }

                    if (xmlhttp != null) {
                        if ($scope.aliyunUploadSp) {
                            serverUrl = 'http://' + window.location.host + '/api/oss/video';
                        } else if ($scope.aliyunUploadImg) {
                            serverUrl = 'http://' + window.location.host + '/api/oss/img';
                        }
                        xmlhttp.open("GET", serverUrl, false);
                        xmlhttp.send(null);
                        return xmlhttp.responseText
                    }
                    else {
                        bjj.dialog.alert('danger', '您的浏览器不支持xmlhttp');
                    }
                };

                function get_signature() {
                    now = timestamp = Date.parse(new Date()) / 1000;
                    body = send_request();
                    var obj = eval("(" + body + ")");
                    host = obj['host']
                    policyBase64 = obj['policy']
                    accessid = obj['accessid']
                    signature = obj['signature']
                    expire = parseInt(obj['expire'])
                    callbackbody = obj['callback']
                    key = obj['dir']
                    return true;
                    /* }
                     return false;*/
                };

                function get_suffix(filename) {

                    pos = filename.lastIndexOf('.')
                    suffix = '';
                    if (pos != -1) {
                        suffix = filename.substring(pos)
                    }
                    return suffix;
                }

                function random_string(len) {

                    len = len || 32;
                    var chars = 'ABCDEFGHJKMNPQRSTWXYZabcdefhijkmnprstwxyz2345678';
                    var maxPos = chars.length;
                    var pwd = '';
                    for (i = 0; i < len; i++) {
                        pwd += chars.charAt(Math.floor(Math.random() * maxPos));
                    }
                    return pwd;
                }

                function calculate_object_name(filename) {
                    suffix = get_suffix(filename)
                    g_object_name = key + random_string(10) + suffix
                }

                function set_upload_param(up, filename, ret) {
                    if (ret == false) {
                        ret = get_signature()
                    }
                    g_object_name = key;
                    if (filename != '') {
                        suffix = get_suffix(filename)
                        calculate_object_name(filename)
                    }
                    new_multipart_params = {
                        'key': g_object_name,
                        'policy': policyBase64,
                        'OSSAccessKeyId': accessid,
                        'success_action_status': '200', //让服务端返回200,不然，默认会返回204
                        'callback': callbackbody,
                        'signature': signature,
                    };

                    up.setOption({
                        'url': host,
                        'multipart_params': new_multipart_params
                    });

                    up.start();
                }

                var uploaderImg = new plupload.Uploader({
                    runtimes: 'html5,flash,silverlight,html4',
                    browse_button: 'img-files',
                    container: document.getElementById('container-img-file'),
                    flash_swf_url: 'lib/plupload-2.1.2/js/Moxie.swf',
                    silverlight_xap_url: 'lib/plupload-2.1.2/js/Moxie.xap',
                    url: 'http://oss.aliyuncs.com',

                    filters: {
                        mime_types: [
                            {title: "Image files", extensions: "jpg,gif,png,bmp"}
                        ],
                        max_file_size: '2mb', //最大限制
                        prevent_duplicates: true
                    },

                    init: {
                        FilesAdded: function (up, files) {
                            set_upload_param(uploaderImg, '', false);
                        },

                        BeforeUpload: function (up, file) {
                            set_upload_param(up, file.name, true);
                        },

                        FileUploaded: function (up, file, info) {
                            if (info.status == 200) {
                                getImgLibList($rootScope, $scope, $http);
                            }
                        },
                        Error: function (up, err) {
                            if (err.code == -600) {
                                bjj.dialog.alert('danger', '请将文件大小控制在2M以内！');
                            }
                            else if (err.code == -601) {
                                bjj.dialog.alert('danger', '选择的文件后缀不对');
                            }
                            else if (err.code == -602) {
                                bjj.dialog.alert('danger', '文件已经上传过一遍啦！');
                            }
                            else {
                                bjj.dialog.alert('danger', 'Error xml:' + err.response + '！');
                            }
                        }
                    }
                });
                var uploaderVideo = new plupload.Uploader({
                    runtimes: 'html5,flash,silverlight,html4',
                    browse_button: 'shipin-files',
                    container: document.getElementById('container-sp-file'),
                    flash_swf_url: 'lib/plupload-2.1.2/js/Moxie.swf',
                    silverlight_xap_url: 'lib/plupload-2.1.2/js/Moxie.xap',
                    url: 'http://oss.aliyuncs.com',

                    filters: {
                        mime_types: [
                            {title: "video files", extensions: "mp4,flv,mov,wmv,avi,MPEG1,MPEG2,MPEG3,MPEG4,rm,rmvb"}
                        ],
                        max_file_size: '200mb', //最大限制
                        prevent_duplicates: true
                    },

                    init: {
                        FilesAdded: function (up, files) {
                            $rootScope.uploadStatus = '上传中...';
                            $rootScope.shipinList.unshift({videoPreviewUrl: ''});
                            $scope.$apply();
                            set_upload_param(uploaderVideo, '', false);
                        },
                        BeforeUpload: function (up, file) {
                            set_upload_param(up, file.name, true);
                        },
                        FileUploaded: function (up, file, info) {
                            if (info.status == 200) {
                                getImgLibList($rootScope, $scope, $http);
                                $rootScope.uploadStatus = '上传成功';

                            } else if (info.status == 203) {
                                getImgLibList($rootScope, $scope, $http);
                            } else {
                                $rootScope.uploadStatus = '上传失败';
                                $rootScope.shipinList.splice(0, 1);
                                bjj.dialog.alert('danger', '上传失败！');
                            }
                        },
                        Error: function (up, err) {
                            if (err.code == -600) {
                                bjj.dialog.alert('danger', '请将文件大小控制在200M以内！');
                            }
                            else if (err.code == -601) {
                                bjj.dialog.alert('danger', '选择的文件后缀不对');
                            }
                            else if (err.code == -602) {
                                bjj.dialog.alert('danger', '文件已经上传过一遍啦！');
                            }
                            else {
                                bjj.dialog.alert('danger', 'Error xml:' + err.response + '！');
                            }
                        }
                    }
                });
                uploaderVideo.init();
                uploaderImg.init();
                initFlag = 2;
            }
        }
    };
    $scope.compileAddDirectory = function() {
        addDirectory($rootScope, $scope, $http);
        bjj.http.ng.get($scope, $http, '/api/favorite/group/list', {}, function (res) {
            // 更新全局group
            $rootScope.allGroup = res.list;
            for(var i = 0; i< $rootScope.allGroup.length; i++) {
                $rootScope.allGroup[i].active = true;
            }
            $rootScope.normalGroup = _.filter($rootScope.allGroup, {'groupType' : "0"});
            $rootScope.normalGroup[$rootScope.normalGroup.length - 1].isShowInput = true;
            var _id = $rootScope.normalGroup[$rootScope.normalGroup.length - 1].id;
            setTimeout(function () {
                $('.groupName-'+_id).select();
            }, 0);
            $scope.blurInput = function(item) {
                if($rootScope.normalGroup[$rootScope.normalGroup.length - 1].groupName.trim() == '') {
                    item.groupName = '新建分组';
                }
                $rootScope.normalGroup[$rootScope.normalGroup.length - 1].isShowInput = false;
                renameDirectoryFunction($scope, $http, item.id,item.groupName.trim());
            };
            if(res.list.length > 0){
                _.map(res.list, function (v, i) {
                    v.groupContShow = false;
                    $scope.sortableSource.push('favoriteNewsListCont' + v.id);
                });

                groupPageInfoInit($scope, $http, res.list);
                getDirectoryNewsList($scope, $http, res.list[0].id, res.list[0].groupType);
                res.list.push({
                    groupName   : '添加分组',
                    groupType   : -1
                });
                $scope.directoryList = _.chunk(res.list, 3);
                bindSortable4Editor($scope, $http);
            }
        }, function (res) {
            bjj.dialog.alert('danger', res.msg);
        });
    };
    $scope.renameDirectoryClick = function($event,item) {
        var groupName = item.groupName;
        item.isShowInput = true;
        setTimeout(function () {
            $('.groupName-'+ item.id).select();
            $('.groupName-'+ item.id).focus().css("color","#333");
        }, 0);
        $scope.blurInput = function(item) {
            if(item.groupName.trim() == '') {
                item.groupName = groupName;
            }
            item.isShowInput = false;
            renameDirectoryFunction($scope, $http, item.id,item.groupName.trim());
        };
    };
    /**鼠标点击文件夹时**/
    $scope.directoryClick = function($event, $index, item, flag) {
        item.active = !item.active;
        var groupType = item.groupType;
        var groupId = $event.target.dataset.groupId;
        _.map($scope.directoryList, function (v) {
            _.map(v, function (w) {
                w.groupContShow = (groupId == w.id);
            });
        });
        $(".directoryItem").toggle();
        $scope.selectedNewsList = _.filter($scope.groupPageInfoMap[groupId].dataList, {'active': true});
        if(item.groupType == 1) {
            $($event.target).css({'backgroundPosition': '-46px 378px'});
            $('.normal-group').css({'backgroundPosition':'0 -116px'});
            $('.share-group').css({'backgroundPosition':'-49px 80px'})
            if(flag){
                $($event.target).css({'backgroundPosition': '-48px 242px'});
            }
            _.forEach($rootScope.allGroup, function(v) {
                if(v.id != item.id) {
                    v.active = true;
                }
            });
            if(item.active) {
                $($event.target).css({'backgroundPosition':'-49px 242'});
            }else {
                $($event.target).css({'backgroundPosition':'-49px 720px'});
            }
        }
        if(item.groupType == 0) {
            $($event.target).css({'backgroundPosition': '0px -39px'});
            $('.normal-group').css({'backgroundPosition':'0 -115px'});
            $('.import-group').css({'backgroundPosition':'-49px 200px'});
            $('.share-group').css({'backgroundPosition':'-49px 80px'});
            if(flag){
                $($event.target).css({'backgroundPosition': '0 0'});
            }
            _.forEach($rootScope.allGroup, function(v) {
                if(v.id != item.id) {
                    v.active = true;
                }
            });
            if(item.active) {
                $($event.target).css({'backgroundPosition':'0 -39px'});
            }else {
                $($event.target).css({'backgroundPosition':'0 0'});
            }
        }
        if(item.groupType == 2) {
            $($event.target).css({'backgroundPosition': '0px -399px'});
            $('.normal-group').css({'backgroundPosition':'0 -115px'});
            $('.import-group').css({'backgroundPosition':'-49px 200px'});
            if(flag){
                $($event.target).css({'backgroundPosition': '0px -399px'});
            }
            _.forEach($rootScope.allGroup, function(v) {
                if(v.id != item.id) {
                    v.active = true;
                }
            });
            if(item.active) {
                $($event.target).css({'backgroundPosition':'-49px 121px'});
            }else {
                $($event.target).css({'backgroundPosition':'0 -399px'});
            }
        }
        if($scope.groupPageInfoMap[groupId].dataList.length == 0){
            getDirectoryNewsList($scope, $http, groupId, groupType);
        }
    };
    /**删除文件夹**/
    $scope.deleteDirectory = function($event,item) {
        if($scope.directoryList.length <= 1) {
            bjj.dialog.alert('danger', '至少保留一个分组！');
            return;
        };
        bjj.dialog.confirm('删除分组后，分组中全部收藏文稿也将一并删除，确定删除吗？', function () {
            bjj.http.ng.del($scope, $http, '/api/favorite/group/' + item.id+'?_method=DELETE', {}, function (res) {
                bjj.dialog.alert('success', res.msg);
                getDirectoryList($rootScope, $scope, $http);
            }, function(res) {
                bjj.dialog.alert('danger', res.msg);
            });
        });
    };
    /*查看预览*/
    $scope.previewDetail = function(group, id) {
        $(".editor-sidebar-right").show();
        $(".editor-cont-center").removeClass('editor-cont-center-width').addClass("mobile-editor-cont-center");
        $(".edui-toolbar").addClass("hasDetail-toolbar");
        if(group == 1) {
            $(".oldContent").hide();
            $(".captureInfo").hide();
        }else {
            $(".oldContent").show();
            $(".captureInfo").show();
        }
        if($(".editor-page").scrollTop() > 135) {
            $(".edui-toolbar").addClass('hasDetail-position-toolbar');
        }
        $scope.detailModal = true;
        bjj.http.ng.get($scope, $http, '/api/capture/news/operation/' + id, {}, function (res) {
            $scope.previewDetailList = res.msg;
            if(group == '2') {
                if (res.msg.news.captureTime == null) {
                    $('.detailBody .clearfix .captureTime').hide();
                } else {
                    $('.detailBody .clearfix .captureTime').show();
                }
                if (res.msg.news.siteName == null) {
                    $('.detailBody .clearfix .siteName').hide();
                } else {
                    $('.detailBody .clearfix .siteName').show();
                }
                if (res.msg.news.url == null) {
                    $('.detailBody .page-header h3 a').hide();
                } else {
                    $('.detailBody .page-header h3 a').show();
                }
            }
        });
    };

    $scope.closeDetail = function() {
        $scope.detailModal = false;
        $(".editor-cont-center").addClass('editor-cont-center-width').removeClass("mobile-editor-cont-center");
        $(".edui-toolbar").removeClass("hasDetail-toolbar hasDetail-position-toolbar");
    }
    $scope.toggleEditorArea = function () {
        var _areaBottomView = $(".editor-cont-center .editor-page .editor-page-bottom");
        if($scope.editorAreaStatus) {
            $('.edui-container').slideUp();
            _areaBottomView.css("padding-bottom", "200px");
            _areaBottomView.css("padding-bottom", "20px");
        } else {
            $('.edui-container').slideDown();
            $('.editor-page').animate({scrollTop: 0}, 0);
        }
        $scope.editorAreaStatus = !$scope.editorAreaStatus;
    };
    $scope.toggleSelectedNews = function (groupId, id) {
        if(!this.item.active) {
            $scope.newsIds.push(id)
        }else {
            _.forEach($scope.newsIds, function (v,i) {
                if(v == id) {
                    $scope.newsIds.splice(i,1)
                }
            })
        }
        this.item.active = !this.item.active;
        var _this_checkbox = $('.favoriteNews-checkbox-' + id);
        this.item.active ? _this_checkbox.addClass('active') : _this_checkbox.removeClass('active');
        $scope.selectedNewsList = _.filter($scope.groupPageInfoMap[groupId].dataList, {'active': true});
    };
    $scope.toggleSelectedChannel = function () {
        this.channel.active = !this.channel.active;
        $scope.selectedChannelList = _.filter($scope.channelList, {'active': true});
    };
    $scope.toggleWechatSyncType = function () {
        if(($scope.material.content.indexOf('</video>')!=-1 || $("#" + _editorContainerId).html().indexOf('</video>')!=-1)){
            $scope.showVideoDenger = true;
            setTimeout(function(){
                $scope.showVideoDenger = false;
                $scope.$apply();
            },1000)
            return;
        }
        $scope.wechatSyncType = ($scope.wechatSyncType + 1) % 2;
    };
    $scope.selectCover = function () {
        var cover = $('#imgUrlsModal .img-item.active img').attr('src');
        if (cover == undefined) {
            cover = _defaultCover;
        }
        $scope.material.picUrl = cover;
        $('.modal').modal('hide');
    };
    $scope.delCover = function () {
        $scope.material.picUrl = _defaultCover;
        $('#imgUrlsModal .img-item').removeClass('active');
    };
    $scope.delFavoriteNews = function (groupId, id) {
        bjj.http.ng.del($scope, $http, '/api/capture/news/operation/' + id, {}, function (res) {
            var list = $scope.groupPageInfoMap[groupId].dataList;
            for(var i = list.length-1; i >= 0; i--){
                if(list[i]._id == id){
                    $scope.groupPageInfoMap[groupId].dataList.splice(i, 1);
                }
            }
        }, function (res) {
            bjj.dialog.alert('danger', res.msg);
        });
    };
    $scope.doSaveMaterial = function (materialId, callback) {
        $scope.tagIds = '';
        _.map(addList,function(item){
            $scope.tagIds += (item.id +',')
        })
        _.map($scope.comitLebelList,function (v) {
            if(v.tagType == 'drop' || v.tagType == 'text' || v.tagType == 'date'){
                if(v.tagValue.length == 0){
                    v.tagValue = '';
                }
            }
        })
        var material = {
            id: $scope.material.id,
            title: $scope.material.title || '',
            author: $scope.material.author || '',
            tagIds: $scope.tagIds,
            picUrl: $scope.material.picUrl == _defaultCover ? '' : $scope.material.picUrl,
            keywords: $scope.material.keywords || '',
            source: $scope.material.source || '',
            content: $("#" + _editorContainerId).html() || '',
            contentAbstract: $scope.material.contentAbstract || '',
            classification: $scope.material.classification || '',
            type: 3,
            customTags: JSON.stringify($scope.comitLebelList)
        };

        // 以下网络请求同时处理新增和编辑的情况
        var successFunction = function (res) {
            if (materialId == undefined || materialId == '') {
                $scope.material.id = res.id;
            }
            callback($scope.material.id);
            $scope.saveBtnStatus = 'normal';
            $scope.pushBtnStatus = 'normal';
        };
        var failureFunction = function (res) {
            bjj.dialog.alert('danger', res.msg);
            $scope.saveBtnStatus = 'normal';
            $scope.pushBtnStatus = 'normal';
        }
        if (materialId == undefined || materialId == '') {
            bjj.http.ng.postBody($scope, $http, '/api/compile/material', JSON.stringify(material), successFunction, failureFunction);
        } else {
            bjj.http.ng.putBody($scope, $http, '/api/compile/material/' + materialId, JSON.stringify(material), successFunction, failureFunction);
        }
        localStorage.removeItem(_MATERIAL_AUTO_SAVE_KEY);
    };
    $scope.saveMaterial = function () {
        if ($scope.saveBtnStatus == 'normal') {
            $scope.saveBtnStatus = 'loading';
            $scope.doSaveMaterial($scope.material.id, function (materialId) {
                bjj.dialog.alert('success', '保存成功！');
            });
        }
    };
    $scope.appPreview = function() {
        $(".btn-all").addClass('btn-primary');
        $(".pc-btn").removeClass('btn-primary');
        $scope.doSaveMaterial($scope.material.id, function (materialId) {
            bjj.http.ng.put($scope, $http, '/api/compile/material/preview/' + materialId, {}, function (res) {
                $scope.newId = res.msg._id;
                $('#appModal').modal('show');
                getEditorPreviewData($scope, $http, $sce, $scope.newId);
                $('#share-qrcode').html("");
                $('#share-qrcode').qrcode({width: 100, height: 100, text: 'http://'+window.location.href.split("/")[2] +'/compile/preview.html?previewId=' + $scope.newId});
            });
        });
    };
    $scope.h5Public = function() {
        bjj.dialog.confirm('是否确认发布文稿?', function () {
            $scope.doSaveMaterial($scope.material.id, function (materialId) {
                bjj.http.ng.post($scope, $http, '/api/compile/material/release', {
                    materialId : materialId
                }, function (res) {
                    $state.go('editorPublicCallback',{ materialId: res.msg._id});
                });
            });
        })
    };
    $scope.previewMaterial = function () {
        $scope.doSaveMaterial($scope.material.id, function (materialId) {
            var previewUrl = '/compile/preview.html?previewId=' + $scope.newId;
            var newTab = window.open('about:blank');
            newTab == undefined ? goto(previewUrl) : (newTab.location.href = previewUrl);
        });
    };
    $scope.syncMaterial = function () {
        $scope.doSaveMaterial($scope.material.id, function (materialId) {
            $('#channelsModal').modal();
        });
    };
    $scope.pushMaterial = function () {
        if ($scope.pushBtnStatus == 'normal') {
            $scope.pushBtnStatus = 'loading';
            $scope.doSaveMaterial($scope.material.id, function (materialId) {
                bjj.http.ng.post($scope, $http, '/api/compile/material/articlePush', {
                    materialId: materialId
                }, function (res) {
                    bjj.dialog.alert('success', '推送成功！');
                }, function (res) {
                    bjj.dialog.alert('danger', res.msg);
                });
            });
        }
    };
    $scope.editorHeight = $("#"+_editorContainerId).height();
    $scope.createMaterialWithAbstract = function () {
        if ($scope.selectedNewsList.length > 0) {
            var content = '';
            var contentAbstract = '';
            var keywords = [], classification = [];
            var contentList = [];
            var i = 0;
            _.forEach($scope.selectedNewsList, function (v, i) {
                var titleForm = "<h3><a href='" + v.news.url + "' target='_blank'>" + v.news.title + "</a></h3>\n";
                var contentForm = "<div>" + v.news.contentAbstract + "</div><p><br></p>";
                contentList.push(titleForm);
                contentList.push(contentForm);
                content += titleForm;
                content += contentForm;
                contentAbstract += v.news.title + ';';
                if(!_.isEmpty(v.news.keywords)){
                    keywords.push(v.news.keywords);
                }
                if(!_.isEmpty(v.news.classification) && i==0){
                    classification.push(v.news.classification);
                }
            });
            if(_.isEmpty($scope.material.keywords)){
                $scope.material.keywords = keywords.join(' ');
            }
            if(_.isEmpty($scope.material.classification)){
                $scope.material.classification = classification.join(' ');
            }
            $scope.material.content = content;
            $scope.material.contentAbstract = contentAbstract.substr(0, 120);
            var time = setInterval(function() {
                if(i < contentList.length) {
                    editor.setContent(contentList[i], true);
                    if($("#editor").height()/ $scope.editorHeight > 1){
                        $('.editor-page').animate({scrollTop: ($('#editor').height()/ $scope.editorHeight)* 200}, 0);
                    }
                }else {
                    bjj.dialog.alert('success', '摘要完成！');
                    $(".modal-backdrop").hide();
                    clearInterval(time)
                }
                i++;
            }, 200);
        }else {
            bjj.dialog.alert('danger', '请选择新闻');
        }
    };
    $scope.abstractShow = function () {
        $(".abstract-type-list").show();
    }
    $scope.getAbstract = function (size) {
        var newsIds = [];
        $(".abstract-type-list").hide();
        $(".wait-tips").show();
        if(size == 10) {
            $scope.abstractLong = true;
            $scope.abstractMiddle = false;
            $scope.abstractShort = false;
        }
        if(size == 5) {
            $scope.abstractMiddle = true;
            $scope.abstractLong = false;
            $scope.abstractShort = false;
        }
        if(size == 3) {
            $scope.abstractShort = true;
            $scope.abstractMiddle = false;
            $scope.abstractLong = false;
        }
        if($scope.selectedNewsList.length == 0) {
            bjj.dialog.alert('danger', '请选择新闻');
            $(".wait-tips").hide();
            return;
        }
        _.forEach($scope.selectedNewsList, function(data) {
            newsIds.push(data._id)
        })
        var newsIds = newsIds.join(',');
        bjj.http.ng.post($scope, $http, '/api/capture/news/operation/roundup', {
            newsIds: newsIds,
            roundupSize: size
        }, function (res) {
            localStorage.setItem('abstractNews', res.list);
            var contentList = [];
            var i = 0;
            $(".wait-tips").hide();
            _.forEach(res.list, function (v, i) {
                var titleForm = "<h3><a href='" + v.url + "' target='_blank'>" + v.title + "</a></h3>\n";
                var contentForm = "<div>" + v.contentAbstract + "</div><p><br></p>";
                contentList.push(titleForm);
                contentList.push(contentForm);
            })
            var time = setInterval(function() {
                if(i < contentList.length) {
                    editor.setContent(contentList[i], true);
                    if($("#editor").height()/ $scope.editorHeight > 1){
                        $('.editor-page').animate({scrollTop: ($('#editor').height()/ $scope.editorHeight)* 200}, 0);
                    }
                }else {
                    bjj.dialog.alert('success', '摘要完成！');
                    $(".modal-backdrop").hide();
                    clearInterval(time)
                }
                i++;
            }, 200);
        }, function (res) {
            $(".wait-tips").hide();
            bjj.dialog.alert('danger', res.msg);
        })
    }
    $scope.creatViewpoint = function () {
        var newsIds = [];
        $(".wait-tips").show();
        _.forEach($scope.selectedNewsList, function(data) {
            newsIds.push(data._id)
        })
        var newsIds = newsIds.join(',');
        bjj.http.ng.post($scope, $http, '/api/capture/news/operation/viewpoint', {
            newsIds: newsIds
        }, function (res) {
            $(".wait-tips").hide();
            var contentList = [];
            var i = 0;
            _.forEach(res.list, function (v, i) {
                var titleForm = "<h3><a href='" + v.url + "' target='_blank'>" + v.title + "</a></h3>\n";
                contentList.push(titleForm);
                _.forEach(v.viewpoint, function (v, i) {
                    var nameForm = '<h4><b>' + v.name + '  重要度：' + v.importance + '</b></h4>';
                    contentList.push(nameForm);
                    _.forEach(v.viewpoints, function (v, i) {
                        var contentForm = "<div><ul><li>" + v + "</li></ul> </div>";
                        contentList.push(contentForm);
                    })
                })
            })
            var time = setInterval(function() {
                if(i < contentList.length) {
                    editor.setContent(contentList[i], true);
                    if($("#editor").height()/ $scope.editorHeight > 1){
                        $('.editor-page').animate({scrollTop: ($('#editor').height()/ $scope.editorHeight)* 200}, 0);
                    }
                }else {
                    bjj.dialog.alert('success', '观点提炼完成！');
                    $(".modal-backdrop").hide();
                    clearInterval(time)
                }
                i++;
            }, 200);
        }, function (res) {
            $(".wait-tips").hide();
            bjj.dialog.alert('danger', res.msg);
        })
    }
    angular.element(window).bind('click',function(event){
        if(event.target.className != 'btn btn-primary gd news-abstract'){
            $(".abstract-type-list").hide();
        }
    })
    $scope.gotoChannelPage = function () {
        $('#channelsModal').on('hidden.bs.modal', function (event) {
            goto('#!/account/channel');
        }).modal('hide');
    };
    $scope.syncMaterial2Channels = function (type) {
        var timeStamp = new Date().getTime();
        var _query = type == 0 ? {'active': true} : {'active': true, 'channelType': type};
        $scope.syncChannelList = _.filter($scope.channelList, _query);

        //validate
        if ($scope.syncChannelList.length <= 0) {
            bjj.dialog.alert('warning', '请选择需要同步的渠道！');
            return;
        }

        _.map($scope.syncChannelList, function (v) {
            v.syncStatus = -1; //-1 - 同步中   1-同步成功  0-同步失败
            v.syncMsg = '努力同步中……';
        });

        $('#channelsModal').modal('hide');
        $('#syncModal').modal();

        // 单渠道进行同步
        $scope.syncChannelList.forEach(function (v, i, t) {
            bjj.http.ng.post($scope, $http, '/api/share/material/' + $scope.material.id, {
                channelIds: v.id,
                wechatSyncType: $scope.wechatSyncType,
                timeStamp      : timeStamp
            }, function (res) {
                var channel = _.filter($scope.syncChannelList, {id: v.id})[0];
                if (res.result[0].detail.status == 1) {
                    channel.syncStatus = 1;
                    channel.syncMsg = '';
                } else {
                    channel.syncStatus = 0;
                    channel.syncMsg = res.result[0].detail.msg;
                }
            }, function (res) {
                bjj.dialog.alert('danger', res.msg);
            });
        });
    };
    $scope.addStyle = function () {
        bjj.http.ng.post($scope, $http, '/api/compile/style/import', {
            content : '<img src="' + $scope.style.content + '">'
        }, function (res) {
            getStyleList($scope, $http);
        });
    };
    $scope.delStyle = function () {
        if(this.item.userId == 0){
            bjj.dialog.alert('warning', '系统样式不能删除！');
            return;
        }
        var styleId = this.item.id;
        bjj.http.ng.del($scope, $http, '/api/compile/style/' + styleId, {}, function (res) {
            var list = $scope.styleList;
            for(var i = list.length-1; i >= 0; i--){
                if(list[i].id == styleId){
                    $scope.styleList.splice(i, 1);
                }
            }
        }, function (res) {
            console.log(res);
        });
    };
    $scope.addImgLib = function () {
        bjj.http.ng.post($scope, $http, '/api/compile/imgLibrary/img', {
            imgUrl : $scope.imgLib.content
        }, function (res) {
            imgLibPageInfoInit($rootScope, $scope, $http);
            getImgLibList($rootScope, $scope, $http);
        });
    };
    $scope.delMaterialLib = function () {
        var id = this.item.id;
        bjj.dialog.confirm('是否确认删除素材？', function() {
            bjj.http.ng.del($scope, $http, '/api/compile/materialLibrary/material/' + id, {}, function (res) {
                if($scope.typeUpload == 'img') {
                    var list = $scope.imgLibList;
                    for (var i = list.length - 1; i >= 0; i--) {
                        if (list[i].id == id) {
                            $scope.imgLibList.splice(i, 1);
                        }
                    }
                }else {
                    var list = $rootScope.shipinList;
                    for (var i = list.length - 1; i >= 0; i--) {
                        if (list[i].id == id) {
                            $rootScope.shipinList.splice(i, 1);
                        }
                    }
                }
            }, function (res) {
                console.log(res);
            });
        })
    };
    $scope.createReport = function () {
        bjj.http.ng.post($scope, $http, '/api/compile/report', {
            name        : $scope.report.name,
            keywords    : $scope.report.keywords,
            timeRange   : $scope.report.timeRange
        }, function (res) {
            console.log(res);
            $scope.report.id = res.reportId;
            $scope.report.status = 1;
            dealReportProgress($scope, $http, $interval);
        }, function (res) {
            bjj.dialog.alert('danger', res.msg);
        })
    };
    $scope.getFavoriteNewsListData = function () {
        //新导入文稿后，重新刷新文稿列表，需要重新初始化。
        getDirectoryList($rootScope, $scope, $http);
    };
    $scope.mouseTemplate = function() {
        $scope.templateShow = true;
        getTemplate($scope, $http);
    }
    $scope.mouseTemplateName = function() {
        $scope.templateShow = false;
    }
    var checkNum = /^[0-9]+\.{0,1}[0-9]{0,2}$/;
    $scope.checkBodyFontSize = function($event, bodyFontSize) {
        if(!checkNum.test(bodyFontSize) || bodyFontSize < 6 || bodyFontSize > 48) {
            $($event.target).addClass('error');
        }else {
            $($event.target).removeClass('error');
        }
    }
    $scope.checkSpace = function($event, space) {
        if(!checkNum.test(space) || space < 1 || space > 5) {
            $($event.target).addClass('error');
        }else {
            $($event.target).removeClass('error');
        }
    }
    $scope.checkTextIndent = function($event, textIndent) {
        if(!checkNum.test(textIndent) || textIndent < 0 || textIndent > 48) {
            $($event.target).addClass('error');
        }else {
            $($event.target).removeClass('error');
        }
    }
    $scope.checkParagraphSpace = function($event, paragraphSpace) {
        if(!checkNum.test(paragraphSpace) || paragraphSpace < 0 || paragraphSpace > 25) {
            $($event.target).addClass('error');
        }else {
            $($event.target).removeClass('error');
        }
    }
    $scope.setTemplate = function() {
        $(".apply").addClass('applyTemplate');
        $(".modal-title").children('span').eq(1).addClass('active').siblings().removeClass('active');
        $scope.templateShow = false;
        $scope.firstTemplate = true;
        $scope.secondTemplate = false;
        $scope.thirdTemplate = false;
        bjj.http.ng.get($scope, $http, '/api/compile/template/0', {}, function (res) {
            var template = res.template;
            if(template.bodyFontFamily) {
                $scope.templateDefault.bodyFontFamily = template.bodyFontFamily;
            }else {
                $scope.templateDefault.bodyFontFamily  = '微软雅黑,Microsoft YaHei';
            }
            $scope.templateDefault.templateName         = template.templateName;
            $scope.templateDefault.bodyFontSize         = template.bodyFontSize;
            $scope.templateDefault.bodyAlignment        = template.bodyAlignment;
            $scope.templateDefault.bodyImgAlignment     = template.bodyImgAlignment;
            $scope.templateDefault.wordSpace            = template.wordSpace;
            $scope.templateDefault.firstLineIndent      = template.firstLineIndent;
            $scope.templateDefault.lineSpace            = template.lineSpace;
            $scope.templateDefault.textIndent           = template.textIndent;
            $scope.templateDefault.beforeParagraphSpace = template.beforeParagraphSpace;
            $scope.templateDefault.afterParagraphSpace  = template.afterParagraphSpace;
            if($scope.templateDefault.firstLineIndent == 'on') {
                $(".toggle-button").prop("checked",true);
            }else {
                $(".toggle-button").prop("checked",false);
            }
        })
    }
    $scope.choseTemplate = function(index) {
        if(index == 0) {
            $scope.firstApply = true;
            $scope.secondApply = false;
            $scope.thirdApply = false;
            $scope.firstTemplate = true;
            $scope.secondTemplate = false;
            $scope.thirdTemplate = false;
        }else if(index == 1) {
            $scope.firstApply = false;
            $scope.secondApply = true;
            $scope.thirdApply = false;
            $scope.firstTemplate = false;
            $scope.secondTemplate = true;
            $scope.thirdTemplate = false;
        }else if(index == 2){
            $scope.firstApply = false;
            $scope.secondApply = false;
            $scope.thirdApply = true;
            $scope.firstTemplate = false;
            $scope.secondTemplate = false;
            $scope.thirdTemplate = true;
        }
        $scope.index = index;
    }
    $scope.saveTemplate = function() {
        if(($('.off').css('display')) == 'none') {
            $scope.templateDefault.firstLineIndent = 'on'
        }else {
            $scope.templateDefault.firstLineIndent = 'off'
        }
        if(($('.one-off').css('display')) == 'none') {
            $scope.customOne.firstLineIndent = 'on'
        }else {
            $scope.customOne.firstLineIndent = 'off'
        }
        if(($('.two-off').css('display')) == 'none') {
            $scope.customTwo.firstLineIndent = 'on'
        }else {
            $scope.customTwo.firstLineIndent = 'off'
        }
        if($scope.index == 0) {
            if($scope.templateDefault.firstLineIndent == 'on') {
                $(".toggle-button").prop("checked",true);
            }else {
                $(".toggle-button").prop("checked",false);
            }
            var params = {
                templateName         :$scope.templateDefault.templateName,
                bodyFontFamily       :$scope.templateDefault.bodyFontFamily,
                bodyFontSize         :parseInt($scope.templateDefault.bodyFontSize),
                bodyAlignment        :$scope.templateDefault.bodyAlignment,
                bodyImgAlignment     :$scope.templateDefault.bodyImgAlignment,
                wordSpace            :$scope.templateDefault.wordSpace,
                firstLineIndent      :$scope.templateDefault.firstLineIndent,
                lineSpace            :$scope.templateDefault.lineSpace,
                textIndent           :parseInt($scope.templateDefault.textIndent),
                beforeParagraphSpace :parseInt($scope.templateDefault.beforeParagraphSpace),
                afterParagraphSpace  :parseInt($scope.templateDefault.afterParagraphSpace)
            }
        }else if($scope.index == 1) {
            if($scope.customOne.firstLineIndent == 'on') {
                $(".one-toggle-button").prop("checked",true);
            }else {
                $(".one-toggle-button").prop("checked",false);
            }
            var params = {
                templateName         :$scope.customOne.templateName,
                bodyFontFamily       :$scope.customOne.bodyFontFamily,
                bodyFontSize         :parseInt($scope.customOne.bodyFontSize),
                bodyAlignment        :$scope.customOne.bodyAlignment,
                bodyImgAlignment     :$scope.customOne.bodyImgAlignment,
                wordSpace            :$scope.customOne.wordSpace,
                firstLineIndent      :$scope.customOne.firstLineIndent,
                lineSpace            :$scope.customOne.lineSpace,
                textIndent           :parseInt($scope.customOne.textIndent),
                beforeParagraphSpace :parseInt($scope.customOne.beforeParagraphSpace),
                afterParagraphSpace  :parseInt($scope.customOne.afterParagraphSpace)
            }
        }else {
            if($scope.customTwo.firstLineIndent == 'on') {
                $(".two-toggle-button").prop("checked",true);
            }else {
                $(".two-toggle-button").prop("checked",false);
            }
            var params = {
                templateName         :$scope.customTwo.templateName,
                bodyFontFamily       :$scope.customTwo.bodyFontFamily,
                bodyFontSize         :parseInt($scope.customTwo.bodyFontSize),
                bodyAlignment        :$scope.customTwo.bodyAlignment,
                bodyImgAlignment     :$scope.customTwo.bodyImgAlignment,
                wordSpace            :$scope.customTwo.wordSpace,
                firstLineIndent      :$scope.customTwo.firstLineIndent,
                lineSpace            :$scope.customTwo.lineSpace,
                textIndent           :parseInt($scope.customTwo.textIndent),
                beforeParagraphSpace :parseInt($scope.customTwo.beforeParagraphSpace),
                afterParagraphSpace  :parseInt($scope.customTwo.afterParagraphSpace)
            }
        }
        if(!checkNum.test(params.bodyFontSize) || !checkNum.test(params.wordSpace) || !checkNum.test(params.lineSpace) || !checkNum.test(params.textIndent) || !checkNum.test(params.beforeParagraphSpace) || !checkNum.test(params.afterParagraphSpace) ||  params.bodyFontSize < 6 || params.bodyFontSize>48 || params.wordSpace < 0 || params.wordSpace> 5 || params.lineSpace < 1 || params.lineSpace > 5 ||
            params.textIndent < 0 || params.textIndent > 48 || params.beforeParagraphSpace < 0 || params.beforeParagraphSpace > 25 || params.afterParagraphSpace < 0 || params.afterParagraphSpace > 25) {
            return;
        }
        bjj.http.ng.put($scope, $http,'/api/compile/template/'+ $scope.index, params, function(res) {
            $(".apply").eq($scope.index).removeClass('applyTemplate');
            bjj.dialog.alert('success', res.msg, {
                callback: function() {
                    getTemplate($scope, $http);
                }
            });
        }, function(res) {
            bjj.dialog.alert('danger', res.msg);
        });
    }
    $scope.useTemplate = function(index) {
        if(editor.getContentTxt().trim() == "") {
            bjj.dialog.alert('danger', '文本内容不能为空！');
            return;
        }
        templateUser($scope, $http, index);
    }
    $scope.applyTemplate = function(index) {
        if(editor.getContentTxt().trim() == "") {
            bjj.dialog.alert('danger', '文本内容不能为空！');
            return;
        }
        if($(".apply").eq($scope.index).hasClass('applyTemplate')) {
            return;
        }
        $('#TemplateModal').modal('hide');
        templateUser($scope, $http, index);
    }
    $scope.removeDuplicateParagraph = function () {
        var content = editor.getContent() || '';
        if(editor.getContentTxt().trim() == "") {
            bjj.dialog.alert('danger', '文本内容不能为空！');
            return;
        }
        bjj.dialog.confirm(" 执行段落去重后您设置的样式将丢失且不支持撤销。确定执行段落去重吗？", function () {
            bjj.http.ng.postBody($scope, $http, '/api/compile/material/removeDuplicateParagraph', content, function (res) {
                var i = 0;
                editor.setContent("");
                var time = setInterval(function() {
                    if(i < res.content.length) {
                        editor.setContent(res.content[i], true);
                        $('.editor-page').animate({scrollTop: 0 }, 10);
                        if($("#editor").height()/$scope.editorHeight > 1){
                            $('.editor-page').animate({scrollTop: ($('#editor').height()/ $scope.editorHeight)* 200}, 0);
                        }
                    }else {
                        bjj.dialog.alert('success', '去重完成！');
                        $(".modal-backdrop").hide();
                        clearInterval(time);
                    }
                    i++;
                }, 200);
            }, function (res) {
                bjj.dialog.alert('danger', res.msg);
            });
        });
    };
    /*从原文选择图片*/
    $scope.getTextImg = function() {
        var content = editor.getContent() || '';
        $scope.textImgBtnStatus = 'loading';
        bjj.http.ng.postBody($scope, $http, '/api/compile/material/images', JSON.stringify({
            content : content
        }), function (res) {
            $scope.imgInfo = {
                imgUrls: res.imageList,
                hasImg: (res.imageList.length > 0)
            };
            $("#imgUrlsModal").modal("show");
            editor.setContent(res.content);
            $scope.textImgBtnStatus = 'normal';
        });
    };
    $scope.back2Top = function () {
        $('.editor-page').animate({scrollTop: 0}, 300);
    };
    /*到达底部*/
    $scope.back2Bottom = function() {
        var boxHeight = $(".edui-body-container").height()- 350;
        $(".editor-page").animate({scrollTop: boxHeight}, 300);
    };
    $scope.saveMaterial2Local = function () {
        $scope.material.content = editor.getContent() || '';
        $scope.material.customTags = JSON.stringify($scope.comitLebelList);
        localStorage.setItem(_MATERIAL_AUTO_SAVE_KEY, JSON.stringify($scope.material))
    };
    /*内容滚动时编辑器头部位置改变*/
    $(".editor-page").scroll(function() {
        if($(".editor-page").scrollTop() > 135) {
            $(".edui-toolbar").addClass("position-toolbar");
            if($(".edui-toolbar").hasClass('hasDetail-toolbar')) {
                $(".edui-toolbar").addClass("hasDetail-position-toolbar");
            }
        }else {
            $(".edui-toolbar").prev().attr("style","");
            $(".edui-toolbar").attr("style","");
            $(".edui-toolbar").removeClass("position-toolbar")
        }
    });
    /* 新闻文稿引导*/
    $scope.manuscriptsNext = function() {
        $('.out-manuscripts .tips').hide();
        $('.out-manuscripts .data').show();
    }
    $scope.dataPrev = function() {
        $('.out-manuscripts .data').hide();
        $('.out-manuscripts .tips').show();
    }
    $scope.dataNext = function() {
        $('.out-manuscripts .data').hide();
        $('.out-manuscripts .selected-top').show();
    }
    $scope.selectedTopPrev = function() {
        $('.out-manuscripts .data').hide();
        $('.out-manuscripts .manuscripts-tips').show();
        $('.out-manuscripts .tips').show();
    }
    $scope.closeDialog = function() {
        finishedFunction($scope, $http);
    }
    $scope.finishedClick = function() {
        finishedFunction($scope, $http);
    }
    $scope.showSectionModel = function () {
        $('.section-model').css({'display':'block'});
        for(var i = 1;i<=maxLevel;i++){
            $('#section-model-'+i).hide();
        }
        cancelBubble();
    }
    $scope.clearSectionModel = function (e) {
        e.preventDefault();
        addList = [];
        $scope.tagIds = '';
        _.forEach(sectionList, function(v, i) {
            v.isHave = false;
        })
        $(('#section-model-0 .add-section-box')).removeClass("active");
        saveSectionAddList($scope, $http);
        $scope.clearClick = false;
        getSectionList($scope, $http);
        for(var i = 1;i<=maxLevel;i++){
            $('#section-model-'+i).hide();
        }
    }
    $scope.checkboxAddValue = function (outindex,interindex,value) {
        var hasValue = false;
        for(var i = 0;i<$scope.comitLebelList[outindex].tagValue.length;i++){
            if($scope.comitLebelList[outindex].tagValue[i] == value){
                $scope.comitLebelList[outindex].tagValue.splice(i,1);
                hasValue = true;
            }
        }
        if(hasValue == false){
            $scope.comitLebelList[outindex].tagValue.push(value);
        }
    }
    $scope.changeSelect = function (t) {
        $scope.comitLebelList[t.$index].tagValue = $scope.dropSelect[t.$index];
    }
    $scope.changeText = function (t) {
        $scope.comitLebelList[t.$index].tagValue = t.item.tagValue;
    }
    $scope.chagnePro = function (t,i) {
        if($scope.LebelList[i].tagValue[2] && $scope.LebelList[i].tagValue[2].length!=0){
            $scope.LebelList[i].tagValue[2].splice(0,$scope.LebelList[i].tagValue[2].length)
        }
        $scope.comitLebelList[t.$index].tagValue[0] = $scope.location[t.$index].pid;
        getLebelCity($scope, $http,t.$index,$scope.location[t.$index].pid); 
    }
    $scope.chagneCity = function (t) {
        $scope.comitLebelList[t.$index].tagValue[1] = $scope.location[t.$index].cid;
        getLebelArea($scope, $http,t.$index,$scope.location[t.$index].cid);
    }
    $scope.chagneArea = function (t) {
        $scope.comitLebelList[t.$index].tagValue[2] = $scope.location[t.$index].aid;
    }
    $scope.dateFocus = function (index) {
        setTimeout(function(){
                    laydate({
                            elem: '#time'+index,
                            format:"YYYY-MM-DD hh:mm:ss",
                            istime: true,
                            isclear: false,
                            choose:function(v){
                                $scope.comitLebelList[index].tagValue = v;
                                $scope.LebelList[index].tagValue = v;
                            }
                    });   
                },0)
    }
    $('.editor-page').click(function(){
        if($scope.tagShow && ($('.section-model').css('display') == 'block')){
            $scope.tagIds = '';
            _.map(addList,function(item){
                $scope.tagIds += (item.id +',')
            })
            $('.section-model').css({'display':'none'});
            saveSectionAddList($scope, $http);
        }
    });
    $scope.$on('$destroy',function(){
        $('.editor-page').unbind("click");
        clearInterval(videoTimer)
        if(typeof autoSaveTimer != "undefined"){
            $interval.cancel(autoSaveTimer);
        }
    })
};
var getLebelCity = function ($scope, $http, index, id) {
    bjj.http.ng.get($scope, $http, '/api/tags/area', {
        parentId:id
    }, function (res) {
        $scope.LebelList[index].tagValue[1] = res.list;
        $scope.location[index].cid = '';
    },function(res) {

    });
}
var getLebelArea = function ($scope, $http, index, id) {
    bjj.http.ng.get($scope, $http, '/api/tags/area', {
        parentId:id
    }, function (res) {
        $scope.LebelList[index].tagValue[2] = res.list;
        $scope.location[index].aid = '';
    },function(res) {

    });
}
/*获取栏目信息list*/
var level = [];//分类层级
var maxLevel = 0;//最后一级分类
var sectionList = [];//分类总数据
var addList = [];//已选择分类数据
//请求分类数据接口
var getSectionList = function ($scope, $http) {
    bjj.http.ng.get($scope, $http, '/api/tag/list', {}, function (res) {
        if(res.list.length == 0){
            $scope.tagShow = false;
        }else{
            $scope.tagShow = true;
            sectionList = res.list;
            mapSectionList(sectionList,$scope, $http);
            if(addList.length == 0){
                $('.editor-cont-center .editor-page .editor-page-bottom .container-section .form-control').html('<div>栏目选择</div>');
            }
            _.map(addList,function(item){
                $scope.tagIds += (item.id +',')
            })
            maxLevel = _.max(level);
            if($scope.clearClick) {
                var html = '<div class="section-model" id="section-model-0" style="left:15px">'+
                    '</div>';
                $('.container-section').append(html);
                for(var i= 0;i<sectionList.length;i++){
                if(sectionList[i].leaf == true){
                    $('#section-model-0').append('<div onclick=changeSection('+i+','+'0'+','+JSON.stringify(sectionList[i])+') class="add-section-box bjj-checkbox"><i class="fa fa-square-o fa-lg last-i" aria-hidden="true"></i>'+sectionList[i].userTagName+'</div>');
                    for(var j=0;j<addList.length;j++){
                        if(sectionList[i].id == addList[j].id){
                            $(('#section-model-'+'0'+' .add-section-box:eq('+ i +')')).addClass("active");
                        }
                    }  
                }else{
                    if(sectionList[i].userTagName.length>11){
                        $(('#section-model-0')).append('<div onclick=chooseSection('+JSON.stringify(sectionList[i])+') class="add-section-box">'+sectionList[i].userTagName.substring(0,10)+'...'+'<i class="fa fa-angle-right not-last-i" aria-hidden="true"></i></div>');
                    }else{
                        $(('#section-model-0')).append('<div onclick=chooseSection('+JSON.stringify(sectionList[i])+') class="add-section-box">'+sectionList[i].userTagName+'<i class="fa fa-angle-right not-last-i" aria-hidden="true"></i></div>');
                    }
                }   
            }
            }
            
            $('.section-model').css({'display':'none'});
        }
    },function(res) {
        bjj.dialog.alert('danger', res.msg);
    });
}
//初始遍历分类数据,得到层级数组,渲染已选分类数据
function mapSectionList(obj,$scope, $http) {
    if (obj.length!=0) {
        for (var i = 0; i < obj.length; i++) {
            if(obj[i].isHave){
                addList.push(obj[i]);
                $('.editor-cont-center .editor-page .editor-page-bottom .container-section .form-control').append('<span>'+ obj[i].userTagName +'<button type="button" class="close" data-dismiss="modal" aria-hidden="true" onclick=removeSection('+JSON.stringify(obj[i])+')>×</button></span>')
            }
            if (!obj[i].leaf) {
                mapSectionList(obj[i].subTagList,$scope, $http)
            }
        }
        level.push(obj[0].level);
    }   
}
//保存分类数据
var saveSectionAddList = function ($scope, $http) {
    bjj.http.ng.post($scope, $http, '/api/tag/history', {
        tagIds:$scope.tagIds
    }, function (res) {

    },function(res) {

    });
}
//点击得到下级分类,并渲染到页面
function chooseSection (item) {
    for(var n = item.level;n<=maxLevel;n++){
        $(('#section-model-'+n)).remove();
    }
    if(!item.leaf){
        var html = '<div class="section-model section-model-'+ item.code +'" id="section-model-'+ item.level +'" style="left:'+(15+(item.level)*185)+'px">'+
            '</div>';
        $('.container-section').append(html);
        for(var i= 0;i<item.subTagList.length;i++){
            if(!item.subTagList[i].leaf){
                if(item.subTagList[i].userTagName.length>11){
                    $(('#section-model-'+item.level)).append('<div onclick=chooseSection('+JSON.stringify(item.subTagList[i])+') class="add-section-box">'+item.subTagList[i].userTagName.substring(0,10)+'...'+'<i class="fa fa-angle-right not-last-i" aria-hidden="true"></i></div>');
                }else{
                    $(('#section-model-'+item.level)).append('<div onclick=chooseSection('+JSON.stringify(item.subTagList[i])+') class="add-section-box">'+item.subTagList[i].userTagName+'<i class="fa fa-angle-right not-last-i" aria-hidden="true"></i></div>');
                }
            }else{
                $(('#section-model-'+item.level)).append('<div onclick=changeSection('+i+','+item.level+','+JSON.stringify(item.subTagList[i])+') class="add-section-box bjj-checkbox"><i class="fa fa-square-o fa-lg last-i" aria-hidden="true"></i>'+item.subTagList[i].userTagName+'</div>');
                for(var j=0;j<addList.length;j++){
                    if(item.subTagList[i].id == addList[j].id){
                        $(('#section-model-'+item.level+' .add-section-box:eq('+ i +')')).addClass("active");
                    }
                }
            }
        }
    }
    cancelBubble();
}
//单个分类添加或删除的递归遍历方法
function changeSectionTag(obj ,id) {
    if (obj) {
        for (var i = 0; i < obj.length; i++) {
            if(obj[i].isHave){
                addList.push(obj[i]);
            }
            if(id == obj[i].id){
                obj[i].isHave = !obj[i].isHave;
            }
            if (!obj[i].leaf) {
                changeSectionTag(obj[i].subTagList,id)
            }
        }
    }
    cancelBubble();
}
//x删除分类的递归遍历方法
function removeSectionTag(obj ,id) {
    if (obj) {
        for (var i = 0; i < obj.length; i++) {
            if(id == obj[i].id){
                obj[i].isHave = false;
                if(obj[i].level == 1){
                    $(('#section-model-0'+' .add-section-box:eq('+ i +')')).removeClass("active");
                }
                $(('.section-model-'+(obj[i].code.substr(0,obj[i].code.length-4))+' .add-section-box:eq('+ i +')')).removeClass("active");
            }
            if(obj[i].isHave){
                addList.push(obj[i]);
            }
            if (!obj[i].leaf) {
                removeSectionTag(obj[i].subTagList,id)
            }
        }
    }
}
//点击单个分类添加或删除
function changeSection (i,level,item) {
    if($(('#section-model-'+level+' .add-section-box:eq('+ i +')')).hasClass("active")){
        $(('#section-model-'+level+' .add-section-box:eq('+ i +')')).removeClass("active");
    }else{
        $(('#section-model-'+level+' .add-section-box:eq('+ i +')')).addClass("active");
    }
    addList = [];
    $('.editor-cont-center .editor-page .editor-page-bottom .container-section .form-control').empty();
    changeSectionTag(sectionList,item.id);
    var indexOfId = addList.indexOfId(item.id)
    if(indexOfId>-1){
        addList.splice(indexOfId,1);
    }else{
        addList.push(item);
    }
    if(addList.length>50){
        addList = [];
        removeSectionTag(sectionList,item.id)
        bjj.dialog.alert('danger', '最多设置50个');
    }

    for(var i=0;i<addList.length;i++){
        $('.editor-cont-center .editor-page .editor-page-bottom .container-section .form-control').append('<span>'+ addList[i].userTagName +'<button type="button" class="close" data-dismiss="modal" aria-hidden="true" onclick=removeSection('+JSON.stringify(addList[i])+')>×</button></span>')
    }
    if(addList.length == 0){
        $('.editor-cont-center .editor-page .editor-page-bottom .container-section .form-control').append('<div>栏目选择</div>');
    }
}
//点击x
function removeSection(item) {
    addList = [];
    $('.editor-cont-center .editor-page .editor-page-bottom .container-section .form-control').empty();
    removeSectionTag(sectionList,item.id);
    for(var i=0;i<addList.length;i++){
        $('.editor-cont-center .editor-page .editor-page-bottom .container-section .form-control').append('<span>'+ addList[i].userTagName +'<button type="button" class="close" data-dismiss="modal" aria-hidden="true" onclick=removeSection('+JSON.stringify(addList[i])+')>×</button></span>')
    }
    var tagIds = '';
    _.map(addList,function(item){
        tagIds += (item.id +',')
    })
    if(addList.length == 0){
        $('.editor-cont-center .editor-page .editor-page-bottom .container-section .form-control').append('<div>栏目选择</div>');
    }
    bjj.http.jq.post('/api/tag/history', {
        tagIds:tagIds
    }, function (res) {

    },function(res) {

    });
    cancelBubble();
}
//阻止冒泡方法
function cancelBubble(e) {
    var evt = e ? e : window.event;
    if (evt.stopPropagation) {        //W3C
        evt.stopPropagation();
    }else {       //IE
        evt.cancelBubble = true;
    }
}
var objDeepCopy = function (source) {
    var sourceCopy = source instanceof Array ? [] : {};
    for (var item in source) {
        sourceCopy[item] = typeof source[item] === 'object' ? objDeepCopy(source[item]) : source[item];
    }
    return sourceCopy;
}
/* 自定义标签 */
let getLebelList = function ($scope, $http) {
    bjj.http.ng.get($scope, $http, '/api/tags/list', {}, function (res) {
        $scope.LebelList = res.list;
        $scope.location = [];
        $scope.dropSelect = [];
        $scope.comitLebelList = objDeepCopy($scope.LebelList);
            for(var i = 0;i<$scope.comitLebelList.length;i++){
                if($scope.comitLebelList[i].tagType == 'drop'){
                    $scope.dropSelect[i] = '';
                }
                if($scope.comitLebelList[i].tagType == 'drop' || $scope.comitLebelList[i].tagType == 'date' || $scope.comitLebelList[i].tagType == 'text'){
                    $scope.comitLebelList[i].tagValue = '';
                }else{
                    $scope.comitLebelList[i].tagValue = [];
                }
                if($scope.comitLebelList[i].tagType == 'area'){
                    $scope.location[i] = {};
                    $scope.location[i].pid = '';
                    $scope.location[i].cid = '';
                    $scope.location[i].aid = '';
                }
            }      
         bjj.http.ng.get($scope, $http, '/api/tags/area', {}, function (resp) {
                    for(var m = 0;m<$scope.LebelList.length;m++){
                        if($scope.LebelList[m].tagType == 'area'){
                            $scope.LebelList[m].tagValue[0] = resp.list;
                        }
                    }
        });
         getCustomTags($scope, $http);
    },function(res){
        bjj.dialog.alert('danger', res.msg);
    });
};
/* 新闻文稿引导*/
var finishedFunction = function ($scope, $http) {
    var setting = { expires: 1000, path: '/' };
    bjj.http.ng.post($scope, $http, '/api/account/compile/initialization', {}, function (res) {
        $scope.isBriefBed = false;
        $.cookie('isCompileInited',   true, setting);
    },function(res) {
        bjj.dialog.alert('danger', res.msg);
    });
};
// 样式数据列表
var getStyleList = function ($scope, $http) {
    bjj.http.ng.get($scope, $http, '/api/compile/styles', {}, function (res) {
        $scope.styleList = res.msg;
        bindSortable4StyleList();
    });
};
// 素材图片数据列表
var getImgLibList = function ($rootScope, $scope, $http) {
    bjj.http.ng.get($scope, $http, '/api/compile/imgLibrary/imgs', {
        pageSize        : 5000,
        pageNo          : $scope.imgLibPageInfo.pno,
        type            : $scope.typeUpload
    }, function (res) {
        //imgLibPageInfoCallback($scope, list);
        //imgLibPageInfoInitScroll($scope, $http);
        if($scope.typeUpload == 'img') {
            $scope.imgLibList = res.list;
            setTimeout(function () {
                _.forEach($scope.imgLibList, function(v, i){
                    new Viewer(document.getElementById('sucai-list-img-viewer-' + v.id), { title: false, toolbar: false, navbar: false });
                });
            }, 0);
        }else {
            _.forEach(res.list, function(v,i) {
                if(v.videoState == 2) {
                    v.videoStateText = '转码中...'
                } else if(v.videoState == 4) {
                    v.videoStateText = '转码失败'
                }
            })
            $rootScope.shipinList = res.list;
        }
    });
};
var getNews = function ($scope, $http, newsId, editor) {
    bjj.http.ng.get($scope, $http, '/api/capture/news/' + newsId, {
        style: 0
    }, function (res) {
        var data = res.msg;
        $scope.imgInfo = {
            imgUrls: data.imgUrls,
            hasImg: data.hasImg
        };
        $scope.material = {
            title: data.title.substr(0, 64),
            author: data.author.substr(0, 8),
            picUrl: data.picUrl ? data.picUrl : _defaultCover,
            source: data.siteName,
            keywords: data.keywords.join(' '),
            content: data.content,
            contentAbstract: data.contentAbstract.substr(0, 120),
            classification: data.classification.join(' '),
        };

        var content = $scope.material.content;
        if(data.isHtmlContent) {
        }else{
            _.forEach(data.imgUrls, function (v, i) {
                content += '<img src="' + v + '">\n';
            });
        }
        editor.setContent(content + '<p><br></p>');
    }, function (res) {
        bjj.dialog.alert('danger', res.msg);
    });
};
var getCustomTags = function ($scope, $http) {
    $scope.location = [];
    if($scope.material.customTags && $scope.material.customTags.length!=0){
        for(var n = 0;n<$scope.material.customTags.length;n++){
            if($scope.material.customTags[n].tagValue){
                $scope.comitLebelList[n].tagValue = $scope.material.customTags[n].tagValue;
            }else{
                if($scope.material.customTags[n].tagType == 'drop' || $scope.material.customTags[n].tagType == 'date' || $scope.material.customTags[n].tagType == 'text'){
                    $scope.comitLebelList[n].tagValue = '';
                }else{
                    $scope.comitLebelList[n].tagValue = [];
                }
                $scope.comitLebelList[n].tagValue = [];
            }        
                    if($scope.material.customTags[n].tagType == 'area'){
                        if($scope.material.customTags[n].tagValue){
                           $scope.location[n] = {
                            pid:'',
                            cid:'',
                            aid:''
                           };
                           if($scope.material.customTags[n].tagValue[0]){
                                $scope.location[n].pid = $scope.material.customTags[n].tagValue[0];
                                $scope.comitLebelList[n].tagValue[0] = $scope.material.customTags[n].tagValue[0];
                           }
                           if($scope.material.customTags[n].tagValue[1]){
                                $scope.location[n].cid = $scope.material.customTags[n].tagValue[1];
                                $scope.comitLebelList[n].tagValue[1] = $scope.material.customTags[n].tagValue[1];
                           }
                           if($scope.material.customTags[n].tagValue[2]){
                                $scope.location[n].aid = $scope.material.customTags[n].tagValue[2];
                                $scope.comitLebelList[n].tagValue[2] = $scope.material.customTags[n].tagValue[2];
                           }     
                        }  
                    }else if($scope.material.customTags[n].tagType == 'text' || $scope.material.customTags[n].tagType == "date"){
                        $scope.LebelList[n].tagValue = $scope.material.customTags[n].tagValue;
                    }else if($scope.material.customTags[n].tagType == 'drop'){
                        if($scope.material.customTags[n].tagValue){
                            $scope.dropSelect[n] = $scope.material.customTags[n].tagValue;
                        }   
                    }
                }
        }
                for(var z = 0;z<$scope.comitLebelList.length;z++){
                    if($scope.comitLebelList[z].tagType == 'area'){
                        if($scope.comitLebelList[z].tagValue[0]){
                            getLebelCity($scope,$http,z,$scope.comitLebelList[z].tagValue[0])      
                        }
                        if($scope.comitLebelList[z].tagValue[1]){
                            getLebelArea($scope,$http,z,$scope.comitLebelList[z].tagValue[1]) 
                        } 
                    }     
                }
}
var getMaterial = function ($scope, $http, materialId, editor) {
    bjj.http.ng.get($scope, $http, '/api/compile/material/' + materialId, {}, function (res) {
        $scope.imgInfo = {hasImg: false};
        $scope.material = res.msg;
        $scope.material.customTags = res.msg.customTags;
        $scope.material.picUrl = res.msg.picUrl ? res.msg.picUrl : _defaultCover;
        var content = $scope.material.content;
        editor.setContent('<p>' + content + '</p><p><br></p>');
    }, function (res) {
        bjj.dialog.alert('danger', res.msg);
    });
};
var getChannelList = function ($scope, $http) {
    bjj.http.ng.get($scope, $http, '/api/share/channels', {}, function (res) {
        $scope.channelList = res.list;
        _.map($scope.channelList, function (v) {
            v.active = true;
        });
        $scope.selectedChannelList = _.filter($scope.channelList, {'active': true});
    });
};
var getReport = function ($scope, $http, $interval) {
    bjj.http.ng.get($scope, $http, '/api/compile/report', {}, function (res) {
        $scope.report = res.msg;
        $scope.report.keywords = res.msg.keyWords;
        $scope.report.timeRange = res.msg.timeRange + '';
        $scope.reportCreateCount = res.count;
        if($scope.report.status == 1 || $scope.report.status == 2) {
            if(res.count > 0) {
                $scope.reportCreateCount = res.count - 1;
            }
        }
        dealReportProgress($scope, $http, $interval);
    }, function (res) {
        $scope.report = {
            id          : 0,
            name        : '',
            keywords    : '',
            timeRange   : '7',
            status      : 1
        }
    });
};
var dealReportProgress = function ($scope, $http, $interval) {
    if($scope.report.status == 1 || $scope.report.status == 2) {
        // 隐藏生成按钮  展示进度条
        reportStatusDom($scope, false, true, false);
        //获取进度条信息
        getReportProgress($scope, $http);
        var reportTimer = $interval(function () {
            if ($scope.progress.reportStatus == 3 && $scope.progress.value >= 100) {
                reportStatusDom($scope, true, false, false);
                $interval.cancel(reportTimer);
                if($scope.reportCreateCount < 5){
                    $scope.reportCreateCount += 1;
                }
                getReportEchartsData($scope, $http);
                return;
            } else if($scope.progress.reportStatus == 4){
                reportStatusDom($scope, true, false, true);
                $interval.cancel(reportTimer);
                return;
            }
            getReportProgress($scope, $http);
        }, 5000);
    } else if($scope.report.status == 3) {
        getReportEchartsData($scope, $http);
    } else if($scope.report.status == 4) {
        getReportProgress($scope, $http);
        reportStatusDom($scope, true, false, true);
    }
};
var getReportProgress = function ($scope, $http) {
    bjj.http.ng.get($scope, $http, '/api/compile/report/' + $scope.report.id + '/progressRate', {}, function (res) {
        if(res.reportStatus == 4){
            reportStatusDom($scope, true, false, true);
        }
        $scope.progress = {
            value           : res.progressRate,
            reportStatus    : res.reportStatus,
            message         : res.msg
        }
    }, function (res) {
    });
};
var reportStatusDom = function ($scope, _reportCreateBtn, _reportProgressBar, _reportResultMsg) {
    $scope.reportCreateBtn = _reportCreateBtn;
    $scope.reportProgressBar = _reportProgressBar;
    $scope.reportResultMsg = _reportResultMsg;
    if(!_reportProgressBar){
        $scope.progress.value = 0;
    }
};
var getReportEchartsData = function ($scope, $http) {
    var reportId = $scope.report.id;
    if(reportId == undefined || reportId == '') return;
    ['sourceDistribution', 'spreadTrend', 'siteRank', 'bloggerDistribution', 'weiboRank'].forEach(function (v, i, t) {
        bjj.http.ng.get($scope, $http, '/api/compile/report/' + reportId + '/' + v, {}, function (res) {
            var result = res.msg.chartData;
            switch (v) {
                case 'sourceDistribution':
                    result = _.sortBy(result, function (v) { return v.count; });
                    var totalCount = _.sum(_.map(result, 'count'));

                    var legendDataName = function (item, totalCount) {
                        return item.sourceTypeName + ' ' + (item.count * 100 / totalCount).toFixed(2) + '%';
                    };
                    var legendData = _.map(result, function (v) {
                        return legendDataName(v, totalCount);
                    });
                    var seriesData = _.map(result, function (v) {
                        return {
                            name    : legendDataName(v, totalCount),
                            value   : v.count
                        };
                    });
                    drawSourceDistribution($scope, legendData, seriesData);
                    break;
                case 'bloggerDistribution':
                    result = _.sortBy(result, 'count');
                    var legendData = ['发稿量'];
                    var seriesDataArea = _.map(result, function (v) {
                        return {
                            name    : v.province,
                            value   : v.count
                        };
                    });

                    if(result.length > 10){
                        result = _.slice(result, result.length - 10, result.length)
                    }
                    var seriesDataRank = _.map(result, 'count');
                    var seriesObjRank = [{
                        name: legendData[0],
                        type: 'bar',
                        data: seriesDataRank,
                        barWidth: '60%',
                        label: {
                            normal: {
                                show: true
                            }
                        }
                    }];
                    var xAxisDataRank = _.map(result, 'province');
                    drawBloggerDistribution($scope, legendData, seriesDataArea);
                    drawBloggerRank($scope, legendData, seriesObjRank, xAxisDataRank);
                    break;
                case 'spreadTrend':
                    result = _.sortBy(result, function (v) { return v.date; });
                    var siteLegendData = ['网站'],
                        mediaLegendData = ['社交媒体'],
                        legendData = siteLegendData.concat(mediaLegendData);
                    var siteSeriesData = _.map(result, 'website'),
                        mediaSeriesData = _.map(result, 'socialMedia');
                    var siteSeriesObj = [{
                            name: siteLegendData[0],
                            type: 'line',
                            smooth: true,
                            itemStyle: {normal: {areaStyle: {type: 'default'}}},
                            data: siteSeriesData
                        }],
                        mediaSeriesObj = [{
                            name: mediaLegendData[0],
                            type: 'line',
                            smooth: true,
                            itemStyle: {normal: {areaStyle: {type: 'default'}}},
                            data: mediaSeriesData
                        }],
                        seriesObj = siteSeriesObj.concat(mediaSeriesObj);
                    var xAxisData = _.map(result, function (v) {
                        return new Date(v.date).Format('M月d日');
                    });
                    drawSpreadTrend($scope, legendData, seriesObj, xAxisData);
                    drawSiteSpreadTrend($scope, siteLegendData, siteSeriesObj, xAxisData);
                    drawBloggerSpreadTrend($scope, mediaLegendData, mediaSeriesObj, xAxisData);
                    break;
                case 'siteRank':
                    result = _.sortBy(result, 'count');
                    var legendData = ['发稿量'];
                    var seriesData = _.map(result, 'count');
                    var seriesObj = [{
                        name: legendData[0],
                        type: 'bar',
                        data: seriesData,
                        barWidth: '60%',
                        label: {
                            normal: {
                                show: true
                            }
                        }
                    }];
                    var xAxisData = _.map(result, 'siteName');
                    drawSiteRank($scope, legendData, seriesObj, xAxisData);
                    break;
                case 'weiboRank':
                    result = _.sortBy(result, 'count');
                    var legendData = ['粉丝量'];
                    var seriesData = _.map(result, 'count');
                    var seriesObj = [{
                        name: legendData[0],
                        type: 'bar',
                        data: seriesData,
                        barWidth: '60%',
                        label: {
                            normal: {
                                show: true
                            }
                        }
                    }];
                    var xAxisData = _.map(result, 'blogger');
                    drawWeiboRank($scope, legendData, seriesObj, xAxisData);
                    break;
            }
        }, function (res) {
            console.log(v + ':::' + res.msg);
        });
    });
    bindSortable4EchartList();
};
var bindSortable4FavoriteNewsList = function (groupId) {
    Sortable.create(document.getElementById('favoriteNewsListCont' + groupId), {
        group: {
            name: 'favoriteNewsListCont' + groupId,
            pull: 'clone'
        },
        animation: 100,
        handle: ".section-item",
        sort: true
    });
};
var bindSortable4StyleList = function () {};
var bindSortable4EchartList = function () {};
var bindSortable4Editor = function ($scope, $http) {
    Sortable.create(editor, {
        group: {
            name: 'editor',
            put: $scope.sortableSource
        },
        disabled: false,
        animation: 100,
        draggable: "p",
        sort: false,
        onAdd: function (evt) {
            var _evt = evt;
            var _this = evt.item.dataset;
            var _id = _this.id;
            var _dom = $('.' + _this.listType + '-cont-' + _id);

            if(_this.listType == 'favoriteNews'){
                bjj.http.ng.get($scope, $http, '/api/capture/news/operation/' + _id, {}, function (res) {
                    var html = res.msg.news.content;
                    if(_.isEmpty($scope.material.keywords)){
                        $scope.material.keywords = _dom.data('newsKeyword');
                    }
                    if(_.isEmpty($scope.material.classification)){
                        $scope.material.classification = _dom.data('newsClassification');
                    }
                    html += "<br/><p></p>";
                    _evt.item.outerHTML = html;
                })
            }else if( _this.listType == 'recommendNews'){
                var html = _dom.html();
                if(_.isEmpty($scope.material.keywords)){
                    $scope.material.keywords = _dom.data('newsKeyword');
                }
                if(_.isEmpty($scope.material.classification)){
                    $scope.material.classification = _dom.data('newsClassification');
                }
                html += "<br/><p></p>";
                _evt.item.outerHTML = html;
            }
        }
    });
};

var graphic = function ($scope) {
    return [{
        type: 'group',
        bounding: 'raw',
        left: 210,
        bottom: 20,
        z: 100,
        children: [{
            type: 'text',
            z: 100,
            style: {
                fill: '#999',
                text: '* 该数据来源于' + $scope.appInfo.appName + '，仅供参考',
                fontSize: 13
            }
        }]
    }];
};
var titleTextStyle = {
    fontWeight: 'normal'
};
var createEchartsImg = function (_echart, _container) {
    var img = document.createElement("img");
    document.getElementById(_container).innerHTML = '';
    document.getElementById(_container).appendChild(img);
    setTimeout(function () {
        img.src = _echart.getDataURL({
            pixelRatio: 2,
            backgroundColor: '#fff'
        });
    }, 100);
    $('[data-id=' + _container + ']').fadeIn();
};
var drawSourceDistribution = function ($scope, legendData, seriesData) {
    var myChart = echarts.init(document.getElementById('sourceDistribution-hide'));
    myChart.setOption({
        graphic: graphic($scope),
        animation: false,
        title : {
            text: '来源分析',
            textStyle: titleTextStyle,
            x:'center'
        },
        color: ["#fb0350", "#f4472f", "#fc9c35", "#fdae1e ", "#fde72d", "#92de29", "#d0e402", "#5793f3", "#259ad6", "#1767f9", "#6c2fc5", "#9135cd", "#fb177c"],
        tooltip : {
            trigger: 'item',
            formatter: "{a} <br/>{b} : {c} ({d}%)"
        },
        legend: {
            orient: 'vertical',
            left: 'left',
            data: legendData
        },
        series : [
            {
                name: '访问来源',
                type: 'pie',
                radius : '55%',
                center: ['50%', '60%'],
                data: seriesData,
                itemStyle: {
                    emphasis: {
                        shadowBlur: 10,
                        shadowOffsetX: 0,
                        shadowColor: 'rgba(0, 0, 0, 0.5)'
                    }
                }
            }
        ]
    });
    createEchartsImg(myChart, 'sourceDistribution');
};
var drawSpreadTrend = function ($scope, legendData, seriesObj, xAxisData) {
    var myChart = echarts.init(document.getElementById('spreadTrend-hide'));
    myChart.setOption({
        graphic: graphic($scope),
        animation: false,
        title : {
            text: '传播走势',
            textStyle: titleTextStyle,
        },
        color: ["#5793f3", "#fc9c35"],
        tooltip : {
            trigger: 'axis'
        },
        legend: {
            data: legendData
        },
        toolbox: {
            show : false
        },
        calculable : true,
        xAxis : [
            {
                type : 'category',
                boundaryGap : false,
                data : xAxisData
            }
        ],
        yAxis : [
            {
                type : 'value'
            }
        ],
        series : seriesObj
    });
    createEchartsImg(myChart, 'spreadTrend');
};
var drawSiteSpreadTrend = function ($scope, legendData, seriesObj, xAxisData) {
    var myChart = echarts.init(document.getElementById('siteSpreadTrend-hide'));
    myChart.setOption({
        graphic: graphic($scope),
        animation: false,
        title : {
            text: '网站传播走势',
            textStyle: titleTextStyle,
        },
        color: ["#5793f3", "#fc9c35"],
        tooltip : {
            trigger: 'axis'
        },
        legend: {
            data: legendData
        },
        toolbox: {
            show : false
        },
        calculable : true,
        xAxis : [
            {
                type : 'category',
                boundaryGap : false,
                data : xAxisData
            }
        ],
        yAxis : [
            {
                type : 'value',
                axisLabel : {
                    formatter: '{value}'
                }
            }
        ],
        series : seriesObj
    });
    createEchartsImg(myChart, 'siteSpreadTrend');
};
var drawBloggerSpreadTrend = function ($scope, legendData, seriesObj, xAxisData) {
    var myChart = echarts.init(document.getElementById('bloggerSpreadTrend-hide'));
    myChart.setOption({
        graphic: graphic($scope),
        animation: false,
        title : {
            text: '社交媒体传播走势',
            textStyle: titleTextStyle,
        },
        color: ["#5793f3", "#fc9c35"],
        tooltip : {
            trigger: 'axis'
        },
        legend: {
            data: legendData
        },
        toolbox: {
            show : false
        },
        calculable : true,
        xAxis : [
            {
                type : 'category',
                boundaryGap : false,
                data : xAxisData
            }
        ],
        yAxis : [
            {
                type : 'value',
                axisLabel : {
                    formatter: '{value}'
                }
            }
        ],
        series : seriesObj
    });
    createEchartsImg(myChart, 'bloggerSpreadTrend');
};
var drawSiteRank = function ($scope, legendData, seriesObj, xAxisData) {
    var myChart = echarts.init(document.getElementById('siteRank-hide'));
    myChart.setOption({
        graphic: graphic($scope),
        animation: false,
        title : {
            text: '媒体排行',
            textStyle: titleTextStyle,
            subtext: '发稿量'
        },
        toolbox: {
            show : false
        },
        color: ['#2ec7c9'],
        grid: {
            left: '3%',
            right: '4%',
            bottom: '10%',
            containLabel: true
        },
        xAxis : [
            {
                type : 'value'
            }
        ],
        yAxis : [
            {
                type : 'category',
                data : xAxisData,
                axisTick: {
                    alignWithLabel: true
                }
            }
        ],
        series : seriesObj
    });
    createEchartsImg(myChart, 'siteRank');
};
var drawWeiboRank = function ($scope, legendData, seriesObj, xAxisData) {
    var myChart = echarts.init(document.getElementById('weiboRank-hide'));
    myChart.setOption({
        graphic: graphic($scope),
        animation: false,
        title : {
            text: '微博意见领袖',
            textStyle: titleTextStyle,
            subtext: '粉丝量'
        },
        toolbox: {
            show : false
        },
        color: ['#fc9c35'],
        grid: {
            left: '3%',
            right: '4%',
            bottom: '10%',
            containLabel: true
        },
        xAxis : [
            {
                type : 'value'
            }
        ],
        yAxis : [
            {
                type : 'category',
                data : xAxisData,
                axisTick: {
                    alignWithLabel: true
                }
            }
        ],
        series : seriesObj
    });
    createEchartsImg(myChart, 'weiboRank');
};
var drawBloggerDistribution = function ($scope, legendData, seriesData) {
    var myChart = echarts.init(document.getElementById('bloggerDistribution-hide'));
    myChart.setOption({
        graphic: graphic($scope),
        animation: false,
        title : {
            text: '网民声量地域分布',
            textStyle: titleTextStyle,
            x:'center'
        },
        color: ["#5793f3", "#5578c2"],
        tooltip : {
            trigger: 'item',
            formatter: '{b}'
        },
        legend: {
            orient: 'vertical',
            x:'left',
            data: legendData
        },
        dataRange: {
            min: 0,
            max: _.max(_.map(seriesData, 'value')),
            x: 'left',
            y: 'bottom',
            text:['高','低'],           // 文本，默认为数值文本
            calculable : true
        },
        toolbox: {
            show: false
        },
        roamController: {
            show: true,
            x: 'right',
            mapTypeControl: {
                'china': true
            }
        },
        series : [{
            name: legendData[0],
            type: 'map',
            mapType: 'china',
            roam: false,
            itemStyle:{
                normal:{label:{show:true}},
                emphasis:{label:{show:true}}
            },
            data:seriesData
        }]
    });
    createEchartsImg(myChart, 'bloggerDistribution');
};
var drawBloggerRank = function ($scope, legendData, seriesObj, xAxisData) {
    var myChart = echarts.init(document.getElementById('bloggerRank-hide'));
    myChart.setOption({
        graphic: graphic($scope),
        animation: false,
        title : {
            text: '网民声量地域排行',
            textStyle: titleTextStyle,
            subtext: '发稿量'
        },
        toolbox: {
            show : false
        },
        color: ['#2ec7c9'],
        grid: {
            left: '3%',
            right: '4%',
            bottom: '10%',
            containLabel: true
        },
        xAxis : [
            {
                type : 'value'
            }
        ],
        yAxis : [
            {
                type : 'category',
                data : xAxisData,
                axisTick: {
                    alignWithLabel: true
                }
            }
        ],
        series : seriesObj
    });
    createEchartsImg(myChart, 'bloggerRank');
};
var autoSaveTimer;
var startAutoSave = function ($scope, $interval, editor) {
    autoSaveTimer = $interval(function () {
        if(editor.hasContents()){
            console.log('auto save !');
            $scope.saveMaterial2Local();
        }
    }, 10000);
};
var startListenBack2Top = function () {
    var _container = $('.editor-page');
    _container.scroll(function () {
        var _backBtn = $('.editor-page .editor-page-tools .back-to-top');
        var _line = $('.editor-page .editor-page-tools .line');
        if(_container.scrollTop() > 200) {
            _backBtn.fadeIn();
            _line.css('display', 'block');
        } else {
            _backBtn.hide();
            _line.hide();
        }
    });
};
var imgLibPageInfoInit = function ($rootScope, $scope, $http) {
    $scope.imgLibPageInfo = {
        pno     : 1,
        psize   : 20,
        canLoad : true,
        dataList: [],
        dataListEmptyView : false
    };
    imgLibPageInfoInitScroll($rootScope, $scope, $http);
};
var imgLibPageInfoInitScroll = function ($rootScope, $scope, $http) {
    var _container = $('.section-list-sucai-view');
    var _scrollPage = $('.section-item-tupian-list');
    _container.scroll(function () {
        var totalHeight = parseFloat(_container.height()) + parseFloat(_container.scrollTop()) + 100;
        var docHeight = parseFloat(_scrollPage.height());
        if(docHeight <= totalHeight && $scope.imgLibPageInfo.canLoad) {
            $scope.imgLibPageInfo.canLoad = false;
            getImgLibList($rootScope, $scope, $http)
        }
    });
};
//图片分页加载
var imgLibPageInfoCallback = function ($scope, list) {
    var imgLibPageInfo = $scope.imgLibPageInfo;
    if(imgLibPageInfo.pno == 1) {
        imgLibPageInfo.dataList = [];
        imgLibPageInfo.dataListEmptyView = false;
    }
    if(list.length > 0) {
        imgLibPageInfo.canLoad = true;
        imgLibPageInfo.dataList = imgLibPageInfo.dataList.concat(list);
        imgLibPageInfo.pno += 1;
        if(list.length < imgLibPageInfo.psize) {
            imgLibPageInfo.canLoad = false;
        }
    } else {
        imgLibPageInfo.canLoad = false;
        if(imgLibPageInfo.pno != 1){
        } else {
            $scope.imgLibPageInfo.dataListEmptyView = true;
        }
    }
};
var groupPageInfoInit = function ($scope, $http, groupList) {
    _.forEach(groupList, function (v, i) {
        $scope.groupPageInfoMap[v.id] = {
            pno     : 1,
            psize   : 10,
            canLoad : true,
            dataList: [],
            dataListEmptyView : false
        };
    });
    groupPageInfoInitScroll($scope, $http);
};
var groupPageInfoInitScroll = function ($scope, $http) {
    var container = '.section-item-list', scrollPage = '.directories';
    var _container = $(container);
    var _scrollPage = $(scrollPage);
    _container.scroll(function () {
        var totalHeight = parseFloat(_container.height()) + parseFloat(_container.scrollTop()) + 100;
        var docHeight = parseFloat(_scrollPage.height());
        var groupId = '';
        var groupType = '';
        _.forEach($scope.directoryList, function (v) {
            _.forEach(v, function (w) {
                if(w.groupContShow){
                    groupId = w.id;
                    groupType = w.groupType;
                };
            });
        });
        if(groupId == ''){
            return;
        }
        if (docHeight <= totalHeight && $scope.groupPageInfoMap[groupId].canLoad) {
            $scope.groupPageInfoMap[groupId].canLoad = false;
            getDirectoryNewsList($scope, $http, groupId, groupType);
        }
    });
};
var groupPageInfoCallback = function ($scope, list, groupId) {
    var groupPageInfo = $scope.groupPageInfoMap[groupId];
    if(groupPageInfo.pno == 1) {
        groupPageInfo.dataList = [];
    }
    if(list.length > 0) {
        groupPageInfo.canLoad = true;
        groupPageInfo.dataList = groupPageInfo.dataList.concat(list);
        groupPageInfo.pno += 1;
        if(list.length < groupPageInfo.psize) {
            groupPageInfo.canLoad = false;
        }
    } else {
        groupPageInfo.canLoad = false;
        if(groupPageInfo.pno != 1){
        } else {
            $scope.groupPageInfoMap[groupId].dataListEmptyView = true;
        }
    }
};
/*获取文件夹列表*/
var getDirectoryList = function($rootScope, $scope, $http) {
    bjj.http.ng.get($scope, $http, '/api/favorite/group/list', {}, function (res) {
        // 更新全局group
        $rootScope.allGroup = res.list;
        for(var i = 0; i< $rootScope.allGroup.length; i++) {
            $rootScope.allGroup[i].active = true;
        }
        $rootScope.normalGroup = _.filter($rootScope.allGroup, {'groupType' : "0"});
        if(res.list.length > 0){
            groupPageInfoInit($scope, $http, res.list);

            res.list.push({
                groupName   : '添加分组',
                groupType   : -1
            });
            $scope.directoryList = _.chunk(res.list, 3);

            _.map(res.list, function (v, i) {
                v.groupContShow = false;
                $scope.sortableSource.push('favoriteNewsListCont' + v.id);
            });
            bindSortable4Editor($scope, $http);
        }
    }, function (res) {
        bjj.dialog.alert('danger', res.msg);
    });
};
/*获取文件夹中的列表*/
var getDirectoryNewsList = function($scope, $http, groupId, groupType) {
    var groupPageInfo = $scope.groupPageInfoMap[groupId];
    bjj.http.ng.get($scope, $http, '/api/favorite/group/showNewsOperations', {
        pageNo  : groupPageInfo.pno,
        pageSize: groupPageInfo.psize,
        groupId : groupId,
        groupType: groupType
    }, function(res) {
        $scope.isShowEmptyList = false;
        _.map(res.list, function (v, i) {
            v.title = v.news.title;
        });
        var delRepeat= function(channelType,arr){
            for(var g=0;g<arr.length;g++){
                if(channelType == arr[g]){
                    return true;
                }
            }
            return false;
        };
        for(var n=0;n<res.list.length;n++){
            if(!res.list[n].shareResult){continue}
            var list = res.list[n].shareResult;
            var arr = [];
            for(var k=0;k<list.length;k++){
                if(!delRepeat(list[k].channelType,arr)){
                    list[k].isshow = true;
                    arr.push(list[k].channelType)
                }else{
                    list[k].isshow = false;
                }
            }
        }
        groupPageInfoCallback($scope, res.list, groupId);
        groupPageInfoInitScroll($scope, $http);
        bindSortable4FavoriteNewsList(groupId);
    }, function(res) {
        bjj.dialog.alert('danger', res.msg);
    }, 'editorSidebarLeftLoading');
};
var addDirectory = function($rootScope, $scope, $http) {
    bjj.http.ng.put($scope, $http,'/api/favorite/group', {
        groupName: ''
    }, function(res) {
        res.isShowInput = true;
    }, function(res) {
        bjj.dialog.alert('danger', res.msg);
    });
};
var renameDirectoryFunction = function($scope, $http, groupId, groupName) {
    bjj.http.ng.post($scope, $http, '/api/favorite/group/' + groupId, {
        groupName : groupName == '' ? '' : groupName
    }, function(res) {
        console.log(res);
    });
};
var getTemplate = function($scope, $http) {
    bjj.http.ng.get($scope, $http, '/api/compile/templateNames', {}, function (res) {
        $scope.templateList = res.templateNameList;
        $scope.settingList = res.settingList;
        if(res.templateNameList.length == 1) {
            $scope.templateNameList = [res.templateNameList[0],'自定义1','自定义2'];
            $scope.templateDefault = {
                afterParagraphSpace  :$scope.settingList[0].afterParagraphSpace,
                beforeParagraphSpace :$scope.settingList[0].beforeParagraphSpace,
                bodyAlignment        :$scope.settingList[0].bodyAlignment,
                bodyFontSize         :$scope.settingList[0].bodyFontSize,
                bodyImgAlignment     :$scope.settingList[0].bodyImgAlignment,
                firstLineIndent      :$scope.settingList[0].firstLineIndent,
                lineSpace            :$scope.settingList[0].lineSpace,
                templateName         :$scope.settingList[0].templateName,
                textIndent           :$scope.settingList[0].textIndent,
                wordSpace            :$scope.settingList[0].wordSpace,
                bodyFontFamily       :$scope.settingList[0].bodyFontFamily
            }
        }else if(res.templateNameList.length >= 2) {
            $scope.templateNameList = [res.templateNameList[0],res.templateNameList[1],'自定义2'];
            $scope.customOne = {
                afterParagraphSpace  :$scope.settingList[1].afterParagraphSpace,
                beforeParagraphSpace :$scope.settingList[1].beforeParagraphSpace,
                bodyAlignment        :$scope.settingList[1].bodyAlignment,
                bodyFontSize         :$scope.settingList[1].bodyFontSize,
                bodyImgAlignment     :$scope.settingList[1].bodyImgAlignment,
                firstLineIndent      :$scope.settingList[1].firstLineIndent,
                lineSpace            :$scope.settingList[1].lineSpace,
                templateName         :$scope.settingList[1].templateName,
                textIndent           :$scope.settingList[1].textIndent,
                wordSpace            :$scope.settingList[1].wordSpace,
                bodyFontFamily       :$scope.settingList[1].bodyFontFamily
            }
            if($scope.customOne.firstLineIndent == 'on') {
                $(".one-toggle-button").prop("checked",true);
            }else {
                $(".one-toggle-button").prop("checked",false);
            }
            if(res.templateNameList.length == 3){
                $scope.templateNameList = res.templateNameList;
                $scope.customTwo = {
                    afterParagraphSpace  :$scope.settingList[2].afterParagraphSpace,
                    beforeParagraphSpace :$scope.settingList[2].beforeParagraphSpace,
                    bodyAlignment        :$scope.settingList[2].bodyAlignment,
                    bodyFontSize         :$scope.settingList[2].bodyFontSize,
                    bodyImgAlignment     :$scope.settingList[2].bodyImgAlignment,
                    firstLineIndent      :$scope.settingList[2].firstLineIndent,
                    lineSpace            :$scope.settingList[2].lineSpace,
                    templateName         :$scope.settingList[2].templateName,
                    textIndent           :$scope.settingList[2].textIndent,
                    wordSpace            :$scope.settingList[2].wordSpace,
                    bodyFontFamily       :$scope.settingList[2].bodyFontFamily
                }
                if($scope.customTwo.firstLineIndent == 'on') {
                    $(".two-toggle-button").prop("checked",true);
                }else {
                    $(".two-toggle-button").prop("checked",false);
                }
            }
        }
    });
}
var applyTemplate = function($scope) {
    if($scope.bodyAlignment == '左对齐') {
        $scope.bodyTextAlignment = 'left';
    }else if($scope.bodyAlignment == '右对齐') {
        $scope.bodyTextAlignment = 'right';
    }else if($scope.bodyAlignment == '居中对齐'){
        $scope.bodyTextAlignment = 'center';
    }else {
        $scope.bodyTextAlignment = 'justify';
    }
    if($scope.firstLineIndent == 'on') {
        $scope.firstLineIndentEm = 2;
    }else {
        $scope.firstLineIndentEm = 0;
    }
    $("#editor").attr('style','min-height: 430px;font-size: '+$scope.bodyFontSize+'px;text-align:'+$scope.bodyTextAlignment +';line-height:'+$scope.lineSpace+';letter-spacing:'+$scope.wordSpace+'px;');
    $("#editor p").attr('style','width: 100%;clear:both; font-family: '+ $scope.bodyFontFamily +';font-size: '+$scope.bodyFontSize+'px;text-align:'+$scope.bodyTextAlignment +';line-height:'+$scope.lineSpace+';letter-spacing:'+$scope.wordSpace+'px; text-indent: '+$scope.firstLineIndentEm+'em;padding-left:'+$scope.textIndent+'px;padding-right:'+$scope.textIndent+'px; padding-top:'+$scope.beforeParagraphSpace+'px; padding-bottom:'+$scope.afterParagraphSpace+'px;');
    $("#editor img").parent().attr('style','width: 100%;padding-left:'+$scope.textIndent+'px;text-align:'+$scope.bodyTextAlignment+';padding-right:'+$scope.textIndent+'px; padding-top:'+$scope.beforeParagraphSpace+'px; padding-bottom:'+$scope.afterParagraphSpace+'px;');
    if($scope.bodyImgAlignment != $scope.bodyAlignment) {
        if($scope.bodyImgAlignment == '左对齐') {
            $scope.bodyImgAlignment = 'left';
        }else if($scope.bodyImgAlignment == '右对齐') {
            $scope.bodyImgAlignment = 'right';
        }else if($scope.bodyImgAlignment == '居中对齐'){
            $scope.bodyImgAlignment = 'center';
        }else {
            $scope.bodyImgAlignment = 'justify';
        }
        $("#editor img").parent().attr('style','width: 100%;padding-left:'+$scope.textIndent+'px;text-align:'+$scope.bodyImgAlignment+';padding-right:'+$scope.textIndent+'px; padding-top:'+$scope.beforeParagraphSpace+'px; padding-bottom:'+$scope.afterParagraphSpace+'px;');
    }
}
var templateUser = function($scope, $http, index) {
    bjj.http.ng.get($scope, $http, '/api/compile/template/'+ index, {}, function (res) {
        var template = res.template;
        $scope.templateName         = template.templateName;
        $scope.bodyFontFamily       = template.bodyFontFamily;
        $scope.bodyFontSize         = template.bodyFontSize;
        $scope.bodyAlignment        = template.bodyAlignment;
        $scope.bodyImgAlignment     = template.bodyImgAlignment;
        $scope.wordSpace            = template.wordSpace;
        $scope.firstLineIndent      = template.firstLineIndent;
        $scope.lineSpace            = template.lineSpace;
        $scope.textIndent           = template.textIndent;
        $scope.beforeParagraphSpace = template.beforeParagraphSpace;
        $scope.afterParagraphSpace  = template.afterParagraphSpace;
        applyTemplate($scope);
    })
}

var getEditorPreviewData = function($scope, $http, $sce, id) {
    bjj.http.ng.get($scope, $http, '/api/compile/material/detail/' + id, {}, function (res) {
        $scope.data = res.msg.material;
        $scope.data.content = $sce.trustAsHtml($scope.data.content);
        $scope.previewArticle = {
            title      : res.msg.material.title,
            author     : res.msg.material.author ? res.msg.material.author : '匿名',
            updateTime : res.msg.material.updateTime,
            contentAbstract : res.msg.material.contentAbstract,
            picUrl     : res.msg.material.picUrl
        };
    });
};
Array.prototype.indexOfId = function(val) {
    for (var i = 0; i < this.length; i++) {
        if (this[i].id == val) return i;
    }
    return -1;
};
