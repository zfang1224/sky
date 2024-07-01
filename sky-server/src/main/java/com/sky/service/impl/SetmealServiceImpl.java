package com.sky.service.impl;

import com.github.pagehelper.Constant;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealSevice;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SetmealServiceImpl implements SetmealSevice {

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Override
    @Transactional
    public void save(SetmealDTO setmealDTO) {
        // 向套餐表插入数据
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmeal.setStatus(StatusConstant.DISABLE);
        setmealMapper.insert(setmeal);

        Long id = setmeal.getId();

        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        for (SetmealDish setmealDish : setmealDishes) {
            setmealDish.setSetmealId(id);
        }
        setmealDishMapper.insertBatch(setmealDishes);
    }

    // 根据id查询套餐
    public SetmealVO getById(Long id) {
        Setmeal setmeal = setmealMapper.selectById(id);
        List<SetmealDish> setmealDishList = setmealDishMapper.selectListBySetmealId(id);
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setSetmealDishes(setmealDishList);

        return setmealVO;
    }

    // 套餐起售或禁售
    public void startOrStop(Long id, Integer status) {
        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .build();
        setmealMapper.update(setmeal);
    }

    // 套餐分页查询
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        Page<Setmeal> setmealPage = setmealMapper.pageQuery(setmealPageQueryDTO);
        PageResult result = new PageResult();
        result.setTotal(setmealPage.getTotal());
        result.setRecords(setmealPage.getResult());
        return result;
    }

    // 修改套餐
    @Transactional
    public void update(SetmealDTO setmealDTO) {

        List<Long> list = new ArrayList<>();
        list.add(setmealDTO.getId());
        // 先删除套餐-菜品表中的数据，后插入
        setmealDishMapper.deleteBatch(list);

        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        // 修改setmeal表数据
        setmealMapper.update(setmeal);
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        for (SetmealDish setmealDish : setmealDishes) {
            setmealDish.setSetmealId(setmealDTO.getId());
        }
        // 新增套餐-菜品表数据
        setmealDishMapper.insertBatch(setmealDishes);
    }

    // 套餐批量删除
    @Transactional
    public void deleteBatch(List<Long> ids) {

        // 先查询，起售中则不能删除
        List<Setmeal> setmealList = setmealMapper.selectBatch(ids);
        for (Setmeal setmeal : setmealList) {
            if (setmeal.getStatus() == StatusConstant.ENABLE) {
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }
        // 先根据id删除 套餐-菜品表数据
        setmealDishMapper.deleteBatch(ids);
        // 删除套餐表数据
        setmealMapper.deleteBatch(ids);
    }

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }

}



