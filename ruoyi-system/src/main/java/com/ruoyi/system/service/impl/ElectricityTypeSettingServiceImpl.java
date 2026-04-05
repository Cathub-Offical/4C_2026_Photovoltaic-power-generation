package com.ruoyi.system.service.impl;

import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.ElectricityTypeSetting;
import com.ruoyi.system.domain.ElectricityTypeSettingItem;
import com.ruoyi.system.mapper.ElectricityTypeSettingMapper;
import com.ruoyi.system.service.IElectricityTypeSettingService;

@Service
public class ElectricityTypeSettingServiceImpl implements IElectricityTypeSettingService {

    @Autowired
    private ElectricityTypeSettingMapper mapper;

    @Override
    public List<ElectricityTypeSetting> selectSettingList(ElectricityTypeSetting setting) {
        return mapper.selectSettingList(setting);
    }

    @Override
    public ElectricityTypeSetting selectSettingById(String id) {
        return mapper.selectSettingById(id);
    }

    @Override
    public int insertSetting(ElectricityTypeSetting setting) {
        setting.setId(UUID.randomUUID().toString().replace("-", ""));
        return mapper.insertSetting(setting);
    }

    @Override
    public int updateSetting(ElectricityTypeSetting setting) {
        return mapper.updateSetting(setting);
    }

    @Override
    public int deleteSettingById(String id) {
        mapper.deleteItemsByParentId(id);
        return mapper.deleteSettingById(id);
    }

    @Override
    public List<ElectricityTypeSettingItem> selectItemsByParentId(String parentId) {
        return mapper.selectItemsByParentId(parentId);
    }

    @Override
    public int saveItems(String parentId, List<ElectricityTypeSettingItem> items) {
        mapper.deleteItemsByParentId(parentId);
        if (items != null && !items.isEmpty()) {
            for (ElectricityTypeSettingItem item : items) {
                item.setId(UUID.randomUUID().toString().replace("-", ""));
                item.setParentId(parentId);
            }
            return mapper.insertItems(items);
        }
        return 0;
    }
}
