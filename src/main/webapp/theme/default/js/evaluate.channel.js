var evaluateChannelCtrl = function ($rootScope, $scope, $http, $stateParams) {
    $rootScope.minBar = true;
    var channelId = $stateParams.channelId;
    $scope.numType = '累计转载';
    $scope.channelTrendEmptyView = false;
    $scope.channelKeywordsEmptyView = false;
    $scope.isMediaAttr = false;
    $scope.isDemo = false;
    if($stateParams.siteType == 1) {
        $scope.channelTendencyLabelList = [
            { key : 'publishCount', value : '发文量'},
            { key : 'multiple', value : '综合指数'},
            { key : 'psi', value : '传播力'},
            { key : 'mii', value : '影响力'},
            { key : 'tsi', value : '公信力'},
            { key : 'bsi', value : '引导力'},
            { key : 'sumReprint', value : '累计转载'},
            { key : 'avgReprint', value : '平均转载'}
        ];
        $scope.channelContentList = [
            { key : 'sumReprint', value : '累计转载'}
        ];
    }
    if($stateParams.siteType == 2) {
        $scope.channelTendencyLabelList = [
            { key : 'publishCount', value : '发文量'},
            { key : 'multiple', value : '综合指数'},
            { key : 'psi', value : '传播力'},
            { key : 'mii', value : '影响力'},
            { key : 'tsi', value : '公信力'},
            { key : 'bsi', value : '引导力'},
            { key : 'sumRead', value : '累计阅读'},
            { key : 'avgRead', value : '平均阅读'},
            { key : 'sumLike', value : '累计点赞'},
            { key : 'avgLike', value : '平均点赞'},
            { key : 'sumReprint', value : '累计转载'},
            { key : 'avgReprint', value : '平均转载'}
        ];
        $scope.channelContentList = [
            { key : 'sumReprint', value : '累计转载'},
            { key : 'sumLike', value : '累计点赞'},
            { key : 'sumRead', value : '累计阅读'}
        ];
    }
    if($stateParams.siteType == 3) {
        $scope.channelTendencyLabelList = [
            { key : 'publishCount', value : '发文量'},
            { key : 'multiple', value : '综合指数'},
            { key : 'psi', value : '传播力'},
            { key : 'mii', value : '影响力'},
            { key : 'tsi', value : '公信力'},
            { key : 'bsi', value : '引导力'},
            { key : 'sumLike', value : '累计点赞'},
            { key : 'avgLike', value : '平均点赞'},
            { key : 'sumReprint', value : '累计转载'},
            { key : 'avgReprint', value : '平均转载'}
        ];
        $scope.channelContentList = [
            { key : 'sumReprint', value : '累计转载'},
            { key : 'sumLike', value : '累计点赞'},
        ];
    }
    $scope.contentType = function() {
        var type = this.item == undefined ? 'sumReprint' : this.item.key;
        //切换选择的标签
        _.forEach($scope.channelContentList, function(v) {
            v.active = (v.key == type);
        });
        if(type == 'sumLike') {
            $scope.numType = '累计点赞';
        }else if(type == 'sumRead') {
            $scope.numType = '累计阅读'
        }else {
            $scope.numType = '累计转载'
        }
        getFourPowerTopNews($scope, $http, channelId, type)
    }
    $scope.toggleFourPowerTrend = function () {
        var type = this.item == undefined ? 'multiple' : this.item.key;
        //切换选择的标签
        _.forEach($scope.channelTendencyLabelList, function(v) {
            v.active = (v.key == type);
        });
        getFourPowerTrend($scope, $http, channelId, type);
    };
    console.log($stateParams.isDemo);
    if(typeof($stateParams.isDemo) == 'undefined'){

    }else{
        $scope.isDemo = $stateParams.isDemo;
    } 
    getEvaluateChannelInfo($scope, $http, channelId);
    $scope.contentType();
    $scope.toggleFourPowerTrend();
    getMediaAttr($scope, $http)
    getClassification($scope, $http)
    getFourPowerWordCloud($scope, $http, channelId);
    $scope.setIndex = function (type) {
        if(type == '') {
            $scope.type = 'multiple';
            getParameters($scope, $http, channelId)
            $scope.isMediaAttr = true;
            $scope.modalTitle = '综合指数倾向';
        }else {
            $scope.type = type;
            getStandard($scope, $http, channelId, type)
            $scope.isMediaAttr = false;
            $scope.modalTitle = '四力倾向';
        }
        $("#setIndex").modal("show")
    }
    $scope.saveIndex = function () {
        if($scope.isMediaAttr) {
            var  reg = /^([1-9]\d?|100)$/;
            if(!reg.test($scope.parameters.psiNum) || !reg.test($scope.parameters.miiNum) || !reg.test($scope.parameters.bsiNum) || !reg.test($scope.parameters.tsiNum)) {
                bjj.dialog.alert('danger', '输入内容不符合规则');
                return;
            }
            if(parseInt($scope.parameters.psiNum) + parseInt($scope.parameters.miiNum) + parseInt($scope.parameters.bsiNum) + parseInt($scope.parameters.tsiNum) != 100) {
                bjj.dialog.alert('danger', '权重值总和必须等于100');
                return;
            }
            if($scope.parameters.mediaAttrName == undefined || $scope.parameters.mediaAttrName == "" ) {
                $scope.parameters.mediaAttrName = ""
            }
            setChannelMultipleWeight($scope, $http, channelId, $scope.parameters.mediaAttrName, $scope.parameters.psiNum, $scope.parameters.miiNum, $scope.parameters.tsiNum, $scope.parameters.bsiNum )
        }else {
            var standardReg = /^(\+?[1-9]\d{0,2}|\+?1000)$/;
            if(!standardReg.test($scope.standard)) {
                bjj.dialog.alert('danger', '输入内容不符合规则');
                return;
            }
            if($scope.classificationName == undefined || $scope.classificationName == "" ) {
                $scope.classificationName = ""
            }
            setChannelFourPowerWeight($scope, $http, channelId, $scope.type, $scope.classificationName, $scope.standard)
        }
    }
    //恢复默认
    $scope.restoreDefault = function () {
        initChannelWeight($scope, $http, channelId, $scope.type)
    }
};
//获取渠道详情
let getEvaluateChannelInfo = function ($scope, $http, channelId) { 
    bjj.http.ng.get($scope, $http, '/api/evaluate/channel/detail/' + channelId, {}, function (res) {
        $scope.info = res.info;
    })
}
//综合指数的媒体级别
let getMediaAttr = function ($scope, $http) {
    bjj.http.ng.get($scope, $http, '/api/evaluate/channel/detail/multiple/mediaAttr', {}, function (res) {
        $scope.mediaAttrList = res.list;
    })
}
//获取综合指数四力权重
let getParameters = function ($scope, $http, channelId) {
    bjj.http.ng.get($scope, $http, '/api/evaluate/channel/detail/multiple/parameters/' + channelId, {}, function (res) {
        if(res.mediaAttr.length == 0) {
            $scope.parameters = {
                mediaAttrName: '',
                psiNum: res.parameters[0].value,
                miiNum: res.parameters[1].value,
                bsiNum: res.parameters[2].value,
                tsiNum: res.parameters[3].value
            }
        }else {
            $scope.parameters = {
                mediaAttrName: res.mediaAttr[0].name,
                psiNum: res.parameters[0].value,
                miiNum: res.parameters[1].value,
                bsiNum: res.parameters[2].value,
                tsiNum: res.parameters[3].value
            }
        }
    })
}
//获取四力倾向标准
let getStandard = function ($scope, $http, channelId, fourPower) {
    bjj.http.ng.get($scope, $http, '/api/evaluate/channel/detail/fourPower/standard/' + channelId, {
        fourPower: fourPower
    }, function (res) {
        if(res.classification.length == 0) {
            $scope.classificationName = ''
        }else {
            $scope.classificationName = res.classification[0].name;
        }
        $scope.standard = res.standard;
    })
}
//获取四力倾向内容分类
let getClassification = function ($scope, $http) {
    bjj.http.ng.get($scope, $http, '/api/evaluate/channel/detail/fourPower/classification', {}, function (res) {
        $scope.fourPowerList = res.list;
    })
}

