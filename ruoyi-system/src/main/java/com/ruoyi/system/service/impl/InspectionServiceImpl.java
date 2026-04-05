package com.ruoyi.system.service.impl;

import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.Inspection;
import com.ruoyi.system.mapper.InspectionMapper;
import com.ruoyi.system.service.IInspectionService;

@Service
public class InspectionServiceImpl implements IInspectionService {

    @Autowired
    private InspectionMapper inspectionMapper;

    @Override
    public List<Inspection> selectInspectionList(Inspection inspection) {
        return inspectionMapper.selectInspectionList(inspection);
    }

    @Override
    public Inspection selectInspectionById(String id) {
        return inspectionMapper.selectInspectionById(id);
    }

    @Override
    public int insertInspection(Inspection inspection) {
        if (inspection.getId() == null || inspection.getId().isEmpty()) {
            inspection.setId(UUID.randomUUID().toString());
        }
        return inspectionMapper.insertInspection(inspection);
    }

    @Override
    public int updateInspection(Inspection inspection) {
        return inspectionMapper.updateInspection(inspection);
    }

    @Override
    public int deleteInspectionById(String id) {
        return inspectionMapper.deleteInspectionById(id);
    }
}
