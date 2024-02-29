export default {
    template: `
        <div class="settings-component settingsApp floatTab dpShad">
            <div class="windowScreen flex flex-col" data-dock="true">
              <div class="restWindow flex-grow flex flex-col">
                <nav class="">
                  <div class="nav_top">
                    <div class="account">
                      <img :src="user.imgPath?user.imgPath:'imgs/logo.png'" alt="" class="user-avatar s60">
                      <div>
                        <p>{{user.nickName?user.nickName:'腾飞Webos社区版'}}</p>
                        <p>{{user.username}}</p>
                      </div>
                    </div>
                    <input type="text" class="search" placeholder="查找设置" name="search">
                  </div>
                  <div class="nav_bottom win11Scroll">
                    <div class="navLink " v-for="menu in menus" :class="{'selected':componentData.selected==menu.action}" @click="toSelectAction(menu.action)">
                      <img :src="'modules/win11/imgs/settings/'+menu.icon" alt="" height="16" width="16">{{menu.name}}</div>
                    <div class="marker">
                    </div>
                  </div>
                </nav>
                <template v-for="menu in menus">
                    <main v-if="componentData.selected == menu.action">
                        <h1>
                        <span @click="componentData.selectedSecond=''">{{menu.name}}</span>
                            <template v-if="componentData.selectedSecond != ''">
                            <span>&gt;</span>
                            <span @click="componentData.selectedThird=''">{{componentData.selectedSecondText}}</span>
                            <template v-if="componentData.selectedThird != ''">
                            <span>&gt;</span>
                            <span>{{componentData.selectedThirdText}}</span>
                            </template>
                            </template>
                        </h1>
                        <div class="tilesCont win11Scroll">
                            <component :component-data="componentData" :is="menu.action"></component>
                        </div>
                    </main>
                </template>
                <div class="navMenuBtn">
                  <svg xmlns="http://www.w3.org/2000/svg" fill="currentColor" viewBox="0 0 48 48" width="24" height="24">
                    <path d="M5.5 9a1.5 1.5 0 1 0 0 3h37a1.5 1.5 0 1 0 0-3h-37zm0 13.5a1.5 1.5 0 1 0 0 3h37a1.5 1.5 0 1 0 0-3h-37zm0 13.5a1.5 1.5 0 1 0 0 3h37a1.5 1.5 0 1 0 0-3h-37z">
                    </path>
                  </svg>
                </div>
              </div>
            </div>
        </div>
    `,
    props: ["win"],
    data(){
      return {
          user:{

          },
          componentData:{
              selected:"settings-user",
              selectedSecond:"",
              selectedSecondText:"",
              selectedThird:"",
              selectedThirdText:"",
          },
          menus:[
              {action:"system",name:"系统",icon:"System.webp"},
              {action:"bluetooth",name:"蓝牙和其他设备",icon:"Bluetooth_devices.webp"},
              {action:"network",name:"网络和Internet",icon:"Network_internet.webp"},
              {action:"personal",name:"个性化",icon:"Personalisation.webp"},
              {action:"apps",name:"应用",icon:"Apps.webp"},
              {action:"settings-user",name:"帐户",icon:"Accounts.webp"},
              {action:"language",name:"时间和语言",icon:"Time_language.webp"},
              {action:"gaming",name:"游戏",icon:"Gaming.webp"},
              {action:"accessibility",name:"辅助功能",icon:"Accessibility.webp"},
              {action:"security",name:"隐私和安全性",icon:"Privacy_security.webp"},
              {action:"update",name:"Windows 更新",icon:"Windows_Update.webp"}
          ]
      }
    },
    methods: {
        init:async function (){
            let that = this;
            that.user = await webos.user.info();
        },
        toSelectAction:function (text){
            const that = this;
            that.componentData.selected = text;
            that.componentData.selectedSecond = "";
            that.componentData.selectedSecondText="";
            that.componentData.selectedThird="";
            that.componentData.selectedThirdText="";
        }
    },
    created: function () {
        this.init();

    }
}