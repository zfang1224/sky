package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService {
    /**
     * 新增菜品和对应的口味
     * @param dto
     */
    public void saveWithFlor(DishDTO dto);

    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    void deleteBatch(List<Long> ids);

    // 根据id查询对应的菜品和口味数据
    DishVO getByIdWithFlavor(Long id);

    // 根据id修改菜品基本信息和口味信息
    void updateWithFlavor(DishDTO dishDTO);

    // 根据类型id查询菜品
    List<Dish> getByCategoryId(Long categoryId);

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    List<DishVO> listWithFlavor(Dish dish);
}
