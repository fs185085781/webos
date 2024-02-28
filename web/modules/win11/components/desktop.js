/*桌面和图标组件*/
let attr = {};
export default {
    template: `
        <div class="desktop-component">
            <!--批量选中蒙版 -->
            <div class="more-select-wrap"></div>
            <!--文件夹之间复制提示-->
            <div class="file-drag-wrap">
                <img :src="dragFile.icon" alt="">
                <div class="text">
                    <div>{{dragFile.length}}</div>
                </div>
            </div>
            <!--拖拽提示-->
            <did class="drag-tips" v-if="dragUpload.path && dragUpload.show">
                <svg>
                    <use xlink:href="#upload"></use>
                </svg>
                <span>拖放将复制到"{{dragUpload.title}}"</span>
            </did>
            <!--桌面壁纸-->
            <img v-if="wallpaper.type == 'img'" class="background" :src="wallpaper.url" style="width:100vw;height:100vh;object-fit:cover;">
            <video v-if="wallpaper.type == 'video'" :src="wallpaper.url" autoplay muted loop style="width:100vw;height:100vh;object-fit:cover;" ></video>
            <!--桌面-->
            <div @mousedown="moreSelectStart($event,'desktop')" class="desktop upload-win webos-file-panel" :data-path="currentPath" data-title="桌面">
                <!--桌面图标-->
                <div class="desktopCont" v-if="showIcon">
                    <div v-for="file in systemApps" :title="file.filterName">
                        <div class="dskApp webos-file webos-in-desktop"
                        :data-path="file.path"
                        :data-type="file.type"
                        :data-name="file.name"
                        :data-icon="file.thumbnail"
                        :data-size="file.size"
                        :data-ext="file.ext"
                        :class="{'select':fileSelectMap[file.path]}"
                        @mousedown="fileDblClick(file,$event)">
                            <div class="uicon dskIcon" data-pr="true">
                                <img :width="iconWidth" :height="iconWidth" :src="file.thumbnail" alt="" style="object-fit: cover;">
                            </div>
                            <div class="appName">{{file.filterName}}</div>
                        </div>
                    </div>
                    <div v-for="file in files" :title="file.filterName">
                        <div class="dskApp webos-file webos-in-desktop" :class="{'rename':rename.name == file.name}"
                        :data-path="file.path"
                        :data-type="file.type"
                        :data-name="file.name"
                        :data-icon="file.thumbnail"
                        :data-size="file.size"
                        :data-ext="file.ext"
                        :class="{'select':fileSelectMap[file.path]}" @mousedown="fileDblClick(file,$event)">
                            <div class="uicon dskIcon" data-pr="true">
                                <img :width="iconWidth" :height="iconWidth" :src="file.thumbnail" alt="" style="object-fit: cover;">
                            </div>
                            <div class="appName rename" v-if="rename.name == file.name">
                              <el-input
                                ref="rename-ta"
                                v-model="rename.newName"
                                autosize
                                type="textarea"
                                @blur="actionRename(file)"
                                autofocus
                              ></el-input>
                            </div>
                            <div class="appName" v-else="rename.name != file.name">{{file.filterName}}</div>
                        </div>
                    </div>
                </div>
            </div>
            <!--窗口-->
            <div class="drag-dialog-win">
                <!--多功能窗口-->
                <window v-for="win in wins" :win="win" ref="wins_dialog"></window>
            </div>
            <!--任务栏-->
            <taskbar :wins="wins" ref="taskbar"></taskbar>
            <!--分享-->
            <div class="modal-window" v-if="shareEdit.show">
                <el-dialog
                    draggable
                    v-model="shareEdit.show"
                    :title="shareEdit.title"
                    width="600px"
                    :close-on-click-modal="false" :close-on-press-escape="false"
                    ref="share-edit"
                  >
                   <el-form :inline="true" :model="shareEdit.data" label-width="120px">
                    <el-form-item label="分享链接">
                      <el-input v-model="shareEdit.data.code" style="display:none;"></el-input>
                      <el-input :value="shareEdit.preUrl+shareEdit.data.code" style="width:392px;"></el-input>
                    </el-form-item>
                    <el-form-item label="提取密码">
                      <el-input v-model="shareEdit.data.password" style="width:120px;"></el-input>
                    </el-form-item>
                    <el-form-item label="过期时间">
                      <el-date-picker
                        style="width:120px;"
                        v-model="shareEdit.data.expireTime"
                        type="date"
                        placeholder="请选择过期时间"
                        value-format="YYYY-MM-DD"
                      ></el-date-picker>
                    </el-form-item>
                    <el-form-item label="分享标题">
                      <el-input v-model="shareEdit.data.name" style="width:392px;"></el-input>
                    </el-form-item>
                    <el-form-item style="width:600px;">
                        <el-col :span="24" style="text-align: center;">
                          <el-button v-if="shareEdit.data.id" @click="toRemoveShareData(shareEdit.data)">取消分享</el-button>
                          <el-button type="primary" @click="saveShareEdit()">分享</el-button>
                          <el-button @click="shareEdit.show = false">关闭</el-button>
                        </el-col>
                    </el-form-item>
                   </el-form>
                </el-dialog>
            </div>
        </div>
    `,
    props: [],
    data(){
        return {
            systemApps:[],
            shareEdit:{
                show:false,
                data:{},
                title:"分享文件",
                preUrl:""
            },
            currentPath:"",
            showIcon:false,
            files:[],
            fileSelectMap:{},
            iconWidth:43,
            wins:[],
            wallpaper:{
               url:"",
               type:""
            },
            dragUpload:{
                path:"",
                title:"",
                show:false,
                uploadTasks:[]
            },
            dragFile:{
                icon:"",
                length:0
            },
            rename:{
                name:"",
                newName:""
            }
        }
    },
    methods: {
        init:async function (){
            var that = this;
            var hasLogin =await webos.user.hasLogin();
            if(hasLogin){
                that.refreshDesktop();
                that.dragUploadFileInit();
            }
        },
        toRemoveShareData:function (item){
            const that = this;
            utils.$.confirm("确认取消'"+item.name+"'共享吗?取消后该分享数据将无法访问!",async function (flag){
                if(!flag){
                    return;
                };
                webos.context.set("showOkErrMsg", true);
                flag = await webos.shareFile.dels([item.id]);
                if(flag){
                    await that.refreshDesktop();
                }
            });
        },
        refreshDesktop:async function (){
            var that = this;
            var count = await webos.userRecycle.count();
            that.systemApps.forEach(function (item){
                if(item.path == "trash"){
                    item.thumbnail = "modules/win11/imgs/icon/trash"+(count==0?"e":"")+".png";
                }
            });
            that.files = [];
            var config = webos.context.get("rightMenuConfig");
            if(!config){
                var config = await webos.softUserData.syncObject("settings_right_menu");
                if(config.largeIcon === undefined){
                    config = {"largeIcon":false,"mediumIcon":true,"smallIcon":false,"showIcon":true,"sortName":"asc","sortSize":false,"sortDate":false};
                }
            }
            if(config){
                if(config.largeIcon){
                    that.iconWidth = 54;
                }else if(config.mediumIcon){
                    that.iconWidth = 43;
                }else if(config.smallIcon){
                    that.iconWidth = 36;
                }
                that.showIcon = config.showIcon;
            }
            var data = await webos.userDrive.specialFiles("desktop");
            if(data){
                that.files = data.list;
                that.currentPath = data.parentPath;
                for (let i = 0; i < that.files.length; i++) {
                    let item = that.files[i];
                    await webos.fileSystem.fileIconCalc(item);
                    if(item.type == 2){
                        if(!item.thumbnail){
                            item.thumbnail = "modules/win11/imgs/folder-sm.png";
                        }
                    }else{
                        if(!item.thumbnail){
                            item.thumbnail = "imgs/file_icon/file.png";
                        }
                    }
                }
                if(config){
                    that.files.sort(function (a,b){
                        var field = "filterName";
                        var order = "asc";
                        if(config.sortName){
                            field = "filterName";
                            order = config.sortName;
                        }else if(config.sortDate){
                            field = "updatedAt";
                            order = config.sortDate;
                        }else if(config.sortSize){
                            field = "size";
                            order = config.sortSize;
                        }
                        var av = a[field]?a[field]:"";
                        var bv = b[field]?b[field]:"";
                        av+="";
                        bv+="";
                        if(order == "asc"){
                            return av.localeCompare(bv);
                        }else{
                            return bv.localeCompare(av);
                        }
                    });
                }
            };
        },
        selectFileAction:async function(accept,multi,fn){
            //accept 如果是folder说明是目录,其他情况是文件的后缀,多个用逗号分割
            //multi 是否多选 true多选 false单选
            //fn回调事件,返回[内容]数组
            const that = this;
            var actionWin = await that.openFile("file-select",3,"请选择文件"+(accept=="folder"?"夹":""),"modules/win11/imgs/icon/explorer.png");
            actionWin.data.multi = multi;
            actionWin.data.fn = fn;
            actionWin.data.selectExt = accept;
            var sz=accept.split(",");
            for (let i = 0; i < sz.length; i++) {
                if(!sz[i]){
                    sz[i] = "*";
                }
                sz[i] = "*."+sz[i];
            }
            actionWin.data.selectExtName = accept=="folder"?"文件夹":"文件("+sz.join(";")+")";
            actionWin.width = 700;
            actionWin.height = 500;
        },
        openFile:async function (path,type,name,icon,expAction){
            //path type为1,2时为文件路径,type为3时为app名字,type为4时候代表页面路径,可以是相对路径,也可以是http开头的url
            //type 1文件 2文件夹 3系统app 4通用app
            //name 名称
            //icon 存在则使用此icon,不存在将使用modules/模板/imgs/icon/应用名称.png
            let isDbl = false;
            if(!expAction){
                expAction = "open";
            }else if(expAction == "edit,open"){
                //双击操作
                isDbl = true;
            }
            var expActions = expAction.split(",");
            var that = this;
            var win = null;
            //winType 1正常 2最大化 3最小化 12  21 13  31  23  32
            for (var i = 0; i < that.wins.length; i++) {
                var tmp = that.wins[i];
                if(tmp.close){
                    win = tmp;
                    break;
                }
            };
            if(!win){
                win = Vue.reactive({
                    id:"id-"+utils.uuid()
                });
                that.wins.push(win);
            };
            var height = window.innerHeight*0.8;
            if(height>700){
                height = 700;
            };
            var width = window.innerWidth*0.8;
            if(width>1024){
                width = 1024;
            };
            var tmpWin = {
                show:false,
                width:width,
                height:height,
                left:window.innerWidth/2-width/2,
                top:window.innerHeight/2-height/2,
                winLastType:3,
                winNowType:1,
                close:true,
                isSimple:false,
                hideMax:false,
                hideMin:false,
                hideSize:false,
                hideClose:false,
                disableMax:false,
                disableClose:false,
                disableMin:false,
            };
            for(var key in tmpWin){
                win[key] = tmpWin[key];
            };
            win.data = {};
            if(type == 2){
                //桌面打开文件夹,调用文件管理器
                if(!icon){
                    icon = "modules/win11/imgs/folder-sm.png";
                }
                win.data = {
                    app: "fileExplorer",
                    icon: icon,
                    name: name,
                    path: path
                };
            }else{
                if(type == 3){
                    //系统app
                    win.data = {
                        app: path,
                        icon: icon?icon:"modules/win11/imgs/icon/"+path+".png",
                        name: name
                    }
                }else if(type == 4){
                    //通用app 用于插件和轻应用
                    win.data = {
                        app: "commonApp",
                        icon: icon?icon:"modules/win11/imgs/icon/commonApp.png",
                        name: name,
                        url: path
                    };
                }else{
                    //打开文件
                    if(name.endsWith(".webosapp")){
                        //这是打开app
                        var systemAppMap = that.systemAppMap();
                        if(systemAppMap[path]){
                            win.data = that.realSystemData(path);
                        }else{
                            var fileCache = await webos.fileSystem.getFileCache(path);
                            var text = String.fromCharCode.apply(null, new Uint8Array(fileCache));
                            text = decodeURIComponent(escape(text));
                            win.data = JSON.parse(text);
                        }
                        var icon = win.data.app;
                        if(win.data.targetApp){
                            win.data.app = win.data.targetApp;
                        }
                        if(!win.data.icon){
                            win.data.icon = "modules/win11/imgs/icon/"+icon+".png";
                        }
                    }else{
                        var ext = webos.util.getExtByName(name);
                        if(isDbl && webos.util.isMedia(ext)){
                            //如果是双击的又是媒体资源,优先使用打开模式
                            expActions = ["open","edit"];
                        }
                        var privateApp = await webos.util.userOpenApp(ext,expActions);
                        if(!privateApp){
                            webos.message.error("此文件不支持打开,可前往商城寻找支持的程序");
                            return;
                        }
                        var fileUrl = await webos.fileSystem.zl(path,2);
                        var url = privateApp.url+"?action=openwith&url="+encodeURIComponent(fileUrl)+"&ext="+encodeURIComponent(ext)+"&fname="+encodeURIComponent(name)+"&path="+encodeURIComponent(path)+"&expAction="+privateApp.expAction;
                        win.data = {
                            app: "commonApp",
                            icon: privateApp.iconUrl,
                            name: privateApp.appName,
                            url: url
                        }
                    }
                }
            }
            win.close = false;
            win.show = true;
            utils.delayAction(function (){
                return document.querySelector("#"+win.id+"header");
            },function (){
                var header = document.querySelector("#"+win.id+"header");
                that.topWindow({target:header});
                var target = header.parentElement.parentElement;
                var aid = webos.el.animationCss({left:document.body.clientWidth/2,top:document.body.clientHeight,height:0,width:0},win);
                target.style.left = win.left+"px";
                target.style.top = win.top+"px";
                target.style.height = win.height+"px";
                target.style.width = win.width+"px";
                target.style.animation = aid+" 0.3s";
                target.style["animation-fill-mode"] = "none";
            });
            return win;
        },
        winMoveAction:function (e){
            if(this.$refs.wins_dialog){
                this.$refs.wins_dialog[0].moveAction(e);
            }

        },
        winMoveStop:function (e){
            if(this.$refs.wins_dialog){
                this.$refs.wins_dialog[0].windowSizeChangeEnd(e);
                this.$refs.wins_dialog[0].windowMoveEnd(e);
            }
        },
        topWindow:function (e){
            if(document.activeElement && document.activeElement.className.indexOf("el-dialog") != -1){
                document.activeElement.blur();
            }
            var that = this;
            var target = e.target;
            if(!target){
                return;
            }
            for (let i = 0; i < 50; i++) {
                if(!target){
                    break;
                }
                if(target.classList.contains("el-dialog")){
                    break;
                }
                target = target.parentElement;
            }
            if(!target){
                return;
            }
            var isFirst = false;
            if(!attr.dragDialog){
                attr.dragDialog = {
                    last:target,
                    index:2001
                }
                isFirst = true;
            }
            if(isFirst || attr.dragDialog.last != target){
                attr.dragDialog.index ++;
                attr.dragDialog.last = target;
                attr.dragDialog.last.style["z-index"] = attr.dragDialog.index;
                var header = target.querySelector(".win-header");
                if(!header){
                    return;
                };
                if(!header.id){
                    return;
                }
                var winId = header.id.replace("header","");
                for (let i = 0; i < that.wins.length; i++) {
                    that.wins[i].active = that.wins[i].id == winId;
                }
            }
        },
        dragUploadFileInit:function (){
            var that = this;
            utils.delayAction(function (){
                return document.body;
            },function (){
                if(webos.context.get("hasBindUpload")){
                    return;
                };
                webos.context.set("hasBindUpload",true);
                function calcDrag(e){
                    //本地拖拽上传
                    var ele = webos.el.isInClass(e.target,"webos-file-panel");
                    if(!ele){
                        that.dragUpload.show = false;
                        return false;
                    };
                    if(webos.fileSystem.isSpecialPath(ele.dataset.path)){
                        that.dragUpload.show = false;
                        return false;
                    };
                    that.dragUpload.title = ele.dataset.title;
                    that.dragUpload.path = ele.dataset.path;
                    that.dragUpload.show = true;
                    return true;
                }
                webos.fileSystem.bindEleToDragUpload({
                    ele:document.body,
                    enter:function(ele,e){
                        calcDrag(e);
                    },
                    leave:function(ele,e){
                        that.dragUpload.show = false;
                    },
                    drop:function(ele,e,flag){
                        that.dragUpload.show = false;
                        if(flag){
                            webos.message.success(e.dataTransfer.items.length+"个文件或文件夹即将上传");
                        }
                    },
                    dragover:function(ele,e){
                        calcDrag(e);
                    },
                    canUpload:function (ele,e){
                        return calcDrag(e);
                    },
                    path:function (){
                        return that.dragUpload.path;
                    },
                    pathName:function (){
                        return that.dragUpload.title;
                    },
                    sourceName:function (){
                        return "本地";
                    },
                    callback:function (task){
                        that.uploadFileCallback(task);
                    },
                });
            });
        },
        refreshFileBrower:function(parentPath){
            var that = this;
            if(parentPath == that.currentPath){
                that.refreshDesktop();
            };
            if(that.$refs.wins_dialog){
                for(var i=0;i<that.$refs.wins_dialog.length;i++){
                    var ref = that.$refs.wins_dialog[i];
                    var tmpApp = ref.$refs["appComponent"];
                    if(!tmpApp){
                        continue;
                    }
                    if(!tmpApp.fileListAction){
                        continue;
                    }
                    tmpApp.refreshFileList(parentPath);
                }
            }
        },
        getWinComById:function (id){
            var that = this;
            if(that.$refs.wins_dialog){
                for(var i=0;i<that.$refs.wins_dialog.length;i++){
                    var ref = that.$refs.wins_dialog[i];
                    if(ref.$props.win.id == id){
                        return ref;
                    }
                }
            }
            return false;
        },
        uploadFileCallback:async function (task){
            let that = this;
            let hasWin = false;
            for (let i = 0; i < that.wins.length; i++) {
                let tmpWin = that.wins[i];
                if(!tmpWin.close && tmpWin.data && tmpWin.data.app == "upload-task"){
                    hasWin = true;
                    break;
                }
            }
            if(!hasWin){
                let actionWin = await that.openFile("upload-task",3,"文件上传","modules/win11/imgs/uploadtask.png");
                actionWin.width = 400;
                actionWin.height = 650;
                actionWin.disableMax = true;
                actionWin.disableClose = true;
                actionWin.hideSize = true;
                let check = setInterval(function (){
                    if(that.dragUpload.uploadTasks.length == 0){
                        actionWin.close = true;
                        actionWin.show = false;
                        that.dragUpload.actionWin = null;
                        clearInterval(check);
                    }else{
                        for (let i = 0; i < that.dragUpload.uploadTasks.length; i++) {
                            const uploadTask = that.dragUpload.uploadTasks[i];
                            if(uploadTask.children.length === 0){
                                that.dragUpload.uploadTasks.splice(i,1);
                                break;
                            }else{
                                for (let j = 0; j < uploadTask.children.length; j++) {
                                    const child = uploadTask.children[j];
                                    if(child.taskType == "upload" && !webos.fileSystem.hasUploading(child.id)){
                                        uploadTask.children.splice(j,1);
                                    }
                                }
                            }
                        }
                        that.dragUpload.actionWin.data.name = that.dragUpload.uploadTasks.length+"个任务正在执行中";
                    }
                },500);
                that.dragUpload.actionWin = actionWin;
                utils.delayAction(function (){
                    let winCom = that.getWinComById(that.dragUpload.actionWin.id);
                    return winCom&&winCom.$refs&&winCom.$refs.appComponent;
                },function (){
                    let winCom = that.getWinComById(that.dragUpload.actionWin.id);
                    winCom.$refs.appComponent.setHeight(actionWin.height-60);
                },10000);
            }
            if(!task.sd){
                task.sd = 0;
            }else if(task.sd == Infinity){
                task.sd = 0;
            }
            if(!task.loaded){
                task.loaded = 0;
            }else if(task.loaded == Infinity){
                task.loaded = 0;
            }
            if(!task.jd){
                task.jd = 0;
            }else if(task.jd == Infinity){
                task.jd = 0;
            }
            let has = false;
            var index = 0;
            var index2 = 0;
            for(let i=0;i<that.dragUpload.uploadTasks.length;i++){
                let taskGroup = that.dragUpload.uploadTasks[i];
                if(taskGroup.groupId == task.groupId){
                    has = true;
                    index = i;
                    var hasChild = false;
                    for (let j = 0; j < taskGroup.children.length; j++) {
                        let uploadTask = taskGroup.children[j];
                        if(uploadTask.id == task.id){
                            hasChild = true;
                            index2 = j;
                            var tmp = {};
                            for(var key in task){
                                tmp[key] = task[key];
                            }
                            taskGroup.children[index2] = tmp;
                            break;
                        }
                    };
                    if(!hasChild){
                        taskGroup.children.push(task);
                        index2 = 0;
                    }
                    break;
                };
            };
            if(!has){
                that.dragUpload.uploadTasks.push({
                    groupId:task.groupId,
                    sourceName:task.sourceName,
                    pathName:task.pathName,
                    children:[task]
                });
                index = that.dragUpload.uploadTasks.length-1;
                index2 = 0;
            };
            if(task.status == 0){
                //初次上传
                utils.delayAction(function (){
                    return document.querySelector("#task_upload_win");
                },function (){
                    that.topWindow({target:document.querySelector("#task_upload_win")});
                });
            }else if(task.status == 1){
                //进度条
            }else if(task.status == 2){
                //上传成功
                if(that.dragUpload.uploadTasks[index].children.length == 1){
                    var methodName = task.methodName;
                    if(!methodName){
                        methodName = "上传";
                    }
                    webos.message.success("'"+task.name+"'"+methodName+"成功");
                    that.refreshFileBrower(task.path);
                }
                that.dragUpload.uploadTasks[index].children.splice(index2,1);
            }else if(task.status == 3){
                //上传失败
            }else if(task.status == 4){
                //暂停反馈
            }else if(task.status == 5){
                //取消反馈
                that.dragUpload.uploadTasks[index].children.splice(index2,1);
            }
            if(that.dragUpload.actionWin){
                let winCom = that.getWinComById(that.dragUpload.actionWin.id);
                if(winCom&&winCom.$refs&&winCom.$refs.appComponent){
                    winCom.$refs.appComponent.calcCompletion(that.dragUpload.uploadTasks,index);
                }
            }
        },
        moreSelectStart:function (e,type,component){
            //多选蒙版开始
            if(!type){
                return;
            }
            var that = this;
            if(!attr.moreSelectLastStatus){
                attr.moreSelectLastStatus = {};
            }
            attr.moreSelectLastStatus.clientX = e.clientX;
            attr.moreSelectLastStatus.clientY = e.clientY;
            attr.moreSelectLastStatus.move = true;
            attr.moreSelectLastStatus.type = type;
            attr.moreSelectLastStatus.component = component;
            attr.moreSelectLastStatus.e = e;
            var div = document.querySelector(".more-select-wrap");
            attr.moreSelectLastStatus.move = true;
            div.style.display="initial";
            div.style.left = e.clientX +"px";
            div.style.top = e.clientY +"px";
            div.style.width = "0px";
            div.style.height = "0px";
            var fileEle = webos.el.isInClass(e.target,"webos-file");
            var isClear = true;
            if(fileEle && fileEle.classList.contains("select")){
                isClear = false;
                if(e.ctrlKey || e.metaKey){
                    setTimeout(function (){
                        if(type == "desktop"){
                            delete that.fileSelectMap[fileEle.dataset.path];
                        }else if(type == "fileExplorer"){
                            delete component.selectMap[fileEle.dataset.path];
                        }
                    },300);
                }
            };
            if(fileEle){
                if(!attr.moreSelectLastFile){
                    attr.moreSelectLastFile = {};
                }
                if(e.shiftKey){
                    if(attr.moreSelectLastFile.type == type){
                        var parentEle = fileEle.parentElement.parentElement;
                        var list = parentEle.querySelectorAll(".webos-file");
                        var count = 0;
                        for(var i=0;i<list.length;i++){
                            var item = list[i];
                            if(item == fileEle || attr.moreSelectLastFile.last == item){
                                count ++;
                            }
                        }
                        if(count == 2){
                            count = 0;
                            for(var i=0;i<list.length;i++){
                                var item = list[i];
                                if(item == fileEle || attr.moreSelectLastFile.last == item){
                                    count++;
                                }
                                //选择
                                if(count == 1 || count == 2){
                                    if(type == "desktop"){
                                        that.fileSelectMap[item.dataset.path] = {
                                            path:item.dataset.path,
                                            type:item.dataset.type,
                                            icon:item.dataset.icon,
                                            name:item.dataset.name,
                                            ext:item.dataset.ext
                                        };
                                    }else if(type == "fileExplorer"){
                                        attr.moreSelectLastFile.component.selectMap[item.dataset.path] = {
                                            path:item.dataset.path,
                                            type:item.dataset.type,
                                            icon:item.dataset.icon,
                                            name:item.dataset.name,
                                            ext:item.dataset.ext
                                        };
                                    }
                                    if(count == 2){
                                        count = 3;
                                    }
                                }
                            }
                        }
                    }
                }else{
                    attr.moreSelectLastFile.last = fileEle;
                    attr.moreSelectLastFile.type = type;
                    attr.moreSelectLastFile.component = component;
                }
            }
            if(isClear && (e.ctrlKey || e.metaKey || e.shiftKey)){
                isClear = false;
            }
            if (isClear){
                //清空桌面选择
                that.fileSelectMap = {};
                //清空窗口选择
                if(that.$refs.wins_dialog){
                    for(var i=0;i<that.$refs.wins_dialog.length;i++){
                        var ref = that.$refs.wins_dialog[i];
                        if(ref&&ref.$refs&&ref.$refs.appComponent&&ref.$refs.appComponent.clearSelectMap){
                            ref.$refs.appComponent.clearSelectMap();
                        }
                    }
                };
            };
            if(type == "fileExplorer"){
                //算好相对固定值
                function getTop(e){
                    var offset=e.offsetTop;
                    if(e.offsetParent!=null){offset+=getTop(e.offsetParent)};
                    return offset;
                }
                function getLeft(e){
                    var offset=e.offsetLeft;
                    if(e.offsetParent!=null){offset+=getLeft(e.offsetParent)};
                    return offset;
                }
                var one = attr.moreSelectLastStatus.component.$refs['filePanel'].querySelector(".gridshow");
                var left = 0;
                var top = 0;
                if(one){
                    left = getLeft(one);
                    top = getTop(one);
                };
                attr.moreSelectLastStatus.offsetLeft = left;
                attr.moreSelectLastStatus.offsetTop = top - attr.moreSelectLastStatus.component.$refs['filePanel'].scrollTop;
            };
            that.calcMoreSelect(e);
            if(fileEle){
                attr.moreSelectLastStatus.move = false;
                div.style.display = "none";
                that.dragEleFileStart(e);
            }
        },
        dragEleFileStart:function (e){
            if(!attr.dragFileEle){
                attr.dragFileEle = {};
            };
            var ele = webos.el.isInClass(e.target,"webos-file-panel");
            if(!ele){
                attr.dragFileEle.move = false;
                return;
            };
            var fileEle = webos.el.isInClass(e.target,"webos-file");
            var selectFile = false;
            if(fileEle){
                selectFile = {
                    type:fileEle.dataset.type,
                    path:fileEle.dataset.path,
                    name:fileEle.dataset.name,
                    icon:fileEle.dataset.icon,
                    size:fileEle.dataset.size
                };
            };
            var files = [];
            if(selectFile){
                files.push(selectFile);
            };
            var that = this;
            setTimeout(function (){
                var list = ele.querySelectorAll(".select.webos-file");
                for (let i = 0; i < list.length; i++) {
                    var file = list[i];
                    if(selectFile && selectFile.path == file.dataset.path){
                        continue;
                    };
                    files.push({
                        type:file.dataset.type,
                        path:file.dataset.path,
                        name:file.dataset.name,
                        icon:file.dataset.icon,
                        size:file.dataset.size
                    });
                };
                attr.dragFileEle.source.files = files;
                that.dragFile.length = files.length;
            },300);
            attr.dragFileEle.source = { path:ele.dataset.path,title:ele.dataset.title};
            attr.dragFileEle.move = true;
            that.dragFile.icon = fileEle.dataset.icon;
        },
        toRename:async function(file,item){
            var that = this;
            if(item){
                //需要将此文件插入末尾
                await webos.fileSystem.fileIconCalc(item);
                if(item.type == 2){
                    if(!item.thumbnail){
                        item.thumbnail = "modules/win11/imgs/folder-sm.png";
                    }
                }else{
                    if(!item.thumbnail){
                        item.thumbnail = "imgs/file_icon/file.png";
                    }
                }
                that.files.push(item);
            }
            file.newName = file.name;
            that.rename = file;
            that.renameStatus(true);
            utils.delayAction(function (){
                return that.$refs && that.$refs["rename-ta"] && (that.$refs["rename-ta"]).length>0;
            },function (){
                that.$refs["rename-ta"][0].focus();
                that.$refs["rename-ta"][0].select();
            },3000);
        },
        actionRename:async function (file){
            var that = this;
            var text = that.rename.newName;
            if(text == file.name){
                that.renameStatus(false);
                return;
            }
            if(webos.util.getExtByName(text) != file.ext && file.type == 1){
                if(!webos.util.getExtByName(text)){
                    webos.message.error("后缀名不可为空");
                    return;
                }
                utils.$.confirm("当前后缀名和之前不一致,确定继续?",async function (flag2){
                    if(!flag2){
                        that.renameStatus(false);
                        return;
                    };
                    webos.context.set("showOkErrMsg", true);
                    await webos.fileSystem.rename({path:file.path,name:text,type:file.type});
                    that.renameStatus(false);
                    await that.refreshDesktop();
                });
            }else{
                webos.context.set("showOkErrMsg", true);
                await webos.fileSystem.rename({path:file.path,name:text,type:file.type});
                that.renameStatus(false);
                await that.refreshDesktop();
            }
        },
        renameStatus:function (flag){
            var that = this;
            attr.reanmeShow = flag;
            if(!flag){
                that.rename.newName = "";
                that.rename.name = "";
            }
        },
        dragFileEleMove:function (e){
            var that = this;
            if(attr.reanmeShow){
                return;
            }
            if(!attr.dragFileEle){
                that.dragUpload.show = false;
                return;
            };
            if(!attr.dragFileEle.move){
                attr.dragFileEle.target = null;
                that.dragUpload.show = false;
                return;
            };
            var dragEle = document.querySelector(".file-drag-wrap");
            dragEle.style.display = "initial";
            dragEle.style.left = e.clientX+"px";
            dragEle.style.top = e.clientY+"px";
            var ele = webos.el.isInClass(e.target,"webos-file-panel");
            if(!ele){
                attr.dragFileEle.target = null;
                that.dragUpload.show = false;
                return;
            };
            if(webos.fileSystem.isSpecialPath(ele.dataset.path)){
                attr.dragFileEle.target = null;
                that.dragUpload.show = false;
                return;
            }
            if(ele.dataset.path == attr.dragFileEle.source.path){
                attr.dragFileEle.target = null;
                that.dragUpload.show = false;
                return;
            };
            attr.dragFileEle.target = {path:ele.dataset.path,title:ele.dataset.title};
            that.dragUpload.title = attr.dragFileEle.target.title;
            that.dragUpload.path = attr.dragFileEle.target.path;
            that.dragUpload.show = true;
        },
        dragFileEleOver:function (e){
            var that = this;
            if(attr&&attr.dragFileEle&&attr.dragFileEle.move){
                that.dragUpload.show = false;
                attr.dragFileEle.move = false;
                var dragEle = document.querySelector(".file-drag-wrap");
                dragEle.style.display = "";
                if(attr.dragFileEle.target){
                    var files = attr.dragFileEle.source.files;
                    var app = webos.el.findParentComponent(that,"app-component");
                    app.$refs["rm"].desktopTzCopy(files,e);
                }
            }
        },
        calcMoreSelect:function (e){
            //计算选中的元素
            var that = this;
            var div = document.querySelector(".more-select-wrap");
            function is_rect_intersect(x01,x02,y01,y02,x11,x12,y11,y12){
                var zx = Math.abs(x01 + x02 -x11 - x12);
                var x  = Math.abs(x01 - x02) + Math.abs(x11 - x12);
                var zy = Math.abs(y01 + y02 - y11 - y12);
                var y  = Math.abs(y01 - y02) + Math.abs(y11 - y12);
                return zx <= x && zy <= y;
            }
            var divX1 = div.offsetLeft;
            var divY1 = div.offsetTop;
            var divX2 = div.offsetLeft+div.offsetWidth;
            var divY2 = div.offsetTop+div.offsetHeight;
            var type = attr.moreSelectLastStatus.type;
            if(type == "desktop"){
                div.style["z-index"] = 500;
                var list = document.querySelector(".webos-file-panel").querySelectorAll(".webos-file");
                for (var i = 0; i < list.length; i++) {
                    var item = list[i];
                    var itemX1 = item.offsetLeft;
                    var itemY1 = item.offsetTop;
                    var itemX2 = item.offsetLeft+item.offsetWidth;
                    var itemY2 = item.offsetTop+item.offsetHeight;
                    if(is_rect_intersect(itemX1,itemX2,itemY1,itemY2,divX1,divX2,divY1,divY2)){
                        that.fileSelectMap[item.dataset.path] = {
                            path:item.dataset.path,
                            type:item.dataset.type,
                            icon:item.dataset.icon,
                            name:item.dataset.name,
                            ext:item.dataset.ext
                        };
                    }
                }
            }else if(type == "fileExplorer"){
                div.style["z-index"] = 9999999;
                var list = attr.moreSelectLastStatus.component.$refs['filePanel'].querySelectorAll(".webos-file");
                for (var i = 0; i < list.length; i++) {
                    var item = list[i];
                    var itemX1 = attr.moreSelectLastStatus.offsetLeft+item.offsetLeft;
                    var itemY1 = attr.moreSelectLastStatus.offsetTop+item.offsetTop;
                    var itemX2 = itemX1+item.offsetWidth;
                    var itemY2 = itemY1+item.offsetHeight;
                    if(is_rect_intersect(itemX1,itemX2,itemY1,itemY2,divX1,divX2,divY1,divY2)){
                        attr.moreSelectLastStatus.component.selectMap[item.dataset.path] = {
                            path:item.dataset.path,
                            type:item.dataset.type,
                            icon:item.dataset.icon,
                            name:item.dataset.name,
                            ext:item.dataset.ext
                        };
                    };
                }
                attr.moreSelectLastStatus.component.emitSelectFile();
            }
        },
        moreSelectMove:function (e){
            var that = this;
            if(attr.moreSelectLastStatus && attr.moreSelectLastStatus.move){
                //多选蒙版处理
                var div = document.querySelector(".more-select-wrap");
                var left = attr.moreSelectLastStatus.clientX;
                var top = attr.moreSelectLastStatus.clientY;
                var width = e.clientX - left;
                var height =e.clientY - top;
                if(width<0){
                    left = e.clientX;
                    width = -width;
                }
                if(height<0){
                    top = e.clientY;
                    height = -height;
                }
                div.style.left = left+"px";
                div.style.top = top+"px";
                div.style.width = width+"px";
                div.style.height = height+"px";
                that.calcMoreSelect(e);
            }
        },
        moreSelectOver:function (){
            if(attr.moreSelectLastStatus && attr.moreSelectLastStatus.move){
                var div = document.querySelector(".more-select-wrap");
                attr.moreSelectLastStatus = {};
                div.style.display="none";
            }
        },
        fileDblClick:function (file,e){
            var that = this;
            if(attr.reanmeShow){
                return;
            }
            if(e.button != 0){
                return;
            }
            if(that.lastClickObj && that.lastClickObj.dataFile == file &&  Date.now() - that.lastClickObj.time <= 400){
                that.openFile(file.path,file.type,file.name,file.thumbnail,"edit,open");
            };
            that.lastClickObj = {
                dataFile:file,
                time:Date.now()
            }
        },
        toEditShareData:async function (id){
            const that = this;
            var info = await webos.shareFile.info(id);
            that.shareEdit.title = info.name;
            that.shareEdit.show = true;
            that.shareEdit.data = info;
            var url = window.location.origin + window.location.pathname;
            var sz = url.split("/");
            sz.length = sz.length - 1;
            that.shareEdit.preUrl = sz.join("/")+"/index.html?share=";
        },
        toShareData:async function(files){
            var that = this;
            var getParentCurrentPath = function (path){
                var sz = path.split("/");
                var currentPath = sz[sz.length-1];
                sz.length = sz.length - 1;
                var parentPath = sz.join("/");
                return {currentPath,parentPath};
            };
            var list = [];
            var parentPath;
            for (let i = 0; i < files.length; i++) {
                var pathMap = getParentCurrentPath(files[i].path);
                if(!parentPath){
                    parentPath = pathMap.parentPath;
                }
                if(parentPath != pathMap.parentPath){
                    webos.message.error("当前选中文件不在同一个目录中");
                    return;
                }
                list.push(pathMap.currentPath);
            }
            var filesStr = list.join(";");
            var shareId = await webos.shareFile.findOne({path:parentPath,files:filesStr});
            if(shareId){
                await that.toEditShareData(shareId);
                return;
            }
            var code = await webos.shareFile.getCode();
            if(!code){
                webos.message.error("编码生成失败");
                return;
            }
            var title = files[0].name;
            if(files.length>1){
                if(title.length>5){
                    title = title.substring(0,5)+"...";
                }
                title += "等"+files.length+"个文件";
            }
            title = title + "分享";

            that.shareEdit.title = title;
            that.shareEdit.show = true;
            that.shareEdit.data = {
                code:code,
                name:title,
                path:parentPath,
                files:filesStr
            };
            var url = window.location.origin + window.location.pathname;
            var sz = url.split("/");
            sz.length = sz.length - 1;
            that.shareEdit.preUrl = sz.join("/")+"/index.html?share=";
        },
        saveShareEdit:async function (){
            var that = this;
            webos.context.set("showOkErrMsg", true);
            var flag = await webos.shareFile.save(that.shareEdit.data);
        },
        systemAppMap:function (){
            var map = {
                fileExplorer:"此电脑",
                trash:"回收站",
                store:"应用商店",
                edgeBrowser:"Microsoft Edge",
            };
            return map;
        },
        systemAppsInit:function (){
            const that = this;
            var map = that.systemAppMap();
            var sz = [];
            for(var key in map){
                sz.push({filterName:map[key],thumbnail:"modules/win11/imgs/icon/"+key+".png",path:key,type:1,name:map[key]+".webosapp"});
            }
            that.systemApps = sz;
        },
        realSystemData:function (app){
            const that = this;
            var data = {app:app,type:3,name:that.systemAppMap()[app]};
            if(app == "fileExplorer"){
                data.path="thispc";
            }else if(app == "trash"){
                data.path="trash";
                data.targetApp = "fileExplorer";
            }
            return data;
        }
    },
    created:function () {
        var that = this;
        that.systemAppsInit();
        //that.init();
    }
}