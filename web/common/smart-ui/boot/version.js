(function(){
    /**
     * 此代码解决版本控制缓存问题
     */
    var version = localStorage.getItem("last_version");
    if(!version){
        version = "0";
    }
    var c = {
        version:version
    };
    if(window.initVersion){
        window.initVersion(c);
    }
})()