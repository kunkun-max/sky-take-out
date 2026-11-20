package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private OrderService orderService;

    @Transactional
    @Override
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //处理各种业务异常（地址簿为空，购物车数据为空）
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if(addressBook==null){
            //抛出业务异常
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }


        //查询当前用户的购物车数据
        Long userId = BaseContext.getCurrentId();

        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);

        if(shoppingCartList==null || shoppingCartList.size()==0){
            //抛出业务异常
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //向订单表插入一条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setAddress(addressBook.getDetail());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(userId);

        orderMapper.insert(orders);

        List<OrderDetail> orderDetailList=new ArrayList<>();
        //向订单明细表插入n条数据
        for(ShoppingCart cart:shoppingCartList){
            OrderDetail orderDetail = new OrderDetail(); //订单明细
            BeanUtils.copyProperties(cart,orderDetail);
            orderDetail.setOrderId(orders.getId()); //设置当前订单明细关联的订单id
            orderDetailList.add(orderDetail);
        }

        orderDetailMapper.insertBatch(orderDetailList);
        //清空当前用户的购物车数据
        shoppingCartMapper.deleteByUserId(userId);
        //封装VO返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();
        return orderSubmitVO;
    }

    //模拟微信支付方法
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception{
        // 直接调用支付成功逻辑
        // 数据库里的订单状态改成“待接单”
        paySuccess(ordersPaymentDTO.getOrderNumber());


        // 前端收到 code: 0，就不会去调用微信支付，而是直接弹个提示
        throw new OrderBusinessException("模拟支付成功，请去订单列表查看最新状态！");
    }

    //模拟支付成功并修改订单状态
    public void paySuccess(String outTradeNo){
        //根据订单号查询订单
        Orders ordersDB= orderMapper.getByNumber(outTradeNo);
        //如果订单不存在或者已经支付过了，直接返回
        if(ordersDB==null||ordersDB.getPayStatus()==Orders.PAID){
            return;
        }
        //根据订单更新订单的状态，支付方式，支付状态，结账时间
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.TO_BE_CONFIRMED); // 变为待接单
        orders.setPayStatus(Orders.PAID);         // 变为已支付
        orders.setCheckoutTime(LocalDateTime.now());

        orderMapper.update(orders);
    }


    @Override
    public PageResult pageQuery4User(int pageNum, int pageSize, Integer status) {
        PageHelper.startPage(pageNum,pageSize);

        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();

        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);

        Page<Orders> page=orderMapper.pageQuery(ordersPageQueryDTO);

       List<OrderVO> list = new ArrayList();

       if(page!=null&&page.getTotal()>0){
           for (Orders orders:page){
               Long orderId = orders.getId();
               List<OrderDetail> orderDetails=orderDetailMapper.getByOrderId(orderId);

               OrderVO orderVO=new OrderVO();
               BeanUtils.copyProperties(orders,orderVO);
               orderVO.setOrderDetailList(orderDetails);

               list.add(orderVO);
           }
       }
       return new PageResult(page.getTotal(),list);
    }

    @Override
    public OrderVO details(Long id) {
       Orders orders  =orderMapper.getById(id);

       List<OrderDetail>orderDetailList=orderDetailMapper.getByOrderId(orders.getId());

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders,orderVO);
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }

    @Override
    public void userCancelById(Long id) {
        Orders orderDB = orderMapper.getById(id);

        if(orderDB==null){
            //订单不存在
            throw new  OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        if(orderDB.getStatus()>2){
            //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orders = new Orders();
        orders.setId(orderDB.getId());

        //订单处于待接单状态下取消，需进行退款
        if(orderDB.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            //调用微信支付退款接口
//            weChatPayUtil.refund(
//                    ordersDB.getNumber(), //商户订单号
//                    ordersDB.getNumber(), //商户退款单号
//                    new BigDecimal(0.01),//退款金额，单位 元
//                    new BigDecimal(0.01));//原订单金额

            //修改支付状态为退款
            orders.setPayStatus(Orders.REFUND);
        }
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);

    }

    @Override
    public void repetition(Long id) {
        Long userId = BaseContext.getCurrentId();

        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        List<ShoppingCart>shoppingCartList=orderDetailList.stream().map(x->{
            ShoppingCart shoppingCart = new ShoppingCart();
            //这里id指的是赋值的时候忽略这个字段，因为id是主键，如果主键一起带过去会导致主键冲突
            BeanUtils.copyProperties(x,shoppingCart,"id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());

        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO>orderVOList=getOrderVOList(page);

        return new PageResult(page.getTotal(),orderVOList);
    }
    private List<OrderVO>getOrderVOList(Page<Orders>page){
        List<OrderVO>orderVOList=new ArrayList<>();

        List<Orders>ordersList=page.getResult();
        if(!CollectionUtils.isEmpty(ordersList)){
            for(Orders orders:ordersList){
                OrderVO orderVO=new OrderVO();
                BeanUtils.copyProperties(orders,orderVO);
                String orderDishes=getOrderDishesStr(orders);

                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    /**
     * 根据订单id获取菜单信息字符串
     * @param orders
     * @return
     */
    private String getOrderDishesStr(Orders orders) {
            List<OrderDetail>orderDetailList=orderDetailMapper.getByOrderId(orders.getId());

            //将每一条订单菜品信息转化为字符串
            List<String> orderDishList=orderDetailList.stream().map(x->{
                String orderDish=x.getName()+"*"+x.getNumber()+";";
                return orderDish;
            }).collect(Collectors.toList());

            return String.join("",orderDishList);
    }
}
