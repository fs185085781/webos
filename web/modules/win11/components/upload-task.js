/*任务上传弹出框组件*/
export default {
    template: `
        <div class="upload-task-component">
            <div :style="{'height':height+'px'}" style="overflow: auto;">
                <el-card v-for="item in uploadTasks">
                    <el-row>
                        <el-col :span="16" style="height:20px;">剩余{{item.children.length}}个项目从<span style="color:#409eff;">{{item.sourceName}}</span>复制到<span style="color:#409eff;">{{item.pathName}}</span></el-col>
                    </el-row>
                    <el-row>
                        <el-col :span="18" style="height:20px;">
                            <span>{{item.state==2?'已暂停':'运行中'}}{{item.state!=2&&item.syTime?'-剩余'+item.syTime:''}}</span>
                        </el-col>
                    </el-row>
                    <el-row v-for="child in item.children" v-if="spread">
                        <el-col :span="3">
                        </el-col>
                        <el-col :span="21">
                            <el-row style="margin-top:5px;">
                                <el-col :span="18">
                                    <el-row style="margin-top:5px;">
                                        <el-col :span="24" style="font-size:14px;text-overflow:ellipsis;white-space:nowrap;overflow:hidden;">{{child.expName?"("+child.expName+")":""}}{{child.name}}</el-col>
                                    </el-row>
                                </el-col>
                                <el-col :span="6" style="height:20px;">
                                    <svg v-if="child.canInterrupt && child.status == 1" style="right:48px;height:10px;width:10px;" class="el-dialog__headerbtn_svg" @click="uploadAction(child,1)">
                                        <use xlink:href="#pause"></use>
                                    </svg>
                                    <svg v-if="child.canInterrupt && child.status == 4" style="right:48px;height:10px;width:10px;" class="el-dialog__headerbtn_svg" @click="uploadAction(child,2)">
                                        <use xlink:href="#start"></use>
                                    </svg>
                                    <svg v-if="child.canInterrupt && child.status == 3" style="right:48px;height:10px;width:10px;" class="el-dialog__headerbtn_svg" @click="uploadAction(child,3)">
                                        <use xlink:href="#restart"></use>
                                    </svg>
                                    <svg style="right:0px;height:10px;width:10px;" class="el-dialog__headerbtn_svg" @click="uploadAction(child,4)">
                                        <use xlink:href="#close"></use>
                                    </svg>
                                </el-col>
                            </el-row>
                            <el-row style="margin-top:5px;">
                                <el-col :span="24">
                                    <el-progress :percentage="(child.jd*100).toFixed(2)" :text-inside="true" :stroke-width="16"></el-progress>
                                </el-col>
                            </el-row>
                            <el-row>
                            <el-col :span="12">
                                <div style="font-size:12px;color:#999">{{child.size/1024/1024>=1?(child.loaded/1024/1024).toFixed(2):(child.loaded/1024).toFixed(2)}}/{{child.size/1024/1024>=1?(child.size/1024/1024).toFixed(2)+'MB':(child.size/1024).toFixed(2)+'KB'}}</div>
                            </el-col>
                            <el-col :span="12">
                                <div v-if="child.status == 3" style="font-size:12px;color:red;">{{child.msg}}</div>
                                <div v-else-if="child.status == 0" style="font-size:12px;color:blue;">等待上传</div>
                                <div v-else style="font-size:12px;color:#409eff;">{{child.sd>=1024?(child.sd/1024).toFixed(2)+'MB':child.sd.toFixed(2)+'KB'}}/S</div>
                            </el-col>
                        </el-row>
                        </el-col>
                    </el-row>
                </el-card>
            </div>
            <el-row>
                <el-col :span="24" style="padding:5px;">
                    <div style="display: flex;" @click="spread = !spread">
                        <svg class="spread-btn">
                            <use :xlink:href="'#switch-'+(spread?'t':'b')"></use>
                        </svg>
                        <span>{{spread?"简略":"详细"}}信息</span>
                    </div>
                </el-col>
            </el-row>
        </div>
    `,
    props: [],
    data(){
      return {
          spread:false,
          uploadTasks:[],
          height:0
      }
    },
    methods: {
        uploadAction:function (task,type){
            if(!task.canInterrupt && (type==1||type==2||type==3)){
                webos.message.error("该任务不可中断");
                return;
            }
            if(task.expName){
                webos.message.error("文件校验中,不可中断");
                return;
            }
            if(type == 4){
                if(task.cancelType == 1){
                    //服务器复制任务
                    webos.fileSystem.serverStop(task.id).then(function (res){
                        if(res){
                            webos.message.success("任务取消成功,将在下一个文件后终止");
                        }else{
                            webos.message.error("任务取消失败");
                        }
                    });
                    return;
                }else if(task.cancelType == 0){

                }else if(task.cancelType == 2){
                    //前端下载
                    task.status = 5;
                    task.callback(task);
                    if(task.currentXhr){
                        task.currentXhr.abort();
                    }
                    webos.message.success("任务取消成功");
                    return;
                }else{
                    webos.message.error("该任务不可取消");
                    return;
                }
            }
            var res = {};
            if(type == 1){
                //暂停
                res = webos.fileSystem.uploadPause(task.id);
            }else if(type == 2){
                //恢复
                res = webos.fileSystem.uploadStart(task.id);
            }else if(type == 3){
                //重试
                res = webos.fileSystem.uploadRestart(task.id);
            }else if(type == 4){
                //取消
                res = webos.fileSystem.uploadCancel(task.id);
            }
            if(res.flag){
                webos.message.success(res.msg);
            }else{
                webos.message.error(res.msg);
            }
        },
        uploadActionGroup:function (item,type){
            //type 1全部暂停 2全部开始 3全部取消
            var that = this;
            item.children.forEach(function(child){
                if(type == 1){
                    if(child.status == 1){
                        that.uploadAction(child,1);
                    }
                }else if(type == 2){
                    if(child.status == 0){

                    }else if(child.status == 3){
                        that.uploadAction(child,3);
                    }else if(child.status == 4){
                        that.uploadAction(child,2);
                    }
                }else if(type == 3){
                    that.uploadAction(child,4);
                }
            });
        },
        calcCompletion:function (uploadTasks,index){
            var that = this;
            if(that.uploadTasks != uploadTasks){
                that.uploadTasks = uploadTasks;
            }
            //status 0等待执行,1上传中,2上传成功,3上传失败,4暂停中
            //state 1运行中(存在1个status=1) 2.暂停(全部是status=0,3,4)
            let uploadTask = uploadTasks[index];
            if(uploadTask.children.length == 0){
                uploadTasks.splice(index,1);
                return;
            };
            var state = 2;
            for (let i = 0; i < uploadTask.children.length; i++) {
                let task = uploadTask.children[i];
                if(state == 2 && task.status == 1){
                    state = 1;
                }
            };
            uploadTask.state = state;
        },
        setHeight:function (tmp){
            this.height = tmp;
        }
    },
    created: function () {
        const that = this;
        const count = {};
        let timeInterval = setInterval(function (){
            if(!count.count){
                count.count = 1;
            }
            if(!that.uploadTasks || that.uploadTasks.length < 1){
                count.count++;
            }
            if(count.count >= 10){
                clearInterval(timeInterval);
                count.count = 0;
            }
            that.uploadTasks.forEach(function (item){
                if(item.state != 2){
                    let sd = 0;
                    let loaded = 0;
                    let size = 0;
                    for (let i = 0; i < item.children.length; i++) {
                        let child = item.children[i];
                        if(child.sd>1&&child.loaded>1&&child.status==1&&child.size>1){
                            sd += child.sd;
                        }
                        if(child.loaded > 1){
                            loaded += child.loaded;
                        }
                        if(child.size > 1){
                            size += child.size;
                        }
                    }
                    if(sd == 0){
                        item.syTime = "";
                    }else{
                        let s = Math.floor((size-loaded)/1024/sd);
                        item.syTime = s + "秒";
                        if(s>60){
                            let f = Math.floor(s/60);
                            s -= f*60;
                            item.syTime = f+"分"+s+"秒";
                        }
                    }
                }
            });
        },1000);
    }
}