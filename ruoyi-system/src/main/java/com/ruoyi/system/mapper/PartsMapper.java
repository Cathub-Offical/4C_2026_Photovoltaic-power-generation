package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.Parts;
import com.ruoyi.system.domain.PartsRecord;

public interface PartsMapper {
    List<Parts> selectPartsList(Parts parts);
    Parts selectPartsById(String id);
    int insertParts(Parts parts);
    int updateParts(Parts parts);
    int deletePartsById(String id);
    List<PartsRecord> selectPartsRecordList(PartsRecord record);
    int insertPartsRecord(PartsRecord record);
}
