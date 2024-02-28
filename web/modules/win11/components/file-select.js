/**/
export default {
    template: `
        <div class="file-select-component">
            <fileExplorer @select-file="onSelectFile()" ref="fileExp" :win="win" style="position: relative;" :style="{height:(win.height-145)+'px'}"></fileExplorer>
            <el-row style="margin-top:35px;">
              <el-col :span="6" style="text-align: right;line-height: 32px;">文件{{win.data.selectExt=='folder'?'夹':''}}名:</el-col>
              <el-col :span="12"><el-input readonly :value="filesName"></el-input></el-col>
              <el-col :span="6" style="padding-right:10px;"><el-input :value="win.data.selectExtName" readonly></el-input></el-col>
            </el-row>
            <el-row style="margin-top:10px;">
              <el-col :span="24" style="text-align: right;">
                <el-button @click="selectFileConfirm()">确定</el-button>
                <el-button style="margin-right:10px;" @click="thatWinClose()">取消</el-button>
              </el-col>
            </el-row>
        </div>
    `,
    props: ['win'],
    data(){
      return {
          hasError:false,
          files:[],
          filesName:""
      }
    },
    methods: {
        clearSelectMap:function (){
            this.$refs["fileExp"].clearSelectMap();
        },
        fileListAction:function (path){
            var that = this;
            if(!path){
                path = that.$refs["fileExp"].$data.dataPath;
            }
            that.$refs["fileExp"].fileListAction(path);
        },
        refreshFileList:function (parentPath){
            var that = this;
            that.$refs["fileExp"].fileListAction(parentPath);
        },
        selectFileConfirm:async function (){
            var that = this;
            var flag = await that.win.data.fn(that.files);
            if(flag){
                that.thatWinClose();
            }
        },
        thatWinClose:function (){
            var win = webos.el.findParentComponent(this,"window-component");
            win.windowAction(4);
        },
        onSelectFile:function (){
            var that = this;
            that.files = [];
            that.filesName = "";
            var selectMap = that.$refs["fileExp"].$data.selectMap;
            var type = "1";
            var sz = that.win.data.selectExt.split(",");
            if(that.win.data.selectExt == "folder"){
                type = "2";
            }
            var count = 0;
            for(var key in selectMap){
                if(!that.win.data.multi && count > 0){
                    delete selectMap[key];
                    continue;
                }
                var val = selectMap[key];
                if(val.type != type){
                    delete selectMap[key];
                    continue;
                }
                if(val.type == "1"){
                    if(!that.win.data.selectExt){
                        count++;
                        continue;
                    }
                    if(!val.ext){
                        delete selectMap[key];
                        continue;
                    }
                    if(sz.includes(val.ext.toLowerCase())){
                        count++;
                    }else{
                        delete selectMap[key];
                    }
                }else{
                    count++;
                }
            }
            var names = [];
            for(var key in selectMap){
                var val = selectMap[key]
                that.files.push(val);
                names.push(val.name);
            }
            that.filesName = names.join(";");
        }
    },
    created:async function () {
    }
}
