(function(){
    /*获取当前script*/
    var script = getCurrentScript();
    /*获取当前项目根路径*/
    var rootPath = getHost(script.src,3);
    /*相对host,使用灵活,无论怎么修改目录发布都不出问题,适合前后端混合开发,需要自行调整入参2的值*/
    var ajaxHost = getHost(script.src,3);
    /*动态绝对host,当在二级目录发布项目会出问题,适合前后端混合开发*/
    //var ajaxHost = window.location.origin;
    /*静态绝对host,每次后台更换地方发布,此处需要改配置,适合前后端分离开发*/
    //var ajaxHost = "http://localhost:8080";
    /*获取init.js后面的参数*/
    //拦截配置信息,方便自行拓展配置信息
    var search = script.src.substring(script.src.indexOf("?")!=-1?script.src.indexOf("?"):script.src.length);
    window.smartInitHook=function(config){
        config.versionUrl=utils.uihost+"/boot/version.js";
        config.plugins.win11=[
            {js:rootPath+"/common/sdk/sdk.js",css:rootPath+"/modules/win11/win11.main.css"},
            {css:rootPath+"/modules/win11/common.css"},
            {css:rootPath+"/common/font-awesome-4.7.0/css/font-awesome.min.css"}
        ];
        utils.rootPath=rootPath;
        utils.ajaxHost=ajaxHost;
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
    //获取当前init.js的上下文,如果想对init.js改名,建议此处也改一下
    //虽然新版浏览器支持从document.currentScript读取上下文,
    //但是老版浏览器还是不支持,此处做一下兼容
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
