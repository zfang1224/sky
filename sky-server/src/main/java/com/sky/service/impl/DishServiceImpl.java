package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增菜品和对应的口味
     *
     * @param dto
     */
    @Transactional
    public void saveWithFlor(DishDTO dto) {

        Dish dish = new Dish();
        BeanUtils.copyProperties(dto, dish);

        // 1. 向菜品表插入1条数据
        dishMapper.insert(dish);
        // 获取insert语句生成的主键值
        Long id = dish.getId();

        // 2. 向口味表插入n条数据
        List<DishFlavor> flavors = dto.getFlavors();
        if (flavors != null && flavors.size() > 0) {

            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(id); // 给口味表的dish_id赋值
            });

            // 向口味表插入n条数据
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 菜品分页查询
     *
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 菜品批量删除功能
     *
     * @param ids
     */
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        // 1. 判断当前菜品是否能删除（起售中）
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if (dish.getStatus() == StatusConstant.ENABLE) {
                // 起售中，不能删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        // 2. 判断当前菜品是否能删除（是否与套餐关联）
        List<Long> setmealIds = setmealDishMapper.getSetmealIdByDishId(ids);
        if (setmealIds != null && setmealIds.size() > 0) {
            // 当前菜品被套餐关联了，不能删除
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        // 删除菜品表中的数据
//        for (Long id : ids) {
//            dishMapper.deleteById(id);
//            // 删除菜品关联的口味数据
//            dishFlavorMapper.deleteByDishId(id);
//        }
        // 批量删除菜品数据和口味数据
        dishMapper.deleteByIds(ids);
        dishFlavorMapper.deleteByDishIds(ids);

    }

    // 根据id查询菜品和对应的口味
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        // 1. 根据菜品id查询菜品数据
        Dish dish = dishMapper.getById(id);
        // 2. 根据菜品id查询口味数据
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);
        // 3. 封装到DishVO
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }

    // 根据id修改菜品基本信息和口味信息

    @Override
    public void updateWithFlavor(DishDTO dishDTO) {
        // 对于口味，先删除，后插入

        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        // 修改菜品表基本信息
        dishMapper.update(dish);
        // 口味表
        dishFlavorMapper.deleteByDishId(dishDTO.getId());

        // 重新插入口味数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {

            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishDTO.getId()); // 给口味表的dish_id赋值
            });
            // 向口味表插入n条数据
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    // 根据分类id查询菜品
    public List<Dish> getByCategoryId(Long categoryId) {
        Dish dish = Dish.builder()
                .status(StatusConstant.ENABLE)
                .categoryId(categoryId)
                .build();

        List<Dish> list = dishMapper.list(dish);
        return list;
    }

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.list(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }

}
