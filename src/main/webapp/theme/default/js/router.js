"use strict";
var bv = "?0315" //+ new Date().getTime();       // bjj version
var app = angular.module("bjjApp", ["ui.router", "oc.lazyLoad", "ngSanitize"]);
app.directive('backToTop', function() {
    return {
        restrict : 'EA',
        replace : true,
        scope : { container : '@' },
        template : '<div class="bjj-back-to-top" ng-click="back2Top();" title="返回顶部"><i class="fa fa-chevron-up" aria-hidden="true"></i></div>',
        link : function(scope, element, attrs) {
            var _container = $('.' + scope.container);
            _container.scroll(function () {
                _container.scrollTop() > 200 ? $('.bjj-back-to-top').fadeIn() : $('.bjj-back-to-top').fadeOut();
            });
            scope.back2Top = function() {
                _container.animate({scrollTop: 0}, 300);
            }
        }
    }
});
app.directive('listEmpty', function() {
    return {
        restrict : 'EA',
        replace : true,
        scope : { msg : '@'},
        template : '<div class="bjj-list-empty"><i class="fa fa-3x fa-exclamation-circle" aria-hidden="true"></i><div>{{msg || "暂无相关数据"}}</div></div>'
    }
});
app.directive('ngRightClick', function(){
    return {
        restrict: 'AE',
        scope:{
            index:"@index",
        },
        link: function(scope, element, attrs) {
            element.bind('contextmenu', function(e) {
                event.preventDefault();
                $('.right-view').hide();
                $(element).find('.right-view')[0].style.display= 'block';
                angular.element(window).bind('click',function(){
                    scope.$apply(function() {
                        $(element).find('.right-view')[0].style.display= 'none';
                    });
                });
            });
        }
    };
});

app.directive('myPagination', function () {
    return {
        restrict: 'EA',
        replace: true,
        scope: {
            option: '=pageOption'
        },
        template: '<ul class="pagination">' +
        '<li ng-click="pageClick(p)" ng-repeat="p in page" class="{{option.curr==p?\'active\':\'\'}}">' +
        '<a href="javascript:;">{{p}}</a>' +
        '</li>' +
        '</ul>',
        link: function (scope, element, attrs) {

            if (!scope.option.curr || isNaN(scope.option.curr) || scope.option.curr < 1) scope.option.curr = 1;
            if (!scope.option.all || isNaN(scope.option.all) || scope.option.all < 1) scope.option.all = 1;
            if (scope.option.curr > scope.option.all) scope.option.curr = scope.option.all;
            if (!scope.option.count || isNaN(scope.option.count) || scope.option.count < 1) scope.option.count = 10;

            scope.page = getRange(scope.option.curr, scope.option.all, scope.option.count);


            scope.pageClick = function (page) {
                if (page == '«') {
                    page = parseInt(scope.option.curr) - 1;
                } else if (page == '»') {
                    page = parseInt(scope.option.curr) + 1;
                }
                if (page < 1) page = 1;
                else if (page > scope.option.all) page = scope.option.all;

                if (page == scope.option.curr) return;
                if (scope.option.click && typeof scope.option.click === 'function') {
                    scope.option.click(page);
                    scope.option.curr = page;
                    scope.page = getRange(scope.option.curr, scope.option.all, scope.option.count);
                }
            };

            function getRange(curr, all, count) {

                curr = parseInt(curr);
                all = parseInt(all);
                count = parseInt(count);
                var from = curr - parseInt(count / 2);
                var to = curr + parseInt(count / 2) + (count % 2) - 1;

                if (from <= 0) {
                    from = 1;
                    to = from + count - 1;
                    if (to > all) {
                        to = all;
                    }
                }
                if (to > all) {
                    to = all;
                    from = to - count + 1;
                    if (from <= 0) {
                        from = 1;
                    }
                }
                var range = [];
                for (var i = from; i <= to; i++) {
                    range.push(i);
                }
                range.push('»');
                range.unshift('«');
                return range;
            }
            scope.$watch('option', function (newVal, oldVal) {
                if (newVal !== oldVal) {
                    scope.page = getRange(newVal.curr,newVal.all,newVal.count)
                }
            });
        }
    }
});
app.filter('orFilter', function () {
    return function(list, keys, filterCont){
        var result = [];
        if(list == undefined || list == null) return result;
        for(var i = list.length-1; i >= 0; i--){
            var item = list[i];
            var filterValueArr = [];
            for(var j = 0; j < keys.length; j++) {
                var param = item[keys[j]];
                if(param != undefined && param != "") {
                    filterValueArr.push(param);
                }
            }
            if(filterValueArr.join('-').toLowerCase().indexOf(filterCont.toLowerCase()) > -1 ){
                result.push(item);
            }
        }
        return result;
    }
});

