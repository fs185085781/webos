(function (){
    Vue.app({
        data(){
            return {
                currentTab:"",
                tabs:[],
                list:[],
                edit:{
                    show:false,
                    data:{},
                    title:""
                },
                currentTheme:""
            }
        },
        methods:{
            changeTab:function (name){
                const that = this;
                that.$nextTick(function (){
                    const termDivs = that.$refs["term"];
                    for (let i = 0; i < termDivs.length; i++) {
                        const tmpDiv = termDivs[i];
                        if(tmpDiv.dataset.key != name){
                            tmpDiv.style.display = "none";
                            continue;
                        }
                        tmpDiv.style.display = "";
                    }
                });
            },
            saveSshData:async function (){
              const that = this;
                var data = JSON.parse(JSON.stringify(that.edit.data));
                var name = data.name;
                delete data.name;
                data.operate = "connect";
                await that.addSshLink({name:name,options:data});
                that.edit.show = false;
            },
            removeSshLink:function (index){
                const that = this;
                const data = that.list[index];
                utils.$.confirm("确认删除'"+data.name+"'?此操作不可逆",function (flag){
                    if(!flag){
                        return;
                    }
                    that.list.splice(index,1);
                });
            },
            toAddSshLink:function (flag){
                const that = this;
                var title = "";
                var record = {"host":"127.0.0.1","port":"22","username":"root","name":"127.0.0.1"};
                if(flag == 0){
                    title = "新增连接";
                }else if(flag == 1){
                    title = "编辑链接";
                    record = {};
                }else{
                    return;
                }
                that.edit.data = record;
                that.edit.title = title;
                that.edit.show = true;

            },
            addSshLink:async function (data){
                const that = this;
                let has = false;
                for (let i = 0; i < that.list.length; i++) {
                    if(that.list[i].name == data.name){
                        that.list[i] = data;
                        has = true;
                        break;
                    }
                }
                if(!has){
                    that.list.push(data);
                }
                parent.webos.context.set("showOkErrMsg", true);
                await parent.webos.softUserData.syncList("webssh_list",that.list);
                await that.initData();
            },
            removeTab:function (name){
                const that = this;
                let currentIndex = -1;
                for (let i = 0; i < that.tabs.length; i++) {
                    const tab = that.tabs[i];
                    if(tab.name == name){
                        currentIndex = i;
                        break;
                    }
                }
                if(currentIndex == -1){
                    return;
                }
                if(that.tabs[currentIndex].client){
                    that.tabs[currentIndex].client.close();
                    that.tabs[currentIndex].client = null;
                }
                that.tabs.splice(currentIndex, 1);
            },
            toOpenSsh:function (item){
                const name = item.name;
                const options = item.options;
                const that = this;
                let currentTab = null;
                for (let i = 0; i < that.tabs.length; i++) {
                    const tab = that.tabs[i];
                    if(tab.name == name){
                        currentTab = tab;
                        break;
                    }
                }
                if(!currentTab){
                    that.tabs.push({"name":name});
                    currentTab = that.tabs[that.tabs.length-1];
                }
                that.$nextTick(function (){
                    const termDivs = that.$refs["term"];
                    for (let i = 0; i < termDivs.length; i++) {
                        const tmpDiv = termDivs[i];
                        if(tmpDiv.dataset.key != currentTab.name){
                            continue;
                        }
                        if(currentTab.client){
                            currentTab.client.close();
                            currentTab.client = null;
                        }
                        currentTab.data = options;
                        currentTab.div = tmpDiv;
                        that.openSsh(currentTab);
                        that.currentTab = currentTab.name;
                       // that.changeTab(currentTab.name);
                    }
                });
            },
            initData:async function (){
                const that = this;
                that.list = await parent.webos.softUserData.syncList("webssh_list")
            },
            init:async function (){
                const that = this;
                window.addEventListener("message",function (e){
                    let data = e.data;
                    if(data.action == "themeChange"){
                        that.setTheme(data.theme);
                    }
                });
                that.setTheme(localStorage.getItem("web_theme"));
                await that.initData();
            },
            setTheme:function(theme){
                theme = theme == "dark"?"dark":"";
                document.querySelector("html").className = theme;
            },
            openSsh:function (tab){
                console.log(tab);
                const options = tab.data;
                const that = this;
                tab.div.innerHTML = "";
                tab.client = new WSSHClient();
                var term = new Terminal({
                    cols: 97,
                    rows: 37,
                    cursorBlink: true, // 光标闪烁
                    cursorStyle: "block", // 光标样式  null | 'block' | 'underline' | 'bar'
                    scrollback: 10000, //回滚
                    tabStopWidth: 8, //制表宽度
                    screenKeys: true
                });
                term.on('data', function (data) {
                    //键盘输入时的回调函数
                    tab.client.sendClientData(data);
                });
                term.open(tab.div);
                //在页面上显示连接中...
                term.write('Connecting...\r\n');
                //执行连接操作
                tab.client.connect({
                    onError: function (error) {
                        //连接失败回调
                        term.write('Error: ' + error + '\r\n');
                    },
                    onConnect: function () {
                        //连接成功回调
                        tab.client.sendInitData(options);
                    },
                    onClose: function () {
                        //连接关闭回调
                        term.write("\rconnection closed");
                    },
                    onData: function (data) {
                        //收到数据时回调
                        term.write(data);
                    }
                });
            }
        },
        mounted:function(){
            const that = this;
            that.init();
        }
    });
})()