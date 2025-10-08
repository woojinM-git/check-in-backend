package com.sist.backend.service;

import com.sist.backend.entity.Area;
import com.sist.backend.repository.AreaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AreaService {

    @Autowired
    private AreaRepository areaRepository;

    public List<Area> findAllAreas() {
        return areaRepository.findAll();
    }
}
