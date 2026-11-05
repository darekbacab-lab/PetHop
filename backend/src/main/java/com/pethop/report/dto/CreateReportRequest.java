package com.pethop.report.dto;
import com.pethop.report.ReportCategory; import jakarta.validation.constraints.*;
public record CreateReportRequest(@NotNull ReportCategory category,@NotBlank@Size(max=4000)String description){}
