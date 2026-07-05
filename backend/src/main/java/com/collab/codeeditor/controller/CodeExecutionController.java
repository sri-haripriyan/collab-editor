package com.collab.codeeditor.controller;

import com.collab.codeeditor.dto.ExecutionRequest;
import com.collab.codeeditor.dto.ExecutionResponse;
import com.collab.codeeditor.service.CodeExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/api/execute")
public class CodeExecutionController {

    @Autowired
    private CodeExecutionService codeExecutionService;

    @PostMapping
    public ResponseEntity<?> executeCode(@RequestBody ExecutionRequest request) {
        if (request.getSourceCode() == null || request.getSourceCode().isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Source code is required"));
        }
        if (request.getLanguage() == null || request.getLanguage().isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Language is required"));
        }

        try {
            ExecutionResponse response = codeExecutionService.executeCode(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Collections.singletonMap("error", e.getMessage()));
        }
    }
}
