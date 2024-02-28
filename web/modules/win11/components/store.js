/**/
export default {
    template: `
        <div class="store-component wnstore floatTab dpShad">
            <div class="windowScreen flex">
              <div class="storeNav h-full w-20 flex flex-col">
                <div @click="selectTypeAction(type)" class="uicon prtclk" :data-payload="type == selectType?'true':'false'" v-for="type in types">
                  <svg style="height:18px;width:18px;">
                     <use :xlink:href="'#'+type"></use>
                  </svg>
                </div>
              </div>
              <div class="restWindow msfull win11Scroll">
                <div v-if="(['house']).includes(selectType)" class="pagecont w-full absolute top-0">
                  <div v-for="item in houseData" :class="item.className" class="frontCont my-8 py-20 w-auto mx-8 flex justify-between noscroll overflow-x-scroll overflow-y-hidden">
                    <div class="flex w-64 flex-col text-gray-100 h-full px-8" style="min-width:100px;">
                      <div class="text-xl">{{item.title}}</div>
                      <div class="text-xs mt-2">{{item.describe}}</div>
                    </div>
                    <div class="flex w-max pr-8">
                      <div v-for="oneApp in item.data" class="ribcont rounded my-auto p-2 pb-2" @click="selectTypeAction('detail',oneApp)">
                        <div class="imageCont prtclk mx-1 py-1 mb-2 rounded" data-back="false">
                          <img width="120" data-free="false" :src="oneApp.imgPath" alt="">
                        </div>
                        <div class="capitalize text-xs font-semibold">{{oneApp.name}}</div>
                        <div class="flex mt-2 items-center">
                          <div class="uicon prtclk" :class="{'bluestar':oneApp.average>=index}" v-for="index in 5">
                            <svg style="height:6px;width:6px;">
                               <use xlink:href="#store-star"></use>
                            </svg>
                          </div>
                          <div class="text-xss">{{oneApp.ratings}}</div>
                        </div>
                        <div class="text-xss mt-8">{{hasMap[oneApp.code]?'已安装':'去安装'}}</div>
                      </div>
                    </div>
                  </div>
                </div>
                <div v-if="(['store-app','store-game','store-local']).includes(selectType)" class="pagecont w-full absolute top-0 box-border p-12">
                  <div class="flex" v-if="selectType != 'store-local'">
                    <div class="catbtn handcr" :value="appOrGame.param.secondCat == ''" @click="changeSecondCats('')">所有</div>
                    <div v-for="secondCat in appOrGame.secondCats" :value="appOrGame.param.secondCat == secondCat" class="catbtn handcr" @click="changeSecondCats(secondCat)">{{secondCat}}</div>
                  </div>
                  <div class="flex" v-if="selectType == 'store-local'">
                    <div class="absolute right-0 mr-4 text-sm">
                      <a class="catbtn" target="_blank" @click="createLocalApp(1)">创建轻应用</a>
                      <a class="catbtn" target="_blank" @click="createLocalApp(2)">本地程序</a>
                    </div>
                  </div>
                  <div class="appscont mt-8">
                    <div v-for="oneApp in appOrGame.data.data" class="ribcont p-4 pt-8 ltShad prtclk" @click="selectTypeAction('detail',oneApp)">
                      <div class="imageCont prtclk mx-4 mb-6 rounded" data-back="false">
                        <img width="100" height="100" :src="oneApp.imgPath" alt="">
                      </div>
                      <div class="capitalize text-xs font-semibold">{{oneApp.name}}</div>
                      <div class="capitalize text-xss text-gray-600">{{oneApp.secondCat}}</div>
                      <div class="flex items-center" v-if="selectType != 'store-local'">
                        <div class="uicon prtclk" :class="{'bluestar':oneApp.average>=index}" v-for="index in 5">
                          <svg style="height:6px;width:6px;">
                             <use xlink:href="#store-star"></use>
                          </svg>
                        </div>
                        <div class="text-xss">{{oneApp.ratings}}</div>
                      </div>
                      <div class="text-xss mt-8">{{hasMap[oneApp.code]?'已安装':'去安装'}}</div>
                    </div>
                  </div>
                </div>
                <div v-if="(['download']).includes(selectType)" class="pagecont w-full absolute top-0 box-border p-12">
                  <div class="appscont mt-8">
                    <div v-for="oneApp in hasList" class="ribcont p-4 pt-8 ltShad prtclk" @click="selectTypeAction('detail',oneApp)">
                      <div class="imageCont prtclk mx-4 mb-6 rounded" data-back="false">
                        <img width="100" height="100" :src="oneApp.imgPath" alt="">
                      </div>
                      <div class="capitalize text-xs font-semibold">{{oneApp.name}}</div>
                      <div class="capitalize text-xss text-gray-600">{{oneApp.secondCat}}</div>
                      <div class="text-xss mt-8">已安装</div>
                    </div>
                  </div>
                </div>
                <div v-if="(['detail']).includes(selectType)" class="detailpage w-full absolute top-0 flex">
                  <div class="detailcont">
                    <div class="imageCont prtclk rounded">
                      <img width="100" height="100" :src="detailApp.imgPath" alt="">
                    </div>
                    <div class="flex flex-col items-center text-center relative">
                      <div class="text-2xl font-semibold mt-6">{{detailApp.name}}</div>
                      <div class="text-xs text-blue-500">{{detailApp.author}}</div>
                      <div class="instbtn mt-12 handcr" v-if="!hasMap[detailApp.code]" @click="actionDetailApp(detailApp,1)">安装</div>
                      <div class="instbtn mt-1 handcr" v-if="hasMap[detailApp.code]" @click="actionDetailApp(detailApp,2)">卸载</div>
                      <div class="instbtn mt-1 handcr" v-if="hasMap[detailApp.code]" @click="actionDetailApp(detailApp,3)">打开</div>
                      <div class="instbtn mt-1 handcr" v-if="detailApp.needUpdate" @click="actionDetailApp(detailApp,5)">升级</div>
                      <div class="flex mt-4">
                        <div>
                          <div class="flex items-center text-sm font-semibold">
                            {{detailApp.average}}
                            <div class="uicon prtclk text-orange-600 ml-1">
                              <svg style="height:14px;width:14px;">
                               <use xlink:href="#store-star"></use>
                              </svg>
                            </div>
                          </div>
                          <span class="text-xss">平均分</span>
                        </div>
                        <div class="w-px bg-gray-300 mx-4"></div>
                        <div>
                          <div class="text-sm font-semibold">{{detailApp.ratings}}</div>
                          <div class="text-xss mt-px pt-1">评论数</div>
                        </div>
                      </div>
                      <div class="descnt text-xs relative w-0">
                        {{detailApp.descr}}
                      </div>
                    </div>
                  </div>
                  <div class="growcont flex flex-col">
                    <div class="briefcont py-2 pb-3">
                      <div class="text-xs font-semibold">
                        截图
                      </div>
                      <div class="overflow-x-scroll win11Scroll mt-4">
                        <div class="w-max flex">
                          <div v-for="screenShot in detailApp.screenShotsData" class="imageCont prtclk mr-2 rounded">
                            <img height="250" data-free="false" :src="screenShot" alt="">
                          </div>
                        </div>
                      </div>
                    </div>
                    <div class="briefcont py-2 pb-3">
                      <div class="text-xs font-semibold">描述</div>
                      <div class="text-xs mt-4">
                        <pre>{{detailApp.descr}}</pre>
                      </div>
                    </div>
                    <div class="briefcont py-2 pb-3">
                      <div class="text-xs font-semibold">评级和评论</div>
                      <div class="flex mt-4 items-center">
                        <div class="flex flex-col items-center" style="min-width:80px;">
                          <div class="text-5xl reviewtxt font-bold">{{detailApp.average}}</div>
                          <div class="text-xss">{{detailApp.ratings}} 评论数</div>
                        </div>
                        <div class="text-xss ml-6">
                          <div v-for="index in [5,4,3,2,1]" class="flex items-center">
                            <div class="h-4">{{index}}</div>
                            <div class="uicon prtclk text-orange-500 ml-1">
                              <svg style="height:8px;width:8px;">
                                <use xlink:href="#store-star"></use>
                              </svg>
                            </div>
                            <div class="w-48 ml-2 bg-orange-200 rounded-full">
                              <div class="rounded-full bg-orange-500" :style="{'width':(detailApp['score'+index]*100/detailApp.ratings).toFixed(3)+'%'}" style="padding: 3px 0px;"></div>
                            </div>
                          </div>
                        </div>
                        <div class="text-xss ml-6" v-if="detailApp.isLocal == 2">
                            <div class="flex items-center">期待你的打分^_^</div>
                            <el-rate v-model="detailPf" @click="toAppScore()"></el-rate>
                        </div>
                      </div>
                    </div>
                    <div class="briefcont py-2 pb-3">
                      <div class="text-xs font-semibold">功能</div>
                      <div class="text-xs mt-4">
                        <pre v-html="detailApp.effect"></pre>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <el-dialog
                draggable
                v-model="addLocalApp.show"
                :title="addLocalApp.title"
                width="410px"
                :close-on-click-modal="false" :close-on-press-escape="false"
                ref="dialog"
              >
               <el-form :model="addLocalApp.data" label-width="120px">
                <el-form-item label="名称" v-if="addLocalApp.type == 1">
                  <el-input v-model="addLocalApp.data.name" style="width:200px;"></el-input>
                </el-form-item>
                <el-form-item label="图标" v-if="addLocalApp.type == 1">
                  <el-input v-model="addLocalApp.data.icon" style="width:200px;"></el-input>
                </el-form-item>
                <el-form-item label="地址" v-if="addLocalApp.type == 1">
                  <el-input v-model="addLocalApp.data.url" style="width:200px;"></el-input>
                </el-form-item>
                <el-form-item label="文件" v-if="addLocalApp.type == 2">
                  <el-upload
                    :limit="1"
                    :auto-upload="false"
                    v-model:file-list="addLocalApp.data.file"
                    :drag="true"
                    accept="application/zip"
                    :on-exceed="fileSelectHandleExceed"
                    ref="upload"
                  >
                    <div><i class="fa fa-upload" style="font-size: 55px;"></i></div>
                    <div class="el-upload__text">
                      拖拽到这里或者 <em>点我上传</em>
                    </div>
                  </el-upload>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="saveLocalApp()">保存</el-button>
                  <el-button @click="addLocalApp.show = false">取消</el-button>
                </el-form-item>
               </el-form>
            </el-dialog>
        </div>
    `,
    props: [],
    data() {
        return {
            selectType: "house",
            detailApp: {},
            detailPf: 0,
            types: ["house", "store-app", "store-game","store-local","download"],
            houseData: [
                {
                    className: "amzApps",
                    title: "精选应用",
                    describe: "使用这些必备应用程序将您的体验提升到新的高度",
                    data: []
                },
                {
                    className: "amzGames",
                    title: "特色游戏",
                    describe: "探索乐趣来玩xbox 游戏并找到一个新收藏",
                    data: []
                }
            ],
            appOrGame:{
                param:{
                    current:1,
                    pageSize:9999,
                    firstCat:"app",
                    secondCat:"",
                    keyWord:"",
                    orderField:"average",
                    orderType:"desc"
                },
                hasFinish:false,
                data:{
                    data:[],
                    count:0
                },
                secondCats:[]
            },
            hasMap:{},
            hasList:[],
            addLocalApp:{
                show:false,
                data:{},
                type:0,
                title:""
            }
        }
    },
    methods: {
        selectTypeAction: function (type, oneApp) {
            var that = this;
            that.selectType = type;
            if (that.selectType == "house") {
                //首页点击
                that.houseInit();
            } else if (that.selectType == "detail") {
                //详情点击
                that.showDetail(oneApp);
            } else if (that.selectType == "store-app" || that.selectType == "store-game") {
                //app列表 //游戏列表
                if (that.selectType == "store-app") {
                    //app列表
                    that.appOrGame.param.firstCat = "app";
                } else if (that.selectType == "store-game") {
                    //游戏列表
                    that.appOrGame.param.firstCat = "game";
                }
                that.appOrGame.param.secondCat = "";
                that.appOrGame.hasFinish = false;
                that.searchAppOrGame(true);
            }else if(that.selectType == "store-local"){
                //本地列表
                that.searchLocalApp();
            }else if(that.selectType == "download"){
                //已安装列表
                that.hasMapInit();
            }
        },
        storeUrl: function () {

        },
        searchLocalApp:async function (){
            var that = this;
            var list = await webos.softUser.list();
            if(list){
                that.appOrGame.data.data = list;
            }
        },
        searchAppOrGame:async function (isInit){
            var that = this;
            if(isInit){
                that.appOrGame.hasFinish = false;
            };
            if(that.appOrGame.hasFinish){
                return;
            };
            if(isInit){
                that.appOrGame.param.current = 1;
                that.appOrGame.data.data = [];
                that.appOrGame.data.count = 0;
                that.appOrGame.secondCats = await webos.softStore.secondCats({firstCat:that.appOrGame.param.firstCat});
            }
            var data = await webos.softStore.list(that.appOrGame.param);
            if(!data){
                that.appOrGame.hasFinish = true;
                return;
            }
            data.data = that.fliterApps(data.data);
            that.appOrGame.data.data = that.appOrGame.data.data.concat(data.data);
            that.appOrGame.data.count = data.count;
            var maxPage = Math.floor(data.count/that.appOrGame.param.pageSize);
            if(data.count%that.appOrGame.param.pageSize != 0){
                maxPage++;
            }
            if(maxPage == 0){
                maxPage = 1;
            }
            if(maxPage == that.appOrGame.param.current){
                that.appOrGame.hasFinish = true;
            }
        },
        showDetail:async function (oneApp){
            var that = this;
            that.detailPf = 0;
            that.detailApp = oneApp;
            var storeData = await webos.softStore.info(that.detailApp.code);
            if(storeData){
              storeData = that.fliterApps([storeData])[0];
            }
            if(storeData){
                that.detailApp = storeData;
                that.detailApp.isLocal = 2;
                if(storeData.type == 0){
                    //检查是否需要升级
                    that.detailApp.needUpdate = await webos.softUser.checkUpdate({code:that.detailApp.code,version:that.detailApp.version});
                }
            }else{
                that.detailApp.isLocal = 1;
            }
            if(that.detailApp.screenShots){
                that.detailApp.screenShotsData = JSON.parse(that.detailApp.screenShots);
            }
        },
        houseInit: async function () {
            var that = this;
            var cats = ["app", "game"];
            for (var i = 0; i < cats.length; i++) {
                var cat = cats[i];
                var data = await webos.softStore.indexList(cat);
                data = that.fliterApps(data);
                if (data) {
                    that.houseData[i].data = data;
                }
            }
        },
        fliterApps:function(data){
          //因官方插件不兼容,屏蔽插件,仅保留轻应用
          if(data && data.length > 0){
            let tmp = [];
            for(let i=0;i<data.length;i++){
               if(data[i].type == 0){
                 continue;
               }
               tmp.push(data[i]);
            }
            data = tmp;
         }
         return data;
        },
        appScoreSubmit: async function () {
            var that = this;
            if (that.detailApp.isLocal != 2) {
                return;
            }
            webos.context.set("showOkErrMsg", true);
            var data = await webos.softStore.rating({code:that.detailApp.code,score:that.detailPf});
            if(data){
                var fields = ["score1","score2","score3","score4","score5","average","ratings"];
                fields.forEach(function (field){
                    that.detailApp[field] = data[field];
                });
            }
        },
        toAppScore: function () {
            var that = this;
            if (that.detailPf <= 2) {
                utils.$.confirm("该软件真的不好用嘛?😭", function (flag) {
                    if (!flag) {
                        return;
                    }
                    that.appScoreSubmit();
                });
            } else {
                that.appScoreSubmit();
            }
        },
        changeSecondCats:function (secondCat){
            var that = this;
            that.appOrGame.param.secondCat = secondCat;
            that.searchAppOrGame(true);
        },
        actionDetailApp:async function (app,type){
            var that = this;
            if(type == 1){
                //安装
                if(app.type == 0 && app.fileId){
                    app.downloadUrl = await webos.softStore.downFile(app.fileId);
                }
                webos.context.set("showOkErrMsg", true);
                var flag = await webos.softUser.install(app);
                if(flag){
                    that.actionDetailApp(app,4);
                    that.selectTypeAction("download");
                    var app = webos.el.findParentComponent(that,"app-component");
                    app.$refs["rm"].init();
                    webos.context.set("openWiths",undefined);
                }
            }else if(type == 2){
                //卸载
                webos.context.set("showOkErrMsg", true);
                var flag = await webos.softUser.uninstall(app.code);
                if(flag){
                    that.selectTypeAction("download");
                    //删除桌面快捷方式
                    var desktop = webos.el.findParentComponent(that,"desktop-component");
                    var path = desktop.currentPath+"/"+app.name+".webosapp";
                    var flag2 = webos.fileSystem.remove({path});
                    if(flag2){
                        desktop.refreshDesktop();
                        var app = webos.el.findParentComponent(that,"app-component");
                        app.$refs["rm"].init();
                        webos.context.set("openWiths",undefined);
                    }
                }
            }else if(type == 3){
                //打开
                var url = "";
                if(app.type == 0){
                    //插件
                    url = "apps/"+app.code+"/index.html";
                }else if(app.type == 1){
                    //轻应用
                    url = app.iframeUrl;
                }
                var desktop = webos.el.findParentComponent(that,"desktop-component");
                desktop.openFile(url,4,app.name,"");
            }else if(type == 4){
                //桌面快捷方式
                var url = "";
                if(app.type == 0){
                    //插件
                    url = "apps/"+app.code+"/index.html";
                }else if(app.type == 1){
                    //轻应用
                    url = app.iframeUrl;
                }
                var appData = {
                    app: "commonApp",
                    icon: app.imgPath,
                    name: app.name,
                    url: url
                };
                var desktop = webos.el.findParentComponent(that,"desktop-component");
                var str = JSON.stringify(appData);
                webos.fileSystem.addUploadFile({
                    file:new Blob([str]),
                    fullPath:"/"+app.name+".webosapp",
                    path:desktop.currentPath,
                    pathName:"本地",
                    sourceName:"桌面",
                    callback:function (res){
                        if(res.status == 2){
                            //刷新桌面
                            desktop.refreshDesktop();
                        }
                    },
                    name:app.name
                });
            }else if(type == 5){
                if(app.type == 0 && app.fileId){
                    app.downloadUrl = await webos.softStore.downFile(app.fileId);
                }
                webos.context.set("showOkErrMsg", true);
                var flag = await webos.softUser.update(app);
                if(flag){
                    that.selectTypeAction("download");
                    var app = webos.el.findParentComponent(that,"app-component");
                    app.$refs["rm"].init();
                    webos.context.set("openWiths",undefined);
                }
            }

        },
        hasMapInit:async function (){
            var that = this;
            var list = await webos.softUser.hasList();
            that.hasMap = {};
            that.hasList = [];
            if(list){
                that.hasList = list;
                list.forEach(function (item){
                    that.hasMap[item.code] = true;
                });
            }
        },
        createLocalApp:function (type){
            var that = this;
            that.addLocalApp.type=type;
            if(type == 1){
                //轻应用
                that.addLocalApp.title="添加轻应用";
                that.addLocalApp.data={};
            }else if(type == 2){
                //本地程序
                that.addLocalApp.title="添加软件";
                that.addLocalApp.data={};
            }
            that.addLocalApp.show = true;
            webos.el.dialogCenter(that.$refs["dialog"]);
        },
        saveLocalApp:async function(){
            var that = this;
            var flag = true;
            webos.context.set("showOkErrMsg", true);
            if(that.addLocalApp.type == 1){
                flag = await webos.softUser.addIframe(that.addLocalApp.data);
            }else if(that.addLocalApp.type == 2){
                var file = that.addLocalApp.data.file[0].raw;
                flag = await webos.softUser.addSoft(file);
            }
            if(flag){
                that.addLocalApp.show = false;
                that.selectTypeAction("download");
            }
        },
        fileSelectHandleExceed:function (files){
            var that = this;
            var refUpload = that.$refs["upload"];
            refUpload.clearFiles();
            refUpload.handleStart(files[0]);
        }
    },
    created: async function () {
        this.hasMapInit();
        this.selectTypeAction("house");
    }
}