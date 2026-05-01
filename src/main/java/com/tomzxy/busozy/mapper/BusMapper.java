package com.tomzxy.busozy.mapper;

import com.tomzxy.busozy.config.GlobalMapperConfig;
import com.tomzxy.busozy.dto.response.BusResDTO;
import com.tomzxy.busozy.dto.response.BusTypeResDTO;
import com.tomzxy.busozy.dto.response.SeatLayoutResDTO;
import com.tomzxy.busozy.dto.response.SeatResDTO;
import com.tomzxy.busozy.entity.Bus;
import com.tomzxy.busozy.entity.BusType;
import com.tomzxy.busozy.entity.SeatLayout;
import com.tomzxy.busozy.entity.Seat;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface BusMapper {

    BusTypeResDTO toBusTypeRes(BusType bt);

    @Mapping(target = "busTypeId", source = "busType.id")
    SeatLayoutResDTO toSeatLayoutRes(SeatLayout sl);

    @Mapping(target = "companyName", expression = "java(b.getCompany() != null ? b.getCompany().getName() : null)")
    @Mapping(target = "busTypeCode", expression = "java(b.getBusType() != null ? b.getBusType().getCode() : null)")
    @Mapping(target = "busTypeName", expression = "java(b.getBusType() != null ? b.getBusType().getName() : null)")
    BusResDTO toBusRes(Bus b);

    SeatResDTO toSeatRes(Seat s);
}
