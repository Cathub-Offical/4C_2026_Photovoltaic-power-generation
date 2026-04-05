package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.ElectricityTypeSetting;
import com.ruoyi.system.domain.ElectricityTypeSettingItem;

public interface ElectricityTypeSettingMapper {
    List<ElectricityTypeSetting> selectSettingList(ElectricityTypeSetting setting);
    ElectricityTypeSetting selectSettingById(String id);
    int insertSetting(ElectricityTypeSetting setting);
    int updateSetting(ElectricityTypeSetting setting);
    int deleteSettingById(String id);
    List<ElectricityTypeSettingItem> selectItemsByParentId(String parentId);
    int insertItems(List<ElectricityTypeSettingItem> items);
    int deleteItemsByParentId(String parentId);
}
