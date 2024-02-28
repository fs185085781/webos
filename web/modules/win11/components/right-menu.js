/**/
let attr = {
    copyCut:{}
}
export default {
    template: `
    <div class="actmenu right-menu-component"
                 :data-hide="!rightMenu.show"
                 :style="{top:rightMenu.position.top+'px',left:rightMenu.position.left+'px','--prefix':'MENU',width:'310px'}">
        <template v-for="menu in rightMenu.menus">
            <div class="menuopt" @mousedown="menuAction($event,menu)" v-if="!menu.isHr && menu.show">
                <div class="spcont" v-if="menu.icon">
                    <div class="uicon" style="color:#5090F1;">
                        <i class="fa fa-lg" :class="menu.icon"></i>
                        <!--<svg :fill="menu.fill?menu.fill:'none'" style="width: 16px; height: 16px;"><use :xlink:href="menu.icon"></use></svg>
                   --> </div>
                </div>
                <div class="nopt">{{menu.name}}</div>
                <template v-if="menu.children && menu.children.length>0">
                    <div class="uicon micon rightIcon">
                        <svg style="width: 10px; height: 10px; color:#999;">
                            <use xlink:href="#arrow-r"></use>
                        </svg>
                    </div>
                    <div class="minimenu" style="min-width: 200px;">
                        <template v-for="child in menu.children">
                            <div class="menuopt" @mousedown="menuAction($event,child)" v-if="!child.isHr && child.show" >
                                <div class="spcont">
                                    <svg v-if="child.icon" style="width:20px;height:20px;">
                                        <use :xlink:href="child.icon"></use>
                                    </svg>
                                    <img v-if="child.iconUrl" :src="child.iconUrl" alt="" style="width:20px;height:20px;">
                                </div>
                                <div class="nopt">{{child.name}}</div>
                                <div class="uicon micon dotIcon" v-if="child.type == 'dotIcon' && rightMenu.checkMap[child.action]">
                                    <svg style="width:4px;height:4px;">
                                        <use xlink:href="#dot"></use>
                                    </svg>
                                </div>
                                <div class="uicon micon checkIcon" v-if="child.type == 'checkIcon' && rightMenu.checkMap[child.action]">
                                    <svg style="width:8px;height:8px;">
                                        <use xlink:href="#duihao"></use>
                                    </svg>
                                </div>
                            </div>
                            <div class="menuhr" v-if="child.isHr"></div>
                        </template>
                    </div>
                </template>
            </div>
            <div class="menuhr" v-if="menu.isHr && menu.show"></div>
        </template>
        <div style="display: none">
            <input ref="upload-file" type="file" multiple @change="toUploadFile()">
            <input ref="upload-dir" type="file" multiple webkitdirectory @change="toUploadDir()">
        </div>
    </div>
    `,
    props: [],
    data(){
        return {
            rightMenu:{
                menus:[
                    {icon:"fa-folder-open",name:"打开",action:"open",show:true},
                    {icon:"fa-edit",name:"编辑",action:"edit",show:true},
                    {icon:"fa-object-ungroup",name:"批量添加到",action:"addAll",show:true,children:[]},
                    {icon:"fa-upload",name:"上传",action:"upload",show:true,children:[
                            {name:"上传文件夹",action:"newFolderUpload",show:true,iconUrl:"modules/win11/imgs/folder-sm.png"},
                            {name:"上传文件",action:"newFileUpload",show:true,iconUrl: "imgs/file_icon/txt.png"}
                    ]},
                    {icon:"fa-download",name:"下载",action:"download",show:true},
                    {icon:"fa-share-alt",name:"共享",action:"share",show:true},
                    {icon:"fa-sign-in",name:"打开方式",action:"openWith",show:true,children:[]},
                    {isHr:true,show:true},
                    {icon:"fa-file-archive-o",name:"添加到\"压缩文件.zip\"",action:"addzip",show:true},
                    {icon:"fa-inbox",name:"解压到当前文件夹",action:"subzipc",show:true},
                    {isHr:true,show:true},
                    {icon:"fa-th-large",name:"查看",show:true,action:"view",children:[
                        {name:"大图标",action:"largeIcon",type:"dotIcon"},
                        {name:"中等图标",action:"mediumIcon",type:"dotIcon"},
                        {name:"小图标",action:"smallIcon",type:"dotIcon"},
                        {isHr:true},
                        {name:"显示桌面图标",action:"showIcon",type:"checkIcon"}
                    ]},{icon:"fa-scissors",name:"排序方式",action:"sortby",show:true,children:[
                        {name:"名称",action:"sortName",type:"dotIcon"},
                        {name:"大小",action:"sortSize",type:"dotIcon"},
                        {name:"修改日期",action:"sortDate",type:"dotIcon"}
                    ]},{icon:"fa-refresh",name:"刷新",action:"refresh"},
                    {isHr:true,show:true},
                    {icon:"fa-plus",name:"新建",show:true,action:"new",children:[]},
                    {isHr:true,show:true},
                    {icon:"fa-scissors",name:"剪切",action:"cut",show:true},
                    {icon:"fa-copy",name:"复制",action:"copy",show:false},
                    {icon:"fa-clipboard",name:"粘贴",action:"paste",show:true},
                    {isHr:true,show:true},
                    {icon:"fa-trash",name:"删除",action:"remove",show:true},
                    {icon:"fa-reply",name:"恢复",action:"restore",show:true},
                    {icon:"fa-trash",name:"彻底删除",action:"removeRecycle",show:true},
                    {icon:"fa-trash",name:"清空回收站",action:"clearRecycle",show:true},
                    {icon:"fa-i-cursor",name:"重命名",action:"rename",show:true},
                    {isHr:true,show:true},
                    {icon:"fa-magic",name:"个性化",action:"theme",show:true},
                    {icon:"fa-info",name:"属性",action:"prop",show:true},
                    {icon:"fa-expand",name:"全屏模式",action:"fullscreen",show:true},
                    {icon:"fa-compress",name:"退出全屏",action:"exitFullscreen",show:true},
                    {icon:"fa-comment-o",name:"意见反馈",action:"feedback",show:true},
                ],
                checkMap:{},
                show:false,
                position:{
                    left:0,
                    top:0
                }
            }
        }
    },
    methods: {
        toUploadCommon:function (refs,pathField){
            var that = this;
            var fileList = that.$refs[refs].files;
            var files = [];
            for (let i = 0; i < fileList.length; i++) {
                files.push(fileList[i]);
            }
            that.$refs[refs].value = "";
            var groupId = utils.uuid();
            var parentPath = attr.selectData.parentPath;
            var parentTitle = attr.selectData.parentTitle;
            var app = webos.el.findParentComponent(that,"app-component");
            var desktop = app.$refs['desktop'];
            for (let i = 0; i < files.length; i++) {
                var file = files[i];
                webos.fileSystem.addUploadFile({
                    file:file,
                    fullPath:"/"+file[pathField],
                    path:parentPath,
                    pathName:parentTitle,
                    sourceName:"本地",
                    callback:desktop.uploadFileCallback,
                    groupId:groupId
                });
            }
        },
        toUploadDir:function (){
            this.toUploadCommon("upload-dir","webkitRelativePath");
        },
        toUploadFile:function (){
            this.toUploadCommon("upload-file","name");
        },
        getSelectType:function (target){
            var ele = webos.el.isInClass(target,"webos-file-panel");
            //-1未选择,不在面板,0未选择,在面板,1单选文件,2.单选文件夹,3.同类文件多选,4,不同类多选(文件,文件夹)
            var data = {selectType:-1}
            if(!ele){
                return data;
            };
            data.panel = ele;
            data.parentPath = ele.dataset.path;
            data.parentTitle = ele.dataset.title;
            var eles = ele.querySelectorAll(".select.webos-file");
            if(eles.length == 0){
                data.selectType = 0;
                return data;
            };
            data.fileEles = eles;
            var files = [];
            for (let i = 0; i < eles.length; i++) {
                var tmp = eles[i];
                var file = {
                    path:tmp.dataset.path,
                    type:tmp.dataset.type,
                    name:tmp.dataset.name,
                    icon:tmp.dataset.icon
                };
                file.ext = file.type=="2"?"":webos.util.getExtByName(file.name);
                files.push(file);
            };
            data.files = files;
            if(eles.length == 1){
                data.selectType = data.files[0].type == "2"?2:1;
                return data;
            };
            var tl = true;
            var ext = data.files[0].ext;
            for(let i=0;i<data.files.length;i++){
                let file = data.files[i];
                if(file.type == "2"){
                    tl = false;
                    break;
                }
                if(file.ext != ext){
                    tl = false;
                    break;
                }
            };
            data.selectType = tl?3:4;
            return data;
        },
        menuPosAndCheck:function (e){
            var that = this;
            if(!attr.systemAppMap){
                var app = webos.el.findParentComponent(that,"app-component");
                attr.systemAppMap = app.$refs["desktop"].systemAppMap();
            }
            //确定选择源
            attr.selectData = that.getSelectType(e.target);
            if(attr.selectData.selectType == -1){
                return;
            }
            //-1未选择,不在面板,0未选择,在面板,1单选文件,2.单选文件夹,3.同类文件多选,4,不同类多选(文件,文件夹)
            var showMap = {
                "0":["fullscreen","exitFullscreen","view","sortby","refresh","new","paste","theme","upload","feedback"],
                "1":["open","edit","download","share","openWith","addzip","subzipc","cut","copy","remove","rename","prop"],
                "2":["open","share","openWith","addzip","cut","copy","remove","rename","prop"],
                "3":["addAll","share","addzip","cut","copy","remove"],
                "4":["share","addzip","cut","copy","remove"]
            };
            var show = false;
            var isTsPath = webos.fileSystem.isSpecialPath(attr.selectData.parentPath);
            that.rightMenu.menus.forEach(function (item){
                if(item.isHr){
                    return;
                }
                if(attr.selectData.selectType == 1 && attr.selectData.files[0].ext == "webosapp" && attr.systemAppMap[attr.selectData.files[0].path]){
                    //系统app
                    item.show = ["open","prop"].includes(item.action);
                    if(attr.selectData.files[0].path == "trash" && item.action == "clearRecycle"){
                        item.show = true;
                    }
                }else{
                    item.show = showMap[attr.selectData.selectType+""].includes(item.action);
                }
                if(item.show){
                    if(isTsPath || !webos.context.get("hasLogin") || attr.selectData.parentPath.startsWith("{sio")){
                        var sz = "prop,view,sortby,refresh,download,open,copy".split(",");
                        item.show = sz.includes(item.action);
                    }
                }
                if(item.show){
                    //处理特殊情况
                    switch(attr.selectData.selectType){
                        case 0:
                            if(item.action == "paste"){
                                item.show = attr.copyCut.has && !isTsPath;
                            }else if(["new","upload"].includes(item.action)){
                                item.show = !isTsPath;
                            }else if(["view","sortby"].includes(item.action)){
                                item.children.forEach(function (child){
                                    child.show = true;
                                });
                            }else if(["fullscreen","exitFullscreen"].includes(item.action)){
                                var fele = document.fullscreenElement || document.mozFullScreenElement||document.webkitFullscreenElement;
                                if(item.action == "fullscreen"){
                                    item.show = !fele
                                }else{
                                    item.show = !!fele
                                }
                            }
                            break;
                        case 1:
                            if(["subzipc"].includes(item.action)){
                                item.show = attr.selectData.files[0].ext == "zip";
                            }else if(["share","openWith"].includes(item.action)){
                                item.show = attr.selectData.files[0].ext != "webosapp";
                                if("openWith" == item.action && item.show){
                                    //校验合法的打开方式
                                    var has = false;
                                    item.children.forEach(function (child){
                                        child.show = child.ext.split(",").includes(attr.selectData.files[0].ext);
                                        if(child.show){
                                            has = true;
                                        }
                                    });
                                    item.show = has;
                                }
                            }
                            break;
                        case 2:
                            if(["share","addzip","cut","copy","remove","rename"].includes(item.action)){
                                item.show = !isTsPath;
                            }else if(["openWith"].includes(item.action)){
                                item.show = attr.selectData.files[0].type == 2;
                                if(item.show){
                                    //校验合法的打开方式
                                    var has = false;
                                    item.children.forEach(function (child){
                                        child.show = child.ext.split(",").includes("folder");
                                        if(child.show){
                                            has = true;
                                        }
                                    });
                                    item.show = has;
                                }
                            }
                            break;
                        case 3:
                            if(["share","addzip","cut","copy","remove"].includes(item.action)){
                                item.show = attr.selectData.files[0].ext != "webosapp";
                            }else if("addAll" == item.action){
                                var has = false;
                                item.children.forEach(function (child){
                                    child.show = child.ext.split(",").includes(attr.selectData.files[0].ext);
                                    if(child.show){
                                        has = true;
                                    }
                                });
                                item.show = has;
                            }
                            break;
                        case 4:
                            if(["share","addzip","cut","copy","remove"].includes(item.action)){
                                var has = false;
                                for(let i=0;i<attr.selectData.files.length;i++){
                                    if(attr.selectData.files[i].ext == "webosapp"){
                                        has = true;
                                        break;
                                    }
                                }
                                item.show = !has;
                            }
                            break;
                    }
                }
                if(item.show){
                    show = true;
                }
                if(attr.selectData.parentPath == "trash"){
                    if(!item.show && ["restore","removeRecycle"].includes(item.action) && attr.selectData.selectType != 0){
                        item.show = true;
                    }else if(!item.show && ["clearRecycle"].includes(item.action) && attr.selectData.selectType == 0){
                        item.show = true;
                    }else if(item.show){
                        if(["copy","open","download"].includes(item.action)){
                            item.show = false;
                        }
                    }
                }
            });
            if(!show){
                return;
            }
            //去除连续和首位线条
            let lastHr = 0;
            for (let i = 0; i < that.rightMenu.menus.length; i++) {
                let menu = that.rightMenu.menus[i];
                if(!menu.show){
                    continue;
                }
                if(menu.isHr && lastHr == -1){
                    lastHr = 0;
                    continue;
                }
                if(menu.isHr){
                    //线
                    menu.show = false;
                }else{
                    //菜单
                    lastHr = -1;
                }
            }
            //确定展开位置
            var left = e.clientX;
            if(left>document.body.clientWidth/2+100){
                left = left - 311;
            }
            if(left>document.body.clientWidth-311){
                left = document.body.clientWidth-311;
            }
            var top = e.clientY;
            that.$nextTick(function (){
                if(top>document.body.clientHeight/2){
                    var actmenu = document.querySelector(".right-menu-component");
                    top = top - actmenu.offsetHeight;
                }
                that.rightMenu.position.left = left;
                that.rightMenu.position.top = top;
                that.rightMenu.show = true;
            });
        },
        init:async function(){
            var that = this;
            //右键配置
            var rightMenuConfig = await webos.softUserData.syncObject("settings_right_menu");
            if(rightMenuConfig.largeIcon === undefined){
                rightMenuConfig = {"largeIcon":false,"mediumIcon":true,"smallIcon":false,"showIcon":true,"sortName":"asc","sortSize":false,"sortDate":false};
            }
            that.setConfig(rightMenuConfig,false);
            var hasLogin = await webos.user.hasLogin();
            if(!hasLogin){
                return;
            };
            var addall = [];
            var openwith = [];
            var newsz = [];
            var newList = [
                {name:"文件夹",action:"newFolder",show:true,iconUrl:"modules/win11/imgs/folder-sm.png"},
                {name:"文本文档",action:"newText",show:true,iconUrl: "imgs/file_icon/txt.png"}
            ];
            var list = await webos.ioFileAss.list();
            if(list){
                for (let i = 0; i < list.length; i++) {
                    var one = list[i];
                    var menu = {name:one.actionName,action:"addAllExt",iconUrl:one.iconUrl,ext:one.ext,url:one.url,show:true,appName:one.appName,expAction:one.expAction};
                    if(one.action == "addall"){
                        menu.action = "addAllExt";
                        addall.push(menu);
                    }else if(one.action == "openwith"){
                        menu.action = "openWithExt";
                        openwith.push(menu);
                    }else if(one.action == "new"){
                        menu.action = "newExt";
                        newsz.push(menu);
                    }
                }
            };
            that.rightMenu.menus.forEach(function (item){
                if(item.action == "openWith"){
                    item.children = [];
                    item.children = item.children.concat(openwith);
                }else if(item.action == "addAll"){
                    item.children = [];
                    item.children = item.children.concat(addall);
                }else if(item.action == "new"){
                    item.children = newList;
                    item.children = item.children.concat(newsz);
                }
            })
        },
        desktopTzCopy:async function (files,e){
            const that = this;
            attr.selectData = that.getSelectType(e.target);
            attr.copyCut.files = files;
            if(attr.copyCut.files.length>0){
                attr.copyCut.has = true;
                attr.copyCut.type = "copy";
                attr.copyCut.parentPath = webos.util.getParentPath(files[0].path);
            }
            await this.menuAction(e,{action:"paste"});
        },
        menuAction:async function (e,menu){
            var that = this;
            var app = webos.el.findParentComponent(that,"app-component");
            let desktop = app.$refs['desktop'];
            let fileExplorer = null;
            if(attr.selectData.panel.classList.contains("file-explorer")){
                var wins = desktop.$refs["wins_dialog"];
                var ele = webos.el.isInClass(attr.selectData.panel,"file-explorer-component");
                var winId = ele.id;
                for (let i = 0; i < wins.length; i++) {
                    var win = wins[i];
                    if(win.$props["win"].id == winId){
                        fileExplorer = win.$refs["appComponent"];
                        break;
                    }
                }
            }
            if(["addAll","openWith","new","view","sortby","upload"].includes(menu.action)){
                return;
            }else if(["cut","copy"].includes(menu.action)){
                attr.copyCut.files = attr.selectData.files;
                if(attr.copyCut.files.length>0){
                    attr.copyCut.has = true;
                    attr.copyCut.type = menu.action;
                    attr.copyCut.parentPath = attr.selectData.parentPath;
                }
            }else if(menu.action == "paste"){
                if(!attr.copyCut.has){
                    webos.message.error("请选择文件后操作");
                    return;
                }
                attr.copyCut.has = false;
                const parentPath = attr.selectData.parentPath;
                var func = "";
                let title = "";
                if(attr.copyCut.type == "cut"){
                    //移动
                    func = "move";
                    title = "移动";
                }else if(attr.copyCut.type == "copy"){
                    //复制
                    func = "copy";
                    title = "复制";
                }else{
                    webos.message.error("暂不支持此操作");
                    return;
                }
                if(parentPath == attr.copyCut.parentPath){
                    if(func == "move"){
                        webos.message.error("移动必须在不同的目录");
                        return;
                    }
                }
                let files = attr.copyCut.files;
                if(fileExplorer){
                    fileExplorer.fileExplorerLoading(true);
                }
                let sourceParent = "";

                const sourceChildren = [];
                const sourceTypes = [];
                for (let i = 0; i < files.length; i++) {
                    let tmpSz = files[i].path.split("/");
                    sourceChildren.push(tmpSz[tmpSz.length-1]);
                    sourceTypes.push(files[i].type*1);
                    if(i == 0){
                        tmpSz.length = tmpSz.length - 1;
                        sourceParent = tmpSz.join("/");
                    }
                }
                let data = await webos.fileSystem[func]({"sourceParent":sourceParent,"sourceChildren":sourceChildren,"sourceTypes":sourceTypes,"target":parentPath});
                if(fileExplorer){
                    fileExplorer.fileExplorerLoading(false);
                }
                let fileName = "";
                if(files.length == 1){
                    fileName = files[0].name;
                }else if(files.length > 1){
                    fileName = files.length+"个项目";
                }
                if(!data || data == "0"){
                    webos.message.error(fileName+title+"失败");
                    if(fileExplorer){
                        fileExplorer.fileListAction(parentPath);
                    }else{
                        desktop.refreshDesktop();
                    }
                    return;
                }
                if(data == "1"){
                    webos.message.success(fileName+title+"成功");
                    if(fileExplorer){
                        fileExplorer.fileListAction(parentPath);
                    }else{
                        desktop.refreshDesktop();
                    }
                    return;
                }
                const  taskId = data;
                let errorTimes = 0;
                const task = {
                    id:taskId,
                    groupId:taskId,
                    canInterrupt:false,
                    cancelType:1,
                    taskType:func
                }
                let hasUpload = false;
                let confirmIng = false;
                const serverAction = setInterval(async function () {
                    if(confirmIng){
                        return;
                    }
                    var taskData = await webos.fileSystem.serverJd(taskId);
                    if(!taskData){
                        errorTimes++;
                        if(errorTimes>20){
                            clearInterval(serverAction);
                        }
                        return;
                    }
                    //sourceName,sd,jd,loaded,size,status,exp,targetName,currentFileName
                    for(var key in taskData){
                        if(key == "targetName"){
                            continue;
                        }
                        if(key == "currentFileName"){
                            continue;
                        }
                        if(key == "exp"){
                            continue;
                        }
                        task[key] = taskData[key];
                    }
                    task.status = hasUpload?1:0;
                    task.pathName = taskData.targetName;
                    task.name = taskData.currentFileName;
                    task.methodName = title;
                    await desktop.uploadFileCallback(task);
                    hasUpload = true;
                    if(taskData.status == 2 || taskData.status == 3){
                        task.status = 2;
                        await desktop.uploadFileCallback(task);
                        if(taskData.status == 3){
                            webos.message.error(task.sourceName+title+"部分失败,请手动检查");
                        }
                        clearInterval(serverAction);
                        //刷新目录
                        if(fileExplorer){
                            fileExplorer.fileListAction(parentPath);
                        }else{
                            desktop.refreshDesktop();
                        }
                    }else if(taskData.status == 4){
                        //前端实现
                        let expData = JSON.parse(taskData.exp);
                        if(expData.type == "2"){
                            //前端复制
                            confirmIng = true;
                            if(fileExplorer){
                                fileExplorer.fileListAction(parentPath);
                            }else{
                                desktop.refreshDesktop();
                            }
                            utils.$.confirm("<div>检测到"+expData.files.length+"个文件无法秒传,建议选用浏览器传输,是否继续?<br><span style='color:green;'>确定:则选用浏览器传输(推荐)</span><br><span style='color:red;'>取消:则继续采用服务器传输(不推荐)</span></div>",async function (flag){
                                if(flag){
                                    //前端传输,先中断数据
                                    task.status = 5;
                                    await desktop.uploadFileCallback(task);
                                    clearInterval(serverAction);
                                    webos.fileSystem.webCrossCopy(expData.files,task.sourceName,taskData.targetName,desktop.uploadFileCallback);
                                }else{
                                    //服务器传输,进行确认
                                    var flag2 = await webos.fileSystem.serverConfirm(taskId);
                                    if(!flag2){
                                        webos.message.error(webos.context.get("lastErrorReqMsg"));
                                        task.status = 5;
                                        await desktop.uploadFileCallback(task);
                                        clearInterval(serverAction);
                                    }
                                }
                                confirmIng = false;
                            });
                        }else{
                            task.status = 5;
                            await desktop.uploadFileCallback(task);
                            clearInterval(serverAction);
                        }
                    }
                },1000);
                if(fileExplorer){
                    fileExplorer.fileListAction(parentPath);
                }else{
                    desktop.refreshDesktop();
                }
            }else if(menu.action == "refresh"){
                //刷新
                if(menu.allFilePanel){
                    desktop.refreshDesktop();
                    var wins = desktop.$refs["wins_dialog"];
                    if(wins){
                        for (let i = 0; i < wins.length; i++) {
                            var win = wins[i];
                            var tmpApp = win.$refs["appComponent"];
                            if(!tmpApp){
                                continue;
                            }
                            if(!tmpApp.fileListAction){
                                continue;
                            }
                            tmpApp.fileListAction(tmpApp.$data.dataPath);
                        }
                    }
                }else{
                    if(fileExplorer){
                        fileExplorer.fileListAction(attr.selectData.parentPath);
                    }else{
                        desktop.refreshDesktop();
                    }
                }

            }else if(["fullscreen","exitFullscreen"].includes(menu.action)){
                webos.util.fullScreen(menu.action == "fullscreen");
            }else if(["feedback"].includes(menu.action)){
                var a = document.createElement("a");
                a.href = "https://support.qq.com/product/464670";
                a.target = "_blank";
                a.click();
            }else if(menu.action == "addAllExt"){
                //批量添加到
                var url = menu.url;
                var flag = await webos.util.setBigData("addAllFiles",attr.selectData.files);
                if(!flag){
                    webos.message.error("批量添加失败");
                    return;
                }
                desktop.openFile(url+"?action=addall&expAction="+menu.expAction,4,menu.appName,menu.iconUrl);
            }else if(menu.action == "openWithExt"){
                //更换打开方式
                var file = attr.selectData.files[0];
                var fileUrl = await webos.fileSystem.zl(file.path);
                var url = menu.url+"?action=openwith&expAction="+menu.expAction+"&url="+encodeURIComponent(fileUrl)+"&ext="+encodeURIComponent(file.ext)+"&fname="+encodeURIComponent(file.name)+"&path="+encodeURIComponent(file.path)+"&icon="+encodeURIComponent(file.icon);
                desktop.openFile(url,4,menu.appName,menu.iconUrl);
            }else if(menu.action == "open" || menu.action == "edit"){
                var file = attr.selectData.files[0];
                if(fileExplorer && attr.selectData.selectType == 2){
                    //窗口中打开文件夹
                    fileExplorer.fileListAction(file.path);
                }else{
                    //桌面打开文件或者文件夹
                    desktop.openFile(file.path,attr.selectData.selectType,file.name,"",menu.action);
                }
            }else if(menu.action == "newFolder"){
                var parentPath = attr.selectData.parentPath;
                var files;
                if(fileExplorer){
                    files = fileExplorer.contentFiles;
                }else{
                    files = desktop.files;
                }
                var getNewMainName = function (name,index,files){
                    var has = false;
                    var tmpName = name + "("+index+")";
                    if(index === 0){
                        tmpName = name;
                    }
                    for(let i=0; i<files.length; i++){
                        if(files[i].name == tmpName){
                            has = true;
                            break;
                        }
                    }
                    if(!has){
                        return tmpName;
                    }else{
                        return getNewMainName(name,index+1,files);
                    }
                }
                var name = getNewMainName("新建文件夹",0,files);
                var fileId = await webos.fileSystem.createDir({path:parentPath,name:name});
                if(!fileId){
                    webos.message.error("文件夹新建失败");
                    return;
                }
                var fileData = {
                    name:name,
                    path:parentPath+"/"+fileId,
                    type:2
                }
                var data = {
                    parentPath:parentPath,
                    name:name
                };
                if(fileExplorer){
                    await fileExplorer.toRename(data, fileData);
                }else{
                    await desktop.toRename(data, fileData);
                }
            }else if(["largeIcon","mediumIcon","smallIcon","showIcon","sortName","sortSize","sortDate"].includes(menu.action)){
                var config = JSON.parse(JSON.stringify(that.rightMenu.checkMap));
                var iconSz = ["largeIcon","mediumIcon","smallIcon"];
                var sortSz = ["sortName","sortSize","sortDate"];
                if(iconSz.includes(menu.action)){
                    for (let i = 0; i < iconSz.length; i++) {
                        var iconType = iconSz[i];
                        config[iconType] = menu.action == iconType;
                    }
                }else if(sortSz.includes(menu.action)){
                    for (let i = 0; i < sortSz.length; i++) {
                        var sortType = sortSz[i];
                        if(sortType == menu.action){
                            if(config[sortType] == "desc"){
                                config[sortType] = "asc";
                            }else{
                                config[sortType] = "desc";
                            }
                        }else{
                            config[sortType] = false;
                        }
                    }
                }else if("showIcon" == menu.action){
                    config[menu.action] = !config[menu.action];
                }
                await that.setConfig(config,true);
                that.menuAction(e,{action:"refresh",allFilePanel:true});
            }else if(menu.action == "newText" || menu.action == "newExt"){
                //新建文件
                var createFileAndReanme = async function(mainName,ext,path,blob){
                    var mainName = await webos.fileSystem.availableMainName({path:path,ext:ext,mainName:mainName});
                    var fileName = mainName+"."+ext;
                    var param = {
                        file:blob,
                        name:fileName,
                        parentPath:path
                    }
                    var fileId = await parent.webos.fileSystem.uploadSmallFile(param);
                    var fileData = {
                        ext:ext,
                        name:fileName,
                        path:path+"/"+fileId,
                        size:0,
                        type:1
                    }
                    var data = {
                        parentPath:path,
                        name:fileName
                    };
                    if(fileExplorer){
                        await fileExplorer.toRename(data, fileData);
                    }else{
                        await desktop.toRename(data, fileData);
                    }
                };
                if(menu.action == "newText"){
                    var path = attr.selectData.parentPath;
                    await createFileAndReanme("新建文本文档","txt",path,new Blob());
                }else{
                    var ext = menu.ext;
                    var uuid = "f"+utils.uuid();
                    var url = menu.url+"?action=new&expAction="+menu.expAction+"&ext="+encodeURIComponent(ext)+"&func="+encodeURIComponent(uuid);
                    var actionWin = await desktop.openFile(url,4,menu.appName,menu.iconUrl);
                    if(!actionWin){
                        return;
                    }
                    actionWin.width = 0;
                    var closeWin = function (){
                        var wins = desktop.$refs["wins_dialog"];
                        for (let i = 0; i < wins.length; i++) {
                            var win = wins[i];
                            if(win.$props["win"].id == actionWin.id){
                                win.windowAction(4);
                                break;
                            }
                        }
                    }
                    window[uuid] = async function(blob){
                        closeWin();
                        var path = attr.selectData.parentPath;
                        await createFileAndReanme("新建"+menu.name,ext,path,blob);
                        delete window[uuid];
                    };
                    setTimeout(function (){
                        try{
                            delete window[uuid];
                            closeWin();
                        }catch (e){
                        }
                    },60*1000);
                }
            }else if(menu.action == "rename"){
                //重命名
                var file = attr.selectData.files[0];
                var data = {parentPath:webos.util.getParentPath(file.path),name:file.name};
                if(fileExplorer){
                    await fileExplorer.toRename(data);
                }else{
                    await desktop.toRename(data);
                }
            }else if(menu.action == "remove"){
                let files = attr.selectData.files;
                if(fileExplorer){
                    fileExplorer.fileExplorerLoading(true);
                }
                let sourceParent = "";
                const sourceChildren = [];
                const sourceTypes = [];
                for (let i = 0; i < files.length; i++) {
                    let tmpSz = files[i].path.split("/");
                    sourceChildren.push(tmpSz[tmpSz.length-1]);
                    sourceTypes.push(files[i].type*1);
                    if(i == 0){
                        tmpSz.length = tmpSz.length - 1;
                        sourceParent = tmpSz.join("/");
                    }
                }
                let res = await webos.fileSystem.remove({"sourceParent":sourceParent,"sourceChildren":sourceChildren,"sourceTypes":sourceTypes});
                if(res){
                    webos.message.success("删除成功");
                }else{
                    webos.message.error("删除失败");
                }
                await that.menuAction(e,{action:"refresh"});
            }else if(menu.action == "restore"){
                var paths = [];
                for (let i = 0; i < attr.selectData.files.length; i++) {
                    paths.push(attr.selectData.files[i].path);
                }
                webos.context.set("showOkErrMsg", true);
                var flag = await webos.userRecycle.restoreByPaths(paths);
                if(flag){
                    await that.menuAction(e,{action:"refresh"});
                }
            }else if(menu.action == "removeRecycle"){
                var files = attr.selectData.files;
                utils.$.confirm("确定彻底删除"+files.length+"项文件(夹)吗?操作后将不可恢复!",async function (flag) {
                    if(!flag){
                        return;
                    }
                    var paths = [];
                    for (let i = 0; i < files.length; i++) {
                        paths.push(files[i].path);
                    }
                    webos.context.set("showOkErrMsg", true);
                    flag = await webos.userRecycle.clearByPaths(paths);
                    if(flag){
                        that.menuAction(e,{action:"refresh"});
                    }
                });
            }else if(menu.action == "clearRecycle"){
                utils.$.confirm("确定清空回收站吗?操作后将不可恢复!",async function (flag) {
                    if(!flag){
                        return;
                    }
                    webos.context.set("showOkErrMsg", true);
                    flag = await webos.userRecycle.clear();
                    if(flag){
                        that.menuAction(e,{action:"refresh"});
                    }
                });
            }else if(menu.action == "subzipc"){
                //解压到当前目录
                webos.message.success("已提交解压请求,请稍后...");
                webos.context.set("showOkErrMsg", true);
                var flag = await webos.fileSystem.unzip({"path":attr.selectData.files[0].path});
                if(flag){
                    that.menuAction(e,{action:"refresh"});
                }
            }else if(menu.action == "download"){
                var zl = await webos.fileSystem.zl(attr.selectData.files[0].path);
                var a = document.createElement("a");
                a.href = zl;
                a.target = "_blank";
                a.download = attr.selectData.files[0].name;
                a.click();
            }else if(menu.action == "addzip"){
                //添加到压缩文件
                var paths = [];
                for (let i = 0; i < attr.selectData.files.length; i++) {
                    paths[i] = attr.selectData.files[i].path;
                }
                var parentPath = attr.selectData.parentPath;
                webos.context.set("showErrMsg", true);
                var flag = await webos.fileSystem.zip({paths,parentPath});
                if(flag){
                    if(flag == "1"){
                        webos.message.success(webos.context.get("lastSuccessReqMsg"));
                        that.menuAction(e,{action:"refresh"});
                    }else if(flag == "2"){
                        webos.message.error(webos.context.get("lastErrorReqMsg"));
                    }
                }

            }else if(menu.action == "newFolderUpload"){
                //上传文件夹
                that.$refs["upload-dir"].click();
            }else if(menu.action == "newFileUpload"){
                //上传文件
                that.$refs["upload-file"].click();
            }else if(menu.action == "share"){
                //分享文件
                desktop.toShareData(attr.selectData.files);
            }else if(menu.action == "theme"){
                var actionWin = await desktop.openFile("settings",3,"设置","");
                utils.delayAction(function(){
                    return desktop.getWinComById(actionWin.id);
                },function (){
                    var winCom = desktop.getWinComById(actionWin.id);
                    utils.delayAction(function(){
                        return winCom.$refs&&winCom.$refs.appComponent;
                    },function (){
                        winCom.$refs.appComponent.toSelectAction("personal");
                    },6000);
                },6000);

            }else if(menu.action == "prop"){
                var file = attr.selectData.files[0];
                var actionWin = await desktop.openFile("properties",3,file.name+"属性",file.icon);
                actionWin.width = 400;
                actionWin.height = 650;
                actionWin.hideMax = true;
                actionWin.hideMin = true;
                actionWin.hideSize = true;
                actionWin.data.filePath = file.path;
                actionWin.data.fileIcon = file.icon;
                utils.delayAction(function(){
                    return desktop.getWinComById(actionWin.id);
                },function (){
                    var winCom = desktop.getWinComById(actionWin.id);
                    utils.delayAction(function(){
                        return winCom.$refs&&winCom.$refs.appComponent;
                    },function (){
                        winCom.$refs.appComponent.initData();
                    },6000);
                },6000);
            }else{
                console.log(menu);
                webos.message.error("此操作暂未实现");
            }
            that.rightMenu.show = false;
        },
        setConfig:async function (config,needSave){
            var that = this;
            that.rightMenu.checkMap = config;
            var app = webos.el.findParentComponent(that,"app-component");
            var desktop = app.$refs['desktop'];
            desktop.showIcon = that.rightMenu.checkMap.showIcon;
            webos.context.set("rightMenuConfig",config);
            if(needSave){
                webos.softUserData.syncObject("settings_right_menu",config);
            }
        }
    },
    created: function () {
        //this.init();
    }
}