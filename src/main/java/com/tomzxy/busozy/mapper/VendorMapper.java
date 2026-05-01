package com.tomzxy.busozy.mapper;

import com.tomzxy.busozy.config.GlobalMapperConfig;
import com.tomzxy.busozy.dto.response.VendorProfileResDTO;
import com.tomzxy.busozy.entity.Company;
import com.tomzxy.busozy.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface VendorMapper {
    @Mapping(target = "userId", source = "id")
    @Mapping(target = "companyId", expression = "java(u.getCompany() != null ? u.getCompany().getId() : null)")
    @Mapping(target = "companyName", expression = "java(mapCompanyName(u.getCompany()))")
    @Mapping(target = "taxCode", expression = "java(mapTaxCode(u.getCompany()))")
    @Mapping(target = "companyPhone", expression = "java(mapCompanyPhone(u.getCompany()))")
    @Mapping(target = "companyAddress", expression = "java(mapCompanyAddress(u.getCompany()))")
    VendorProfileResDTO toProfileRes(User u);
    default String mapCompanyName(Company c) { return c != null ? c.getName() : null; }
    default String mapTaxCode(Company c) { return c != null ? c.getTaxCode() : null; }
    default String mapCompanyPhone(Company c) { return c != null ? c.getPhone() : null; }
    default String mapCompanyAddress(Company c) { return c != null ? c.getAddress() : null; }
}
