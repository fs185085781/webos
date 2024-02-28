(function (){
    webos.fileSystem.kodboxUpload = {
        lastId:0,
        getFpSizeFpl:function (size){
            var fpSize = 5242880;
            var fpSl = size / fpSize;
            fpSl = Math.floor(fpSl);
            if (size % fpSize != 0) {
                fpSl++;
            }
            return {fpSize,fpSl};
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
            param.fpMap["index"+index] = {fp:fp,simpleHash:await webos.fileSystem.fileHashSimple(fp)};
            return param.fpMap["index"+index];
        },
        uploadPreCommon:async function (param){
            var that = this;
            if(param.uploadPreRes){
                return true;
            }
            var fpData = that.getFpSizeFpl(param.size);
            param.needFps = fpData.fpSl;
            param.fpSize = fpData.fpSize;
            let postData = {
                fullPath:param.fullPath,
                name:param.name,
                checkType:"checkHash",
                checkHashSimple:param.preHash,
                size:param.size,
                modifyTime:param.file.lastModified,
                chunkSize:param.fpSize,
                chunks:0
            };
            let dataStr = await webos.request.commonData("fileSystem", "uploadPre",{path:param.path,expand:JSON.stringify(postData),name:param.filePath});
            param.lastErrorReqMsg = webos.context.get("lastErrorReqMsg");
            param.lastSuccessReqMsg = webos.context.get("lastSuccessReqMsg");
            try{
                param.uploadPreRes = JSON.parse(dataStr);
                if(param.uploadPreRes.checkChunkArray.length === 0){
                    param.uploadPreRes.checkChunkArray = {}
                }
                if(param.uploadPreRes){
                    return true;
                }
            }catch (e){

            }
            return false;
        },
        upload:async function (param,progress){
            var that = this;
            if(param.expName){
                webos.message.error("文件合并中,操作失败");
                return;
            };
            if(!param.preHash){
                param.preHash = await webos.fileSystem.fileHashSimple(param.file);
            };
            if(!param.uploadPreRes){
                var flag = await that.uploadPreCommon(param);
                if(!flag){
                    param.status = 3;
                    param.msg = param.lastErrorReqMsg;
                    param.statusChange(param);
                    return;
                }
                param.kodboxId = ++that.lastId;
            };
            if(!param.hasCheckHash){
                param.hasCheckHash = true;
                webos.fileSystem.fileMd5(param.file).then(function (fileHash){
                    param.fileHash = fileHash;
                    that.checkComplete(param,param.hasComplete);
                });
            }
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
                if(param.uploadPreRes.checkChunkArray["part_"+i] == part.simpleHash){
                    continue;
                }
                let success = false;
                const formData = new FormData();
                formData.append("id","WU_FILE_"+param.kodboxId);
                formData.append("name",param.name);
                formData.append("lastModifiedDate",param.file.lastModifiedDate+"");
                formData.append("size",param.size);
                formData.append("chunks",param.needFps);
                formData.append("chunk",i);
                formData.append("fullPath",param.fullPath);
                formData.append("modifyTime",param.file.lastModified);
                formData.append("checkHashSimple",param.preHash);
                formData.append("chunkSize",param.fpSize);
                formData.append("file",part.fp);
                formData.append("API_ROUTE","explorer/upload/fileUpload");
                for(let h=0;h<3;h++){
                    if(param.status != 1){
                        return;
                    }
                    param.currentXhr = new XMLHttpRequest();
                    let xhr = await webos.request.xhrReq("POST",webos.request.getAbsoluteUrl("fileSystem","commonDriveReq"),formData,progress,
                        {
                            "webos-token":webos.request.getTokenStr(),
                            "drive-path":encodeURIComponent(param.path),
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
                        if(res.code == 0){
                            success = true;
                            if(res.data.info){
                                param.uploadComplete = true;
                            }
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
                await that.checkComplete(param,true);
            }else{
                param.expName = "合并中";
                param.hasComplete = true;
            }
        },
        checkComplete:async function (param,flag){
            let postData = {
                fullPath:param.fullPath,
                name:param.name,
                checkType:"matchMd5",
                checkHashSimple:param.preHash,
                checkHashMd5:param.fileHash,
                size:param.size,
                modifyTime:param.file.lastModified,
                chunkSize:param.fpSize,
                chunks:0
            };
            webos.request.commonData("fileSystem", "uploadPre",{path:param.path,expand:JSON.stringify(postData),name:param.filePath}).then(function (dataStr){
                if(dataStr == "1"){
                    if(param.currentXhr){
                        param.currentXhr.abort();
                    }
                    param.status = 2;
                    param.msg = "上传成功";
                    param.statusChange(param);
                }else if(flag){
                    if(param.uploadComplete){
                        param.status = 2;
                        param.msg = "上传成功";
                        param.statusChange(param);
                    }else{
                        param.status = 3;
                        param.msg = "上传失败";
                        param.statusChange(param);
                    }
                }
            });
        }
    }
})()