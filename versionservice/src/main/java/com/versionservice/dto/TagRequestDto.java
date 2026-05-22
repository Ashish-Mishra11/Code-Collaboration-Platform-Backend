package com.versionservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TagRequestDto {
    @NotBlank(message = "Tag name is required")
    private String tag;
}
