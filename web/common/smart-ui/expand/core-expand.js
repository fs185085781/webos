(function(){
    "use strict";
    /**
     * 1.封装时间处理,包含时间转化,时间格式化,时间加减,时间求差
     * 2.封装数值处理,包含数值格式化,数值精准计算
     */
    /**时间拓展----开始*/
    function numToMonth(m,flag){
        function numToStr(i){
            switch (i) {
                case 1:return "一";
                case 2:return "二";
                case 3:return "三";
                case 4:return "四";
                case 5:return "五";
                case 6:return "六";
                case 7:return "七";
                case 8:return "八";
                case 9:return "九";
                case 10:return "十";
                case 11:return "十一";
                case 12:return "十二";
            }
            return "NaN";
        }
        if(flag){
            return numToStr(m)+"月";
        }else{
            return m+"月";
        }
    }
    function numToWeek(w,flag){
        function numToStr(i){
            switch (i) {
                case 0:return "日";
                case 1:return "一";
                case 2:return "二";
                case 3:return "三";
                case 4:return "四";
                case 5:return "五";
                case 6:return "六";
            }
            return "NaN";
        }
        var r = numToStr(w);
        if(flag == 1){
            r = "星期"+r;
        }else if(flag == 2){
            r = "周"+r;
        }
        return r;
    }
    function numToQuarter(m,flag){
        if(m>9){
            return flag?"四季度":"Q4";
        }else if(m>6){
            return flag?"三季度":"Q3";
        }else if(m>3){
            return flag?"二季度":"Q2";
        }else{
            return flag?"一季度":"Q1";
        }
    }
    function numToHealthyYear(m){
        return m>6?"下半年":"上半年";
    }
    var dateUtils = {
        formatDate:function(date,format){
            if(!date || !(date instanceof Date)){
                return "";
            }
            if(!format){
                format = "yyyy-MM-dd HH:mm:ss";
            }
            var yyyy = date.getFullYear();
            var data = {
                y:yyyy - parseInt(yyyy/100)*100,
                M:date.getMonth()+1,
                d:date.getDate(),
                H:date.getHours(),
                h:date.getHours()>12?(date.getHours()-12):date.getHours(),
                m:date.getMinutes(),
                s:date.getSeconds(),
                f:date.getMilliseconds()
            };
            var sfs = ["y","M","d","H","h","m","s","f"];
            for(var i=0;i<sfs.length;i++){
                var key = sfs[i];
                data[key+key] = data[key]<10?"0"+data[key]:""+data[key];
            }
            data.MMM = numToMonth(data.M);
            data.MMMM = numToMonth(data.M,true);
            data.ddd = numToWeek(date.getDay());
            data.dddd = numToWeek(date.getDay(),1);
            data.ddddd = numToWeek(date.getDay(),2);
            data.yyyy = yyyy;
            data.fff = data.f<100?(data.f<10?"00"+data.f:"0"+data.f):data.f;
            data.tt = data.H>=12?"下午":"上午";
            data.t = data.H>=12?"PM":"AM";
            data.Q=numToQuarter(data.M,true);
            data.q=numToQuarter(data.M);
            data.hy=numToHealthyYear(data.M);
            var fields = ["yyyy","yy","hy","y","Q","q","MMMM","MMM","MM","M","ddddd","dddd","ddd","dd","d","HH","H","hh","h","mm","m","ss","s","fff","ff","f","tt","t"];
            for(var i=0;i<fields.length;i++){
                var field = fields[i];
                format = format.replace(new RegExp(field,"g"),data[field]);
            }
            return format;
        },
        parseDate:function(str, ignoreTimeZone){
            try {
                var date = eval(str);
                if(date && date.getFullYear){
                    return date;
                }
            }catch(e){}
            if(typeof str == "object"){
                return isNaN(str) ? null : str;
            }
            if (typeof str == "number") {
                var date = new Date(str);
                if (date.getTime() != str){
                    return null;
                }
                return isNaN(date) ? null : date;
            }
            if (typeof str == "string") {
                var m = str.match(/^([0-9]{4})([0-9]{2})([0-9]{0,2})$/);
                if (m) {
                    var date = new Date(parseInt(m[1], 10), parseInt(m[2], 10) - 1);
                    if (m[3]){
                        date.setDate(m[3]);
                    }
                    return date;
                }
                m = str.match(/^([0-9]{4}).([0-9]*)$/);
                if (m) {
                    var date = new Date(m[1], m[2] - 1);
                    return date;
                }
                if (str.match(/^\d+(\.\d+)?$/)) {
                    var ms = parseInt(str);
                    var date = new Date(ms);
                    if(date.getTime() != ms) {
                        return null;
                    }
                    return isNaN(date) ? null : date;
                }
                if (ignoreTimeZone === undefined){
                    ignoreTimeZone = true;
                }
                var date = this.parseISO8601(str,ignoreTimeZone) || (str ? new Date(str) : null);
                return isNaN(date) ? null : date;
            }
            return null;
        },parseISO8601:function(str, ignoreTimeZone) {
            var m = str.match(/^([0-9]{4})([-\/]([0-9]{1,2})([-\/]([0-9]{1,2})([T ]([0-9]{1,2}):([0-9]{1,2})(:([0-9]{1,2})(\.([0-9]+))?)?(Z|(([-+])([0-9]{2})(:?([0-9]{2}))?))?)?)?)?$/);
            if (!m) {
                m = str.match(/^([0-9]{4})[-\/]([0-9]{2})[-\/]([0-9]{2})[T ]([0-9]{1,2})/);
                if (m) {
                    var date = new Date(m[1], m[2] - 1, m[3], m[4]);
                    return date;
                }
                m = str.match(/^([0-9]{4}).([0-9]*)$/);
                if (m) {
                    var date = new Date(m[1], m[2] - 1);
                    return date;
                }
                m = str.match(/^([0-9]{4}).([0-9]*).([0-9]*)/);
                if (m) {
                    var date = new Date(m[1], m[2] - 1, m[3]);
                    return date;
                }
                m = str.match(/^([0-9]{2})-([0-9]{2})-([0-9]{4})$/);
                if (!m) {
                    return null;
                }else {
                    var date = new Date(m[3], m[1] - 1, m[2]);
                    return date;
                }
            }
            var date = new Date(m[1], 0, 1);
            if (ignoreTimeZone || !m[14]) {
                var tempDate = new Date(m[1], 0, 1, 9, 0);
                if (m[3]) {
                    date.setMonth(m[3] - 1);
                    tempDate.setMonth(m[3] - 1);
                }
                if (m[5]) {
                    date.setDate(m[5]);
                    tempDate.setDate(m[5]);
                }
                this.fixDate(date, tempDate);
                if (m[7]){ date.setHours(m[7]);}
                if (m[8]){ date.setMinutes(m[8]);}
                if (m[10]){ date.setSeconds(m[10]);}
                if (m[12]){ date.setMilliseconds(Number("0." + m[12]) * 1000);}
                this.fixDate(date, tempDate);
            } else {
                date.setUTCFullYear(m[1], m[3] ? m[3] - 1 : 0, m[5] || 1);
                date.setUTCHours(m[7] || 0, m[8] || 0, m[10] || 0, m[12] ? Number("0." + m[12]) * 1000 : 0);
                var num = Number(m[16]) * 60 + (m[18] ? Number(m[18]) : 0);
                num *= m[15] == "-" ? 1 : -1;
                date = new Date(+date + (num * 60 * 1000))
            }
            return date;
        },
        fixDate:function(date, tempDate) {
            if (!(+date)){
                return;
            }
            while (date.getDate() != tempDate.getDate()){
                date.setTime(+date + (date < tempDate ? 1 : -1) * 3600000);
            }
        }
    };
    Date.prototype.addMilliseconds = function(x){
        var v = this.getTime() + x;
        return new Date(v);
    };
    Date.prototype.addMonths = function(x){
        var months = this.getFullYear()*12+this.getMonth()+x+1;
        var date = this.getDate();
        var year = parseInt(months/12);
        var temp = new Date(year,months-year*12,1,this.getHours(),this.getMinutes(),this.getSeconds(),this.getMilliseconds());
        var data = temp.addDays(-1);
        if(date<data.getDate()){
            data.setDate(date);
        }
        return data;
    };
    var dateMethods = [
        {prop:"addSeconds",parentProp:"addMilliseconds",pow:1000},
        {prop:"addMinutes",parentProp:"addSeconds",pow:60},
        {prop:"addHours",parentProp:"addMinutes",pow:60},
        {prop:"addDays",parentProp:"addHours",pow:24},
        {prop:"addYears",parentProp:"addMonths",pow:12}];
    for(var i=0;i<dateMethods.length;i++){
        var temp = dateMethods[i];
        (function(map){
            Date.prototype[map.prop] = function(x){
                return this[map.parentProp](x*map.pow);
            }
        })(temp);
    }
    var betweenMethods = [
        {prop:"betweenMilliseconds",pow:1},
        {prop:"betweenSeconds",pow:1000},
        {prop:"betweenMinutes",pow:1000*60},
        {prop:"betweenHours",pow:1000*60*60},
        {prop:"betweenDays",pow:1000*60*60*24}
    ];
    /**根据回调获取差值*/
    function getCha(d1,d2,callback) {
        var max,min;
        if(d1.getTime()>=d2.getTime()){
            max = d1;
            min = d2;
        }else{
            max = d2;
            min = d1;
        }
        var cha = callback(max,min);
        return {max:max,min:min,cha:cha};
    }
    /**比较日期+小时+分钟+秒+毫秒的大小*/
    function isMax(d1,d2) {
        var max,min;
        if(d1.getTime()>=d2.getTime()){
            max = d1;
            min = d2;
        }else{
            max = d2;
            min = d1;
        }
        var xms= max.getDate()*24*60*60*1000+max.getHours()*60*60*1000+max.getMinutes()*60*1000+max.getSeconds()*1000+max.getMilliseconds();
        var tms= min.getDate()*24*60*60*1000+min.getHours()*60*60*1000+min.getMinutes()*60*1000+min.getSeconds()*1000+min.getMilliseconds();
        return xms>=tms;
    }
    for(var i=0;i<betweenMethods.length;i++){
        var temp = betweenMethods[i];
        (function(map){
            Date.prototype[map.prop] = function(x){
                var options = getCha(this,x,function (max,min) {
                    return max.getTime() - min.getTime();
                });
                return parseInt(options.cha/map.pow);
            }
        })(temp);
    }
    Date.prototype.betweenMonths=function(x){
        var options = getCha(this,x,function (max,min) {
            return max.getFullYear()*12+max.getMonth()-min.getFullYear()*12-min.getMonth();
        });
        if(isMax(options.min,options.max)){
            return options.cha;
        }
        return  options.cha - 1;
    }
    Date.prototype.betweenYears=function(x){
        var options = getCha(this,x,function (max,min) {
            return max.getFullYear()-min.getFullYear();
        });
        if(options.max.getMonth()> options.min.getMonth()){
            return options.cha;
        }
        if(options.max.getMonth()<options.min.getMonth()){
            return options.cha-1;
        }
        if(isMax(options.min,options.max)){
            return options.cha;
        }
        return  options.cha - 1;
    }
    Date.prototype.format = function(format){
        return dateUtils.formatDate(this,format);
    };
    Date.parseDate = function(str,ignoreTimeZone){
        if(Date.parse){
            var number = Date.parse(str);
            if(!isNaN(number)){
                return new Date(number);
            }
        }
        return dateUtils.parseDate(str,ignoreTimeZone);
    };
    /**时间拓展----结束*/
    /**数值拓展----开始*/
    var numberUtils = {
        formatNumber:function(number, format){
            function joinZeroStr(nums) {
                var str = "";
                while(nums>0) {
                    nums -= 1;
                    str += "0"
                }
                return str;
            }
            function fillZeroPosition(value, fixedNums, isRightFill) {
                value = value + "";
                fixedNums = typeof fixedNums == "number" ? fixedNums : 2;
                var fillNums = fixedNums - value.length;
                if (fillNums > 0) {
                    var str = joinZeroStr(fillNums);
                    return isRightFill? value + str : str + value;
                }
                return value;
            }
            function getFormatValue(value, format, formatMap) {
                var regExp = /[^0#]/g;
                value = Math.abs(value);
                var hasDh = format.indexOf(",") != -1;
                var formatSplit = format.split(".");
                var integralFormat = (formatSplit[0] || "").replace(regExp, "");
                var fractionalFormat = (formatSplit[1] || "").replace(regExp, "");
                var formatValue = "";
                var decimalsSeparator = formatMap.decimalsSeparator;
                var groupSeparator = formatMap.groupSeparator;
                var hasZero = integralFormat.indexOf("0");
                integralFormat = hasZero == -1 ? "0" : (integralFormat.substr(hasZero) || "0");
                var fractionalLength = fractionalFormat.length;
                var fillZreoLenth = fractionalFormat.substr(0, fractionalFormat.lastIndexOf("0") + 1).length;
                function numRound(number, fractionDigits) {
                    return Math.round(number * Math.pow(10, fractionDigits))/Math.pow(10, fractionDigits);
                }
                value = numRound(value, fractionalLength);
                var valueSz = String(value).split(".");
                var integralValue = valueSz[0];
                var fractionalValue = valueSz[1] || "";
                if (integralValue) {
                    integralValue = fillZeroPosition(integralValue, integralFormat.length);
                    if (hasDh) {
                        for(var i = 0; i < Math.floor((integralValue.length - (1 + i)) / 3); i++){
                            integralValue = integralValue.substring(0, integralValue.length - (4 * i + 3)) + groupSeparator + integralValue.substring(integralValue.length - (4 * i + 3));
                        }
                    }
                    formatValue += integralValue
                }
                if (fractionalLength > 0) {
                    fractionalValue = fillZeroPosition(fractionalValue.substr(0, fractionalLength), fillZreoLenth, true);
                    if (fractionalValue !== "") {
                        formatValue += decimalsSeparator;
                        formatValue += fractionalValue;
                    }
                }
                return formatValue;
            }
            function getFormatIndex(format) {
                if (!format) {
                    return null;
                }
                function getZeroIndexs(format) {
                    var zeroIndex = format.indexOf("0");
                    var hashIndex = format.indexOf("#");
                    if (zeroIndex == -1 || (hashIndex != -1 && hashIndex < zeroIndex)) {
                        zeroIndex = hashIndex;
                    }
                    var lastZeroIndex = format.lastIndexOf("0");
                    var lastHashIndex = format.lastIndexOf("#");
                    if (lastZeroIndex == -1 || (lastHashIndex != -1 && lastHashIndex > lastZeroIndex)) {
                        lastZeroIndex = lastHashIndex;
                    }
                    return [zeroIndex, lastZeroIndex];
                }
                var zeroIndexs = getZeroIndexs(format);
                var zeroIndex = zeroIndexs[0];
                var lastZeroIndex = zeroIndexs[1];
                return zeroIndex > -1 ? {begin: zeroIndex, end: lastZeroIndex, format: format.substring(zeroIndex, lastZeroIndex + 1)} : null
            }
            function hasZeroOrHash(value) {
                return value.indexOf("0") != -1 || value.indexOf("#") != -1;
            }
            function getCommonFormat(format, number, formatMap, formatSz) {
                var ncpReg = /^(n|c|p)(\d*)$/i;
                var formatAttrs = formatMap.number;
                var ncpRes = ncpReg.exec(format);
                if (ncpRes != null) {
                    var ncpValue = ncpRes[1];
                    var ncpCount = ncpRes[2];
                    if (ncpValue == "p") {
                        formatAttrs = formatMap.percent;
                    } else if (ncpValue == "c") {
                        formatAttrs = formatMap.currency;
                    }
                    var pointCount = ncpCount ? parseInt(ncpCount) : formatAttrs.decimals;
                    var plusMinusFormat = formatAttrs.pattern[number < 0 ? 1 : 0];
                    plusMinusFormat = plusMinusFormat.replace("n", "#,#" + (pointCount > 0 ? "." + joinZeroStr(pointCount) : ""));
                    format = format.replace(ncpValue + ncpCount, plusMinusFormat).replace("$", formatMap.currency.symbol).replace("%", formatMap.percent.symbol);
                } else if (hasZeroOrHash(format)){
                    if (number < 0 && !formatSz[1]) {
                        format = "-" + format;
                    }
                }
                return format;
            }
            var indexNumberReg = /^(e)(\d*)$/i;
            if (typeof number != "number") {
                return "";
            }
            if (!format) {
                return String(number);
            }
            var formatSz = format.split(";");
            format = formatSz[0];
            if (number < 0 && formatSz.length >= 2) {
                format = formatSz[1];
            }
            if (number == 0 && formatSz.length >= 3) {
                format = formatSz[2];
            }
            var formatMap = {"number":{"pattern":["n","-n"],"decimals":2,"decimalsSeparator":".","groupSeparator":",","groupSize":[3]},"percent":{"pattern":["n%","-n%"],"decimals":2,"decimalsSeparator":".","groupSeparator":",","groupSize":[3],"symbol":"%"},"currency":{"pattern":["$n","$-n"],"decimals":2,"decimalsSeparator":".","groupSeparator":",","groupSize":[3],"symbol":"¥"}};
            var numberFormat = formatMap.number;
            var percentFormat = formatMap.percent;
            var currencyFormat = formatMap.currency;
            var commonFormat = getCommonFormat(format, number, formatMap, formatSz);
            var hasCurrencySymbol = commonFormat.indexOf(currencyFormat.symbol) != -1;
            var hasPercentSymbol = commonFormat.indexOf(percentFormat.symbol) != -1;
            var hasZeroHash = hasZeroOrHash(commonFormat);
            var realFormat = hasCurrencySymbol ? currencyFormat : (hasPercentSymbol ? currencyFormat : numberFormat);
            var formatNumber = hasPercentSymbol ? number * 100 : number;
            var indexNumberRes = indexNumberReg.exec(commonFormat);
            if (indexNumberRes) {
                var indexCount = parseInt(indexNumberRes[2]);
                return isNaN(indexCount) ? formatNumber.toExponential() : formatNumber.toExponential(indexCount);
            }
            if (!hasZeroHash) {
                return commonFormat;
            }
            var formatValue = "";
            var formatIndex = getFormatIndex(commonFormat);
            if (formatIndex != null) {
                formatValue = getFormatValue(formatNumber, formatIndex.format, realFormat);
                formatValue = commonFormat.substr(0, formatIndex.begin) + formatValue + commonFormat.substr(formatIndex.end + 1);
            } else {
                formatValue = commonFormat;
            }
            return formatValue;
        }
    };
    Number.prototype.format = function(format){
        return numberUtils.formatNumber(this,format);
    };
    /**数值拓展----结束*/
    /**精准计算拓展---开始*/
    var numberMethods = ["floor","round","ceil"];
    for(var i=0;i<numberMethods.length;i++){
        var field = numberMethods[i];
        (function(f){
            Number.prototype[f] = function(x){
                if(x<0){
                    x = 0;
                }
                var pow = Math.pow(10,x);
                var temp = Math[f](this*pow);
                return temp/pow;
            }
        })(field);
    };
    var decimalMethods = ["add","subtract","multiply","divide"];
    for(var i=0;i<decimalMethods.length;i++){
        var field = decimalMethods[i];
        (function(f){
            Number.prototype[f] = function(x,maxJd,type){
                if(isNaN(x)){
                    throw x+" is not a number";
                }
                if(typeof x != "number"){
                    x = x*1;
                }
                if(isNaN(maxJd)){
                    console.warn(maxJd+" is not a number,will be reset to 16");
                    maxJd = 16;
                }
                if((!maxJd && maxJd != 0) || maxJd>16){
                    maxJd = 16;
                }
                type = type || "round";
                if(type != "floor" && type != "round" && type != "ceil"){
                    console.warn(type+" is not a number,will be reset to 'round'");
                    type = "round";
                }
                function getAssist(v){
                    var str = String(v);
                    var eindex = str.indexOf("e");
                    var addres = 0;
                    if(eindex != -1){
                        addres = str.substring(eindex+1)*-1;
                        str = str.substring(0,eindex);
                    }
                    var index = str.indexOf(".");
                    var res = index != -1?str.length - index -1:0;
                    res = res+addres;
                    if(res>maxJd){
                        res = maxJd;
                    }
                    if(res<0){
                        res = 0;
                    }
                    var pow = Math.pow(10,res);
                    var num = v[type](res);
                    return {pow:pow,num:num};
                }
                var a = getAssist(this);
                var b = getAssist(x);
                var max = a.pow>b.pow?a.pow:b.pow;
                var jg = NaN;
                if(f=="add"){
                    jg = (a.num*max+b.num*max)/max;
                }else if(f=="subtract"){
                    jg = (a.num*max-b.num*max)/max;
                }else if(f=="multiply"){
                    jg = (a.num*a.pow)*(b.num*b.pow)/(a.pow*b.pow);
                }else if(f=="divide"){
                    jg = (a.num*max)/(b.num*max);
                }
                return jg;
            }
        })(field);
    }
    /**精准计算拓展---结束*/
})();