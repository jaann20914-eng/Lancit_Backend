package com.ssafy.lancit.domain.contract.scheduler;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.lancit.domain.contract.dto.ContractDTO;
import com.ssafy.lancit.domain.contract.mapper.ContractMapper;
import com.ssafy.lancit.domain.notification.service.NotificationService;
import com.ssafy.lancit.global.enums.ContractStatus;
import com.ssafy.lancit.global.enums.NotificationType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ContractScheduler {

    private final ContractMapper contractMapper;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void updateCompletedPendingContracts() {

        List<Integer> contractIds =
                contractMapper.findCompletedPendingTargets();

        for (Integer contractId : contractIds) {

            ContractDTO contract = contractMapper.findById(contractId);

            contractMapper.updateStatus(
                    contractId,
                    ContractStatus.COMPLETED_PENDING
            );

            notificationService.createNotification(
                    contract.getCompanyEmail(),
                    NotificationType.CONTRACT_COMPLETED_PENDING,
                    contractId
            );

            notificationService.createNotification(
                    contract.getFreelancerEmail(),
                    NotificationType.CONTRACT_COMPLETED_PENDING,
                    contractId
            );
        }
    }
}