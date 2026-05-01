package com.tomzxy.busozy.mapper;

import com.tomzxy.busozy.config.GlobalMapperConfig;
import com.tomzxy.busozy.dto.response.UserResDTO;
import com.tomzxy.busozy.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface UserMapper {

    @Mapping(target = "companyId", expression = "java(user.getCompany() != null ? user.getCompany().getId() : null)")
    @Mapping(target = "companyName", expression = "java(user.getCompany() != null ? user.getCompany().getName() : null)")
    UserResDTO toResDTO(User user);
}
