/**/
let attr = {}
export default {
    template: `
        <div class="window-component" :class="{
        'full':win.winNowType == 2,
        'simple-window':win.isSimple
        }" :style="{
        'display':win.width==0?'none':''
        }">
            <el-dialog v-model="win.show" :modal="false" :draggable="false"
                       :close-on-click-modal="false" :close-on-press-escape="false" :destroy-on-close="win.close"
            >
                <template #header>
                  <div style="font-size:14px;" :id="win.id+'header'" class="win-header" @mousedown="winSizeChangeDbl($event)">
                      <img width="14" :src="win.data.icon" alt="" style="vertical-align: middle;">
                      <span class="win-title">{{win.data.name}}</span>
                  </div>   
                    <div v-if="!win.hideMin" :class="{'disabled':win.disableMin}" class="snapbox h-full" style="right: 96px;" @click="windowAction(1)">
                        <div class="uicon win-right-btn" data-pr="true">
                            <img width="12" src="modules/win11/imgs/minimize.png" alt="">
                        </div>
                    </div>
                    <div v-if="!win.hideMax" :class="{'disabled':win.disableMax}" class="snapbox h-full win-layout" style="right: 48px;" @click="windowAction(win.winNowType==2?3:2)">
                        <div class="uicon win-right-btn" data-pr="true">
                            <img width="12" :src="'modules/win11/imgs/'+(win.winNowType==2?'maximize':'maxmin')+'.png'" alt="">
                        </div>
                        <div class="snapcont mdShad" @click="windowLayout($event)" v-if="!win.disableMax">
                          <div class="snapLay">
                            <div class="snapper" data-size="0,0,0.5,1" style="border-radius: 4px 0px 0px 4px;"></div>
                            <div class="snapper" data-size="0.5,0,0.5,1" style="border-radius: 0px 4px 4px 0px;"></div>
                          </div>
                          <div class="snapLay">
                            <div class="snapper" data-size="0,0,0.6666,1" style="border-radius: 4px 0px 0px 4px;"></div>
                            <div class="snapper" data-size="0.6666,0,0.3334,1" style="border-radius: 0px 4px 4px 0px;"></div>
                          </div>
                          <div class="snapLay">
                            <div class="snapper" data-size="0,0,0.3333,1" style="border-radius: 4px 0px 0px 4px;"></div>
                            <div class="snapper" data-size="0.3333,0,0.3333,1" style="border-radius: 0px;"></div>
                            <div class="snapper" data-size="0.6666,0,0.3334,1" style="border-radius: 0px 4px 4px 0px;"></div>
                          </div>
                          <div class="snapLay">
                            <div class="snapper" data-size="0,0,0.5,1" style="border-radius: 4px 0px 0px 4px;"></div>
                            <div class="snapper" data-size="0.5,0,0.5,0.5" style="border-radius: 0px 4px 0px 0px;"></div>
                            <div class="snapper" data-size="0.5,0.5,0.5,0.5" style="border-radius: 0px 0px 4px;"></div>
                          </div>
                          <div class="snapLay">
                            <div class="snapper" data-size="0,0,0.5,0.5" style="border-radius: 4px 0px 0px;"></div>
                            <div class="snapper" data-size="0.5,0,0.5,0.5" style="border-radius: 0px 4px 0px 0px;"></div>
                            <div class="snapper" data-size="0,0.5,0.5,0.5" style="border-radius: 0px 0px 0px 4px;"></div>
                            <div class="snapper" data-size="0.5,0.5,0.5,0.5" style="border-radius: 0px 0px 4px;"></div>
                          </div>
                          <div class="snapLay">
                            <div class="snapper" data-size="0,0,0.3,1" style="border-radius: 4px 0px 0px 4px;"></div>
                            <div class="snapper" data-size="0.3,0,0.4,1" style="border-radius: 0px;"></div>
                            <div class="snapper" data-size="0.7,0,0.3,1" style="border-radius: 0px 4px 4px 0px;"></div>
                          </div>
                        </div>
                    </div>
                    <div v-if="!win.hideClose" :class="{'disabled':win.disableClose}" class="snapbox h-full" style="right: 0px;" @click="windowAction(4)">
                        <div class="uicon win-right-btn" data-pr="true">
                            <img width="12" src="modules/win11/imgs/close.png" alt="">
                        </div>
                    </div>
                </template>
                <div style="overflow-y:auto" @mousedown="topWindow($event)">
                    <div v-if="!win.hideSize">
                        <div class="resizecont topone">
                            <div class="flex">
                                <div class="conrsz cursor-nw" data-op="1" data-vec="-1,-1" @mousedown="windowSizeChangeStart($event,'left_top')"></div>
                                <div class="edgrsz cursor-n wdws" data-op="1" data-vec="-1,0" @mousedown="windowSizeChangeStart($event,'top')"></div>
                            </div>
                        </div>
                        <div class="resizecont leftone">
                            <div class="h-full">
                                <div class="edgrsz cursor-w hdws" data-op="1" data-vec="0,-1" @mousedown="windowSizeChangeStart($event,'left')"></div>
                            </div>
                        </div>
                        <div class="resizecont rightone">
                            <div class="h-full">
                                <div class="edgrsz cursor-w hdws" data-op="1" data-vec="0,1" @mousedown="windowSizeChangeStart($event,'right')"></div>
                            </div>
                        </div>
                        <div class="resizecont bottomone">
                            <div class="flex">
                                <div class="conrsz cursor-ne" data-op="1" data-vec="1,-1" @mousedown="windowSizeChangeStart($event,'left_bottom')"></div>
                                <div class="edgrsz cursor-n wdws" data-op="1" data-vec="1,0" @mousedown="windowSizeChangeStart($event,'bottom')"></div>
                                <div class="conrsz cursor-nw" data-op="1" data-vec="1,1" @mousedown="windowSizeChangeStart($event,'right_bottom')"></div>
                            </div>
                        </div>
                    </div>
                    <div :id="win.id+'body'">
                        <div class="body-modal" style="display: none;"></div>
                        <component ref="appComponent" :is="win.data.app" :win="win"></component>
                    </div>
                </div>
            </el-dialog>
        </div>
    `,
    props: ['win'],
    data(){
        return {
            lastStatus:{},
        }
    },
    methods: {
        windowAction:function (type,moveData){
            var that =this;
            //winType 1正常 2最大化 3最小化 12  21 13  31  23  32
            if(type==1 || type == 2 || type == 3){
                //最小化切换23 32  13  31  正常到全屏12  全屏到正常21
                if(type == 1){
                    var tmp = that.win.winLastType;
                    that.win.winLastType = that.win.winNowType;
                    if(that.win.winLastType != 3){
                        tmp = 3;
                    };
                    that.win.winNowType = tmp;
                    if(that.win.winNowType == 3){
                        //正常或最大化  转变成 最小化,需要记录窗口情况
                        that.win.lastStatus1 = {
                            height:that.win.height,
                            width:that.win.width,
                            left:that.win.left,
                            top:that.win.top
                        };
                    }
                }else{
                    that.win.winLastType = type == 2?1:2;
                    that.win.winNowType = type == 2?2:1;
                    if(type == 2){
                        that.win.lastStatus2 = {
                            height:that.win.height,
                            width:that.win.width,
                            left:that.win.left,
                            top:that.win.top
                        };
                    }
                }
                var oldData = {
                    height:that.win.height,
                    width:that.win.width,
                    left:that.win.left,
                    top:that.win.top
                };
                var newData;
                if(that.win.winNowType == 1){
                    //正常
                    if(that.win.winLastType == 2){
                        //全屏到正常
                        newData = JSON.parse(JSON.stringify(that.win.lastStatus2));
                    }else if(that.win.winLastType == 3){
                        //最小化到正常
                        newData = JSON.parse(JSON.stringify(that.win.lastStatus1));
                    }
                }else if(that.win.winNowType == 2){
                    //最大化
                    newData = {
                        height:document.body.clientHeight-48,
                        width:document.body.clientWidth,
                        left:0,
                        top:0
                    };
                }else if(that.win.winNowType == 3){
                    //最小化
                    newData = {
                        left:document.body.clientWidth/2,
                        top:document.body.clientHeight,
                        height:1,
                        width:1
                    };
                };
                if(moveData && moveData.left){
                    newData.left = moveData.left;
                    newData.top = moveData.top;
                };
                var animation = true;
                var set = true;
                if(moveData){
                    animation = false;
                    set = moveData.set;
                }
                that.animationWindow(oldData,newData,animation,set);
            }else if(type == 4){
                //关闭
                that.win.show = false;
                that.win.close = true;
            }
        },
        animationWindow:function (oldData,newData,animation,set){
            var that = this;
            var target = document.querySelector("#"+that.win.id+"header").parentElement.parentElement;
            var aid = webos.el.animationCss(oldData,newData);
            if(set){
                target.style.left = newData.left+"px";
                target.style.top = newData.top+"px";
                target.style.height = newData.height+"px";
                target.style.width = newData.width+"px";
                that.win.height = newData.height;
                that.win.width = newData.width;
                that.win.left = newData.left;
                that.win.top = newData.top;
            };
            if(animation){
                target.style.animation = aid+" 0.3s";
                target.style["animation-fill-mode"] = "none";
            };
        },
        windowSizeChangeStart:function (e,type){
            var that = this;
            var iframe = document.querySelector("#"+that.win.id+"body iframe");
            if(iframe){
                document.querySelector("#"+that.win.id+"body .body-modal").style.display = "";
            }
            //窗口修改大小开始
            if(!attr.winLastStatus){
                attr.winLastStatus = {};
            };
            attr.winLastStatus.clientX = e.clientX;
            attr.winLastStatus.clientY = e.clientY;
            attr.winLastStatus.that = that;
            attr.winLastStatus.type = type;
        },
        moveAction:function (e){
            if(attr.winLastStatus && attr.winLastStatus.that){
                //处理窗口大小变化
                var type = attr.winLastStatus.type;
                var x = e.clientX-attr.winLastStatus.clientX;
                var y = e.clientY-attr.winLastStatus.clientY;
                var target = document.querySelector("#"+attr.winLastStatus.that.win.id+"header").parentElement.parentElement;
                if(type.includes("left")){
                    x = -x;
                    var left = attr.winLastStatus.that.win.left - x;
                    target.style.left = left+"px";
                }
                if(type.includes("top")){
                    y = -y;
                    var top = attr.winLastStatus.that.win.top - y;
                    target.style.top = top+"px";
                }
                if(type.includes("left") || type.includes("right")){
                    var width = attr.winLastStatus.that.win.width + x;
                    target.style.width = width+"px";
                }
                if(type.includes("top") || type.includes("bottom")){
                    var height = attr.winLastStatus.that.win.height + y;
                    target.style.height = height+"px";
                }
            }
            if(attr.winMoveStatus && attr.winMoveStatus.that){
                var target = document.querySelector("#"+attr.winMoveStatus.that.win.id+"header").parentElement.parentElement;
                if(attr.winMoveStatus.that.win.winNowType == 2 && !attr.winMoveStatus.hasActionZdh){
                    attr.winMoveStatus.that.windowAction(3,{left:e.clientX - attr.winMoveStatus.that.win.lastStatus2.width/2,top:e.clientY - 24,set:true});
                    attr.winMoveStatus.hasActionZdh = true;
                };
                var x = e.clientX-attr.winMoveStatus.clientX;
                var y = e.clientY-attr.winMoveStatus.clientY;
                var top = attr.winMoveStatus.that.win.top + y;
                var left = attr.winMoveStatus.that.win.left + x;
                top<0?top=0:top>document.body.clientHeight-88?top=document.body.clientHeight-88:"";
                left<154-attr.winMoveStatus.that.win.width?left=154-attr.winMoveStatus.that.win.width:left>document.body.clientWidth-100?left=document.body.clientWidth-100:"";
                target.style.left = left+"px";
                target.style.top = top+"px";
            }
        },
        windowSizeChangeEnd:function (e){
            if(attr.winLastStatus && attr.winLastStatus.that){
                var that = attr.winLastStatus.that;
                var target = document.querySelector("#"+attr.winLastStatus.that.win.id+"header").parentElement.parentElement;
                attr.winLastStatus.that.win.left = target.offsetLeft;
                attr.winLastStatus.that.win.top = target.offsetTop;
                attr.winLastStatus.that.win.width = target.offsetWidth;
                attr.winLastStatus.that.win.height = target.offsetHeight;
                attr.winLastStatus = {};
                var iframe = document.querySelector("#"+that.win.id+"body iframe");
                if(iframe){
                    document.querySelector("#"+that.win.id+"body .body-modal").style.display = "none";
                }
            }
        },
        windowMoveStart:function (e){
            var that = this;
            //窗口移动开始
            if(!attr.winMoveStatus){
                attr.winMoveStatus = {};
            };
            attr.winMoveStatus.clientX = e.clientX;
            attr.winMoveStatus.clientY = e.clientY;
            attr.winMoveStatus.that = that;
            var iframe = document.querySelector("#"+that.win.id+"body iframe");
            if(iframe){
                document.querySelector("#"+that.win.id+"body .body-modal").style.display = "";
            }
        },
        windowMoveEnd:function (){
            if(attr.winMoveStatus && attr.winMoveStatus.that){
                var that = attr.winMoveStatus.that;
                var target = document.querySelector("#"+attr.winMoveStatus.that.win.id+"header").parentElement.parentElement;
                attr.winMoveStatus.that.win.left = target.offsetLeft;
                attr.winMoveStatus.that.win.top = target.offsetTop;
                attr.winMoveStatus = {};
                var iframe = document.querySelector("#"+that.win.id+"body iframe");
                if(iframe){
                    document.querySelector("#"+that.win.id+"body .body-modal").style.display = "none";
                }
            }
        },
        winSizeChangeDbl:function (e){
            var that = this;
            that.topWindow(e);
            that.windowMoveStart(e);
            if(that.lastClickObj && Date.now() - that.lastClickObj.time <= 400){
                if(!that.win.disableMax && !that.win.hideMax && !that.win.hideSize){
                    that.windowAction(that.win.winNowType == 2?3:2);
                }
            };
            that.lastClickObj = {
                time:Date.now()
            }
        },
        topWindow:function (e){
            var that = this;
            var desktop = webos.el.findParentComponent(that,"desktop-component");
            desktop.topWindow(e);
        },
        winChangeSize:function (width,height,left,top,auto){
            var that = this;
            if(auto){
                if(that.win.winNowType!=2){
                    that.windowAction(2,{set:false});
                };
            }
            var left = left;
            var top = top;
            var width = width;
            var height = height;
            if(left == undefined){
                var fullWidth = document.body.clientWidth;
                left = Math.floor((fullWidth - width)/2);
            }
            if(top == undefined){
                var fullHeight = document.body.clientHeight-48;
                top = Math.floor((fullHeight - height)/2);
            }
            var target = document.querySelector("#"+that.win.id+"header").parentElement.parentElement;
            var oldData = {
                left:target.offsetLeft,
                top:target.offsetTop,
                width:target.offsetWidth,
                height:target.offsetHeight
            };
            var newData = {
                left:left,
                top:top,
                width:width,
                height:height
            };
            that.animationWindow(oldData,newData,true,true);
        },
        windowLayout:function (e){
            e.stopPropagation();
            if(!e.target.classList.contains("snapper")){
                return;
            }
            var that = this;
            var sz = e.target.dataset.size.split(",");
            var fullWidth = document.body.clientWidth;
            var fullHeight = document.body.clientHeight-48;
            var left = sz[0]*fullWidth;
            var top = sz[1]*fullHeight;
            var width = sz[2]*fullWidth;
            var height = sz[3]*fullHeight;
            that.winChangeSize(width,height,left,top,true);
        }
    },
    created: function () {

    }
}