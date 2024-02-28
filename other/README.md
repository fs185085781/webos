不编译安装  
1.手动安装jdk  
2.从右侧发行版本中下载到本地后解压  
3.双击restart.bat或者cd xxx/xxx && sh restart.sh即可 

编译安装  
1.打包api中的java程序然后重命名为webos.jar  
安装jdk和maven环境  
cd到api目录  
执行`mvn install`命令等待片刻即可在target目录中生成jar包  

2.构建成如下结构的目录  
```
xxx/xxx  
 ├── web
 │    └── index.html  
 └── api  
      └── restart.sh  
      └── start.bat  
      └── restart.bat  
      └── webos.jar  
```
3.启动  
linux通过 cd xxx/xxx/api && sh restart.sh启动  
win通过 双击start.bat启动  

4.访问  
浏览器访问 `http://127.0.0.1:8088` 或 `http://127.0.0.1:8088/init` 进行引导安装即可