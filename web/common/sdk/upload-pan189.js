(function (){
    webos.fileSystem.pan189Upload = {
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
            if(!param.fpMap){
                param.fpMap = {}
            }
            if(index<0){
                index = 0;
            }else if(index >= param.needFps){
                index = param.needFps - 1;
            }
            if(param.fpMap && param.fpMap["index"+index]){
                return param.fpMap["index"+index];
            }
            let end = (index+1)*param.fpSize;
            if(param.size < end){
                end = param.size;
            }
            const fp = param.file.slice(index*param.fpSize,end);
            param.fpMap["index"+index] = {fp:fp,md5:await webos.fileSystem.fileMd5(fp)};
            return param.fpMap["index"+index];
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
        getFpSizeFpl:function (size){
            var tenMSize = 10485760;
            var fpSize = 0;
            if (size >= 0 && size < 1000 * tenMSize) {
                //10M
                fpSize = tenMSize * 1;
            } else if (1000 * tenMSize >= 0 && size < 2000 * tenMSize) {
                //20M
                fpSize = tenMSize * 2;
            } else if (size >= 2000 * tenMSize && size < 10000 * tenMSize) {
                //50M
                fpSize = tenMSize * 5;
            } else {
                var n = size / 2000 / tenMSize;
                n = Math.floor(n);
                if (size % (2000 * tenMSize) != 0) {
                    n++;
                }
                fpSize = tenMSize * n;
            }
            var fpSl = size / fpSize;
            fpSl = Math.floor(fpSl);
            if (size % fpSize != 0) {
                fpSl++;
            }
            return {fpSize,fpSl};
        },
        uploadPreCommon:async function (param){
            var that = this;
            if(param.uploadPreRes){
                return true;
            }
            let postData = {
                currentType:1,
                data:{
                    fileName:param.name,
                    fileSize:param.size,
                    sliceSize:param.fpSize,
                    lazyCheck:1
                }
            };
            let dataStr = await webos.request.commonData("fileSystem", "uploadPre",{path:param.path,expand:JSON.stringify(postData),name:param.filePath});
            param.lastErrorReqMsg = webos.context.get("lastErrorReqMsg");
            param.lastSuccessReqMsg = webos.context.get("lastSuccessReqMsg");
            try{
                param.uploadPreRes = JSON.parse(dataStr);
                if(param.uploadPreRes){
                    return true;
                }
            }catch (e){

            }
            return false;
        },
        hexStr2Blob:function (hexStr){
            var u8array = new Uint8Array(hexStr.match(/[\da-f]{2}/gi).map(function (h) {
                return parseInt(h, 16)
            }));
            return new Blob([u8array.buffer]);
        },
        blob2Base64:function (blob){
            return new Promise(function (success,error){
                var reader = new FileReader();
                reader.onload = function (e) {
                    success(e.target.result.split(",")[1])
                }
                reader.readAsDataURL(blob);
            });
        },
        getHeaderMap:function (headerStr){
            var phsz = headerStr.split("&");
            var headerMap = {};
            for (let i = 0; i < phsz.length; i++) {
                var ph = phsz[i];
                var isz = ph.split("=");
                var key = isz[0];
                isz.splice(0,1);
                headerMap[key]=isz.join("=");
            }
            return headerMap;
        },
        upload:async function (param,progress){
            var that = this;
            if(param.expName){
                webos.message.error("文件校验中,操作失败");
                return;
            }
            if(!param.uploadPreRes){
                var fpData = that.getFpSizeFpl(param.size);
                param.needFps = fpData.fpSl;
                param.fpSize = fpData.fpSize;
                var simpleHash = await webos.fileSystem.fileHashSimple(param.file);
                param.uploadKey = "upload"+simpleHash+param.path;
                var cache = await webos.fileSystem.getCacheValue(param.uploadKey);
                if(cache && cache.time > Date.now() - 60*60*1000){
                    param.uploadPreRes = cache.uploadPreRes;
                    param.currentFp = cache.currentFp;
                }else{
                    var flag = await that.uploadPreCommon(param);
                    if(!flag){
                        param.status = 3;
                        param.msg = param.lastErrorReqMsg;
                        param.statusChange(param);
                        return;
                    }
                }
            };
            if(!param.checkTransSecond){
                param.checkTransSecond = true;
                new Promise(async function (success,error){
                    //进行秒传检查
                    let sliceMd5;
                    let fileMd5 = await webos.fileSystem.fileMd5(param.file);
                    if(param.needFps > 1){
                        let sliceMd5Plus = "";
                        for(let i=0;i<param.needFps;i++){
                            let part = await that.getFp(i,param);
                            if(i>0){
                                sliceMd5Plus += "\n";
                            }
                            sliceMd5Plus += part.md5.toUpperCase()
                        }
                        sliceMd5 = await webos.fileSystem.fileMd5(new Blob([sliceMd5Plus]));
                    }else{
                        sliceMd5 = fileMd5;
                    }
                    param.sliceMd5Data = sliceMd5;
                    param.fileMd5Data = fileMd5;
                    if(param.hasComplete){
                        await that.checkComplete(param);
                    }else{
                        let postData = {
                            currentType:3,
                            data:{
                                uploadFileId:param.uploadPreRes.uploadFileId,
                                fileMd5:param.fileMd5Data,
                                sliceMd5:param.sliceMd5Data
                            }
                        };
                        let dataStr = await webos.request.commonData("fileSystem", "uploadPre",{path:param.path,expand:JSON.stringify(postData),name:param.filePath});
                        let resData = JSON.parse(dataStr);
                        if(resData.fileDataExists == 1){
                            param.status = 4;
                            if(param.currentXhr){
                                param.currentXhr.abort();
                            }
                            await that.checkComplete(param);
                        }
                    }
                });
            }
            var errMsg;
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
                let urlDataStr = await webos.request.commonData("fileSystem", "uploadUrl",{path:param.path,expand:JSON.stringify({
                        "uploadFileId":param.uploadPreRes.uploadFileId,
                        "partInfo":(i+1)+"-"+ await that.blob2Base64(that.hexStr2Blob(part.md5))
                    }),name:param.filePath});
                let urlData;
                try{
                    urlData = JSON.parse(urlDataStr)["partNumber_"+(i+1)];
                }catch (e){

                }
                if(!urlData){
                    param.status = 3;
                    param.msg = errMsg?errMsg:"分片地址获取失败";
                    param.statusChange(param);
                    return;
                }
                let url = urlData.requestURL;
                let headerMap = that.getHeaderMap(urlData.requestHeader);
                let success = false;
                for(let h=0;h<3;h++){
                    if(param.status != 1){
                        return;
                    }
                    param.currentXhr = new XMLHttpRequest();
                    let xhr = await webos.request.xhrReq("PUT",url,part.fp,progress,
                        headerMap,param.currentXhr);
                    param.currentXhr = undefined;
                    if(param.status == 4){
                        param.status = 4;
                        param.msg = "上传暂停";
                        param.statusChange(param);
                        return;
                    }
                    if(xhr && xhr.status && xhr.status == 200){
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
                    currentFp:param.currentFp+1
                });
            };
            if(param.fileMd5Data){
                await that.checkComplete(param);
            }else{
                param.expName = "合并中";
                param.hasComplete = true;
            };
        },
        checkComplete:async function (param){
            var afterData = {
                uploadFileId:param.uploadPreRes.uploadFileId,
                lazyCheck:1,
                fileMd5:param.fileMd5Data,
                sliceMd5:param.sliceMd5Data
            }
            let checkStr = await webos.request.commonData("fileSystem", "uploadAfter",{path:param.path,expand:JSON.stringify(afterData),name:param.filePath});
            var errMsg = webos.context.get("lastErrorReqMsg");
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