(function(){
    Vue.app({
        data(){
            return {
                list:[],
                themes:[
                    {theme:"default",color:"linear-gradient(to bottom,#fff,#fff 50%,#000 50%,#000)"},
                    {theme:"aftertherain",color:"#9fff5b"},
                    {theme:"hazel",color:"#79CBCA"}
                ],
                currentTheme:""
            }
        },
        methods:{
            setTheme:async function(theme){
                var that = this;
                if(!theme){
                    theme = await parent.webos.softUserData.syncData("music_theme");
                }
                if(!theme){
                    theme = "default";
                }
                if(theme == "default" || theme == "dark"){
                    var tmp = parent.webos.util.getCacheTheme();
                    theme = tmp == "dark"?"dark":"default";
                    if(theme == "dark"){
                        document.querySelector("html").className = "dark";
                    }else{
                        document.querySelector("html").className = "";
                    }
                }else{
                    document.querySelector("html").className = "";
                }
                that.currentTheme = theme;
                parent.webos.softUserData.syncData("music_theme",theme);
                var link = document.querySelector("#theme");
                if(link){
                    link.href = "css/"+theme+".css";
                }else{
                    link = document.createElement("link");
                    link.rel = "stylesheet";
                    link.id = "theme";
                    link.type = "text/css";
                    link.href = "css/"+theme+".css";
                    document.head.append(link);
                }
            },
            toAddRemoveBtn:function (){
                var that = this;
                var lis = document.querySelectorAll("#music-player .aplayer-list li");
                for (let i = 0; i < lis.length; i++) {
                    let li = lis[i];
                    if(li.querySelector(".aplayer-list-remove")){
                        continue;
                    }
                    var span = document.createElement("span");
                    span.innerHTML = "<i class=\"el-icon\"><img src='imgs/remove.svg'></i>";
                    span.className = "aplayer-list-remove";
                    li.append(span);
                    span.addEventListener("click",function (e){
                        e.stopPropagation();
                        var tmps = document.querySelectorAll("#music-player .aplayer-list li");
                        for (let j = 0; j < tmps.length; j++) {
                            if(tmps[j] == li){
                                that.removeData(j);
                                break;
                            }
                        }
                    });
                }
            },
            removeData:function (index){
                var that = this;
                var data = that.list[index];
                var mainName = parent.webos.util.getMainByName(data.name);
                parent.utils.$.confirm("确认删除'"+mainName+"'歌曲吗?",async function(flag){
                    if(!flag){
                        return;
                    }
                    that.list.splice(index,1);
                    that.ap.list.remove(index);
                    await parent.webos.softUserData.syncList("music_list",that.list);
                });
            },
            init:async function (){
                var that = this;
                window.addEventListener("message",function (e){
                    let data = e.data;
                    if(data.action == "themeChange"){
                        if(that.currentTheme == "default" || that.currentTheme == "dark"){
                            that.setTheme("default");
                        }
                    }
                });
                var winCom = parent.webos.util.getCurrentWinByIframe(window);
                if(winCom){
                    var height = winCom.$props.win.height;
                    winCom.winChangeSize(350,height);
                }
                parent.webos.util.setParentSimple(window);
                var param = new URL(location.href).searchParams;
                var list = await parent.webos.softUserData.syncList("music_list");
                var expAction  = param.get("expAction");
                var index = -1;
                if(expAction == "open"){
                    //打开模式
                    var data = {
                        name:param.get("fname"),
                        path:param.get("path")
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
                        var data = {name:file.name,path:file.path};
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
                that.list = list;
                parent.webos.softUserData.syncList("music_list",list);
                that.setTheme();
                var musicList = [];
                for (let i = 0; i < list.length; i++) {
                    var data = list[i];
                    var mainName = parent.webos.util.getMainByName(data.name);
                    var sz = mainName.split("-");
                    var songArtist = sz[0].trim();
                    var songName = songArtist;
                    if(sz.length > 1){
                        songName = sz[1].trim();
                    }
                    var parentPath = parent.webos.util.getParentPath(data.path);
                    var lrc = await parent.webos.fileSystem.zlByName(parentPath+"/"+mainName+".lrc");
                    var music = {
                        name: songName,
                        artist: songArtist,
                        url: await parent.webos.fileSystem.zl(data.path),
                        cover: "",
                        lrc: lrc
                    }
                    musicList.push(music);
                }
                const ap = new APlayer({
                    container: document.getElementById('music-player'),
                    mini: false,
                    fixed: false,
                    autoplay: true,
                    theme: '#9fff5b',
                    loop: 'all',
                    order: 'random',
                    preload: 'auto',
                    volume: 0.3,
                    mutex: true,
                    listFolded: false,
                    listMaxHeight: "calc(100vh - 170px)",
                    lrcType: 3,
                    audio: musicList
                });
                that.ap = ap;
                let setMusicCover = function (index){
                    var audio = that.ap.list.audios[index];
                    var searchName = audio.name;
                    if(searchName != audio.artist){
                        searchName = audio.artist + "-" + searchName;
                    }
                    player.syncGetKuGouImg(searchName).then(function (url){
                        if(url){
                            document.querySelector("#music-player .aplayer-pic").style["background-image"] = "url("+url+")";
                        }
                    });
                }
                that.ap.on('listswitch', function (one){
                    setMusicCover(one.index);
                });
                that.toAddRemoveBtn();
                that.ap.list.switch(index);
            }
        },
        mounted:function(){
            this.init();
        }
    });
})()