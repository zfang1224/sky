package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 营业额统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {

        // 存放begin，end之间的日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            // 计算指定日期的后一天日期
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            // 查询date日期对应的营业额，订单状态已完成的订单金额
            LocalDateTime b = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime e = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("begin", b);
            map.put("end", e);
            map.put("status", Orders.COMPLETED);

            Double turnover = orderMapper.sumByMap(map);

            turnover = turnover == null ? 0.0 : turnover;

            turnoverList.add(turnover);
        }

        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    // 用户统计
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        // 存放时间
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        // 存放新增用户
        // select count(*) from user where create_time < xx and create_time > xx;
        List<Integer> newUserList = new ArrayList<>();
        // 存放总用户
        // select count(*) from user where create_time < xx;
        List<Integer> totalUserList = new ArrayList<>();

        for (LocalDate date : dateList) {
            // 遍历每天
            LocalDateTime b = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime e = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("end", b);
            // 总数量
            Integer totalUser = userMapper.countByMap(map);

            map.put("begin", b);
            Integer newUser = userMapper.countByMap(map);

            totalUserList.add(totalUser);
            newUserList.add(newUser);

        }
        return UserReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();
    }

    // 统计指定时间的订单数据
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        // 存放时间
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        // 每天订单总数
        List<Integer> orderCountList = new ArrayList<>();
        // 有效订单数
        List<Integer> validOrderCountList = new ArrayList<>();
        // 遍历集合，查询有效订单数和订单总数
        for (LocalDate date : dateList) {
            // 查询每天的订单总数 select count(*) from orders where order_time > ? and order_time < ?;
            LocalDateTime b = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime e = LocalDateTime.of(date, LocalTime.MAX);
            Integer totalOrders = getOrderCount(b, e, null);
            // 查询每天的有效订单数 select count(*) from orders where order_time > ? and order_time < ? and status = 5
            Integer validOrders = getOrderCount(b, e, Orders.COMPLETED);

            orderCountList.add(totalOrders);
            validOrderCountList.add(validOrders);
        }

        // 区间内订单总数
        Integer total = orderCountList.stream().reduce(Integer::sum).get();
        // 区间内的有效订单数
        Integer valids = validOrderCountList.stream().reduce(Integer::sum).get();
        // 计算订单完成率

        Double cc = 0.0;
        if (total != 0) {
            cc = valids.doubleValue() / total.doubleValue();
        }

        OrderReportVO orderReportVO = new OrderReportVO();
        orderReportVO.setDateList(StringUtils.join(dateList, ","));
        orderReportVO.setOrderCountList(StringUtils.join(orderCountList, ","));
        orderReportVO.setValidOrderCountList(StringUtils.join(validOrderCountList, ","));

        orderReportVO.setTotalOrderCount(total);
        orderReportVO.setValidOrderCount(valids);
        orderReportVO.setOrderCompletionRate(cc);

        return orderReportVO;
    }

    // 查询订单数
    private Integer getOrderCount(LocalDateTime begin, LocalDateTime end, Integer status) {
        Map map = new HashMap();
        map.put("begin", begin);
        map.put("end", end);
        map.put("status", status);

        return orderMapper.countByMap(map);
    }

    // 销量排名前10
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime b = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime e = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(b, e);

        List<String> nameList = new ArrayList<>();
        List<Integer> numberList = new ArrayList<>();
        for (GoodsSalesDTO dto : salesTop10) {
            nameList.add(dto.getName());
            numberList.add(dto.getNumber());
        }

        String names = StringUtils.join(nameList, ",");
        String numbers = StringUtils.join(numberList, ",");

        return SalesTop10ReportVO
                .builder()
                .nameList(names)
                .numberList(numbers)
                .build();
    }

}



