export default {
    template: `
        <div class="persomal-component">
          <template v-if="componentData.selectedSecond == ''">
            <div class="personaliseTop">
              <img v-if="wallpaperData.type == 'img'" class="mainImg" :src="pagerDataUrl" alt="">
              <video v-if="wallpaperData.type == 'video'" :src="pagerDataUrl" autoplay muted loop style="height:200px;"></video>
            </div>
            <div class="personaliseTop">
              <el-button style="padding:0px 5px;" @click="selectImgData('img')">本地图像</el-button>
              <el-button style="padding:0px 5px;" @click="selectImgData('video')">本地视频</el-button>
            </div>
            <div class="personaliseTop">
              <div>
                <h3>请选择一个主题</h3>
                <div class="bgBox">
                  <div v-for="(item,index) in list" class="imageCont prtcl" :class="{'selected':index == themeData.index}" @click="selectItem(index)">
                    <img :src="item.img" alt="">
                  </div>
                </div>
              </div>
            </div>
            <div class="personaliseTop" v-if="hasXiaoNiaoBiZhi">
              <el-button style="padding:0px 5px;" @click="selectMore()">更多壁纸</el-button>
            </div>
          </template>
        </div>
    `,
    props: ['componentData'],
    data(){
      return {
          list:[
              {theme:"light",img:"modules/win11/imgs/theme/light1.jpg"},
              {theme:"light",img:"modules/win11/imgs/theme/light2.jpg"},
              {theme:"light",img:"modules/win11/imgs/theme/light3.jpg"},
              {theme:"dark",img:"modules/win11/imgs/theme/dark1.jpg"},
              {theme:"dark",img:"modules/win11/imgs/theme/dark2.jpg"},
              {theme:"dark",img:"modules/win11/imgs/theme/dark3.jpg"}
          ],
          themeData:{},
          wallpaperData:{},
          pagerDataUrl:"",
          hasXiaoNiaoBiZhi:false
      }
    },
    methods: {
        initData:async function (){
            const that = this;
            let data = await webos.softUserData.syncObject("themeWin11Data");
            if(data.index == undefined){
                data = {
                    index:0
                }
            }
            that.themeData = data;
            var app = webos.el.findParentComponent(that,"app-component");
            that.wallpaperData = app.getWallpaper();
            that.pagerDataUrl = await webos.util.url2blobUrl(that.wallpaperData.wallpaper);
            await that.initMoreSelect();
        },
        initMoreSelect:async function (){
            const that = this;
            if(that.hasXiaoNiaoBiZhi){
                return;
            }
            var app = "birdpaper";
            if(!webos.context.get("hasInstall"+app)){
                webos.context.set("hasInstall"+app,await webos.softUser.hasInstall(app));
            }
            if(!webos.context.get("hasInstall"+app)){
                return;
            }
            that.hasXiaoNiaoBiZhi = true;
        },
        selectItem:async function (index){
            const that = this;
            that.themeData.index = index;
            await webos.softUserData.syncObject("themeWin11Data",that.themeData);
            let item = that.list[index];
            var desktop = webos.el.findParentComponent(that,"desktop-component");
            desktop.$refs['taskbar'].setTheme(item.theme);
            var app = webos.el.findParentComponent(that,"app-component");
            that.wallpaperData.wallpaper = item.img;
            that.wallpaperData.type = "img";
            that.pagerDataUrl = await webos.util.url2blobUrl(that.wallpaperData.wallpaper);
            await app.changeWallpaper(item.img,"img");
        },
        selectImgData:async function (type){
            var that = this;
            var accept = type == "video"?"mp4,mkv":"jpg,jpeg,png,gif";
            var desktop = webos.el.findParentComponent(that,"desktop-component");
            await desktop.selectFileAction(accept,false,async function (sz){
                if(sz.length != 1){
                    webos.message.error("请选择一个"+(type == "video"?"视频":"图片")+"后重试");
                    return false;
                }
                var zl = await webos.fileSystem.zl(sz[0].path);
                var app = webos.el.findParentComponent(that,"app-component");
                await app.changeWallpaper(zl,type);
                that.pagerDataUrl = await webos.util.url2blobUrl(zl);
                return true;
            });
        },
        selectMore:async function (){
            const that = this;
            var url = "apps/birdpaper/index.html";
            var desktop = webos.el.findParentComponent(that,"desktop-component");
            await desktop.openFile(url,4,"小鸟壁纸","");
        }
    },
    created: async function () {
        await this.initData();
    }
}