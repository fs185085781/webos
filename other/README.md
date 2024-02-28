编译说明
1.打包api中的java程序然后重命名为webos.jar
2.构建成如下结构的目录
xxx/xxx
├── web
│ └── index.html
└── api
    └── restart.sh
    └── start.bat
    └── webos.jar
3.启动
linux通过 cd xxx/xxx/api && sh restart.sh启动
win通过 双击start.bat启动
4.访问
浏览器访问http://127.0.0.1:8088进行引导安装即可