//设置综合指数的权重 (四力权重之和必须等于100)
let setChannelMultipleWeight = function ($scope, $http, channelId, mediaAttrName, psiWeight, miiWeight, tsiWeight, bsiWeight) {
    bjj.http.ng.post($scope, $http, '/api/evaluate/channel/detail/' + channelId , {
        mediaAttrName: mediaAttrName,
        psiWeight: psiWeight,
        miiWeight: miiWeight,
        tsiWeight: tsiWeight,
        bsiWeight: bsiWeight
    }, function (res) {
        getEvaluateChannelInfo($scope, $http, channelId);
        $scope.toggleFourPowerTrend();
        $("#setIndex").modal("hide");
    }, function (res) {
        bjj.dialog.alert('danger', res.msg);
    })
}
//设置四力的权重
let setChannelFourPowerWeight = function ($scope, $http, channelId, fourPower, classificationName, standard) {
    bjj.http.ng.post($scope, $http, '/api/evaluate/channel/detail/fourPower/' + channelId , {
        fourPower: fourPower,
        classificationName: classificationName,
        standard: standard
    }, function (res) {
        getEvaluateChannelInfo($scope, $http, channelId);
        $scope.toggleFourPowerTrend();
        $("#setIndex").modal("hide")
    }, function (res) {
        bjj.dialog.alert('danger', res.msg);
    })
}

