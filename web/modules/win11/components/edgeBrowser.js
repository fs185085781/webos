/*edge浏览器组件*/
export default {
    template: `
        <div class="edge-browser-component edgeBrowser floatTab dpShad" style="z-index: -1;">
            <div class="windowScreen flex flex-col">
                <div class="overTool flex">
                    <div class="uicon  ">
                        <img width="14" data-click="false" data-flip="false" data-invert="false" data-rounded="false" src="modules/win11/imgs/icon/edge.png" alt="" style="margin: 0px 6px;">
                    </div>
                </div>
                <el-card class="star-panel" v-if="star.show">
                    <el-form
                            label-position="left"
                            label-width="50px"
                            :model="star.data">
                        <el-form-item label="名称">
                          <el-input v-model="star.data.name" />
                        </el-form-item>
                        <el-form-item label="网址">
                          <el-input v-model="star.data.url" />
                        </el-form-item>
                        <el-form-item>
                          <el-button type="primary" @click="edgeAction(7)">保存</el-button>
                          <el-button @click="star.show = false">取消</el-button>
                        </el-form-item>
                    </el-form>
                </el-card>
                <div class="restWindow flex-grow flex flex-col">
                    <div class="addressBar w-full h-10 flex items-center">
                        <div class="uicon edgenavicon" data-payload="4" @click="edgeAction(1)">
                            <img width="14" data-payload="4" data-click="false" data-flip="false" data-invert="false" data-rounded="false" src="modules/win11/imgs/left.png" alt="" style="margin: 0px 8px;">
                        </div>
                        <div class="uicon edgenavicon" data-payload="5" @click="edgeAction(2)">
                            <img width="14" data-payload="5" data-click="false" data-flip="false" data-invert="false" data-rounded="false" src="modules/win11/imgs/right.png" alt="" style="margin: 0px 8px;">
                        </div>
                        <div class="uicon " data-payload="0" @click="edgeAction(3)">
                            <svg fill="none" style="width: 30px; height: 14px;">
                                <use xlink:href="#refresh"></use>
                            </svg>
                        </div>
                        <div class="uicon " data-payload="1" @click="edgeAction(4)">
                            <svg fill="none" style="width: 30px; height: 14px;">
                                <use xlink:href="#home"></use>
                            </svg>
                        </div>
                        <div class="addCont relative flex items-center">
                            <input class="w-full h-6 px-4" data-payload="3" placeholder="Type url or a query to search" type="text" v-model="url" @keyup.enter="edgeAction(5)">
                            <div class="uicon " data-payload="1" @click="edgeAction(6)">
                                <svg fill="none" style="width: 30px; height: 14px;">
                                    <use xlink:href="#star"></use>
                                </svg>
                            </div>
                            <div class="uicon z-1 handcr" data-payload="2" @click="edgeAction(5)">
                                <img width="14" data-payload="2" data-click="false" data-flip="false" data-invert="false" data-rounded="false" src="modules/win11/imgs/search.png" alt="" style="margin: 0px 10px;">
                            </div>
                        </div>
                    </div>
                    <div class="w-full bookbar py-2 edge-star">
                        <div class="flex">
                            <div v-for="sq in data.starList" class="flex handcr items-center ml-2 mr-1" data-payload="6" >
                                <div class="text-xs" @click="edgeAction(9,sq)">{{sq.name}}</div>
                                <span class="remove" @click="edgeAction(8,sq)">x</span>
                            </div>
                        </div>
                    </div>
                    <div class="siteFrame flex-grow overflow-hidden">
                        <iframe :src="url" style="border:0px;" class="edge-iframe w-full h-full"></iframe>
                    </div>
                </div>
            </div>
        </div>
        `,
    props: [],
    data(){
      return Vue.reactive({
          caches:[],
          index:0,
          star:{

          },
          data:{
              starList:[],
              home:"https://www.bing.com"
          },
          url:"https://www.bing.com",
      })
    },
    methods: {
        edgeAction: function (type, sq) {
            var that = this;
            if (type == 1) {
                //上一页
                if (that.caches.length == 0) {
                    return;
                }
                ;
                if (that.index < 1) {
                    return;
                }
                that.index--;
                that.url = that.caches[that.index];
            } else if (type == 2) {
                //下一页
                if (that.caches.length == 0) {
                    return;
                }
                ;
                if (that.index > that.caches.length - 2) {
                    return;
                }
                ;
                that.index++;
                that.url = that.caches[that.index];
            } else if (type == 3) {
                //刷新
            } else if (type == 4) {
                //首页
                if (that.data && that.data.home) {
                    that.url = that.data.home;
                    that.edgeAction(5);
                }
            } else if (type == 5) {
                //访问
                if (!that.url.toLowerCase().startsWith("http")) {
                    that.url = "https://www.bing.com/search?q=" + encodeURIComponent(that.url);
                }
                if (that.caches.length == 0) {
                    that.caches.push(that.url);
                }
                ;
                if (that.caches[that.caches.length - 1] != that.url) {
                    that.caches.push(that.url);
                }
                ;
                that.index = that.caches.length - 1;
            } else if (type == 6) {
                //收藏
                that.star = {
                    show: true,
                    data: {
                        name: "未填写",
                        url: that.url
                    }
                }
            } else if (type == 7) {
                //保存书签
                var data = that.star.data;
                data.id = utils.uuid();
                that.data.starList.push(data);
                webos.context.set("showOkErrMsg", true);
                var flag = webos.softUserData.save({appCode: "edgeBrowser", data: JSON.stringify(that.data)});
                if (flag) {
                    that.star.show = false;
                }
            } else if (type == 8) {
                //删除书签
                utils.$.confirm("确认删除书签'" + sq.name + "'?", function (flag) {
                    if (!flag) {
                        return;
                    }
                    ;
                    for (var i = 0; i < that.data.starList.length; i++) {
                        var tmp = that.data.starList[i];
                        if (tmp.id == sq.id) {
                            that.data.starList.splice(i, 1);
                            break;
                        }
                    }
                    webos.softUserData.save({appCode: "edgeBrowser", data: JSON.stringify(that.data)});
                });
            } else if (type == 9) {
                //访问书签
                that.url = sq.url;
                that.edgeAction(5);
            }
        }
    },
    created: async function () {
        var that = this;
        that.edgeAction(5);
        var res = await webos.softUserData.get({appCode: "edgeBrowser"});
        if (res) {
            var data = JSON.parse(res);
            if (data) {
                that.data = data;
            }
        }
    }
}