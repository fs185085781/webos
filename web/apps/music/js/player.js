(function(){
    window.player = {
        syncGetKuGouImg:function (title){
            var that = this;
            return new Promise(function (success,error){
                that.getKuGouImg(title,function (img){
                    success(img);
                });
            });
        },
        getKuGouImg:function(title,fn){
            var that = this;
            var time = new Date().getTime()+"";
            var url = new URL("https://complexsearch.kugou.com/v2/search/song");
            var func = "f"+utils.uuid();
            var param = {
                //bitrate:"0",
                callback:func,
                //clienttime:time,
                clientver:"2000",
                dfid:"-",
                //inputtype:"0",
                //iscorrection:"1",
                //isfuzzy:"0",
                keyword:title,
                mid:time,
                page:"1",
                pagesize:"1",
                platform:"WebFilter",
                //privilege_filter:"0",
                srcappid:"2919",
                //token:"",
                userid:"0",
                //uuid:time
            }
            var list = [];
            for(var key in param){
                list.push(key+"="+param[key])
                url.searchParams.set(key,param[key])
            }
            list.sort(function (a,b) {
                return a.localeCompare(b);
            });
            var jmList = [];
            var my = "NVPh5oo715z5DIWAeQlhMDsWXXQV4hwt";
            jmList.push(my);
            for(var i=0;i<list.length;i++){
                jmList.push(list[i]);
            }
            jmList.push(my); var str = jmList.join("");
            var signature = utils.md5(str);
            url.searchParams.set("signature",signature);
            that.jsonp(url.href,function (res) {
                if(res && res.data && res.data.lists && res.data.lists.length>0 && res.data.lists[0].FileHash){
                    var hash = res.data.lists[0].FileHash;
                    var fileUrl = "https://wwwapi.kugou.com/yy/index.php?r=play/getdata&hash="+hash+"&dfid=1tyNlO3K9g4Q2Kwxmx4GiWCc&appid=1014&mid=69bcea3c872b8e473308274c057793cc&platid=4";
                    that.jsonp(fileUrl,function (res2) {
                        if(res2 && res2.data && res2.data.img){
                            var img = res2.data.img;
                            if(img.startsWith("https") || window.location.protocol == "http:"){
                                fn(img);
                            }else{
                                fn(img);
                            }
                        }else{
                            fn();
                        }
                    });
                }else{
                    fn();
                }
            },func);
        },
        jsonp:function(url,callback,callbackName,jsonp){
            var that = this;
            var map = {};
            var index = url.indexOf("?");
            var urlHost = url;
            if(index!=-1){
                map = utils.getSearchByStr(url.substring(index));
                urlHost = url.substring(0,index);
            }
            if(!jsonp){
                jsonp = "callback";
            }
            if(map[jsonp]){
                callbackName = map[jsonp];
            }
            if(!callbackName){
                callbackName = "jsonp_"+Date.now()+parseInt(Math.random()*100000);
            }
            map[jsonp] = callbackName;
            var searchStr = "";
            for(var key in map){
                if(!searchStr){
                    searchStr += "?";
                }else{
                    searchStr +="&";
                }
                searchStr += key+"="+map[key];
            }
            var realUrl = urlHost+searchStr;
            var div = document.createElement("div");
            var body = document.body;
            window[callbackName] = function(res){
                try{
                    that.cancelLoading();
                    utils.removeProp(window,callbackName);
                    utils.removeProp(window,"html"+callbackName);
                    body.removeChild(div);
                }catch(e) {
                }
                callback(res);
            }
            window["html"+callbackName] = "<script>window['"+callbackName+"'] = parent['"+callbackName+"']</"+"script><script src='"+realUrl+"'></"+"script>";
            div.innerHTML = "<iframe src=\"javascript:parent.html"+callbackName+"\" style='display:none;'></iframe>";
            body.appendChild(div);
        },
        hasInList(list, data) {
            for (let i = 0; i < list.length; i++) {
                if(list[i].path == data.path){
                    return i;
                }
            }
            return -1;
        }
    }
})()