package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import io.swagger.models.auth.In;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Autowired
    private WebSocketServer webSocketServer;

    // 用户下单
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        // 1. 各种业务异常(地址簿为空，购物车数据为空)
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        // 查询购物车数据
        Long id = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(id);
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if (list == null || list.size() == 0) {
            // 没有数据
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 2. 向订单表插入一条数据

        AddressBook address = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());

        Orders orders = new Orders();


        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));

        orders.setAddressBookId(ordersSubmitDTO.getAddressBookId());
        orders.setAddress(address.getProvinceName() + address.getCityName() + address.getDistrictName() + address.getDetail());

        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(id);
        orderMapper.insert(orders);

        // 3. 向订单明细表插入n条数据
        List<OrderDetail> orderDetailList = new ArrayList<>();

        for (ShoppingCart cart : list) {
            // 订单明细
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            // 主键回显
            orderDetail.setOrderId(orders.getId()); // 设置当前订单明细对应的订单id
            orderDetail.setNumber(cart.getNumber());
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);

        // 4. 下单成功，清空用户购物车数据
        shoppingCartMapper.deleteByUserId(id);

        // 5. 封装vo返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder().id(orders.getId()).orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber()).orderAmount(orders.getAmount()).build();

        return orderSubmitVO;
    }


    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );

        JSONObject jsonObject = new JSONObject();

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        long l = System.currentTimeMillis();
        jsonObject.put("package", "vx" + l);

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));
        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        // 通过websocket向客户端浏览器发送消息：type，orderId，content
        Map map = new HashMap();
        map.put("type", 1); // 1 来单提醒 2催单
        map.put("orderId", ordersDB.getId());
        map.put("content", "订单号：" + outTradeNo);

        String json = JSON.toJSONString(map);

        webSocketServer.sendToAllClient(json);

    }

    // 历史订单
    @Override
    public PageResult history(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Long id = BaseContext.getCurrentId();
        ordersPageQueryDTO.setUserId(id);

        Page<OrderVO> page = orderMapper.selectList(ordersPageQueryDTO);

//        if (page == null || page.size() == 0) {
//            throw new OrderBusinessException("暂无订单");
//        }

        long total = page.getTotal();
        List<OrderVO> result = page.getResult();

        // 根据订单id查询订单详细信息
        List<OrderDetail> orderDetailList = new ArrayList<>();

        for (OrderVO orderVO : result) {
            orderDetailList = orderDetailMapper.selectListDetailById(orderVO.getId());
            orderVO.setOrderDetailList(orderDetailList);
        }

        PageResult pageResult = new PageResult();
        pageResult.setTotal(total);
        pageResult.setRecords(result);

        return pageResult;
    }

    // 订单详情
    public OrderVO orderDetail(Long id) {
        List<OrderDetail> orderDetailList = orderDetailMapper.selectListDetailById(id);

        OrderVO orderVO = orderMapper.selectById(id);
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }

    // 取消订单
    public void cancel(Long id) {

        // 先进行校验，是否存在，以及校验订单状态
        // xxx


        // 修改订单状态
        Orders orders = new Orders();
        orders.setId(id);
        orders.setStatus(Orders.CANCELLED);
        orderMapper.update(orders);
    }

    // 再来一单
    public void repeat(Long id) {

        Long userId = BaseContext.getCurrentId();
        // 清空购物车
        shoppingCartMapper.deleteByUserId(userId);

        // 添加商品到购物车
        // 查询商品
        List<OrderDetail> orderDetailList = orderDetailMapper.selectListByOrderId(id);

        List<ShoppingCart> shoppingCartList = new ArrayList<>();

        for (OrderDetail orderDetail : orderDetailList) {
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(orderDetail, shoppingCart);
            shoppingCart.setId(null);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCart.setUserId(userId);
            shoppingCartList.add(shoppingCart);
        }

        shoppingCartMapper.insertBatch(shoppingCartList);

    }

    // 管理端查找订单信息
    public PageResult search(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Page<OrderVO> page = orderMapper.selectList(ordersPageQueryDTO);

        PageResult result = new PageResult();

        List<OrderVO> res = page.getResult();
        for (OrderVO re : res) {
            Long orderId = re.getId();
            String ordersDishName = getOrdersDish(orderId);
            re.setOrderDishes(ordersDishName);
        }

        result.setTotal(page.getTotal());
        result.setRecords(res);


        return result;
    }

    public String getOrdersDish(Long id) {
        List<OrderDetail> orderDetailList = orderDetailMapper.selectListByOrderId(id);
        StringBuilder builder = new StringBuilder();
        for (OrderDetail orderDetail : orderDetailList) {
            String t = null;
            if (orderDetail.getDishFlavor() != null) {
                t = "(" + orderDetail.getName() + "*" + orderDetail.getNumber()
                        + orderDetail.getDishFlavor() + ")";
            } else {
                t = "(" + orderDetail.getName() + "*" + orderDetail.getNumber() + ")";
            }
            builder.append(t);
        }
        return builder.toString();
    }


    @Override
    public OrderStatisticsVO statistic() {

        OrderStatisticsVO orderStatisticsVO = orderMapper.selectStatistics();

        return orderStatisticsVO;
    }

    // 接单
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = Orders.builder().id(ordersConfirmDTO.getId()).status(Orders.CONFIRMED).build();
        orderMapper.update(orders);
    }

    // 拒单
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        Orders orders = Orders.builder()
                .id(ordersRejectionDTO.getId())
                .rejectionReason(ordersRejectionDTO.getRejectionReason())
                .status(Orders.CANCELLED)
                .build();

        orderMapper.update(orders);
    }

    @Override
    public void delivery(Long id) {
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.DELIVERY_IN_PROGRESS)
                .build();

        orderMapper.update(orders);
    }

    // 商家取消订单
    public void adminCancel(OrdersCancelDTO ordersCancelDTO) {
        Orders orders = Orders.builder()
                .id(ordersCancelDTO.getId())
                .cancelReason(ordersCancelDTO.getCancelReason())
                .status(Orders.CANCELLED)
                .build();

        orderMapper.update(orders);
    }

    // 订单完成
    public void complete(Long id) {
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.COMPLETED)
                .deliveryTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    @Override
    public Orders getLastOrder() {
        Orders orders = orderMapper.selectLastOrder();
        return orders;
    }

    // 客户催单
    public void reminder(Long id) {
        // 根据id查询订单
        Orders orders = orderMapper.getById(id);
        // 校验订单是否存在
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Map map = new HashMap();
        map.put("type", 2); // 1 来单提醒；2 催单
        map.put("id", id);
        map.put("content", "订单号：" + orders.getNumber());

        // 通过websocket发动推送信息
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }

}









