package hu.javadev.vigszinhaz.service;

import java.io.IOException;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JsoupWebPageClient implements WebPageClient {

  private static final String USER_AGENT =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
          + "(KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36";

  private final int requestTimeout;

  public JsoupWebPageClient(
      @Value("${vigszinhaz.monitor.request-timeout:10000}") int requestTimeout) {
    this.requestTimeout = requestTimeout;
  }

  @Override
  public String load(String url) throws IOException {
    return Jsoup.connect(url)
        .userAgent(USER_AGENT)
        .timeout(requestTimeout)
        .get()
        .outerHtml();
  }
}