app.config(function ($stateProvider, $urlRouterProvider, $ocLazyLoadProvider) {

    $ocLazyLoadProvider.config({
        debug   : false, //是否启用调试模式
        events  : true  //事件绑定是否启用
    });
    $urlRouterProvider
        .otherwise('dashboard');
    $stateProvider
    // 会员中心
        .state("profile", {
            url         : "/account/profile",
            templateUrl : "/account/profile.html" + bv,
            controller  : "profileCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/lib/laydate/laydate.css",
                            "/lib/laydate/default/laydate.css",
                            "/lib/laydate/laydate.dev.js",
                            "/theme/default/js/account.profile.js" + bv,
                            "/theme/default/css/account.profile.css" + bv
                        ]
                    });
                }
            }
        })
        .state("modifyPwd", {
            url         : "/account/password/modify",
            templateUrl : "/account/password_modify.html" + bv,
            controller  : "modifyPwdCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [ "/theme/default/js/account.password.modify.js" + bv,
                            "/theme/default/css/account.password.modify.css" + bv
                        ]
                    });
                }
            }
        })
        .state("accountChannel", {
            url         : "/account/channel",
            templateUrl : "/account/channel/index.html" + bv,
            controller  : "accountChannelCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/theme/default/js/account.channel.js" + bv,
                            "/theme/default/css/account.channel.css" + bv
                        ]
                    });
                }
            }
        })
        .state("accountSite", {
            url         : "/account/site?:captureSiteId",
            templateUrl : "/account/site/index.html" + bv,
            controller  : "accountSiteCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [ "/theme/default/js/account.site.js" + bv,
                            "/theme/default/css/account.site.css" + bv
                        ]
                    });
                }
            }
        })
        .state("accountSite.modify", {
            url         : "/modify?:siteId",
            templateUrl : "/account/site/modify.html" + bv,
            controller  : "accountSiteModifyCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [ "/theme/default/js/account.site.modify.js" + bv,
                            "/theme/default/css/account.site.css" + bv
                        ]
                    });
                }
            }
        })
        .state("accountSite.add", {
            url         : "/add",
            templateUrl : "/account/site/add.html" + bv,
            controller  : "accountSiteAddCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [ "/theme/default/js/account.site.add.js" + bv,
                            "/theme/default/css/account.site.css" + bv
                        ]
                    });
                }
            }
        })
        .state("accountSubject", {
            url         : "/account/subject?:captureSubjectId",
            templateUrl : "/account/subject/index.html" + bv,
            cache       : false,
            controller  : "accountSubjectCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/theme/default/js/account.subject.js" + bv,
                            "/theme/default/css/account.subject.css" + bv
                        ]
                    });
                }
            }
        })
        .state("accountSubject.modify", {
            url         : "/modify?:subjectId",
            templateUrl : "/account/subject/modify.html" + bv,
            controller  : "accountSubjectModifyCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/theme/default/js/account.subject.modify.js" + bv,
                            "/theme/default/css/account.subject.modify.css" + bv
                        ]
                    });
                }
            }
        })
        .state("accountMonitor", {
            url         : "/account/monitor",
            templateUrl : "/account/monitor/index.html" + bv,
            controller  : "accountMonitorCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [ "/theme/default/js/account.monitor.js" + bv,
                            "/theme/default/css/account.monitor.css" + bv
                        ]
                    });
                }
            }
        })
        .state("accountMonitor.modify", {
            url         : "/modify?:monitorId",
            templateUrl : "/account/monitor/modify.html" + bv,
            controller  : "accountMonitorModifyCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/lib/laydate/laydate.css",
                            "/lib/laydate/default/laydate.css",
                            "/lib/laydate/laydate.dev.js",
                            "/theme/default/js/account.monitor.modify.js" + bv,
                            "/theme/default/css/account.monitor.modify.css" + bv
                        ]
                    });
                }
            }
        })
        .state("accountPushLog", {
            url         : "/account/pushLog",
            templateUrl : "/account/push/push_log.html" + bv,
            controller  : "accountPushLogCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/lib/laydate/laydate.css",
                            "/lib/laydate/default/laydate.css",
                            "/lib/laydate/laydate.dev.js",
                            "/theme/default/js/account.push.log.js" + bv,
                            "/theme/default/css/account.push.log.css" + bv
                        ]
                    });
                }
            }
        })
        .state("accountDashboard", {
            url         : "/account/dashboard",
            templateUrl : "/account/dashboard/index.html" + bv,
            controller  : "accountDashboardCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [ "/theme/default/js/account.dashboard.js" + bv,
                            "/theme/default/css/account.dashboard.css" + bv
                        ]
                    });
                }
            }
        })
        // 消息提示
        .state("messageNotice", {
            url         : "/account/message",
            templateUrl : "/account/message/index.html" + bv,
            controller  : "messageNoticeCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/theme/default/js/account.message.js" + bv,
                            "/theme/default/css/account.message.css" + bv
                        ]
                    });
                }
            }
        })
        // 自动采编
        .state("dashboard", {
            url         : "/dashboard",
            templateUrl : "/capture/dashboard.html" + bv,
            controller  : "dashboardCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "https://cdnjs.cloudflare.com/ajax/libs/viewerjs/1.0.0-beta.1/viewer.min.js",
                            "/theme/default/js/capture.dashboard.js" + bv,
                            "/theme/default/js/capture.dashboard.branch.js" + bv,
                            "/theme/default/css/capture.dashboard.css" + bv
                        ]
                    });
                }
            }
        })
        .state("highlightNewsList", {
            url         : "/highlightNewsList?:modal",
            templateUrl : "/capture/dashboard_news/list.html" + bv,
            controller  : "highlightNewsListCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/lib/laydate/laydate.css",
                            "/lib/laydate/default/laydate.css",
                            "/lib/laydate/laydate.dev.js",
                            "/theme/default/js/capture.highlight.news.list.js" + bv,
                            "/theme/default/css/capture.news.css" + bv,
                            "/theme/default/css/capture.news.detail.css" + bv
                        ]
                    });
                }
            }
        })
        .state("riseRateMonitorList", {
            url         : "/riseRateMonitorList?:modal",
            templateUrl : "/capture/dashboard_news/list.html" + bv,
            controller  : "riseRateMonitorListCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/lib/laydate/laydate.css",
                            "/lib/laydate/default/laydate.css",
                            "/lib/laydate/laydate.dev.js",
                            "/theme/default/js/capture.rise.rate.monitor.list.js" + bv,
                            "/theme/default/css/capture.news.css" + bv,
                            "/theme/default/css/capture.news.detail.css" + bv
                        ]
                    });
                }
            }
        })
        .state("reprintMediaMonitorList", {
            url         : "/reprintMediaMonitorList?:modal&:page",
            templateUrl : "/capture/dashboard_news/list.html" + bv,
            controller  : "reprintMediaMonitorListCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/lib/laydate/laydate.css",
                            "/lib/laydate/default/laydate.css",
                            "/lib/laydate/laydate.dev.js",
                            "/theme/default/js/capture.reprint.media.monitor.list.js" + bv,
                            "/theme/default/css/capture.news.css" + bv,
                            "/theme/default/css/capture.news.detail.css" + bv
                        ]
                    });
                }
            }
        })
        .state("dashboardNewsDetail", {
            url         : "/dashboardNewsDetail?:newsId&:page",
            templateUrl : "/capture/news/detail.html" + bv,
            controller  : "newsDetailCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/theme/default/js/capture.news.detail.js" + bv,
                            "/theme/default/css/capture.news.detail.css" + bv
                        ]
                    });
                }
            }
        })
        //资讯快选
        .state("instantNews", {
            url         : "/capture/instantNews?:type",
            templateUrl : "/capture/instant/list.html" + bv,
            controller  : "instantNewsCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/theme/default/js/capture.instant.news.js" + bv,
                            "/theme/default/css/capture.instant.news.css" + bv
                        ]
                    });
                }
            }
        })
        .state("instantNewsMark", {
            url         : "/capture/instantNewsMark?:firstNewsTime",
            templateUrl : "/capture/instant/mark_list.html" + bv,
            controller  : "instantNewsMarkCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/theme/default/js/capture.instant.news.mark.js" + bv,
                            "/theme/default/css/capture.instant.news.css" + bv
                        ]
                    });
                }
            }
        })
        //最新资讯
        .state("captureSite", {
            url         : "/capture/site?:type&:siteType&:currentSiteId&:order&:startTime",
            templateUrl : "/capture/site/index.html" + bv,
            controller  : "captureSiteCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/theme/default/js/capture.site.js" + bv,
                            "/theme/default/css/capture.site.css" + bv
                        ]
                    });
                }
            }
        })
        .state("captureSite.newsDetail", {
            url         : "/news/detail?:newsId",
            templateUrl : "/capture/news/detail.html" + bv,
            controller  : "newsDetailCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/theme/default/js/capture.news.detail.js" + bv,
                            "/theme/default/css/capture.news.detail.css" + bv
                        ]
                    });
                }
            }
        })
        .state("captureSite.news", {
            url         : "/news?:siteId&:siteName&:randomNum&:auto&:defaultTime&:byOrder",
            templateUrl : "/capture/site/news.html" + bv,
            controller  : "captureSiteNewsCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/lib/laydate/laydate.css",
                            "/lib/laydate/default/laydate.css",
                            "/lib/laydate/laydate.dev.js",
                            "/theme/default/css/capture.news.css" + bv,
                            "/theme/default/js/capture.site.news.js" + bv,
                            "/theme/default/css/capture.news.detail.css" + bv
                        ]
                    });
                }
            }
        })
        .state("captureSite.news.list", {
            url         : "/list?:r&:heatOrder",
            templateUrl : "/capture/news/list.html" + bv,
            controller  : "siteNewsListCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [ "/theme/default/js/capture.site.news.list.js" + bv ]
                    });
                }
            }
        })
        .state("captureSite.weibolist", {
            url         : "/weibo/list?:siteId&:siteName&:randomNum&:auto",
            templateUrl : "/capture/news/weibolist.html" + bv,
            controller  : "weiboListCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/lib/laydate/laydate.css",
                            "/lib/laydate/default/laydate.css",
                            "/lib/laydate/laydate.dev.js",
                            "https://cdnjs.cloudflare.com/ajax/libs/viewerjs/1.0.0-beta.1/viewer.min.js",
                            "/theme/default/css/capture.news.css" + bv,
                            "/theme/default/js/capture.site.weibo.list.js" + bv
                        ]
                    });
                }
            }
        })
        // 全网监控
        .state("captureSubject", {
            url         : "/capture/subject?:type",
            templateUrl : "/capture/subject/index.html" + bv,
            controller  : "captureSubjectCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/theme/default/js/capture.subject.js" + bv,
                            "/theme/default/css/capture.subject.css" + bv
                        ]
                    });
                }
            }
        })
        .state("captureSubject.newsDetail", {
            url         : "/news/detail?:newsId",
            templateUrl : "/capture/news/detail.html" + bv,
            controller  : "newsDetailCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/theme/default/js/capture.news.detail.js" + bv,
                            "/theme/default/css/capture.news.detail.css" + bv
                        ]
                    });
                }
            }
        })
        .state("captureSubject.news", {
            url         : "/news?:subjectId&:subjectName&:randomNum",
            templateUrl : "/capture/subject/news.html" + bv,
            controller  : "captureSubjectNewsCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/lib/laydate/laydate.css",
                            "/lib/laydate/default/laydate.css",
                            "/lib/laydate/laydate.dev.js",
                            "/theme/default/js/capture.subject.news.js" + bv,
                            "/theme/default/css/capture.news.css" + bv,
                            "/theme/default/css/capture.news.detail.css" + bv
                        ]
                    });
                }
            }
        })
        .state("captureSubject.news.list", {
            url         : "/list?:r",
            templateUrl : "/capture/news/list.html" + bv,
            controller  : "subjectNewsListCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [ "/theme/default/js/capture.subject.news.list.js" + bv ]
                    });
                }
            }
        })
        // 智能组稿
        .state("editor", {
            url         : "/compile/editor?:newsId&:materialId",
            templateUrl : "/compile/editor/index.html" + bv,
            controller  : "editorCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "https://cdnjs.cloudflare.com/ajax/libs/viewerjs/1.0.0-beta.1/viewer.min.js",
                            "/lib/laydate/laydate.css",
                            "/lib/laydate/default/laydate.css",
                            "/lib/laydate/laydate.dev.js",
                            "/lib/upload/plupload.full.min.js" + bv,
                            "/theme/default/js/compile.editor.js" + bv,
                            "/theme/default/css/compile.editor.css" + bv
                        ]
                    });
                }
            }
        })
        .state("editorPublicCallback", {
            url         : "/compile/editor/editorPublicCallback?:materialId",
            templateUrl : "/compile/editor/public_callback.html" + bv,
            controller  : "editorPublicCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/theme/default/js/compile.editor.public.js" + bv,
                            "/theme/default/css/compile.editor.css" + bv
                        ]
                    });
                }
            }
        })
        //我的文稿
        .state("materialList", {
            url         : "/compile/material/list",
            templateUrl : "/compile/material/list.html" + bv,
            controller  : "materialListCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/theme/default/js/compile.material.list.js" + bv,
                            "/theme/default/css/compile.material.list.css" + bv
                        ]
                    });
                }
            }
        })
        // 传播测评
        .state("analysis", {
            url         : "/analysis",
            templateUrl : "/analysis/index.html" + bv,
            controller  : "analysisCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/theme/default/js/analysis.js" + bv,
                            "http://echarts.baidu.com/build/dist/echarts.js",
                            "/theme/default/css/analysis.css" + bv
                        ]
                    });
                }
            }
        })
        //测评概览
        .state("evaluateSummary", {
            url         : "/evaluateSummary",
            templateUrl : "/evaluate/summary.html" + bv,
            controller  : "evaluateSummaryCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/theme/default/js/evaluate.summary.js" + bv,
                            "/theme/default/css/evaluate.summary.css" + bv
                        ]
                    });
                }
            }
        })
        //内容测评
        .state("evaluateContent", {
            url         : "/evaluateContent",
            templateUrl : "/evaluate/content.html" + bv,
            controller  : "evaluateContentCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/theme/default/js/evaluate.content.js" + bv,
                            "/theme/default/css/evaluate.content.css" + bv
                        ]
                    });
                }
            }
        })
        //渠道详情
        .state("evaluateChannel", {
            url         : "/evaluateChannel?:channelId&:siteType&:isDemo",
            templateUrl : "/evaluate/channel.html" + bv,
            controller  : "evaluateChannelCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/theme/default/js/evaluate.channel.js" + bv,
                            "/theme/default/css/evaluate.channel.css" + bv
                        ]
                    });
                }
            }
        })
        //测评管理
        .state("evaluateManagement", {
            url         : "/evaluateManagement",
            templateUrl : "/evaluate/management.html" + bv,
            controller  : "evaluateManagementCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/theme/default/js/evaluate.management.js" + bv,
                            "/theme/default/css/evaluate.management.css" + bv
                        ]
                    });
                }
            }
        })
        //渠道管理
        .state("evaluateReport", {
            url         : "/evaluateReport",
            templateUrl : "/evaluate/report.html" + bv,
            controller  : "evaluateReportCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/theme/default/js/evaluate.report.js" + bv,
                            "/theme/default/css/evaluate.report.css" + bv
                        ]
                    });
                }
            }
        })
        // 版权监控
        .state("copyright", {
            url         : "/copyright?:type",
            templateUrl : "/copyright/index.html" + bv,
            controller  : "copyrightCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/theme/default/js/copyright.js" + bv,
                            "/theme/default/css/copyright.css" + bv
                        ]
                    });
                }
            }
        })
        .state("copyright.monitor", {
            url         : "/monitor?:monitorId&:randomNum",
            templateUrl : "/copyright/monitor.html" + bv,
            controller  : "copyrightMonitorCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/theme/default/js/copyright.monitor.js" + bv,
                            "/theme/default/css/copyright.monitor.css" + bv
                        ]
                    });
                }
            }
        })
        // 账号管理
        .state("manage", {
            url         : "/manage",
            templateUrl : "/manage/account/list.html" + bv,
            controller  : "manageCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/theme/default/js/manage.account.list.js" + bv,
                            "/theme/default/css/manage.account.list.css" + bv
                        ]
                    });
                }
            }
        })
        .state("labelManage", {
            url         : "/labelManage",
            templateUrl : "/manage/labelManage.html" + bv,
            controller  : "labelManageCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/theme/default/js/manage.labelManage.js" + bv,
                            "/theme/default/css/manage.labelManage.css" + bv
                        ]
                    });
                }
            }
        })
        .state("statisticsManage", {
            url         : "/statisticsManage",
            templateUrl : "/manage/statistics/list.html" + bv,
            controller  : "statisticsManageCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/lib/countup/countup.min.js",
                            "/lib/countup/angular-countUp.min.js",
                            "/lib/laydate/laydate.css",
                            "/lib/laydate/default/laydate.css",
                            "/lib/laydate/laydate.dev.js",
                            "/theme/default/js/manage.statistics.list.js" + bv,
                            "/theme/default/css/manage.statistics.list.css" + bv
                        ]
                    });
                }
            }
        })
        //统计管理
        .state("modifyAccount", {
            url         : "/modifyAccount?:id",
            templateUrl : "/manage/account/modify.html" + bv,
            controller  : "modifyAccountCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/theme/default/js/manage.account.modify.js" + bv,
                            "/theme/default/css/manage.account.modify.css" + bv
                        ]
                    });
                }
            }
        })
        // 订阅管理
        .state("subscription", {
            url         : "/subscription",
            templateUrl : "/subscription/index.html" + bv,
            controller  : "subscriptionCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/theme/default/js/subscription.index.js" + bv,
                            "/theme/default/css/subscription.index.css" + bv
                        ]
                    });
                }
            }
        })
        .state("subscription.newList", {
            url         : "/subscription/newList?:subjectId&:subjectName&:randomNum&:count",
            templateUrl : "/subscription/list.html" + bv,
            controller  : "subscriptionNewListCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/theme/default/js/subscription.list.js" + bv,
                            "/theme/default/css/capture.news.detail.css" + bv,
                            "/theme/default/css/subscription.list.css" + bv
                        ]
                    });
                }
            }
        })
        .state("accountSubscription", {
            url         : "/account/subscription",
            templateUrl : "/account/subscription/index.html" + bv,
            controller  : "accountSubscriptionCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/theme/default/js/account.subscription.js" + bv,
                            "/theme/default/css/account.subscription.css" + bv
                        ]
                    });
                }
            }
        })
        .state("accountSubscription.modify", {
            url         : "/modify?subjectId",
            templateUrl : "/account/subscription/modify.html" + bv,
            controller  : "accountSubscriptionModifyCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/lib/laydate/laydate.css",
                            "/lib/laydate/default/laydate.css",
                            "/lib/laydate/laydate.dev.js",
                            "/theme/default/js/account.subscription.modify.js" + bv,
                            "/theme/default/css/account.subscription.modify.css" + bv
                        ]
                    });
                }
            }
        })
        // 排除词设置
        .state("subscriptionExclusion", {
            url         : "/subscription/exclusion",
            templateUrl : "/subscription/exclusion.html" + bv,
            controller  : "subscriptionExclusionCtrl",
            resolve     : {
                deps : function($ocLazyLoad){
                    return $ocLazyLoad.load({
                        files : [
                            "/theme/default/js/subscription.exclusion.js" + bv,
                            "/theme/default/css/subscription.exclusion.css" + bv
                        ]
                    });
                }
            }
        })
});
//自动采编
app.controller("dashboardCtrl",         function($rootScope, $scope, $http, $state, $stateParams){
    dashboardCtrl($rootScope, $scope, $http, $state, $stateParams);
    dashboardBranchCtrl($rootScope, $scope, $http, $state);
});
app.controller("messageNoticeCtrl",     function($scope, $http){ messageNoticeCtrl($scope, $http); });
app.controller("instantNewsCtrl",       function($rootScope, $scope, $http, $state, $stateParams, $sce, $interval, $document){ instantNewsCtrl($rootScope, $scope, $http, $state, $stateParams, $sce, $interval, $document); });
app.controller("instantNewsMarkCtrl",   function($rootScope, $scope, $http, $state, $stateParams, $sce, $interval, $document){ instantNewsMarkCtrl($rootScope, $scope, $http, $state, $stateParams, $sce, $interval, $document); });
app.controller("captureSiteCtrl",       function($rootScope, $scope, $http, $state, $stateParams){ captureSiteCtrl($rootScope, $scope, $http, $state, $stateParams); });
app.controller("captureSiteNewsCtrl",   function($rootScope, $scope, $http, $state, $stateParams){ captureSiteNewsCtrl($rootScope, $scope, $http, $state, $stateParams);});
app.controller("captureSubjectCtrl",    function($rootScope, $scope, $http, $state, $stateParams){ captureSubjectCtrl($rootScope, $scope, $http, $state, $stateParams); });
app.controller("captureSubjectNewsCtrl",function($rootScope, $scope, $http, $state, $stateParams){ captureSubjectNewsCtrl($rootScope, $scope, $http, $state, $stateParams); });
app.controller("highlightNewsListCtrl", function($rootScope, $scope, $http, $state, $sce, $stateParams){ highlightNewsListCtrl($rootScope, $scope, $http, $state, $sce, $stateParams); });
app.controller("riseRateMonitorListCtrl",       function($rootScope, $scope, $http, $state, $sce, $stateParams){ riseRateMonitorListCtrl($rootScope, $scope, $http, $state, $sce, $stateParams); });
app.controller("reprintMediaMonitorListCtrl",   function($rootScope, $scope, $http, $state, $sce, $stateParams){ reprintMediaMonitorListCtrl($rootScope, $scope, $http, $state, $sce, $stateParams); });
app.controller("siteNewsListCtrl",      function($rootScope, $scope, $http, $sce, $stateParams){ siteNewsListCtrl($rootScope, $scope, $http, $sce, $stateParams);});
app.controller("weiboListCtrl",         function($rootScope, $scope, $http, $sce, $stateParams, $state){ weiboListCtrl($rootScope, $scope, $http, $sce, $stateParams, $state);});

