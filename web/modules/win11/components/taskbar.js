/**/
const attr = {}
export default {
    template: `
        <div class="taskbar-component taskbar">
            <div :data-hide="!about.show" class="aboutApp floatTab dpShad" :style="{'top':about.top+'px','left':about.left+'px'}">
              <div class="content p-6">
                <div class="text-xl font-semibold">关于</div>
                <p>腾飞webos是一个商业化项目，希望在网络上复制Windows 11的桌面体验。</p>
                <p>此项目仅采用开源项目<a target="_blank" href="https://github.com/blueedgetechno/win11React" rel="noreferrer">win11React</a>的样式</p>
                <p>前后端逻辑和动画由阿范🎈完全从0写的,由前端Vue+后端Java共同实现</p>
                <p class="pl-4">联系QQ :&nbsp;185085781</p>
                <p class="pl-4">联系微信 :&nbsp;O_o-T_T_o_O</p>
                <p>此项目绝不附属于微软，且不应与微软操作系统或产品混淆。</p>
                <p>这也不是&nbsp;<a target="_blank" href="https://www.microsoft.com/en-in/windows-365" rel="noreferrer">Windows 365 cloud PC</a>.</p>
                <p>本项目中微软、Windows和其他示范产品是微软公司组的商标。.</p>
              </div>
              <div class="okbtn px-6 py-4">
                <div data-allow="true" @click="barWidget()">好的，我明白了 </div>
              </div>
            </div>
          <!--任务栏-->
          <div class="taskcont">
            <div class="tasksCont" data-side="center">
              <div class="tsbar">
                <div class="uicon tsIcon widget" @click="barWidget()">
                  <img width="24" data-click="true" src="modules/win11/imgs/widget.png" alt="">
                </div>
                <div ref="homeBtn" class="uicon tsIcon">
                  <div style="width: 24px; height: 24px;" @click="showHideHomePanel(!startMenu.show)">
                    <img width="24" data-click="true" src="modules/win11/imgs/home.png" alt="">
                  </div>
                </div>
                <template v-for="win in wins">
                    <div v-if="!win.close">
                        <div class="uicon tsIcon " :data-active="win.active" data-open="true" @click="showWindow(win)">
                            <div style="width: 24px; height: 24px;">
                              <img width="24" data-click="true" :src="win.data.icon" alt="">
                            </div>
                        </div>
                    </div>
                </template>
              </div>
          </div>
          <div class="taskright">
              <!--wifi,音量,电池-->
              <div ref="sidePanelBtn" class="handcr my-1 px-1 hvlight flex rounded" :class="{'active':sidePane.show}" @click="showSidePane()">
                <div class="uicon taskIcon ">
                    <img width="16" src="modules/win11/imgs/wifi.png" alt="">
                </div>
                <div class="uicon taskIcon ">
                    <img width="16" src="modules/win11/imgs/audio3.png" alt="">
                </div>
                <div class="uicon taskIcon">
                  <span class="battery" :title="'电池状态：'+battery.level+'%'+(battery.charging?'可用(电源已接通)':'剩余')">
                    <div class="uicon btPlug">
                      <i v-if="battery.charging" class="fa fa-bolt"></i>
                    </div>
                    <i class="fa" :class="{
                    'fa-battery-4':battery.level>=90,
                    'fa-battery-3':battery.level>=60&&battery.level<90,
                    'fa-battery-2':battery.level>=40&&battery.level<60,
                    'fa-battery-1':battery.level>=20&&battery.level<40,
                    'fa-battery-0':battery.level<20,
                    }"></i>
                  </span>
                </div>
              </div>
              <!--时间日期-->
              <div ref="calnPaneBtn" class="taskDate m-1 handcr rounded hvlight" style="width:100px;" :class="{'active':calnPane.show}" @click="showCalnPane()">
                <div>{{time.timeStr}}</div>
                <div>{{time.dateStr}}</div>
              </div>
              <!--显示桌面-->
              <div class="uicon graybd my-4 " @click="showDesktop()">
                <img width="6" data-click="true">
              </div>
          </div>
          <div ref="startMenu" class="webos-start-menu startMenu dpShad" data-hide="false" data-align="center"
           style="--prefix:START;height:0px;">
             <div class="stmenu">
               <div class="menuUp">
                 <div class="pinnedApps">
                   <div class="stAcbar">
                     <div class="gpname">已固定</div>
                   <div class="gpbtn prtclk" data-action="STARTALL">
                     <div>所有应用</div>
                       <div class="uicon prtclk ">
                         <svg aria-hidden="true" focusable="false" data-prefix="fas" data-icon="chevron-right" class="svg-inline--fa fa-chevron-right " role="img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 320 512" data-flip="false" data-invert="false" data-rounded="false" style="width: 8px; height: 8px;"><path fill="currentColor" d="M96 480c-8.188 0-16.38-3.125-22.62-9.375c-12.5-12.5-12.5-32.75 0-45.25L242.8 256L73.38 86.63c-12.5-12.5-12.5-32.75 0-45.25s32.75-12.5 45.25 0l192 192c12.5 12.5 12.5 32.75 0 45.25l-192 192C112.4 476.9 104.2 480 96 480z"></path></svg>
                       </div>
                     </div>
                   </div>
                   <div class="pnApps">
                     <div v-for="item in pinneds" class="prtclk pnApp" @click="openApp(item)">
                       <div class="uicon pnIcon ">
                         <img width="32" :src="item.icon?item.icon:('modules/win11/imgs/icon/'+item.app+'.png')" alt="">
                       </div>
                       <div class="appName">{{item.name}}</div>
                     </div>
                   </div>
                 </div>
                 <div class="recApps win11Scroll">
                   <div class="stAcbar">
                     <div class="gpname">推荐的项目</div>
                     <div class="gpbtn">
                       <div>更多</div>
                       <div class="uicon prtclk ">
                         <svg aria-hidden="true" focusable="false" data-prefix="fas" data-icon="chevron-right" class="svg-inline--fa fa-chevron-right " role="img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 320 512" data-flip="false" data-invert="false" data-rounded="false" style="width: 8px; height: 8px;"><path fill="currentColor" d="M96 480c-8.188 0-16.38-3.125-22.62-9.375c-12.5-12.5-12.5-32.75 0-45.25L242.8 256L73.38 86.63c-12.5-12.5-12.5-32.75 0-45.25s32.75-12.5 45.25 0l192 192c12.5 12.5 12.5 32.75 0 45.25l-192 192C112.4 476.9 104.2 480 96 480z"></path></svg>
                       </div>
                     </div>
                   </div>
                   <div class="reApps">
                     <div v-for="item in pinneds" class="rnApp" @click="openApp(item)">
                       <div class="uicon pnIcon ">
                         <img width="32" :src="item.icon?item.icon:('modules/win11/imgs/icon/'+item.app+'.png')" alt="">
                       </div>
                       <div class="acInfo">
                         <div class="appName">{{item.name}}</div>
                         <div class="timeUsed"></div>
                       </div>
                     </div>
                   </div>
                 </div>
               </div>
             </div>
             <div class="menuBar">
               <div class="profile handcr" @click="toUserInfo()">
                 <div class="uicon">
                   <img :src="user.imgPath?user.imgPath:'imgs/logo.png'" alt="" class="user-avatar s26">
                 </div>
                 <div class="usName">{{user.nickName?user.nickName:'腾飞WebOS'}}</div>
               </div>
               <div class="relative powerMenu" @click="startMenu.shutDownShow = !startMenu.shutDownShow">
                 <div class="powerCont" :data-vis="startMenu.shutDownShow">
                   <div class="flex prtclk" @click="shutDownAction(1)">
                     <div class="uicon prtclk ">
                       <i class="fa fa-power-off" aria-hidden="true" style="font-size:19px;margin-top: auto;"></i>
                     </div>
                     <span>关机</span>
                   </div>
                   <div class="flex prtclk" @click="shutDownAction(2)">
                     <div class="uicon prtclk ">
                       <i class="fa fa-repeat" aria-hidden="true" style="font-size:19px;margin-top: auto;transform: rotateY(180deg);"></i>
                     </div>
                     <span>重启</span>
                   </div>
                   <div class="flex prtclk" @click="shutDownAction(3)">
                     <div class="uicon prtclk ">
                        <i class="fa fa-lock" aria-hidden="true" style="font-size:19px;margin-top: auto;"></i>
                     </div>
                     <span>锁定</span>
                   </div>
                 </div>
                 <div class="uicon prtclk ">
                    <i class="fa fa-power-off" aria-hidden="true" style="font-size:19px;margin-top: auto;"></i>
                 </div>
               </div>
             </div>
           </div>
           <div ref="sidePane" class="sidePane dpShad" :data-hide="!sidePane.show" style="--prefix:PANE;">
             <div class="quickSettings p-5 pb-8">
               <div class="qkCont">
                 <div class="qkGrp">
                   <div class="qkbtn handcr prtclk" :data-state="settings.wifi" @click="switchAction('wifi')">
                     <div class="uicon quickIcon ">
                       <img width="14" :data-invert="settings.wifi" src="modules/win11/imgs/wifi.png" alt="">
                     </div>
                   </div>
                   <div class="qktext">WLAN</div>
                 </div>
                 <div class="qkGrp">
                   <div class="qkbtn handcr prtclk" :data-state="settings.bluetooth" @click="switchAction('bluetooth')">
                     <div class="uicon quickIcon ">
                       <img width="14" :data-invert="settings.bluetooth" src="modules/win11/imgs/bluetooth.png" alt="">
                     </div>
                   </div>
                   <div class="qktext">蓝牙</div>
                 </div>
                 <div class="qkGrp">
                   <div class="qkbtn handcr prtclk" :data-state="settings.airplane" @click="switchAction('airplane')">
                   <div class="uicon quickIcon ">
                     <img width="14" :data-invert="settings.airplane" src="modules/win11/imgs/airplane.png" alt=""></div>
                   </div>
                   <div class="qktext">飞行模式</div>
                 </div>
                 <div class="qkGrp">
                   <div class="qkbtn handcr prtclk" :data-state="settings.saver" @click="switchAction('saver')">
                     <div class="uicon quickIcon ">
                       <img width="14" :data-invert="settings.saver" src="modules/win11/imgs/saver.png" alt="">
                     </div>
                   </div>
                   <div class="qktext">节能模式</div>
                 </div>
                 <div class="qkGrp">
                   <div class="qkbtn handcr prtclk" :data-state="settings.theme == 'dark'" @click="switchAction('theme')">
                     <div class="uicon quickIcon ">
                       <img width="14" :data-invert="settings.theme == 'dark'" :src="'modules/win11/imgs/'+(settings.theme == 'dark'?'moon':'sun')+'.png'" alt="">
                     </div>
                   </div>
                   <div class="qktext">主题</div>
                 </div>
                 <div class="qkGrp">
                   <div class="qkbtn handcr prtclk" :data-state="settings.nightSun" @click="switchAction('nightSun')">
                     <div class="uicon quickIcon ">
                       <img width="14" :data-invert="settings.nightSun" src="modules/win11/imgs/nightlight.png" alt="">
                     </div>
                   </div>
                   <div class="qktext">护眼模式</div>
                 </div>
               </div>
               <div class="sliderCont">
                 <div class="uicon mx-2 ">
                   <img width="20" data-invert="false" src="modules/win11/imgs/sun.png" alt="">
                 </div>
                 <input class="sliders bSlider" type="range" min="10" max="100" v-model="settings.lightValue" @input="lightValueChange(false)" @change="lightValueChange(true)" 
                 :style="{'--track-color':'linear-gradient(90deg, #0067c0 '+(settings.lightValue-3)+'%, #888888 '+settings.lightValue+'%)'}">
               </div>
               <div class="sliderCont">
                 <div class="uicon mx-2 ">
                   <img width="18" data-invert="false" src="modules/win11/imgs/audio3.png" alt="">
                 </div>
                 <input class="sliders vSlider" type="range" min="0" max="100" value="100" v-model="settings.volume" @input="volumeChange(false)" @change="volumeChange(true)"
                 :style="{'--track-color':'linear-gradient(90deg, #0067c0 '+(settings.volume-3)+'%, #888888 '+settings.volume+'%)'}">
               </div>
             </div>
             <div class="p-1 bottomBar">
               <div class="px-3 bettery">
                 <div class="uicon taskIcon">
                   <span class="battery">
                     <div class="uicon prtclk btPlug">
                       <i v-if="battery.charging" class="fa fa-bolt"></i>
                     </div>
                     <i class="fa" :class="{
                    'fa-battery-4':battery.level>=90,
                    'fa-battery-3':battery.level>=60&&battery.level<90,
                    'fa-battery-2':battery.level>=40&&battery.level<60,
                    'fa-battery-1':battery.level>=20&&battery.level<40,
                    'fa-battery-0':battery.level<20,
                    }"></i>
                   </span>
                 </div>
                 <div class="text-xs">{{battery.level}}%</div>
               </div>
             </div>
           </div>
           <div ref="calnPane" class="calnpane dpShad" :data-hide="!calnPane.show" style="--prefix:CALN;">
             <div class="topBar pl-4 text-sm">
               <div class="date">
                 <p>{{calnPane.p1}}</p>
                 <p>{{calnPane.p2}}</p>
               </div>
               <div>
                <iframe ref="tianqi" allowtransparency="true" scrolling="no"></iframe>
               </div>
             </div>
             <div>
               <el-calendar v-model="calnPane.val">
                 <template #header="{ date }">
                     <span @click="$refs.monthSelect.focus()" class="title">{{date}}</span>
                     <el-date-picker
                        ref="monthSelect"
                        v-model="calnPane.val"
                        type="month"
                        :teleported="false"
                        :prefix-icon="1"
                      ></el-date-picker>
                 </template>
                 <template #dateCell="{ data }">
                  <div>
                    <p>{{data.day.split('-')[2]}}</p>
                    <p>{{getLunar(data.date)}}</p>
                  </div>
                 </template>
               </el-calendar>
             </div>
           </div>
      </div>
    </div>
    `,
    props: ['wins'],
    data(){
      return {
          user:{},
          getLunar:function (date){
              var res = webos.util.solar2lunar(date);
              return res.dayCn;
          },
          pinneds:[],
          time:{
              dateStr:"",
              timeStr:"",
          },
          battery:{
              charging:false,
              level:100
          },
          startMenu:{
              show:false,
              shutDownShow:false
          },
          sidePane:{
              show:false
          },
          settings:{
              lightValue:100,
              theme:"light",
              wifi:true,
              bluetooth:false,
              airplane:false,
              saver:false,
              nightSun:false,
              volume:100
          },
          calnPane:{
              show:false,
              val:new Date(),
              p1:"",
              p2:""
          },
          about:{
              show:false,
              top:0,
              left:0
          }
      }
    },
    methods: {
        taskBarEleChange:function (e){
            var target = e.target;
            this.startMenuChange(target);
            this.sidePanelChange(target);
            this.calnPanelChange(target);
        },
        hasApp:async function (app){
            if(!webos.context.get("hasInstall"+app)){
                webos.context.set("hasInstall"+app,await webos.softUser.hasInstall(app));
            }
            return webos.context.get("hasInstall"+app);
        },
        init:async function (){
            var that = this;
            that.user = await webos.user.info();
            if(!that.user){
                that.user = {};
            }
            that.pinneds = [];
            that.pinneds.push({"app":"settings","name":"设置","type":3});
            that.pinneds.push({"app":"fileExplorer","name":"文件管理器","icon":"modules/win11/imgs/icon/explorer.png","type":3});
            that.pinneds.push({"app":"store","name":"应用商店","type":3});
            if(await that.hasApp("webssh")){
                that.pinneds.push({"app":"apps/webssh/index.html","name":"终端","type":4,"icon":"apps/webssh/icon.png"});
            }
            that.timeAndBettery();
            await that.initSettings();
        },
        timeAndBettery:function (){
            var that = this;
            if(attr.hasInit){
                return;
            }
            attr.hasInit = true;
            var count = 0;
            setInterval(function (){
                if(count%2==0){
                    if(navigator.getBattery){
                        navigator.getBattery().then(function(res){
                            that.battery = {
                                charging:res.charging,
                                level:Math.floor(res.level*100)
                            }
                        });
                    }
                };
                count++;
                if(count>10000){
                    count = 0;
                };
                var date = new Date();
                that.time = {
                    dateStr:date.format("yyyy/MM/dd"),
                    timeStr:date.format("dddddHH:mm:ss"),
                };
            },500);
            setInterval(function (){
                that.calnPane.val = new Date();
                that.setTianQiColor();
            },300*1000);
        },
        showDesktop:function (){
            console.log("展示桌面")
        },
        barWidget:function (){
            var that = this;
            that.about.top = -(176+window.innerHeight/2);
            that.about.left =(window.innerWidth - 448)/2;
            that.about.show = !that.about.show;
        },
        showWindow:function (win){
            var that = this;
            var desktop = webos.el.findParentComponent(that,"desktop-component");
            var list = desktop.$refs['wins_dialog'];
            for(var i=0;i<list.length;i++){
                var one = list[i];
                if(win.id == one.win.id){
                    if(win.active || one.win.winNowType == 3){
                        one.windowAction(1);
                    }
                    break;
                }
            }
            if(!win.active){
                desktop.topWindow({target:document.querySelector("#"+win.id+"header")});
            }
        },
        startMenuChange:function (target){
            let that = this;
            if(!that.startMenu.show){
                return;
            }
            var ele = that&&that.$refs&&that.$refs.startMenu;
            if(!ele){
                return;
            };
            if(!webos.el.isChildren(ele,target) && !webos.el.isChildren(that.$refs.homeBtn,target)){
                that.showHideHomePanel(false);
            }
        },
        sidePanelChange:function (target){
            let that = this;
            if(!that.sidePane.show){
                return;
            }
            var ele = that&&that.$refs&&that.$refs.sidePane;
            if(!ele){
                return;
            };
            if(!webos.el.isChildren(ele,target) && !webos.el.isChildren(that.$refs.sidePanelBtn,target)){
                that.sidePane.show = false;
            }
        },
        calnPanelChange:function (target){
            let that = this;
            if(!that.calnPane.show){
                return;
            }
            var ele = that&&that.$refs&&that.$refs.calnPane;
            if(!ele){
                return;
            };
            if(!webos.el.isChildren(ele,target) && !webos.el.isChildren(that.$refs.calnPaneBtn,target)){
                that.calnPane.show = false;
            }
        },
        shutDownAction:function (type){
            var that = this;
            if(type == 3){
                webos.user.userLock();//主动锁定
            }else{
                webos.user.logOut();
                var app = webos.el.findParentComponent(that,"app-component");
                app.checkLogin();
            }
        },
        openApp:function (item){
            var that = this;
            that.showHideHomePanel(false);
            var desktop = webos.el.findParentComponent(that,"desktop-component");
            desktop.openFile(item.app,item.type,item.name,item.icon?item.icon:"");
        },
        showHideHomePanel:function (flag){
            var that = this;
            if(!flag){
                that.startMenu.shutDownShow = false;
            }
            var ele = that&&that.$refs&&that.$refs.startMenu;
            if(!ele){
                return;
            };
            that.startMenu.show = flag;
            var oldData = {bottom:"60px"};
            var newData = {bottom:"-700px"};
            if(that.startMenu.show){
                ele.style.height = "min(100vh - 24px,720px)";
                oldData = {bottom:"-700px"};
                newData = {bottom:"60px"};
            }
            var aid = webos.el.animationCssObj(oldData,newData,"taskbar-home-css");
            ele.style.animation = aid+" 0.3s";
            ele.style["animation-fill-mode"] = "forwards";
            setTimeout(function (){
                if(!that.startMenu.show){
                    ele.style.height = "0px";
                }
            },200);
        },
        lightValueChange:function (needSave){
            let that = this;
            document.body.style.opacity = that.settings.lightValue/100;
            document.body.style.background = "#000000";
            if(needSave){
                that.saveSettings();
            }
        },
        volumeChange:function (needSave){
            let that = this;
            if(needSave){
                that.saveSettings();
            }
        },
        showSidePane:function (){
            let that = this;
            that.sidePane.show = !that.sidePane.show;
        },
        showCalnPane:function (){
            let that = this;
            that.calnPane.show = !that.calnPane.show;
            if(that.calnPane.show){
                var date = new Date();
                that.calnPane.p1 = date.format("MM月d日dddd");
                var res = webos.util.solar2lunar(date);
                that.calnPane.p2 = res.monthCn + res.dayCn;
            }
        },
        setTianQiColor:function (theme){
            var that = this;
            if(!theme){
                theme = webos.util.getCacheTheme();
            }
            var color = theme == "dark"?"fff":"000";
            that.$refs["tianqi"].src = "https://tianqiapi.com/api.php?style=tf&skin=pitaya&color="+color;
        },
        switchAction:function (field){
            let that = this;
            if(field == "theme"){
                that.settings[field] = that.settings[field] == "dark"?"light":"dark";
            }else{
                that.settings[field] = !that.settings[field];
            }
            that.saveSettings();
            if(field == "theme"){
                document.body.dataset.theme = that.settings.theme;
                document.querySelector("html").className = that.settings.theme;
                webos.util.setCacheTheme(that.settings.theme);
                that.setTianQiColor(that.settings.theme);
            }
            if(field == "nightSun"){
                document.body.dataset.sepia = that.settings.nightSun;
            }
        },
        initSettings:async function (){
            let that = this;
            var config = await webos.softUserData.syncObject("settings_taskbar");
            if(config.theme === undefined){
                config = {
                    lightValue:100,
                    theme:"light",
                    wifi:true,
                    bluetooth:false,
                    airplane:false,
                    saver:false,
                    nightSun:false,
                    volume:100
                }
            }
            that.settings = config;
            document.body.dataset.theme = that.settings.theme;
            document.body.dataset.sepia = that.settings.nightSun;
            document.querySelector("html").className = that.settings.theme;
            that.setTianQiColor(that.settings.theme);
            webos.util.setCacheTheme(that.settings.theme);
            if(that.settings.lightValue != 100){
                that.lightValueChange(false);
            }
            that.calnPane.val = new Date();
            that.time = {
                dateStr:that.calnPane.val.format("yyyy/MM/dd"),
                timeStr:that.calnPane.val.format("dddddHH:mm:ss"),
            };
        },
        saveSettings:function (){
            webos.softUserData.syncObject("settings_taskbar",this.settings);
        },
        setTheme:function (theme){
            const that = this;
            if(that.settings.theme == theme){
                return;
            }
            that.switchAction("theme");
        },
        toUserInfo:async function (){
            const that = this;
            var desktop = webos.el.findParentComponent(that,"desktop-component");
            var actionWin = await desktop.openFile("settings",3,"设置","");
            utils.delayAction(function(){
                return desktop.getWinComById(actionWin.id);
            },function (){
                var winCom = desktop.getWinComById(actionWin.id);
                utils.delayAction(function(){
                    return winCom.$refs&&winCom.$refs.appComponent;
                },function (){
                    winCom.$refs.appComponent.toSelectAction("settings-user");
                    that.showHideHomePanel(false);
                },6000);
            },6000);
        }
    },
    created: async function () {
        await this.init();
    }
}