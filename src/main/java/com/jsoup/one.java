package com.jsoup;

/**
 * @author liujiahe
 * @create 2020-04-29 12:14
 */
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Example program to list links from a URL.
 */
public class one {
    public static void main(String[] args) throws IOException {
        Connection connect = Jsoup.connect("https://cas.gzhu.edu.cn/cas_server/login");
        Connection.Request request = connect.request();
        Document document = connect.get();

        Map<String,String> sub = new HashMap<String, String>();

        //解析It execution _eventId
        Elements select = document.select(".btn-row").select("input");
        for(int i=0;i<select.size();i++){
            if ("hidden".equals(select.get(i).attr("type"))) {
                String key = select.get(i).attr("name");
                String value = select.get(i).attr("value");
                sub.put(key,value);
            }
        }

        Connection.Response response = connect.response();
        String cookie = response.header("Set-Cookie");
        request.header("Referer",request.url().toString());
        request.addHeader("Cookie",cookie);

        // 获取验证码
        connect.url("https://cas.gzhu.edu.cn/cas_server/captcha.jsp");
        Connection.Response execute = connect.ignoreContentType(true).execute();
        byte[] img = execute.bodyAsBytes();
        ImgUtils.savaImage(img, "F://jsoup//", "code.png");
//        print("\nContext: (%s)",execute.headers().toString());
        String code = ImgUtils.getCode("F://jsoup//code.png");

        System.out.println(code);
        Scanner sc = new Scanner(System.in);
        code = sc.next();
        sc.close();

        // 登录
        connect.url("https://cas.gzhu.edu.cn/cas_server/login");
        connect.data("username", "").data("password", "")
                .data("captcha", code).data("warn", "true")
                .data("submit","登录");
        for (Map.Entry<String, String> entry : sub.entrySet()) {
            connect.data(entry.getKey(),entry.getValue());
            System.out.println(entry.getKey() + "-" + entry.getValue());
        }
        Connection.Response res = connect.ignoreContentType(true).method(Connection.Method.POST).execute();// 执行请求
        //print("\nContext: (%s)",post.toString());
        Map<String, String> cookies = res.cookies();
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            request.cookie(entry.getKey(),entry.getValue());
            System.out.println(entry.getKey() + "-" + entry.getValue());
        }

        //直接进入登录教务系统（必须先登录教务系统才能获取数据）
        request.header("Referer","http://my.gzhu.edu.cn/");
        connect.url("http://jwxt.gzhu.edu.cn/sso/lyiotlogin");
        Document document1 = connect.get();
//        System.out.println(document1.html());
        res = connect.response();// 执行请求
        //print("\nContext: (%s)",post.toString());
        for (Map.Entry<String, String> entry : res.cookies().entrySet()) {
            // 这里切不切换cookie都可以获取成绩，可能后端两个cookie绑定了
            if (request.hasCookie(entry.getKey())){
                request.removeCookie(entry.getKey());
            }
            request.cookie(entry.getKey(),entry.getValue());
            System.out.println(entry.getKey() + ":  " + entry.getValue());
        }

        //获取成绩
        request.header("Referer","http://jwxt.gzhu.edu.cn/jwglxt/xtgl/index_initMenu.html");
        connect.url("http://jwxt.gzhu.edu.cn/jwglxt/cjcx/cjcx_cxDgXscj.html?doType=query");
        connect.data("xnm", "2019").data("xqm", "3")
                .data("_search", "false").data("time", "1").data("queryModel.showCount", "20")
                .data("queryModel.currentPage", "1").data("queryModel.sortName", "").data("queryModel.soryOrder", "asc");
        res = connect.ignoreContentType(true).method(Connection.Method.POST).execute();// 执行请求

        JSONObject json = JSONObject.parseObject(res.body());

        JSONArray jsonArray = (JSONArray)json.getJSONArray("items");
        for(int i=0;i<jsonArray.size();i++){
            // 遍历 jsonarray 数组，把每一个对象转成 json 对象
            JSONObject job = jsonArray.getJSONObject(i);
            // 得到 每个对象中的属性值
            System.out.println(job.get("kcmc")+":  "+job.get("cj")) ;
        }

        //查看是否登录成功
//        connect.url("http://my.gzhu.edu.cn/");
//        Document document1 = connect.get();
//        System.out.println(document1.html());
    }

    public static void main1(String[] args) throws IOException {
        Connection connect = Jsoup.connect("https://cas.gzhu.edu.cn/cas_server/login");
        Connection.Request request = connect.request();
        Document document = connect.get();
        //解析It execution _eventId
        Elements select = document.select(".btn-row").select("input");
        for(int i=0;i<select.size();i++){
            String text=select.get(i).attr("name");
            System.out.println(text);
            text=select.get(i).attr("value");
            System.out.println(text);
        }
    }

    public static void GetAllLink(String url) throws IOException {
        //String url = "http://www.xinhuanet.com/english/list/china-business.htm";
        print("Fetching %s...", url);

        Document doc = Jsoup.connect(url).get();
        Elements links = doc.select("a[href]");
        Elements media = doc.select("[src]");

        print("\nMedia: (%d)", media.size());
        for (Element src : media) {
            if (src.tagName().equals("img"))
                print(" * %s: <%s> %sx%s (%s)",
                        src.tagName(), src.attr("abs:src"), src.attr("width"), src.attr("height"),
                        trim(src.attr("alt"), 20));
            else
                print(" * %s: <%s>", src.tagName(), src.attr("abs:src"));
        }

        print("\nLinks: (%d)", links.size());
        for (Element link : links) {
            print(" * a: <%s>  (%s)", link.attr("abs:href"), trim(link.text(), 35));
        }
    }

    private static void print(String msg, Object... args) {
        System.out.println(String.format(msg, args));
    }

    private static String trim(String s, int width) {
        if (s.length() > width)
            return s.substring(0, width-1) + ".";
        else
            return s;
    }
}
