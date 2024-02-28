(function(){
    window.album = {
        hasInList(list, data) {
            for (let i = 0; i < list.length; i++) {
                if(list[i].path == data.path){
                    return i;
                }
            }
            return -1;
        },
        isImage:function (ext){
            return "jpeg,png,gif,bmp,jpg,tiff,svg,ico".split(",").includes(ext.toLowerCase());
        }
    }
})()