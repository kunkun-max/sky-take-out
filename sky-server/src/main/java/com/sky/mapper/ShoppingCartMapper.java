package com.sky.mapper;

import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {

    /**
     * 动态条件查询
     * @param shoppingCart
     * @return
     */
    List<ShoppingCart> list(ShoppingCart shoppingCart);


    /**
     * 根据id增加商品数量
     * @param id
     */
    @Update("update shopping_cart set number=number+1 where id=#{id}")
    void increaseNumberById(Long id);

    /**
     * 根据id减少商品数量
     * @param id
     */
    @Update("update shopping_cart set number=number-1 where id=#{id} and number>1")
    void deleteNumberById(Long id);



    /**
     * 插入购物车数据
     * @param shoppingCart
     */
    @Insert("insert into shopping_cart(name,user_id,dish_id,setmeal_id,dish_flavor,number,amount,image,create_time)"+
    " values (#{name},#{userId},#{dishId},#{setmealId},#{dishFlavor},#{number},#{amount},#{image},#{createTime})")
    void insert(ShoppingCart shoppingCart);

    /**
     * 根据用户id清空购物车
     * @param userId
     */
    @Delete("delete from shopping_cart where user_id=#{userId}")
    void deleteByUserId(Long userId);

    /**
     * 根据购物车id删除单条购物车数据
     * @param id
     */
    @Delete("delete from shopping_cart where id=#{id}")
    void deleteById(Long id);

    /**
     * 批量插入购物车数据
     * @param shoppingCartList
     */
    void insertBatch(List<ShoppingCart> shoppingCartList);
}
