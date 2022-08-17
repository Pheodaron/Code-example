package com.aboba.domain.vk.main.threads;

import com.aboba.domain.vk.main.entity.ParsedGroup;
import com.aboba.domain.vk.main.entity.VkAccount;
import com.aboba.domain.vk.main.service.VkService;
import com.aboba.exception.CustomControllerAdvice;
import com.aboba.utils.ApplicationContextHolder;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.groups.responses.GetByIdObjectLegacyResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadThread extends Thread {
    private final Integer groupId;
    private final String groupShortName;
    private final VkAccount credentials;
    private final GetByIdObjectLegacyResponse groupInfo;
    private final ParsedGroup group;

    private final Logger logger = LoggerFactory.getLogger(CustomControllerAdvice.class);

    public DownloadThread(
            String name,
            String groupShortName,
            VkAccount credentials,
            GetByIdObjectLegacyResponse groupInfo,
            ParsedGroup group
    ) {
        super(name);
        this.groupShortName = groupShortName;
        this.credentials = credentials;
        this.groupInfo = groupInfo;
        this.groupId = groupInfo.getId();
        this.group = group;
    }

    public void run() {
        logger.info("Start downloading group members: groupId=" + groupId);
        try {
            var vkService = ApplicationContextHolder.getApplicationContext().getBean(VkService.class);
            vkService.downloadGroupMembers(credentials, groupInfo, groupShortName, group);
        } catch (ClientException | InterruptedException | ApiException e) {
            logger.error(e.getMessage());
        }
        logger.info("End downloading group members: groupId=" + groupId);
    }

    public Integer getGroupId() {
        return groupId;
    }
}
