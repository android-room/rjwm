package com.reggie.utils;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

public class QQEmail {
    public static void sendAuthCodeEmail(String email, String authCode) throws EmailException {
        SimpleEmail mail = new SimpleEmail();
        mail.setHostName("smtp.qq.com");//发送邮件的服务器,这个是qq邮箱的，不用修改
        mail.setAuthentication("1502568451@qq.com", "rawgxqcclndphhbg");//第一个参数是对应的邮箱用户名一般就是自己的邮箱第二个参数就是SMTP的密码,我们上面获取过了
        mail.setFrom("1502568451@qq.com","老坛酸菜牛肉面");  //发送邮件的邮箱和发件人
        mail.setSSLOnConnect(false); //使用安全链接
        mail.addTo("2640602053@qq.com");//接收的邮箱
        mail.setSubject("验证码");//设置邮件的主题
        mail.setMsg("尊敬的用户:你好!\n 登陆验证码为:" + authCode+"\n"+"     (有效期为一分钟)");//设置邮件的内容
        mail.send();//发送
    }
}
