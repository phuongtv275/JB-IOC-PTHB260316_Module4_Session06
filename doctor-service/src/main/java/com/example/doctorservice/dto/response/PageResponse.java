package com.example.doctorservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * DTO chuẩn hóa cho dữ liệu phân trang trả về từ API GET danh sách bác sĩ.
 *
 * @param <T> Kiểu dữ liệu của các phần tử trong danh sách phân trang.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> {

    private List<T> items;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean isFirst;
    private boolean isLast;
    private boolean hasNext;
    private boolean hasPrevious;

    /**
     * Chuyển đổi từ Spring Data Page sang PageResponse.
     *
     * @param page Đối tượng Page từ Spring Data.
     * @param <T>  Kiểu dữ liệu.
     * @return Đối tượng PageResponse đã đóng gói.
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .items(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }
}
