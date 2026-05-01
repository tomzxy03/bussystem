package com.tomzxy.busozy.mapper;

import com.tomzxy.busozy.config.GlobalMapperConfig;
import com.tomzxy.busozy.dto.response.DistrictResDTO;
import com.tomzxy.busozy.dto.response.ProvinceResDTO;
import com.tomzxy.busozy.dto.response.StopResDTO;
import com.tomzxy.busozy.entity.District;
import com.tomzxy.busozy.entity.Province;
import com.tomzxy.busozy.entity.Stop;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface LocationMapper {

    @Mapping(target = "lat", source = "latitude")
    @Mapping(target = "lng", source = "longitude")
    ProvinceResDTO toProvinceRes(Province p);

    @Mapping(target = "provinceId", source = "province.id")
    DistrictResDTO toDistrictRes(District d);

    @Mapping(target = "lat", source = "latitude")
    @Mapping(target = "lng", source = "longitude")
    @Mapping(target = "provinceName", source = "province.name")
    @Mapping(target = "districtName", source = "district.name")
    StopResDTO toStopRes(Stop s);
}
