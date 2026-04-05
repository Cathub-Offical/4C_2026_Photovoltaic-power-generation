package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.Inspection;

public interface InspectionMapper {

    List<Inspection> selectInspectionList(Inspection inspection);

    Inspection selectInspectionById(String id);

    int insertInspection(Inspection inspection);

    int updateInspection(Inspection inspection);

    int deleteInspectionById(String id);
}
