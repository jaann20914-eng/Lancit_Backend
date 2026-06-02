package com.ssafy.lancit.common.config;

import org.apache.ibatis.type.EnumTypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ssafy.lancit.global.enums.ApplicationStatus;
import com.ssafy.lancit.global.enums.ContractStatus;
import com.ssafy.lancit.global.enums.FileParentType;
import com.ssafy.lancit.global.enums.JobCategory;
import com.ssafy.lancit.global.enums.MessageType;
import com.ssafy.lancit.global.enums.NotificationType;
import com.ssafy.lancit.global.enums.OwnerType;
import com.ssafy.lancit.global.enums.ProposalStatus;
import com.ssafy.lancit.global.enums.RecruitmentStatus;
import com.ssafy.lancit.global.enums.TaskStatus;
import com.ssafy.lancit.global.enums.Weekday;

@Configuration
public class MybatisConfig {

    /**
     값이 정해진 상수 목록
     Java Enum ↔ DB 문자열 자동 변환 (미등록 시 Enum 매핑 오류 발생)
     새 Enum 추가 시 여기에 반드시 등록해야 DB 매핑 정상 동작
     */
    @Bean
    public ConfigurationCustomizer mybatisConfigurationCustomizer() {
        return configuration -> {
            TypeHandlerRegistry registry = configuration.getTypeHandlerRegistry();
            registry.register(JobCategory.class,        EnumTypeHandler.class);
            registry.register(OwnerType.class,          EnumTypeHandler.class);
            registry.register(TaskStatus.class,         EnumTypeHandler.class);
            registry.register(ContractStatus.class,     EnumTypeHandler.class);
            registry.register(RecruitmentStatus.class,  EnumTypeHandler.class);
            registry.register(ApplicationStatus.class,  EnumTypeHandler.class);
            registry.register(ProposalStatus.class,     EnumTypeHandler.class);
            registry.register(FileParentType.class,     EnumTypeHandler.class);
            registry.register(MessageType.class,        EnumTypeHandler.class);
            registry.register(Weekday.class,            EnumTypeHandler.class);
            registry.register(NotificationType.class, EnumTypeHandler.class);
        };
    }
}