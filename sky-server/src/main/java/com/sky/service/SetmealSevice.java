package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.result.PageResult;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface SetmealSevice {
    // 新增套餐
    void save(SetmealDTO setmealDTO);

    // 根据id查询套餐
    SetmealVO getById(Long id);

    void startOrStop(Long id, Integer status);

    // 套餐分页查询
    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    void update(SetmealDTO setmealDTO);

    // 批量删除套餐
    void deleteBatch(List<Long> ids);


    /**
     * 条件查询
     *
     * @param setmeal
     * @return
     */
    List<Setmeal> list(Setmeal setmeal);


    /**
     * 根据id查询菜品选项
     *
     * @param id
     * @return
     */
    List<DishItemVO> getDishItemById(Long id);
}