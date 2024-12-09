package com.zero.stockdividend.scheduler;

import com.zero.stockdividend.model.Company;
import com.zero.stockdividend.model.ScrapedResult;
import com.zero.stockdividend.model.constants.CacheKey;
import com.zero.stockdividend.persist.CompanyRepository;
import com.zero.stockdividend.persist.DividendRepository;
import com.zero.stockdividend.persist.entity.CompanyEntity;
import com.zero.stockdividend.persist.entity.DividendEntity;
import com.zero.stockdividend.scraper.Scraper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@EnableCaching
@AllArgsConstructor
public class ScraperScheduler {
    private final CompanyRepository companyRepository;
    private final DividendRepository dividendRepository;

    private final Scraper yahooFinanceScraper;

    @CacheEvict(value = CacheKey.KEY_FINANCE, allEntries = true)
    @Scheduled(cron = "${scheduler.scrap.yahoo}")
    public void yahooFinanceScheduling() {
//        log.info("scraping scheduler is started"); // 스크래핑 스케줄러가 돌아갈 때마다 로그가 남음. 스케줄러가 정상적으로 돌아가는거 확인

        List<CompanyEntity> companies = this.companyRepository.findAll();

        for (var company : companies) {
            log.info("scraping scheduler is started -> " + company.getName()); // 어느 회사스크래핑이 일어났는지 알고싶을때
            ScrapedResult scrapedResult = this.yahooFinanceScraper.scrap(
                    new Company(company.getTicker(), company.getName()));

            scrapedResult.getDividends().stream()
                    .map(e -> new DividendEntity(company.getId(), e))
                    .forEach(e -> {
                        boolean exists = this.dividendRepository.existsByCompanyIdAndDate(e.getCompanyId(), e.getDate());
                        if (!exists) {
                            this.dividendRepository.save(e);
                            log.info("insert new dividend -> " + e.toString());
                        }
                    });

            // 스크래핑 대상 사이트 서버에 연속적으로 요청을 날리지 않도록 일시정지
            try {
                Thread.sleep(3000); // 3 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

}
