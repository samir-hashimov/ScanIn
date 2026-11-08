package com.scanin.dto.request;

import lombok.Data;

@Data
public class CompleteTaskRequest {
    private String comment;
    private String completionComment;
}
