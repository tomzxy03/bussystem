package com.tomzxy.busozy.service.interfaces;

import com.tomzxy.busozy.common.enums.StopType;
import com.tomzxy.busozy.dto.request.StopReqDTO;
import com.tomzxy.busozy.dto.response.DistrictResDTO;
import com.tomzxy.busozy.dto.response.ProvinceResDTO;
import com.tomzxy.busozy.dto.response.StopResDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LocationService {

    // Provinces
    List<ProvinceResDTO> getProvinces(String keyword);

    // Districts
    List<DistrictResDTO> getDistrictsByProvince(Long provinceId);

    // Stops – public search
    Page<StopResDTO> searchStops(Long provinceId, Long districtId, StopType type,
            Boolean isMajor, String keyword, Pageable pageable);

    StopResDTO getStopById(Long id);

    // Stops – admin CRUD
    StopResDTO createStop(StopReqDTO req, String idempotencyKey);

    StopResDTO updateStop(Long id, StopReqDTO req);

    void deleteStop(Long id);
}
