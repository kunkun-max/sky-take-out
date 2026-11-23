package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.core.annotation.Order;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {


    /**
     * 插入订单数据
     * @param orders
     */
    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     * @param outTradeNo
     */
    @Select("select * from orders where number=#{orderNumber}")
    Orders getByNumber(String outTradeNo);


    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 分页条件查询并按下单时间排序
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据id查询订单
     * @param id
     * @return
     */
    @Select("select * from orders where id=#{id}")
    Orders getById(Long id);

    /**
     * 根据状态统计订单数量
     * @param toBeConfirmed
     * @return
     */
    @Select("select count(id) from orders where status=#{status}")
    Integer countStatus(Integer toBeConfirmed);

    /**
     * 商家接单，状态判断防超卖
     * @param orders
     */
    void confirm(Orders orders);

    /**
     * 根据订单状态和下单时间查询订单
     * @param status
     * @param orderTime
     * @return
     */
    @Select("select * from orders where status=#{status} and order_time<#{orderTime}")
    List<Orders> getByStatusAndOrderTimeLT(Integer status, LocalDateTime orderTime);


    /**
     * 批量取消超时订单
     * @param timeoutTime 超时时间基准线
     * @param status 待更新状态
     * @param cancelReason 取消原因
     */
    @Update("update orders set status = #{status}, cancel_reason = #{cancelReason}, cancel_time = now() " +
            "where status = 1 and order_time < #{timeoutTime}")
    void cancelTimeoutOrders(@Param("timeoutTime") LocalDateTime timeoutTime,
                             @Param("status") Integer status,
                             @Param("cancelReason") String cancelReason);


    /**
     * 批量完成派送中订单
     * @param time
     * @param status
     */
    @Update("update orders set status = #{status},delivery_time=now() where status = 4 and order_time < #{time}")
    void completeDeliveryOrders(@Param("time") LocalDateTime time, @Param("status") Integer status);
}
