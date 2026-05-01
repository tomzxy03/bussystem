package com.tomzxy.busozy.mapper;

import com.tomzxy.busozy.config.GlobalMapperConfig;
import com.tomzxy.busozy.dto.response.RouteDetailResDTO;
import com.tomzxy.busozy.dto.response.RouteResDTO;
import com.tomzxy.busozy.entity.Company;
import com.tomzxy.busozy.entity.Route;
import com.tomzxy.busozy.entity.RoutePrice;
import com.tomzxy.busozy.entity.RouteStop;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface RouteMapper {

    @Mapping(target = "companyName", expression = "java(mapCompanyName(r.getCompany()))")
    RouteResDTO toRouteRes(Route r);

    @Mapping(target = "stopName", source = "stop.name")
    @Mapping(target = "provinceName", source = "stop.province.name")
    @Mapping(target = "minutesFromOrigin", source = "estimatedMinutesFromOrigin")
    RouteDetailResDTO.RouteStopResDTO toRouteStopRes(RouteStop rs);

    RouteDetailResDTO.RoutePriceResDTO toRoutePriceRes(RoutePrice rp);

    default String mapCompanyName(Company c) {
        return c != null ? c.getName() : null;
    }
}
