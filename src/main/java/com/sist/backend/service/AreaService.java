package com.sist.backend.service;

import com.sist.backend.entity.Area;
import com.sist.backend.repository.AreaRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AreaService {

    
    final private AreaRepository areaRepository;

    public List<Area> findAllAreas() {
        return areaRepository.findAll();
    }
}
