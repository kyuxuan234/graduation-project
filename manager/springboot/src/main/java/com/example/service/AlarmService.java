package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.example.common.enums.AlarmStatusEnum;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Alarm;
import com.example.exception.BusinessException;
import com.example.mapper.AlarmMapper;
import com.example.utils.AuthUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 告警记录业务处理
 **/
@Service
public class AlarmService {

    private static final Logger log = LoggerFactory.getLogger(AlarmService.class);

    @Resource
    private AlarmMapper alarmMapper;
    @Resource
    private AlarmMailService alarmMailService;

    /**
     * 新增
     */
    public void add(Alarm alarm) {
        Account currentUser = AuthUtils.requireRole(ResultCodeEnum.ALARM_HANDLE_NO_PERMISSION, RoleEnum.OPS);
        if (ObjectUtil.isEmpty(alarm.getAlarmNo())) {
            alarm.setAlarmNo(generateAlarmNo());
        }
        alarm.setStatus(AlarmStatusEnum.UNHANDLED.name());
        alarm.setHandler(null);
        alarm.setHandleTime(null);
        alarm.setResolveTime(null);
        alarmMapper.insert(alarm);
        alarmMailService.notifyNewAlarm(alarm, currentUser);
    }

    /**
     * 探活写入。同一标记还有未关闭的告警时只更新原文，不新建。
     * 已解决且仍在冷却期内也不新建；冷却结束后故障还在，才另开一条并注明故障复发。
     */
    public void raiseAuto(String marker, Integer appId, Integer serverId, String deployNo,
                          String level, String content, int cooldownMinutes) {
        try {
            Alarm latest = alarmMapper.selectLatestByMarker(marker, appId, serverId);
            boolean open = latest != null && (AlarmStatusEnum.UNHANDLED.name().equals(latest.getStatus())
                    || AlarmStatusEnum.HANDLING.name().equals(latest.getStatus()));
            if (open) {
                noteStillFailing(latest);
                return;
            }
            if (latest != null && withinCooldown(latest, cooldownMinutes)) {
                return;
            }
            String text = content;
            if (latest != null) {
                text = content.replace("【自动检测】", "【自动检测】故障复发 ");
                if (!text.contains("【自动检测】")) {
                    text = "【自动检测】故障复发 " + content;
                }
            } else if (!text.contains("【自动检测】")) {
                text = "【自动检测】" + text;
            }
            if (text.length() > 500) {
                text = text.substring(0, 500);
            }
            Alarm alarm = new Alarm();
            alarm.setAlarmNo(generateAlarmNo());
            alarm.setAppId(appId);
            alarm.setServerId(serverId);
            alarm.setDeployNo(deployNo);
            alarm.setLevel(level);
            alarm.setContent(text);
            alarm.setStatus(AlarmStatusEnum.UNHANDLED.name());
            alarmMapper.insert(alarm);
            try {
                alarmMailService.notifyNewAlarm(alarm, null);
            } catch (Exception mailError) {
                log.warn("自动告警邮件未发出 alarmNo={}", alarm.getAlarmNo());
            }
        } catch (Exception e) {
            log.warn("自动告警写入失败: {}", e.getMessage());
        }
    }

    /**
     * 探活恢复时只在内容里补一句，不把状态改成已解决。
     */
    public void markRecovered(String marker, Integer appId, Integer serverId) {
        try {
            Alarm latest = alarmMapper.selectLatestByMarker(marker, appId, serverId);
            if (latest == null) {
                return;
            }
            if (!AlarmStatusEnum.UNHANDLED.name().equals(latest.getStatus())
                    && !AlarmStatusEnum.HANDLING.name().equals(latest.getStatus())) {
                return;
            }
            String content = latest.getContent() == null ? "" : latest.getContent();
            if (content.contains("探活已恢复")) {
                return;
            }
            String note = content + "；探活已恢复 " + DateUtil.now();
            if (note.length() > 500) {
                note = note.substring(0, 500);
            }
            alarmMapper.updateContent(latest.getId(), note);
        } catch (Exception e) {
            log.warn("标注探活恢复失败: {}", e.getMessage());
        }
    }

    /** 同一条未关闭告警上刷新“仍在异常”，避免内容每次探活都变长。 */
    private void noteStillFailing(Alarm latest) {
        String content = latest.getContent() == null ? "" : latest.getContent();
        String note = "仍在异常 " + DateUtil.now();
        int idx = content.indexOf("；仍在异常");
        String next = idx >= 0 ? content.substring(0, idx) + "；" + note : content + "；" + note;
        if (next.length() > 500) {
            next = next.substring(0, 500);
        }
        alarmMapper.updateContent(latest.getId(), next);
    }

