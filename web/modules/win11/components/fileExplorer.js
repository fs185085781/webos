/**/
export default {
    template: `
        <div class="file-explorer-component msfiles floatTab dpShad" :id="win.id">
            <div class="windowScreen flex flex-col">
                <!--上部工具栏-->
                <div class="msribbon flex">
                    <div class="ribsec">
                        <div class="drdwcont flex toolbtn" @click="topToolAction('new')">
                            <div class="uicon  ">
                                <img width="18" data-click="false" data-flip="false" data-invert="false" data-rounded="false" src="modules/win11/imgs/new.png" alt="" style="margin: 0px 6px;">
                            </div>
                            <span>新建</span>
                        </div>
                    </div>
                    <div class="ribsec">
                        <div class="uicon toolbtn" @click="topToolAction('move')">
                            <img width="18" data-click="false" data-flip="false" data-invert="false" data-rounded="false" src="modules/win11/imgs/cut.png" alt="" style="margin: 0px 6px;"></div>
                        <div class="uicon toolbtn" @click="topToolAction('copy')">
                            <img width="18" data-click="false" data-flip="false" data-invert="false" data-rounded="false" src="modules/win11/imgs/copy.png" alt="" style="margin: 0px 6px;"></div>
                        <div class="uicon toolbtn" @click="topToolAction('paste')">
                            <img width="18" data-click="false" data-flip="false" data-invert="false" data-rounded="false" src="modules/win11/imgs/paste.png" alt="" style="margin: 0px 6px;"></div>
                        <div class="uicon toolbtn" @click="topToolAction('rename')">
                            <img width="18" data-click="false" data-flip="false" data-invert="false" data-rounded="false" src="modules/win11/imgs/rename.png" alt="" style="margin: 0px 6px;"></div>
                    </div>
                    <div class="ribsec">
                        <div class="drdwcont flex toolbtn" @click="topToolAction('sort')">
                            <div class="uicon ">
                                <img width="18" data-click="false" data-flip="false" data-invert="false" data-rounded="false" src="modules/win11/imgs/sort.png" alt="" style="margin: 0px 6px;"></div>
                            <span>排序</span>
                        </div>
                        <div class="drdwcont flex toolbtn" @click="topToolAction('view')">
                            <div class="uicon  ">
                                <img width="18" data-click="false" data-flip="false" data-invert="false" data-rounded="false" src="modules/win11/imgs/view.png" alt="" style="margin: 0px 6px;"></div>
                            <span>查看</span>
                        </div>
                    </div>
                </div>
                <!--主体数据-->
                <div class="restWindow flex-grow flex flex-col">
                    <div class="sec1">
                        <!--上一页,下一页,上一级-->
                        <div :class="{'disableIt': cacheIndex == 0}" class="uicon navIcon hvtheme" @mousedown="fileListActionCache(-1)">
                            <svg style="height:14px;width:14px;">
                                <use xlink:href="#left"></use>
                            </svg>
                        </div>
                        <div :class="{'disableIt': cacheIndex == caches.length -1}" class="uicon navIcon hvtheme" @mousedown="fileListActionCache(1)">
                            <svg style="height:14px;width:14px;">
                                <use xlink:href="#right"></use>
                            </svg>
                        </div>
                        <div class="uicon navIcon hvtheme" @mousedown="fileListActionLast()">
                            <svg style="height:14px;width:14px;">
                                <use xlink:href="#top"></use>
                            </svg>
                        </div>
                        <!--地址栏-->
                        <div class="path-bar noscroll" tabindex="-1">
                            <input class="path-field" type="text" v-model="dataPath" @keyup.enter="fileListAction(dataPath)">
                            <div class="dirfbox h-full flex" tabindex="-1">
                                <div class="dirCont flex items-center">
                                    <div class="uicon pr-1 pb-px ">
                                        <img width="16" data-click="false" data-flip="false" data-invert="false" data-rounded="false" src="modules/win11/imgs/user-sm.png" alt="">
                                    </div>
                                    <div class="uicon prtclk dirchev">
                                        <svg style="height:8px;width:8px;">
                                            <use xlink:href="#arrow-r"></use>
                                        </svg>
                                    </div>
                                </div>
                                <div class="dirCont flex items-center" v-for="(path,index) in pathData">
                                    <div v-if="index < pathData.length-1" class="dncont" @mousedown="fileListAction(path.path)">
                                        {{path.pathName}}
                                    </div>
                                    <div v-if="index == pathData.length-1" class="dncont">
                                        {{path.pathName}}
                                    </div>
                                    <div class="uicon prtclk dirchev">
                                        <svg style="height:8px;width:8px;">
                                            <use xlink:href="#arrow-r"></use>
                                        </svg>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <!--搜索框-->
                        <div class="srchbar">
                            <div class="uicon searchIcon ">
                                <img width="12" data-click="false" data-flip="false" data-invert="false" data-rounded="false" src="modules/win11/imgs/search.png" alt="">
                            </div>
                            <input type="text" placeholder="搜索" value="" />
                        </div>
                    </div>
                    <!--左侧快捷访问,目录层次-->
                    <div class="sec2">
                        <div class="navpane win11Scroll" v-if="hasLogin">
                            <div class="extcont">
                                <div class="dropdownmenu">
                                    <div class="droptitle">
                                        <div class="uicon arrUi" @click="starZk = !starZk">
                                            <svg class="svg-inline--fa fa-chevron-right" style="height:10px;width:10px;">
                                                <use :xlink:href="'#arrow-'+(starZk?'b':'r')"></use>
                                            </svg>
                                        </div>
                                        <div class="navtitle flex">
                                            <div class="uicon mr-1 ">
                                                <img width="16" data-click="false" data-flip="false" data-invert="false" data-rounded="false" src="modules/win11/imgs/star-sm.png" alt="">
                                            </div>
                                            <span>快速访问</span>
                                        </div>
                                    </div>
                                    <div class="dropcontent" v-if="starZk">
                                        <div class="dropdownmenu" v-for="star in starList">
                                            <div class="droptitle">
                                                <div class="uicon arrUi opacity-0">
                                                    <svg style="height:10px;width:10px;">
                                                        <use xlink:href="#kbzw"></use>
                                                    </svg>
                                                </div>
                                                <div class="navtitle flex" @mousedown="fileListAction(star.path)">
                                                    <div class="uicon mr-1 ">
                                                        <img v-if="star.type == 'desktop'" width="16" data-click="false" data-flip="false" data-invert="false" data-rounded="false" src="modules/win11/imgs/desk-sm.png" alt="">
                                                        <img v-else-if="star.type == 'downloads'" width="16" data-click="false" data-flip="false" data-invert="false" data-rounded="false" src="modules/win11/imgs/down-sm.png" alt="">
                                                        <img v-else-if="star.type == 'documents'" width="16" data-click="false" data-flip="false" data-invert="false" data-rounded="false" src="modules/win11/imgs/docs-sm.png" alt="">
                                                        <img v-else-if="star.type == 'pictures'" width="16" data-click="false" data-flip="false" data-invert="false" data-rounded="false" src="modules/win11/imgs/pics-sm.png" alt="">
                                                        <img v-else-if="star.type == 'videos'" width="16" data-click="false" data-flip="false" data-invert="false" data-rounded="false" src="modules/win11/imgs/vid-sm.png" alt="">
                                                        <img v-else-if="star.type == 'music'" width="16" data-click="false" data-flip="false" data-invert="false" data-rounded="false" src="modules/win11/imgs/music-sm.png" alt="">
                                                        <img v-else-if="star.type == 'share'" width="16" data-click="false" data-flip="false" data-invert="false" data-rounded="false" src="modules/win11/imgs/share.png" alt="">
                                                        <img v-else-if="star.type == 'trash'" width="16" data-click="false" data-flip="false" data-invert="false" data-rounded="false" src="modules/win11/imgs/icon/trash.png" alt="">
                                                        <img v-else width="16" data-click="false" data-flip="false" data-invert="false" data-rounded="false" src="modules/win11/imgs/folder-sm.png" alt="">
                                                    </div>
                                                    <span>{{star.name}}</span>
                                                </div>
                                                <div class="uicon pinUi ">
                                                    <img width="16" data-click="false" data-flip="false" data-invert="false" data-rounded="false" src="modules/win11/imgs/pinned.png" alt="">
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                                <div class="dropdownmenu" v-if="isMain">
                                    <div class="droptitle">
                                        <div class="uicon arrUi">
                                            <svg class="svg-inline--fa fa-chevron-right" style="height:10px;width:10px;">
                                                <use xlink:href=""></use>
                                            </svg>
                                        </div>
                                        <div class="navtitle flex" @click="fileListAction('disk')">
                                            <div class="uicon mr-1 ">
                                                <img width="16" data-click="false" data-flip="false" data-invert="false" data-rounded="false" src="modules/win11/imgs/disk-sm.png" alt="">
                                            </div>
                                            <span>磁盘管理</span>
                                        </div>
                                    </div>
                                </div>
                                <folder-tree :nodes="[fileTree]" @path-click="path2=>fileListAction(path2)"></folder-tree>
                            </div>
                        </div>
                        <!--主体面板-->
                        <div class="contentarea" @mousedown="moreSelectStart($event)" v-loading="loading">
                            <div ref="filePanel" class="contentwrap win11Scroll upload-win webos-file-panel file-explorer" :data-path="dataPath" :data-title="title">
                                <div class="gridshow" data-size="lg">
                                    <div v-for="file in contentFiles" :title="file.filterName">
                                        <div class="dskApp webos-file" 
                                        :data-path="file.path"
                                        :data-type="file.type"
                                        :data-name="file.name"
                                        :data-icon="file.thumbnail"
                                        :data-size="file.size"
                                        :data-ext="file.ext"
                                        :class="{'select':selectMap[file.path],'rename':rename.name == file.name}" @mousedown="fileViewInWinMouseDbl(file,$event)">
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
                                            <div class="appName" v-else="rename.name != file.name" :class="{'disk':dataPath == 'disk'}">
                                              <div v-if="dataPath == 'disk'" class="edit">
                                                <svg style="height:16px;width:16px;" @click="toEditIoDrive(file.id)" title="编辑磁盘">
                                                    <use xlink:href="#edit"></use>
                                                </svg>
                                                <svg style="height:16px;width:16px;" @click="toRemoveIoDrive(file)" title="删除磁盘">
                                                    <use xlink:href="#remove"></use>
                                                </svg>
                                              </div>
                                              <div v-if="dataPath == 'share'" class="edit">
                                                <svg style="height:16px;width:16px;" @click="toEditShareData(file.id)" title="编辑共享">
                                                    <use xlink:href="#edit"></use>
                                                </svg>
                                                <svg style="height:16px;width:16px;" @click="toRemoveShareData(file)" title="删除共享">
                                                    <use xlink:href="#remove"></use>
                                                </svg>
                                              </div>
                                              {{file.filterName}}
                                            </div>
                                        </div>
                                    </div>
                                    <div @click="toEditIoDrive()" v-if="dataPath == 'disk'">
                                        <div class="dskApp">
                                          <div class="uicon dskIcon" data-pr="true">
                                            <img :width="iconWidth" src="modules/win11/imgs/new.png" alt="">
                                          </div>
                                          <div class="appName">添加</div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                    <!--底部工具栏-->
                    <div class="sec3">
                        <div class="item-count text-xs">{{contentFiles.length}}个项目</div>
                        <div class="view-opts flex">
                            <div class="uicon viewicon hvtheme p-1 " data-open="false" data-action="FILEVIEW" data-payload="5">
                                <img width="16" data-action="FILEVIEW" data-payload="5" data-click="true" data-flip="false" data-invert="false" data-rounded="false" src="modules/win11/imgs/viewinfo.png" alt="">
                            </div>
                            <div class="uicon viewicon hvtheme p-1 " data-open="true" data-action="FILEVIEW" data-payload="1">
                                <img width="16" data-action="FILEVIEW" data-payload="1" data-click="true" data-flip="false" data-invert="false" data-rounded="false" src="modules/win11/imgs/viewlarge.png" alt="">
                            </div>
                        </div>
                    </div>
                    <!--新增编辑磁盘-->
                    <el-dialog
                        draggable
                        v-model="ioDriveEdit.show"
                        :title="ioDriveEdit.title"
                        width="410px"
                        :close-on-click-modal="false" :close-on-press-escape="false"
                        ref="io-drive-edit"
                      >
                       <el-form :model="ioDriveEdit.data" label-width="120px" style="margin-top:10px;">
                        <el-form-item label="名称">
                          <el-input v-model="ioDriveEdit.data.name" style="width:200px;"></el-input>
                        </el-form-item>
                        <el-form-item label="类型">
                          <el-select :disabled="ioDriveEdit.data.id" v-model="ioDriveEdit.data.driveType" placeholder="请选择磁盘类型" style="width:200px;">
                            <el-option v-for="item in driveTypeList" :label="item.name" :value="item.val"></el-option>
                          </el-select>
                        </el-form-item>
                        <el-form-item label="容量(GB)">
                          <el-input v-model="ioDriveEdit.data.maxSize" style="width:200px;"></el-input>
                        </el-form-item>
                        <el-form-item label="token配置" v-if="ioDriveEdit.data.driveType != 'local'">
                          <el-input v-model="ioDriveEdit.data.tokenId" :disabled="ioDriveEdit.data.driveType == 'local' && ioDriveEdit.data.driveType" style="width:200px;"></el-input>
                          <el-button v-if="ioDriveEdit.data.driveType != 'local' && ioDriveEdit.data.driveType" type="primary" size="small" style="margin-left:5px;" @click="toGetToken()">去获取</el-button>
                        </el-form-item>
                        <el-form-item label="别名">
                          <el-input v-model="ioDriveEdit.data.userDriveName" style="width:200px;"></el-input>
                        </el-form-item>
                        <el-form-item label="路径">
                          <el-input :disabled="ioDriveEdit.data.id" v-model="ioDriveEdit.data.path" style="width:200px;"></el-input>
                          <el-button v-if="!ioDriveEdit.data.id && ioDriveEdit.data.driveType" type="primary" size="small" style="margin-left:5px;" @click="selectPath()">选择</el-button>
                          <div v-if="ioDriveEdit.data.driveType == 'kodbox'" style="color:red;line-height: 5px;"><p>请选择具备创建文件夹权限的目录</p>
                          <p>否则将无法分配给子用户使用</p></div>
                        </el-form-item>
                        <el-form-item label="秒传支持" v-if="ioDriveEdit.data.driveType == 'local'">
                            <el-switch :disabled="ioDriveEdit.data.id" v-model="ioDriveEdit.data.secondTransmission" :active-value="1" :inactive-value="2"></el-switch>
                            <div style="color:red;" v-if="ioDriveEdit.data.secondTransmission == 1"><p>当前开启秒传能力,上述路径将存储文件索引</p><p>真实文件储存在下方位置</p></div>
                            <div style="color:red;" v-if="ioDriveEdit.data.secondTransmission == 2"><p>当前关闭秒传能力,上述路径将存储真实文件</p></div>
                        </el-form-item>
                        <el-form-item label="真实文件路径" v-if="ioDriveEdit.data.driveType == 'local' && ioDriveEdit.data.secondTransmission == 1">
                          <el-input :disabled="ioDriveEdit.data.id" v-model="ioDriveEdit.data.realFilePath" style="width:200px;"></el-input>
                          <el-button type="primary" size="small" style="margin-left:5px;" @click="selectRealPath()">选择</el-button>
                          <div style="color:red;"><p>建议所有盘共用同一个真实文件路径</p></div>
                        </el-form-item>
                        <el-form-item>
                          <el-button type="primary" @click="saveIoDrive">保存</el-button>
                          <el-button @click="ioDriveEdit.show = false">取消</el-button>
                        </el-form-item>
                       </el-form>
                    </el-dialog>
                    <el-dialog
                        draggable
                        v-model="configDialog.show"
                        :title="configDialog.title"
                        :width="configDialog.width"
                        :close-on-click-modal="false" :close-on-press-escape="false"
                        ref="iframe-dialog"
                      >
                      <iframe :style="{'height':configDialog.height+'px','width':'100%'}" :src="configDialog.url" frameborder="0"></iframe>
                        <div style="text-align:center;">
                            <el-button @click="configDialogRefresh">刷新</el-button>
                            <el-button @click="configDialog.show=false;configDialog.url=''">取消</el-button>
                        </div>
                    </el-dialog>
                    <el-dialog draggable
                        ref="io-drive-select-path"
                        v-model="selectPathData.show"
                        title="请选择路径"
                        width="600px"
                        :close-on-click-modal="false"
                        :close-on-press-escape="false">
                        <el-row>
                         <el-col :span="24">
                            <el-input v-model="selectPathData.parentPath" style="margin:10px;width:300px;"></el-input>
                            <el-button size="small" type="primary" @click="getFolderByParentPath(selectPathData.parentPath)">前往</el-button>
                            <el-button size="small" type="primary" @click="selectCurrentPath()">选择当前目录</el-button>
                            <el-button size="small" type="primary" @click="selectPathData.show = false">取消</el-button>
                         </el-col>
                        </el-row>
                        <el-row class="path-folder-select" v-loading="selectPathData.loading">
                            <el-col :span="4" @dblclick="getFolderByParentPathLast()">
                                <div><img src="modules/win11/imgs/folder-sm.png" alt=""> </div>
                                <div>..</div>
                            </el-col>
                            <el-col v-for="item in selectPathData.list" :span="4" @dblclick="getFolderByParentPath(item.path)">
                                <div><img src="modules/win11/imgs/folder-sm.png" alt=""> </div>
                                <div class="path-name">{{item.name}}</div>
                            </el-col>
                        </el-row>
                    </el-dialog>
                </div>
            </div>
        </div>
    `,
    props: ['win'],
    data(){
      return {
          hasLogin:true,
          contentFiles:Vue.reactive([]),
          title:"",
          fileTree:{},
          caches:[],
          cacheIndex:0,
          pathData:[],
          dataPath:"",
          selectMap:{},
          starList:[],
          starZk:true,
          isMain:false,
          ioDriveEdit:{
              show:false,
              data:{},
              title:"新增磁盘"
          },
          driveTypeList:[],
          configDialog:{
              show:false,
              url:"",
              title:"阿里云盘",
              width:0,
              height:0,
          },
          iconWidth:43,
          rename:{
              name:"",
              newName:""
          },
          loading:false,
          selectPathData:{
              show:false,
              parentPath:"",
              list:[],
              loading:false,
              field:""
          }
      }
    },
    methods: {
        selectCurrentPath:function (){
            const that = this;
            that.ioDriveEdit.data[that.selectPathData.field] = that.selectPathData.parentPath.replace("//","/");
            that.selectPathData.show = false;
        },
        getFolderByParentPathLast:async function (){
            const that = this;
            var sz = that.selectPathData.parentPath.split("/");
            sz.length -= 1;
            await that.getFolderByParentPath(sz.join("/"));
        },
        getFolderByParentPath:async function(parentPath){
            const that = this;
            var driveType = that.ioDriveEdit.data.driveType;
            var tokenId = that.ioDriveEdit.data.tokenId;
            that.selectPathData.loading = true;
            var res = await webos.drive.getFolderByParentPath({parentPath,driveType,tokenId});
            that.selectPathData.list = res.data;
            that.selectPathData.parentPath = res.parent;
            that.selectPathData.loading = false;
        },
        selectPath:async function (){
            const that = this;
            if(that.ioDriveEdit.data.driveType != "local"){
                webos.context.set("showErrMsg", true);
                var tid = await webos.drive.getTokenId(that.ioDriveEdit.data.driveType,that.ioDriveEdit.data.tokenId);
                if(tid){
                    that.ioDriveEdit.data.tokenId = tid;
                }else{
                    return;
                }
            }
            await that.getFolderByParentPath("");
            that.selectPathData.field = "path";
            that.selectPathData.show = true;
            webos.el.dialogCenter(that.$refs["io-drive-select-path"]);
        },
        selectRealPath:async function (){
            const that = this;
            await that.getFolderByParentPath("");
            that.selectPathData.field = "realFilePath";
            that.selectPathData.show = true;
            webos.el.dialogCenter(that.$refs["io-drive-select-path"]);
        },
        topToolAction:function (type){
            webos.message.error("请使用右键代替");
        },
        fileListActionLast:function (){
            var that = this
            var sz = that.dataPath.split("/");
            if(sz.length>1){
                sz.length = sz.length - 1;
                that.fileListAction(sz.join("/"));
            }else{
                var path = sz[0];
                if(!webos.fileSystem.isSpecialPath(path)){
                    if(path.startsWith("{io:")){
                        that.fileListAction("disk");
                    }else if(path.startsWith("{sio:")){
                        that.fileListAction("share");
                    }else if(path.startsWith("{uio:")){
                        that.fileListAction("thispc");
                    }
                }
            }
        },
        fileListActionCache:function (type){
            var that = this;
            var oldIndex = that.cacheIndex;
            var index = that.cacheIndex + type;
            if(index>that.caches.length -1){
                index = that.caches.length -1;
            }
            if(index < 0){
                index = 0;
            }
            if(oldIndex  == index){
                return;
            }
            that.cacheIndex = index;
            that.fileListAction(that.caches[index],true);
        },
        fileStarAction:async function (){
            var that = this;
            var data =await webos.userDrive.starList();
            data.push({"name":"我的共享","path":"share","type":"share"});
            data.push({"name":"回收站","path":"trash","type":"trash"});
            that.starList = data;
        },
        emitSelectFile:function (){
            var that = this;
            that.$emit("select-file");
        },
        fileExplorerLoading:function (flag){
            this.loading = flag;
        },
        fileListAction:async function (path,ignoreCache){
            var that = this;
            if(that.actionIng){
                return
            }
            if(!path){
                path = "thispc";
            }
            if(path !="/" && path.endsWith("/")){
                path = path.substring(0,path.length-1);
            }
            that.dataPath = path;
            that.contentFiles = [];
            if(!that.caches){
                that.caches = [path];
                that.cacheIndex = 0;
            };
            if(!ignoreCache){
                //需要操作缓存
                if(that.caches[that.caches.length-1] != path){
                    that.caches.push(path);
                };
                that.cacheIndex = that.caches.length-1;
            };
            that.actionIng = true;
            that.loading = true;
            let hasExpData = false;
            var data = await webos.fileSystem.getFileListByParentPath(path,function (fileList,expData){
                that.loading = false;
                if(!hasExpData){
                    hasExpData = true;
                    that.pathData = expData.pathData;
                    that.title = expData.title;
                }
                if(path == "thispc"){
                    fileList.forEach(function (item){
                        item.thumbnail = "modules/win11/imgs/"+(item.isSystem===1?"disc-sm.png":"disk-sm.png");
                    });
                }else if(path == "disk"){
                    fileList.forEach(function (item){
                        item.thumbnail = "imgs/"+item.driveType+".png";
                        if(item.driveType == "local"){
                            item.thumbnail = "modules/win11/imgs/disk-sm.png";
                        }
                    });
                }else{
                    fileList.forEach(function (item){
                        if(item.type == 2){
                            if(!item.thumbnail){
                                item.thumbnail = "modules/win11/imgs/folder-sm.png";
                            }
                        }else{
                            if(!item.thumbnail){
                                item.thumbnail = "imgs/file_icon/file.png";
                            }
                        }
                    });
                };
                that.contentFiles = that.contentFiles.concat(fileList);
            });
            if(!data){
                that.actionIng = false;
                return;
            };
            var config = webos.context.get("rightMenuConfig");
            if(config){
                if(config.largeIcon){
                    that.iconWidth = 54;
                }else if(config.mediumIcon){
                    that.iconWidth = 43;
                }else if(config.smallIcon){
                    that.iconWidth = 36;
                }
            }
            if(config && that.contentFiles.length>0){
                that.contentFiles.sort(function (a,b){
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
            let win = webos.el.findParentComponent(that,"window-component");
            let propsWin = win.$props.win;
            propsWin.data.name = that.title;
            propsWin.data.icon = "modules/win11/imgs/" + (path == "thispc"?"thispc.png":"folder-sm.png");
            that.actionIng = false;
        },
        fileTreeAction:function (node){
            if(!node){
                this.fileTree = Vue.reactive({
                    path:"thispc",
                    name:"此电脑",
                    icon:"modules/win11/imgs/thispc.png",
                    starZk:false
                });
            }
        },
        fileViewInWinMouseDbl:function (file,e){
            var that = this;
            if(e.button != 0){
                return;
            }
            if(that.lastClickObj && that.lastClickObj.dataFile == file &&  Date.now() - that.lastClickObj.time <= 400){
                if(file.path.startsWith("{trash:")){
                    webos.message.error("请恢复后再进行访问");
                    return;
                }
                if(file.type == 2){
                    that.fileListAction(file.path);
                }else{
                    var desktop = webos.el.findParentComponent(that,"desktop-component");
                    desktop.openFile(file.path,file.type,file.name,file.thumbnail,"edit,open");
                }
            };
            that.lastClickObj = {
                dataFile:file,
                time:Date.now()
            }
        },
        refreshFileList:function (path){
            if(this.dataPath == path){
                this.fileListAction(path);
            }
        },
        clearSelectMap:function (){
            this.selectMap = {};
        },
        toEditIoDrive:async function (id){
            var that = this;
            if(that.driveTypeList.length < 1){
                that.driveTypeList = await webos.dict.select("IO_DRIVE_TYPE");
            }
            if(!id){
                //新增
                that.ioDriveEdit.title = "添加磁盘";
                that.ioDriveEdit.data = {}
            }else{
                //修改
                that.ioDriveEdit.title = "编辑磁盘";
                that.ioDriveEdit.data = await webos.drive.info(id);
            }
            that.ioDriveEdit.show = true;
            webos.el.dialogCenter(that.$refs["io-drive-edit"]);
        },
        toRemoveIoDrive:function (item){
            const that = this;
            utils.$.confirm("确认移除'"+item.filterName+"'存储吗?",function (flag){
                if(!flag){
                    return;
                };
                utils.$.confirm("再次确认移除'"+item.filterName+"'存储吗?移除后该盘下的用户数据将无法访问!",async function (flag2){
                    if(!flag2){
                        return;
                    };
                    webos.context.set("showOkErrMsg", true);
                    flag = await webos.drive.dels([item.id]);
                    if(flag){
                        that.fileListAction(that.dataPath);
                    }
                });
            });
        },
        toEditShareData:function (id){
            var that = this;
            var desktop = webos.el.findParentComponent(that,"desktop-component");
            desktop.toEditShareData(id);
        },
        toRemoveShareData:function (item){
            const that = this;
            utils.$.confirm("确认取消'"+item.filterName+"'共享吗?取消后该分享数据将无法访问!",async function (flag){
                if(!flag){
                    return;
                };
                webos.context.set("showOkErrMsg", true);
                flag = await webos.shareFile.dels([item.id]);
                if(flag){
                    await that.fileListAction(that.dataPath);
                }
            });
        },
        configDialogRefresh:function (){
            var that = this;
            var tmpUrl = that.configDialog.url;
            that.configDialog.url = "";
            setTimeout(function (){
                that.configDialog.url = tmpUrl;
            },300);
        },
        toGetToken:function (){
            const that = this;
            const driveType = that.ioDriveEdit.data.driveType;
            const onTokenOrCookie = function (tokenId){
                that.ioDriveEdit.data.tokenId = tokenId;
                that.configDialog.url = "";
                that.configDialog.show = false;
            }
            const defaultFunc = function (){
                webos.drive.defaultEventAction = function (e){
                    var data = e.data;
                    if(data.type == "cookie"){
                        onTokenOrCookie(data.data);
                    };
                }
                if(webos.drive.initDefaultEvent){
                    return;
                }
                if(!webos.drive.initDefaultEvent){
                    webos.drive.initDefaultEvent = true;
                    window.addEventListener("message",function (e){
                        webos.drive.defaultEventAction(e);
                    });
                }
            }
            const typeMap = {
                pan123:false,
                local:false,
                aliyundrive:{
                    title:"阿里云盘-登录",
                    url:"https://passport.aliyundrive.com/mini_login.htm?lang=zh_cn&appName=aliyun_drive&appEntrance=web&styleType=auto&bizParams=&notLoadSsoView=false&notKeepLogin=false&isMobile=false&ad__pass__q__rememberLogin=false&ad__pass__q__forgotPassword=true&ad__pass__q__licenseMargin=true&ad__pass__q__loginType=normal&hidePhoneCode=true",
                    func:function (){
                        webos.drive.aliyunEvent(function (res){
                            if(res.type == "token"){
                                //获取token
                                var data = res.data;
                                onTokenOrCookie(data.refreshToken);
                            }
                        });
                    }
                },
                pan189:{
                    title:"天翼云盘-登录",
                    url:"common/pan189_web/login.html",
                    func:defaultFunc
                },
                kodbox:{
                    title:"可道云-登录",
                    url:"common/kodbox_web/login.html",
                    func:defaultFunc
                },
            }
            if(!typeMap[driveType]){
                webos.message.error("当前不支持使用窗口获取token");
                return;
            };
            const typeData = typeMap[driveType];
            //登录页
            that.configDialog.title = typeData.title;
            that.configDialog.url = typeData.url;
            that.configDialog.width = 388;
            that.configDialog.height = 343;
            that.configDialog.show = true;
            typeData.func();
            webos.el.dialogCenter(that.$refs["iframe-dialog"]);
        },
        saveIoDrive:async function (){
            var that = this;
            var flag;
            webos.context.set("showOkErrMsg", true);
            if(that.ioDriveEdit.data.id){
                //编辑
                flag = await webos.drive.update(that.ioDriveEdit.data);
            }else{
                //新增
                flag = await webos.drive.save(that.ioDriveEdit.data);
            }
            if(flag){
                that.ioDriveEdit.show = false;
                that.fileListAction("disk");
            }
        },
        moreSelectStart:function (e){
            var desktop = webos.el.findParentComponent(this,"desktop-component");
            desktop.moreSelectStart(e,"fileExplorer",this);
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
                that.contentFiles.push(item);
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
                await that.fileListAction(that.dataPath);
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
                    await that.fileListAction(that.dataPath);
                });
            }else{
                webos.context.set("showOkErrMsg", true);
                await webos.fileSystem.rename({path:file.path,name:text,type:file.type});
                that.renameStatus(false);
                await that.fileListAction(that.dataPath);
            }
        },
        renameStatus:function (flag){
            var that = this;
            if(!flag){
                that.rename.newName = "";
                that.rename.name = "";
            }
        },
    },
    created:async function () {
        this.fileListAction(this.win.data.path,true);
        var hasLogin = await webos.user.hasLogin();
        if(hasLogin){
            await this.fileStarAction();
            this.fileTreeAction();
        }else {
            this.hasLogin = false;
        }
        if(this.hasLogin){
            var info = await webos.user.info();
            if(info){
                this.isMain = info.userType == 1;
            }
        }
    }
}