//恢复默认 初始化四力的权重   key :multiple:综合指数  psi:传播力  mii:影响力   bsi:引导力  tsi:公信力
let initChannelWeight = function ($scope, $http, channelId, fourPower) {
    bjj.http.ng.put($scope, $http, '/api/evaluate/channel/detail/fourPower/' + channelId, {
        fourPower: fourPower//传四力
    }, function (res) {
        $scope.list = res.msg;
        getEvaluateChannelInfo($scope, $http, channelId);
        $scope.toggleFourPowerTrend();
        $("#setIndex").modal("hide")
    }, function (res) {
        bjj.dialog.alert('danger', res.msg);
    })
}
//趋势统计 传参和一前一样publishCount:发文量   multiple:综合指数  psi:传播力  mii:影响力   bsi:引导力  tsi:公信力
//sumReprint:累计转载  sumLike:累计点赞  avgReprint:平均转载  avgLike:平均点赞
let getFourPowerTrend = function ($scope, $http, channelId, type) {
    bjj.http.ng.get($scope, $http, '/api/evaluate/channel/detail/' + channelId + '/trend', {
        key: type
    }, function (res) {
        if(res.list.length == 0) {
            $scope.channelTrendEmptyView = true;
            $scope.channelTrendEmptyMsg = res.msg;
            return;
        }else {
            $scope.channelTrendEmptyView = false;
        }
        var seriesData = res.list.data;
        var xAxisData = _.map(res.list.timeRange, function (v) {
            return new Date(v).Format("M月d日");
        });
        var obj = _.filter($scope.channelTendencyLabelList, { 'key': type });
        drawFourPowerTrend($scope, seriesData, xAxisData, obj[0].value);
    });
};
//获取top10
let getFourPowerTopNews = function ($scope, $http, channelId, key) {
    bjj.http.ng.get($scope, $http, '/api/evaluate/channel/detail/' + channelId + '/topNews', {
        key: key//传四力
    }, function (res) {
        if(res.list.length == 0) {
            $scope.contentListEmptyView = true;
            $scope.contentListEmptyMsg = res.msg;
            return;
        }else {
            $scope.contentListEmptyView = false;
        }
        $scope.contentList = res.list;
    })
}
//c词云
let getFourPowerWordCloud = function ($scope, $http, channelId) {
    bjj.http.ng.get($scope, $http, '/api/evaluate/channel/detail/' + channelId + '/wordCloud', {}, function (res) {

        if(res.list.length == 0) {
            $scope.channelKeywordsEmptyView = true;
            $scope.channelKeywordsEmptyMsg = res.msg;
            return;
        }else {
            $scope.channelKeywordsEmptyView = false;
        }

        var seriesData = _.map(res.list, function (v) {
            return {
                name    : v.word,
                value   : v.count
            };
        });
        drawFourPowerKeywords($scope, seriesData);
    });

};

var graphic = function ($scope) {
    return [{
        type: 'group',
        bounding: 'raw',
        left: 380,
        bottom: 15,
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
/**
 * 绘制趋势统计
 */
var drawFourPowerTrend = function($scope, seriesData, xAxisData, name) {
    var myChart = echarts.init(document.getElementById('channel-trend'));
    myChart.setOption({
        animation: false,
        tooltip: {
            trigger: 'axis',
            formatter: '日期：{b}<br>' + name + '：{c}'
        },
        grid: {
            left: '0%',
            right: '20',
            top: '10',
            bottom: '20',
            containLabel: true
        },
        xAxis: {
            type: 'category',
            boundaryGap: false,
            data: xAxisData
        },
        yAxis: {
            type: 'value'
        },
        series: [{
            data: seriesData,
            type: 'line',
            smooth: true
        }],
        color: ['#6174FA']
    });
    window.onresize = myChart.resize;
};
/**
 * 绘制词云分析echarts图
 */
var drawFourPowerKeywords = function ($scope, seriesData) {
    var myChart = echarts.init(document.getElementById('channel-keywords'));
    myChart.setOption({
        animation: false,
        tooltip: {},
        series: [ {
            type: 'wordCloud',
            gridSize: 20,
            sizeRange: [18, 50],
            rotationRange: [-80, 90],   //旋转范围
            shape: 'pentagon',
            drawOutOfBound: true,
            textStyle: {
                normal: {
                    color: function () {
                        return 'rgb(' + [
                            Math.round(Math.random() * 160),
                            Math.round(Math.random() * 160),
                            Math.round(Math.random() * 160)
                        ].join(',') + ')';
                    }
                },
                emphasis: {
                    shadowBlur: 5,
                    shadowColor: '#ccc'
                }
            },
            data: seriesData
        }]
    });
    window.onresize = myChart.resize;
};
