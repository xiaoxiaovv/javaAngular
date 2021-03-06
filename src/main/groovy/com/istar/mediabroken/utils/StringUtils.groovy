package com.istar.mediabroken.utils

import groovy.util.logging.Slf4j
import org.apache.http.protocol.HTTP

import java.util.regex.Matcher
import java.util.regex.Pattern;

@Slf4j
public class StringUtils {

    private static final String default_sentence_separator = "[િી—｜🏻🧀🌭=＝/、~.●《》“”（）(),，…。:：？?！!；;\"【】]"

    public static String stripHtml2(String content) {
// <p>段落替换为换行
        content = content.replaceAll('<p .*?>', '\r\n');
// <br><br/>替换为换行
        content = content.replaceAll('<br\\s*/?>', '\r\n');
// 去掉其它的<>之间的东西
        content = content.replaceAll('\\<.*?>', '');
// 还原HTML
        content = content.replaceAll('\r\n', '<br/>')
        return content;
    }


    static Pattern tagPattern = Pattern.compile(/\<.*?>/)
    static Pattern resolveTagPattern = ~/(<p .*?>)|(<br\s*\/?>)|(<\/\s*br\s*\/?>)|(<img.*\/?>)/

    public static String stripHtml(String content) {
        def matcher = tagPattern.matcher(content)
        def sb = new StringBuffer(content.length())
        while (matcher.find()) {
            def tag = matcher.group()

            if (resolveTagPattern.matcher(tag).matches()) {
                matcher.appendReplacement(sb, tag)
            } else {
                matcher.appendReplacement(sb, '')
            }
        }
        matcher.appendTail(sb)
        return sb.toString()
    }

    public static String html2text(String content) {
// <p>段落替换为换行
        content = content.replaceAll('<p .*?>', '\r\n');
// <br><br/>替换为换行
        content = content.replaceAll('<br\\s*/?>', '\r\n');
// 去掉其它的<>之间的东西
        content = content.replaceAll('\\<.*?>', '');
//替换BR
        content = content.replaceAll('<br/>', '\r\n');
//替换空格转义字符
        content = content.replaceAll('&nbsp;', ' ');
//替换@字符
        content = content.replaceAll('&copy;', '@');
        //替换&字符
        content = content.replaceAll('&', " ");
        content = content.replaceAll('gt;', "");
        content = content.replaceAll(':', " ");
        content = content.replaceAll('\\?', " ");
        return content;
    }

