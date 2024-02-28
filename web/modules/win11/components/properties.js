/**/
export default {
    template: `
        <div class="properties-component notepad floatTab dpShad" :id="win.id">
            <div class="windowScreen flex flex-col">
              <el-card class="box-card">
                 <el-tabs type="border-card" :style="{'height':(win.height-93)+'px'}">
                    <el-tab-pane label="常规">
                        <el-row style="border-bottom:1px solid var(--el-border-color);">
                            <el-col :span="5">
                                <img :src="info.thumbnail" style="width:60px;height:60px;object-fit: cover;">
                            </el-col>
                            <el-col :span="19" style="padding-top:13px;">
                                <el-input readonly :value="info.name"></el-input>
                            </el-col>
                        </el-row>
                        <el-row style="border-bottom:1px solid var(--el-border-color);" v-if="info.type == '1'">
                            <el-col :span="5" style="margin-top:10px">
                                文件类型:
                            </el-col>
                            <el-col :span="19" style="margin-top:10px">
                                {{info.ext}}
                            </el-col>
                            <el-col :span="5" style="margin-top:10px;margin-bottom:10px">
                                打开方式:
                            </el-col>
                            <el-col :span="19" style="margin-top:10px;margin-bottom:10px">
                                {{extApp}}<el-tag v-if="!isSystemApp" @click="changeAppOpen(info.ext)" style="cursor:pointer;float:right;">更改</el-tag>
                            </el-col>
                        </el-row>
                        <el-row style="border-bottom:1px solid var(--el-border-color);">
                            <el-col :span="5" style="margin-top:10px;margin-bottom:10px">
                                位置:
                            </el-col>
                            <el-col :span="16" style="margin-top:10px;margin-bottom:10px" class="prop-link">
                                {{info.path}}
                            </el-col>
                            <el-col :span="3" style="margin-top:10px;">
                              <el-tag v-if="!isSystemApp" @click="copyUrl(info.path)" style="cursor:pointer;float:right;">复制</el-tag>
                            </el-col>
                            <el-col :span="5" style="margin-bottom:10px" v-if="info.type == '1' && info.link">
                                外链:
                            </el-col>
                            <el-col :span="16" style="margin-bottom:10px" v-if="info.type == '1' && info.link" class="prop-link">
                                {{info.link}}&nbsp;
                            </el-col>
                            <el-col :span="3" style="margin-bottom:10px" v-if="info.type == '1' && info.link">
                              <el-tag @click="copyUrl(info.link)" style="cursor:pointer;float:right;">复制</el-tag>
                            </el-col>
                            <el-col :span="5" style="margin-bottom:10px" v-if="info.type == '1' && info.size > 0">
                                大小:
                            </el-col>
                            <el-col :span="19" style="margin-bottom:10px" v-if="info.type == '1' && info.size > 0">
                                {{info.sizeName}}({{info.size}}字节)
                            </el-col>
                        </el-row>
                        <el-row v-if="info.createdAt">
                            <el-col :span="5" style="margin-top:10px">
                                {{isTrash?"删除":"创建"}}时间:
                            </el-col>
                            <el-col :span="19" style="margin-top:10px">
                                {{info.createdAt}}
                            </el-col>
                            <el-col v-if="info.updatedAt" :span="5" style="margin-top:10px;margin-bottom:10px">
                                修改时间:
                            </el-col>
                            <el-col v-if="info.updatedAt" :span="19" style="margin-top:10px;margin-bottom:10px">
                                {{info.updatedAt}}
                            </el-col>
                        </el-row>
                    </el-tab-pane>
                    <el-tab-pane label="安全">此卡片敬请期待</el-tab-pane>
                    <el-tab-pane label="详细信息">此卡片敬请期待</el-tab-pane>
                 </el-tabs>
                 <div class="prop-btns">
                   <el-button @click="closeWinAction()">确定</el-button>
                   <el-button @click="closeWinAction()">取消</el-button>
                   <el-button disabled>应用</el-button>
                 </div>
              </el-card>
            </div>
        </div>
    `,
    props: ['win'],
    data(){
      return {
          info:{},
          extApp:"未知应用",
          isTrash:false,
          isSystemApp:false
      }
    },
    methods: {
        initData:async function (){
            var that = this;
            if(that.win.data.filePath.startsWith("{trash:")){
                that.isTrash = true;
            }
            var desktop = webos.el.findParentComponent(that,"desktop-component");
            var systemMap = desktop.systemAppMap();
            if(systemMap[that.win.data.filePath]){
                var key = that.win.data.filePath;
                that.info = {filterName:systemMap[key],thumbnail:"modules/win11/imgs/icon/"+key+".png",path:"桌面",type:1,name:systemMap[key]+".webosapp",size:0,ext:"webosapp"};
                that.isSystemApp = true;
            }else{
                that.info = await webos.fileSystem.fileInfo(that.win.data.filePath);
            }
            if(that.info && that.info.name){
                var win = webos.el.findParentComponent(that,"window-component");
                win.$props["win"].data.name = that.info.name+"属性";
            }
            await webos.fileSystem.fileIconCalc(that.info);
            if(!that.info.thumbnail){
                that.info.thumbnail = that.win.data.fileIcon;
            }
            if(that.info.type == 1){
                that.info.sizeName = that.getSizeName(that.info.size);
                if(that.info.ext.toLowerCase() != "webosapp" && !that.isTrash){
                    that.info.link = await webos.fileSystem.zl(that.win.data.filePath);
                }
                if(that.info.ext){
                    if(that.info.ext.toLowerCase() != "webosapp"){
                        var sz = ["edit","open"];
                        if(webos.util.isMedia(that.info.ext)){
                            sz = ["open","edit"];
                        }
                        let app = await webos.util.userOpenApp(that.info.ext,sz);
                        if(app){
                            that.extApp =  app.appName;
                        }
                    }else{
                        that.extApp =  "应用程序";
                    }
                }

            }
        },
        copyUrl:function (url){
            if(utils.copyText(url)){
                webos.message.success("复制成功");
            }
        },
        getSizeName:function (size){
            var sz = ["B","KB","MB","GB","TB","PB"];
            var sizeAction = function(data){
                if(data.size>=1024){
                    return sizeAction({size:data.size/1024,dw:data.dw+1});
                }else{
                    return data.size.toFixed(2)+sz[data.dw];
                }
            }
            return sizeAction({size:size,dw:0});
        },
        changeAppOpen:function (){
            webos.message.error("此功能敬请期待");
        },
        closeWinAction:function (){
            var that = this;
            var winCom = webos.el.findParentComponent(that,"window-component");
            winCom.windowAction(4);
        }
    },
    created:async function () {
    }
}