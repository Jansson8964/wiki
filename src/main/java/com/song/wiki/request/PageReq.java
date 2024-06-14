package com.song.wiki.request;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;

/**
 * page request is a father class , many functions will use pagination
 */
public class PageReq {
    @NotNull(message = "Page number cannot be empty")
    private int page;
    @NotNull(message = "Page size cannot be empty")
    @Max(value = 1000, message = "Page size cannot exceed 1000")
    private int size;

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("PageReq{");
        sb.append("page=").append(page);
        sb.append(", size=").append(size);
        sb.append('}');
        return sb.toString();
    }
}