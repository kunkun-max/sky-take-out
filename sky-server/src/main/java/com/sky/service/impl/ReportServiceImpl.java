package com.sky.service.impl;

import com.sky.dto.DateCountDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.commons.lang.StringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)){
            begin=begin.plusDays(1);
            dateList.add(begin);
        }

        List<Double>turnoverList=new ArrayList<>();
        for(LocalDate date:dateList){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map=new HashMap();
            map.put("begin",beginTime);
            map.put("end",endTime);
            map.put("status", Orders.COMPLETED);

            Double turnover=orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }

        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList,","))
                .turnoverList(StringUtils.join(turnoverList,","))
                .build();
    }

    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        //存放从begin到end之间的每天对应的日期
        List<LocalDate>dateList=new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)){
            begin=begin.plusDays(1);
            dateList.add(begin);
        }

        //一次性查出日期列表范围内的所有新增用户数
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end.plusDays(1), LocalTime.MIN);
        List<DateCountDTO> newUserDtoList = userMapper.getNewUsersByDateRange(beginTime, endTime);

        //将结果转成<日期，数量>的结构
        Map<LocalDate,Integer> newUserMap=newUserDtoList.stream()
                .collect(Collectors.toMap(DateCountDTO::getDate,DateCountDTO::getCount));

        Integer totalUserBefore = userMapper.getTotalUserBeforeDate(beginTime);
        if(totalUserBefore==null){
            totalUserBefore=0;
        }

        //存放每天的新增用户数据
        List<Integer>newUserList=new ArrayList<>();
        //存放每天的总用户数据
        List<Integer>totalUserList=new ArrayList<>();

        int currentTotalUser=totalUserBefore;
        for(LocalDate date:dateList){
            //取出当天的新增用户，如果这一天没有新增则赋为0
            Integer newUserToday = newUserMap.getOrDefault(date, 0);

            //当天的总用户=昨天的总用户+今天的新增用户
            currentTotalUser+=newUserToday;

            newUserList.add(newUserToday);
            totalUserList.add(currentTotalUser);
        }

        return UserReportVO
                .builder()
                .dateList(StringUtils.join(dateList,","))
                .totalUserList(StringUtils.join(totalUserList,","))
                .newUserList(StringUtils.join(newUserList,","))
                .build();
    }

}