    private boolean withinCooldown(Alarm alarm, int cooldownMinutes) {
        if (alarm == null || cooldownMinutes <= 0) {
            return false;
        }
        String stamp = alarm.getResolveTime();
        if (StrUtil.isBlank(stamp)) {
            stamp = alarm.getUpdateTime();
        }
        if (StrUtil.isBlank(stamp)) {
            stamp = alarm.getCreateTime();
        }
        if (StrUtil.isBlank(stamp)) {
            return false;
        }
        try {
            Date closed = DateUtil.parse(stamp);
            return closed.getTime() + cooldownMinutes * 60L * 1000L > System.currentTimeMillis();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 删除
     */
    public void deleteById(Integer id) {
        Alarm dbAlarm = getExistAlarm(id);
        if (AlarmStatusEnum.RESOLVED.name().equals(dbAlarm.getStatus())) {
            throw new BusinessException(ResultCodeEnum.STATUS_LOCKED_ERROR);
        }
        alarmMapper.deleteById(id);
    }

    /**
     * 批量删除
     */
    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            deleteById(id);
        }
    }

    /**
     * 修改
     */
    public void updateById(Alarm alarm) {
        Alarm dbAlarm = getExistAlarm(alarm.getId());
        if (!AlarmStatusEnum.UNHANDLED.name().equals(dbAlarm.getStatus())) {
            throw new BusinessException(ResultCodeEnum.STATUS_LOCKED_ERROR);
        }
        alarm.setStatus(null);
        alarm.setHandler(null);
        alarm.setHandleTime(null);
        alarm.setResolveTime(null);
        alarmMapper.updateById(alarm);
    }

    /**
     * 根据ID查询
     */
    public Alarm selectById(Integer id) {
        return alarmMapper.selectById(id);
    }

    /**
     * 查询所有
     */
    public List<Alarm> selectAll(Alarm alarm) {
        return alarmMapper.selectAll(alarm);
    }

    /**
     * 分页查询
     */
    public PageInfo<Alarm> selectPage(Alarm alarm, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Alarm> list = alarmMapper.selectAll(alarm);
        return PageInfo.of(list);
    }

    /**
     * 接手处理，只有未处理的告警可以接手
     */
    public void handle(Integer id) {
        Account currentUser = AuthUtils.requireRole(ResultCodeEnum.ALARM_HANDLE_NO_PERMISSION, RoleEnum.OPS);
        Alarm dbAlarm = getExistAlarm(id);
        checkStatus(dbAlarm, AlarmStatusEnum.UNHANDLED);

        Alarm updateAlarm = new Alarm();
        updateAlarm.setId(id);
        updateAlarm.setStatus(AlarmStatusEnum.HANDLING.name());
        updateAlarm.setHandler(currentUser.getUsername());
        updateAlarm.setHandleTime(DateUtil.now());
        alarmMapper.updateById(updateAlarm);
    }

    /**
     * 解决，只有处理中的告警可以由接手人解决
     */
    public void resolve(Integer id) {
        Account currentUser = AuthUtils.requireRole(ResultCodeEnum.ALARM_HANDLE_NO_PERMISSION, RoleEnum.OPS);
        Alarm dbAlarm = getExistAlarm(id);
        checkStatus(dbAlarm, AlarmStatusEnum.HANDLING);
        boolean admin = RoleEnum.ADMIN.name().equals(currentUser.getRole());
        if (!admin && ObjectUtil.isNotEmpty(dbAlarm.getHandler()) && !currentUser.getUsername().equals(dbAlarm.getHandler())) {
            throw new BusinessException(ResultCodeEnum.ALARM_NOT_HANDLER_ERROR);
        }

        Alarm updateAlarm = new Alarm();
        updateAlarm.setId(id);
        updateAlarm.setStatus(AlarmStatusEnum.RESOLVED.name());
        updateAlarm.setResolveTime(DateUtil.now());
        alarmMapper.updateById(updateAlarm);
    }

    /**
     * 告警编号：A + yyyyMMddHHmmss + 3位随机数
     */
    private String generateAlarmNo() {
        return "A" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + RandomUtil.randomNumbers(3);
    }

    /**
     * 查询告警，不存在则抛异常
     */
    private Alarm getExistAlarm(Integer id) {
        Alarm dbAlarm = alarmMapper.selectById(id);
        if (ObjectUtil.isNull(dbAlarm)) {
            throw new BusinessException(ResultCodeEnum.ALARM_NOT_EXIST_ERROR);
        }
        return dbAlarm;
    }

    /**
     * 状态流转校验，当前状态与期望状态不一致即为非法跳转
     */
    private void checkStatus(Alarm alarm, AlarmStatusEnum expectStatus) {
        if (!expectStatus.name().equals(alarm.getStatus())) {
            throw new BusinessException(ResultCodeEnum.ALARM_STATUS_ERROR);
        }
    }

}
