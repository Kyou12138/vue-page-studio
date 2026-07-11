package com.studio.vuepage.api;

import com.studio.vuepage.ai.AiDslService;
import com.studio.vuepage.dsl.model.PageDsl;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 生成 / 修改 PageDSL 接口。
 */
@RestController
@RequestMapping("/api/ai")
@Validated
public class AiController {

    private final AiDslService aiDslService;

    public AiController(AiDslService aiDslService) {
        this.aiDslService = aiDslService;
    }

    @PostMapping("/generate")
    public ApiResponse<PageDsl> generate(@RequestBody GenerateRequest request) {
        if (request == null || request.description() == null || request.description().isBlank()) {
            throw new IllegalArgumentException("description is required");
        }
        PageDsl dsl = aiDslService.generate(
                request.description(),
                request.pageType(),
                request.style()
        );
        return ApiResponse.ok(dsl);
    }

    @PostMapping("/modify")
    public ApiResponse<PageDsl> modify(@RequestBody ModifyRequest request) {
        if (request == null || request.dsl() == null) {
            throw new IllegalArgumentException("dsl is required");
        }
        if (request.instruction() == null || request.instruction().isBlank()) {
            throw new IllegalArgumentException("instruction is required");
        }
        PageDsl dsl = aiDslService.modify(request.dsl(), request.instruction());
        return ApiResponse.ok(dsl);
    }

    public record GenerateRequest(
            @NotBlank String description,
            String pageType,
            String style
    ) {
    }

    public record ModifyRequest(
            @NotNull PageDsl dsl,
            @NotBlank String instruction
    ) {
    }
}
