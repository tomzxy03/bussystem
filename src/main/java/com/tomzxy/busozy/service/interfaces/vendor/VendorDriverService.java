package com.tomzxy.busozy.service.interfaces.vendor;

import com.tomzxy.busozy.common.enums.UserStatus;
import com.tomzxy.busozy.dto.request.DriverReqDTO;
import com.tomzxy.busozy.dto.response.DriverResDTO;
import com.tomzxy.busozy.entity.User;

import java.util.List;

public interface VendorDriverService {

    List<DriverResDTO> getDrivers(User currentUser);

    DriverResDTO createDriver(User currentUser, DriverReqDTO req);

    DriverResDTO updateDriver(User currentUser, Long driverId, DriverReqDTO req);

    DriverResDTO updateDriverStatus(User currentUser, Long driverId, UserStatus status);
}
