package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
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
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 告警记录业务处理
 **/
@Service
public class AlarmService {

    @Resource
    private AlarmMapper alarmMapper;

    /**
     * 新增
     */
    public void add(Alarm alarm) {
        AuthUtils.requireRole(ResultCodeEnum.ALARM_HANDLE_NO_PERMISSION, RoleEnum.OPS);
        if (ObjectUtil.isEmpty(alarm.getAlarmNo())) {
            alarm.setAlarmNo(generateAlarmNo());
        }
        alarm.setStatus(AlarmStatusEnum.UNHANDLED.name());
        alarm.setHandler(null);
        alarm.setHandleTime(null);
        alarm.setResolveTime(null);
        alarmMapper.insert(alarm);
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
