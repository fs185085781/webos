(function (){
    webos.fileSystem.aliyundriveUpload = {
        sha1:function (file,fn){
            if(!window.sha1){
                utils.syncLoadData(webos.sdkUrl+"/sha1.min.js",function(text){
                    eval(text);
                });
            };
            return new Promise(function (success,error){
                var reader = new FileReader();
                var batch = 1024 * 1024 * 2;
                var start = 0;
                var total = file.size;
                var current = window.sha1;
                reader.onload = function (event) {
                    try {
                        current = current.update(event.target.result);
                        asyncUpdate();
                    } catch(e) {
                        error(e);
                    }
                };
                var asyncUpdate = function () {
                    if (start < total) {
                        if(fn){
                            fn(start,total);
                        }
                        var end = Math.min(start + batch, total);
                        reader.readAsArrayBuffer(file.slice(start, end));
                        start = end;
                    } else {
                        var dex = current.hex();
                        success(dex);
                    }
                };
                asyncUpdate();
            });
        },
        base64:function(file){
            return new Promise(function(resolve, reject){
                const reader = new FileReader();
                reader.readAsDataURL(file);
                reader.onload = function (){
                    let str = reader.result;
                    resolve(str.split(",")[1]);
                };
                reader.onerror=function (error){
                    reject(error);
                };
            });
        },
        getFp:async function (index,param){
            var that = this;
            if(index<0){
                index = 0;
            }else if(index >= param.needFps){
                index = param.needFps - 1;
            }
            let end = (index+1)*param.fpSize;
            if(param.size < end){
                end = param.size;
            }
            const fp = param.file.slice(index*param.fpSize,end);
            const fp_hash = await that.sha1(fp);
            return {fp:fp,hash:fp_hash};
        },
        proofCodeCalc:function(m,file){
            let that = this;
            if(!window.BigNumber){
                utils.syncLoadData(webos.sdkUrl+"/bignumber.min.js",function(text){
                    eval(text);
                });
            };
            let r = new BigNumber("0x".concat(m))
                , i = new BigNumber(file.size)
                , o = i ? r.mod(i) : new BigNumber(0);
            let qp = file.slice(o.toNumber(), Math.min(o.plus(8).toNumber(), file.size));
            return that.base64(qp);
        },
        uploadPreCommon:async function (type,param){
            var that = this;
            if(param.uploadPreRes){
                if(param.uploadPreRes.rapid_upload || param.uploadPreRes.file_id){
                    return true;
                }
            }
            let postData = {
                check_name_mode:"overwrite",
                part_info_list:[],
                size:param.size,
                type:"file"
            };
            if(type == 2){
                //完整算法模式
                param.expName = "校验中0%";
                postData.content_hash = await that.sha1(param.file,function (loaded,total){
                    param.expName = "校验中"+(loaded / total * 100).toFixed(2) + '%';
                    if(param.callback){
                        param.callback(param);
                    }
                });
                param.expName = "";
                if(param.callback){
                    param.callback(param);
                }
                postData.content_hash = postData.content_hash.toUpperCase();
                postData.content_hash_name = "sha1";
                postData.proof_version = "v1";
                postData.proof_code = await that.proofCodeCalc(param.uploadPreMy,param.file);
            }else{
                //前缀算法模式
                postData.pre_hash = await that.sha1(param.file.slice(0,1024))
            }
            let count = 20;
            if(param.needFps<count){
                count = param.needFps;
            }
            for(let i=1;i<=count;i++){
                postData.part_info_list.push({part_number: i});
            }
            let expand = {
                postData:postData,
                type:type
            };
            let dataStr = await webos.request.commonData("fileSystem", "uploadPre",{path:param.path,expand:JSON.stringify(expand),name:param.filePath});
            var errMsg = webos.context.get("lastErrorReqMsg");
            if(!dataStr){
                param.status = 3;
                param.msg = errMsg?errMsg:"上传出错";
                param.statusChange(param);
                return false;
            }
            try{
                param.uploadPreRes = JSON.parse(dataStr);
            }catch (e){
            }
            if(!param.uploadPreRes){
                param.status = 3;
                param.msg = "上传错误";
                param.statusChange(param);
                return false;
            }
            if(param.uploadPreRes.rapid_upload){
                return true;
            }
            if(param.uploadPreRes.pre_hash){
                return that.uploadPreCommon(2,param)
            }
            param.uploadUrlMap = {};
            let info_list = param.uploadPreRes.part_info_list;
            for (let i = 0; i < info_list.length; i++) {
                const tmp = info_list[i];
                param.uploadUrlMap[tmp.part_number] = tmp.upload_url;
            }
            if(!param.uploadPreRes.file_id){
                param.status = 3;
                param.msg = "上传错误";
                param.statusChange(param);
                return false;
            }
            return !!param.uploadPreRes.file_id;
        },
        upload:async function (param,progress){
            var that = this;
            if(param.expName){
                webos.message.error("文件校验中,操作失败");
                return;
            }
            param.uploadPreMy = await webos.request.commonData("fileSystem", "uploadPre",{path:param.path,expand:JSON.stringify({type:1}),name:param.filePath});
            if(!param.uploadPreMy){
                param.status = 3;
                param.msg = webos.context.get("lastErrorReqMsg");
                param.statusChange(param);
                return;
            }
            var errMsg;
            if(!param.uploadPreRes){
                var simpleHash = await webos.fileSystem.fileHashSimple(param.file);
                param.uploadKey = "upload"+simpleHash+param.path;
                var cache = await webos.fileSystem.getCacheValue(param.uploadKey);
                if(cache && cache.time > Date.now() - 60*60*1000){
                    param.uploadPreRes = cache.uploadPreRes;
                    param.currentFp = cache.currentFp;
                    param.uploadUrlMap = cache.uploadUrlMap;
                }else{
                    var type = param.needFps>10?3:2;
                    var flag = await that.uploadPreCommon(type,param);
                    if(!flag){
                        param.uploadPreRes = null;
                    }
                }
            };
            if(!param.uploadPreRes){
                param.status = 3;
                param.msg = "上传出错";
                param.statusChange(param);
                return;
            };
            if(param.uploadPreRes.rapid_upload){
                param.status = 2;
                param.msg = "上传成功";
                param.statusChange(param);
                return;
            };
            let isFirst = true;
            for(let i=0;i<param.needFps;i++){
                if(i<param.currentFp){
                    continue;
                };
                if(param.status != 1){
                    return;
                }
                param.currentFp = i;
                if(isFirst){
                    isFirst = false;
                    param.startTime = Math.floor(new Date().getTime()/1000);
                    param.startSize = param.fpSize*i;
                }
                let part = await that.getFp(i,param);
                let url = param.uploadUrlMap[i+1];
                if(!url){
                    let data = {file_id:param.uploadPreRes.file_id,upload_id:param.uploadPreRes.upload_id,part_info_list:[]};
                    let count = 20;
                    if(param.needFps-i<count){
                        count = param.needFps-i;
                    }
                    for(let m=1;m<=count;m++){
                        data.part_info_list.push({part_number: m+i});
                    }
                    let urlDataStr = await webos.request.commonData("fileSystem", "uploadUrl",{path:param.path,expand:JSON.stringify(data),name:param.filePath});
                    errMsg = webos.context.get("lastErrorReqMsg");
                    if(!urlDataStr){
                        param.status = 3;
                        param.msg = errMsg?errMsg:"上传出错";
                        param.statusChange(param);
                        return;
                    }
                    try{
                        let list = JSON.parse(urlDataStr);
                        for(let n=0;n<list.length;n++){
                            let one = list[n];
                            param.uploadUrlMap[one.part_number] = one.upload_url;
                        }
                    }catch (e){
                    }
                    url = param.uploadUrlMap[i+1];
                    if(!url){
                        param.status = 3;
                        param.msg = "上传出错";
                        param.statusChange(param);
                        return;
                    }
                };
                let success = false;
                for(let h=0;h<3;h++){
                    if(param.status != 1){
                        return;
                    }
                    param.currentXhr = new XMLHttpRequest();
                    let xhr = await webos.request.xhrReq("PUT",url,part.fp,progress,
                        {
                            "Content-Type":""
                        },param.currentXhr);
                    param.currentXhr = undefined;
                    if(param.status == 4){
                        param.status = 4;
                        param.msg = "上传暂停";
                        param.statusChange(param);
                        return;
                    }
                    if(xhr && xhr.status && (xhr.status == 200 || xhr.status == 409)){
                        success = true;
                        break;
                    };
                }
                if(!success){
                    param.status = 3;
                    param.msg = "上传出错";
                    param.statusChange(param);
                    return;
                }
                webos.fileSystem.setCacheValue(param.uploadKey,{
                    time:Date.now(),
                    uploadPreRes:param.uploadPreRes,
                    currentFp:param.currentFp+1,
                    uploadUrlMap:param.uploadUrlMap
                });
            };
            let checkStr = await webos.request.commonData("fileSystem", "uploadAfter",{path:param.path,expand:JSON.stringify({file_id:param.uploadPreRes.file_id,upload_id:param.uploadPreRes.upload_id}),name:param.filePath});
            errMsg = webos.context.get("lastErrorReqMsg");
            if(checkStr == "1"){
                param.status = 2;
                param.msg = "上传成功";
            }else{
                param.status = 3;
                param.msg = errMsg?errMsg:"上传出错";
            }
            param.statusChange(param);
        }
    };
})()