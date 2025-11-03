package com.sist.backend.repository;

import com.sist.backend.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Integer> {
    
    @Query("SELECT e FROM Event e WHERE e.contentid = :contentid")
    List<Event> findByContentid(@Param("contentid") String contentid);
}

