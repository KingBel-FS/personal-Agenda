package com.ia.api.task.api;

import com.ia.api.common.api.ApiResponse;
import com.ia.api.task.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<TaskResponse> createTask(
            Authentication authentication,
            @Valid @ModelAttribute CreateTaskRequest request
    ) throws IOException {
        return ApiResponse.of(taskService.createTask(authentication.getName(), request));
    }

    @PostMapping(path = "/preview", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<TaskPreviewResponse> previewNextOccurrence(
            Authentication authentication,
            @Valid @RequestBody TaskPreviewRequest request
    ) {
        return ApiResponse.of(taskService.previewNextOccurrence(authentication.getName(), request));
    }

    @GetMapping("/occurrences")
    public ApiResponse<TaskOccurrencePageResponse> listOccurrences(
            Authentication authentication,
            @ModelAttribute TaskOccurrenceListRequest request
    ) {
        return ApiResponse.of(taskService.listOccurrences(authentication.getName(), request));
    }

    @PutMapping(path = "/occurrences/{occurrenceId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<TaskOccurrenceResponse> updateOccurrence(
            Authentication authentication,
            @PathVariable UUID occurrenceId,
            @Valid @RequestBody UpdateTaskOccurrenceRequest request
    ) {
        return ApiResponse.of(taskService.updateOccurrence(authentication.getName(), occurrenceId, request));
    }

    @PostMapping(path = "/occurrences/{occurrenceId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<TaskOccurrenceResponse> updateOccurrencePhoto(
            Authentication authentication,
            @PathVariable UUID occurrenceId,
            @RequestParam TaskMutationScope scope,
            @RequestParam MultipartFile photo
    ) throws IOException {
        return ApiResponse.of(taskService.updateOccurrencePhoto(authentication.getName(), occurrenceId, scope, photo));
    }

    @DeleteMapping(path = "/occurrences/{occurrenceId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<com.ia.api.auth.api.MessageResponse> deleteOccurrence(
            Authentication authentication,
            @PathVariable UUID occurrenceId,
            @Valid @RequestBody DeleteTaskOccurrenceRequest request
    ) {
        return ApiResponse.of(taskService.deleteOccurrence(authentication.getName(), occurrenceId, request));
    }
}
