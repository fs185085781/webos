/**/
const attr = {}
export default {
    template: `
        <div class="login-component">
            <div class="lockscreen" @click="toLogin()" data-unlock="false" data-action="splash" :data-blur="loginDiv.blur"
             :style="{'background-image':'url('+lock+')'}">
            <div class="splashScreen mt-40" :data-faded="loginDiv.faded">
                <div class="text-6xl font-semibold text-gray-100">{{time.time}}</div>
                <div class="text-lg font-medium text-gray-200">{{time.week}}</div>
            </div>
            <div class="fadeinScreen login-form" :data-faded="loginDiv.loginFaded" data-unlock="false">
                <div class="imageCont prtclk rounded-full overflow-hidden" data-back="false">
                    <img :src="user.imgPath?user.imgPath:'imgs/logo.png'" alt="" class="user-avatar s200"></div>
                <div class="mt-2 text-2xl font-medium text-gray-200">{{user.nickName?user.nickName:'腾飞Webos社区版'}}</div>
                <div class="text-xs text-gray-400 mt-4" v-if="loginType == 0 && lockType == 0">
                    <el-input name="no" clearable v-model="loginData.parentUserNo" class="login-input" placeholder="主用户编码,为空则主用户登录"></el-input>
                </div>
                <div class="text-xs text-gray-400 mt-4" v-if="lockType == 0">
                    <el-input name="username" clearable v-model="loginData.username" class="login-input" placeholder="请输入用户名"></el-input>
                </div>
                <div class="text-xs text-gray-400 mt-4" v-if="lockType == 0">
                    <el-input name="password" show-password clearable v-model="loginData.password" class="login-input" placeholder="请输入密码"></el-input>
                </div>
                <div class="text-xs text-gray-400 mt-4" v-if="lockType == 1">
                    <el-input name="password" show-password clearable v-model="lockData.password" class="login-input" placeholder="请输入锁屏密码或账号密码"></el-input>
                </div>
                <div class="flex items-center mt-6 signInBtn" @click="loginSubmit()">{{lockType==0?'登录':'解锁'}}</div>
                <div v-if="lockType!=0" class="flex items-center mt-6 signInBtn" @click="logOut()">切换账号</div>
            </div>
            <div class="bottomInfo flex">
                <div class="uicon mx-2 "><img width="16" data-click="false" data-flip="false" data-invert="true"
                                              data-rounded="false" src="modules/win11/imgs/wifi.png" alt=""></div>
                <div class="uicon mx-2 "><img width="16" data-click="false" data-flip="false" data-invert="true"
                                              data-rounded="false" src="modules/win11/imgs/battery.png" alt=""></div>
            </div>
        </div>
    </div>
    `,
    props: ["lock"],
    data(){
        return {
            time:{
                time:new Date().format("HH:mm:ss"),
                week:new Date().format("M月dd日dddd"),
            },
            loginDiv:{
                blur:"false",
                faded:"false",
                loginFaded:"true",
            },
            loginData:{
                username:"",
                parentUserNo:"",
                password:"",
            },
            loginType:0,
            lockType:0,//0需要输入账号,密码 1.只需要输入密码
            lockData:{
                userId:"",
                password:"",
            },
            user:{}
        }
    },
    methods: {
        init:async function () {
            var that = this;
            if(attr.hasInit){
               return;
            }
            attr.hasInit = true;
            setInterval(function (){
                that.time = {
                    time:new Date().format("HH:mm:ss"),
                    week:new Date().format("M月dd日dddd"),
                }
            },500);
            that.user = webos.user.userLockInfo();
        },
        toLogin:function (){
            var that = this;
            that.loginDiv.blur = "true";
            that.loginDiv.faded = "true";
            that.loginDiv.loginFaded = "false";
        },
        loginSubmit:async function (){
            var that = this;
            if(that.lockType == 0){
                var type = that.loginData.parentUserNo?2:1;
                webos.context.set("showOkErrMsg",true);
                var flag =await webos.user.login(type,that.loginData.username,that.loginData.password,that.loginData.parentUserNo);
                if(flag){
                    that.$emit('login-success');
                }
            }else{
                //解锁
                webos.context.set("showOkErrMsg",true);
                var flag =await webos.user.loginByLock(that.lockData.userId,that.lockData.password);
                if(flag){
                    that.$emit('login-success');
                }
            }
        },
        logOut:function(){
            webos.user.logOut();
            var app = webos.el.findParentComponent(this,"app-component");
            app.checkLogin();
        },
        toLock:function (){
            const that = this;
            var wt = localStorage.getItem("webosToken");
            if(wt){
                wt = JSON.parse(wt);
                var token = wt.webosToken;
                var tokenStr = token.split(".")[0];
                var userData = JSON.parse(atob(tokenStr));
                var userId = userData.userId;
                that.lockData.userId = userId;
                that.lockType = 1;
                that.user = webos.user.userLockInfo();
            }else{
                that.lockType = 0;
            }
            webos.context.set("hasLogin",false);
        }
    },
    created: function () {
        var no = utils.getParamer("no");
        if(no && !isNaN(no)){
            this.loginData.parentUserNo = no;
            this.loginType = 2;
        }
        this.init();
    }
}