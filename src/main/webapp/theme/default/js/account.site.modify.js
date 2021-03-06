/**
 * Author : YCSnail
 * Date : 2017-07-26
 * Email : liyancai1986@163.com
 */
var accountSiteModifyCtrl = function ($rootScope, $scope, $http, $state,$stateParams) {
    $rootScope.deleteFlag = false;
    $scope.siteTypeList = [
        { 'siteTypeName': '网站', value: '1'},
        { 'siteTypeName': '公众号', value: '2'},
        { 'siteTypeName': '微博', value: '3'}
    ];
    if($rootScope.siteList == undefined) {
        $state.go("accountSite");
    }
    if($stateParams.siteId == undefined) {
        $(".siteAddPage").hide()
    }
    bjj.http.ng.get($scope, $http, '/api/capture/site/'+$stateParams.siteId, {}, function (res) {
        $scope.siteName = res.msg.siteName;
        $scope.websiteName = res.msg.websiteName;
        $scope.websiteDomain = res.msg.websiteDomain;
        $scope.siteTypes = res.msg.siteType;
        if($scope.siteTypes == 1) {
            $scope.siteType = "网站";
            $scope.siteFlag = true;
            $scope.weiboFlag = false;
            $scope.wechatFlag = true;
        }else if ($scope.siteTypes == 2){
            $scope.siteType = "公众号";
            $scope.siteFlag = false;
            $scope.weiboFlag = false;
            $scope.wechatFlag = true;
        }else if ($scope.siteTypes == 3){
            $scope.siteType = "微博";
            $scope.siteFlag = false;
            $scope.weiboFlag = true;
            $scope.wechatFlag = false;
        }
    },function(res) {
        bjj.dialog.alert('danger', res.msg);
    });
    $scope.editTypeChange = function() {
        if($scope.siteType == "网站") {
            $scope.siteFlag = true;
        }else if ($scope.siteType == "公众号"){
            $scope.siteFlag = false;
        }else if ($scope.siteType == "微博"){
            $scope.siteFlag = false;
        }
    }
    bjj.input.autocomplete('#basic-url', {
        serviceUrl : '/api/capture/site/urlSuggestion?siteType=1',
        paramName  : 'url'
    }, function (value, data) {
        $('#basic-url').val(value).change();
        $('#setSiteName').val(data).change();
        $('input[name=ModifySiteName]').val(data).change();
        return;
    });
    bjj.input.autocomplete('#setSiteName', {
        serviceUrl : '/api/capture/site/urlSuggestion?siteType=2',
        paramName  : 'name'
    }, function (value, data) {
        $('#setSiteName').val(value).change();
        $('input[name=ModifySiteName]').val(data).change();
        return;
    });
    bjj.input.autocomplete('#weiboSiteName', {
        serviceUrl : '/api/capture/site/urlSuggestion?siteType=3',
        paramName  : 'name'
    }, function (value, data) {
        $('#setWeiboSiteName').val(value).change();
        $('input[name=ModifySiteName]').val(data).change();
        return;
    });
    $scope.deletesSiteId = function() {
        var siteId = $stateParams.siteId;
        bjj.dialog.confirm('确定删除？', function(){
            bjj.http.ng.del($scope, $http, '/api/capture/site/' + siteId + '?_method=delete', {}, function (res) {
                bjj.dialog.alert('success', '删除成功');
                $rootScope.editSaveSite = false;
                bjj.http.ng.get($scope, $http, '/api/capture/sites', {}, function (res) {
                    for(var i = 1;i<res.msg.length; i++) {
                        if(res.msg[i].siteId == siteId) {
                            res.msg.splice(i,1)
                        }
                    }
                    $rootScope.siteList = res.msg;
                    if($rootScope.siteList.length > 0){
                        setTimeout(function () {
                            $(".site-list ul li").eq(1).addClass("active");
                            $state.go("accountSite.modify", { siteId: res.msg[0].siteId });
                        }, 0);
                    }
                    $(".site-list .nav-list").on("click", "li", function() {
                        $(this).addClass("active").siblings().removeClass("active");
                    });
                },function(res) {
                    $rootScope.siteList = [];
                });
                $rootScope.deleteFlag = true;
            },function(res) {
                bjj.dialog.alert('danger', res.msg);
                $rootScope.deleteFlag = false;
            });
        });
    };
    $scope.editSaveSite = function(){
        if($scope.siteType == "网站") {
            $scope.siteType=1
        }else if($scope.siteType == "公众号"){
            $scope.siteType=2
        }else if($scope.siteType == "微博"){
            $scope.siteType=3
        };
        $(".autocomplete-suggestions").hide();
        bjj.http.ng.put($scope, $http, '/api/capture/site/'+ $stateParams.siteId +'?_method=put', {
            siteName: $scope.siteName,
            websiteName: $scope.websiteName,
            websiteDomain: $scope.websiteDomain,
            siteType:$scope.siteType
        }, function (res) {
            $scope.siteTypes = $scope.siteType
            if($scope.siteTypes == 1) {
                $scope.siteType = "网站"
            }else if($scope.siteTypes == 2){
                $scope.siteType = "公众号"
            }else if($scope.siteTypes == 3){
                $scope.siteType = "微博"
            };
            $rootScope.allSiteActive = false;
            $rootScope.AllSiteIds = [];
            $rootScope.siteType.addSiteType = '';
            $rootScope.siteType.keywords = '';
            bjj.http.ng.get($scope, $http, '/api/capture/sites', {}, function (res) {
                bjj.dialog.alert('success', '保存成功');
                $('.bjj-account-save-view .btn-primary').attr("disabled", true);
                setTimeout(function() {
                    $('.bjj-account-save-view .btn-primary').attr("disabled", false);
                    $(".site-list ul li").eq(1).addClass("actively");
                    $state.go("accountSite.modify", { siteId: res.msg[0].siteId });
                },500);
                $rootScope.siteList = res.msg;
            });
        },function(res) {
            bjj.dialog.alert('danger', res.msg);
        });
    };
};