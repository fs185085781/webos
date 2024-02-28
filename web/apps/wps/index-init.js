(function (){
    var url = new URL(window.location.href);
    var expAction = url.searchParams.get("expAction");
    if(expAction == "new"){
        var ext = url.searchParams.get("ext");
        var func = url.searchParams.get("func");
        parent[func](new Blob());
        return;
    }
    if(expAction == "clear"){
        console.log(window.location.href);
        return;
    }
    Vue.app({
        data(){
            return {
                toLogin:false,//金山文档扫码
                saving:false,//保存中
                saverr:false,//保存出错
                fileData:{},//文件数据
                officeData:{},//金山文档数据
                mode:0,//1.阿里云盘2非阿里云盘3金山文档
                wpsData:{},//阿里云编辑器数据
                neverNotify:false,//金山文档扫码(永不提醒)
                toCoordination:false,//协同办公弹窗
                coordinationVal:0,
                coordinationMap:{
                    1:"任何人编辑",
                    2:"任何人评论",
                    3:"任何人查看",
                    4:"未开启",
                    5:"未知"
                },
                userLogin:false
            }
        },
        methods:{
            toWps:async function (flag) {
                const that = this;
                if(that.neverNotify){
                    utils.setLocalStorage("neverNotify",true);
                }
                if(flag){
                    utils.setLocalStorage("next","wps");
                    location.reload();
                    return;
                }else{
                    utils.delLocalStorage("next");
                }
                that.toLogin = false;
                utils.$.loading("正在获取数据,请耐心等待...");
                var res = await parent.webos.wps.url({path:that.fileData.path,edit:that.fileData.expAction});
                utils.$.cancelLoading();
                if(!res){
                    utils.$.errorMsg(parent.webos.context.get("lastErrorReqMsg"));
                    return;
                };
                that.wpsData = res;
                that.mode = that.wpsData.type;
                await that.jsSdkInit(that.wpsData.url,that.wpsData.token);
            },
            saveCookie:async function (cookie){
                const that = this;
                parent.webos.context.set("showErrMsg", true);
                var flag = await parent.webos.office.saveCookie(cookie);
                if(flag){
                    await that.init();
                }
            },
            init:async function(){
                var that = this;
                var url = new URL(location.href);
                var data = {};
                url.searchParams.forEach(function (val,key){
                    data[key]= val;
                });
                that.fileData = data;
                if(utils.getLocalStorage("next") == "wps"){
                    await that.toWps(false);
                }else{
                    await that.toJinShan();
                }
            },
            toJinShan:async function () {
              const that = this;
                utils.$.loading("正在获取数据,请耐心等待...");
                var res = await parent.webos.office.url({path:that.fileData.path});
                if(!res){
                    parent.webos.message.error(parent.webos.context.get("lastErrorReqMsg"));
                    utils.$.cancelLoading();
                    return;
                }
                utils.$.cancelLoading();
                //1.文件链接  2.分享链接 3.扫码+阿里云 4.阿里云
                if(res.type == 4){
                    await that.toWps(true);
                    return;
                }else if(res.type == 3){
                    if(utils.getLocalStorage("neverNotify")){
                        await that.toWps(true);
                        return;
                    }
                    that.toLogin = true;
                    return;
                }
                that.mode = 3;
                that.officeData = res;
                that.officeData.expireTimeStr = Date.parseDate(that.officeData.expireTime).format("MM-dd HH:mm");
                await that.jsSdkInit(res.url);
            },
            jsSdkInit:async function (url,token) {
                const that = this;
                if(that.jssdk){
                    await that.jssdk.destroy();
                }
                let jssdk = WebOfficeSDK.config({
                    commonOptions:{
                        isShowTopArea:true,
                        isShowHeader:true
                    },
                    url: url, // 该地址需要后端提供，https://wwo.wps.cn/office/p/xxx
                    commandBars: [
                        {
                            cmbId: 'HeaderLeft',
                            attributes: {
                                visible: that.mode==3?true:false,
                                enable: false,
                            },
                        },
                        {
                            cmbId: 'HeaderRight',
                            attributes: {
                                visible: that.mode==3?true:false,
                                enable: true,
                            },
                        },
                        {
                            cmbId: 'ShareLink',
                            attributes: {
                                visible: that.mode==3?true:false,
                                enable: false,
                            },
                        },
                        {
                            cmbId: 'MobileHeader',
                            attributes: {
                                visible: that.mode==3?true:false,
                                enable: false,
                            },
                        },
                    ],
                });
                jssdk.on('fileStatus', (v) => {
                    if(v.status == 7 || v.status == 1 || v.status == 4){
                        that.saving = true;
                        that.saverr = false;
                        (async function () {
                            const res = await jssdk.save();
                            if(res.result == "ok"){
                                var flag = false;
                                //1.阿里云盘2非阿里云盘3金山文档
                                if(that.mode == 3){
                                    flag = await parent.webos.office.save({path:that.fileData.path});
                                }else if(that.mode == 2){
                                    flag = await parent.webos.wps.save({path:that.fileData.path,name:that.fileData.fname,fileId:that.wpsData.fileId});
                                }else if(that.mode == 1){
                                    flag = true;
                                }
                                that.saving = false;
                                if(!flag){
                                    that.saverr = true;
                                }
                            }else{
                                that.saving = false;
                                var map = {
                                    "SpaceFull":"账号空间已满",
                                    "QueneFull":"保存中请勿频繁操作",
                                    "fail":"保存失败",
                                    "SavedEmptyFile":"空文件不支持保存"
                                }
                                if(map[res.result]){
                                    that.saverr = true;
                                }
                            }
                        })();
                    }
                });
                if(token){
                    jssdk.setToken({
                        token: token
                    });
                }
                await jssdk.ready();
                if(that.mode == 3 && that.officeData.coordinationVal == 1){
                    const result = await jssdk.save();
                    if(result.error == "PermissionDenied"){
                        that.userLogin = true;
                    }
                }
                that.jssdk = jssdk;
            },
            toShowMenus:function () {
                const that = this;
                if(that.mode == 3 && that.officeData.type != 1){
                    return;
                }
                if(that.mode != 3 && !that.wpsData.hasFileAuth){
                    return;
                }
                that.$refs["dropdown"].handleOpen()
            },
            toLogoutWps:function (){
                const that = this;
                utils.$.confirm("退出后文件将从金山文档删除,并且使用阿里云盘编辑器操作文件,确认继续?",async function (flag) {
                    if(!flag){
                        return;
                    }
                    parent.webos.context.set("showErrMsg",true);
                    var flag2 = await parent.webos.office.logOut({path:that.fileData.path});
                    if(flag2){
                        await that.toWps(true);
                    }
                });
            },
            changeToJinShan:async function () {
                utils.delLocalStorage("neverNotify");
                location.reload();
            },
            toWpsWriteOrRead:function (isWrite) {
                var url = new URL(location.href);
                url.searchParams.set("expAction",isWrite?"edit":"open");
                utils.setLocalStorage("next","wps");
                location.href = url.href;
            },
            xuQi:function () {
                const that = this;
                utils.$.confirm("当前绑定到期为'"+that.officeData.expireTimeStr+"',是否续满期(此时算起+30天)?",async function (flag) {
                    if(!flag){
                        return;
                    }
                    parent.webos.context.set("showOkErrMsg",true);
                    var res = await parent.webos.office.renewal({path:that.fileData.path});
                    if(res){
                        that.officeData.expireTime = res;
                        that.officeData.expireTimeStr = Date.parseDate(res).format("MM-dd HH:mm");
                    }
                });
            },
            toCoordinationAction:function () {
                const that = this;
                that.coordinationVal = that.officeData.coordinationVal;
                that.toCoordination = true;
            },
            submitCoordinationVal:async function () {
                const that = this;
                if(that.coordinationVal == 5){
                    parent.webos.message.error("不支持此操作");
                    return;
                }
                parent.webos.context.set("showOkErrMsg",true);
                var flag = await parent.webos.office.coordination({path:that.fileData.path,value:that.coordinationVal});
                if(flag){
                    that.officeData.coordinationVal = that.coordinationVal;
                    that.toCoordination = false;
                }

            },
            checkXieTongEdit:function () {
                location.reload();
            },
            toLoginJinShan:function () {
                var a = document.createElement("a");
                a.href = "https://www.kdocs.cn/latest?from=docs";
                a.target="_blank";
                a.click();
            }
        },
        mounted:function(){
            const that = this;
            window.addEventListener("message",function (e){
                let data = e.data;
                if(data.type == "cookie"){
                    that.saveCookie(data.cookie);
                    that.toLogin = false;
                }
            });
            that.init();
        }
    });
})()