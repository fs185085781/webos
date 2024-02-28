export default {
    template: `
        <div class="app-component">
            <el-config-provider :locale="locale">
                <!--图标专用-->
                <div v-html="svgIcon"></div>
                <!--桌面元素-->
                <desktop ref="desktop" :style="{'display':isLogin?'initial':'none'}"></desktop>
                <login ref="login" v-if="!isLogin" @login-success="checkLogin()" :lock="settings.theme.lock"></login>
                <!--右键菜单-->
                <right-menu ref="rm"></right-menu>
                <!--批量选中蒙版 -->
                <div class="more-select-wrap"></div>
            </el-config-provider>
        </div>
    `,
    props: [],
    data(){
        return {
            svgIcon:"",
            isLogin:true,
            hasShare:false,
            settings:{
                theme:{}
            },
            locale:ElementPlusLocaleZhCn
        }
    },
    methods:{
        init:function (){
            let that = this;
            that.initSvg();
            that.initMouseKeyboardEvent();
        },
        initSvg:function (){
            let that = this;
            fetch("modules/win11/imgs/icon.svg").then(function (res){
                return res.text();
            }).then(function (text){
                that.svgIcon = text;
            });
        },
        initMouseKeyboardEvent:function (){
            let that = this;
            document.onmousedown=function (e){
                that.$refs.rm.$data.rightMenu.show = false;
                that.$refs&&that.$refs.desktop&&that.$refs.desktop.$refs&&that.$refs.desktop.$refs.taskbar&&that.$refs.desktop.$refs.taskbar.taskBarEleChange(e);
            }
            document.oncontextmenu = function(e){
                return false;
            }
            document.onmousemove= function (e){
                if(that.$refs.desktop){
                    that.$refs.desktop.winMoveAction(e);
                    that.$refs.desktop.moreSelectMove(e);
                    that.$refs.desktop.dragFileEleMove(e);
                }
            }
            var onmouseup = function (e){
                if(that.$refs.desktop){
                    that.$refs.desktop.winMoveStop(e);
                    that.$refs.desktop.moreSelectOver(e);
                    that.$refs.desktop.dragFileEleOver(e);
                };
                if(e.button == 2){
                    that.$refs.rm.menuPosAndCheck(e);
                };
            }
            document.body.onmouseout = function (e){
                if(e.relatedTarget && e.relatedTarget.nodeName.toLowerCase() != "iframe"){
                    return;
                };
                onmouseup(e);
            }
            document.onmouseup= onmouseup;
            document.ondrag = function (e){
                if(that.$refs.desktop){
                    that.$refs.desktop.moreSelectOver(e);
                };
            }
        },
        checkShareAuth:async function (){
            var that = this;
            var shareCode = utils.getParamer("share");
            var sharePassword = localStorage.getItem("share"+shareCode);
            webos.context.set("showErrMsg",true);
            var res = await webos.shareFile.shareData({code:shareCode,password:sharePassword});
            if(!res){
                await that.checkLogin();
                return;
            }
            if(res.type == -1){
                webos.message.error("当前分享密码不正确");
                utils.$.prompt("请输入分享密码?",function (flag,text){
                    if(!flag){
                        that.checkLogin();
                        return;
                    }
                    localStorage.setItem("share"+shareCode,text);
                    that.checkShareAuth();
                });
            }else{
                var shareData = res.data;
                that.$refs['desktop'].openFile("{sio:"+shareData.no+"}",2,shareData.name,"");
            }
        },
        checkShare:async function (){
            var that = this;
            var hasShare = false;
            var shareCode = utils.getParamer("share");
            if(shareCode){
                hasShare = await webos.shareFile.hasShare(shareCode);
            }
            await that.checkLogin();
            if(hasShare){
                that.isLogin = true;
                await that.initSettings();
                await that.checkShareAuth();
            }
        },
        pageLock:function (){
            var that = this;
            that.isLogin = false;
            utils.delayAction(function (){
                return that.$refs && that.$refs["login"];
            },function (){
                that.$refs["login"].toLock();
            },6000)
        },
        checkLogin:async function (){
            var that = this;
            that.isLogin = await webos.user.hasLogin();
            utils.delayAction(function (){
                return that.$refs && that.$refs["rm"];
            },function (){
                that.$refs["rm"].init();
            },10000);
            utils.delayAction(function (){
                return that.$refs && that.$refs["desktop"];
            },function (){
                that.$refs["desktop"].init();
            },10000);
            webos.context.set("openWiths",undefined);
            await that.initSettings();
        },
        changeWallpaper:async function (img,type){
            var that = this;
            that.settings.theme.wallpaper = img;
            that.settings.theme.type = type;
            utils.delayAction(function (){
                return that&&that.$refs&&that.$refs.desktop
            },async function (){
                var url = that.settings.theme.wallpaper;
                url = await webos.util.url2blobUrl(url);
                that.$refs.desktop.wallpaper = {url:url,type:that.settings.theme.type};
            },10000);
            await webos.softUserData.syncObject("settings_theme_win11",that.settings.theme);
        },
        getWallpaper:function (){
            return this.settings.theme;
        },
        initSettings:async function (){
            var that = this;
            //主题配置
            var theme = await webos.softUserData.syncObject("settings_theme_win11");
            if(!theme.wallpaper){
                theme = {
                    wallpaper:"modules/win11/imgs/theme/light1.jpg",
                    lock:"modules/win11/imgs/lock.jpg",
                    type:"img"
                }
                var user_lock = localStorage.getItem("user_lock");
                if(user_lock){
                    theme.lock = user_lock;
                }
            }
            that.settings.theme = theme;
            //设置桌面壁纸
            utils.delayAction(function (){
                return that&&that.$refs&&that.$refs.desktop
            },async function (){
                var url = that.settings.theme.wallpaper;
                url = await webos.util.url2blobUrl(url);
                that.$refs.desktop.wallpaper = {url:url,type:that.settings.theme.type};
            },10000);
            //设置锁屏壁纸缓存
            localStorage.setItem("user_lock",that.settings.theme.lock);
            //触发任务栏配置
            utils.delayAction(function (){
                return that.$refs&&that.$refs.desktop&&that.$refs.desktop.$refs&&that.$refs.desktop.$refs.taskbar
            },function (){
                that.$refs.desktop.$refs.taskbar.initSettings();
            },10000);

        }
    },
    created:async function () {
        var that = this;
        var sz = ["edgeBrowser","fileExplorer","settings","store","trash"];
        var map = {};
        for (let i = 0; i < sz.length; i++) {
            map[sz[i]] = "modules/win11/imgs/icon/"+sz[i]+".png"
        }
        webos.context.set("defaultAppIcon",webos.util.mapMerge(map));
        await that.checkShare();
        that.init();
    }
}