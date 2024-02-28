(function(){
    var path = utils.getCurrentBootScriptPath();
    var version = utils.config.version;
    document.write('<meta name="viewport" content="width=device-width,initial-scale=1,minimum-scale=1,maximum-scale=1,user-scalable=no" />');
    document.write('<link href="' + path + '/index.css?jsv='+version+'" rel="stylesheet" type="text/css" />');
    document.write('<link href="' + path + '/base.css?jsv='+version+'" rel="stylesheet" type="text/css" />');
    document.write('<link href="' + path + '/css-vars.css?jsv='+version+'" rel="stylesheet" type="text/css" />');
    document.write('<script src="' + path + '/vue.global.prod.js?jsv='+version+'" type="text/javascript"></sc' + 'ript>');
    document.write('<script src="' + path + '/index.full.min.js?jsv='+version+'" type="text/javascript"></sc' + 'ript>');
    document.write('<script src="' + path + '/locale/zh-cn.min.js?jsv='+version+'" type="text/javascript"></sc' + 'ript>');
    document.write('<script src="' + path + '/icon.min.js?jsv='+version+'" type="text/javascript"></sc' + 'ript>');
    document.write('<script src="' + path + '/message.js?jsv='+version+'" type="text/javascript"></sc' + 'ript>');
    utils.delayAction(function (){
        return window.webos && window.webos.message;
    },function (){
        webos.message.commonMsg = function(text,title){
            var map = {
                "信息":"info",
                "错误":"error",
                "警告":"warning",
                "成功":"success"
            };
            ElementPlus.ElNotification({
                title: title,
                message: text,
                type: map[title],
            });
        };
    },30*1000);
    window.VueUse = function (app){
        app.use(ElementPlus);
        app.use(ElementPlusLocaleZhCn);
        for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
            app.component(key, component);
        }
    }
})()