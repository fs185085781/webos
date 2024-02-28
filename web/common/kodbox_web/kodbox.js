(function (){
    var tmp = {
        base64Encode: function(str) {
            for (var c1, c2, c3, base64EncodeChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/", len = str.length, i = 0, out = ""; i < len; ) {
                if (c1 = 255 & str.charCodeAt(i++),
                i == len) {
                    out = (out += base64EncodeChars.charAt(c1 >> 2)) + base64EncodeChars.charAt((3 & c1) << 4) + "==";
                    break
                }
                if (c2 = str.charCodeAt(i++),
                i == len) {
                    out = (out = (out += base64EncodeChars.charAt(c1 >> 2)) + base64EncodeChars.charAt((3 & c1) << 4 | (240 & c2) >> 4)) + base64EncodeChars.charAt((15 & c2) << 2) + "=";
                    break
                }
                c3 = str.charCodeAt(i++),
                    out = (out = (out = (out += base64EncodeChars.charAt(c1 >> 2)) + base64EncodeChars.charAt((3 & c1) << 4 | (240 & c2) >> 4)) + base64EncodeChars.charAt((15 & c2) << 2 | (192 & c3) >> 6)) + base64EncodeChars.charAt(63 & c3)
            }
            return out
        },
        md5:function(str) {
            return window.md5(str);
        },
        authcode:function(str, operation, key, expiry) {
            var that = this;
            var timeFloat, sec, operation = operation || "DECODE", expiry = expiry || 0, keya = (key = that.md5(key = key || ""),
                that.md5(key.substr(0, 16))), key = that.md5(key.substr(16, 16)), cryptkey = (sec = "DECODE" == operation ? str.substr(0, 4) : (timeStamp = (timeFloat = that.md5((timeStamp = (new Date).getTime(),
                sec = parseInt(timeStamp / 1e3),
                timeFloat ? timeStamp / 1e3 : (timeStamp - 1e3 * sec) / 1e3 + " " + sec))).length - 4,
                timeFloat.substr(timeStamp, 4)),
            keya + that.md5(keya + sec));
            if ("DECODE" == operation)
                str = str.substr(4),
                    strbuf = base64Decode(str);
            else {
                var tmpstr = (expiry = expiry ? expiry + time() : 0).toString();
                if (10 <= tmpstr.length)
                    str = tmpstr.substr(0, 10) + that.md5(str + key).substr(0, 16) + str;
                else {
                    for (var count = 10 - tmpstr.length, i = 0; i < count; i++)
                        tmpstr = "0" + tmpstr;
                    str = tmpstr + that.md5(str + key).substr(0, 16) + str
                }
                strbuf = str
            }
            for (var box = new Array(256), i = 0; i < 256; i++)
                box[i] = i;
            for (var rndkey = new Array, i = 0; i < 256; i++)
                rndkey[i] = cryptkey.charCodeAt(i % cryptkey.length);
            for (var j = i = 0; i < 256; i++) {
                var j = (j + box[i] + rndkey[i]) % 256
                    , tmp = box[i];
                box[i] = box[j],
                    box[j] = tmp
            }
            for (var timeStamp, s = "", strbuf = strbuf.split(""), a = j = i = 0; i < strbuf.length; i++) {
                j = (j + box[a = (a + 1) % 256]) % 256;
                tmp = box[a];
                box[a] = box[j],
                    box[j] = tmp,
                    s += function(s) {
                        return String.fromCharCode(s)
                    }(strbuf[i].charCodeAt() ^ box[(box[a] + box[j]) % 256])
            }
            return s = "DECODE" == operation ? (0 == s.substr(0, 10) || 0 < s.substr(0, 10) - time()) && s.substr(10, 16) == that.md5(s.substr(26) + key).substr(0, 16) ? s.substr(26) : "" : (s = that.base64Encode(s),
                timeStamp = new RegExp("=","g"),
            sec + (s = s.replace(timeStamp, "")))
        },
        authEncode:function(str, key, expiry) {
            str = encodeURIComponent(str);
            str = this.authcode(str, "ENCODE", key, expiry);
            return str = (str = (str = str.replace(/\+/g, "-")).replace(/\//g, "_")).replace(/=/g, ".");
        },
        roundFromTo:function(start, end) {
            end -= --start,
                end = Math.ceil(Math.random() * end + start);
            return 0 == end ? 0 : end
        },
        roundString:function(len) {
            var result = ""
                , charArr = "01234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
            len = len || 5;
            for (var i = 0; i < len; i++) {
                var index = this.roundFromTo(0, charArr.length - 1);
                result += charArr.charAt(index)
            }
            return result
        }
    }
    window.kodbox = {
        createCsrf:function (){
            return tmp.roundString(16);
        },
        encryPassword:function (e){
            var t = tmp.roundString(5);
            var a = "2&$%@(*@(djfhj1923"
            return t + tmp.authEncode(e, t + a)
        }
    }
})()