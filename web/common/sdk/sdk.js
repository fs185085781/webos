(function (window,document) {
    if(!Blob.prototype.arrayBuffer){
        Blob.prototype.arrayBuffer = function (){
            let that = this;
            return new Promise(function (success,error){
                let reader = new FileReader();
                reader.onload = function() {
                    success(this.result);
                }
                reader.onerror = function (){
                    error();
                }
                reader.readAsArrayBuffer(that)
            });
        }
    }
    let sz = document.currentScript.src.split("/");
    sz.length -= 3;
    var host = sz.join("/");
    let commonUrl = host+"/common";
    var sdkUrl = commonUrl+"/sdk";
    let ajaxHostUrl = host+"/api";
    let webos = {
        sdkUrl:sdkUrl,
        commonUrl:commonUrl
    };
    let isShare = false;
    if(location.search.includes("share=")){
        isShare = true;
    }
    let pageId = "";
    function createWebosPageId(){
        pageId = localStorage.getItem("uid");
        if(!pageId){
            pageId = utils.uuid();
            localStorage.setItem("uid",pageId);
        }
        return pageId;
    }
    createWebosPageId();
    webos.request = {
        getTokenStr: function () {
            let str = localStorage.getItem("webosToken");
            if (str) {
                try {
                    let obj = JSON.parse(str);
                    return obj.webosToken;
                } catch (e) {
                    return "";
                }
            } else {
                return "";
            }
        },
        getUrl: function (module, action) {
            return "?module=" + module + "&action=" + action;
        },
        getAbsoluteUrl: function (module, action) {
            return ajaxHostUrl+this.getUrl(module, action);
        },
        xhrReq:function (method,url,data,progress,headerMap,xhr,responseType){
            if(!xhr){
                xhr = new XMLHttpRequest();
            }
            return new Promise(function (success,error){
                xhr.open(method, url);
                if(headerMap){
                    for(let key in headerMap){
                        let val = headerMap[key];
                        xhr.setRequestHeader(key,val);
                    }
                }
                if(responseType){
                    xhr.responseType = responseType;
                }
                xhr.onload = function() {
                    if(xhr.readyState===4) {
                        success(xhr);
                    }
                };
                xhr.upload.onprogress = function(evt) {
                    if(progress){
                        progress(evt.loaded,evt.total,1);
                    }
                };
                xhr.onprogress = function(evt) {
                    if(progress){
                        progress(evt.loaded,evt.total,2);
                    }
                };
                xhr.onerror = function() {
                    error();
                };
                try{
                    xhr.send(data);
                }catch (e){
                    error();
                }
            });
        },
        post: function (url, data) {
            let that = this;
            const showData = that.getAndRemoveShow();
            if(!url.startsWith("http")){
                url = ajaxHostUrl + url;
            };
            if(data instanceof Blob){
                //单文件上传
                const fd = new FormData();
                fd.append('file', data);
                data = fd;
            }else if(data instanceof FormData){
                //带文件的表单上传
            }else{
                data = JSON.stringify(data);
            }
            return fetch(url, {
                method: 'POST',
                body: data,
                headers: {
                    'content-type': 'application/json',
                    'webos-token': that.getTokenStr(),
                    'page-id': createWebosPageId(),
                    'app-origin':location.origin
                }
            }).then(resp => resp.json()).then(res => {
                return that.commonRes(res,showData);
            });
        },
        get: function (url) {
            let that = this;
            const showData = that.getAndRemoveShow();
            if(!url.startsWith("http")){
                url = ajaxHostUrl + url;
            };
            return fetch(url, {
                method: 'GET',
                headers: {
                    'webos-token': that.getTokenStr(),
                    'page-id': createWebosPageId(),
                    'app-origin':location.origin
                }
            }).then(resp => resp.json()).then(res => {
                return that.commonRes(res,showData);
            });
        },
        getAndRemoveShow:function (){
            let showSuccess = false;
            let showError = false;
            if(webos.context.get("showOkMsg") || webos.context.get("showOkErrMsg")){
                showSuccess = true;
            };
            if(webos.context.get("showErrMsg") || webos.context.get("showOkErrMsg")){
                showError = true;
            };
            webos.context.set("showErrMsg",false);
            webos.context.set("showOkMsg",false);
            webos.context.set("showOkErrMsg",false);
            webos.context.set("lastSuccessReqMsg",undefined);
            webos.context.set("lastErrorReqMsg",undefined);
            return {showSuccess,showError};
        },
        commonRes: function (res,showData) {
            return new Promise(function (success, error) {
                if(res.code == 407){
                    webos.context.set("install",false);
                }else{
                    webos.context.set("install",true);
                }
                if (res.code == 0) {
                    if(showData.showSuccess){
                        webos.message.success(res.msg);
                    };
                    success(res);
                } else {
                    /*-1失败 401未登录 403权限不足 404未找到 407*/
                    if(res.code == 401){
                        webos.context.set("hasLogin",false);
                        if(!isShare){
                            webos.user.userLock(true);//被动锁定
                        }
                    }else if(res.code == 408){
                        if(!isShare){
                            webos.user.userLock(true);//被动锁定
                        }
                    };
                    if(showData.showError && res.code != 401){
                        if(res.msg){
                            webos.message.error(res.msg);
                        }else{
                            webos.message.error("未知错误");
                        }
                    }
                    error(res.msg);
                }
            });
        },
        commonPostData:function(param,urlFn,resFn){
            let url = urlFn();
            return webos.request.post(url, param).then(function (res) {
                webos.context.set("lastSuccessReqMsg",res.msg);
                return resFn(res.data,true);
            }).catch(function (err){
                webos.context.set("lastErrorReqMsg",err);
                return resFn(err,false);
            });
        },
        commonData:function (module, action,param){
            return webos.request.commonPostData(param,function(){
                return webos.request.getUrl(module, action);
            },function(data,flag){
                if(flag){
                    return data;
                }else{
                    return null;
                }
            });
        },
        commonFlag:function (module, action,param){
            return webos.request.commonPostData(param,function(){
                return webos.request.getUrl(module, action);
            },function(data,flag){
                if(flag){
                    return true;
                }else{
                    return false;
                }
            });
        }
    };
    webos.softStore={
        storeApi:"https://support.tenfell.cn/store/php/index.php",
        request:function (action,param){
            var url = this.storeApi+"?type="+action;
            return fetch(url, {
                method: 'POST',
                body: JSON.stringify(param),
                headers: {
                    'content-type': 'application/json'
                }
            }).then(resp => resp.json());
        },
        commonData:function (action,param){
            var that = this;
            return that.request(action,param).then(function(res){
                if(res.flag){
                    webos.context.set("lastSuccessReqMsg",res.msg);
                    return res.data;
                }else{
                    webos.context.set("lastErrorReqMsg",res.msg);
                    return null;
                }
            }).catch(function(){
                webos.context.set("lastErrorReqMsg","接口访问失败");
                return null;
            });
        },
        commonFlag:function (action,param){
            var that = this;
            return that.request(action,param).then(function(res){
                if(res.flag){
                    webos.context.set("lastSuccessReqMsg",res.msg);
                }else {
                    webos.context.set("lastErrorReqMsg", res.msg);
                }
                return res.flag;
            }).catch(function(){
                webos.context.set("lastErrorReqMsg","接口访问失败");
                return false;
            });
        },
        indexList:function(firstCat){
            return this.commonData("indexList",{firstCat});
        },
        rating:function(param) {
            return this.commonData("rating",param);
        },
        list:function (param){
            return this.commonData("clientList",param);
        },
        secondCats:function (param){
            return this.commonData("secondCats",param);
        },
        info:function (code){
            return this.commonData("info",{code});
        },
        downFile:function (fileId){
            return this.commonData("downFile",{fileId});
        }
    };
    webos.softUser={
        list:function (){
            return webos.request.commonData("softUser", "list",{});
        },
        hasList:function (){
            return webos.request.commonData("softUser", "hasList",{});
        },
        install:function (param){
            return webos.request.commonFlag("softUser", "install",param);
        },
        uninstall:function (code){
            return webos.request.commonFlag("softUser", "uninstall",{code});
        },
        addIframe:function (param){
            return webos.request.commonFlag("softUser", "addIframe",param);
        },
        addSoft:function (file){
            return webos.request.commonFlag("softUser", "addSoft",file);
        },
        checkUpdate:function (param){
            return webos.request.commonFlag("softUser", "checkUpdate",param);
        },
        update:function (param){
            return webos.request.commonFlag("softUser", "update",param);
        },
        hasInstall:function (code){
            return webos.request.commonFlag("softUser", "hasInstall",{code});
        }
    };
    webos.ioFileAss={
        list:function (){
            return webos.request.commonData("ioFileAss", "list",{});
        }
    };
    webos.shareFile = {
        findOne:function (param){
            return webos.request.commonData("shareFile", "findOne",param);
        },
        getCode:function (){
            return webos.request.commonData("shareFile", "getCode",{});
        },
        save:function (param){
            return webos.request.commonFlag("shareFile", "save",param);
        },
        hasShare:function (code){
            return webos.request.commonFlag("shareFile", "hasShare",{code});
        },
        shareData:function (param){
            return webos.request.commonData("shareFile", "shareData",param);
        },
        list:function (param){
            return webos.request.commonData("shareFile", "list",param);
        },
        dels:function (ids){
            return webos.request.commonFlag("shareFile", "dels",ids);
        },
        info:function (id){
            return webos.request.commonData("shareFile", "info",{id});
        },
    };
    webos.user = {
        del:function (id) {
            return webos.request.commonFlag("user", "del",{id});
        },
        defaultPwd:function () {
            return webos.request.commonData("user", "defaultPwd",{});
        },
        info:async function () {
            var user = webos.context.get("loginUser");
            if(!user){
                user = await webos.request.commonData("user", "info",{});
                if(user){
                    webos.context.set("loginUser",user);
                    localStorage.setItem("loginUserInfo",JSON.stringify(user));
                }
            }else{
                user = JSON.parse(JSON.stringify(user));
            }
            return user;
        },
        infoById:function (id) {
            return webos.request.commonData("user", "infoById",{id});
        },
        lock:function () {
            return webos.request.commonFlag("user", "lock",{});
        },
        checkLock:function () {
            if(isShare){
                return false;
            }
            return webos.request.commonFlag("user", "checkLock",{});
        },
        login:function (userType, username, password,parentUserNo) {
            let data = webos.request.commonData("user", "login",{userType, username, password,parentUserNo});
            return data.then(function (res){
                if(res){
                    localStorage.setItem("webosToken", JSON.stringify(res));
                    webos.context.set("hasLogin",true);
                    return true;
                }else{
                    return false;
                }
            }).catch(function (){
                return false;
            });
        },
        loginByLock:function (userId,password){
            let data = webos.request.commonData("user", "loginByLock",{userId,password});
            return data.then(function (res){
                if(res){
                    localStorage.setItem("webosToken", JSON.stringify(res));
                    webos.context.set("hasLogin",true);
                    return true;
                }else{
                    return false;
                }
            }).catch(function (){
                return false;
            });
        },
        userLock:function (flag){
            //flag true被动锁定,false主动锁定
            if(!flag){
                webos.user.lock();
            }
            try{
                vm.$refs["app"].pageLock();
            }catch (e){
            }
        },
        userLockInfo:function (){
            try{
                var user = webos.context.get("loginUser");
                if(user){
                    return user;
                }
                var token = webos.request.getTokenStr();
                var tokenStr = token.split(".")[0];
                var userData = JSON.parse(atob(tokenStr));
                var userId = userData.userId;
                var userStr = localStorage.getItem("loginUserInfo");
                var tmpUser = JSON.parse(userStr);
                if(tmpUser.id == userId){
                    return tmpUser;
                }
            }catch (e){

            }
            return {};
        },
        checkAndRefreshToken: function () {
            try {
                let tokenObj = JSON.parse(localStorage.getItem("webosToken"));
                let time = tokenObj.expireTime;
                if (time < 1) {
                    /*当前数据异常不刷新*/
                    return;
                }
                if (time > new Date().getTime() + 10 * 60 * 1000) {
                    /*过期时间超过当前时间10分钟不刷新*/
                    return;
                }
                let url = webos.request.getUrl("user", "refreshToken");
                webos.request.post(url, tokenObj).then(function (res) {
                    if (res.code == 0) {
                        localStorage.setItem("webosToken", JSON.stringify(res.data));
                        return true;
                    } else {
                        webos.user.userLock();//refreshToken失败锁定
                        return false;
                    }
                }).catch(function (){
                    webos.user.userLock();//refreshToken失败锁定
                });
            } catch (e) {
                /*当前未登录不刷新*/
            }
        },
        list:function (param){
            return webos.request.commonData("user", "list",param);
        },
        createChild:function (param){
            return webos.request.commonFlag("user", "createChild",param);
        },
        createMain:function (param){
            return webos.request.commonFlag("user", "createMain",param);
        },
        reg:function (param){
            let data = webos.request.commonData("user", "reg",param);
            return data.then(function (res){
                if(res){
                    localStorage.setItem("webosToken", JSON.stringify(res));
                    return true;
                }else{
                    return false;
                }
            }).catch(function (){
                return false;
            });
        },
        update:function (param){
            return webos.request.commonFlag("user", "update",param);
        },
        updatePassword:function (oldPassword,password){
            return webos.request.commonFlag("user", "updatePassword",{oldPassword,password});
        },
        updateInfo:async function (param){
            var flag = await webos.request.commonFlag("user", "updateInfo",param);
            if(flag){
                var user = await webos.request.commonData("user", "info",{});
                if(user){
                    webos.context.set("loginUser",user);
                }
            }
            return flag;
        },
        resetPassword:function (id){
            return webos.request.commonFlag("user", "resetPassword",{id});
        },
        select:function(){
            return webos.request.commonData("user", "select",{});
        },
        selectMap:function(){
            return webos.request.commonData("user", "selectMap",{});
        },
        logOut:function (){
            localStorage.removeItem("webosToken");
            webos.context.set("hasLogin",false);
        },
        hasLogin:async function (){
            if(webos.context.get("hasLogin") != undefined){
                return webos.context.get("hasLogin");
            }else{
                var info = await this.info();
                var flag = !!info;
                webos.context.set("hasLogin",flag);
                return flag;
            }
        }
    };
    webos.dict = {
        list:function (param){
            return webos.request.commonData("dict", "list",param);
        },
        edit:function (param){
            return webos.request.commonFlag("dict", "edit",param);
        },
        childEdit:function (param){
            return webos.request.commonFlag("dict", "childEdit",param);
        },
        info:function(id){
            return webos.request.commonData("dict", "info",{id});
        },
        childInfo:function(id){
            return webos.request.commonData("dict", "childInfo",{id});
        },
        dels:function (codes){
            return webos.request.commonFlag("dict", "dels",codes);
        },
        childDels:function (ids){
            return webos.request.commonFlag("dict", "childDels",ids);
        },
        select:function(code){
            return webos.request.commonData("dict", "select",{code});
        },
        selectMap:function(code){
            return webos.request.commonData("dict", "selectMap",{code});
        }
    };
    webos.userRecycle = {
        list:function (param){
            return webos.request.commonData("userRecycle", "list",param);
        },
        restoreByPaths:function (paths){
            return webos.request.commonFlag("userRecycle", "restoreByPaths",paths);
        },
        clearByPaths:function (paths){
            return webos.request.commonFlag("userRecycle", "clearByPaths",paths);
        },
        clear:function (){
            return webos.request.commonFlag("userRecycle", "clear",{});
        },
        count:function (){
            return webos.request.commonData("userRecycle", "count",{});
        }
    };
    webos.drive = {
        list:function (param){
            return webos.request.commonData("ioDrive", "list",param);
        },
        getFolderByParentPath:function (param){
            return webos.request.commonData("ioDrive", "getFolderByParentPath",param);
        },
        getTokenId:function (driveType,tokenId){
            return webos.request.commonData("ioDrive", "getTokenId",{driveType,tokenId});
        },
        info:function(id){
            return webos.request.commonData("ioDrive", "info",{id});
        },
        update:function(param){
            return webos.request.commonFlag("ioDrive", "update",param);
        },
        save:function(param){
            return webos.request.commonFlag("ioDrive", "save",param);
        },
        dels:function (ids){
            return webos.request.commonFlag("ioDrive", "dels",ids);
        },
        select:function(){
            return webos.request.commonData("ioDrive", "select",{});
        },
        selectMap:function(){
            return webos.request.commonData("ioDrive", "selectMap",{});
        },
        aliyunEvent:function (fn){
            var that = this;
            if(that.initAliyunEvent){
                that.aliyunEventAction = fn;
                return;
            }
            if(!that.initAliyunEvent){
                that.initAliyunEvent = true;
                that.aliyunEventAction = fn;
                (function () {
                    function b(e) {
                        var t = atob(e)
                            , r = t.length
                            , n = new Uint8Array(r);
                        while (r--)
                            n[r] = t.charCodeAt(r);
                        return new Blob([n])
                    }
                    function v(e){
                        return new Promise((function(t, r) {
                                var n = b(e)
                                    , a = new FileReader;
                                a.onloadend = function(e) {
                                    t(e.target.result)
                                }
                                    ,
                                    a.onerror = function(e) {
                                        return r(e)
                                    }
                                    ,
                                    a.readAsText(n, "gbk")
                            }
                        ))
                    }
                    var jmdecode = v;
                    window.addEventListener("message",function (ev) {
                        var data = decodeURIComponent(ev.data);
                        var obj = JSON.parse(data);
                        if(obj.loginResult && obj.action == "loginResult"){
                            jmdecode(obj.bizExt).then(function(res){
                                var json = JSON.parse(res);
                                var user = json.pds_login_result;
                                var accessToken = user.accessToken;
                                var refreshToken = user.refreshToken;
                                var expireTime = user.expireTime;
                                var token = {
                                    access_token:accessToken,
                                    expire_time:expireTime
                                };
                                that.aliyunEventAction({"type":"token","data":{"token":token,"refreshToken":refreshToken}});
                            });
                        }else if(obj.event == "select_path"){
                            that.aliyunEventAction({"type":"select_path","data":obj});
                        }
                    });
                })();
            }
        }
    };
    webos.userDrive = {
        list:function (param){
            return webos.request.commonData("ioUserDrive", "list",param);
        },
        info:function(id){
            return webos.request.commonData("ioUserDrive", "info",{id});
        },
        update:function(param){
            return webos.request.commonFlag("ioUserDrive", "update",param);
        },
        save:function(param){
            return webos.request.commonFlag("ioUserDrive", "save",param);
        },
        dels:function (ids){
            return webos.request.commonFlag("ioUserDrive", "dels",ids);
        },
        specialFiles :function (type){
            return webos.request.commonData("ioUserDrive", "specialFiles",{type:type});
        },
        specialPath :function (type){
            return webos.request.commonData("ioUserDrive", "specialPath",{type:type});
        },
        starList :function (){
            return webos.request.commonData("ioUserDrive", "starList",{});
        }
    };
    webos.fileSystem = {
        fileHashSimple:function (file){
            var that = this;
            var l = function(e, t) {
                var i = new FileReader;
                i.readAsArrayBuffer(e),
                    i.onload = function() {
                        t(i.result)
                    }
            };
            var c = function(t, i) {
                t.slice = t.mozSlice || t.webkitSlice || t.slice;
                var e = t;
                if (1e4 < t.size) {
                    for (var n = parseInt(t.size / 50), a = [], s = 0; s < 50; s++) {
                        var o = t.slice(n * s, n * s + 200);
                        a.push(o)
                    }
                    var r = t.slice(t.size - 200, t.size);
                    a.push(r),
                        e = new Blob(a)
                }
                l(e, async function(e) {
                    e = await that.fileMd5(new Blob([e])) + t.size;
                    i(e)
                })
            };
            return new Promise(function (success,error){
                c(file,success);
            });
        },
        fileMd5:function(file,fn){
            return new Promise(function (success,error){
                if(!window.SparkMD5){
                    utils.syncLoadData(webos.sdkUrl+"/spark-md5.min.js",function(text){
                        eval(text);
                    });
                };
                var blobSlice = File.prototype.slice || File.prototype.mozSlice || File.prototype.webkitSlice,
                    chunkSize = 2097152,
                    chunks = Math.ceil(file.size / chunkSize),
                    currentChunk = 0,
                    spark = new SparkMD5.ArrayBuffer(),
                    frOnload = function(e){
                        spark.append(e.target.result);
                        currentChunk++;
                        if(fn){
                            fn(chunkSize*currentChunk,file.size);
                        }
                        if (currentChunk < chunks){
                            loadNext();
                        }else{
                            success(spark.end());
                        }
                    },
                    frOnerror = function () {
                        error();
                    };
                function loadNext() {
                    var fileReader = new FileReader();
                    fileReader.onload = frOnload;
                    fileReader.onerror = frOnerror;
                    var start = currentChunk * chunkSize,
                        end = ((start + chunkSize) >= file.size) ? file.size : start + chunkSize;
                    fileReader.readAsArrayBuffer(blobSlice.call(file, start, end));
                };
                loadNext();
            });
        },
        isSpecialPath:function (path){
            return path == "thispc" || path == "disk" || path == "share" || path == "trash";
        },
        webCrossCopy:async function (files,sourceName,pathName,callback){
            const that = this;
            if(sourceName.length>3){
                sourceName = sourceName.substr(0,3)+"...";
            }
            if(pathName.length>3){
                pathName = pathName.substr(0,3)+"...";
            }
            //为保证数据安全,跨盘不支持剪切
            var groupId = utils.uuid();
            for (let i = 0; i < files.length; i++) {
                const fileData = files[i];
                const task = {
                    id:utils.uuid(),
                    groupId:groupId,
                    canInterrupt:false,
                    cancelType:2,
                    sourceName:sourceName,
                    sd:0,
                    jd:0,
                    loaded:0,
                    size:fileData.size,
                    status:0,
                    pathName:pathName,
                    name:fileData.fileName,
                    methodName:"复制",
                    callback:callback
                }
                task.callback(task);
                const getFileAndProcess = async function(){
                    if(task.size == 0){
                        return new File([], fileData.fileName);
                    }else{
                        task.startTime = Math.floor(new Date().getTime()/1000);
                        task.status = 1;
                        task.name = "(下载中)"+fileData.fileName;
                        task.currentXhr = new XMLHttpRequest();
                        var data = await webos.request.xhrReq("GET",await that.zl(fileData.source),undefined,function (loaded,total,type){
                                if(type != 2){
                                    return;
                                }
                                let jd = loaded/task.size;
                                let time = Math.floor(new Date().getTime()/1000) - task.startTime;
                                let sd = loaded/1024/time;/*kb/s*/
                                if(sd == Infinity){
                                    sd = 0;
                                };
                                task.jd = jd;
                                task.sd = sd;
                                task.loaded = loaded;
                                if(task.callback){
                                    task.callback(task);
                                }
                            },
                            {
                            },task.currentXhr,"blob").catch(function (){
                                //不填表示返回空白
                        });
                        if(!data){
                            task.name = "(下载出错)"+fileData.fileName;
                            task.status = 3;
                            task.msg = "下载出错,请尝试服务器传输";
                            if(task.callback){
                                task.callback(task);
                            }
                        }
                        task.name = fileData.fileName;
                        return task.currentXhr.response;
                    }
                }
                getFileAndProcess().then(function (file){
                    const fullPath = "/"+fileData.name;
                    that.addUploadFile({
                        file:file,
                        name:fileData.fileName,
                        path:fileData.path,
                        fullPath:fullPath,
                        pathName:sourceName,
                        sourceName:pathName,
                        callback:callback,
                        groupId:groupId,
                        id:task.id
                    });
                });
            }
        },
        pathEncrypt:function(path){
            return webos.request.commonData("fileSystem", "pathEncrypt",{path});
        },
        addShareProp:function (param){
            var shareCode = utils.getParamer("share");
            if(shareCode){
                param.shareCode = shareCode;
                param.sharePwd = localStorage.getItem("share"+shareCode);
            }
        },
        uploadSmallFile:function (param){
            var formData = new FormData();
            for(var key in param){
                formData.append(key, param[key]);
            }
            return webos.request.commonData("fileSystem", "uploadSmallFile",formData);
        },
        zl:async function (path,type){
            //type 1下载内容(使用重定向)  2.ajax获取内容(支持的使用重定向,不支持的使用中转)
            if(!type || type != 2){
                type = 1;
            }
            const that = this;
            var pathCipher = await that.pathEncrypt(path);
            var url = webos.request.getAbsoluteUrl("fileSystem",type==1?"url":"content")+"&path="+encodeURIComponent(pathCipher);
            if(path.startsWith("{sio:")){
                var share = utils.getParamer("share");
                if(share){
                    url += "&share="+encodeURIComponent(share);
                    var pwd = localStorage.getItem("share"+share)
                    if(pwd){
                        url += "&pwd="+encodeURIComponent(pwd);
                    }
                }
            }
            return url;
        },
        zlByName:async function (path){
            return await this.zl(path)+"&lastname=1";
        },
        fileList:function (param){
            this.addShareProp(param);
            return webos.request.commonData("fileSystem", "fileList",param);
        },
        availableMainName:function (param){
            return webos.request.commonData("fileSystem", "availableMainName",param);
        },
        unzip:function (param){
            return webos.request.commonFlag("fileSystem", "unzip",param);
        },
        downUrl:function(param){
            this.addShareProp(param);
            return webos.request.commonData("fileSystem", "downUrl",param);
        },
        remove:function(param){
            return webos.request.commonFlag("fileSystem", "remove",param);
        },
        copy:function(param){
            return webos.request.commonData("fileSystem", "copy",param);
        },
        move:function(param){
            return webos.request.commonData("fileSystem", "move",param);
        },
        serverJd:function(taskId){
            return webos.request.commonData("fileSystem", "serverJd",{taskId});
        },
        serverStop:function(taskId){
            return webos.request.commonFlag("fileSystem", "serverStop",{taskId});
        },
        serverConfirm:function(taskId){
            return webos.request.commonFlag("fileSystem", "serverConfirm",{taskId});
        },
        getDriveType:function (path){
            var param = {path};
            this.addShareProp(param);
            return webos.request.commonData("fileSystem", "getDriveType",param);
        },
        fileInfo:function (path){
            var param = {path};
            this.addShareProp(param);
            return webos.request.commonData("fileSystem", "fileInfo",param);
        },
        zip:function(param){
            return webos.request.commonData("fileSystem", "zip",param);
        },
        rename:function(param){
            return webos.request.commonFlag("fileSystem", "rename",param);
        },
        createDir:function(param){
            return webos.request.commonData("fileSystem", "createDir",param);
        },
        pathName:function(param){
            this.addShareProp(param);
            return webos.request.commonData("fileSystem", "pathName",param);
        },
        fileIconCalc:async function (item){
            if(item.type == 2){
                item.filterName = item.name;
                return;
            }
            if(item.name.endsWith(".webosapp")){
                item.filterName = item.name.substring(0,item.name.length-9);
                var fileCache = await webos.fileSystem.getFileCache(item.path);
                if(!fileCache){
                    return;
                }
                var text = String.fromCharCode.apply(null, new Uint8Array(fileCache));
                text = decodeURIComponent(escape(text));
                var data = JSON.parse(text);
                if(data.icon){
                    item.thumbnail = data.icon;
                }
                if(!item.thumbnail){
                    var defaultAppIcon = webos.context.get("defaultAppIcon");
                    if(!defaultAppIcon){
                        defaultAppIcon={};
                    }
                    var tmpThumbnail = defaultAppIcon[data.app];
                    if(tmpThumbnail){
                        item.thumbnail = tmpThumbnail;
                    }
                }
            }else{
                var defaultFileIcon = webos.context.get("defaultFileIcon");
                if(!item.thumbnail && item.ext){
                    var tmpThumbnail = defaultFileIcon[item.ext.toLowerCase()];
                    if(tmpThumbnail){
                        item.thumbnail = tmpThumbnail;
                    }
                }
                if("doc,dot,wps,wpt,docx,dotx,docm,dotm,rtf,xls,xlt,et,xlsx,xltx,xlsm,xltm,ppt,pptx,pptm,ppsx,ppsm,pps,potx,potm,dpt,dps".split(",").includes(item.ext.toLowerCase())){
                    item.thumbnail = defaultFileIcon[item.ext.toLowerCase()];
                }
                item.filterName = item.name;
            }
        },
        getFileListByParentPath:async function (path,fn){
            //fn回调函数,如果存在就调用,参数1为[],参数2为{dataPath,pathData,title}
            var _that = this;
            var that = {};
            path = path.replace(/\\/g,"/");
            if(path && path !="/" && path.endsWith("/")){
                path = path.substring(0,path.length-1);
            }
            that.dataPath = path;
            if(path == "thispc"){
                let fileList = [];
                that.pathData = [{pathName:"此电脑",path:"thispc"}];
                that.title = "此电脑";
                webos.context.set("showErrMsg", true);
                var res = await webos.userDrive.list({"current":1,"pageSize":9999});
                var list = res.data;
                for(var i=0;i<list.length;i++){
                    var tmp = list[i];
                    fileList.push({
                        isSystem:tmp.isSystem,
                        filterName:tmp.name,
                        path:"{uio:"+tmp.no+"}",
                        size:0,
                        type:2
                    });
                }
                if(fn){
                    fn(fileList,that);
                }
                that.contentFiles = fileList;
            }else if(path == "disk"){
                let fileList = [];
                that.pathData = [{pathName:"磁盘管理",path:"disk"}];
                that.title = "磁盘管理";
                webos.context.set("showErrMsg", true);
                var res = await webos.drive.list({"current":1,"pageSize":9999});
                var list = res.data;
                for(var i=0;i<list.length;i++){
                    var tmp = list[i];
                    fileList.push({
                        filterName:tmp.name,
                        path:"{io:"+tmp.no+"}",
                        size:0,
                        type:2,
                        id:tmp.id,
                        driveType:tmp.driveType
                    });
                }
                if(fn){
                    fn(fileList,that);
                }
                that.contentFiles = fileList;
            }else if(path == "share"){
                history.pushState("", "", location.pathname);
                let fileList = [];
                that.pathData = [{pathName:"我的共享",path:"share"}];
                that.title = "我的共享";
                webos.context.set("showErrMsg", true);
                var res = await webos.shareFile.list({"current":1,"pageSize":9999});
                var list = res.data;
                for(var i=0;i<list.length;i++){
                    var tmp = list[i];
                    var item = JSON.parse(tmp.files);
                    item.id = tmp.id;
                    await _that.fileIconCalc(item);
                    fileList.push(item);
                }
                if(fn){
                    fn(fileList,that);
                }
                that.contentFiles = fileList;
            }else if(path == "trash"){
                let fileList = [];
                that.pathData = [{pathName:"回收站",path:"trash"}];
                that.title = "回收站";
                webos.context.set("showErrMsg", true);
                var res = await webos.userRecycle.list({"current":1,"pageSize":9999});
                var list = res.data;
                for(var i=0;i<list.length;i++){
                    var tmp = list[i];
                    var item = {
                        name:tmp.name,
                        path:"{trash:"+tmp.id+"}",
                        size:tmp.size,
                        type:tmp.type,
                        id:tmp.id,
                        ext:webos.util.getExtByName(tmp.name)
                    };
                    await _that.fileIconCalc(item);
                    fileList.push(item);
                }
                if(fn){
                    fn(fileList,that);
                }
                that.contentFiles = fileList;
            }else{
                var param = {path:that.dataPath};
                var pathName = await webos.fileSystem.pathName(param);
                if(pathName){
                    var paths = that.dataPath.split("/");
                    if(that.dataPath == "/"){
                        paths = [""];
                    };
                    var pathNames = pathName.split("/");
                    if(paths.length != pathNames.length){
                        webos.message.error("文件夹和磁盘名称请勿出现斜杆'/'符号");
                        return;
                    };
                    that.title = pathNames[pathNames.length-1];
                    var pathData = [];
                    for(var i=0;i<pathNames.length;i++){
                        var tmpPaths = [];
                        for(var n=0;n<=i;n++){
                            tmpPaths.push(paths[n]);
                        };
                        var pathSzStr = tmpPaths.join("/");
                        if(!pathSzStr){
                            pathSzStr = "/";
                        }
                        pathData.push({
                            pathName:pathNames[i],
                            path:pathSzStr
                        });
                    };
                    that.pathData = pathData;
                };
                let fileList = [];
                var type = 0;
                var next = "";
                while(true){
                    webos.context.set("showErrMsg", true);
                    var param = {
                        parentPath:that.dataPath,
                        type:type,
                        next:next
                    };
                    var data = await webos.fileSystem.fileList(param);
                    if(!data){
                        break;
                    };
                    if(data.list){
                        let oneList = data.list;
                        for (let i = 0; i < oneList.length; i++) {
                            let item = oneList[i];
                            await _that.fileIconCalc(item);
                        }
                        if(fn){
                            fn(oneList,that);
                        }
                        fileList = fileList.concat(oneList);
                    };
                    if(data.type == 0){
                        break;
                    };
                    if(!data.next){
                        break;
                    }
                    type = data.type;
                    next = data.next;
                };
                that.contentFiles = fileList;
            };
            return that;
        },
        unBindEleToDragUpload:function (ele){
            if(!ele.uploadEvent){
                return;
            }
            ele.removeEventListener('dragenter',ele.uploadEvent.dragenter);
            ele.removeEventListener('dragleave',ele.uploadEvent.dragleave);
            ele.removeEventListener('drop',ele.uploadEvent.drop);
            ele.removeEventListener('dragover',ele.uploadEvent.dragover);
            ele.uploadEvent = undefined;
        },
        bindEleToDragUpload:function(param){
            let that = this;
            /*ele,enter,leave,drop,path,progress,complete
            ele 绑定拖拽元素
            enter 拖入回调函数
            leave 拖出回调函数
            drop 拖放回调函数
            path 获取服务器路径函数
            callback 回调函数*/
            function uploadDirectorys(dirs,groupId){
                for(let i=0;i<dirs.length;i++){
                    const dir = dirs[i];
                    if(!dir){
                        continue;
                    };
                    if(dir.isDirectory){
                        dir.createReader().readEntries(function (tempDirs){
                            uploadDirectorys(tempDirs,groupId);
                        });
                    }else{
                        dir.file(function (file){
                            that.addUploadFile({
                                file:file,
                                fullPath:dir.fullPath,
                                path:param.path(),
                                pathName:param.pathName(),
                                sourceName:param.sourceName(),
                                callback:param.callback,
                                groupId:groupId
                            });
                        });
                    }
                }
            }
            function disableDefaultEvents() {
                const doc = document.documentElement;
                doc.addEventListener('dragleave', (e) => e.preventDefault());
                doc.addEventListener('drop', (e) => e.preventDefault());
                doc.addEventListener('dragenter', (e) => e.preventDefault());
                doc.addEventListener('dragover', (e) => e.preventDefault());
            }
            disableDefaultEvents();
            let ele = param.ele;
            ele.entry = false;
            let dragenter = function (e){
                if(param.enter && !ele.entry){
                    ele.entry = true;
                    param.enter(ele,e);
                }
            };
            let dragleave = function(e){
                if(param.leave){
                    if(!e.relatedTarget || !webos.el.isChildren(ele,e.relatedTarget)){
                        ele.entry = false;
                        param.leave(ele,e);
                    }else{
                    }
                }
            };
            let drop = function(e){
                let items = e.dataTransfer.items;
                let dirs = [];
                for(let i=0;i<items.length;i++){
                    let item = items[i];
                    var dir = item.webkitGetAsEntry();
                    if(dir){
                        dirs.push(dir);
                    };
                };
                var groupId = that.uuid();
                var upload = param.canUpload(ele,e);
                if(upload){
                    uploadDirectorys(dirs,groupId);
                }
                if(param.drop){
                    param.drop(ele,e,upload);
                }
            };
            let dragover = function(e){
                if(param.dragover){
                    param.dragover(ele,e);
                }
            };
            ele.addEventListener('dragenter',dragenter);
            ele.addEventListener('dragleave',dragleave);
            ele.addEventListener('drop',drop);
            ele.addEventListener('dragover',dragover);
            ele.uploadEvent = {
                dragenter:dragenter,
                dragleave:dragleave,
                drop:drop,
                dragover:dragover
            };
        },
        uuid:function(){
            function S4() {
                return (((1 + Math.random()) * 0x10000) | 0).toString(16).substring(1);
            }
            return S4() + S4() + S4() + S4() + S4() + S4() + S4() + S4();
        },
        hasUploading:function (id){
            let that = this;
            if(!that.uploadFileMap){
                return false;
            };
            let param = that.uploadFileMap[id];
            if(!param){
                return false;
            };
            return true;
        },
        uploadPause:function (id){
            let that = this;
            if(!that.uploadFileMap){
                return {flag:false,msg:"此上传数据不存在"};
            };
            let param = that.uploadFileMap[id];
            if(!param){
                return {flag:false,msg:"此上传数据不存在"};
            };
            if(param.status != 1){
                return {flag:false,msg:"只有上传中的任务可进行暂停"};
            };
            if(param.currentXhr){
                try{
                    param.currentXhr.abort();
                }catch (e){

                }
                param.currentXhr = undefined;
            }
            param.status = 4;
            param.statusChange(param);
            return {flag:true,msg:"任务已暂停"};
        },
        uploadStart:function (id){
            let that = this;
            if(!that.uploadFileMap){
                return {flag:false,msg:"此上传数据不存在"};
            };
            let param = that.uploadFileMap[id];
            if(!param){
                return {flag:false,msg:"此上传数据不存在"};
            };
            if(param.status != 4){
                return {flag:false,msg:"只有暂停中的任务可进行恢复"};
            };
            that.uploadFile(id);
            return {flag:true,msg:"任务已恢复"};
        },
        uploadCancel:function (id){
            let that = this;
            if(!that.uploadFileMap){
                return {flag:false,msg:"此上传数据不存在"};
            };
            let param = that.uploadFileMap[id];
            if(!param){
                return {flag:false,msg:"此上传数据不存在"};
            };
            if(param.currentXhr){
                try{
                    param.currentXhr.abort();
                }catch (e){

                }
                param.currentXhr = undefined;
            };
            param.status = 5;
            param.statusChange(param);
            return {flag:true,msg:"任务已取消"};
        },
        uploadRestart:function (id){
            let that = this;
            if(!that.uploadFileMap){
                return {flag:false,msg:"此上传数据不存在"};
            };
            let param = that.uploadFileMap[id];
            if(!param){
                return {flag:false,msg:"此上传数据不存在"};
            }
            if(param.status != 3){
                return {flag:false,msg:"只有上传失败的任务可进行重试"};
            }
            if(param.errorCount>2){
                that.addUploadFile(param);
                return {flag:true,msg:"任务已重新开始上传"};
            }else{
                param.status = 4;
                return that.uploadStart(id);
            }

        },
        uploadCheckAndProcess:function (){
            let that = this;
            if(!that.uploadFileMap){
                return;
            };
            /*status 0等待执行,1上传中,2上传成功,3上传失败,4暂停中,5已取消*/
            let count = 0;
            for(let key in that.uploadFileMap){
                let data = that.uploadFileMap[key];
                if(data.status == 2 || data.status == 5){
                    delete that.uploadFileMap[key];
                    continue;
                }
                if(count>=5){
                    break;
                }
                if(data.status == 1){
                    count++;
                    continue;
                }
                if(data.status == 3 || data.status == 4){
                    continue;
                };
                if(data.status != 0){
                    continue;
                };
                that.uploadFile(data.id);
                count++;
            }
        },
        addUploadFile:async function (item){
            let that = this;
            /*file,fullPath,path,callback
            file 文件对象
            fullPath 文件路径以/开头
            path 服务器路径以{io:1},{uio:1}开头
            callback 回调函数*/
            if(!that.uploadFileMap){
                that.uploadFileMap = {};
            }
            let param = {
                name:item.name,
                file:item.file,
                size:item.file.size,
                fullPath:item.fullPath,
                path:item.path,
                pathName:item.pathName,
                sourceName:item.sourceName,
                callback:item.callback,
                errorCount:0,
                taskType:"upload",
                statusChange:function(res){
                    if(res.status == 3){
                        res.errorCount += 1;
                    };
                    if(res.callback){
                        res.callback(res);
                    };
                    that.uploadCheckAndProcess();
                },
                id:item.id,
                groupId:item.groupId,
                status:0,
                canInterrupt:true,
                cancelType:0
            };
            param.name = param.name?param.name:param.file.name;
            param.id = param.id?param.id:that.uuid();
            param.groupId = param.groupId?param.groupId:that.uuid();
            param.fpSize = 1024*1024*10;
            let fps = Math.floor(param.size/param.fpSize);
            if(param.size%param.fpSize != 0){
                fps ++;
            };
            param.needFps = fps;
            param.currentFp = 0;
            that.uploadFileMap[param.id] = param;
            param.statusChange(param);
        },
        uploadFile:async function(id){
            let that = this;
            if(!that.uploadFileMap){
                return;
            }
            let param = that.uploadFileMap[id];
            if(!param){
                return;
            };
            param.status = 1;
            if(!param.jd){
                param.jd = 0;
            };
            if(!param.sd){
                param.sd = 0;
            };
            param.statusChange(param);
            if(!param.filePath){
                if(param.fullPath[0] != "/"){
                    param.status = 3;
                    param.msg = "文件必须以/开头";
                    param.statusChange(param);
                    return;
                }
                param.filePath = param.fullPath.substring(1);
            };
            var errMsg = "";
            if(!param.driveType){
                param.driveType = await webos.fileSystem.getDriveType(param.path);
                errMsg = webos.context.get("lastErrorReqMsg");
            };
            if(!param.driveType){
                param.status = 3;
                param.msg = errMsg?errMsg:"磁盘位置不存在";
                param.statusChange(param);
                return;
            };
            let progress = function (loaded,total,type){
                /*if(type != 1){
                    return;
                }*/
                let cSize = loaded+param.fpSize*param.currentFp;
                let jd = cSize/param.size;
                let time = Math.floor(new Date().getTime()/1000) - param.startTime;
                let size = cSize - param.startSize;
                let sd = size/1024/time;/*kb/s*/
                if(sd == Infinity){
                    sd = 0;
                };
                param.jd = jd;
                param.sd = sd;
                param.loaded = cSize;
                if(param.callback){
                    param.callback(param);
                }
            };
            /*status 0等待执行,1上传中,2上传成功,3上传失败,4暂停中*/
            var uploadType = param.driveType;
            if(uploadType == "server"){
                uploadType = "local";
            }
            if(!webos.fileSystem[uploadType+"Upload"]){
                utils.syncLoadData(sdkUrl+"/upload-"+uploadType+".js",function(text){
                    eval(text);
                });
            }
            webos.fileSystem[uploadType+"Upload"].upload(param,progress);
        },
        setCacheValue:function (key,val){
            utils.setBigData("cache_"+key,{data:val,createTime:Date.now()});
        },
        getCacheValue:function (key){
            return new Promise(function (success,error){
                utils.getBigData("cache_"+key,function(cache,flag){
                    if(cache && cache.createTime){
                        success(cache.data);
                    }else{
                        success()
                    }
                });
            });
        },
        getCache:function (key,fn,second){
            let back = async function (){
                return await fn();
            };
            return new Promise(async function (success,error){
                if(second == 0 || second == undefined || second == null){
                    let data = await back();
                    success(data);
                    return;
                };
                utils.getBigData("cache_"+key,async function(cache,flag){
                    if(!flag){
                        let data = await back();
                        success(data);
                        return;
                    };
                    let tmpData;
                    if(cache === null || cache === undefined){
                        tmpData = await back();
                        if(tmpData){
                            cache = {data:tmpData,time:Date.now()+second*1000,createTime:Date.now()}
                            utils.setBigData("cache_"+key,cache);
                        }
                    }else if(Date.now() > cache.time){
                        tmpData = cache.data;
                        back().then(function (data){
                            if(data){
                                let tmpCache = {data:data,time:Date.now()+second*1000,createTime:Date.now()};
                                utils.setBigData("cache_"+key,tmpCache);
                            }
                        });
                    }else{
                        if(!cache.data){
                            tmpData = await back();
                            if(tmpData){
                                cache = {data:tmpData,time:Date.now()+second*1000,createTime:Date.now()}
                                utils.setBigData("cache_"+key,cache);
                            }
                        }else{
                            tmpData = cache.data;
                        }
                    }
                    success(tmpData);
                });
            });
        },
        getFileCache:async function (path){
            return webos.fileSystem.getCache("file_"+path,async function (){
                var url = await webos.fileSystem.downUrl({path:path});
                if(!url){
                    return false;
                };
                const buffer = await fetch(url).then(function (res) {
                    return res.arrayBuffer();
                }).catch(function (){
                    return false;
                });
                if(buffer.byteLength == 0){
                    return false;
                }
                return buffer;
            },300);
        }
    };
    webos.softUserData = {
        get:function(param){
            return webos.request.commonData("softUserData", "get",param);
        },
        save:function(param){
            return webos.request.commonFlag("softUserData", "save",param);
        },
        syncData:async function (code,str){
            var that = this;
            if(!str){
                var res = await that.get({appCode: code});
                return res;
            }else{
                await that.save({appCode: code, data: str});
                return str;
            }
        },
        syncObject:async function(code,object){
            var that = this;
            var str = undefined;
            if(object){
                str = JSON.stringify(object);
            }
            var res = await that.syncData(code,str);
            if(!res){
                res = "{}";
            }
            return JSON.parse(res);
        },
        syncList:async function(code,list){
            var that = this;
            var str = undefined;
            if(list){
                str = JSON.stringify(list);
            }
            var res = await that.syncData(code,str);
            if(!res){
                res = "[]";
            }
            return JSON.parse(res);
        }
    };
    webos.wps={
        url:function (param){
            webos.fileSystem.addShareProp(param);
            return webos.request.commonData("wps", "url",param);
        },
        save:function (param){
            return webos.request.commonFlag("wps", "save",param);
        }
    };
    webos.office={
        url:function (param){
            webos.fileSystem.addShareProp(param);
            return webos.request.commonData("office", "url",param);
        },
        save:function (param){
            webos.fileSystem.addShareProp(param);
            return webos.request.commonFlag("office", "save",param);
        },
        saveCookie:function (cookie){
            return webos.request.commonFlag("office", "saveCookie",{cookie});
        },
        logOut:function (param){
            return webos.request.commonFlag("office", "logOut",param);
        },
        renewal:function (param){
            return webos.request.commonData("office", "renewal",param);
        },
        coordination:function (param){
            return webos.request.commonFlag("office", "coordination",param);
        },
    };
    webos.el = {
        findParentComponent:function(that,clazz){
            while (true){
                if(!that){
                    return false;
                };
                if(that && that._ && that._.vnode && that._.vnode.el && that._.vnode.el.classList && that._.vnode.el.classList.contains(clazz)){
                    return that;
                };
                that = that.$parent;
            }
        },
        isChildren:function(ele,target){
            let flag = false;
            while (true){
                if(!target || target == document.body){
                    flag = false;
                    break;
                }
                if(target == ele){
                    flag = true;
                    break;
                }else{
                    target = target.parentElement;
                }
            }
            return flag;
        },
        isInClass:function (target,clazz){
            var flag = false;
            while (true){
                if(!target || target == document.body){
                    flag = false;
                    break;
                }
                if(target.classList.contains(clazz)){
                    flag = target;
                    break;
                }else{
                    target = target.parentElement;
                }
            }
            return flag;
        },
        animationCss:function (oldData,newData,cssId){
            for(var key in oldData){
                oldData[key] = oldData[key]+"px";
            }
            var newObj = {};
            for(var key in oldData){
                newObj[key] = newData[key]+"px";
            }
            return this.animationCssObj(oldData,newObj,cssId);
        },
        animationCssObj:function (oldObj,newObj,cssId){
            if(!cssId){
                cssId = "window-animation-css";
            }
            var css = document.querySelector("#"+cssId);
            if(!css){
                css = document.createElement("style");
                css.id = cssId;
                document.body.appendChild(css);
            }
            var oldStr = "";
            for(var key in oldObj){
                oldStr += key+":"+oldObj[key]+";";
            }
            var newStr = "";
            for(var key in newObj){
                newStr += key+":"+newObj[key]+";";
            }
            var id = "a"+webos.fileSystem.uuid();
            css.innerHTML = "@keyframes "+id+"{0%{"+oldStr+"}100%{"+newStr+"}}";
            return id;
        },
        dialogCenter:function(dialog){
            utils.delayAction(function(){
                return dialog.dialogContentRef && dialog.dialogContentRef.$refs && dialog.dialogContentRef.$refs.headerRef && dialog.dialogContentRef.$refs.headerRef.parentElement && dialog.dialogContentRef.$refs.headerRef.parentElement.clientWidth>0;
            },function(){
                var ele = dialog.dialogContentRef.$refs.headerRef.parentElement;
                var width = ele.clientWidth;
                var height = ele.clientHeight;
                var left = 0;
                var top = 0;
                if(ele.style.transform){
                    left = ele.style.transform.split("(")[1].split("px")[0]*1;
                    top = ele.style.transform.split(",")[1].split("px")[0].trim()*1;
                }
                left = (document.body.clientWidth - width)/2 - left;
                top = (document.body.clientHeight - height)/2 - top;
                ele.style.left = left+"px";
                ele.style.top = top+"px";
            });
        }
    };
    webos.util = {
        url2blobUrl:async function(url){
            try{
                if(new URL(url).origin != location.origin){
                    return url;
                }
            }catch (e){
                return url;
            }
            var blob = await webos.fileSystem.getCache(url,function (){
                return fetch(url).then(function (res){return res.blob()}).catch(function (res){return url;});
            },999999999);
            if(blob instanceof Blob){
                return URL.createObjectURL(blob);
            }else{
                return url;
            }
        },
        isMedia:function (ext){
            var music = "mp3,flac,aac,ogg,wav,m4a".split(",");
            if(music.includes(ext.toLowerCase())){
                return true;
            }
            var video = "mp4,ogg,webm,flv,m3u8,mpd,torrent,mkv,mov,m3u8x".split(",");
            if(video.includes(ext.toLowerCase())){
                return true;
            }
            var picture = "jpeg,png,gif,bmp,jpg,tiff,svg,ico".split(",");
            if(picture.includes(ext.toLowerCase())){
                return true;
            }
            return false;
        },
        solar2lunar:function (date){
            //公转农
            if(!window.solarlunar){
                utils.syncLoadData(webos.sdkUrl+"/solarlunar.min.js",function(text){
                    eval(text);
                });
            };
            return window.solarlunar.solar2lunar(date.getFullYear(),date.getMonth()+1,date.getDate());
        },
        lunar2solar:function (date){
            //农转公
        },
        getCacheTheme:function (){
            return localStorage.getItem("web_theme");
        },
        setCacheTheme:function (theme){
            localStorage.setItem("web_theme",theme);
            try{
                var iframes = document.querySelectorAll("iframe");
                for (let i = 0; i < iframes.length; i++) {
                    var iframe = iframes[i];
                    try{
                        iframe.contentWindow.postMessage({"action":"themeChange","theme":theme},"*");
                    }catch (e){
                    }
                }
            }catch (e){
            }
        },
        fullScreen:function (flag){
            try{
                if(flag){
                    var de = document.documentElement;
                    var full = de.requestFullscreen||de.msRequestFullscreen||de.mozRequestFullScreen||de.webkitRequestFullscreen;
                    full.apply(de).catch(function (){
                    });
                }else{
                    var de = document;
                    var exitFull = de.exitFullscreen||de.msExitFullscreen||de.mozCancelFullScreen||de.webkitExitFullscreen;
                    exitFull.apply(de);
                }
            }catch (e){
            }
        },
        setBigData:function(key,val){
            return new Promise(function (success,error){
                utils.setBigData(key,val,function (flag){
                    if(flag){
                        success(true);
                    }else{
                        error(false);
                    }
                });
            });
        },
        getBigData:function(key){
            return new Promise(function (success,error){
                utils.getBigData(key,function (data,flag){
                    if(flag){
                        success(data);
                    }else{
                        error(false);
                    }
                });
            });
        },
        mapMerge:function (tmpMap){
            var dataMap = {};
            for(var key in tmpMap){
                var keys = key.split(",");
                var val = tmpMap[key];
                for (let i = 0; i < keys.length; i++) {
                    dataMap[keys[i]] = val;
                }
            }
            return dataMap;
        },
        defaultFileIcon:function (){
            var iconStr = "asm,ogg,lnk,eps,hlp,md,vst,arj,indd,xsl,cdr,xlsb,h,class,gif,code,psd,as,jar,asax,resx,tga,air,dwg,vdx,csproj,accdb,ifc,xaml,,vsdx,sitx,pkg,vcxproj,movie,msg,dwf,mkv,xps,cer,dll,asmx,y,autodesk,key,pst,json,bz2,o,cpp,zip,dng,vsd,doc,vss,dot,pl,eml,file,tar,sql,java,music,prproj,py,skp,rb,js,xml,rar,numbers,pub,docx,3dm,txt,ico,rtf,docm,ini,xltx,font,exe,rvt,pot,png,gz,vcf,vtx,ashx,xls,pptx,hdr,cab,avi,html,potx,iam,dotx,mhtml,swift,vbs,jpg,xap,cmd,3ds,mov,aspx,ldf,bin,dotm,rmvb,xlt,vnd,dae,mht,dtd,msi,vbproj,vdw,pdf,ppt,ipa,epub,c,dxf,flv,fla,odt,sln,chm,dmg,apk,tgz,swf,cs,iso,php,mpp,suo,css,makefile,xsd,mdb,mdf,vsx,framework,ods,svg,ai,mpt,s,cshtml,reg,bmp,pps,mp4,xlsx,csv,ascx,vcproj,xlsm,stl,odp,midi,7z,wasm,djvu,ppsx,pdb,fbx,f,vb,lrc,subtitle";
            var icons = iconStr.split(",");
            var map = {};
            for (let i = 0; i < icons.length; i++) {
                let icon = icons[i];
                if(!icon){
                    continue;
                };
                if(icon == "music"){
                    var sz = "mp3,wma,wav,ape,flac,ogg,aac,m4a".split(",");
                    for (let j = 0; j < sz.length; j++) {
                        map[sz[j]] = "imgs/file_icon/"+icon+".png";
                    }
                    continue;
                }
                if(icon == "movie"){
                    var sz = "m3u8x,m3u8,wmv,avchd,webm,m4v,mpeg,vob,ogv,3gp,f4v".split(",");
                    for (let j = 0; j < sz.length; j++) {
                        map[sz[j]] = "imgs/file_icon/"+icon+".png";
                    }
                    continue;
                }
                if(icon == "subtitle"){
                    var sz = "srt,vtt,ass".split(",");
                    for (let j = 0; j < sz.length; j++) {
                        map[sz[j]] = "imgs/file_icon/"+icon+".png";
                    }
                    continue;
                }
                map[icon] = "imgs/file_icon/"+icon+".png";
            }
            return map;
        },
        getExtByName:function (name){
            var sz = name.split(".");
            if(sz.length>1){
                return sz[sz.length-1].toLowerCase();
            }else{
                return "";
            }
        },
        userOpenApp:async function (ext,expActions){
            var privateApp = await webos.util.systemOpenUrl(ext,expActions);
            if(!privateApp){
                var openWiths = webos.context.get("openWiths");
                if(!openWiths){
                    var list = await webos.ioFileAss.list();
                    if(!list){
                        webos.context.set("openWiths",[]);
                        openWiths = webos.context.get("openWiths");
                    }else{
                        openWiths = [];
                        for (let i = 0; i < list.length; i++) {
                            var one = list[i];
                            if(one.action == "openwith"){
                                var menu = {name:one.actionName,action:"openWithExt",iconUrl:one.iconUrl,ext:one.ext,url:one.url,show:true,appName:one.appName,expAction:one.expAction};
                                openWiths.push(menu);
                            }
                        }
                        webos.context.set("openWiths",openWiths);
                    }
                }
                var openWith = null;
                for (let i = 0; i < expActions.length; i++) {
                    let expAction = expActions[i];
                    for (let j = 0; j < openWiths.length; j++) {
                        var tmp = openWiths[j];
                        if(tmp.ext.indexOf(ext) != -1 && expAction == tmp.expAction){
                            openWith = tmp;
                            break;
                        }
                    }
                    if(openWith){
                        break;
                    }
                }
                if(!openWith){
                    return false;
                }
                privateApp = openWith;
            }
            return privateApp;
        },
        systemOpenUrl:async function(ext,expActions){
            //edit  open
            var allApp = {
                "ace2":{
                    data:{
                        url:"apps/ace/md-senior.html",
                        appName:"代码编辑器",
                        iconUrl:"apps/ace/icon.png"
                    },
                    edit:"md,markdown",
                    open:"md,markdown"
                },
                "wps":{
                    data:{
                        url:"apps/wps/index.html",
                        appName:"腾飞Office",
                        iconUrl:"apps/wps/icon.png"
                    },
                    edit:"doc,dot,wps,wpt,docx,dotx,docm,dotm,rtf,xls,xlt,et,xlsx,xltx,xlsm,xltm,ppt,pptx,pptm,ppsx,ppsm,pps,potx,potm,dpt,dps",
                    open:"doc,dot,wps,wpt,docx,dotx,docm,dotm,rtf,xls,xlt,et,xlsx,xltx,csv,xlsm,xltm,ppt,pptx,pptm,ppsx,ppsm,pps,potx,potm,dpt,dps,pdf"
                },
                "album":{
                    data:{
                        url:"apps/album/index.html",
                        appName:"相册",
                        iconUrl:"apps/album/icon.png"
                    },
                    edit:"",
                    open:"jpeg,png,gif,bmp,jpg,tiff,svg,ico"
                },
                "music":{
                    data:{
                        url:"apps/music/index.html",
                        appName:"音乐播放器",
                        iconUrl:"apps/music/icon.png"
                    },
                    edit:"",
                    open:"mp3,flac,aac,ogg,wav,m4a"
                },
                "video":{
                    data:{
                        url:"apps/video/index.html",
                        appName:"视频播放器",
                        iconUrl:"apps/video/icon.png"
                    },
                    edit:"",
                    open:"mp4,ogg,webm,flv,m3u8,mpd,torrent,mkv,mov,m3u8x,m4v"
                },
                "ace":{
                    data:{
                        url:"apps/ace/index.html",
                        appName:"代码编辑器",
                        iconUrl:"apps/ace/icon.png"
                    },
                    edit:"pp,css,sbt,ion,py,stylus,patch,robot,cakefile,java,nim,bat,ocamlmakefile,xml,makefile,blade.php,shtml,nix,scm,sco,ctp,bro,exs,tex,module,textile,diff,kts,pgsql,njk,mediawiki,rb,str,as,rd,groovy,aw,reds,wlk,abap,aql,phtml,rs,ru,scheme,jsm,json5,jsp,scad,lucene,jsx,latte,rst,rss,wsdl,sh,snippets,markdown,sm,typescript,tgr,cc,gemfile,a,cson,abc,c,cf,sv,curly,d,svh,e,cfg,svg,f,gitignore,h,haml,hpp,cfm,m,cr,ejs,cs,p,xaml,txt,r,tf,component,s,rdf,v,mjs,asm,asl,asp,ts,di,scss,pql,latex,vhdl,ps1,red,handlebars,mysql,adb,ada,swift,yaml,cxx,hrl,pas,elm,lisp,mli,vb,rakumod,vh,ex,swig,vm,hbs,smithy,hjson,gemspec,page,html.eex,wpy,psc,bashrc,gql,xbl,fs,mml,mustache,we,htpasswd,fsscript,sqlserver,prisma,twig,oak,json,ge,mask,asciidoc,htm,orc,dockerfile,go,cjs,prolog,xhtml,prefs,coffee,gbs,xq,bib,bash,sjs,hh,nunjucks,styl,pug,vert,slim,conf,hs,gcode,xul,apex,hx,asl.json,adoc,sass,rakefile,clj,epp,ahk,io,frt,zeek,mathml,cls,cmd,fsi,scrypt,fsl,jl,nsh,log,nsi,vbs,html.erb,jq,dot,wpgm,js,jssm_state,guardfile,skim,fsx,wtest,praat,cabal,dart,jssm,feature,erb,nunjs,rakutest,vala,rkt,fth,cljs,erl,ftl,proc,praatscript,kt,rhtml,trigger,toml,cshtml,tpl,liquid,cob,php,logic,atom,htaccess,lp,lql,vue,ls,wiki,php4,php5,space,php3,pig,gnumakefile,mc,md,html,soy,drl,ml,matlab,mm,cpp,nginx,resource,4th,cirru,edi,less,mz,mixal,proto,pl6,smarty,dsl,nj,vfp,frag,lsl,alda,scala,p6,phpt,jade,partiql,glsl,mush,phps,sql,qml,sac,mel,pm6,tsx,eex,yml,tcl,make,inc,ini,htgroups,plg,ino,cbl,raku,jack,ltx,vhd,csd,c9search_results,lua,ldr,xslt,f90,pl,rake,pm,properties,m3u8,m3u8x,lrc,lrcx",
                    open:"pp,css,sbt,ion,py,stylus,patch,robot,cakefile,java,nim,bat,ocamlmakefile,xml,makefile,blade.php,shtml,nix,scm,sco,ctp,bro,exs,tex,module,textile,diff,kts,pgsql,njk,mediawiki,rb,str,as,rd,groovy,aw,reds,wlk,abap,aql,phtml,rs,ru,scheme,jsm,json5,jsp,scad,lucene,jsx,latte,rst,rss,wsdl,sh,snippets,markdown,sm,typescript,tgr,cc,gemfile,a,cson,abc,c,cf,sv,curly,d,svh,e,cfg,svg,f,gitignore,h,haml,hpp,cfm,m,cr,ejs,cs,p,xaml,txt,r,tf,component,s,rdf,v,mjs,asm,asl,asp,ts,di,scss,pql,latex,vhdl,ps1,red,handlebars,mysql,adb,ada,swift,yaml,cxx,hrl,pas,elm,lisp,mli,vb,rakumod,vh,ex,swig,vm,hbs,smithy,hjson,gemspec,page,html.eex,wpy,psc,bashrc,gql,xbl,fs,mml,mustache,we,htpasswd,fsscript,sqlserver,prisma,twig,oak,json,ge,mask,asciidoc,htm,orc,dockerfile,go,cjs,prolog,xhtml,prefs,coffee,gbs,xq,bib,bash,sjs,hh,nunjucks,styl,pug,vert,slim,conf,hs,gcode,xul,apex,hx,asl.json,adoc,sass,rakefile,clj,epp,ahk,io,frt,zeek,mathml,cls,cmd,fsi,scrypt,fsl,jl,nsh,log,nsi,vbs,html.erb,jq,dot,wpgm,js,jssm_state,guardfile,skim,fsx,wtest,praat,cabal,dart,jssm,feature,erb,nunjs,rakutest,vala,rkt,fth,cljs,erl,ftl,proc,praatscript,kt,rhtml,trigger,toml,cshtml,tpl,liquid,cob,php,logic,atom,htaccess,lp,lql,vue,ls,wiki,php4,php5,space,php3,pig,gnumakefile,mc,md,html,soy,drl,ml,matlab,mm,cpp,nginx,resource,4th,cirru,edi,less,mz,mixal,proto,pl6,smarty,dsl,nj,vfp,frag,lsl,alda,scala,p6,phpt,jade,partiql,glsl,mush,phps,sql,qml,sac,mel,pm6,tsx,eex,yml,tcl,make,inc,ini,htgroups,plg,ino,cbl,raku,jack,ltx,vhd,csd,c9search_results,lua,ldr,xslt,f90,pl,rake,pm,properties,lrc,lrcx"
                }
            };
            for(let i=0;i<expActions.length;i++){
                let expAction = expActions[i];
                for(let app in allApp){
                    var tmp = allApp[app];
                    if(app === "ace2"){
                        app = "ace";
                    }
                    if(!webos.context.get("hasInstall"+expAction+app)){
                        webos.context.set("hasInstall"+expAction+app,await webos.softUser.hasInstall(app));
                    }
                    if(!webos.context.get("hasInstall"+expAction+app)){
                        continue;
                    }
                    var str = tmp[expAction];
                    if(!str){
                        continue;
                    }
                    if(!str.split(",").includes(ext)){
                        continue;
                    }
                    const appData = tmp.data;
                    appData.expAction = expAction;
                    return appData;
                }
            }
            return false;
        },
        getMainByName:function (name){
            var sz = name.split(".");
            if(sz.length>1){
                sz.length = sz.length-1;
            }
            return sz.join(".");
        },
        getParentPath:function (path){
            var sz = path.split("/");
            sz.length = sz.length - 1;
            return sz.join("/");
        },
        getCurrentWinByIframe:function(thatWindow){
            if(!parent.vm){
                return false;
            };
            var app = parent.vm.$refs['app'];
            if(!app){
                return false;
            };
            var desktop = app.$refs["desktop"];
            if(!desktop){
                return false;
            };
            var iframes = parent.document.querySelectorAll("iframe");
            var iframe = null;
            for (let i = 0; i < iframes.length; i++) {
                if(iframes[i].contentWindow == thatWindow){
                    iframe = iframes[i];
                    break;
                }
            }
            var winId = iframe.dataset.id;
            var wins = desktop.$refs["wins_dialog"];
            for (let i = 0; i < wins.length; i++) {
                var win = wins[i].$props.win;
                if(win.id == winId){
                    return wins[i];
                }
            }
            return false;
        },
        setParentSimple:function (thatWindow){
            var that = this;
            var winCom = that.getCurrentWinByIframe(thatWindow);
            if(winCom){
                winCom.$props.win.isSimple = true;
            }
        }
    };
    webos.message = {
        info: function (text) {
            this.commonMsg(text,"信息");
        },
        error: function (text) {
            this.commonMsg(text,"错误");
        },
        warn: function (text) {
            this.commonMsg(text,"警告");
        },
        success: function (text) {
            this.commonMsg(text,"成功");
        }
    };
    webos.context = {
        set:function(key,val){
            this[key] = val;
        },
        get:function (key,val){
            return this[key];
        }
    };
    window.webos = webos;
    webos.context.set("defaultFileIcon",webos.util.defaultFileIcon());
    /*检查token刷新*/
    let count = 0;
    setInterval(function () {
        count++;
        if(count % 6 == 0){
            //每隔1分钟检查一次刷新
            webos.user.checkAndRefreshToken();
        }
        if(count % 3 == 0){
            //每隔30秒检查一次锁定
            webos.user.checkLock();
        }
        if(count >= 10000){
            count = 0;
        }
    }, 10000);
    utils.delayAction(function (){
        return window.vm && vm.$refs["app"] && window.vm.$refs["app"].$data.isLogin && webos.user;
    },function (){
        webos.user.checkLock();
        //双重检查
        setTimeout(function (){
            webos.user.checkLock();
        },2000);
    },10000);
})(window,document)