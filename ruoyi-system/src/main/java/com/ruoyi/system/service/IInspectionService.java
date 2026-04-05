package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.Inspection;

public interface IInspectionService {

    List<Inspection> selectInspectionList(Inspection inspection);

    Inspection selectInspectionById(String id);

    int insertInspection(Inspection inspection);

    int updateInspection(Inspection inspection);

    int deleteInspectionById(String id);
}
