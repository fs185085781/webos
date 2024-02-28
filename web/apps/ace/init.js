(function(){
    /*获取当前script*/
    var script = getCurrentScript();
    var rootPath = getHost(script.src,3);
    var search = script.src.substring(script.src.indexOf("?")!=-1?script.src.indexOf("?"):script.src.length);
    window.smartInitHook=function(config){
        config.versionUrl=utils.uihost+"/boot/version.js";
        utils.rootPath=rootPath;
        config.plugins.ace=[
            {js:rootPath+"/apps/ace/js/boot.js"}
        ];
    }
    var utiljs = rootPath+"/common/smart-ui/boot/utils.js"+search;
    document.write("<script src='"+utiljs+"'></script>");
    //根据路径进行按深度截取
    function getHost(src,length){
        var ss = src.split("/");
        ss.length = ss.length - length;
        var path = ss.join("/");
        return path;
    }
    function getCurrentScript() {
        var js = "init.js";
        var script = document.currentScript;
        if(!script && document.querySelector){
            script = document.querySelector("script[src*='"+js+"']");
        }
        if(!script){
            var scripts = document.getElementsByTagName("script");
            for (var i = 0, l = scripts.length; i < l; i++) {
                var src = scripts[i].src;
                if (src.indexOf(js) != -1) {
                    script = scripts[i];
                    break;
                }
            }
        }
        return script;
    }
})()
