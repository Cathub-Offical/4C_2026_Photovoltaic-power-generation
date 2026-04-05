package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.InvLoc;

public interface InvLocMapper {
    List<InvLoc> selectInvLocList();
    InvLoc selectInvLocById(String id);
    int insertInvLoc(InvLoc invLoc);
    int updateInvLoc(InvLoc invLoc);
    int deleteInvLocById(String id);
}
