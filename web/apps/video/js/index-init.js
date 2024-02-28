(function (){
    Vue.app({
        data(){
            return {
                list:[],
                current:0,
                listOpen:false
            }
        },
        methods:{
            onSizeChange:function (width,height){
                var winCom = parent.webos.util.getCurrentWinByIframe(window);
                if(winCom){
                    var maxHeight = parseInt((parent.document.body.clientHeight-48)*0.9);
                    var maxWidth = parseInt(parent.document.body.clientWidth*0.9);
                    var possibleHeight = parseInt(maxWidth*height/width);
                    if(possibleHeight<=maxHeight){
                        winCom.winChangeSize(maxWidth,possibleHeight);
                    }else{
                        var possibleWidth = parseInt(maxHeight*width/height);
                        winCom.winChangeSize(possibleWidth,maxHeight);
                    }
                }
            },
            playData:async function (index){
                var that = this;
                var data = that.list[index];
                that.lastTime = new Date().getTime();
                var mainName = data.name;
                that.current = index;
                var url = "";
                var parentPath = "";
                if(!data.path){
                    var param = new URL(location.href).searchParams;
                    url = param.get("url");
                }else{
                    url = await parent.webos.fileSystem.zl(data.path);
                    parentPath = parent.webos.util.getParentPath(data.path);
                }
                var ext = data.ext;
                if(that.art){
                    that.art.destroy();
                }
                if(ext === "m3u8x"){
                    ext = "m3u8";
                    utils.syncLoadData(url,function(res){
                        url = res.trim();
                    });
                }
                that.art = new Artplayer({
                    container: '#artplayer-app',
                    url: url,
                    title: mainName,
                    volume: 0.5,
                    isLive: false,
                    muted: false,
                    autoplay: true,
                    pip: true,
                    autoSize: true,
                    autoMini: true,
                    screenshot: true,
                    setting: true,
                    loop: true,
                    flip: true,
                    playbackRate: true,
                    aspectRatio: true,
                    fullscreen: true,
                    fullscreenWeb: false,
                    subtitleOffset: true,
                    miniProgressBar: true,
                    mutex: true,
                    backdrop: true,
                    playsInline: true,
                    autoPlayback: true,
                    airplay: true,
                    theme: '#23ade5',
                    type: ext,
                    lang: navigator.language.toLowerCase(),
                    whitelist: ['*'],
                    moreVideoAttr: {
                        crossOrigin: 'anonymous',
                    },
                    controls:[
                        {
                            position: 'right',
                            html: '<div class="list-btn"><img width="22" heigth="22" src="./imgs/subtitle.svg"></div>',
                            tooltip: '节目列表',
                            style: {
                                color: 'green',
                            },
                            click: function () {
                                that.listOpen = !that.listOpen;
                            },
                        }
                    ],
                    settings: [
                        {
                            width: 200,
                            html: '字幕语言',
                            tooltip: 'SRT字幕',
                            icon: '<img width="22" heigth="22" src="./imgs/subtitle.svg">',
                            selector: [
                                {
                                    html: '显示字幕',
                                    tooltip: '显示',
                                    switch: true,
                                    onSwitch: function (item) {
                                        item.tooltip = item.switch ? '隐藏' : '显示';
                                        that.art.subtitle.show = !item.switch;
                                        return !item.switch;
                                    },
                                },
                                {
                                    default: true,
                                    html: 'SRT字幕',
                                    url: await parent.webos.fileSystem.zlByName(parentPath+"/"+mainName+".srt"),
                                },
                                {
                                    html: 'VTT字幕',
                                    url: await parent.webos.fileSystem.zlByName(parentPath+"/"+mainName+".vtt"),
                                },
                                {
                                    html: 'ASS字幕',
                                    url: await parent.webos.fileSystem.zlByName(parentPath+"/"+mainName+".ass"),
                                }
                            ],
                            onSelect: function (item) {
                                that.art.subtitle.switch(item.url, {
                                    name: item.html,
                                });
                                return item.html;
                            },
                        }
                    ],
                    subtitle: {
                        url: await parent.webos.fileSystem.zlByName(parentPath+"/"+mainName+".srt"),
                        type: 'srt',
                        style: {
                            color: '#fe9200',
                            fontSize: '20px',
                        },
                        encoding: 'utf-8',
                    },
                    customType: {
                        flv: function (video, url) {
                            console.log("播放flv",video,url)
                            if (flvjs.isSupported()) {
                                const flvPlayer = flvjs.createPlayer({
                                    type: 'flv',
                                    url: url,
                                });
                                flvPlayer.attachMediaElement(video);
                                flvPlayer.load();
                            } else {
                                parent.webos.message.error("不支持此视频")
                            }
                        },
                        m3u8: function (video, url) {
                            if (Hls.isSupported()) {
                                const hls = new Hls();
                                hls.loadSource(url);
                                hls.attachMedia(video);
                            } else {
                                const canPlay = video.canPlayType('application/vnd.apple.mpegurl');
                                if (canPlay === 'probably' || canPlay === 'maybe') {
                                    video.src = url;
                                } else {
                                    parent.webos.message.error("不支持此视频")
                                }
                            }
                        },
                        mpd: function (video, url) {
                            var player = dashjs.MediaPlayer().create();
                            player.initialize(video, url, true);
                        },
                        torrent: function (video, url, art) {
                            var client = new WebTorrent();
                            that.art.loading.show = true;
                            client.add(url, function (torrent) {
                                var file = torrent.files[0];
                                file.renderTo(video, {
                                    autoplay: true,
                                });
                            });
                        },
                    }
                });
                that.art.on('video:loadeddata', function(){
                    setTimeout(function (){
                        that.onSizeChange(that.art.width,that.art.height);
                    },500);
                });
                that.art.on('video:ended',function (){
                    var index = that.current + 1;
                    if(index >= that.list.length){
                        index = 0;
                    }
                    that.playData(index);
                });
            },
            removeData:function (index){
                var that = this;
                var data = that.list[index];
                parent.utils.$.confirm("确认删除'"+data.name+"'视频吗?",async function(flag){
                    if(!flag){
                        return;
                    }
                    that.list.splice(index,1);
                    await parent.webos.softUserData.syncList("video_list",that.list);
                    if(index >= that.list.length){
                        index = that.list.length - 1;
                    }
                    that.playData(index);
                });
            },
            removeDataAll:function (){
                var that = this;
                parent.utils.$.confirm("确认移除全部视频吗?此操作不可逆",async function(flag){
                    if(!flag){
                        return;
                    }
                    that.list = [];
                    await parent.webos.softUserData.syncList("video_list",that.list);
                });
            },
            init:async function (){
                var that = this;
                var mfetch = window.fetch;
                window.fetch = function (a,b){
                    if(b){
                        b.mode = undefined;
                        b.referrerPolicy = undefined
                    }
                    return mfetch(a,b);
                }
                parent.webos.util.setParentSimple(window);
                var param = new URL(location.href).searchParams;
                var list = await parent.webos.softUserData.syncList("video_list");
                var expAction  = param.get("expAction");
                var index = -1;
                if(expAction == "open"){
                    //打开模式
                    var data = {
                        name:"",
                        path:param.get("path"),
                        ext:param.get("ext")
                    }
                    if(param.get("fname")){
                        data.name = parent.webos.util.getMainByName(param.get("fname"));
                    }
                    index = player.hasInList(list,data);
                    if(index == -1){
                        list.push(data);
                        index = list.length - 1;
                    }
                }else if(expAction == "playlist"){
                    //播放列表
                    var files = await parent.webos.util.getBigData("addAllFiles");
                    for (let i = 0; i < files.length; i++) {
                        var file = files[i];
                        var data = {name:"",ext:file.ext,path:file.path};
                        if(file.name){
                            data.name = parent.webos.util.getMainByName(file.name);
                        }
                        var tmpIndex = player.hasInList(list,data);
                        if(tmpIndex == -1){
                            list.push(data);
                            tmpIndex = list.length - 1;
                        }
                        if(index == -1){
                            index = tmpIndex;
                        }
                    }
                }
                if(index == -1){
                    index = 0;
                }
                parent.webos.softUserData.syncList("video_list",list);
                that.list = list;
                that.playData(index);
            }
        },
        mounted:function(){
            var that = this;
            that.init();
            document.onclick = function (e){
                var flag = parent.webos.el.isInClass(e.target,"list-btn");
                var flag2 = parent.webos.el.isInClass(e.target,"play-list");
                if(flag || flag2){
                    return;
                }
                that.listOpen = false;
            }
        }
    });
})()