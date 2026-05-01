package com.tomzxy.busozy.mapper;

import com.tomzxy.busozy.config.GlobalMapperConfig;
import com.tomzxy.busozy.dto.response.TripDetailResDTO;
import com.tomzxy.busozy.dto.response.TripResDTO;
import com.tomzxy.busozy.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface TripMapper {

    @Mapping(target = "routeCode", expression = "java(t.getRoute() != null ? t.getRoute().getCode() : null)")
    @Mapping(target = "routeName", expression = "java(t.getRoute() != null ? t.getRoute().getName() : null)")
    @Mapping(target = "companyName", expression = "java(t.getRoute() != null && t.getRoute().getCompany() != null ? t.getRoute().getCompany().getName() : null)")
    @Mapping(target = "busLicensePlate", expression = "java(t.getBus() != null ? t.getBus().getLicensePlate() : null)")
    @Mapping(target = "busTypeName", expression = "java(t.getBus() != null && t.getBus().getBusType() != null ? t.getBus().getBusType().getName() : null)")
    @Mapping(target = "driverName", expression = "java(t.getDriver() != null ? t.getDriver().getFullName() : null)")
    TripResDTO toTripRes(Trip t);

    TripDetailResDTO.TripSegmentResDTO toSegmentRes(TripSegment s);
}
