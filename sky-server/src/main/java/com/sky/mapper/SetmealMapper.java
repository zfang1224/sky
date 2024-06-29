package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFIll;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.enumeration.OperationType;
import com.sky.vo.SetmealVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealMapper {

    /**
     * 根据分类id查询套餐的数量
     * @param id
     * @return
     */
    @Select("select count(id) from setmeal where category_id = #{categoryId}")
    Integer countByCategoryId(Long id);

    // 新增菜品
    @AutoFIll(value = OperationType.INSERT)
    void insert(Setmeal setmeal);

    // 根据id查询
    Setmeal selectById(Long id);

    @AutoFIll(OperationType.UPDATE)
    void update(Setmeal setmeal);

    Page<Setmeal> pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    List<Setmeal> selectBatch(List<Long> ids);

    void deleteBatch(List<Long> ids);
}