app.controller("subjectNewsListCtrl",   function($rootScope, $scope, $http, $sce){ subjectNewsListCtrl($rootScope, $scope, $http, $sce); });
app.controller("newsDetailCtrl",        function($rootScope, $scope, $http, $state, $stateParams, $sce){ newsDetailCtrl($rootScope, $scope, $http, $state, $stateParams, $sce); });
//智能组稿
app.controller("editorCtrl",            function($rootScope, $scope, $http, $state, $stateParams, $interval, $sce){ editorCtrl($rootScope, $scope, $http, $state, $stateParams, $interval, $sce); });
app.controller("editorPublicCtrl",      function($rootScope, $scope, $http, $stateParams, $interval, $sce){ editorPublicCtrl($rootScope, $scope, $http, $stateParams, $interval, $sce); });
app.controller("materialListCtrl",      function($scope, $http, $sce){ materialListCtrl($scope, $http, $sce); });
//传播测评
app.controller("analysisCtrl",          function($scope, $http){ analysisCtrl($scope, $http); });
//测评概览
app.controller("evaluateSummaryCtrl",   function($rootScope, $scope, $http){ evaluateSummaryCtrl($rootScope, $scope, $http); });
//内容测评
app.controller("evaluateContentCtrl",   function($rootScope, $scope, $http){ evaluateContentCtrl($rootScope, $scope, $http); });
//测评管理
app.controller("evaluateManagementCtrl",function($rootScope, $scope, $http){ evaluateManagementCtrl($rootScope, $scope, $http); });
//渠道详情
app.controller("evaluateChannelCtrl",function($rootScope, $scope, $http, $stateParams){ evaluateChannelCtrl($rootScope, $scope, $http, $stateParams); });
//渠道管理
app.controller("evaluateReportCtrl",    function($rootScope, $scope, $http, $state){ evaluateReportCtrl($rootScope, $scope, $http, $state); });

