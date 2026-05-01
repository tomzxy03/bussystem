package com.tomzxy.busozy.mapper;

import com.tomzxy.busozy.config.GlobalMapperConfig;
import com.tomzxy.busozy.dto.response.BookingDetailResDTO;
import com.tomzxy.busozy.dto.response.BookingResDTO;
import com.tomzxy.busozy.entity.Booking;
import com.tomzxy.busozy.entity.BookingSeat;
import com.tomzxy.busozy.entity.Passenger;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface BookingMapper {

    @Mapping(target = "tripId", source = "trip.id")
    @Mapping(target = "routeName", expression = "java(b.getTrip() != null && b.getTrip().getRoute() != null ? b.getTrip().getRoute().getName() : null)")
    @Mapping(target = "companyName", expression = "java(b.getTrip() != null && b.getTrip().getRoute() != null && b.getTrip().getRoute().getCompany() != null ? b.getTrip().getRoute().getCompany().getName() : null)")
    @Mapping(target = "departureDate", source = "trip.departureDate")
    @Mapping(target = "departureTime", source = "trip.departureTime")
    @Mapping(target = "totalAmount", source = "finalPrice")
    @Mapping(target = "expiredAt", source = "reservedUntil")
    BookingResDTO toBookingRes(Booking b);

    @Mapping(target = "seatNumber", source = "seat.seatNumber")
    @Mapping(target = "seatType", expression = "java(bs.getSeat() != null && bs.getSeat().getSeatType() != null ? bs.getSeat().getSeatType().name() : null)")
    BookingDetailResDTO.BookingSeatResDTO toSeatRes(BookingSeat bs);

    BookingDetailResDTO.PassengerResDTO toPassengerRes(Passenger p);
}
