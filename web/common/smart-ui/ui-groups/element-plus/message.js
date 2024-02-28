(function(){
    utils.message = {
        msg:function(text,type){
            var that = this;
            if(!type){
                type = "default";
            }
            ElementPlus.ElMessage({
                message:text,
                type:type
            })
        },
        loading:function(text){
            var that = this;
            that.loadingService = ElementPlus.ElLoading.service({text:text})
        },
        cancelLoading:function(){
            var that = this;
            if(that.loadingService){
                that.loadingService.close();
            }
        },
        alert:function(text,callback){
            ElementPlus.ElMessageBox.alert(text,"提示",{callback:function (){
                if(callback){
                    callback();
                }
            },"showClose":false,
                "confirmButtonText":"确定",
                "cancelButtonText":"取消",
                "dangerouslyUseHTMLString":true,
                "closeOnClickModal":false,
                "closeOnPressEscape":false,
                "closeOnHashChange":false,
            });
        },
        confirm:function(text,callback){
            ElementPlus.ElMessageBox.confirm(text,"提示",{
                callback:function (e){
                    if(callback){
                        callback(e == "confirm"?1:0)
                    }
                },"showClose":false,
                "confirmButtonText":"确定",
                "cancelButtonText":"取消",
                "dangerouslyUseHTMLString":true,
                "closeOnClickModal":false,
                "closeOnPressEscape":false,
                "closeOnHashChange":false,
            });
        },
        prompt:function(text,callback,def){
            ElementPlus.ElMessageBox.prompt(text,"提示",{
                callback:function (e){
                    if(callback){
                        callback(e.action == "confirm"?1:0,e.value);
                    }
                },"showClose":false,
                "confirmButtonText":"确定",
                "cancelButtonText":"取消",
                "dangerouslyUseHTMLString":true,
                "closeOnClickModal":false,
                "closeOnPressEscape":false,
                "closeOnHashChange":false,
                "inputValue":def
            });
        }
    }
})();