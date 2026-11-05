package com.pethop.report.dto;
import com.pethop.report.ReportAction; import jakarta.validation.constraints.*;
public record ReportActionRequest(@NotNull ReportAction action,@Size(max=2000)String details){}
