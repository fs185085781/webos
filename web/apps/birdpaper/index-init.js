(function (){
    Vue.app({
        data(){
            return {
                actionType:0,//0分类分页 1搜索分页
                type:0,//0:PC专区,1:手机专区
                pageType:0,//0分类选择1分页结果
                catData:{
                    pcCat:[],
                    mDong:[],
                    mJin:[]
                },//分类数据
                dataList:[],//分页列表
                dataParam:{
                    page:1,
                    count:24,
                    type:"1",//1动态壁纸 2静态壁纸
                    cat:"",
                    total:1,
                    keyword:""
                },
                showItem:{
                    show:false,
                    item:{}
                },
                loading:false
            }
        },
        methods:{
            initData:function (){
                const that = this;
                fetch("https://wp.shanhutech.cn/intf/getCategory")
                    .then(function (res){return res.json()})
                    .then(function (res){
                        that.catData.pcCat = res.data;
                    });
                fetch("https://digbird.shanhutech.cn/intf/getCategory?type=1")
                    .then(function (res){return res.json()})
                    .then(function (res){
                        that.catData.mDong = res.data;
                    });
                fetch("https://digbird.shanhutech.cn/intf/getCategory?type=2")
                    .then(function (res){return res.json()})
                    .then(function (res){
                        that.catData.mJin = res.data;
                    });
                that.changeType();
                window.addEventListener("resize",function (){
                    that.changeType();
                });
                window.addEventListener("message",function (e){
                    let data = e.data;
                    if(data.action == "themeChange"){
                        that.setTheme(data.theme);
                    }
                });
                that.setTheme(localStorage.getItem("web_theme"));
            },
            setTheme:function (theme){
                theme = theme == "dark"?"dark":"";
                document.querySelector("html").className = theme;
            },
            changeType:function (){
                const that = this;
                if(window.innerHeight>window.innerWidth){
                    //手机效果
                    that.type = 1;
                }else{
                    //电脑效果
                    that.type = 0;
                }
                that.pageType = 0;
                that.actionType = 0;
            },
            searchData:function (){
                const that = this;
                that.actionType = 1;
                that.catPageList(1);
            },
            catPageList:function (page){
                const that = this;
                that.loading = true;
                that.dataParam.page = page;
                that.pageType = 1;
                that.dataList = [];
                var url = "";
                if(that.type == 0){
                    //电脑
                    if(that.dataParam.type == "1"){
                        //动态
                        const script = document.createElement("script");
                        script.id = "jsonp-id";
                        let fname = "f"+utils.uuid();
                        window[fname] = function (res){
                            that.loading = false;
                            var sz = res.data.list;
                            that.dataParam.total = res.data.total_count;
                            var list = [];
                            for (let i = 0; i < sz.length; i++) {
                                var a = sz[i];
                                list.push({
                                    img:a.image,
                                    name:a.title,
                                    mp4:a.file_url_10s
                                });
                            }
                            that.dataList = list;
                        }
                        var jsonpUrl = "https://bizhi.shanhutech.cn/live/wallpaper/categoryList?callback="+fname+"&category="+that.dataParam.cat+"&pageno="+page+"&count="+that.dataParam.count+"&type=1";
                        if(that.actionType == 1){
                            jsonpUrl = "https://bizhi.shanhutech.cn/live/wallpaper/searchList?callback="+fname+"&keyword="+encodeURIComponent(that.dataParam.keyword)+"&pageno="+page+"&count="+that.dataParam.count;
                        }
                        script.src = jsonpUrl;
                        document.body.appendChild(script);
                        setTimeout(function (){
                            document.body.removeChild(script);
                        },1);
                    }else{
                        //静态
                        url = "https://wp.shanhutech.cn/intf/GetListByCategory?cids="+that.dataParam.cat+"&pageno="+page+"&count="+that.dataParam.count;
                        if(that.actionType == 1){
                            url = "https://wp.shanhutech.cn/intf/search?content="+encodeURIComponent(that.dataParam.keyword)+"&pageno="+page+"&count="+that.dataParam.count;
                        }
                    }
                }else{
                    //手机
                    url = "https://digbird.shanhutech.cn/intf/getCategoryList?type="+that.dataParam.type+"&category="+that.dataParam.cat+"&appver=2.0.0&count="+that.dataParam.count+"&pageno="+page;
                }
                if(url){
                    fetch(url).then(function (res){return res.json()}).then(function (res){
                        that.loading = false;
                        if(that.type == 0 && that.dataParam.type != "1"){
                            var sz = res.data.list;
                            that.dataParam.total = res.data.total_count;
                            var list = [];
                            for (let i = 0; i < sz.length; i++) {
                                var a = sz[i];
                                list.push({
                                    img:a.url,
                                    name:a.tag
                                });
                            }
                            that.dataList = list;
                        }else if (that.type == 1){
                            console.log(res)
                            var sz = res.data.list;
                            that.dataParam.total = res.data.total_count;
                            var list = [];
                            for (let i = 0; i < sz.length; i++) {
                                var a = sz[i];
                                if(that.dataParam.type == "1"){
                                    list.push({
                                        img:a.image,
                                        name:a.title,
                                        mp4:a.url_preview,
                                    });
                                }else{
                                    list.push({
                                        img:a.url,
                                        name:a.tag
                                    });
                                }

                            }
                            that.dataList = list;
                        }
                    });
                }
            },
            selectCat:function (type,cat){
                const that = this;
                that.dataParam.type = type;
                if(that.type == 0){
                    //电脑
                    if(that.dataParam.type == "1"){
                        //动态
                        that.dataParam.cat = cat.show_name;
                    }else{
                        //静态
                        that.dataParam.cat = cat.old_id;
                    }
                }else{
                    //手机
                    if(that.dataParam.type == "1"){
                        //动态
                        that.dataParam.cat = cat.category;
                    }else{
                        //静态
                        that.dataParam.cat = cat.category;
                    }
                }
                that.actionType = 0;
                that.catPageList(1);
            },
            showBgAction:function (item){
                const that = this;
                that.showItem.item = item;
                that.showItem.show = true;
            },
            applyData:async function (sys,type){
                //sys:win11,macos,mobile
                //type:1动态2静态
                const that = this;
                var url = that.showItem.item.mp4;
                var wallpaperType = "video";
                if(type == "2"){
                    url = that.showItem.item.img;
                    wallpaperType = "img";
                }
                if(sys == "win11"){
                    var has = false;
                    var winCom = parent.webos.util.getCurrentWinByIframe(window);
                    if(winCom){
                        var app = parent.webos.el.findParentComponent(winCom,"app-component");
                        if(app){
                            await app.changeWallpaper(url, wallpaperType);
                            has = true;
                        }
                    }
                    if(has){
                        parent.webos.message.success("应用壁纸成功");
                    }else{
                        var data = await parent.webos.softUserData.syncObject("settings_theme_win11");
                        if(data){
                            data.wallpaper = url;
                            data.type = wallpaperType;
                            await parent.webos.softUserData.syncObject("settings_theme_win11",data);
                            parent.webos.message.success("应用壁纸成功");
                        }else{
                            parent.webos.message.success("应用壁纸失败");
                        }
                    }
                }else if(sys == "macos"){
                    parent.webos.message.error("该系统暂未上线");
                }if(sys == "mobile"){
                    parent.webos.message.error("该系统暂未上线");
                }
            }
        },
        mounted:function(){
            this.initData();
        }
    });
})()