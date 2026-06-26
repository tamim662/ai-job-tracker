package com.jobtracker.dto;

import java.util.List;

public record JobSearchPageDto(
        List<JobSearchResultDto> results,
        int totalCount,
        int page,
        int resultsPerPage,
        int totalPages
) {}
