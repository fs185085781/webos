(function(){
    /*
    * 模拟IE5环境
    * 方便在谷歌浏览器下测试IE5js的兼容情况
    * 会操作如下事情
    * 1.ieProp.ltIE9改成true
    * 2.删除window.JSON
    * 3.删除window.localStorage
    * 4.删除window.Promise
    * 5.删除String.prototype.trim
    * 7.删除String.prototype.startsWith
    * 8.删除Date.now
    * 9.删除window.addEventListener
    * */
    moniIE5(window.mnIE5);
    /*兼容ie5*/
    initIE5();
    var uihost = getJsPath("utils.js",2);
    var ieProp = getIeProp();
    window.utils = initUtils();
    utils.uihost = uihost;
    window.initConfig=function(config){
        if(window.smartInitHook){
            window.smartInitHook(config);
            utils.removeProp(window,"smartInitHook");
        }
        utils.config = config;
        document.write('<script src="'+utils.config.versionUrl+'?_='+new Date().getTime()+'"></script>');
        utils.removeProp(window,"initConfig");
    };
    window.initVersion=function(vc){
        utils.config.version = vc.version;
        var config = utils.config;
        var version = config.version;
        utils.getCurrentBootScriptPath =function(){
            var script = document.currentScript;
            if(!script){
                script = document.querySelector("script[smart-boot-script]");
            }
            var path = script.src;
            var ss = path.split("/");
            ss.length = ss.length - 1;
            path = ss.join("/");
            utils.removeProp(utils,"getCurrentBootScriptPath");
            return path;
        };
        document.write('<script src="'+utils.uihost+'/expand/core-expand.js?jsv='+version+'"></script>');
        document.write('<script src="'+utils.uihost+'/expand/smart-html.js?jsv='+version+'"></script>');
        /*页面小图标,书签小图标*/
        document.write('<link href="' + config.logo+'?jsv='+version+'" rel="shortcut icon" />');
        document.write('<link href="' + config.logo+'?jsv='+version+'" rel="bookmark" />');
        /*禁用缓存*/
        document.write("<meta http-equiv=\"Cache-Control\" content=\"no-cache,no-store\">\n" +
            "<meta http-equiv=\"Pragma\" content=\"no-cache\">");
        utils.from = utils.from || "none";
        var from = config[utils.from];
        if(from){
            utils.uiVersion=from.version;
            document.write('<script smart-boot-script src="'+utils.uihost+from.boot+'?jsv='+version+'"></script>');
        };
        if(config.debug){
            if(utils.getParamer("debug") == "true"){
                utils.plugins.push("eruda");
                utils.delayAction(function(){
                    return window.eruda!=null;
                },function(){
                    window.eruda.init();
                });
            }
        };
        if(!config.plugins.router){
            config.plugins.router = [{js:utils.uihost+'/plugins/router/'+utils.from+'.router.js?jsv='+version}];
        }
        document.write('<script src="'+utils.uihost+'/expand/utils-expand.js?jsv='+version+'"></script>');
        document.write('<link href="' + utils.uihost+'/expand/utils-expand.css?jsv='+version+'" rel="stylesheet" type="text/css" />');
        initPlugins(config.plugins,utils.plugins);
        utils.removeProp(window,"initVersion");
    };
    document.write("<script src='"+utils.uihost+"/boot/config.js'></script>");
    function initPlugins(pluginMap,plugins) {
        var version = utils.config.version;
        for(var i=0;i<plugins.length;i++){
            var arrs = pluginMap[plugins[i]];
            if(!arrs){
                continue;
            }
            for(var n=0;n<arrs.length;n++){
                var plugin = arrs[n];
                if(!plugin){
                    continue;
                }
                if(plugin.js){
                    document.write('<script src="'+utils.urlAddProp(plugin.js,"jsv",version)+'"></script>');
                }
                if(plugin.css){
                    document.write('<link href="' + utils.urlAddProp(plugin.css,"jsv",version) + '" rel="stylesheet" type="text/css" />');
                }
            }
        }
    }
    function initUtils(){
        function initBigDb(that,fn){
            if(that.indexDb){
                fn(true);
                return;
            }
            window.indexedDB = window.indexedDB || window.mozIndexedDB || window.webkitIndexedDB || window.msIndexedDB;
            var req = window.indexedDB.open("smart-ui");
            req.onerror = function(event) {
                if(fn){
                    fn(false);
                }
            };
            req.onsuccess = function(e) {
                that.indexDb = e.target.result;
                if(fn){
                    fn(true);
                }
            };
            req.onupgradeneeded = function (e){
                e.target.result.createObjectStore('smartData', {
                    keyPath: 'key'
                });
            }
        }
        var tools = {
            getParamer: function (key) {
                var map = this.getSearch();
                return map[key];
            },
            getSearch: function () {
                var search = window.location.search;
                return this.getSearchByStr(search);
            },
            getSearchByStr:function(search){
                if (search) {
                    search = search.substring(1);
                } else {
                    return {};
                }
                var strsz = search.split("&");
                var map = {};
                for (var i=0; i<strsz.length; i++){
                    var strs = strsz[i];
                    if (strs.indexOf("=") != -1) {
                        var tempsz = strs.split("=");
                        var tempkey = tempsz[0];
                        var tempvalue = tempsz[1];
                        map[tempkey] = decodeURIComponent(tempvalue);
                    }
                }
                return map;
            },
            setLocalStorage:function(key,val){
                localStorage.setObject(key,val);
            },
            getLocalStorage:function(key){
                return localStorage.getObject(key);
            },
            delLocalStorage:function(key){
                localStorage.removeItem(key);
            },
            removeProp:function(obj,fieldName){
                try{
                    delete obj[fieldName];
                }catch (e) {
                    obj[fieldName] = undefined;
                }
            },
            delayAction:function(tjFn,acFn,maxDelay){
                var that = this;
                if(!maxDelay){
                    maxDelay = 24*60*60*1000;
                }
                var key = "da"+that.uuid();
                var timeKey = "time"+key;
                that[timeKey]=Date.now();
                that[key]=function () {
                    if(Date.now()-that[timeKey]>maxDelay){
                        that.removeProp(that,key);
                        that.removeProp(that,timeKey);
                    }else{
                        if(tjFn()){
                            that.removeProp(that,key);
                            that.removeProp(that,timeKey);
                            acFn();
                        }else{
                            setTimeout(that[key],100);
                        }
                    }
                }
                that[key]();
            },
            uuid:function(){
                function S4() {
                    return (((1 + Math.random()) * 0x10000) | 0).toString(16).substring(1);
                }
                return S4() + S4() + S4() + S4() + S4() + S4() + S4() + S4();
            },
            reloadUrl:function(url){
                if(!url){
                    url = window.location.href;
                }
                url = this.urlAddProp(url,"jsv",utils.config.version);
                window.location.href = url;
            },
            urlAddProp:function (url,key,val) {
                var jing = url.indexOf("#");
                var left = "";
                var right = "";
                if(jing== -1){
                    /*无#号情况*/
                    left = url;
                }else{
                    /*有#号情况*/
                    left = url.substring(0,jing);
                    right = url.substring(jing);
                }
                if(left.indexOf("?"+key+"=")!=-1 || left.indexOf("&"+key+"=")!=-1){
                    var start = left.indexOf("?"+key+"=");
                    if(start == -1){
                        start = left.indexOf("&"+key+"=");
                    }
                    var end = left.indexOf("&",start+1);
                    if(end == -1){
                        end = left.length;
                    }
                    right = left.substring(end)+right;
                    left = left.substring(0,start+1);
                }else{
                    var type = left.indexOf("?") != -1?"&":"?";
                    left += type;
                }
                var data = key+"="+val;
                return left+data+right;
            },
            /*延时执行,延时期间内多次调用只算最后一次*/
            delayOneAction:function (keyOrOptions,time,callback) {
                var type = Object.prototype.toString.call(keyOrOptions);
                var options = keyOrOptions;
                if(type == "[object String]"){
                    options = {
                        key:keyOrOptions,
                        time:time,
                        callback:callback
                    }
                }
                /*key,time,callback*/
                var that = this;
                if(!that.actionOne){
                    that.actionOne = {};
                }
                if(!that.actionOne[options.key]){
                    that.actionOne[options.key] = {};
                }
                var action = that.actionOne[options.key];
                if(!action.callbacks){
                    action.callbacks = [];
                    action.key = options.key;
                    setTimeout(function () {
                        var callback = action.callbacks.pop();
                        that.removeProp(that.actionOne,action.key);
                        callback();
                    },options.time);
                }
                action.callbacks.push(options.callback);
            },
            syncLoadData:function(url,fn){
                var xmlhttp;
                if (window.XMLHttpRequest){
                    xmlhttp=new XMLHttpRequest();
                }else{
                    xmlhttp=new ActiveXObject("Microsoft.XMLHTTP");
                }
                xmlhttp.onreadystatechange=function(){
                    if(xmlhttp.readyState==4){
                        if(xmlhttp.status == 200){
                            try{
                                fn(xmlhttp.responseText);
                            }catch (e) {
                                fn();
                            }
                        }else{
                            fn();
                        }

                    }
                }
                xmlhttp.open("GET",url,false);
                xmlhttp.send();
                return xmlhttp;
            },
            setFormData:function(formSelect,data) {
                actionFormData(formSelect,function(field,type,name) {
                    var value = data[name];
                    if(type == "radio" || type == "checkbox"){
                        field.checked = value == field.value;
                    }else{
                        if(!value && value != 0){
                            value = "";
                        }
                        field.value = value;
                    }
                });
            },
            getFormData:function(formSelect) {
                var data = {};
                actionFormData(formSelect,function(field,type,name) {
                    if(type == "radio" || type == "checkbox"){
                        if(field.checked){
                            data[name] = field.value;
                        }
                    }else{
                        data[name] = field.value;
                    }
                });
                return data;
            },
            documentReady:function (callback) {
                if(!callback){
                    return;
                }
                this.delayAction(function () {
                    return document.readyState == "complete";
                },callback);
            },
            copyText:function (text){
                if(navigator.clipboard && navigator.clipboard.writeText){
                    navigator.clipboard.writeText(text);
                    return true;
                }else{
                    var input = document.createElement("input");
                    document.body.appendChild(input);
                    input.value = text;
                    input.select();
                    var flag = document.execCommand('copy');
                    document.body.removeChild(input);
                    return flag;
                }
            },
            pasteText:function(){
                if(window.clipboardData && window.clipboardData.getData){
                    return Promise.resolve(window.clipboardData.getData("Text"));
                }else if(navigator.clipboard){
                    return navigator.clipboard.readText();
                }else{
                    return Promise.reject("当前浏览器不支持");
                }
            },
            getCookie:function(key){
                var arr = document.cookie.match(new RegExp("(^| )"+key+"=([^;]*)(;|$)"));
                if(arr != null){
                    return unescape(arr[2]);
                }else{
                    return null;
                }
            },
            setCookie:function(key,value){
                var Days = 365*100;
                var exp  = new Date();
                exp.setTime(exp.getTime() + Days*24*60*60*1000);
                document.cookie = key + "="+ escape (value) + ";expires=" + exp.toGMTString()+";path=/";
            },
            delCookie:function (key){
                var exp = new Date();
                exp.setTime(exp.getTime() - 1);
                document.cookie= key + "=;expires="+exp.toGMTString()+";path=/";
            },
            setBigData:function(key,val,fn){
                var that = this;
                if(val === undefined){
                    val = null;
                }
                that.getBigData(key,function (data,flag){
                    if(!flag){
                        if(fn){
                            fn(false);
                        }
                        return;
                    }
                    var req;
                    if(data === undefined){
                        //新增
                        req = that.indexDb.transaction(['smartData'], 'readwrite')
                            .objectStore('smartData')
                            .add({ key: key,val:val});
                    }else{
                        //修改
                        req = that.indexDb.transaction(['smartData'], 'readwrite')
                            .objectStore('smartData')
                            .put({ key: key,val:val});
                    }
                    req.onsuccess = function (event) {
                        if(fn){
                            fn(true)
                        }
                    };
                    req.onerror = function (event) {
                        if(fn){
                            fn(false)
                        }
                    }
                });
            },
            getBigData:function(key,fn){
                var that = this;
                initBigDb(that,function (flag){
                    if(!flag){
                        if(fn){
                            fn(undefined,false);
                        }
                        return;
                    }
                    var req = that.indexDb.transaction(["smartData"]).objectStore("smartData").get(key);
                    req.onerror = function(event) {
                        if(fn){
                            fn(undefined,false);
                        }
                    };
                    req.onsuccess = function(event) {
                        if(fn){
                            var data = undefined;
                            if(req.result!==undefined){
                                data = req.result.val;
                            }
                            fn(data,true);
                        }
                    };
                });
            },
            delBigData:function(key,fn){
                var that = this;
                initBigDb(that,function (flag){
                    if(!flag){
                        if(fn){
                            fn(false);
                        }
                        return;
                    }
                    var req = that.indexDb.transaction(["smartData"],"readwrite").objectStore("smartData").delete(key);
                    req.onerror = function(event) {
                        if(fn){
                            fn(false);
                        }
                    };
                    req.onsuccess = function(event) {
                        if(fn){
                            fn(true);
                        }
                    };
                });
            },
            allBigData:function (fn) {
                var that = this;
                initBigDb(that,function (flag){
                    if(!flag){
                        if(fn){
                            fn(undefined,false);
                        }
                        return;
                    }
                    var os = that.indexDb.transaction(["smartData"]).objectStore("smartData");
                    var map = {};
                    os.openCursor().onsuccess = function (event) {
                        var cursor = event.target.result;
                        if (cursor) {
                            map[cursor.key]=cursor.value;
                            cursor.continue();
                        } else {
                            fn(map,true);
                        }
                    };
                });
            }
        }
        var jsSearch = getJsSearch("utils.js");
        var plugins = [];
        if(jsSearch.plugins && jsSearch.plugins.trim()){
            plugins=jsSearch.plugins.trim().split(",");
        }
        jsSearch.plugins = plugins;
        for(var key in jsSearch){
            tools[key] = jsSearch[key];
        }
        function getJsSearch(js){
            var scripts = document.getElementsByTagName("script");
            var map = {};
            var c;
            for (var i = 0, l = scripts.length; i < l; i++) {
                var src = scripts[i].src;
                if ((c = src.indexOf(js) ) != -1) {
                    map = tools.getSearchByStr(src.substring(c+js.length));
                    break;
                }
            }
            return map;
        }
        function actionFormData(formSelect,callback){
            var form = document.querySelector(formSelect);
            if(!form){
                console.warn("表单不可为空")
                return;
            }
            var inputs = document.querySelectorAll(formSelect+" input");
            var selects = document.querySelectorAll(formSelect+" select");
            var textareas = document.querySelectorAll(formSelect+" textarea");
            var fields = [];
            for(var i=0;i<inputs.length;i++){
                fields.push(inputs[i]);
            }
            for(var i=0;i<selects.length;i++){
                fields.push(selects[i]);
            }
            for(var i=0;i<textareas.length;i++){
                fields.push(textareas[i]);
            }
            for(var i=0;i<fields.length;i++){
                var field = fields[i];
                var name = field.getAttribute("name");
                if(!name){
                    continue;
                }
                var type = field.getAttribute("type");
                callback(field,type,name);

            }
        }
        return tools;
    }
    function getJsPath(js, length) {
        var scripts = document.getElementsByTagName("script");
        var path = "";
        for (var i = 0, l = scripts.length; i < l; i++) {
            var src = scripts[i].src;
            if (src.indexOf(js) != -1) {
                path = src;
                break;
            }
        }
        var ss = path.split("/");
        ss.length = ss.length - length;
        var url = ss.join("/");
        return url;
    }
    function initIE5(){
        /*改变json格式化--开始*/
        Date.prototype.toJSON = function () {
            return this.format("yyyy-MM-dd HH:mm:ss.fff");
        };
        if(!window.JSON){
            /*增加json*/
            window.JSON = {
                stringify:function(obj){
                    return jsonStrByData(obj);
                    function jsonStrByData(data){
                        var type = Object.prototype.toString.call(data);
                        if(type =="[object Array]"){
                            var result = "[";
                            for(var i=0;i<data.length;i++){
                                if(i>0){
                                    result += ",";
                                }
                                result += jsonStrByData(data[i]);
                            }
                            result += "]";
                            return result;
                        }else if(type == "[object Date]"){
                            return '"'+data.toJSON()+'"';
                        }else if(type == "[object Object]"){
                            if(!data){
                                var res = "undefined";
                                if(data === null){
                                    res = "null";
                                }
                                return res;
                            }
                            var result = "{";
                            var isFirst=true;
                            for(var key in data){
                                var str = jsonStrByData(data[key]);
                                if(str == "undefined"){
                                    continue;
                                }
                                if(!isFirst){
                                    result += ",";
                                }
                                result += "\""+key+"\":"+str;
                                isFirst = false;
                            }
                            result+="}";
                            return result;
                        }else if(type == "[object String]"){
                            return '"'+data+'"';
                        }else if(type == "[object Number]" || type == "[object Boolean]" || type == "[object Null]"){
                            return String(data);
                        }else{
                            return "undefined";
                        }
                    }
                },
                parse:function(str){
                    if(typeof str == "object"){
                        return str;
                    }
                    var map = {data:eval("("+str+")")};
                    parseDateDg(map);
                    function parseDateDg(data){
                        if(!data){
                            return;
                        }
                        var type = Object.prototype.toString.call(data);
                        if(type =="[object Array]"){
                            for(var i=0;i<data.length;i++){
                                setDate(data,i);
                            }
                        }else if(type == "[object Object]"){
                            for(var key in data){
                                setDate(data,key);
                            }
                        }else{
                            return;
                        }
                    }
                    function setDate(data,key){
                        if(typeof data[key] == "string" && /\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}/.test(data[key])){
                            data[key] = Date.parseDate(data[key]);
                        }else{
                            parseDateDg(data[key]);
                        }
                    }
                    return map.data;
                }
            };
        }else{
            var strToObj = JSON.parse;
            JSON.parse = function (text, reviver) {
                if(!reviver){
                    reviver = function(key,val){
                        var type = Object.prototype.toString.call(val);
                        if(type == "[object String]"){
                            return toDate(val);
                        }else{
                            return val;
                        }
                        function toDate(data) {
                            if(/\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}/.test(data)){
                                return Date.parseDate(data);
                            }else{
                                return data;
                            }
                        }
                    }
                }
                return strToObj(text,reviver);
            };
        }
        /*改变json格式化--结束*/
        /*增加localStorage实体形式--开始*/
        if(!window.Storage){
            window.Storage = function () {
            };
            window.Storage.prototype.removeItem=function(key){
                if(window.utils){
                    utils.delCookie(key);
                }
            };
            window.Storage.prototype.setItem=function(key,val){
                if(window.utils){
                    utils.setCookie(key,val);
                }
            };
            window.Storage.prototype.getItem=function(key){
                if(window.utils){
                    return utils.getCookie(key);
                }else{
                    return null;
                }
            };
        }
        Storage.prototype.getObject = function(key){
            var szStr = this.getItem(key);
            if(!szStr){
                szStr = "[]";
            }
            var sz = JSON.parse(szStr);
            return sz[0];
        };
        Storage.prototype.setObject = function(key,val){
            var sz = [val];
            if(val === undefined){
                sz = [];
            }
            this.setItem(key,JSON.stringify(sz));
        };
        try{
            if(!window.localStorage){
                window.localStorage = new Storage();
            }
        }catch (e) {
            console.log("localStorage加载失败",e);
        }
        /*增加localStorage实体形式--结束*/
        /*增加Promise--开始*/
        if(!window.Promise){
            window.Promise = function (handle) {
                if(typeof handle != "function"){
                    throw new Error('Promise must accept a function as a parameter');
                }
                var that = this;
                that._status = "pending";
                that._value = undefined;
                that._fulfilledQueues = [];
                that._rejectedQueues = [];
                var _resolve = function (val) {
                    var run = function () {
                        if(that._status != "pending"){
                            return;
                        }
                        that._status = "resolved";
                        var runResolved = function (value) {
                            var cb;
                            while (cb = that._fulfilledQueues.shift()){
                                cb(value);
                            }
                        };
                        var runRejected = function (value) {
                            var cb;
                            while (cb = that._rejectedQueues.shift()){
                                cb(value);
                            }
                        };
                        if(val instanceof Promise){
                            val["then"](
                                function (value) {
                                    that._value = value;
                                    runResolved(value);
                                },function (error) {
                                    that._value = error;
                                    runRejected(error);
                                });
                        }else{
                            that._value = val;
                            runResolved(val);
                        }
                    };
                    setTimeout(run, 0);
                };
                var _reject = function (val) {
                    if(that._status != "pending"){
                        return;
                    }
                    var run = function () {
                        that._status = "rejected";
                        that._value = val;
                        var cb;
                        while (cb = that._rejectedQueues.shift()){
                            cb(val);
                        }
                    };
                    setTimeout(run, 0);
                };
                try{
                    handle(_resolve,_reject);
                }catch (e) {
                    _reject(e);
                }
            }
            Promise.prototype["then"] = function (onFulfilled,onRejected) {
                var old = this;
                var _value = old._value;
                var _status = old._status;
                return new Promise(function (onFulfilledNext,onRejectedNext) {
                    var that = this;
                    var fulfilled = function (value) {
                        try {
                            if (typeof onFulfilled != "function") {
                                onFulfilledNext(value);
                            } else {
                                var res =  onFulfilled(value);
                                if (res instanceof Promise) {
                                    res["then"](onFulfilledNext, onRejectedNext);
                                } else {
                                    onFulfilledNext(res);
                                }
                            }
                        } catch (err) {
                            /* 如果函数执行出错，新的Promise对象的状态为失败*/
                            onRejectedNext(err);
                        }
                    }
                    /* 封装一个失败时执行的函数 */
                    var rejected = function(error){
                        try {
                            if (typeof onRejected != "function") {
                                onRejectedNext(error);
                            } else {
                                var res = onRejected(error);
                                if (res instanceof Promise) {
                                    res["then"](onFulfilledNext, onRejectedNext);
                                } else {
                                    onFulfilledNext(res);
                                }
                            }
                        } catch (err) {
                            onRejectedNext(err)
                        }
                    };
                    if(_status == "pending"){
                        old._fulfilledQueues.push(fulfilled);
                        old._rejectedQueues.push(rejected);
                    }else if(_status == "resolved"){
                        fulfilled(_value);
                    }else{
                        rejected(_value);
                    }
                });
            };
            Promise.prototype["catch"] = function (onRejected) {
                return this["then"](undefined, onRejected);
            };
            Promise.prototype["finally"] = function (cb) {
                return this["then"](function (value) {
                    return Promise.resolve(cb())["then"](function () {
                        return value;
                    });
                },function (reason) {
                    return Promise.resolve(cb())["then"](function () {
                        throw reason;
                    });
                });
            };
            Promise.resolve = function (value) {
                if (value instanceof Promise) {
                    return value;
                }
                return new Promise(function (resolve) {
                    return resolve(value);
                });
            };
            Promise.reject = function (value) {
                if (value instanceof Promise) {
                    return value;
                }
                return new Promise(function (resolve,reject) {
                    return reject(value);
                });
            };
            Promise.all=function(list){
                return new Promise(function (resolve, reject) {
                    var values = [];
                    for(var i=0;i<list.length;i++){
                        var item = list[i];
                        item.then(function (res) {
                            values.push(res);
                            if(values.length == list.length){
                                resolve(values);
                            }
                        },function (e) {
                            reject(e);
                        });
                    }
                });
            }
            Promise.race=function (list) {
                return new Promise(function (resolve, reject) {
                    for(var i=0;i<list.length;i++){
                        var item = list[i];
                        item.then(function (res) {
                            resolve(res);
                        },function (e) {
                            reject(e);
                        });
                    }
                });
            }
        }

        /*增加Promise--结束*/
        /*增加string的trim方法*/
        if(!String.prototype.trim) {
            String.prototype.trim = function() {
                return this.replace(/^\s+|\s+$/g, '');
            }
        }
        /*增加string的startsWith方法*/
        if(!String.prototype.startsWith) {
            String.prototype.startsWith = function(str) {
                return this.indexOf(str) == 0;
            }
        }
        /*增加Date的now方法*/
        if(!Date.now) {
            Date.now = function() {
                return new Date().getTime();
            }
        }
        /*防止操作地址栏报错*/
        if(!history.pushState){
            history.pushState = function(){};
        }
        /*增加窗口绑定事件*/
        if(!window.addEventListener){
            window.eventAttr = {};
            window.addEventListener = function(type,fn){
                var list = window.eventAttr[type];
                if(!list){
                    window.eventAttr[type] = [];
                    (function(t){
                        window["on"+t] = function(){
                            var fns = window.eventAttr[t];
                            for(var i=0;i<fns.length;i++){
                                var fn = fns[i];
                                try{
                                    fn();
                                }catch (e) {
                                }
                            }
                        }
                    })(type);
                    list = window.eventAttr[type];
                }
                list.push(fn);
            }
        }
    }
    function getIeProp(){
        var ua = navigator.userAgent.toLowerCase();
        function check(str) {
            return ua.indexOf(str)!=-1;
        }
        var isIE = !check("opera") && check("msie");
        var ieProp = {};
        ieProp.ltIE9 = isIE && document.documentMode && document.documentMode < 9;
        if(window.toTestIE5){
            ieProp.ltIE9 = true;
        }
        if(ieProp.ltIE9){
            if(!document.querySelectorAll){
                document.querySelectorAll = function(selector){
                    return jQuery(selector);
                }
            }
            if(!document.querySelector){
                document.querySelector = function(selector){
                    var temp = jQuery(selector);
                    if(temp.length>0){
                        return temp[0];
                    }
                    return null;
                }
            }
        }
        return ieProp;
    }
    function moniIE5(flag) {
        if(!flag){
            return;
        }
        function deleteProp(obj,prop){
            try{
                delete obj[prop];
            }catch (e) {
                try{
                    obj[prop] = undefined;
                }catch (e) {

                }
            }
        }
        window.toTestIE5 = true;
        window.tmpJSON = window.JSON;
        deleteProp(window,"JSON");
        deleteProp(window,"localStorage");
        deleteProp(window,"Storage");
        deleteProp(window,"Promise");
        deleteProp(String.prototype,"trim");
        deleteProp(String.prototype,"startsWith");
        deleteProp(Date,"now");
        deleteProp(window,"addEventListener");
    }
})();
