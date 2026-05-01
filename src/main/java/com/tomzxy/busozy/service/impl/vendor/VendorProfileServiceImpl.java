package com.tomzxy.busozy.service.impl.vendor;

import com.tomzxy.busozy.common.enums.BusStatus;
import com.tomzxy.busozy.common.enums.ErrorCode;
import com.tomzxy.busozy.common.enums.TripStatus;
import com.tomzxy.busozy.common.enums.UserStatus;
import com.tomzxy.busozy.dto.request.UpdateVendorProfileReqDTO;
import com.tomzxy.busozy.dto.response.VendorDashboardResDTO;
import com.tomzxy.busozy.dto.response.VendorProfileResDTO;
import com.tomzxy.busozy.entity.Company;
import com.tomzxy.busozy.entity.User;
import com.tomzxy.busozy.exception.ConflictException;
import com.tomzxy.busozy.exception.ResourceNotFoundException;
import com.tomzxy.busozy.mapper.VendorMapper;
import com.tomzxy.busozy.repository.BookingRepository;
import com.tomzxy.busozy.repository.BusRepository;
import com.tomzxy.busozy.repository.CompanyRepository;
import com.tomzxy.busozy.repository.DriverRepository;
import com.tomzxy.busozy.repository.TripRepository;
import com.tomzxy.busozy.repository.UserRepository;
import com.tomzxy.busozy.service.interfaces.vendor.VendorProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VendorProfileServiceImpl implements VendorProfileService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final DriverRepository driverRepository;
    private final BusRepository busRepository;
    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final VendorMapper vendorMapper;

    @Override
    public VendorProfileResDTO getProfile(Long userId) {
        return vendorMapper.toProfileRes(requireVendorUser(userId));
    }

    @Override
    @Transactional
    public VendorProfileResDTO updateProfile(Long userId, UpdateVendorProfileReqDTO req) {
        User user = requireVendorUser(userId);
        Company company = user.getCompany();

        if (!user.getEmail().equals(req.getEmail()) && userRepository.existsByEmail(req.getEmail())) {
            throw new ConflictException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (req.getPhone() != null && !req.getPhone().equals(user.getPhone())
                && userRepository.existsByPhone(req.getPhone())) {
            throw new ConflictException(ErrorCode.PHONE_ALREADY_EXISTS);
        }
        if (req.getTaxCode() != null && !req.getTaxCode().equals(company.getTaxCode())
                && companyRepository.existsByTaxCode(req.getTaxCode())) {
            throw new ConflictException(ErrorCode.TAX_CODE_EXISTS);
        }

        user.setFullName(req.getFullName());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        company.setName(req.getCompanyName());
        company.setTaxCode(req.getTaxCode());
        company.setPhone(req.getCompanyPhone());
        company.setAddress(req.getAddress());

        companyRepository.save(company);
        userRepository.save(user);
        return vendorMapper.toProfileRes(user);
    }

    @Override
    public VendorDashboardResDTO getDashboard(Long userId) {
        User user = requireVendorUser(userId);
        Long companyId = user.getCompany().getId();
        int activeTrips = Math.toIntExact(tripRepository.countByRouteCompanyIdAndStatusIn(
                companyId, List.of(TripStatus.SCHEDULED, TripStatus.DELAYED, TripStatus.DEPARTED)));
        int activeBuses = Math.toIntExact(busRepository.countByCompanyIdAndStatus(companyId, BusStatus.ACTIVE));
        int totalDrivers = Math.toIntExact(driverRepository.countByCompanyIdAndStatus(companyId, UserStatus.ACTIVE));
        int pendingBookings = Math.toIntExact(bookingRepository.countPendingByCompanyId(companyId));
        java.math.BigDecimal todayRevenue = bookingRepository.sumTodayRevenueByCompanyId(companyId, LocalDate.now());
        return new VendorDashboardResDTO(activeTrips, activeBuses, totalDrivers, todayRevenue, pendingBookings);
    }

    private User requireVendorUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        if (!user.isVendor()) {
            throw new AccessDeniedException("User is not a vendor");
        }
        return user;
    }
}
