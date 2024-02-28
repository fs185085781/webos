(function (){
    var url = new URL(window.location.href);
    var expAction = url.searchParams.get("expAction");
    if(expAction == "new"){
        var ext = url.searchParams.get("ext");
        var func = url.searchParams.get("func");
        parent[func](new Blob());
        return;
    };
    Vue.app({
        data(){
            return {
                fileData:{},
                config:{
                    theme:"sunny"
                },
                isMarkdown:false
            }
        },
        methods:{
            changeSeniorMd:function (){
                var url = new URL(window.location.href);
                var data = {};
                url.searchParams.forEach(function (val,key){
                    data[key]= val;
                });
                var oldUrlSz = (window.location.origin+window.location.pathname).split("/");
                oldUrlSz.length -= 1;
                var url2 = new URL(oldUrlSz.join("/")+"/md-senior.html");
                for(var key in data){
                    url2.searchParams.set(key,data[key]);
                }
                window.location.href = url2.href;
            },
            changeTheme:async function (){
                var that = this;
                if(that.config.theme == "sunny"){
                    that.config.theme = "moon";
                    that.editor.setTheme("ace/theme/sqlserver");
                }else{
                    that.config.theme = "sunny";
                    that.editor.setTheme("ace/theme/vibrant_ink");
                }
                await parent.webos.softUserData.save({appCode: "aceEditor", data: JSON.stringify(that.config)});
            },
            saveData:async function (){
                var that = this;
                var str = that.editor.getSession().getValue();
                var param = {
                    file:new Blob([str]),
                    name:that.fileData.fname,
                    parentPath:that.fileData.parentPath
                }
                var flag = await parent.webos.fileSystem.uploadSmallFile(param);
                if(flag){
                    utils.$.successMsg(parent.webos.context.get("lastSuccessReqMsg"));
                }else{
                    utils.$.errorMsg(parent.webos.context.get("lastErrorReqMsg"));
                }
            },
            init:async function (){
                var that = this;
                window.addEventListener("message",function (e){
                    let data = e.data;
                    if(data.action == "themeChange"){
                        var theme = data.theme == "dark"?"moon":"sunny";
                        if(theme == that.config.theme){
                            that.changeTheme();
                        }
                    }
                });
                var editor = ace.edit("editor");
                that.editor = editor;
                var configStr = await parent.webos.softUserData.get({appCode: "aceEditor"});
                if(!configStr){
                    configStr = JSON.stringify({theme:"sunny"});
                };
                that.config = JSON.parse(configStr);
                var theme = localStorage.getItem("web_theme");
                that.config.theme = theme != "dark"?"moon":"sunny";
                if(that.config.theme == "sunny"){
                    that.editor.setTheme("ace/theme/vibrant_ink");
                }else{
                    that.editor.setTheme("ace/theme/sqlserver");
                }
                editor.setOption("enableLiveAutocompletion",true);//自定代码提示
                editor.setOption("enableEmmet",true);//Emmet语法,主要针对html
                editor.setOption("enableBasicAutocompletion",true);
                editor.setOption("enableSnippets",true);//快捷函数语法
                editor.setOption("useElasticTabstops",true);
                editor.commands.addCommand({
                    name: 'save',
                    bindKey: {win: 'Ctrl-S',  mac: 'Command-S'},
                    exec: async function(editor) {
                        if(that.fileData.expAction != "edit"){
                            return;
                        }
                        await that.saveData();
                    }
                });
                var url = new URL(window.location.href);
                var data = {};
                url.searchParams.forEach(function (val,key){
                    data[key]= val;
                });
                that.fileData = data;
                that.changeMode(that.fileData.ext.toLowerCase());
                var sz = data.path.split("/");
                sz.length -= 1;
                that.fileData.parentPath = sz.join("/");
                if(data.expAction != "edit"){
                    that.editor.setReadOnly(true);
                }
                editor.getSession().setUseWrapMode(true);
                editor.getSession().on('change', function (){
                    that.markdownViewDataSet();
                });
                fetch(data.url)
                    .then(function (res){return res.text()})
                    .then(function (res){
                        that.editor.getSession().setValue(res);
                        that.markdownViewDataSet();
                    });
            },
            markdownViewDataSet:function (){
                var that = this;
                if(!that.isMarkdown){
                    return;
                }
                var str = that.editor.getSession().getValue();
                document.getElementById('markdown-view').innerHTML = marked.parse(str);
            },
            changeMode:function (ext){
                var that = this;
                var map = {"abap":"abap","abc":"abc","as":"actionscript","ada|adb":"ada","alda":"alda","htaccess|htgroups|htpasswd|conf|htaccess|htgroups|htpasswd":"apache_conf","apex|cls|trigger|tgr":"apex","aql":"aql","asciidoc|adoc":"asciidoc","dsl|asl|asl.json":"asl","asm|a":"assembly_x86","ahk":"autohotkey","bat|cmd":"batchfile","cpp|c|cc|cxx|h|hh|hpp|ino":"c_cpp","c9search_results":"c9search","cirru|cr":"cirru","clj|cljs":"clojure","cbl|cob":"cobol","coffee|cf|cson|cakefile":"coffee","cfm":"coldfusion","cr":"crystal","cs":"csharp","csd":"csound_document","orc":"csound_orchestra","sco":"csound_score","css":"css","curly":"curly","d|di":"d","dart":"dart","diff|patch":"diff","dockerfile":"dockerfile","dot":"dot","drl":"drools","edi":"edifact","e|ge":"eiffel","ejs":"ejs","ex|exs":"elixir","elm":"elm","erl|hrl":"erlang","frt|fs|ldr|fth|4th":"forth","f|f90":"fortran","fsi|fs|ml|mli|fsx|fsscript":"fsharp","fsl":"fsl","ftl":"ftl","gcode":"gcode","feature":"gherkin","gitignore":"gitignore","glsl|frag|vert":"glsl","gbs":"gobstones","go":"golang","gql":"graphqlschema","groovy":"groovy","haml":"haml","hbs|handlebars|tpl|mustache":"handlebars","hs":"haskell","cabal":"haskell_cabal","hx":"haxe","hjson":"hjson","html|htm|xhtml|vue|we|wpy":"html","eex|html.eex":"html_elixir","erb|rhtml|html.erb":"html_ruby","ini|conf|cfg|prefs":"ini","io":"io","ion":"ion","jack":"jack","jade|pug":"jade","java":"java","js|jsm|jsx|cjs|mjs":"javascript","json":"json","json5":"json5","jq":"jsoniq","jsp":"jsp","jssm|jssm_state":"jssm","jsx":"jsx","jl":"julia","kt|kts":"kotlin","tex|latex|ltx|bib":"latex","latte":"latte","less":"less","liquid":"liquid","lisp":"lisp","ls":"livescript","log":"log","logic|lql":"logiql","lsl":"lsl","lua":"lua","lp":"luapage","lucene":"lucene","makefile|gnumakefile|makefile|ocamlmakefile|make":"makefile","md|markdown":"markdown","mask":"mask","matlab":"matlab","mz":"maze","wiki|mediawiki":"mediawiki","mel":"mel","s|asm":"mips","mixal":"mixal","mc|mush":"mushcode","mysql":"mysql","nginx|conf":"nginx","nim":"nim","nix":"nix","nsi|nsh":"nsis","nunjucks|nunjs|nj|njk":"nunjucks","m|mm":"objectivec","ml|mli":"ocaml","partiql|pql":"partiql","pas|p":"pascal","pl|pm":"perl","pgsql":"pgsql","blade.php":"php_laravel_blade","php|inc|phtml|shtml|php3|php4|php5|phps|phpt|aw|ctp|module":"php","pig":"pig","ps1":"powershell","praat|praatscript|psc|proc":"praat","prisma":"prisma","plg|prolog":"prolog","properties":"properties","proto":"protobuf","epp|pp":"puppet","py":"python","qml":"qml","r":"r","raku|rakumod|rakutest|p6|pl6|pm6":"raku","cshtml|asp":"razor","rd":"rdoc","red|reds":"red","rhtml":"rhtml","robot|resource":"robot","rst":"rst","rb|ru|gemspec|rake|guardfile|rakefile|gemfile":"ruby","rs":"rust","sac":"sac","sass":"sass","scad":"scad","scala|sbt":"scala","scm|sm|rkt|oak|scheme":"scheme","scrypt":"scrypt","scss":"scss","sh|bash|bashrc":"sh","sjs":"sjs","slim|skim":"slim","smarty|tpl":"smarty","smithy":"smithy","snippets":"snippets","soy":"soy_template","space":"space","sql":"sql","sqlserver":"sqlserver","styl|stylus":"stylus","svg":"svg","swift":"swift","tcl":"tcl","tf":"terraform","tex":"tex","txt":"text","textile":"textile","toml":"toml","tsx":"tsx","twig|swig":"twig","ts|typescript|str":"typescript","vala":"vala","vbs|vb":"vbscript","vm":"velocity","v|vh|sv|svh":"verilog","vhd|vhdl":"vhdl","vfp|component|page":"visualforce","wlk|wpgm|wtest":"wollok","xml|rdf|rss|wsdl|xslt|atom|mathml|mml|xul|xbl|xaml":"xml","xq":"xquery","yaml|yml":"yaml","zeek|bro":"zeek","html":"django"};
                for(var key in map){
                    if(key==ext || key.split("|").includes(ext)){
                        var val = map[key];
                        that.isMarkdown = val == "markdown";
                        that.editor.session.setMode("ace/mode/"+val);
                        break;
                    }
                }
            }
        },
        mounted:function(){
            this.init();
        }
    });
})()