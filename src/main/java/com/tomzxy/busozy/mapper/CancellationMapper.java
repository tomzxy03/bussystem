package com.tomzxy.busozy.mapper;

import com.tomzxy.busozy.config.GlobalMapperConfig;
import com.tomzxy.busozy.dto.response.CancellationResDTO;
import com.tomzxy.busozy.entity.Cancellation;
import com.tomzxy.busozy.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface CancellationMapper {

    @Mapping(target = "cancellationId", source = "id")
    @Mapping(target = "bookingCode", expression = "java(c.getBooking() != null && c.getBooking().getBookingCode() != null ? c.getBooking().getBookingCode().toString() : null)")
    @Mapping(target = "cancelledByUsername", expression = "java(mapCancelledByUsername(c.getCancelledBy()))")
    CancellationResDTO toCancellationRes(Cancellation c);

    default String mapCancelledByUsername(User user) {
        return user != null ? user.getUsername() : null;
    }
}
