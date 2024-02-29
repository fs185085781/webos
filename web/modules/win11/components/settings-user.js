/**/
export default {
    template: `
        <div class="settings-user-component">
          <template v-if="componentData.selectedSecond == ''">
              <div class="accountsTop ">
                <img :src="user.imgPath?user.imgPath:'imgs/logo.png'" alt="" class="user-avatar">
                <div><p>{{user.nickName?user.nickName:'腾飞Webos社区版'}}</p><p>{{user.username}}</p></div>
              </div>
              <div class="tile" @click="changeSecondView('info','帐户信息')">
                <span class="settingsIcon"><i class="fa fa-user"></i></span>
                <div><p>帐户信息</p><p class="tile_desc">当前登录的用户信息</p></div>
              </div>
              <div class="tile" @click="changeSecondView('password','密码修改')">
                <span class="settingsIcon"><i class="fa fa-eye-slash"></i></span>
                <div><p>密码修改</p><p class="tile_desc">修改当前用户的登录密码</p></div>
              </div>
              <div class="tile" @click="changeSecondView('children','家庭和其他用户')" v-if="user.userType == 1">
                <span class="settingsIcon"><i class="fa fa-users"></i></span>
                <div><p>家庭和其他用户</p><p class="tile_desc">子账号管理</p></div>
              </div>
          </template>
          <template v-if="componentData.selectedSecond == 'info'">
             <el-card>
                 <el-form :model="editForm" label-width="120px" style="width:350px;">
                    <el-form-item label="用户名">
                      <el-input v-model="editForm.username"></el-input>
                    </el-form-item>
                    <el-form-item label="图像">
                      <img :src="editForm.imgPath?editForm.imgPath:'imgs/logo.png'" alt="" class="user-avatar s60">
                      <el-button size="small" type="primary" @click="selectUserAvatar()" style="margin-left:5px;">选择</el-button>
                    </el-form-item>
                    <el-form-item label="昵称">
                      <el-input v-model="editForm.nickName"></el-input>
                    </el-form-item>
                    <el-form-item label="锁屏密码">
                      <el-input v-model="editForm.spPassword"></el-input>
                    </el-form-item>
                    <el-form-item>
                      <el-button size="small" type="primary" @click="saveUserInfo()">保存</el-button>
                    </el-form-item>
                 </el-form>
             </el-card>
          </template>
          <template v-if="componentData.selectedSecond == 'password'">
             <el-card>
                 <el-form :model="editForm" label-width="120px" style="width:420px;">
                    <el-form-item label="旧密码">
                      <el-input v-model="editForm.oldPassword"></el-input>
                    </el-form-item>
                    <el-form-item label="新密码">
                      <el-input v-model="editForm.password"></el-input>
                    </el-form-item>
                    <el-form-item label="确认密码">
                      <el-input v-model="editForm.confirmPassword"></el-input>
                    </el-form-item>
                    <el-form-item>
                      <el-button size="small" type="primary" @click="saveUserPassword()">保存</el-button>
                    </el-form-item>
                 </el-form>
             </el-card>
          </template>
          <template v-if="componentData.selectedSecond == 'children'">
             <template v-if="componentData.selectedThird == ''">
                 <el-card style="height:100%;">
                     <el-row>
                            <el-col :span="24">
                                <el-input size="small" v-model="userChildren.params.keyword" placeholder="输入用户名查询" style="width:100px;"></el-input>
                                <el-button size="small" type="primary" @click="userSearch()" style="margin-left: 10px;">查询</el-button>
                                <el-button v-if="user.isAdmin == 1" size="small" type="primary" @click="userEdit(0)" style="margin-left: 10px;">新增主用户</el-button>
                            </el-col>
                     </el-row>
                     <el-table :data="userChildren.data" style="width: 100%;margin-top:10px;" :style="{'height':(win.height-180)+'px'}">
                        <el-table-column fixed prop="username" label="用户名" width="80"></el-table-column>
                        <el-table-column prop="parentUserNo" label="企业编号" width="80"></el-table-column>
                        <el-table-column prop="imgPath" label="图像" width="100">
                            <template #default="scope">
                                <img :src="scope.row.imgPath?scope.row.imgPath:'imgs/logo.png'" alt="" class="user-avatar s40">
                            </template>
                        </el-table-column>
                        <el-table-column prop="nickName" label="昵称" width="130"></el-table-column>
                        <el-table-column prop="createdTime" label="注册时间" width="150"></el-table-column>
                        <el-table-column prop="valid" label="状态" width="50">
                            <template #default="scope">
                                {{userValidMap[scope.row.valid]}}
                            </template>
                        </el-table-column>
                        <el-table-column fixed="right" label="操作" width="250">
                          <template #default="scope">
                            <el-button link type="primary" size="small" @click="userDriveManage(scope.row)">磁盘管理</el-button>
                            <el-button link type="primary" size="small" style="margin-left:2px;min-width:20px;" @click="userEdit(2,scope.row)">编辑</el-button>
                            <el-button link type="primary" size="small" style="margin-left:2px;min-width:20px;" @click="userDel(scope.row)">删除</el-button>
                            <el-button link type="primary" size="small" style="margin-left:2px;min-width:20px;" @click="resetPassword(scope.row)">重置密码</el-button>
                            <el-button link type="primary" size="small" style="margin-left:2px;min-width:20px;" v-if="scope.row.userType == 1" @click="userEdit(1,scope.row)">新增子用户</el-button>
                          </template>
                        </el-table-column>
                     </el-table>
                     <el-pagination
                        small
                        style="margin-top:10px;"
                        background
                        :total="userChildren.pagination.total"
                        layout="total, sizes, prev, pager, next, jumper"
                        v-model="userChildren.pagination.pageSize"
                        @size-change="userSearchSize"
                        @current-change="userSearchPage"
                        @prev-click="userSearchPage"
                        @next-click="userSearchPage"
                     ></el-pagination>
                 </el-card>
             </template>
             <template v-if="componentData.selectedThird == 'edit'">
                 <el-card>
                     <el-form :model="editForm" label-width="120px" style="width:420px;">
                        <el-form-item label="主用户编号" v-if="editForm.parentUserNo">
                            <el-input v-model="editForm.parentUserNo" :disabled="true"></el-input>
                        </el-form-item>
                        <el-form-item label="用户名">
                            <el-input v-model="editForm.username"></el-input>
                        </el-form-item>
                        <el-form-item label="图像">
                            <el-input v-model="editForm.imgPath" :disabled="userChildren.submitType != 2"></el-input>
                        </el-form-item>
                        <el-form-item label="昵称">
                            <el-input v-model="editForm.nickName" :disabled="userChildren.submitType != 2"></el-input>
                        </el-form-item>
                        <el-form-item label="状态">
                            <el-select v-model="editForm.valid" style="width: 100%" :disabled="userChildren.submitType != 2">
                                <el-option v-for="item in userChildren.userValidList" 
                                :value="item.val*1"
                                :label="item.name"
                                :key="item.val*1"
                                >
                                </el-option>
                            </el-select>
                        </el-form-item>
                        <p style="text-align:center;margin:5px 0px;">默认密码:{{defaultPwd}}</p>
                        <el-form-item :wrapper-col="{ span: 14, offset: 4 }">
                            <el-button size="small" type="primary" @click="userEditSubmit">
                                保存
                            </el-button>
                            <el-button size="small" style="margin-left: 10px;" @click="componentData.selectedThird = ''">
                                取消
                            </el-button>
                        </el-form-item>
                     </el-form>
                 </el-card>
             </template>
             <template v-if="componentData.selectedThird == 'user_drive'">
                 <el-card>
                     <el-row v-if="!userChildren.userDrive.edit">
                        <el-col :span="24">
                            <el-button size="small" type="primary" @click="userDriveEdit(0)">新增</el-button>
                        </el-col>
                     </el-row>
                     <el-form v-if="userChildren.userDrive.edit" :model="userChildren.userDrive.editData" label-width="120px" style="width:420px;">
                        <el-form-item label="网盘名称">
                          <el-input v-model="userChildren.userDrive.editData.name" placeholder="磁盘名称"></el-input>
                        </el-form-item>
                        <el-form-item label="磁盘选择">
                            <el-select v-model="userChildren.userDrive.editData.driveId" style="width: 100%">
                                <el-option v-for="item in userChildren.userDrive.ioDriveList" 
                                :value="item.id"
                                :label="item.name+'('+item.path+')'"
                                :key="item.id"
                                >
                                </el-option>
                            </el-select>
                        </el-form-item>
                        <el-form-item label="规格(GB)">
                          <el-input v-model="userChildren.userDrive.editData.maxSize" placeholder="规格(GB)"></el-input>
                        </el-form-item>
                        <el-form-item label="状态">
                            <el-select v-model="userChildren.userDrive.editData.valid" style="width: 100%">
                                <el-option v-for="item in userChildren.userDrive.userDriveValidList" 
                                :value="item.val*1"
                                :label="item.name"
                                :key="item.val*1"
                                >
                                </el-option>
                            </el-select>
                        </el-form-item>
                        <el-form-item>
                            <el-button size="small" type="primary" @click="saveUserDrive()">保存</el-button>
                            <el-button size="small" type="primary" @click="userChildren.userDrive.edit = false;" style="margin-left: 10px;">取消</el-button>
                        </el-form-item>
                     </el-form>
                     <el-table :data="userChildren.userDrive.list" style="width: 100%;margin-top:10px;" :style="{'height':(win.height-180)+'px'}">
                        <el-table-column fixed prop="name" label="名称" width="100"></el-table-column>
                        <el-table-column prop="maxSize" label="总空间" width="100"></el-table-column>
                        <el-table-column prop="useSize" label="已用空间" width="100"></el-table-column>
                        <el-table-column prop="availSize" label="剩余空间" width="100"></el-table-column>
                        <el-table-column prop="valid" label="状态" width="100">
                            <template #default="scope">
                                {{userValidMap[scope.row.valid]}}
                            </template>
                        </el-table-column>
                        <el-table-column fixed="right" label="操作" width="250">
                          <template #default="scope">
                            <el-button link type="primary" size="small" style="margin-left:5px;" @click="userDriveEdit(1,scope.row)">编辑</el-button>
                            <el-button link type="primary" size="small" style="margin-left:5px;" @click="delUserDrive(scope.row)">删除</el-button>
                            <el-button link type="primary" size="small" style="margin-left:5px;" @click="toManageFile(scope.row)">文件管理</el-button>
                          </template>
                        </el-table-column>
                     </el-table>
                 </el-card>
             </template>
          </template>
        </div>
    `,
    props: ['componentData'],
    data(){
      return {
          win:{},
          user:{},
          editForm:{},
          userChildren:{
              data: [],
              pagination: {
                  current: 0,
                  pageSize: 1,
                  total:0
              },
              params: {
                  keyword:"",
                  pageSize:10
              },
              userValidList:[],
              submitType:0,
              userDrive:{
                  list:[],
                  user:{},
                  editData:{
                      valid:1
                  },
                  edit:false,
                  ioDriveList:[],
                  userDriveValidList:[],
                  userDriveValidMap:{}
              }
          },
          userValidMap:{},
          defaultPwd:"123456"
      }
    },
    methods: {
        selectUserAvatar:async function (){
            const that = this;
            var desktop = webos.el.findParentComponent(that,"desktop-component");
            await desktop.selectFileAction("jpg,jpeg,png,bmp,gif",false, async function (files){
                if(files.length > 0){
                    var url = await webos.fileSystem.zl(files[0].path);
                    that.editForm.imgPath = url;
                    return true;
                }
            });
        },
        changeSecondView:async function (val,text){
            var that = this;
            that.componentData.selectedSecond = val;
            that.componentData.selectedSecondText=text;
            that.componentData.selectedThird = "";
            that.editForm = {};
            if(that.componentData.selectedSecond == "info"){
                that.user = await webos.user.info();
                that.editForm = that.user;
            }else if(that.componentData.selectedSecond == "children"){
                that.userSearch();
            }
        },
        saveUserInfo:async function (){
            var that = this;
            webos.context.set("showOkErrMsg",true);
            var flag = await webos.user.updateInfo(that.editForm);
            if(flag){
                that.componentData.selectedSecond = "";
                that.componentData.selectedThird = "";
                var settings = webos.el.findParentComponent(that,"settings-component");
                if(settings){
                    settings.init();
                }
                var desktop = webos.el.findParentComponent(that,"desktop-component");
                if(desktop){
                    desktop.$refs["taskbar"].init();
                }
            }
        },
        saveUserPassword:async function (){
            var that = this;
            if(that.editForm.password != that.editForm.confirmPassword){
                webos.message.error("两次密码输入不一致");
                return;
            }
            webos.context.set("showOkErrMsg",true);
            var flag = await webos.user.updatePassword(that.editForm.oldPassword,that.editForm.password);
            if(flag){
                that.componentData.selectedSecond = "";
                webos.user.logOut();
                var app = webos.el.findParentComponent(that,"app-component");
                app.checkLogin();
            }
        },
        userSearch:async function (page,size){
            var that = this;
            if(!page){
                page = 1;
            }
            if(!size){
                size = 10;
            }
            that.userChildren.params.current = page;
            that.userChildren.params.pageSize = size;
            var data = await webos.user.list(that.userChildren.params);
            if (data) {
                that.userChildren.data = data.data;
                that.userChildren.pagination.total = data.count;
                that.userChildren.pagination.current = that.userChildren.params.current;
                that.userChildren.pagination.pageSize = that.userChildren.params.pageSize;
            }
        },
        userSearchSize:function (size){
            this.userSearch(1,size);
        },
        userSearchPage:function (page){
            this.userSearch(page,this.userChildren.params.pageSize);
        },
        userDel:function (record){
            const that = this;
            utils.$.confirm("确认删除'"+record.username+"'?此操作不可逆",async function (flag){
                if(!flag){
                    return;
                }
                webos.context.set("showOkErrMsg",true);
                var res = await webos.user.del(record.id);
                if(res){
                    await that.userSearch();
                }
            });
        },
        userEdit:async function (type,record){
            //0新增主 1新增子 2编辑
            var that = this;
            that.componentData.selectedThird = "edit";
            if(that.userChildren.userValidList.length < 1){
                that.userChildren.userValidList = await webos.dict.select("USER_VALID");
            }
            if(type == 2){
                //编辑
                that.editForm = await webos.user.infoById(record.id);
                that.componentData.selectedThirdText = "编辑用户";
            }else if(type == 1){
                //新增子
                that.editForm = {valid:1,parentUserNo:record.parentUserNo};
                that.componentData.selectedThirdText = "新增子用户";
            }else{
                //新增主
                that.editForm = {valid:1};
                that.componentData.selectedThirdText = "新增主用户";
            }
            that.userChildren.submitType = type;
        },
        userEditSubmit:async function (){
            var that = this;
            var flag;
            webos.context.set("showOkErrMsg",true);
            if(that.userChildren.submitType == 2){
                //编辑
                flag = await webos.user.update(that.editForm);
            }else if(that.userChildren.submitType == 1){
                //新增子
                flag = await webos.user.createChild(that.editForm);
            }else{
                //新增主
                flag = await webos.user.createMain(that.editForm);
            }
            if(flag){
                that.componentData.selectedThird = "";
                that.userSearch();
            }
        },
        resetPassword:async function (record){
            var that = this;
            utils.$.confirm("确认重置'"+record.username+"'的用户密码为"+that.defaultPwd+"吗?",async function (flag){
                if(!flag){
                    return;
                }
                webos.context.set("showOkErrMsg",true);
                flag = await webos.user.resetPassword(record.id);
                if(flag){
                    that.userSearch();
                }
            });
        },
        userDriveManage:async function (record){
            var that = this;
            that.userChildren.userDrive.user=record;
            that.componentData.selectedThird='user_drive';
            that.componentData.selectedThirdText= record.username+"的磁盘";
            var res = await webos.userDrive.list({"current":1,"pageSize":9999,"userId":record.id});
            that.userChildren.userDrive.list = res.data;
            that.userChildren.userDrive.userDriveValidMap = await webos.dict.selectMap("USER_VALID");
        },
        saveUserDrive:async function (){
            var that = this;
            var flag;
            webos.context.set("showOkErrMsg",true);
            if(that.userChildren.userDrive.editData.id){
                //编辑
                flag = await webos.userDrive.update(that.userChildren.userDrive.editData);
            }else{
                //新增
                flag = await webos.userDrive.save(that.userChildren.userDrive.editData);
            }
            if(flag){
                that.userDriveManage(that.userChildren.userDrive.user);
                that.userChildren.userDrive.edit = false;
            }
        },
        userDriveEdit:async function (type,record){
            var that = this;
            if(that.userChildren.userDrive.ioDriveList.length < 1){
                that.userChildren.userDrive.ioDriveList = await webos.drive.select();
            }
            if(that.userChildren.userDrive.userDriveValidList.length < 1){
                that.userChildren.userDrive.userDriveValidList = await webos.dict.select("USER_VALID");
            }
            if(type == 1){
                //编辑
                that.userChildren.userDrive.editData = await webos.userDrive.info(record.id);
            }else{
                //新增
                that.userChildren.userDrive.editData = {valid:1,userId:that.userChildren.userDrive.user.id}
            }
            that.userChildren.userDrive.edit = true;
        },
        delUserDrive:async function (record){
            var that = this;
            utils.$.confirm("确认删除'"+record.name+"'?(此操作仅删除关联关系)",async function (flag){
                if(!flag){
                    return;
                };
                webos.context.set("showOkErrMsg",true);
                flag = await webos.userDrive.dels([record.id]);
                if(flag){
                    that.userDriveManage(that.userChildren.userDrive.user);
                }
            });
        },
        toManageFile:function (record){
            var that = this;
            var path = "{uio:"+record.no+"}";
            var name = record.name;
            var icon = "modules/win11/imgs/folder-sm.png";
            var desktop = webos.el.findParentComponent(that,"desktop-component");
            desktop.openFile(path,2,name,icon);
        }
    },
    created: async function () {
        var that = this;
        that.user = await webos.user.info();
        let winRef = webos.el.findParentComponent(that,"window-component");
        that.win = winRef.win;
        that.userValidMap = await webos.dict.selectMap("USER_VALID");
        that.defaultPwd = await webos.user.defaultPwd();
    }
}