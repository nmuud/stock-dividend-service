package com.zero.stockdividend.scraper;

import com.zero.stockdividend.model.Company;
import com.zero.stockdividend.model.Dividend;
import com.zero.stockdividend.model.ScrapedResult;
import com.zero.stockdividend.model.constants.Month;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class YahooFinanceScraper implements Scraper {

    private static final String STATISTICS_URL = "https://finance.yahoo.com/quote/%s/history/?frequency=1mo&period1=%d&period2=%d";

    private static final String SUMMARY_URL = "https://finance.yahoo.com/quote/%s?p=%s";

    private static final long START_TIME = 86400; // 60 * 60 * 24

    @Override
    public ScrapedResult scrap(Company company) {
        var scrapedResult = new ScrapedResult();
        scrapedResult.setCompany(company);

        try {
            long now = System.currentTimeMillis() / 1000;
            String url = String.format(STATISTICS_URL, company.getTicker(), START_TIME, now);

            System.out.println("Request URL: " + url); // 요청 URL 확인

            Connection connection = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/85.0.4183.121 Safari/537.36")
                    .timeout(10000);
            Document document = connection.get();

            Elements tableEle = document.select("table.yf-j5d1ld.noDl");
//            System.out.println("Table Elements: " + tableElements);

            if (tableEle.isEmpty()) {
                System.out.println("No table found in the document.");
                return scrapedResult;
            }

            Element table = tableEle.first();
            Element tbody = table.selectFirst("tbody");
            if (tbody == null) {
                System.out.println("Tbody not found in the table.");
                return scrapedResult;
            }
//            System.out.println("Tbody Content: " + tbody);

            List<Dividend> dividends = new ArrayList<>();
            for (Element row : tbody.children()) {
                String text = row.text();
//            System.out.println("Row Content: " + text); // 각 행의 데이터 확인

                if (!text.endsWith("Dividend")) {
                    continue;
                }

                String[] splits = text.split(" ");
                if (splits.length < 4) {
                    System.out.println("Unexpected row format: " + text);
                    continue;
                }

                int month = Month.strToNumber(splits[0]);
                int day = Integer.parseInt(splits[1].replace(",", ""));
                int year = Integer.parseInt(splits[2]);
                String dividend = splits[3];

                if (month < 0) {
                    System.out.println("Invalid month: " + splits[0]);
//                    throw new RuntimeException("Unexpected Month enum value -> " + splits[0]);
                    continue;
                }

                dividends.add(new Dividend(LocalDateTime.of(year, month, day, 0, 0), dividend));
            }

            scrapedResult.setDividends(dividends);

        } catch (IOException e) {
            log.error("Scraper: Error while scraping data.", e);
        }

        return scrapedResult;
    }

    @Override
    public Company scrapCompanyByTicker(String ticker) {
        String url = String.format(SUMMARY_URL, ticker, ticker);

        try {
            Document document = Jsoup.connect(url).get();
            Element titleEle = document.selectFirst("h1.yf-xxbei9");

          //System.out.println("titleEle  = " + titleEle);

            String title = titleEle.text().split("\\(")[0].trim();

            return new Company(ticker, title);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