    static Pattern imgTagPattern = ~/(<img.*\/?>)/
    static Pattern srcPropertyPattern = ~/src=['"](.*?)['"]/

    public static List extractImgUrl(String content) {
        def matcher = tagPattern.matcher(content)
        def list = []
        while (matcher.find()) {
            def tag = matcher.group()
            if (imgTagPattern.matcher(tag).matches()) {
                def srcMatcher = srcPropertyPattern.matcher(tag)
                if (srcMatcher.find()) {
                    list << srcMatcher.group(1)
                }
            }
        }
        return list
    }

    static Pattern tagPattern2 = Pattern.compile(/<b class='high-light'>.*?<\/b>/)

    public static Map extractHighlight(String content) {
        def matcher = tagPattern2.matcher(content)
        Map result = [:]
        while (matcher.find()) {
            String key = matcher.group()
            String value = key.substring(22, key.length() - 4)
            result.put(key, value)
        }
        return result
    }

    public static String solrHtml2text(String content) {
        content = content.trim()
        content = content.replaceAll("\\\\n", '');
        content = content.replaceAll("\\\\t", '');
        content = content.replaceAll("\\\\r", '');
        return content;
    }

    public static String solrText2Html(String content) {
        content = content.trim()
        content = content.replaceAll("\\\\r\\\\n", "<br/>");
        content = content.replaceAll("\\\\n", "<br/>");
        content = content.replaceAll("\\\\t", "  ");
        content = content.replaceAll("\\\\r", "<br/>");
        content = content.replaceAll("<br/><br/>", "<br/>");
        return content;
    }

    public static String ContentText2Html(String content) {
        content=content.replaceAll("　"," ");
//        content = content.replaceAll("/", "|");
        content = content.replaceAll("\\r\\n\\r\\n", "<br>");
        content = content.replaceAll("\\r\\n", "<br>");
        content = content.replaceAll("\\n", "<br>");
        content = content.replaceAll("\\t", "　");
        content = content.replaceAll("\\r", "<br>");
        content = content.replaceAll("\r", "<br>");
        content = content.replaceAll("\n", "<br>");
        content = content.replaceAll("\\>\\s*\\<", "><")
        content = content.replaceAll("(<br>)+", "<br/>")
        StringBuffer stringBuffer = new StringBuffer()
        content.split("<br/>").each {
            if(it.toString().trim()){
                stringBuffer.append("<p>")
                stringBuffer.append(it)
                stringBuffer.append("</p>")
            }
        }
        return stringBuffer.toString();
    }

    public static String Text2Html(String content) {
        content = content.trim()
        content = content.replaceAll("\\r\\n", "<br/>");
        content = content.replaceAll("\\n", "<br/>");
        content = content.replaceAll("\\t", "");
        content = content.replaceAll("\\r", "<br/>");
        content = content.replaceAll("<br/><br/>", "<br/>");
        return content;
    }

    public static String removeSpaceCode(String content) {
        content = content.trim()
        content = content.replaceAll("&#13;", "");
        content = content.replaceAll("\\<p>[　]+", "<p>");
        content = content.replaceAll("\\<p>\\s*", "<p>");
        return content;
    }

    public static String illegalText2Html(String content) {
        content = content.trim()
        content = content.replaceAll("\\r\\n", "");
        content = content.replaceAll("\\n", "");
        content = content.replaceAll("\\t", "");
        content = content.replaceAll("\\r", "");
        content = content.replaceAll("<br/><br/>", "<br/>");
        return content;
    }

    /**
     * 用单个空格字符替换多个空格字符
     * @param str
     * @return
     */
    public static String replaceMultipleSpace(String str){
        str = str.replaceAll(" ", " ")
        str = str.replaceAll(" +"," ")
        return str
    }

    //判断字符串是不是以数字开头
    public static boolean isStartWithNumber(String str) {
        if (str != null && !"".equals(str)){
            if (str.matches("[0-9]+.*")){
                return true
            }else {
                return false
            }
        }
    }

    //去掉微博正文后缀视频地址 http://t.cn/RrGZA5m
    public static String removeWeiboSuffix(String str) {
        try {
            if (str != null && !"".equals(str)) {
                int index = str.lastIndexOf("http://t.cn")
                if (str.length() == (index + 19)) {
                    str = str.substring(0, index)
                } else {
                    str = str.substring(0, index) + str.substring(index + 19, str.length())
                }
            }
        }catch (IndexOutOfBoundsException e){
            return str
        }
        return str
    }


    public static String decodeStr(String source) {
        def resultStr = ""
        if (source) {
            if (source.contains("%")) {
                try {
                    resultStr = URLDecoder.decode(source, HTTP.UTF_8)
                } catch (UnsupportedEncodingException e) {
                    log.error("decode exception ::: {}", e)
                }
            } else {
                resultStr = source
            }
        }
        return resultStr
    }

    public static String encodeStr(String source) {
        try {
            if (source) {
                return URLEncoder.encode(source, HTTP.UTF_8)
            } else {
                return ''
            }
        } catch (UnsupportedEncodingException e) {
            log.error("encode exception ::: {}", e)
        }
    }

    public static String removeSpecialCode(String str) {
        String regEx = "[\"＂`~!@#%^&*()+=|\${}':;',  　　\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(str);
        return m.replaceAll("").trim();
    }

    //全网监控表达式不能包含特殊字符，除括号外
    public static boolean existSpecialCode(String str) {
        String regEx = "[\"＂`~!@#%^&*+=|\${}':;',,.\\[\\].<>/?~！@#￥%……&*——+|{}【】‘；：”“’。，、？]";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(str);
        return m.find();
    }

    //检查字符串是否只有文本、数字、下划线
    public static boolean isLegitimate(String str) {
        String regEx = "^\\w+\$"
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(str);
        return m.matches()
    }
    //检查字符串是否只有数字中文下划线
    public static boolean isCheckName(String str) {
        String regEx = "[^(a-zA-Z0-9\\\\u4e00-\\\\u9fa5)]"
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(str);
        return m.matches()
    }

    //检查密码的安全度,6-18位 包含数字、字母、下划线中至少两种
    public static boolean securityPwd(String pwd) {
        if (pwd && pwd.length() >= 6 && pwd.length() <= 18) {
            if (isLegitimate(pwd)) {
                //如果只包含数字或字母或下划线
                String numReg = "^[0-9]+\$"
                String wordReg = "^[A-Za-z]+\$"
                String lineReg = "^[_]+\$"
                if (Pattern.compile(numReg).matcher(pwd) || Pattern.compile(wordReg).matcher(pwd) || Pattern.compile(lineReg).matcher(pwd)) {
                    return false
                } else {
                    return true
                }
            } else {
                return false
            }
        } else {
            return false
        }
    }

    /**
     * 过滤文本中的a标签
     * @param str
     * @return
     */
    public static String removeAElement(String str) {
        return str ? str.replaceAll('<a.+href[^>]*>', '').replaceAll('</a>', '') : ''
    }

    public static String isNullOrNot(String str) {
        String strRe = "";
        strRe = (str == null ? "" : html2text(str))
        return strRe
    }

    public static boolean isMobileNumber(String mobiles) {
        return Pattern.compile('^((13[0-9])|(14[0-9])|(15[0-9])|(16[0-9])|(18[0-9])|(17[0-9])|(19[0-9])|(147))\\\\d{8}$').matcher(mobiles).matches();
    }

    /**
     * 切分句子
     * @param document
     * @param sentence_separator
     * @return
     */
    static Set<String> splitSentence(String document, String sentence_separator) {
        def map = [:]

        if (sentence_separator == null || sentence_separator.isEmpty()) {
            sentence_separator = default_sentence_separator
        }
        document = document.replaceAll(" ", "\n").replaceAll("　", "\n")

        for (String line : document.split("[\t\r\n]")) {
            if (line.length() == 0) continue
            for (String sent : line.split(sentence_separator)) {
                if (sent.length() == 0) continue
                for (String str : sent.split("\\s+")) {
                    if (str) {
                        map.put(str, org.apache.commons.lang3.StringUtils.deleteWhitespace(
                                str.replaceAll(" ", "")
                        ).length())
                    }
                }
            }
        }
        def sort = map.sort { -it.value }

        def set = sort.take(3).keySet()

        return set
    }

    /**
     * java生成随机数字和字母组合
     * @param length[生成随机数的长度]
     */
    public static String getCharAndNumr(int length) {
        String val = "";
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            // 输出字母还是数字
            String charOrNum = random.nextInt(2) % 2 == 0 ? "char" : "num";
            // 字符串
            if ("char".equalsIgnoreCase(charOrNum)) {
                // 取得大写字母还是小写字母
                int choice = random.nextInt(2) % 2 == 0 ? 65 : 97;
                val += (char) (choice + random.nextInt(26));
            } else if ("num".equalsIgnoreCase(charOrNum)) { // 数字
                val += String.valueOf(random.nextInt(10));
            }
        }
        return val;
    }

