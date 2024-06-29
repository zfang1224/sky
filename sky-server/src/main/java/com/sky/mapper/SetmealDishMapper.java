package com.sky.mapper;

import com.sky.annotation.AutoFIll;
import com.sky.entity.SetmealDish;
import com.sky.enumeration.OperationType;
import io.swagger.models.auth.In;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /**
     * 根据菜品id查询对应的套餐id
     * @param dishIds
     * @return
     */
    List<Long> getSetmealIdByDishId(List<Long> dishIds);

    List<SetmealDish> selectListBySetmealId(Long setmealId);

    void insertBatch(List<SetmealDish> setmealDishes);

    // 根据套餐id，删除套餐-菜品表的数据
//    @Delete("delete from setmeal_dish where setmeal_id = #{setmealId}")
    void deleteBatch(List<Long> ids);
}
