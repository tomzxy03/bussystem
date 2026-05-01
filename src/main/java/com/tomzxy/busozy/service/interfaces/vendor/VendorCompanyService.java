package com.tomzxy.busozy.service.interfaces.vendor;

import com.tomzxy.busozy.dto.request.CompanyReqDTO;
import com.tomzxy.busozy.dto.response.CompanyResDTO;
import com.tomzxy.busozy.entity.User;

public interface VendorCompanyService {

    CompanyResDTO getCompany(User currentUser);

    CompanyResDTO updateCompany(User currentUser, CompanyReqDTO req);
}
