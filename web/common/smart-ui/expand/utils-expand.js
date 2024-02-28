(function () {
    /**
     * 1.消息通知,包含successMsg,infoMsg,warningMsg,errorMsg
     * 2.加载层,包含loading,cancelLoading
     * 3.弹窗通知,包含alert,confirm,prompt
     * 4.http提交,包含jsonp,post,get,del,put,req,nativeReq
     * 5.文件上传组件,initFileUpload
     */
    /**封装文档加载完毕才开始执行*/
    function asyncFunction(callback){
        utils.delayAction(function(){
            return document.readyState=="complete";
        },callback);
    }
    asyncFunction(function () {
        utils.$.attr.init = true;
    });
    /*try{
        if(window != window.top && window.top.utils && window.top.utils.message && window.top.utils.from == utils.from){
            utils.message = window.top.utils.message;
        }
    }catch (e){
    }*/
    /**封装消息通知,弹窗通知,加载框,jsonp提交,http提交,文件上传组件*/
    utils.$ = {
        attr:{
            init:false,
            mask:true,//全局ajax遮罩层
            tmpMask:true//临时ajax遮罩层
        },
        successMsg:function(text){
            this.msg(text,"success");
        },
        infoMsg:function(text){
            this.msg(text,"info");
        },
        warningMsg:function(text){
            this.msg(text,"warning");
        },
        errorMsg:function(text){
            this.msg(text,"error");
        },
        msg:function(text,type){
            asyncFunction(function () {
                utils.message.msg(text,type);
            });
        },
        loading:function(text){
            if(!this.attr.init){
                return;
            }
            utils.message.loading(text);
        },
        cancelLoading:function(){
            var that = this;
            that.attr.tmpMask = true;
            utils.message.cancelLoading();
        },
        alert:function(text,callback){
            asyncFunction(function () {
                utils.message.alert(text,callback);
            });
        },
        confirm:function(text,callback){
            asyncFunction(function () {
                utils.message.confirm(text,callback);
            });
        },
        prompt:function(text,callback,def){
            asyncFunction(function () {
                utils.message.prompt(text,callback,def);
            });
        }
    }
    /**封装简化Vue写法*/
    if(window.Vue){
        Vue.app = function(options){
            if(window.vm){
                throw 'vm全局变量已经存在';
            }
            if(!options.el){
                options.el = "#app";
            }
            if(Vue.createApp){
                //vue3
                var app = Vue.createApp(options);
                if(window.VueUse){
                    window.VueUse(app);
                }
                window.app = app;
                if(window.InitVueComponent){
                    window.InitVueComponent(app);
                }
                return window.vm = app.mount(options.el);
            }else{
                //vue1-2
                window.app = new Vue(options);
                return window.vm = app;
            }
        }
    }
    /**Vue依赖原生JSON,在模拟IE5下跳过模拟JSON,还原JSON*/
    if(window.Vue && window.tmpJSON){
        window.JSON = window.tmpJSON;
        utils.removeProp(window,"tmpJSON");
    }
})();
