package com.zero.stockdividend.service;

import com.zero.stockdividend.exception.impl.NoCompanyException;
import com.zero.stockdividend.model.Company;
import com.zero.stockdividend.model.Dividend;
import com.zero.stockdividend.model.ScrapedResult;
import com.zero.stockdividend.model.constants.CacheKey;
import com.zero.stockdividend.persist.CompanyRepository;
import com.zero.stockdividend.persist.DividendRepository;
import com.zero.stockdividend.persist.entity.CompanyEntity;
import com.zero.stockdividend.persist.entity.DividendEntity;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class FinanceService {

    private final CompanyRepository companyRepository;
    private final DividendRepository dividendRepository;

    @Cacheable(key = "#companyName", value = CacheKey.KEY_FINANCE)
    public ScrapedResult getDividendByCompanyName(String companyName) {
        log.info("search company -> {}", companyName);
        CompanyEntity company = this.companyRepository.findByName(companyName)
                .orElseThrow(() -> new NoCompanyException());

        List<DividendEntity> dividendEntities = dividendRepository.findAllByCompanyIdOrderByDateDesc(company.getId());

        List<Dividend> dividends = dividendEntities.stream()
                .map(e -> new Dividend(e.getDate(), e.getDividend()))
                .collect(Collectors.toList());

        return new ScrapedResult(new Company(company.getTicker(), company.getName()),
                dividends);
    }

}
