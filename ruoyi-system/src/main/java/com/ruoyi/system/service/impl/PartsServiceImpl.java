package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.Parts;
import com.ruoyi.system.domain.PartsRecord;
import com.ruoyi.system.mapper.PartsMapper;
import com.ruoyi.system.service.IPartsService;

@Service
public class PartsServiceImpl implements IPartsService {

    @Autowired
    private PartsMapper partsMapper;

    @Override
    public List<Parts> selectPartsList(Parts parts) {
        return partsMapper.selectPartsList(parts);
    }

    @Override
    public Parts selectPartsById(String id) {
        return partsMapper.selectPartsById(id);
    }

    @Override
    public int insertParts(Parts parts) {
        parts.setId(java.util.UUID.randomUUID().toString().replace("-", ""));
        return partsMapper.insertParts(parts);
    }

    @Override
    public int updateParts(Parts parts) {
        return partsMapper.updateParts(parts);
    }

    @Override
    public int deletePartsById(String id) {
        return partsMapper.deletePartsById(id);
    }

    @Override
    public List<PartsRecord> selectPartsRecordList(PartsRecord record) {
        return partsMapper.selectPartsRecordList(record);
    }
}
