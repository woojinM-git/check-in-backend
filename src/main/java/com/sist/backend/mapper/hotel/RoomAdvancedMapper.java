package com.sist.backend.mapper.hotel;

import com.sist.backend.entity.Room;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RoomAdvancedMapper {

    List<Room> searchRooms(
            @Param("contentId") String contentId,
            @Param("name") String name,
            @Param("minCapacity") Integer minCapacity,
            @Param("maxCapacity") Integer maxCapacity
    );
}
