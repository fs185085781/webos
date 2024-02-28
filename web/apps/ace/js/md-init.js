(function (){
    Vue.app({
        data(){
            return {
                fileData:{}
            }
        },
        methods:{
            changeSeniorMd:function (){
                var url = new URL(window.location.href);
                var data = {};
                url.searchParams.forEach(function (val,key){
                    data[key]= val;
                });
                var oldUrlSz = (window.location.origin+window.location.pathname).split("/");
                oldUrlSz.length -= 1;
                var url2 = new URL(oldUrlSz.join("/")+"/index.html");
                for(var key in data){
                    url2.searchParams.set(key,data[key]);
                }
                window.location.href = url2.href;
            },
            toSaveData:function (){
                var that = this;
                that.$refs["iframe"].contentWindow.postMessage({"type":"getContent"},"*");
            },
            saveData:async function (content){
                var that = this;
                var param = {
                    file:new Blob([content]),
                    name:that.fileData.fname,
                    parentPath:that.fileData.parentPath
                }
                var flag = await parent.webos.fileSystem.uploadSmallFile(param);
                if(flag){
                    utils.$.successMsg(parent.webos.context.get("lastSuccessReqMsg"));
                }else{
                    utils.$.errorMsg(parent.webos.context.get("lastErrorReqMsg"));
                }
            },
            init:async function (){
                var that = this;
                window.addEventListener("message",function (e){
                    var param = e.data;
                    if(param.type == "init"){
                        fetch(that.fileData.url)
                            .then(function (res){return res.text()})
                            .then(function (res){
                                that.$refs["iframe"].contentWindow.postMessage({"type":"setContent","content":res},"*");
                            });
                        that.$refs["iframe"].contentWindow.postMessage({"type":"onlyRead","edit":that.fileData.expAction == "edit"},"*");
                    }else if(param.type == "getContent"){
                        var content = param.content;
                        that.saveData(content);
                    }
                });
                var url = new URL(window.location.href);
                var data = {};
                url.searchParams.forEach(function (val,key){
                    data[key]= val;
                });
                that.fileData = data;
                var sz = data.path.split("/");
                sz.length -= 1;
                that.fileData.parentPath = sz.join("/");
                that.$refs["iframe"].src = "https://support.tenfell.cn/markdown/";
            },
        },
        mounted:function(){
            this.init();
        }
    });
})()