package com.zero.stockdividend.web;

import com.zero.stockdividend.model.Company;
import com.zero.stockdividend.model.constants.CacheKey;
import com.zero.stockdividend.persist.entity.CompanyEntity;
import com.zero.stockdividend.service.CompanyService;
import lombok.AllArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/company")
@AllArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final CacheManager redisCacheManager;

    @GetMapping("/autocomplete")
    public ResponseEntity<?> autocomplete(@RequestParam String keyword) { // 실제 저장된 트라이에서 데이터를 가져옴
        //var result = this.companyService.autoComplete(keyword); // 트라이 이용한 자동완성

        // 데이터를 찾는 연산이 DB 에서 이루어짐. DB 에 부하를 줄 수 있음
        var result = this.companyService.getCompanyNamesByKeyword(keyword); // 이렇게 구현하면 따로 트라이에 저장할 필요가 없기떄문에 회사명을 저장할 때 트라이에 데이터를 저장하는 로직은 필요없음
        return ResponseEntity.ok(result);
    }

    // 회사 리스트 조회
    @GetMapping
    @PreAuthorize("hasRole('READ')")
    public ResponseEntity<?> searchCompany(final Pageable pageable) {
        Page<CompanyEntity> companies = this.companyService.getAllCompany(pageable);
        return ResponseEntity.ok(companies);
    }

    // 배당금 데이터 저장
    @PostMapping
    @PreAuthorize("hasRole('WRITE')") // 체크
    public ResponseEntity<?> addCompany(@RequestBody Company request) {
        String ticker = request.getTicker().trim();
        if (ObjectUtils.isEmpty(ticker)) {
            throw new RuntimeException("ticker is empty");
        }

        Company company = this.companyService.save(ticker);
        this.companyService.addAutocompleteKeyword(company.getName()); // 회사를 저장할 때마다 트라이에 회사명이 저장됨
        return ResponseEntity.ok(company);
    }

    // 회사 및 배당금 삭제
    @DeleteMapping("/{ticker}")
    @PreAuthorize("hasRole('WRITE')")
    public ResponseEntity<?> deleteCompany(@PathVariable String ticker) {
        String companyName = this.companyService.deleteCompany(ticker);
        this.clearFinanceCache(companyName);
        return ResponseEntity.ok(companyName);
    }

    // 회사 및 배당금 캐시 삭제
    public void clearFinanceCache(String companyName) {
        this.redisCacheManager.getCache(CacheKey.KEY_FINANCE).evict(companyName);
    }

}
