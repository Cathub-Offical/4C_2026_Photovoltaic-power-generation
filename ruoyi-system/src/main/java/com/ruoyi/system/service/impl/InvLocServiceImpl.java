package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.InvLoc;
import com.ruoyi.system.mapper.InvLocMapper;
import com.ruoyi.system.service.IInvLocService;

@Service
public class InvLocServiceImpl implements IInvLocService {

    @Autowired
    private InvLocMapper invLocMapper;

    @Override
    public List<InvLoc> selectInvLocList() {
        return invLocMapper.selectInvLocList();
    }

    @Override
    public InvLoc selectInvLocById(String id) {
        return invLocMapper.selectInvLocById(id);
    }

    @Override
    public int insertInvLoc(InvLoc invLoc) {
        invLoc.setId(java.util.UUID.randomUUID().toString().replace("-", ""));
        return invLocMapper.insertInvLoc(invLoc);
    }

    @Override
    public int updateInvLoc(InvLoc invLoc) {
        return invLocMapper.updateInvLoc(invLoc);
    }

    @Override
    public int deleteInvLocById(String id) {
        return invLocMapper.deleteInvLocById(id);
    }
}
