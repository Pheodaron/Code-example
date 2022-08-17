package com.aboba.vk.metrics;

import com.aboba.annotation.StandaloneCredentials;
import com.aboba.domain.vk.main.entity.VkAccount;
import com.aboba.domain.vk.main.enums.EVkTimeBucket;
import com.aboba.domain.vk.metrics.dto.RegisterVkMetricRequest;
import com.aboba.domain.vk.metrics.dto.VkMetricsResponse;
import com.aboba.exception.IO.WrongParamsException;
import com.aboba.exception.MismatchEnumException;
import com.aboba.swagger.vk.metric.SwaggerRegisterGET;
import com.aboba.swagger.vk.metric.SwaggerRegisterPOST;
import com.aboba.utils.DateTimeParseHelper;
import com.aboba.utils.dto.Response;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@CrossOrigin
@RestController
@RequestMapping("/v1/vk/metrics")
@Tag(name = "v1/vk/metrics", description = "vk metric API")
public class VkMetricController {
    private final VkMetricService vkMetricService;

    public VkMetricController(VkMetricService vkMetricService) {
        this.vkMetricService = vkMetricService;
    }

    @PostMapping("/register")
    @SwaggerRegisterPOST
    public ResponseEntity<Void> registerMetrics(
            @Parameter(hidden = true) @StandaloneCredentials VkAccount credentials,
            @RequestBody RegisterVkMetricRequest request
    ) throws MismatchEnumException {
        var metricType = request.getType();
        vkMetricService.registerMetrics(credentials.getId(), metricType);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/get")
    @SwaggerRegisterGET
    public ResponseEntity<Response<VkMetricsResponse>> getMetrics(
            @Parameter(hidden = true) @StandaloneCredentials VkAccount credentials,
            @RequestParam String timeBucket,
            @RequestParam String type,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) throws MismatchEnumException, WrongParamsException {
        if (!EVkMetric.isEVkMetric(type)) {
            throw new WrongParamsException();
        }
        if (!EVkTimeBucket.isEVkTimeBucket(timeBucket)) {
            throw new WrongParamsException();
        }

        var metricType = EVkMetric.of(type);
        var timeBucketEnum = EVkTimeBucket.of(timeBucket);

        LocalDateTime localDateTimeFrom;
        LocalDateTime localDateTimeTo;
        if (timeBucketEnum.equals(EVkTimeBucket.INTERVAL)) {
            if (from != null && !from.equals("") && to != null && !to.equals("")) {
                localDateTimeFrom = DateTimeParseHelper.fromStringToLocalDateTime(from);
                localDateTimeTo = DateTimeParseHelper.fromStringToLocalDateTime(to);
                if (localDateTimeFrom.isAfter(localDateTimeTo)) {
                    throw new WrongParamsException();
                }
            } else {
                throw new WrongParamsException();
            }
        }
        var response = vkMetricService.getMetrics(credentials.getId(), timeBucketEnum, metricType, from, to);

        return ResponseEntity.ok(new Response<>(true, response));
    }
}
