package com.arsalankhan.venuego;

import java.util.Map;

class SearchHistory {
    private int searchId;
    private String userId;
    private String searchQuery;
    private Map<String, Object> filters;
    private int resultCount;
    private String searchedAt;

    // Getters and setters
    public int getSearchId() { return searchId; }
    public void setSearchId(int searchId) { this.searchId = searchId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getSearchQuery() { return searchQuery; }
    public void setSearchQuery(String searchQuery) { this.searchQuery = searchQuery; }

    public Map<String, Object> getFilters() { return filters; }
    public void setFilters(Map<String, Object> filters) { this.filters = filters; }

    public int getResultCount() { return resultCount; }
    public void setResultCount(int resultCount) { this.resultCount = resultCount; }

    public String getSearchedAt() { return searchedAt; }
    public void setSearchedAt(String searchedAt) { this.searchedAt = searchedAt; }
}