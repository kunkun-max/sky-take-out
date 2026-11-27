package com.sky.service.impl;

import com.sky.constant.StatusConstant;
import com.sky.entity.Orders;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.WorkSpaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class WorkSpaceServiceImpl implements WorkSpaceService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private DishMapper dishMapper;

    @Override
    public BusinessDataVO getBuinessData(LocalDateTime begin, LocalDateTime end) {
        Map map=new HashMap();
        map.put("begin",begin);
        map.put("end",end);

        //查询今日订单总数
        Integer totalOrderCount=orderMapper.countByMap(map);

        map.put("status", Orders.COMPLETED);
        //营业额
        Double turnover=orderMapper.sumByMap(map);
        turnover=turnover==null?0.0:turnover;

        //有效订单数
        Integer validOrderCount = orderMapper.countByMap(map);
        map.remove("status");

        Double unitPrice=0.0;

        Double orderCompletionRate=0.0;
        if(totalOrderCount!=0&&validOrderCount!=0){
            //订单完成率
            orderCompletionRate=validOrderCount.doubleValue()/totalOrderCount;
            //平均单价
            unitPrice=turnover/validOrderCount;
        }
        //新增用户数
        Integer newUsers=userMapper.countByMap(map);

        return BusinessDataVO.builder()
                .turnover(turnover)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .newUsers(newUsers)
                .build();
    }

    @Override
    public OrderOverViewVO getOrderOverView() {
        Map map=new HashMap();
        map.put("begin",LocalDateTime.now().with(LocalTime.MIN));
        //待接单
        map.put("status",Orders.TO_BE_CONFIRMED);
        Integer watingOrders = orderMapper.countByMap(map);
        //待派送
        map.put("status",Orders.CONFIRMED);
        Integer deliveryOrders = orderMapper.countByMap(map);
        //已完成
        map.put("status",Orders.COMPLETED);
        Integer completedOrders = orderMapper.countByMap(map);
        //已取消
        map.put("status",Orders.CANCELLED);
        Integer cancelledOrders = orderMapper.countByMap(map);
        //全部订单
        map.put("status",null);
        Integer allOrders = orderMapper.countByMap(map);

        return OrderOverViewVO.builder()
                .waitingOrders(watingOrders)
                .deliveredOrders(deliveryOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .allOrders(allOrders)
                .build();
    }

    @Override
    public DishOverViewVO getDishOverView() {
        Map map=new HashMap();
        map.put("status", StatusConstant.ENABLE);
        Integer sold=dishMapper.countByMap(map);

        map.put("status",StatusConstant.DISABLE);
        Integer discontinued = dishMapper.countByMap(map);

        return DishOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }

    @Override
    public SetmealOverViewVO getSetmealOverView() {
        Map map=new HashMap();
        map.put("status",StatusConstant.ENABLE);
        Integer sold=setmealMapper.countByMap(map);

        map.put("status",StatusConstant.DISABLE);
        Integer discontinued=setmealMapper.countByMap(map);

        return SetmealOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }


}