    //生成word 时候url 转义
    public static String replaceSpecialWord(String str){

        if(str){
            return str.replaceAll("&","&amp;").replaceAll("<","&lt;")
            .replaceAll(">","&gt;").replaceAll("\"","&quot;").replaceAll("'","&apos;")
        }else {
            return ""
        }
    }

    public static def getFirstVideoUrl(String htmlStr){
        if( htmlStr == null ){
            return null;
        }

        String video = "";
        String videoSrc = "";
        Pattern p_video;
        Matcher m_video;

        String regEx_video = "<video.*src\\s*=\\s*(.*?)[^>]*?>";
        p_video = Pattern.compile(regEx_video, Pattern.CASE_INSENSITIVE);
        m_video = p_video.matcher(htmlStr);
        while (m_video.find()) {
            video = m_video.group();
            Matcher m = Pattern.compile("src\\s*=\\s*\"?(.*?)(\"|>|\\s+)").matcher(video);

            while (m.find()) {
                videoSrc = m.group(1);
                break;
            }
        }
        return videoSrc;
    }


    public static void main(String[] args) {
        String a = "你说你是个小狗狗，我" +
                "<img src=\"http://p1.img.cctvpic.com/photoworkspace/contentimg/2019/01/14/2019011410220288274.jpg\" alt=\" \" width=\"500\">说我" +
                "说个<p>小<video src=\"http://zhxg-01.oss-cn-beijing.aliyuncs.com/1547778992377/my-first-key1.mp4\"  ></video>猫咪，小喵的</p>"
        def aa = getFirstVideoUrl(a)
        println(aa)
    }
}
