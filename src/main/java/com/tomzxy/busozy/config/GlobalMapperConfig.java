package com.tomzxy.busozy.config;

import org.mapstruct.MapperConfig;
import org.mapstruct.ReportingPolicy;

/**
 * Shared MapStruct mapper configuration (Qwen recommendation).
 * Reference this in each @Mapper via: config = GlobalMapperConfig.class
 */
@MapperConfig(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GlobalMapperConfig {
}
