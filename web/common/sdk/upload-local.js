(function (){
    webos.fileSystem.localUpload = {
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
            const fp_hash = await webos.fileSystem.fileMd5(fp);
            return {fp:fp,hash:fp_hash};
        },
        upload:async function (param,progress){
            var that = this;
            if(param.expName){
                webos.message.error("文件合并中,操作失败");
                return;
            }
            if(!param.preHash){
                param.preHash = await webos.fileSystem.fileHashSimple(param.file);
            }
            if(!param.hasCheckHash){
                param.hasCheckHash = true;
                webos.fileSystem.fileMd5(param.file).then(function (fileHash){
                    param.fileHash = fileHash;
                    if(param.hasComplete){
                        that.checkComplete(param);
                    }else{
                        webos.request.commonData("fileSystem", "uploadPre",{path:param.path,expand:JSON.stringify({"file_hash":param.fileHash,"pre_hash":param.preHash}),name:param.filePath}).then(
                            function (dataStr){
                                if(!dataStr){
                                    return;
                                }
                                const uploadPreRes = JSON.parse(dataStr);
                                if(uploadPreRes.has){
                                    if(param.currentXhr){
                                        param.currentXhr.abort();
                                    }
                                    param.status = 2;
                                    param.msg = "上传成功";
                                    param.statusChange(param);
                                    return;
                                }else{
                                    if(param.hasComplete){
                                        that.checkComplete(param);
                                    }
                                }
                            }
                        );
                    }
                });
            }
            var errMsg;
            if(!param.uploadPreRes){
                let dataStr = await webos.request.commonData("fileSystem", "uploadPre",{path:param.path,expand:JSON.stringify({"file_hash":param.fileHash,"pre_hash":param.preHash}),name:param.filePath});
                errMsg = webos.context.get("lastErrorReqMsg");
                if(!dataStr){
                    param.status = 3;
                    param.msg = errMsg?errMsg:"上传出错";
                    param.statusChange(param);
                    return;
                }
                try{
                    param.uploadPreRes = JSON.parse(dataStr);
                }catch (e){
                }
                if(param.uploadPreRes){
                    param.currentFp = param.uploadPreRes.currentFp;
                }
            };
            if(!param.uploadPreRes){
                param.status = 3;
                param.msg = "上传出错";
                param.statusChange(param);
                return;
            };
            if(param.uploadPreRes.has){
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
                let part = await that.getFp(i,param)
                let success = false;
                for(let h=0;h<3;h++){
                    if(param.status != 1){
                        return;
                    }
                    param.currentXhr = new XMLHttpRequest();
                    let xhr = await webos.request.xhrReq("POST",webos.request.getAbsoluteUrl("fileSystem","localFileUpload"),part.fp,progress,
                        {
                            "Content-Type":"",
                            "upload-id":param.uploadPreRes.upload_id,
                            "fp-hash":part.hash,
                            "upload-index":(i+1)
                        },param.currentXhr);
                    param.currentXhr = undefined;
                    if(param.status == 4){
                        param.status = 4;
                        param.msg = "上传暂停";
                        param.statusChange(param);
                        return;
                    }
                    if(xhr && xhr.responseText){
                        let res = JSON.parse(xhr.responseText);
                        if(res && res.data == "1"){
                            success = true;
                            break;
                        }
                    }
                };
                if(!success){
                    param.status = 3;
                    param.msg = "上传出错";
                    param.statusChange(param);
                    return;
                }
            }
            if(param.fileHash){
                await that.checkComplete(param);
            }else{
                param.expName = "合并中";
                param.hasComplete = true;
            }
        },
        checkComplete:async function (param){
            let checkStr = await webos.request.commonData("fileSystem", "uploadAfter",{path:param.path,expand:JSON.stringify({upload_id:param.uploadPreRes.upload_id,file_hash:param.fileHash,fps:param.needFps}),name:param.filePath});
            var errMsg = webos.context.get("lastErrorReqMsg");
            if(checkStr == "1"){
                param.status = 2;
                param.msg = "上传成功";
            }else{
                param.status = 3;
                param.msg = errMsg?errMsg:"上传出错";
            }
            param.expName = "";
            param.statusChange(param);
        }
    }
})()