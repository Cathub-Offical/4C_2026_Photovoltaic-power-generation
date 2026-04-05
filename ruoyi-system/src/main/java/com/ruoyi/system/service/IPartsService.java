package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.Parts;
import com.ruoyi.system.domain.PartsRecord;

public interface IPartsService {
    List<Parts> selectPartsList(Parts parts);
    Parts selectPartsById(String id);
    int insertParts(Parts parts);
    int updateParts(Parts parts);
    int deletePartsById(String id);
    List<PartsRecord> selectPartsRecordList(PartsRecord record);
}
