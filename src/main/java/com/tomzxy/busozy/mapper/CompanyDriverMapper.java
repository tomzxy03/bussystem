package com.tomzxy.busozy.mapper;

import com.tomzxy.busozy.config.GlobalMapperConfig;
import com.tomzxy.busozy.dto.response.CompanyResDTO;
import com.tomzxy.busozy.dto.response.DriverResDTO;
import com.tomzxy.busozy.entity.Company;
import com.tomzxy.busozy.entity.Driver;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface CompanyDriverMapper {

    CompanyResDTO toCompanyRes(Company c);

    @Mapping(target = "company", expression = "java(mapCompany(d.getCompany()))")
    DriverResDTO toDriverRes(Driver d);

    default DriverResDTO.CompanySummary mapCompany(Company c) {
        return c != null ? new DriverResDTO.CompanySummary(c.getId(), c.getName()) : null;
    }
}
