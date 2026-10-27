package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import javax.validation.constraints.Digits;
import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /**
     * 批量保存菜品和套餐的关系
     * @param setmealDishes
     */
    void insertBatch(List<SetmealDish> setmealDishes);

    void deleteBySetmealIds(@Param("ids") List<Long> ids);

    @Select("select * from setmeal_dish where setmeal_id=#{setmealId}")
    List<SetmealDish> getBySetmealId(Long setmealId);
    @Delete("delete from setmeal_dish where setmeal_id=#{setmealId}")
    void deleteBySetmealId(Long setmealId);
}
