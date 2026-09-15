package com.example.service;

import cn.hutool.core.util.StrUtil;
import com.example.common.enums.AlarmLevelEnum;
import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Account;
import com.example.entity.Alarm;
import com.example.entity.PersonMailAddress;
import com.example.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.mail.internet.MimeMessage;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 告警邮件：测试信只用 SMTP / alarm.mail 默认值；正式告警只读人员资料邮箱。
 */
@Service
public class AlarmMailService {

    private static final Logger log = LoggerFactory.getLogger(AlarmMailService.class);

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "alarm-mail");
        t.setDaemon(true);
        return t;
    });

    @Resource
    private JavaMailSender mailSender;
    @Resource
    private PersonMailService personMailService;

    @Value("${alarm.mail.enabled:true}")
    private boolean enabled;

    /** 仅超级管理员连通性测试使用，正式告警不得回落到此值 */
    @Value("${alarm.mail.to:yuxuan236@yeah.net}")
    private String testTo;

    /** SMTP 登录账号，只用于传输认证，不作为测试/正式邮件的展示发件人 */
    @Value("${spring.mail.username:}")
    private String smtpUser;

    @Value("${spring.mail.password:}")
    private String password;

    public boolean isTransportReady() {
        return enabled && StrUtil.isNotBlank(smtpUser) && StrUtil.isNotBlank(password);
    }

    /**
     * 正式告警：发件人/收件人取人员资料，INFO 不发；缺邮箱或失败只打日志。
     */
    public void notifyNewAlarm(Alarm alarm, Account operator) {
        if (alarm == null || AlarmLevelEnum.INFO.name().equals(alarm.getLevel())) {
            return;
        }
        if (!isTransportReady()) {
            log.warn("告警邮件未发送：未配置 MAIL_PASSWORD 或已关闭 alarm.mail.enabled");
            return;
        }
        String from = personMailService.emailOf(operator);
        if (StrUtil.isBlank(from)) {
            log.warn("告警邮件未发送：登记人未在人员资料中配置邮箱 alarmNo={}", alarm.getAlarmNo());
            return;
        }
        List<PersonMailAddress> recipients = personMailService.notifyRecipients(operator);
        if (recipients.isEmpty()) {
            log.warn("告警邮件未发送：没有可通知的人员邮箱 alarmNo={}", alarm.getAlarmNo());
            return;
        }
        String fromName = personMailService.displayName(operator);
        executor.execute(() -> {
            try {
                send(from, fromName, recipients, subject(alarm), body(alarm));
                log.info("告警邮件已发送 from={} to={} alarmNo={}", from,
                        recipients.stream().map(PersonMailAddress::display).collect(java.util.stream.Collectors.toList()),
                        alarm.getAlarmNo());
            } catch (Exception e) {
                log.error("告警邮件发送失败 alarmNo={}", alarm.getAlarmNo(), e);
            }
        });
    }

    /**
     * 超级管理员连通性测试：发件人取超管人员邮箱，收件人用 alarm.mail.to。
     */
    public Map<String, String> sendTest(Account admin) {
        if (!enabled || StrUtil.isBlank(smtpUser) || StrUtil.isBlank(testTo)) {
            throw new BusinessException(ResultCodeEnum.MAIL_NOT_CONFIGURED_ERROR);
        }
        if (StrUtil.isBlank(password)) {
            throw new BusinessException(ResultCodeEnum.MAIL_NOT_CONFIGURED_ERROR);
        }
        String from = personMailService.emailOf(admin);
        if (StrUtil.isBlank(from)) {
            throw new BusinessException(ResultCodeEnum.MAIL_PERSON_EMAIL_MISSING);
        }
        String senderName = "超级管理员";
        try {
            send(from, senderName,
                    java.util.Collections.singletonList(new PersonMailAddress("连通性测试", testTo)),
                    "【DevOps 告警测试】邮箱通道验证",
                    "这是超级管理员发起的连通性测试邮件。\n"
                            + "若你能看到这封信，说明 SMTP 通道可用。\n"
                            + "正式告警不会使用该测试邮箱配置，只会发送到人员资料中填写的邮箱。");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("测试告警邮件发送失败 to={}", testTo, e);
            throw new BusinessException(ResultCodeEnum.MAIL_SEND_ERROR);
        }
        Map<String, String> result = new LinkedHashMap<>();
        result.put("from", from);
        result.put("to", testTo);
        result.put("senderName", senderName);
        return result;
    }

    static String subject(Alarm alarm) {
        String level = levelLabel(alarm.getLevel());
        String app = StrUtil.blankToDefault(alarm.getAppName(), "未指定应用");
        return "【DevOps 告警】" + level + " " + app;
    }

    static String body(Alarm alarm) {
        return "运维管理系统产生新告警，请及时接手。\n\n"
                + "编号：" + StrUtil.blankToDefault(alarm.getAlarmNo(), "-") + "\n"
                + "级别：" + levelLabel(alarm.getLevel()) + "\n"
                + "应用：" + StrUtil.blankToDefault(alarm.getAppName(), "-") + "\n"
                + "服务器：" + StrUtil.blankToDefault(alarm.getServerName(), "-") + "\n"
                + "部署单：" + StrUtil.blankToDefault(alarm.getDeployNo(), "-") + "\n"
                + "内容：" + StrUtil.blankToDefault(alarm.getContent(), "-") + "\n"
                + "时间：" + StrUtil.blankToDefault(alarm.getCreateTime(), "-") + "\n";
    }

    static String levelLabel(String level) {
        if (AlarmLevelEnum.CRITICAL.name().equals(level)) {
            return "严重";
        }
        if (AlarmLevelEnum.WARNING.name().equals(level)) {
            return "警告";
        }
        if (AlarmLevelEnum.INFO.name().equals(level)) {
            return "提示";
        }
        return StrUtil.blankToDefault(level, "-");
    }

    /**
     * 收件人、回复地址用人员表邮箱。网易 SMTP 的 553 要求信封 From 必须等于授权登录账号，
     * 不能把人员表里的 QQ/其它邮箱写进 MAIL FROM，否则信发不出去。
     */
    private void send(String fromEmail, String fromName, List<PersonMailAddress> recipients, String subject, String text) throws Exception {
        MimeMessage mime = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
        String senderName = StrUtil.blankToDefault(fromName, "运维管理系统");
        helper.setFrom(smtpUser, senderName);
        if (StrUtil.isNotBlank(fromEmail) && !PersonMailService.sameAddress(fromEmail, smtpUser)) {
            helper.setReplyTo(fromEmail, senderName);
        }
        for (PersonMailAddress item : recipients) {
            helper.addTo(item.getEmail(), item.personal());
        }
        helper.setSubject(subject);
        helper.setText(text);
        mailSender.send(mime);
    }
}
