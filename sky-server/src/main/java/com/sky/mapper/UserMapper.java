package com.sky.mapper;

import com.sky.dto.DateCountDTO;
import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {

    @Select("select * from user where openid = #{openid}")
    User getByOpenId(String openid);

    /**
     * 插入数据
     * @param user
     */
    void insert(User user);

    /**
     * 统计指定日期范围内的每日新增用户数
     * @param begin
     * @param end
     * @return
     */
    List<DateCountDTO> getNewUsersByDateRange(@Param("begin")LocalDateTime begin,@Param("end")LocalDateTime end);


    /**
     * 统计指定时间点之前的用户总数
     * @param dateTime
     * @return
     */
    Integer getTotalUserBeforeDate(@Param("dateTime")LocalDateTime dateTime);
}
