(function(){
    Vue.app({
        data(){
            return {
                photos:[],
                firstPath:"",
                parentPath:"",
                view:{
                    show:false,
                    list:[],
                    current:0
                },
                openType:0,
                list:[]
            }
        },
        methods:{
            removeData:function (index){
                var that = this;
                var one = that.photos[index];
                parent.utils.$.confirm("确认删除'"+one.mainName+"'图片吗?",async function(flag){
                    if(!flag){
                        return;
                    }
                    that.list.splice(index,1);
                    that.photos.splice(index,1);
                    await parent.webos.softUserData.syncList("album_list",that.list);
                });
            },
            toShowLastPath:function (){
                var that = this;
                var sz = that.parentPath.split("/");
                sz.length -= 1;
                that.openFolderByPath(sz.join("/"));
            },
            setTheme:function(theme){
                theme = theme == "dark"?"dark":"";
                document.querySelector("html").className = theme;
            },
            init:async function (){
                var that = this;
                window.addEventListener("message",function (e){
                    let data = e.data;
                    if(data.action == "themeChange"){
                        that.setTheme(data.theme);
                    }
                });
                that.setTheme(localStorage.getItem("web_theme"));
                var param = new URL(location.href).searchParams;
                var expAction  = param.get("expAction");
                if(expAction == "openfolder"){
                    //文件夹打开
                    var data = {
                        name:param.get("fname"),
                        path:param.get("path")
                    }
                    that.firstPath = data.path;
                    await that.openFolderByPath(data.path);
                }else{
                    var index = -1;
                    var list = await parent.webos.softUserData.syncList("album_list");
                    var needSync = false;
                    that.openType = 1;
                    if(expAction == "open"){
                        //打开单个文件
                        var data = {
                            name:param.get("fname"),
                            path:param.get("path")
                        }
                        index = album.hasInList(list,data);
                        if(index == -1){
                            list.push(data);
                            index = list.length - 1;
                            needSync = true;
                        }
                    }else if(expAction == "playlist"){
                        //添加到默认相册
                        var files = await parent.webos.util.getBigData("addAllFiles");
                        for (let i = 0; i < files.length; i++) {
                            var file = files[i];
                            var data = {name:file.name,path:file.path};
                            var tmpIndex = album.hasInList(list,data);
                            if(tmpIndex == -1){
                                list.push(data);
                                tmpIndex = list.length - 1;
                                needSync = true;
                            }
                            if(index == -1){
                                index = tmpIndex;
                            }
                        }
                    }
                    if(index == -1){
                        index = 0;
                    }
                    if(needSync){
                        parent.webos.softUserData.syncList("album_list",list);
                    }
                    var photos = [];
                    for (let i = 0; i < list.length; i++) {
                        var one = list[i];
                        one.type = 1;
                        one.url = await parent.webos.fileSystem.zl(one.path);
                        one.thumbnail = one.url;
                        one.mainName = parent.webos.util.getMainByName(one.name);
                        photos.push(one);
                    }
                    that.photos = photos;
                    that.list = list;
                    that.toShowPhoto(index);
                }
            },
            openFolderByPath:async function (path){
                var that = this;
                that.parentPath = path;
                that.photos = [];
                await parent.webos.fileSystem.getFileListByParentPath(path,async function (list,expData){
                    for (let i = 0; i < list.length; i++) {
                        var tmp = list[i];
                        if(tmp.type != 2 && !album.isImage(tmp.ext)){
                            continue;
                        }
                        var one = {
                            type:tmp.type,
                            path:tmp.path
                        };
                        if(tmp.type == 2){
                            one.mainName = tmp.filterName;
                            one.thumbnail = "icon.png";
                        }else{
                            one.mainName = parent.webos.util.getMainByName(tmp.name);
                            one.url = await parent.webos.fileSystem.zl(tmp.path);
                            one.thumbnail = tmp.thumbnail
                        }
                        that.photos.push(one);
                    }
                });
            },
            toShowPhoto:function (index){
                var that = this;
                var item = that.photos[index];
                if(item.type == 2){
                    //打开目录
                    that.openFolderByPath(item.path);
                }else{
                    //打开文件预览
                    var photos = [];
                    var current = 0;
                    for (let i = 0; i < that.photos.length; i++) {
                        var one = that.photos[i];
                        if(one.type != 1){
                            continue;
                        }
                        if(item.url == one.url && current == 0){
                            current = photos.length;
                        }
                        photos.push(one.url);
                    }
                    that.view.list = photos;
                    that.view.current = current;
                    that.view.show = true;
                }
            }
        },
        mounted:function(){
            this.init();
        }
    });
})()