//版权监控
app.controller("copyrightCtrl",         function($scope, $http, $state, $stateParams){ copyrightCtrl($scope, $http, $state, $stateParams); });
app.controller("copyrightMonitorCtrl",  function($rootScope, $scope, $http, $state, $stateParams){ copyrightMonitorCtrl($rootScope, $scope, $http, $state, $stateParams); });
//会员中心
app.controller("profileCtrl",           function($rootScope, $scope, $http, $interval){ profileCtrl($rootScope, $scope, $http, $interval); });
app.controller("modifyPwdCtrl",         function($scope, $http){ modifyPwdCtrl($scope, $http); });
app.controller("accountChannelCtrl",    function($scope, $http){ accountChannelCtrl($scope, $http); });
app.controller("accountSiteCtrl",       function($rootScope, $scope, $http, $state, $stateParams){ accountSiteCtrl($rootScope, $scope, $http, $state, $stateParams); });
app.controller("accountSiteModifyCtrl", function($rootScope, $scope, $http, $state, $stateParams){ accountSiteModifyCtrl($rootScope, $scope, $http, $state, $stateParams); });
app.controller("accountSiteAddCtrl",    function($rootScope, $scope, $http, $timeout){ accountSiteAddCtrl($rootScope, $scope, $http, $timeout); });
app.controller("accountSubjectCtrl",    function($rootScope, $scope, $http, $state, $stateParams){ accountSubjectCtrl($rootScope, $scope, $http, $state, $stateParams); });
app.controller("accountSubjectModifyCtrl",  function($rootScope, $scope, $http, $state, $stateParams, $timeout){ accountSubjectModifyCtrl($rootScope, $scope, $http, $state, $stateParams, $timeout); });
app.controller("accountMonitorCtrl",        function($rootScope, $scope, $http, $state){ accountMonitorCtrl($rootScope, $scope, $http, $state); });
app.controller("accountMonitorModifyCtrl",  function($rootScope, $scope, $http, $state, $stateParams){ accountMonitorModifyCtrl($rootScope, $scope, $http, $state, $stateParams); });
app.controller("accountDashboardCtrl",      function($rootScope, $scope, $http){ accountDashboardCtrl($rootScope, $scope, $http); });
app.controller("accountPushLogCtrl",        function($rootScope, $scope, $http, $state){ accountPushLogCtrl($rootScope, $scope, $http, $state); });

