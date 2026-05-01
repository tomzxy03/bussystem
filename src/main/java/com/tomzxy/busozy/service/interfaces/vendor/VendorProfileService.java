package com.tomzxy.busozy.service.interfaces.vendor;

import com.tomzxy.busozy.dto.request.UpdateVendorProfileReqDTO;
import com.tomzxy.busozy.dto.response.VendorDashboardResDTO;
import com.tomzxy.busozy.dto.response.VendorProfileResDTO;

public interface VendorProfileService {

    VendorProfileResDTO getProfile(Long userId);

    VendorProfileResDTO updateProfile(Long userId, UpdateVendorProfileReqDTO req);

    VendorDashboardResDTO getDashboard(Long userId);
}
