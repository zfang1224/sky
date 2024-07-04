package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderDetailMapper {
    // 批量插入订单明细
    void insertBatch(List<OrderDetail> orderDetailList);

    // 查询订单详细信息
    @Select("select * from sky_take_out.order_detail where order_id = #{if};")
    List<OrderDetail> selectListDetailById(Long id);

    @Select("select * from sky_take_out.order_detail where order_id = #{id}")
    List<OrderDetail> selectListByOrderId(Long id);
}