//账号管理
app.controller("manageCtrl",            function($scope, $http){ manageCtrl($scope, $http); });
app.controller("modifyAccountCtrl",     function($scope, $http, $stateParams){ modifyAccountCtrl($scope, $http, $stateParams); });
app.controller("statisticsManageCtrl",  function($scope, $http){ statisticsManageCtrl($scope, $http); });

//订阅管理
app.controller("subscriptionCtrl",              function($rootScope, $scope, $http, $state, $stateParams){ subscriptionCtrl($rootScope, $scope, $http, $state, $stateParams); });
app.controller("subscriptionNewListCtrl",       function($rootScope, $scope, $http, $sce, $state, $stateParams){ subscriptionNewListCtrl($rootScope, $scope, $http, $sce, $state, $stateParams); });
app.controller("accountSubscriptionCtrl",       function($rootScope, $scope, $http, $state, $stateParams){ accountSubscriptionCtrl($rootScope, $scope, $http, $state, $stateParams); });
app.controller("accountSubscriptionModifyCtrl", function($rootScope, $scope, $http, $stateParams, $state){ accountSubscriptionModifyCtrl($rootScope, $scope, $http, $stateParams, $state); });
app.controller("subscriptionExclusionCtrl",     function($rootScope, $scope, $http){ subscriptionExclusionCtrl($rootScope, $scope, $http); });
app.controller("labelManageCtrl",     function($rootScope, $scope, $http){ labelManageCtrl($rootScope, $scope, $http); });