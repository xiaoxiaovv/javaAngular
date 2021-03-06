var evaluateContentCtrl = function ($rootScope, $scope, $http) {
    $rootScope.minBar = true;
    $scope.commentCount = true;
    $scope.likeCount = false;
    $scope.reprintCount = false;
    $scope.type = '阅读量';
    $scope.contentRankEmptyView = false;
    $scope.contentKeywordsEmptyView = false;
    $scope.contentReadAndLikeEmptyView = false;
    $scope.isDemo = false;
    getEvaluateContentRank($scope, $http, "sumRead");
    evaluateSummaryInfo($scope, $http);
    getEvaluateContentKeywords($scope, $http);
    getEvaluateContentReadAndLike($scope, $http);

    $scope.contentRank = function (type) {
        getEvaluateContentRank($scope, $http, type)
        if(type == 'sumRead') {
            $scope.commentCount = true;
            $scope.likeCount = false;
            $scope.reprintCount = false;
            $scope.type = '阅读量';
            return;
        }else if(type == 'sumLike') {
            $scope.likeCount = true;
            $scope.commentCount = false;
            $scope.reprintCount = false;
            $scope.type = '点赞数';
            return;
        }else {
            $scope.reprintCount = true;
            $scope.likeCount = false;
            $scope.commentCount = false;
            $scope.type = '转载数';
        }
    }
}
//获取概览详情的信息
let evaluateSummaryInfo = function ($scope, $http) {
    bjj.http.ng.get($scope, $http, '/api/evaluate/info', {}, function (res) {
        $scope.info = res.info;
        $scope.isDemo = res.isDemo;
    })
}

//内容排行Top10  参数 sumRead:累计阅读  sumLike:累计点赞  sumReprint:累计转载
let getEvaluateContentRank = function ($scope, $http, type) {
    bjj.http.ng.get($scope, $http, "/api/evaluate/content/rank", {
        rankType: type
    }, function (res) {
        if(res.list.length == 0) {
            $scope.contentRankEmptyView = true;
            $scope.contentRankEmptyMsg = res.msg;
            return;
        }else {
            $scope.contentRankEmptyView = false;
        }
        $scope.contentRankList = res.list;
    })
}
//词云分析
let getEvaluateContentKeywords = function ($scope, $http) {
    bjj.http.ng.get($scope, $http, "/api/evaluate/content/keywords", {}, function (res) {

        if(res.list.length == 0) {
            $scope.contentKeywordsEmptyView = true;
            $scope.contentKeywordsEmptyMsg = res.msg;
            return;
        }

        var seriesData = _.map(res.list, function (v) {
            return {
                name    : v.word,
                value   : v.count
            };
        });
        drawEvaluateContentKeywords($scope, seriesData);
    });
};
//平均点赞分布  最外面的avgRead   avgLike 代表坐标中心 data 里面是数据
let getEvaluateContentReadAndLike = function ($scope, $http) {
    bjj.http.ng.get($scope, $http, "/api/evaluate/content/readAndLike", {}, function (res) {

        if(res.list.length == 0) {
            $scope.contentReadAndLikeEmptyView = true;
            $scope.contentReadAndLikeEmptyMsg = res.msg;
            return;
        }

        var axisInfo = { x : parseInt(res.list.avgRead), y : parseInt(res.list.avgLike) };
        var seriesData = _.map(res.list.data, function (v) {
            return [v.avgRead - axisInfo.x, v.avgLike - axisInfo.y];
        });
        var itemData = _.map(res.list.data, function (v) {
            return {
                id          : v.id,
                avgLike     : v.avgLike,
                avgRead     : v.avgRead,
                siteDomain  : v.siteDomain
            };
        });
        drawEvaluateContentReadAndLike($scope, seriesData, itemData, axisInfo);
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
 * 绘制词云分析echarts图
 */
var drawEvaluateContentKeywords = function ($scope, seriesData) {
    var myChart = echarts.init(document.getElementById('content-keywords'));
    myChart.setOption({
        animation: false,
        tooltip: {},
        series: [ {
            gridSize: 20,
            type: 'wordCloud',
            size: ['100%', '100%'],
            textRotation : [0, 45, 90, -45],
            textPadding: 10,
            autoSize: {
                enable: true,
                minSize: 20
            },
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
            data: seriesData.sort(function(a,b){
                return b.value-a.value;
            })
        }]
    });
    window.onresize = myChart.resize;
};
/**
 * 绘制平均阅读点赞分布
 */
var drawEvaluateContentReadAndLike = function ($scope, seriesData, itemData, axisInfo) {
    var myChart = echarts.init(document.getElementById('content-read-and-like'));
    myChart.setOption({
        animation: false,
        tooltip: {
            formatter: function (data, index) {
                return itemData[index.substr(index.lastIndexOf('_') + 1)].siteDomain + '<br/>平均阅读数：' + (data.value[0] + axisInfo.x) + '<br/>' + '平均点赞数：' +(data.value[1] + axisInfo.y);
            }
        },
        xAxis: {
            name: '平均阅读数',
            nameLocation: 'middle',
            nameGap: 25,
            axisLabel: {
                formatter: function (value, index) {
                    return value + axisInfo.x;
                }
            }
        },
        yAxis: {
            name: '平均点赞数',
            nameLocation: 'middle',
            nameGap: 40,
            axisLabel: {
                formatter: function (value, index) {
                    return value + axisInfo.y;
                }
            }
        },
        series: [{
            type: 'scatter',
            symbolSize: 20,
            data: seriesData
        }],
        color: ['#FFA16E', '#6EABFF', '#9B6DFD', '#FFC61F', '#00BD5B', '#1FCEFF', '#EB6DFD', '#F86574','#6174FA', '#00CCAF', '#FB4558', '#0550E4', '#D301BD']
    });
    window.onresize = myChart.resize;
};
