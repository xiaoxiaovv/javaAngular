/**
 * Author : YCSnail
 * Date : 2017-07-26
 * Email : liyancai1986@163.com
 */
var profileCtrl = function ($rootScope, $scope, $http, $interval) {
    $scope.duties = [{duty: '网站小编', value: 1}, {duty: '自媒体小编', value: 2}, {duty: '大咖', value: 3}];
    //修改手机号
    $scope.phoneNumberShow = false;
    $scope.TruephoneShow = false;
    $scope.verifyCodeShow  = false;
    $scope.trueVerifyCodeShow = false;
    $scope.verifyCodeBtnStatus = true;
    $scope.exitPhone = false;
    $("input").focus(function() {
            $scope.phoneNumberShow = false;
            $scope.TruephoneShow = false;
            $scope.verifyCodeShow  = false;
            $scope.trueVerifyCodeShow = false;
            $scope.exitPhone = false;
        })
    var thisUrl = window.location.href;
    if(thisUrl.indexOf('flag')!=-1){
        $('#modifyModel').show();
    }
    $scope.modifyPhone = function () {
        $('#modifyModel').show();
    }
    $scope.hideModel = function () {
        $('#modifyModel').hide();
        if(thisUrl.indexOf('flag')!=-1){
            var targetUrl = thisUrl.replace(/\?flag=1/, "");
            window.location.href = targetUrl;
        }   
    }
    /*获取验证码*/
        $scope.getVerifyCode = function () {
            if(!$scope.verifyCodeBtnStatus){
                return;
            }
            var phoneNumber = $scope.phoneNumber;
            var phoneNumbers = /^1[345678]\d{9}$/;
            if(phoneNumber != "" && !phoneNumbers.test(phoneNumber)) {
                if( phoneNumber == undefined ) {
                    $scope.phoneNumberShow = true;
                    return;
                }else {
                    $scope.phoneNumberShow = false;
                    $scope.TruephoneShow = true;
                    $scope.exitPhone = false;
                    return;
                }
            }
            if(phoneNumber == undefined){
                $scope.phoneNumberShow = true;
                return;
            }else{
                $scope.phoneNumberShow = false;
                $scope.exitPhone = false;
            }
            if($scope.verifyCodeText != undefined) {
                return;
            }
            bjj.http.ng.get($scope, $http, '/api/account/verifyCode', {
                phoneNumber: phoneNumber
            }, function (res) {
                $(".reg_value").css('color','#999');
                var _verifyCodeTimeSpace = 60;
                setTimeout(function () {
                    $scope.verifyCodeText = _verifyCodeTimeSpace + 's';
                    _verifyCodeTimeSpace--;
                    $("#verifyCodes").attr("disabled","disabled");
                }, 0);
                $scope.verifyCodeBtnStatus = false;
                var timer = $interval(function () {
                    $scope.verifyCodeText = _verifyCodeTimeSpace + 's';
                    if (_verifyCodeTimeSpace == 0) {
                        $interval.cancel(timer);
                        $scope.verifyCodeText = undefined;
                        $scope.verifyCodeBtnStatus = true;
                        $("#verifyCodes").removeAttr("disabled");
                        return;
                    }
                    _verifyCodeTimeSpace--;
                }, 1000);

            }, function(res){
                $scope.verifyCodeBtnStatus = true;
                $scope.exitPhone = true;
                $scope.phoneNumberShow = false;
                $scope.TruephoneShow = false;
            });
        };
        /*确定*/
        $scope.changePhone= function(){
            var phoneNumber = $scope.phoneNumber;
            var verifyCode  = $scope.verifyCode;
            var phoneNumbers = /^1[345678]\d{9}$/;
            if(phoneNumber != "" && !phoneNumbers.test(phoneNumber)) {
                if( phoneNumber == undefined ) {
                    $scope.phoneNumberShow = true;
                    return;
                }else {
                    $scope.phoneNumberShow = false;
                    $scope.TruephoneShow = true;
                    $scope.exitPhone = false;
                    return;
                }
            }
            if(verifyCode == undefined){
                $scope.verifyCodeShow = true;
                return;
            }else{
                $scope.verifyCodeShow = false;
            }
            bjj.http.ng.put($scope, $http, '/api/account/phoneNumber', {
                phoneNumber:phoneNumber,
                verifyCode:verifyCode
            }, function (res) {
                $('#modifyModel').hide();
                $.cookie('showTips','false');
                $scope.mobile = phoneNumber;
                bjj.dialog.alert('success',res.msg);
                var targetUrl = thisUrl.replace(/\?flag=1/, "");
                window.location.href = targetUrl;
            },function(res){
                if(res.msg == '验证码不正确') {
                    $scope.trueVerifyCodeShow = true;
                }else {
                    bjj.dialog.alert('warning',res.msg);
                }
            });
        }
    /*获取用户信息*/
    $scope.avatar = '/theme/default/images/default-avatar.png';
    bjj.http.ng.get($scope, $http, '/api/account/user/account', {}, function (res) {
        if(res.status ==200) {
            $scope.userName = res.msg.userName;
            $scope.mobile = res.msg.phoneNumber;
            $scope.nickName = res.msg.nickName;
            $scope.gender = res.msg.gender + '';
            $scope.birthday = res.msg.birthday;
            $scope.company = res.msg.company;
            $scope.nowDuty     = res.msg.duty + '';
            $scope.qqNumber = res.msg.qqNumber;
            $scope.weChatNumber = res.msg.weChatNumber;
            $scope.email = res.msg.email;
            $scope.avatar = res.msg.avatar;
        }
    })

    /*日期插件*/
    var birthTime = {
        elem:"#birthTime",
        format:"YYYY-MM-DD",
        istime: true,
        isclear: false,
        choose:function(v){
            $scope.birthday = v;
        }
    };
    setTimeout(function(){
        laydate(birthTime);
    },0)
    $scope.toggleGender = function (v) {
        $scope.gender = v;
    }
    /*保存信息*/
    $scope.save = function(){
        $scope.qqShow = false;
        $scope.emailShow = false;
        if($scope.gender == 'null') {
            $scope.gender = 0;
        }
        var avatar = getValue($scope.avatar);
        var qq = /^\d{5,10}$/;
        var email  = /^([a-zA-Z0-9_\.\-])+\@(([a-zA-Z0-9\-])+\.)+([a-zA-Z0-9]{2,4})+$/;
        if($scope.qqNumber!="" && !qq.test($scope.qqNumber)) {
            $scope.qqShow = true;
            return;
        }
        if ($scope.email!="" && !email.test($scope.email)){
            $scope.emailShow = true;
            return;
        }
        bjj.http.ng.put($scope, $http, '/api/account/user/basic?_method=PUT', {
            avatar          : avatar,
            nickName        : getValue($scope.nickName),
            gender          : $scope.gender ,
            birthday        : $scope.birthday,
            company         : getValue($scope.company),
            duty            : $scope.nowDuty,
            qqNumber        : $scope.qqNumber,
            weChatNumber    : $scope.weChatNumber,
            email           : $scope.email
        }, function (res) {
            $.cookie('avatar', avatar, { expires: 1000, path: '/' });
            $.cookie('nickName',$scope.nickName);
            var userName = $.cookie('userName'), nickName = $.cookie('nickName'), avatars = $.cookie('avatar');
            $rootScope.user = {
                nickname: (nickName == 'null' || nickName == '') ? userName : nickName,
                avatar  : avatars == 'null' ? '/theme/default/images/default-avatar.png' : avatars,
                userName: userName
            };
            bjj.dialog.alert('success', res.msg);
            $('.bjj-account-save-view .btn-primary').attr("disabled", true);
            setTimeout(function() {
                $('.bjj-account-save-view .btn-primary').attr("disabled", false);
            }, 500)
        }, function(res) {
            bjj.dialog.alert('danger', res.msg);
        });
    };
};
function getValue(value) {
    return value == undefined ? "" : value
}
