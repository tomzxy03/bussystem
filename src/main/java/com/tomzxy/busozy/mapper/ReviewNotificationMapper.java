package com.tomzxy.busozy.mapper;

import com.tomzxy.busozy.config.GlobalMapperConfig;
import com.tomzxy.busozy.dto.response.NotificationResDTO;
import com.tomzxy.busozy.dto.response.ReviewResDTO;
import com.tomzxy.busozy.entity.Notification;
import com.tomzxy.busozy.entity.Review;
import com.tomzxy.busozy.entity.Trip;
import com.tomzxy.busozy.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface ReviewNotificationMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", expression = "java(mapUsername(review.getUser()))")
    @Mapping(target = "tripId", source = "trip.id")
    @Mapping(target = "routeName", expression = "java(mapRouteName(review.getTrip()))")
    ReviewResDTO toReviewRes(Review review);

    @Mapping(target = "userId", expression = "java(null)")
    @Mapping(target = "username", expression = "java(mapPublicUsername(review.getUser()))")
    @Mapping(target = "tripId", source = "trip.id")
    @Mapping(target = "routeName", expression = "java(mapRouteName(review.getTrip()))")
    ReviewResDTO toPublicReviewRes(Review review);

    NotificationResDTO toNotificationRes(Notification notification);

    default String mapUsername(User user) {
        return user != null ? user.getUsername() : null;
    }

    default String mapPublicUsername(User user) {
        if (user == null || user.getId() == null) {
            return "anonymous_customer";
        }
        return "user_" + user.getId();
    }

    default String mapRouteName(Trip trip) {
        return trip != null && trip.getRoute() != null ? trip.getRoute().getName() : null;
    }
}
