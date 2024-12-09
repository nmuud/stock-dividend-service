package com.zero.stockdividend.scraper;

import com.zero.stockdividend.model.Company;
import com.zero.stockdividend.model.ScrapedResult;

public interface Scraper {
    Company scrapCompanyByTicker(String ticker);

    ScrapedResult scrap(Company company);

}
