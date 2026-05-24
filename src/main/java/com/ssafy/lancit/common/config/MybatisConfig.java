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
import com.ssafy.lancit.global.enums.OwnerType;
import com.ssafy.lancit.global.enums.ProposalStatus;
import com.ssafy.lancit.global.enums.RecruitmentStatus;
import com.ssafy.lancit.global.enums.TaskStatus;
 
@Configuration
public class MybatisConfig {
 
    /**
     * MyBatis Enum TypeHandler 등록
     * DB에 Enum 이름 문자열로 저장/조회
     */
    @Bean
    public ConfigurationCustomizer mybatisConfigurationCustomizer() {
        return configuration -> {
            TypeHandlerRegistry registry = configuration.getTypeHandlerRegistry();
            registry.register(JobCategory.class, EnumTypeHandler.class);
            registry.register(OwnerType.class, EnumTypeHandler.class);
            registry.register(TaskStatus.class, EnumTypeHandler.class);
            registry.register(ContractStatus.class, EnumTypeHandler.class);
            registry.register(RecruitmentStatus.class, EnumTypeHandler.class);
            registry.register(ApplicationStatus.class, EnumTypeHandler.class);
            registry.register(ProposalStatus.class, EnumTypeHandler.class);
            registry.register(FileParentType.class, EnumTypeHandler.class);
            registry.register(MessageType.class, EnumTypeHandler.class);
        };
    }
}
