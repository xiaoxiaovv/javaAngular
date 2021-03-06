var evaluateSummaryCtrl = function ($rootScope, $scope, $http) {
    $scope.tendencyLabelList = [
        { key : 'publishCount', value : '发文量'},
        { key : 'multiple', value : '综合指数'},
        { key : 'psi', value : '传播力'},
        { key : 'mii', value : '影响力'},
        { key : 'tsi', value : '公信力'},
        { key : 'bsi', value : '引导力'}
    ];
    $rootScope.minBar = true;
    $scope.siteType = 'publishCount';
    $scope.wechatTypeName = 'publishCount';
    $scope.weiboTypeName = 'publishCount';
    $scope.webType = '发文量';
    $scope.wechatType = '发文量';
    $scope.weiboType = '发文量';
    $scope.webPage = 1;
    $scope.wechatPage = 1;
    $scope.weiboPage = 1;
    $scope.tendencyEmptyView = false;
    $scope.websiteListEmptyView = false;
    $scope.wechatListEmptyView = false;
    $scope.weiboListEmptyView = false;
    $scope.evaluateTendencyData = null;
    $scope.page = {};
    $scope.isDemo = false;
    $scope.websiteEmptyView = false;
    $scope.wechatEmptyView = false;
    $scope.weiboEmptyView = false;
    $scope.webOption = {
        curr: 1,
        all: 1,
        count: 1,
        click: function (page) {
        }
    };
    $scope.wechatOption={
        curr: 1,
        all: 1,
        count: 1,
        click: function (page) {
        }
    };
    $scope.weiboOption={
        curr: 1,
        all: 1,
        count: 1,
        click: function (page) {
        }
    };
    getEvaluateSummaryWebsiteRank($scope, $http, 'publishCount', 1);
    getEvaluateSummaryWechatRank($scope, $http, 'publishCount', 1);
    getEvaluateSummaryWeiboRank($scope, $http, 'publishCount', 1);
    $scope.websiteRank = function(type) {
        $scope.webPage = 1;
        $scope.siteType = type;
        getEvaluateSummaryWebsiteRank($scope, $http, type, 1);
        switch (type) {
            case 'multiple' :
                $scope.webType = '综合指数';
                break;
            case 'psi' :
                $scope.webType = '传播力指数';
                break;
            case 'mii' :
                $scope.webType = '影响力指数';
                break;
            case 'bsi' :
                $scope.webType = '引导力指数';
                break;
            case 'tsi' :
                $scope.webType = '公信力指数';
                break;
            case 'publishCount' :
                $scope.webType = '发文量';
                break;
        }
    }
    $scope.wechatRank = function(type) {
        $scope.wechatPage = 1;
        $scope.wechatTypeName = type;
        getEvaluateSummaryWechatRank($scope, $http, type, 1);
        switch (type) {
            case 'multiple' :
                $scope.wechatType = '综合指数';
                break;
            case 'psi' :
                $scope.wechatType = '传播力指数';
                break;
            case 'mii' :
                $scope.wechatType = '影响力指数';
                break;
            case 'bsi' :
                $scope.wechatType = '引导力指数';
                break;
            case 'tsi' :
                $scope.wechatType = '公信力指数';
                break;
            case 'publishCount' :
                $scope.wechatType = '发文量';
                break;
            case 'sumRead' :
                $scope.wechatType = '累计阅读';
                break;
            case 'sumLike' :
                $scope.wechatType = '累计点赞';
                break;
            case 'avgRead' :
                $scope.wechatType = '平均阅读';
                break;
            case 'avgLike' :
                $scope.wechatType = '平均点赞';
                break;
        }
    }
    $scope.weiboRank = function(type) {
        $scope.weiboPage = 1;
        $scope.weiboTypeName = type;
        getEvaluateSummaryWeiboRank($scope, $http, type, 1);
        switch (type) {
            case 'multiple' :
                $scope.weiboType = '综合指数';
                break;
            case 'psi' :
                $scope.weiboType = '传播力指数';
                break;
            case 'mii' :
                $scope.weiboType = '影响力指数';
                break;
            case 'bsi' :
                $scope.weiboType = '引导力指数';
                break;
            case 'tsi' :
                $scope.weiboType = '公信力指数';
                break;
            case 'publishCount' :
                $scope.weiboType = '发文量';
                break;
            case 'sumReprint' :
                $scope.weiboType = '累计转载';
                break;
            case 'sumLike' :
                $scope.weiboType = '累计点赞';
                break;
            case 'avgReprint' :
                $scope.weiboType = '平均转载';
                break;
            case 'avgLike' :
                $scope.weiboType = '平均点赞';
                break;
        }
    }
    $scope.webGoPage = function() {
        $scope.webPage = $scope.page.webPageNo;
        getEvaluateSummaryWebsiteRank($scope, $http, $scope.siteType, $scope.page.webPageNo);
    }
    $scope.wechatGoPage = function() {
        $scope.wechatPage = $scope.page.wechatPageNo;
        $scope.wechatOption.curr = $scope.page.wechatPageNo;
        getEvaluateSummaryWechatRank($scope, $http, $scope.wechatTypeName, $scope.page.wechatPageNo);
    }
    $scope.weiboGoPage = function() {
        $scope.weiboPage = $scope.page.weiboPageNo;
        getEvaluateSummaryWeiboRank($scope, $http, $scope.weiboTypeName, $scope.page.weiboPageNo);
    }
    $scope.getTendency = function () {
        var type = this.item == undefined ? 'publishCount' : this.item.key;
        //切换选择的标签
        _.forEach($scope.tendencyLabelList, function(v) {
            v.active = (v.key == type);
        });

        //处理echarts渲染前的数据，并最终渲染图形
        var dealData = function () {
            var result = $scope.evaluateTendencyData[type];
            var xAxisData = [];
            var seriesObj = _.map(result, function (v, key) {
                v = _.sortBy(v, 'time');
                xAxisData = _.map(v, function (item) {
                    return new Date(item.time).Format("M月d日");
                });
                return {
                    name    : key,
                    type    : 'line',
                    stack   : key,
                    smooth  : true,
                    data    : _.map(v, 'count')
                };
            });
            drawEvaluateTendency($scope, seriesObj, xAxisData);
        };

        if($scope.evaluateTendencyData == null) {
            //获取概览的趋势统计Top10 参数(publishCount:发文量   multiple:综合指数  psi:传播力  mii:影响力   bsi:引导力  tsi:公信力)
            bjj.http.ng.get($scope, $http, '/api/evaluate/tendency', {}, function (res) {
                if(res.info == null) {
                    $scope.tendencyEmptyView = true;
                    $scope.tendencyEmptyMsg = res.msg;
                    return;
                }
                $scope.evaluateTendencyData = res.info;
                dealData();
            });
        } else {
            dealData();
        }
    };

    getEvaluateSummaryInfo($scope, $http);
    $scope.getTendency();
    getChannelStatus($scope, $http);

};
//获取概览详情的信息
let getEvaluateSummaryInfo = function ($scope, $http) {
    bjj.http.ng.get($scope, $http, '/api/evaluate/info', {}, function (res) {
        $scope.info = res.info;
        $scope.isDemo = res.isDemo;
    })
};
//获取概览的网站排行 参数（publishCount:发文量   multiple:综合指数  psi:传播力  mii:影响力   bsi:引导力  tsi:公信力 ）
let getEvaluateSummaryWebsiteRank = function ($scope, $http, type, pageNo) {
    bjj.http.ng.get($scope, $http, '/api/evaluate/websiteRank', {
        rankIndex: type,
        pageNo: pageNo,
        pageSize:10
    }, function (res) {
        if(res.msg == '未创建该类型渠道，请设置'){
            $scope.websiteEmptyView = true;
        }
        if(res.list.length == 0) {
            $scope.websiteListEmptyView = true;
            $scope.websiteListEmptyMsg = res.msg;
            return;
        }else {
            $scope.websiteListEmptyView = false;
        }
        $scope.websiteList = res.list;
        $scope.webTotalCount = res.totalCount;
        $scope.webOption = {
            curr: $scope.webPage,
            all: res.totalPageNo,
            count: res.totalPageNo,
            click: function (page) {
                $scope.webPage = page;
                getEvaluateSummaryWebsiteRank($scope, $http, $scope.siteType, page);
            }
        }
    })
};
//获取概览的微信排行 参数（publishCount:发文量   multiple:综合指数  psi:传播力  mii:影响力   bsi:引导力  tsi:公信力
// sumRead:累计阅读  sumLike:累计点赞  avgRead:平均阅读  avgLike:平均点赞 ）
let getEvaluateSummaryWechatRank = function ($scope, $http, type, pageNo) {
    bjj.http.ng.get($scope, $http, '/api/evaluate/wechatRank', {
        rankIndex: type,
        pageNo: pageNo,
        pageSize:10
    }, function (res) {
        if(res.msg == '未创建该类型渠道，请设置'){
            $scope.wechatEmptyView = true;
        }
        if(res.list.length == 0) {
            $scope.wechatListEmptyView = true;
            $scope.wechatListEmptyMsg = res.msg;
            return;
        }else {
            $scope.wechatListEmptyView = false;
        }
        $scope.wechatRankList = res.list;
        $scope.wechatTotalCount = res.totalCount;
        $scope.wechatOption = {
            curr: $scope.wechatPage,
            all: res.totalPageNo,
            count: res.totalPageNo,
            click: function (page) {
                $scope.wechatPage = page;
                getEvaluateSummaryWechatRank($scope, $http, $scope.wechatTypeName, page);
            }
        }
    })
};
//获取概览的微博排行 参数（publishCount:发文量   multiple:综合指数  psi:传播力  mii:影响力   bsi:引导力  tsi:公信力
//sumReprint:累计转载  sumLike:累计点赞  avgReprint:平均转载  avgLike:平均点赞
let getEvaluateSummaryWeiboRank = function ($scope, $http, type, pageNo) {
    bjj.http.ng.get($scope, $http, '/api/evaluate/weiboRank', {
        rankIndex: type,
        pageNo: pageNo,
        pageSize: 10
    }, function (res) {
        if(res.msg == '未创建该类型渠道，请设置'){
            $scope.weiboEmptyView = true;
        }
        if(res.list.length == 0) {
            $scope.weiboListEmptyView = true;
            $scope.weiboListEmptyMsg = res.msg;
            return;
        }else {
            $scope.weiboListEmptyView = false;
        }
        $scope.weiboRankList = res.list;
        $scope.weiboTotalCount = res.totalCount;
        $scope.weiboOption = {
            curr: $scope.weiboPage,
            all: res.totalPageNo,
            count: res.totalPageNo,
            click: function (page) {
                $scope.weiboPage = page;
                getEvaluateSummaryWeiboRank($scope, $http, $scope.weiboTypeName, page);
            }
        }
    })
};
//获取渠道状态
let getChannelStatus = function ($scope, $http) {
    bjj.http.ng.get($scope, $http, '/api/evaluate/channel/status', {}, function (res) {
        $scope.status = res.list;
    })
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
 * 绘制趋势统计echarts图
 */
var drawEvaluateTendency = function ($scope, seriesObj, xAxisData) {
    var myChart = echarts.init(document.getElementById('echartsEvaluateTendency'));
    myChart.setOption({
        animation: false,
        title: {
            show: false
        },
        tooltip: {
            trigger: 'axis'
        },
        legend: {
            show: false
        },
        grid: {
            left: '0%',
            right: '20',
            top: '10',
            bottom: '20',
            containLabel: true
        },
        toolbox: {
            show: false
        },
        xAxis: {
            type: 'category',
            boundaryGap: false,
            data: xAxisData
        },
        yAxis: {
            type: 'value'
        },
        series: seriesObj,
        color: ['#FFA16E', '#6EABFF', '#9B6DFD', '#FFC61F', '#00BD5B', '#1FCEFF', '#EB6DFD', '#F86574','#6174FA', '#00CCAF', '#FB4558', '#0550E4', '#D301BD']
    });
};
